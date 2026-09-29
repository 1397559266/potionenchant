package net.diexv.potionenchant.SkyRender.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders;
import net.diexv.potionenchant.SkyRender.client.shader.DiexvSwordShaders;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DiexvSword（diexv_sword 物品）的像素体素多面体网格 + 特效。
 *
 * 模型逻辑：逐像素扫描贴图 alpha 构造成体素；相邻像素（含对角，8 连通）
 * 若贴图本体颜色相同则合并为同一个模型整体（实心区域渲染为单个大块，
 * 非实心区域回退逐像素）。渲染时所有面按像素段细分，紧贴邻居的共享
 * 内部面段跳过（透明模式无内部面叠加）。
 */
@OnlyIn(Dist.CLIENT)
public final class DiexvSwordVoxelMesh {

    private static final ResourceLocation TEXTURE = new ResourceLocation("potionenchant", "item/diexv_sword");
    private static final int ALPHA_THRESHOLD = 32;
    /** elastic ease-out 首个波峰（用于归一化使峰值精确等于 TRIGGER_SCALE） */
    private static final float ELASTIC_PEAK = 1.354F;

    private static TextureAtlasSprite cachedSprite;
    private static List<Voxel> cachedVoxels;
    private static List<MergeBlock> cachedBlocks;
    /** 模型中心（0..1 空间） */
    private static float cachedCenterX = 0.5F;
    private static float cachedCenterY = 0.5F;
    /** 非透明像素网格（面剔除用） */
    private static boolean[] cachedOpaque;
    private static int cachedW;
    private static int cachedH;
    private static boolean buildFailed;

    // ===== 特效状态（时间基准统一为墙钟 AvaritiaShaders.cosmicTimeTicks()，暂停也继续走） =====
    private static final RandomSource RANDOM = RandomSource.create();
    /** 活跃触发：单像素索引 → 触发数据（含碎片）；多个可同时存在，动画结束后清理 */
    private static final Map<Integer, TriggerData> ACTIVE_TRIGGERS = new HashMap<>();
    /** 最近触发时间戳队列（严格时间窗口计数用） */
    private static final ArrayDeque<Float> RECENT_TRIGGERS = new ArrayDeque<>();

    // ===== 蓄力动画状态机（右键蓄力 → 发射：爆裂碎开与冷却恢复并行 → 常态） =====
    private enum ChargePhase { REST, CHARGING, RECOVERING }
    private static ChargePhase chargePhase = ChargePhase.REST;
    private static int chargePhaseTick = 0;
    /** 蓄力进度 0~1（底部 → 顶部变蓝） */
    private static float chargeProgress = 0.0F;
    private static int lastChargeTick = -1;
    private static final int CHARGE_TICKS = 20;      // 蓄力 1 秒（与服务端激光蓄力同步）
    private static final int FIRE_TICKS = 10;        // 爆裂碎开动画时长（tick）
    private static final int SHARD_RESIDUE_TICKS = 8; // 爆裂碎片残留淡出时长（tick，碎片残留更久）
    private static final int COOLDOWN_TICKS = 20;    // 冷却恢复 1 秒（从发射时刻起算，与服务端激光冷却同步）
    /** 发射瞬间全剑爆裂的触发数据（所有像素同时爆开） */
    private static final Map<Integer, TriggerData> FIRE_TRIGGERS = new HashMap<>();
    private static float fireStartTime = 0.0F;
    /** 发射后并行计时：爆裂碎开与冷却恢复同时推进 */
    private static int fireTick = 0;
    private static int cooldownTick = 0;

    /** 是否对当前展示环境启用体素替代 */
    public static boolean shouldVoxelReplace(ItemDisplayContext context) {
        if (!DiexvSwordShaders.VOXEL_ENABLED) return false;
        if (context == ItemDisplayContext.GUI && !DiexvSwordShaders.VOXEL_GUI_ENABLED) return false;
        return true;
    }

    /** 是否处于蓄力动画阶段（蓄力/碎开/冷却恢复）：只有此时才用体素替代原贴图模型。
     *  注意：正在右键蓄力（使用剑）时必须直接返回 true——
     *  chargePhase 由 renderMesh 内的 updateChargeState 推进，而 renderMesh 仅在体素启用时被调用，
     *  若此处不直接判定"使用中"，会形成死锁导致蓄力动画永不触发。 */
    public static boolean isChargeAnimating() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isUsingItem() && player.getUseItem().getItem() == ModItems.DIEXV_SWORD.get()) {
            return true;
        }
        return chargePhase != ChargePhase.REST;
    }

    /** 在调用方 PoseStack 中渲染体素多面体（含特效） */
    public static void renderMesh(PoseStack poseStack, MultiBufferSource buffers) {
        if (!DiexvSwordShaders.isMeshReady()) return;
        List<Voxel> voxels = getVoxels();
        List<MergeBlock> blocks = getBlocks();
        if (voxels == null || voxels.isEmpty() || blocks == null || blocks.isEmpty()) return;

        // 连续时间（统一墙钟，含毫秒精度）：动画平滑，暂停也继续走（与 GL 粒子/着色器一致）
        // 不要改回 renderTime/renderFrame（那是 tick 基准、暂停冻结）
        float now = AvaritiaShaders.cosmicTimeTicks();
        updateChargeState();

        // 蓝白渐变基础色：固定蓝色 hue=0.6（蓝），饱和度在 0.6 与 0 之间平滑往返
        // （蓝→白→蓝），避免经过红黄等其他色相。用余弦往返（三角平滑）替代锯齿波，
        // 避免到端点瞬间跳变导致的"突然中断"。
        float cycle = (now / 20.0F * DiexvSwordShaders.HUE_SPEED) % 2.0F;
        float ping = 0.5F + 0.5F * (float) Math.cos(cycle * Math.PI); // 1→0→1：蓝→白→蓝
        float hue = 0.6F;
        float saturation = 0.6F * ping;
        int base = hsvToRgb(hue, saturation, 1.0F);
        float baseR = ((base >> 16) & 0xFF) / 255.0F;
        float baseG = ((base >> 8) & 0xFF) / 255.0F;
        float baseB = (base & 0xFF) / 255.0F;

        VertexConsumer consumer = buffers.getBuffer(DiexvSwordShaders.DIEXVSWORD_MESH_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        float thickness = Math.max(DiexvSwordShaders.Z_THICKNESS, 0.1F);

        if (chargePhase == ChargePhase.REST) {
            // 常态：随机触发爆裂
            updateTriggers(now, voxels.size());
        }

        for (MergeBlock block : blocks) {
            if (chargePhase == ChargePhase.REST && block.solid()) {
                // 实心合并块：整体渲染（面按像素段剔除紧贴邻居）
                renderUnit(matrix, consumer,
                        block.x0(), block.y0(), block.x1(), block.y1(),
                        block.minJ(), block.maxJ(), block.minK(), block.maxK(),
                        thickness, 1.0F, baseR, baseG, baseB, DiexvSwordShaders.BASE_ALPHA);
                // 块内被触发的单像素：弹性放大 → 爆开碎裂消失
                for (int idx : block.pixels()) {
                    TriggerData trig = ACTIVE_TRIGGERS.get(idx);
                    if (trig == null) continue;
                    renderTrigger(matrix, consumer, voxels.get(idx), trig, now, thickness);
                }
            } else {
                // 非实心 / 蓄力动画阶段：逐像素渲染
                for (int idx : block.pixels()) {
                    Voxel v = voxels.get(idx);
                    switch (chargePhase) {
                        case CHARGING -> renderVoxelCharging(matrix, consumer, v, idx, now, thickness, baseR, baseG, baseB);
                        case RECOVERING -> renderVoxelRecovering(matrix, consumer, v, idx, now, thickness, baseR, baseG, baseB);
                        default -> {
                            TriggerData trig = ACTIVE_TRIGGERS.get(idx);
                            if (trig == null) {
                                renderUnit(matrix, consumer,
                                        v.x0(), v.y0(), v.x1(), v.y1(),
                                        v.j(), v.j(), v.k(), v.k(),
                                        thickness, 1.0F, baseR, baseG, baseB, DiexvSwordShaders.BASE_ALPHA);
                            } else {
                                renderTrigger(matrix, consumer, v, trig, now, thickness);
                            }
                        }
                    }
                }
            }
        }
    }

    /** 每客户端 tick 推进一次蓄力状态机（闸门走统一墙钟：粒度不变，但暂停也继续走） */
    private static void updateChargeState() {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean using = player != null && player.isUsingItem()
                && player.getUseItem().getItem() == ModItems.DIEXV_SWORD.get();
        int tick = (int) AvaritiaShaders.cosmicTimeTicks();
        if (tick == lastChargeTick) return;
        lastChargeTick = tick;

        switch (chargePhase) {
            case REST -> {
                FIRE_TRIGGERS.clear();
                if (using) {
                    chargePhase = ChargePhase.CHARGING;
                    chargePhaseTick = 0;
                    chargeProgress = 0.0F;
                }
            }
            case CHARGING -> {
                if (!using) {
                    // 松开右键：取消蓄力，立即恢复常态
                    chargePhase = ChargePhase.REST;
                    chargePhaseTick = 0;
                    chargeProgress = 0.0F;
                    FIRE_TRIGGERS.clear();
                    return;
                }
                chargePhaseTick++;
                chargeProgress = Math.min(1.0F, chargePhaseTick / (float) CHARGE_TICKS);
                // 蓄力满后保持 CHARGING，等待服务端 FIRE 同步包（forceFire）触发爆裂，
                // 使爆裂动画与激光发射瞬间精确同步
            }
            case RECOVERING -> {
                // 爆裂碎开与冷却计时并行；冷却播完剑恢复完整可见
                fireTick++;
                cooldownTick++;
                if (cooldownTick >= COOLDOWN_TICKS) {
                    chargePhase = ChargePhase.REST;
                    chargePhaseTick = 0;
                    chargeProgress = 0.0F;
                    fireTick = 0;
                    cooldownTick = 0;
                    FIRE_TRIGGERS.clear();
                }
            }
        }
    }

    /** 服务端同步：开始蓄力 */
    public static void forceCharge() {
        chargePhase = ChargePhase.CHARGING;
        chargePhaseTick = 0;
        chargeProgress = 0.0F;
        FIRE_TRIGGERS.clear();
    }

    /** 服务端同步：激光发射瞬间 → 全剑爆裂碎开 + 冷却开始（与服务端 fireBeam 同刻） */
    public static void forceFire() {
        chargePhase = ChargePhase.RECOVERING;
        chargePhaseTick = 0;
        fireTick = 0;
        cooldownTick = 0;
        fireStartTime = AvaritiaShaders.cosmicTimeTicks();
        buildFireTriggers();
    }

    /** 服务端同步：冷却结束 → 剑恢复完整可见 */
    public static void forceRestore() {
        chargePhase = ChargePhase.REST;
        chargePhaseTick = 0;
        chargeProgress = 0.0F;
        fireTick = 0;
        cooldownTick = 0;
        FIRE_TRIGGERS.clear();
    }

    /** 为所有像素生成一次性爆裂触发（发射瞬间整剑碎开，强化碎片/放大，更明显） */
    private static void buildFireTriggers() {
        List<Voxel> voxels = getVoxels();
        FIRE_TRIGGERS.clear();
        if (voxels == null || voxels.isEmpty()) return;
        float unit = 1.0F / Math.max(1, Math.max(cachedW, cachedH));
        float dur = FIRE_TICKS;
        for (int idx = 0; idx < voxels.size(); idx++) {
            float hue = 0.55F + RANDOM.nextFloat() * 0.12F;
            float sat = 0.25F + RANDOM.nextFloat() * 0.55F;
            int cc = hsvToRgb(hue, sat, 1.0F);
            FIRE_TRIGGERS.put(idx, new TriggerData(fireStartTime, dur, makeFireShards(unit),
                    ((cc >> 16) & 0xFF) / 255.0F, ((cc >> 8) & 0xFF) / 255.0F, (cc & 0xFF) / 255.0F,
                    2.2F, false)); // reborn=false：爆裂后直接消失，不播重生段（避免闪现完整剑）
        }
    }

    /** 蓄力阶段：从底部按进度变蓝；变蓝块放大到 TRIGGER_SCALE 峰值并抖动/晃动 */
    private static void renderVoxelCharging(Matrix4f matrix, VertexConsumer consumer, Voxel v, int idx,
                                            float now, float thickness,
                                            float baseR, float baseG, float baseB) {
        float cy = (v.y0() + v.y1()) * 0.5F;
        if (cy > chargeProgress) {
            // 尚未变蓝：常态基础色
            renderUnit(matrix, consumer,
                    v.x0(), v.y0(), v.x1(), v.y1(),
                    v.j(), v.j(), v.k(), v.k(),
                    thickness, 1.0F, baseR, baseG, baseB, DiexvSwordShaders.BASE_ALPHA);
            return;
        }
        // 变蓝块：常态动态色（蓝白渐变）基础上向纯蓝偏移 85%——既明显"变蓝"，
        // 又随常态模型变色衔接（明暗随 HUE_SPEED 渐变，避免固定色跳变）
        float scale = DiexvSwordShaders.TRIGGER_SCALE;
        float cx = (v.x0() + v.x1()) * 0.5F;
        float half = Math.min(v.x1() - v.x0(), v.y1() - v.y0()) * 0.5F * scale;
        // 抖动（原平滑晃动）：基于离散 tick 的伪随机跳变——每 tick 变蓝块整体突然
        // 跳变一次（同一 tick 内各帧一致，暂停冻结），顿挫感强；叠加像素级小相位差
        // 保留层次，避免整体过于僵硬
        int tick = (int) AvaritiaShaders.cosmicTimeTicks();
        float jit = 0.025F; // 位移幅度（模型空间，约 2 像素）
        float dx = (hash01(tick * 131 + 17) - 0.5F) * 2.0F * jit;
        float dy = (hash01(tick * 197 + 29) - 0.5F) * 2.0F * jit;
        float rot = (hash01(tick * 223 + 41) - 0.5F) * 2.0F * 0.45F;
        dx += (float) (Math.sin(idx * 1.7F) * 0.004F);
        dy += (float) (Math.cos(idx * 1.3F) * 0.004F);
        float vR = baseR + (0.25F - baseR) * 0.85F;
        float vG = baseG + (0.55F - baseG) * 0.85F;
        float vB = baseB + (1.0F - baseB) * 0.85F;
        renderShard(matrix, consumer, cx + dx, cy + dy, 0.5F, half, rot,
                vR, vG, vB, DiexvSwordShaders.BASE_ALPHA);
    }

    /** 恢复阶段（发射后，由服务端 FIRE 同步包驱动）：
     *  - 爆裂（前 FIRE_TICKS）：整剑作为整体先弹性放大（所有像素一起），随后整剑碎裂成碎片飞散
     *  - 碎片残留（FIRE_TICKS ~ FIRE_TICKS+SHARD_RESIDUE_TICKS）：碎片继续飞散并缓慢淡出
     *  - 冷却恢复：与爆裂并行推进，从底部以常态动态色渐现长回（无虚影，未恢复区域不渲染）
     *  - 冷却播完（回 REST / forceRestore）：剑恢复完整可见 */
    private static void renderVoxelRecovering(Matrix4f matrix, VertexConsumer consumer, Voxel v, int idx,
                                              float now, float thickness,
                                              float baseR, float baseG, float baseB) {
        float fireProgress = now - fireStartTime;
        if (fireProgress < FIRE_TICKS) {
            float t = fireProgress / FIRE_TICKS; // 0~1 爆裂进度
            float appear = Math.max(0.05F, Math.min(0.95F, DiexvSwordShaders.TRIGGER_APPEAR_FRACTION));
            if (t < appear) {
                // 整剑整体弹性放大（所有像素一起渲染同一整体缩放，作为整体爆裂）
                float s = triggerScale(t / appear, 2.2F);
                float cr = baseR + (0.45F - baseR) * 0.6F;
                float cg = baseG + (0.72F - baseG) * 0.6F;
                float cb = baseB + (1.0F - baseB) * 0.6F;
                renderUnit(matrix, consumer,
                        v.x0(), v.y0(), v.x1(), v.y1(),
                        v.j(), v.j(), v.k(), v.k(),
                        thickness, s, cr, cg, cb, DiexvSwordShaders.TRIGGER_ALPHA);
            } else {
                // 整剑碎裂：每个像素的碎片同时飞散（碎片残留更久：alpha 缓降）
                TriggerData trig = FIRE_TRIGGERS.get(idx);
                if (trig == null) return;
                float bt = (t - appear) / (1.0F - appear);
                float alpha = DiexvSwordShaders.TRIGGER_ALPHA * (float) Math.pow(1.0F - bt, 0.6);
                if (alpha <= 0.02F) return;
                float cx = (v.x0() + v.x1()) * 0.5F;
                float cy = (v.y0() + v.y1()) * 0.5F;
                float cz = 0.5F;
                float tSec = bt * (FIRE_TICKS / 20.0F) * 2.2F; // 碎片飞散时间（更远）
                for (Shard s : trig.shards()) {
                    float dist = s.speed() * tSec;
                    renderShard(matrix, consumer,
                            cx + s.dirX() * dist, cy + s.dirY() * dist, cz + s.dirZ() * dist,
                            s.size(), s.rot() + tSec * s.rotSpeed(), trig.r(), trig.g(), trig.b(), alpha);
                }
            }
        } else if (fireProgress < FIRE_TICKS + SHARD_RESIDUE_TICKS) {
            // 碎片残留：继续飞散并缓慢淡出
            TriggerData trig = FIRE_TRIGGERS.get(idx);
            if (trig == null) return;
            float rt = (fireProgress - FIRE_TICKS) / SHARD_RESIDUE_TICKS;
            float alpha = DiexvSwordShaders.TRIGGER_ALPHA * 0.35F * (1.0F - rt);
            if (alpha <= 0.02F) return;
            float cx = (v.x0() + v.x1()) * 0.5F;
            float cy = (v.y0() + v.y1()) * 0.5F;
            float cz = 0.5F;
            float tSec = (FIRE_TICKS / 20.0F) * 2.2F + rt * (SHARD_RESIDUE_TICKS / 20.0F) * 2.2F;
            for (Shard s : trig.shards()) {
                float dist = s.speed() * tSec;
                renderShard(matrix, consumer,
                        cx + s.dirX() * dist, cy + s.dirY() * dist, cz + s.dirZ() * dist,
                        s.size(), s.rot() + tSec * s.rotSpeed(), trig.r(), trig.g(), trig.b(), alpha);
            }
        }
        // 冷却恢复：从底部以常态动态色（蓝白渐变 baseR/G/B）渐现，与常态模型变色完全衔接
        float rec = cooldownTick / (float) COOLDOWN_TICKS;
        float cy2 = (v.y0() + v.y1()) * 0.5F;
        if (cy2 > rec) return;
        float grow = Mth.clamp((rec - cy2) / 0.15F, 0.0F, 1.0F);
        renderUnit(matrix, consumer,
                v.x0(), v.y0(), v.x1(), v.y1(),
                v.j(), v.j(), v.k(), v.k(),
                thickness, Math.max(0.05F, grow), baseR, baseG, baseB,
                DiexvSwordShaders.BASE_ALPHA * grow);
    }

    /** 渲染触发动画三段：出现段弹性放大 → 爆开段碎片飞散消失 → 重生段原位置从小到大重新生成 */
    private static void renderTrigger(Matrix4f matrix, VertexConsumer consumer, Voxel v,
                                      TriggerData trig, float now, float thickness) {
        float t = (now - trig.start()) / trig.dur();
        if (t < 0.0F || t >= 1.0F) return;
        float appear = Math.max(0.05F, Math.min(0.95F, DiexvSwordShaders.TRIGGER_APPEAR_FRACTION));
        float burstEnd = Math.max(appear + 0.05F, Math.min(0.98F, DiexvSwordShaders.TRIGGER_BURST_FRACTION));
        float r = trig.r(), g = trig.g(), b = trig.b();

        if (t < appear) {
            // 出现段：像素块弹性放大（蓝白系触发色，爆裂时峰值更大）
            renderUnit(matrix, consumer,
                    v.x0(), v.y0(), v.x1(), v.y1(),
                    v.j(), v.j(), v.k(), v.k(),
                    thickness, triggerScale(t, trig.scale()), r, g, b, DiexvSwordShaders.TRIGGER_ALPHA);
            return;
        }
        if (t < burstEnd) {
            // 爆开段：碎片沿随机方向飞散，透明度渐变消失
            float bt = (t - appear) / (burstEnd - appear);
            float fade = (float) Math.pow(1.0F - bt, 1.5);
            float alpha = DiexvSwordShaders.TRIGGER_ALPHA * fade;
            if (alpha <= 0.01F) return;
            float cx = (v.x0() + v.x1()) * 0.5F;
            float cy = (v.y0() + v.y1()) * 0.5F;
            float cz = 0.5F;
            float tSec = bt * (trig.dur() / 20.0F) * (burstEnd - appear); // 爆开经过的秒数
            for (Shard s : trig.shards()) {
                float dist = s.speed() * tSec;
                renderShard(matrix, consumer,
                        cx + s.dirX() * dist, cy + s.dirY() * dist, cz + s.dirZ() * dist,
                        s.size(), s.rot() + tSec * s.rotSpeed(), r, g, b, alpha);
            }
            return;
        }
        // 重生段：仅常态随机爆炸播放（爆裂动画 reborn=false，爆开后直接消失，避免闪现完整剑）
        if (!trig.reborn()) return;
        float rt = (t - burstEnd) / (1.0F - burstEnd);
        float grow = smoothstep(Math.max(0.0F, Math.min(1.0F, rt)));
        renderUnit(matrix, consumer,
                v.x0(), v.y0(), v.x1(), v.y1(),
                v.j(), v.j(), v.k(), v.k(),
                thickness, Math.max(0.05F, grow), r, g, b,
                DiexvSwordShaders.TRIGGER_ALPHA * grow);
    }

    /** 渲染单个碎片小方块：绕 z 轴自转 + 平移 */
    private static void renderShard(Matrix4f matrix, VertexConsumer b, float px, float py, float pz,
                                    float half, float rot, float r, float g, float bl, float a) {
        if (half <= 0.0001F || a <= 0.01F) return;
        float c = Mth.cos(rot), s = Mth.sin(rot);
        float[] p = new float[3];
        float h = half;
        // DOWN (y=-h)
        shardV(px, py, pz, -h, -h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, -h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, -h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, -h, -h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        // UP (y=+h)
        shardV(px, py, pz, -h, h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, -h, h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        // NORTH (z=-h)
        shardV(px, py, pz, -h, -h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, -h, h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, -h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        // SOUTH (z=+h)
        shardV(px, py, pz, -h, -h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, -h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, -h, h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        // WEST (x=-h)
        shardV(px, py, pz, -h, -h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, -h, -h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, -h, h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, -h, h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        // EAST (x=+h)
        shardV(px, py, pz, h, -h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, h, -h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
        shardV(px, py, pz, h, -h, h, c, s, p); b.vertex(matrix, p[0], p[1], p[2]).color(r, g, bl, a).endVertex();
    }

    /** 碎片角点：绕 z 轴旋转 + 平移 */
    private static void shardV(float px, float py, float pz, float ox, float oy, float oz,
                               float c, float s, float[] out) {
        out[0] = px + ox * c - oy * s;
        out[1] = py + ox * s + oy * c;
        out[2] = pz + oz;
    }

    /** 随机触发：严格时间窗口（每 TRIGGER_WINDOW_MS 内最多 TRIGGER_MAX_PER_WINDOW 个新触发）；
     *  动画周期每次随机 0.5~2 秒；不限制同时活跃数量 */
    private static void updateTriggers(float now, int pixelCount) {
        if (pixelCount <= 0) return;
        // 清理已结束动画
        if (!ACTIVE_TRIGGERS.isEmpty()) {
            ACTIVE_TRIGGERS.entrySet().removeIf(e -> now - e.getValue().start() >= e.getValue().dur());
        }
        // 滑动窗口：移除窗口外的触发时间戳
        float window = Math.max(1.0F, DiexvSwordShaders.TRIGGER_WINDOW_MS / 50.0F);
        while (!RECENT_TRIGGERS.isEmpty() && now - RECENT_TRIGGERS.peekFirst() > window) {
            RECENT_TRIGGERS.pollFirst();
        }
        // 严格时间限制：窗口内数量达到上限则不触发
        if (RECENT_TRIGGERS.size() >= Math.max(1, DiexvSwordShaders.TRIGGER_MAX_PER_WINDOW)) return;
        // 最小触发间隔 = 窗口 / 上限
        float minInterval = window / Math.max(1, DiexvSwordShaders.TRIGGER_MAX_PER_WINDOW);
        if (!RECENT_TRIGGERS.isEmpty() && now - RECENT_TRIGGERS.peekLast() < minInterval) return;

        long min = Math.max(1L, DiexvSwordShaders.TRIGGER_DURATION_MIN_MS / 50L);
        long max = Math.max(min, DiexvSwordShaders.TRIGGER_DURATION_MAX_MS / 50L);
        float dur = (float) (min + RANDOM.nextInt((int) (max - min + 1)));
        float unit = 1.0F / Math.max(1, Math.max(cachedW, cachedH));
        // 触发爆裂色：蓝白色系随机（hue 0.55 ~ 0.67 纯蓝区，
        // 饱和度 0.25 ~ 0.8 在"深蓝 ↔ 白蓝"之间随机，v=1 保持明亮）
        float hue = 0.55F + RANDOM.nextFloat() * 0.12F;
        float sat = 0.25F + RANDOM.nextFloat() * 0.55F;
        int cc = hsvToRgb(hue, sat, 1.0F);
        ACTIVE_TRIGGERS.put(RANDOM.nextInt(pixelCount),
                new TriggerData(now, dur, makeShards(unit),
                        ((cc >> 16) & 0xFF) / 255.0F, ((cc >> 8) & 0xFF) / 255.0F, (cc & 0xFF) / 255.0F,
                        DiexvSwordShaders.TRIGGER_SCALE, true));
        RECENT_TRIGGERS.addLast(now);
    }

    /** 生成爆开碎片：随机球面方向（z 方向压低，贴图平面为主）、随机速度/大小/旋转 */
    private static Shard[] makeShards(float unit) {
        int count = 6 + RANDOM.nextInt(6);
        Shard[] shards = new Shard[count];
        for (int i = 0; i < count; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2.0 * RANDOM.nextDouble() - 1.0);
            float sx = (float) (Math.sin(phi) * Math.cos(theta));
            float sy = (float) (Math.sin(phi) * Math.sin(theta));
            float sz = (float) (Math.cos(phi)) * 0.25F; // z 压低
            shards[i] = new Shard(sx, sy, sz,
                    0.4F + RANDOM.nextFloat() * 0.6F,               // 速度（模型空间单位/秒）
                    unit * (0.12F + RANDOM.nextFloat() * 0.18F),    // 大小（像素的 12%~30%）
                    RANDOM.nextFloat() * 360.0F,                    // 初始旋转
                    80.0F + RANDOM.nextFloat() * 160.0F);           // 旋转速度（度/秒）
        }
        return shards;
    }

    /** 发射爆裂专用碎片：数量更多、更大、飞散更快（爆裂动画更明显） */
    private static Shard[] makeFireShards(float unit) {
        int count = 10 + RANDOM.nextInt(8);
        Shard[] shards = new Shard[count];
        for (int i = 0; i < count; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2.0 * RANDOM.nextDouble() - 1.0);
            float sx = (float) (Math.sin(phi) * Math.cos(theta));
            float sy = (float) (Math.sin(phi) * Math.sin(theta));
            float sz = (float) (Math.cos(phi)) * 0.25F; // z 压低
            shards[i] = new Shard(sx, sy, sz,
                    0.8F + RANDOM.nextFloat() * 1.0F,               // 速度更快（0.8~1.8）
                    unit * (0.18F + RANDOM.nextFloat() * 0.22F),    // 大小更大（像素的 18%~40%）
                    RANDOM.nextFloat() * 360.0F,                    // 初始旋转
                    100.0F + RANDOM.nextFloat() * 200.0F);          // 旋转更快（度/秒）
        }
        return shards;
    }

    /**
     * 触发色块出现段缩放曲线（elastic ease-out，平滑插值）：
     * 0 ~ APPEAR_FRACTION 内从 1.0 弹性放大到 targetScale（常态 TRIGGER_SCALE，爆裂更大）。
     */
    private static float triggerScale(float t, float targetScale) {
        if (t <= 0.0F) return 1.0F;
        float amp = targetScale - 1.0F;
        float appear = Math.max(0.05F, Math.min(0.95F, DiexvSwordShaders.TRIGGER_APPEAR_FRACTION));
        float u = Math.min(1.0F, t / appear);
        double c4 = (2.0 * Math.PI) / 3.0;
        float e = (float) (Math.pow(2.0, -10.0 * u) * Math.sin((u * 10.0 - 0.75) * c4) + 1.0);
        return 1.0F + amp * (e / ELASTIC_PEAK);
    }

    private static float smoothstep(float x) {
        return x * x * (3.0F - 2.0F * x);
    }

    /** 整数种子 → 0..1 确定性伪随机（抖动用：同一 tick 内所有帧结果一致，暂停冻结） */
    private static float hash01(int seed) {
        seed ^= seed >>> 16;
        seed *= 0x45d9f3b;
        seed ^= seed >>> 16;
        seed *= 0x45d9f3b;
        seed ^= seed >>> 16;
        return (seed & 0xFFFF) / 65535.0F;
    }

    /** HSV → RGB（返回 0xRRGGBB） */
    private static int hsvToRgb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        int i = (int) (h * 6.0F);
        float f = h * 6.0F - i;
        float p = v * (1.0F - s);
        float q = v * (1.0F - f * s);
        float t = v * (1.0F - (1.0F - f) * s);
        switch (i % 6) {
            case 0: return rgb(v, t, p);
            case 1: return rgb(q, v, p);
            case 2: return rgb(p, v, t);
            case 3: return rgb(p, q, v);
            case 4: return rgb(t, p, v);
            default: return rgb(v, p, q);
        }
    }

    private static int rgb(float r, float g, float b) {
        return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
    }

    private static boolean colorEqual(int c1, int c2) {
        int tol = DiexvSwordShaders.MERGE_TOLERANCE;
        if (tol <= 0) return c1 == c2;
        return Math.abs((c1 & 0xFF) - (c2 & 0xFF)) <= tol
                && Math.abs(((c1 >> 8) & 0xFF) - ((c2 >> 8) & 0xFF)) <= tol
                && Math.abs(((c1 >> 16) & 0xFF) - ((c2 >> 16) & 0xFF)) <= tol;
    }

    /** 判断邻居是否"外部透明"（越界视为外部；透明且属于外部连通区域才算外部） */
    private static boolean isExternalTransparent(int j, int k, int w, int h, boolean[] opaque, boolean[] external) {
        if (j < 0 || j >= w || k < 0 || k >= h) return true;
        int idx = k * w + j;
        return !opaque[idx] && external[idx];
    }

    private static boolean isOpaque(int j, int k) {
        if (cachedOpaque == null) return false;
        if (j < 0 || j >= cachedW || k < 0 || k >= cachedH) return false;
        return cachedOpaque[k * cachedW + j];
    }

    /** 延迟构建（资源重载后自动重建） */
    private static long lastBuiltLogMs = 0L;

    private static void ensureBuilt(TextureAtlasSprite sprite) {
        if (cachedSprite == sprite && cachedVoxels != null && cachedBlocks != null) return;
        cachedSprite = sprite;
        try {
            build(sprite);
            buildFailed = false;
            if (cachedVoxels != null && !cachedVoxels.isEmpty()) {
                long now = System.currentTimeMillis();
                if (now - lastBuiltLogMs >= 5000L) { // 节流：资源重载反复重建时不刷屏
                    lastBuiltLogMs = now;
                    PotionEnchantMod.LOGGER.info("[DiexvSwordMesh] built {} pixels / {} merge blocks, shaderReady={}",
                            cachedVoxels.size(), cachedBlocks.size(), DiexvSwordShaders.isMeshReady());
                }
            }
        } catch (Exception e) {
            buildFailed = true;
            cachedVoxels = null;
            cachedBlocks = null;
            PotionEnchantMod.LOGGER.warn("[DiexvSwordMesh] Failed to build voxel mesh from {}", TEXTURE, e);
        }
    }

    private static List<Voxel> getVoxels() {
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TEXTURE);
            if (sprite != null) ensureBuilt(sprite);
        } catch (Exception ignored) {
        }
        return cachedVoxels;
    }

    private static List<MergeBlock> getBlocks() {
        return cachedBlocks;
    }

    /**
     * 扫描贴图：非透明像素 → 单像素体素；8 连通（含对角）且颜色相同的像素合并为模型整体。
     * 合并块若为实心（AABB 内无非本块像素）则渲染为单个大块，否则回退逐像素。
     */
    private static void build(TextureAtlasSprite sprite) {
        NativeImage img = sprite.contents().getOriginalImage();
        int w = img.getWidth();
        int h = img.getHeight();
        List<Voxel> voxels = new ArrayList<>();
        List<MergeBlock> blocks = new ArrayList<>();
        if (w <= 0 || h <= 0) {
            cachedVoxels = voxels;
            cachedBlocks = blocks;
            cachedOpaque = new boolean[0];
            cachedW = w;
            cachedH = h;
            return;
        }

        boolean[] opaque = new boolean[w * h];
        for (int k = 0; k < h; k++) {
            for (int j = 0; j < w; j++) {
                if (FastColor.ABGR32.alpha(img.getPixelRGBA(j, k)) >= ALPHA_THRESHOLD) {
                    opaque[k * w + j] = true;
                }
            }
        }
        cachedOpaque = opaque;
        cachedW = w;
        cachedH = h;
        // 模型中心（像素包围盒中心，y 含翻转）
        int gMinJ = w, gMaxJ = -1, gMinK = h, gMaxK = -1;
        for (int kk = 0; kk < h; kk++) {
            for (int jj = 0; jj < w; jj++) {
                if (opaque[kk * w + jj]) {
                    gMinJ = Math.min(gMinJ, jj);
                    gMaxJ = Math.max(gMaxJ, jj);
                    gMinK = Math.min(gMinK, kk);
                    gMaxK = Math.max(gMaxK, kk);
                }
            }
        }
        if (gMaxJ >= gMinJ && gMaxK >= gMinK) {
            cachedCenterX = (gMinJ + gMaxJ + 1) / 2.0F / w;
            cachedCenterY = 1.0F - (gMinK + gMaxK + 1) / 2.0F / h;
        } else {
            cachedCenterX = 0.5F;
            cachedCenterY = 0.5F;
        }

        // 洪水填充：标记从贴图边缘可达的"外部透明"区域（内部镂空/孔洞不可达）
        boolean[] external = new boolean[w * h];
        ArrayDeque<int[]> extQueue = new ArrayDeque<>();
        for (int j = 0; j < w; j++) {
            if (!opaque[j]) { external[j] = true; extQueue.add(new int[]{j, 0}); }
            if (!opaque[(h - 1) * w + j]) { external[(h - 1) * w + j] = true; extQueue.add(new int[]{j, h - 1}); }
        }
        for (int k = 0; k < h; k++) {
            if (!opaque[k * w]) { external[k * w] = true; extQueue.add(new int[]{0, k}); }
            if (!opaque[k * w + w - 1]) { external[k * w + w - 1] = true; extQueue.add(new int[]{w - 1, k}); }
        }
        while (!extQueue.isEmpty()) {
            int[] cur = extQueue.poll();
            int cj = cur[0], ck = cur[1];
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if ((di == 0) == (dj == 0)) continue;
                    int nj = cj + dj, nk = ck + di;
                    if (nj < 0 || nj >= w || nk < 0 || nk >= h) continue;
                    int nIdx = nk * w + nj;
                    if (opaque[nIdx] || external[nIdx]) continue;
                    external[nIdx] = true;
                    extQueue.add(new int[]{nj, nk});
                }
            }
        }

        boolean[] visited = new boolean[w * h];
        for (int k = 0; k < h; k++) {
            for (int j = 0; j < w; j++) {
                if (!opaque[k * w + j]) continue;
                int idx = k * w + j;
                if (visited[idx]) continue;
                visited[idx] = true;
                int baseColor = img.getPixelRGBA(j, k) & 0xFFFFFF;

                // BFS 8 连通同色
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{j, k});
                List<Integer> pixels = new ArrayList<>();
                int minJ = j, maxJ = j, minK = k, maxK = k;
                while (!queue.isEmpty()) {
                    int[] cur = queue.poll();
                    int cj = cur[0], ck = cur[1];
                    pixels.add(voxels.size());
                    voxels.add(new Voxel(cj, ck,
                            pixelX0(cj, w), pixelY0(ck, h), pixelX1(cj, w), pixelY1(ck, h)));
                    minJ = Math.min(minJ, cj);
                    maxJ = Math.max(maxJ, cj);
                    minK = Math.min(minK, ck);
                    maxK = Math.max(maxK, ck);
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            if (di == 0 && dj == 0) continue;
                            int nj = cj + dj, nk = ck + di;
                            if (nj < 0 || nj >= w || nk < 0 || nk >= h) continue;
                            int nIdx = nk * w + nj;
                            if (visited[nIdx] || !opaque[nIdx]) continue;
                            int np = img.getPixelRGBA(nj, nk);
                            if (!colorEqual(baseColor, np & 0xFFFFFF)) continue;
                            visited[nIdx] = true;
                            queue.add(new int[]{nj, nk});
                        }
                    }
                }

                // 实心检查：AABB 内不允许存在本块之外的任何像素（异色或透明孔洞）
                boolean solid = true;
                outer:
                for (int kk = minK; kk <= maxK; kk++) {
                    for (int jj = minJ; jj <= maxJ; jj++) {
                        if (!opaque[kk * w + jj]) {
                            solid = false;
                            break outer;
                        }
                        if (!visited[kk * w + jj]
                                || !colorEqual(baseColor, img.getPixelRGBA(jj, kk) & 0xFFFFFF)) {
                            solid = false;
                            break outer;
                        }
                    }
                }

                blocks.add(new MergeBlock(
                        pixelX0(minJ, w), pixelY0(minK, h), pixelX1(maxJ, w), pixelY1(maxK, h),
                        minJ, maxJ, minK, maxK, pixels, solid));
            }
        }
        cachedVoxels = voxels;
        cachedBlocks = blocks;
    }

    private static float pixelX0(int j, int w) {
        return DiexvSwordShaders.FLIP_X ? 1.0F - (j + 1) / (float) w : j / (float) w;
    }

    private static float pixelX1(int j, int w) {
        return DiexvSwordShaders.FLIP_X ? 1.0F - j / (float) w : (j + 1) / (float) w;
    }

    private static float pixelY0(int k, int h) {
        float y = 1.0F - (k + 1) / (float) h;
        return DiexvSwordShaders.FLIP_Y ? 1.0F - y - 1.0F / (float) h : y;
    }

    private static float pixelY1(int k, int h) {
        float y = 1.0F - k / (float) h;
        return DiexvSwordShaders.FLIP_Y ? 1.0F - y + 1.0F / (float) h : y;
    }

    /** 坐标变换：原始 0..1 坐标 → 以中心 c 为原点的弹性缩放 */
    private static float scaleCoord(float v, float c, float scale) {
        return c + (v - c) * scale;
    }

    /**
     * 渲染一个单元（单像素或合并块）的外表面：所有面按像素段细分，
     * 紧贴邻居的共享内部面段跳过（透明模式下避免内部面叠加）。
     */
    private static void renderUnit(Matrix4f matrix, VertexConsumer b,
                                   float vx0, float vy0, float vx1, float vy1,
                                   int minJ, int maxJ, int minK, int maxK,
                                   float thickness, float scale,
                                   float r, float g, float bl, float a) {
        if (cachedOpaque == null || cachedW <= 0 || cachedH <= 0) return;
        float cx = (vx0 + vx1) * 0.5F;
        float cy = (vy0 + vy1) * 0.5F;
        float unit = Math.min(vx1 - vx0, vy1 - vy0);
        float zc = 0.5F;
        float halfT = unit * thickness * 0.5F * scale;
        float z0 = zc - halfT;
        float z1 = zc + halfT;

        // DOWN 面（y0 = 贴图底部侧 maxK 侧）：按 x 像素段，下方(maxk+1)有邻居则跳过
        for (int jj = minJ; jj <= maxJ; jj++) {
            if (isOpaque(jj, maxK + 1)) continue;
            float sx0 = scaleCoord(pixelX0(jj, cachedW), cx, scale);
            float sx1 = scaleCoord(pixelX1(jj, cachedW), cx, scale);
            float sy0 = scaleCoord(vy0, cy, scale);
            b.vertex(matrix, sx0, sy0, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx1, sy0, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx1, sy0, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx0, sy0, z1).color(r, g, bl, a).endVertex();
        }
        // UP 面（y1 = 贴图顶部侧 minK 侧）：按 x 像素段，上方(mink-1)有邻居则跳过
        for (int jj = minJ; jj <= maxJ; jj++) {
            if (isOpaque(jj, minK - 1)) continue;
            float sx0 = scaleCoord(pixelX0(jj, cachedW), cx, scale);
            float sx1 = scaleCoord(pixelX1(jj, cachedW), cx, scale);
            float sy1 = scaleCoord(vy1, cy, scale);
            b.vertex(matrix, sx0, sy1, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx0, sy1, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx1, sy1, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx1, sy1, z0).color(r, g, bl, a).endVertex();
        }
        // WEST 面（x0）：按 y 像素段，左方(minj-1)有邻居则跳过
        for (int kk = minK; kk <= maxK; kk++) {
            if (isOpaque(minJ - 1, kk)) continue;
            float sy0 = scaleCoord(pixelY0(kk, cachedH), cy, scale);
            float sy1 = scaleCoord(pixelY1(kk, cachedH), cy, scale);
            float sx0 = scaleCoord(vx0, cx, scale);
            b.vertex(matrix, sx0, sy0, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx0, sy0, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx0, sy1, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx0, sy1, z0).color(r, g, bl, a).endVertex();
        }
        // EAST 面（x1）：按 y 像素段，右方(maxj+1)有邻居则跳过
        for (int kk = minK; kk <= maxK; kk++) {
            if (isOpaque(maxJ + 1, kk)) continue;
            float sy0 = scaleCoord(pixelY0(kk, cachedH), cy, scale);
            float sy1 = scaleCoord(pixelY1(kk, cachedH), cy, scale);
            float sx1 = scaleCoord(vx1, cx, scale);
            b.vertex(matrix, sx1, sy0, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx1, sy1, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx1, sy1, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, sx1, sy0, z1).color(r, g, bl, a).endVertex();
        }
        // NORTH 面（z0）：整面
        {
            float fx0 = scaleCoord(vx0, cx, scale), fx1 = scaleCoord(vx1, cx, scale);
            float fy0 = scaleCoord(vy0, cy, scale), fy1 = scaleCoord(vy1, cy, scale);
            b.vertex(matrix, fx0, fy0, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, fx0, fy1, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, fx1, fy1, z0).color(r, g, bl, a).endVertex();
            b.vertex(matrix, fx1, fy0, z0).color(r, g, bl, a).endVertex();
        }
        // SOUTH 面（z1）：整面
        {
            float fx0 = scaleCoord(vx0, cx, scale), fx1 = scaleCoord(vx1, cx, scale);
            float fy0 = scaleCoord(vy0, cy, scale), fy1 = scaleCoord(vy1, cy, scale);
            b.vertex(matrix, fx0, fy0, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, fx1, fy0, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, fx1, fy1, z1).color(r, g, bl, a).endVertex();
            b.vertex(matrix, fx0, fy1, z1).color(r, g, bl, a).endVertex();
        }
    }

    /** 单像素体素：像素坐标 + 0..1 模型空间矩形 */
    private record Voxel(int j, int k, float x0, float y0, float x1, float y1) {}

    /** 合并块：8 连通同色像素的整体。solid=true 渲染为单个大块，否则逐像素 */
    private record MergeBlock(float x0, float y0, float x1, float y1,
                              int minJ, int maxJ, int minK, int maxK,
                              List<Integer> pixels, boolean solid) {}

    /** 触发数据：开始时间(连续tick) + 动画时长(tick) + 爆开碎片 + 触发色(蓝白系随机) + 弹性放大峰值 + 是否播重生段 */
    private record TriggerData(float start, float dur, Shard[] shards, float r, float g, float b, float scale, boolean reborn) {}

    /** 爆开碎片：方向(未归一化亦可) + 速度(模型空间单位/秒) + 大小 + 旋转 */
    private record Shard(float dirX, float dirY, float dirZ, float speed, float size,
                         float rot, float rotSpeed) {}

    private DiexvSwordVoxelMesh() {}
}
