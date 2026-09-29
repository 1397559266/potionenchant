package net.diexv.potionenchant.SkyRender.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.api.client.shader.CCShaderInstance;
import net.diexv.potionenchant.SkyRender.api.client.shader.CCUniform;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.util.Objects;

/**
 * DiexvSword（diexv_sword 物品）的着色器集合：
 * - 体素多面体专用动态着色器（diexvsword_mesh）
 * - mask 流光着色器（sword_cosmic，移植自源项目 diexvdream:cosmic —— 天空代码雨）
 */
@OnlyIn(Dist.CLIENT)
public final class DiexvSwordShaders {

    // ========== 视觉控制参数（运行时修改立即生效） ==========
    public static boolean VOXEL_ENABLED = true;
    public static boolean VOXEL_GUI_ENABLED = true;
    /** 体素厚度倍数：1.0 = 原版 json 模型的厚度（1/16 单位） */
    public static float Z_THICKNESS = 1.0F;
    /** 体素缝隙（相对像素单元）：0 = 与原模型一样紧密相连 */
    public static float GAP = 0.0F;
    /** 方向修正：若体素左右/上下颠倒可切换 */
    public static boolean FLIP_X = false;
    public static boolean FLIP_Y = false;
    public static float OPACITY = 1.0F;
    public static int TINT_MODE = 0;
    public static float TINT_R = 1.0F;
    public static float TINT_G = 1.0F;
    public static float TINT_B = 1.0F;
    public static int ANIM_MODE = 0;
    public static float ANIM_SPEED = 1.0F;
    /** ===== DiexvSword 特效参数 ===== */
    /** 基础透明度（剑整体保持 42%） */
    public static float BASE_ALPHA = 0.42F;
    /** 触发色块透明度（浅蓝色块 67%） */
    public static float TRIGGER_ALPHA = 0.67F;
    /** 触发频率（严格按时间窗口计算）：每 TRIGGER_WINDOW_MS 毫秒内最多触发 TRIGGER_MAX_PER_WINDOW 个新色块 */
    public static long TRIGGER_WINDOW_MS = 100L;
    public static int TRIGGER_MAX_PER_WINDOW = 1;
    /** 动画周期随机范围（毫秒）：每次触发随机 0.5 ~ 2 秒 */
    public static long TRIGGER_DURATION_MIN_MS = 500L;
    public static long TRIGGER_DURATION_MAX_MS = 2000L;
    /** 触发色块弹性放大倍数 */
    public static float TRIGGER_SCALE = 1.5F;
    /** 出现段占动画比例（0~1）：前段弹性放大 */
    public static float TRIGGER_APPEAR_FRACTION = 0.35F;
    /** 爆开段结束比例（出现段 → 爆开段 → 重生段 三段划分，默认 0.7） */
    public static float TRIGGER_BURST_FRACTION = 0.7F;
    /** 浅彩虹色相循环速度（每秒循环比例，0.5 = 约 2 秒一圈，加快蓝白渐变） */
    public static float HUE_SPEED = 0.5F;
    /** 合并容差：相邻像素（含对角）RGB 每通道差值不超过该值视为同色合并（0 = 严格相同） */
    public static int MERGE_TOLERANCE = 12;
    /** 触发色块颜色（浅蓝白） */
    public static float TRIGGER_R = 0.75F;
    public static float TRIGGER_G = 0.85F;
    public static float TRIGGER_B = 1.0F;

    // ========== 体素着色器与 uniform ==========
    public static CCShaderInstance diexvSwordMeshShader;
    public static CCUniform meshTime;
    public static CCUniform meshOpacity;
    public static CCUniform meshTint;
    public static CCUniform meshTintMode;
    public static CCUniform meshAnimMode;
    public static CCUniform meshAnimSpeed;

    // ========== 剑的 mask 流光着色器（移植自源项目 diexvdream:cosmic —— 天空代码雨） ==========
    public static CCShaderInstance swordCosmicShader;
    public static CCUniform swordCosmicTime;
    public static CCUniform swordCosmicYaw;
    public static CCUniform swordCosmicPitch;
    public static CCUniform swordCosmicExternalScale;
    public static CCUniform swordCosmicOpacity;
    public static CCUniform swordCosmicUvs;

    /** RenderStateShard 内部字段访问器 */
    private static class RenderStateShardAccess extends RenderStateShard {
        private static final DepthTestStateShard LEQUAL_DEPTH_TEST = RenderStateShard.LEQUAL_DEPTH_TEST;
        private static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = RenderStateShard.TRANSLUCENT_TRANSPARENCY;
        private static final CullStateShard CULL = RenderStateShard.CULL;
        private static final CullStateShard NO_CULL = RenderStateShard.NO_CULL;
        private static final EmptyTextureStateShard NO_TEXTURE = RenderStateShard.NO_TEXTURE;
        private static final WriteMaskStateShard COLOR_WRITE = RenderStateShard.COLOR_WRITE;
        private static final LightmapStateShard LIGHT_MAP = RenderStateShard.LIGHTMAP;
        private static final TextureStateShard BLOCK_SHEET_MIPPED = RenderStateShard.BLOCK_SHEET_MIPPED;
        private static final OutputStateShard MAIN_TARGET = RenderStateShard.MAIN_TARGET;
        private static final LayeringStateShard SHADER_LAYER_DEPTH_BIAS = new LayeringStateShard("potionenchant_shader_layer_depth_bias", () -> {
            RenderSystem.polygonOffset(-1.0F, -32.0F);
            RenderSystem.enablePolygonOffset();
        }, () -> {
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
        });

        private RenderStateShardAccess(String pName, Runnable pSetupState, Runnable pClearState) {
            super(pName, pSetupState, pClearState);
        }
    }

    /** 模型本体 RenderType */
    public static final RenderType DIEXVSWORD_MESH_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":diexvsword_mesh",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> diexvSwordMeshShader))
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderStateShardAccess.NO_TEXTURE)
                    .setCullState(RenderStateShardAccess.CULL)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(false));

    /** 剑 mask 普通渲染（无光影 / GUI） */
    public static final RenderType SWORD_COSMIC_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":sword_cosmic",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> swordCosmicShader))
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
                    .setCullState(RenderStateShardAccess.NO_CULL)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(true));

    /** 剑 mask 延迟渲染（光影兼容，非手部物品） */
    public static final RenderType SWORD_COSMIC_ITEM_AFTER_LEVEL_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":sword_cosmic_item_after_level",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> swordCosmicShader))
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
                    .setLayeringState(RenderStateShardAccess.SHADER_LAYER_DEPTH_BIAS)
                    .setOutputState(RenderStateShardAccess.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(true));

    /** 剑 mask 延迟渲染（光影兼容，第一人称手部物品） */
    public static final RenderType SWORD_COSMIC_HAND_AFTER_LEVEL_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":sword_cosmic_hand_after_level",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> swordCosmicShader))
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
                    .setLayeringState(RenderStateShardAccess.SHADER_LAYER_DEPTH_BIAS)
                    .setOutputState(RenderStateShardAccess.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(true));

    public static void onRegisterShaders(RegisterShadersEvent event) {
        event.registerShader(CCShaderInstance.create(event.getResourceProvider(), new ResourceLocation(PotionEnchantMod.MODID, "diexvsword_mesh"), DefaultVertexFormat.POSITION_COLOR), e -> {
            diexvSwordMeshShader = (CCShaderInstance) e;
            meshTime = Objects.requireNonNull(diexvSwordMeshShader.getUniform("time"));
            meshOpacity = Objects.requireNonNull(diexvSwordMeshShader.getUniform("opacity"));
            meshTint = Objects.requireNonNull(diexvSwordMeshShader.getUniform("tint"));
            meshTintMode = Objects.requireNonNull(diexvSwordMeshShader.getUniform("tintMode"));
            meshAnimMode = Objects.requireNonNull(diexvSwordMeshShader.getUniform("animMode"));
            meshAnimSpeed = Objects.requireNonNull(diexvSwordMeshShader.getUniform("animSpeed"));

            meshTime.set(AvaritiaShaders.cosmicTimeSeconds());
            diexvSwordMeshShader.onApply(() -> {
                // 统一墙钟时间基准：每秒 1 单位（暂停也继续走，与其余着色器/特效同一个时钟）
                meshTime.set(AvaritiaShaders.cosmicTimeSeconds());
                meshOpacity.set(OPACITY);
                meshTint.set(TINT_R, TINT_G, TINT_B);
                meshTintMode.set(TINT_MODE);
                meshAnimMode.set(ANIM_MODE);
                meshAnimSpeed.set(ANIM_SPEED);
            });
        });

        // 剑的 mask 流光（源项目 cosmic 天空代码雨）
        event.registerShader(CCShaderInstance.create(event.getResourceProvider(),
                new ResourceLocation(PotionEnchantMod.MODID, "sword_cosmic"),
                DefaultVertexFormat.BLOCK), e -> {
            swordCosmicShader = (CCShaderInstance) e;
            swordCosmicTime = Objects.requireNonNull(swordCosmicShader.getUniform("time"));
            swordCosmicYaw = Objects.requireNonNull(swordCosmicShader.getUniform("yaw"));
            swordCosmicPitch = Objects.requireNonNull(swordCosmicShader.getUniform("pitch"));
            swordCosmicExternalScale = Objects.requireNonNull(swordCosmicShader.getUniform("externalScale"));
            swordCosmicOpacity = Objects.requireNonNull(swordCosmicShader.getUniform("opacity"));
            swordCosmicUvs = swordCosmicShader.getUniform("cosmicuvs"); // 源 cosmic.fsh 未使用，可为 null

            // 与源项目 AvaritiaShaders 一致：onApply 只刷 time（不覆盖 externalScale/opacity，
            // GUI 的 scale=100 由 upload 设置并保持）；时间统一走墙钟 tick 等价单位
            swordCosmicShader.onApply(() -> swordCosmicTime.set(AvaritiaShaders.cosmicTimeTicks()));
        });
    }

    public static boolean isMeshReady() {
        return diexvSwordMeshShader != null
                && meshTime != null && meshOpacity != null && meshTint != null
                && meshTintMode != null && meshAnimMode != null && meshAnimSpeed != null
                ;
    }

    public static boolean isSwordCosmicReady() {
        return swordCosmicShader != null
                && swordCosmicTime != null && swordCosmicYaw != null && swordCosmicPitch != null
                && swordCosmicExternalScale != null && swordCosmicOpacity != null;
    }

    /** 上传剑 cosmic uniform（时间基准：统一墙钟 tick 等价单位，与其余着色器/特效同一个时钟）：
     *  yaw/pitch 随玩家视角，GUI 固定视角 + 缩小星体。 */
    public static void uploadSwordCosmicUniforms(boolean gui, boolean lateRender) {
        if (!isSwordCosmicReady()) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        float yaw = 0.0F;
        float pitch = 0.0F;
        if (!gui && mc.player != null) {
            yaw = (float) (mc.player.getYRot() * Math.PI / 180.0F);
            pitch = -(float) (mc.player.getXRot() * Math.PI / 180.0F);
        }
        float scale = gui ? 100.0F : 1.0F;
        swordCosmicTime.set(AvaritiaShaders.cosmicTimeTicks());
        swordCosmicYaw.set(yaw);
        swordCosmicPitch.set(pitch);
        swordCosmicExternalScale.set(scale);
        swordCosmicOpacity.set(1.0F);
    }

    private DiexvSwordShaders() {}
}
