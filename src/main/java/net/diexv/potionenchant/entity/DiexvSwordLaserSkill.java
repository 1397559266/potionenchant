package net.diexv.potionenchant.entity;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.network.DiexvSwordLaserNetwork;
import net.diexv.potionenchant.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DiexvSword（DiexvSwordItem）右键激光技能（移植自 DiexvCreeperLaserSkill，玩家操作版）。
 *
 * - 右键按住蓄力 20 tick（1 秒），蓄力时长一到自动发射（无需松开）。
 * - 发射方向为玩家十字准星所指的精确方向（getLookAngle）。
 * - 光柱：多束并存、无限射程、按 9 格/tick 推进、30 格长度尾段、命中半径 2.5。
 * - 伤害：直接命中 1000，命中后 3 秒（60 tick）DoT 每 tick 10 点。
 * - 音效：蓄力 ray_os、发射 ray（随机 1~6）、飞行 ray_skylance（96 格内玩家）。
 * - 冷却 3 秒（60 tick），从开始蓄力起算。
 * - 光束数据通过 DiexvSwordLaserNetwork 同步给客户端渲染光柱/冲击环。
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DiexvSwordLaserSkill {

    private static final int LASER_COOLDOWN = 20;          // 1 秒冷却（发射后恢复期），与客户端蓄力冷却动画一致
    private static final int LASER_WINDUP = 20;            // 蓄力（前摇）时长（tick），1 秒整
    private static final double BEAM_SPEED_BPS = 180.0;    // 光柱飞行速度（格/秒）= 9 格/tick
    public static final double BEAM_SPEED = BEAM_SPEED_BPS / 20.0;
    private static final double BEAM_MAX_PROGRESS = 100000.0;
    private static final int HIT_DOT_TICKS = 60;           // 命中后持续伤害时长（3 秒）
    private static final float DIRECT_HIT_DAMAGE = 1000.0F; // 激光直接命中伤害
    private static final float DOT_DAMAGE = 10.0F;         // DoT 每 tick 伤害
    private static final double FLY_SOUND_RANGE = 96.0;
    public static final double BEAM_LENGTH = 30.0;
    public static final double BEAM_HIT_RADIUS = 2.5;

    /** 蓄力剩余 tick（玩家 UUID -> 剩余）；值存在即表示蓄力中 */
    private static final Map<UUID, Integer> WINDUP_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN_TICKS = new HashMap<>();
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static final Map<UUID, Integer> DOT_TICKS = new HashMap<>();

    private DiexvSwordLaserSkill() {}

    /** 客户端查询：本地玩家是否正在蓄力（用于预瞄光柱渲染） */
    public static boolean isCharging(Player player) {
        if (player == null) return false;
        return player.isUsingItem() && player.getUseItem().getItem() == ModItems.DIEXV_SWORD.get();
    }

    /** 取消蓄力（松开右键/切换物品时调用，不发射） */
    public static void cancelCharge(Player player) {
        if (player == null || player.level().isClientSide) return;
        WINDUP_TICKS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        UUID id = player.getUUID();

        // 冷却倒计时
        if (COOLDOWN_TICKS.containsKey(id)) {
            int left = COOLDOWN_TICKS.get(id) - 1;
            if (left <= 0) {
                COOLDOWN_TICKS.remove(id);
                // 冷却结束：通知客户端剑恢复完整可见（与服务端冷却同步）
                DiexvSwordLaserNetwork.broadcastChargeState(DiexvSwordLaserNetwork.SyncLaserChargePacket.RESTORE, id);
            } else {
                COOLDOWN_TICKS.put(id, left);
            }
        }

        boolean charging = WINDUP_TICKS.containsKey(id);
        boolean usingSword = player.isUsingItem() && player.getUseItem().getItem() == ModItems.DIEXV_SWORD.get();

        if (charging && !usingSword) {
            // 松开右键 / 切换物品：取消蓄力（不发射）
            WINDUP_TICKS.remove(id);
        } else if (!charging && usingSword && !COOLDOWN_TICKS.containsKey(id)) {
            // 开始蓄力：通知客户端开始蓄力动画（与服务端同步）
            WINDUP_TICKS.put(id, LASER_WINDUP);
            DiexvSwordLaserNetwork.broadcastChargeState(DiexvSwordLaserNetwork.SyncLaserChargePacket.CHARGE_START, id);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.DIEXV_CREEPER_RAY_OS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        if (WINDUP_TICKS.containsKey(id)) {
            int left = WINDUP_TICKS.get(id) - 1;
            if (left <= 0) {
                WINDUP_TICKS.remove(id);
                COOLDOWN_TICKS.put(id, LASER_COOLDOWN);
                fireBeam(player);
            } else {
                WINDUP_TICKS.put(id, left);
            }
        }

        tickBeams();
        tickDotDamage();
    }

    /** 发射激光：方向 = 玩家十字准星精确朝向 */
    private static void fireBeam(Player player) {
        Vec3 look = player.getLookAngle().normalize();
        // 发射起点：眼睛位置 + 枪口偏移（与渲染一致）
        Vec3 eyePos = player.getEyePosition();
        double yawRad = Math.toRadians(player.getYRot());
        Vec3 origin = new Vec3(eyePos.x - Math.sin(yawRad) * 0.35,
                eyePos.y - 0.15,
                eyePos.z + Math.cos(yawRad) * 0.35);

        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        BEAMS.add(new Beam(serverLevel.dimension(), player.getUUID(), origin, look));

        // 通知客户端：激光发射瞬间 → 剑全剑爆裂碎开 + 冷却开始（动画与激光精确同步）
        DiexvSwordLaserNetwork.broadcastChargeState(DiexvSwordLaserNetwork.SyncLaserChargePacket.FIRE, player.getUUID());

        // 发射音效：ray1~6 随机（sounds.json 多个音源由游戏随机挑选），音量 2 倍
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.DIEXV_CREEPER_RAY.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        // 飞行音效：对 96 格（6 区块）内玩家各播放一次
        double rangeSq = FLY_SOUND_RANGE * FLY_SOUND_RANGE;
        for (ServerPlayer p : serverLevel.players()) {
            if (distanceToRaySq(p.getX(), p.getY(), p.getZ(), origin, look) <= rangeSq) {
                p.connection.send(new ClientboundSoundPacket(
                        Holder.direct(ModSounds.DIEXV_CREEPER_RAY_SKYLANCE.get()),
                        SoundSource.PLAYERS, origin.x, origin.y, origin.z, 1.0F, 1.0F,
                        p.getRandom().nextLong()));
            }
        }

        syncBeams();
    }

    private static void tickBeams() {
        if (BEAMS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Iterator<Beam> it = BEAMS.iterator();
        while (it.hasNext()) {
            Beam beam = it.next();
            ServerLevel level = server.getLevel(beam.worldKey);
            if (level == null) {
                it.remove();
                continue;
            }
            beam.progress += BEAM_SPEED;
            if (beam.progress > BEAM_MAX_PROGRESS) {
                it.remove();
                continue;
            }
            Vec3 head = beam.origin.add(beam.dir.scale(beam.progress));
            if (level.isLoaded(BlockPos.containing(head))) {
                applyBeamHits(level, beam);
            }
        }
        syncBeams();
    }

    private static void applyBeamHits(ServerLevel level, Beam beam) {
        double tailDist = Math.max(0.0, beam.progress - BEAM_LENGTH);
        double segLen = beam.progress - tailDist;
        if (segLen <= 0.0) return;
        Vec3 tail = beam.origin.add(beam.dir.scale(tailDist));
        Vec3 head = beam.origin.add(beam.dir.scale(beam.progress));
        AABB box = new AABB(tail, head).inflate(BEAM_HIT_RADIUS);

        Player owner = level.getPlayerByUUID(beam.ownerId);
        if (owner == null) return;

        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != owner && !(e instanceof Player) && e.isAlive());

        for (LivingEntity hit : hits) {
            if (beam.hit.contains(hit.getUUID())) continue;
            if (!isOnBeam(tail, beam.dir, segLen, hit)) continue;
            beam.hit.add(hit.getUUID());
            // 激光命中目标：标记为归零目标（SynchedData 读写/血量全部归 0，持续瘫痪）
            net.diexv.potionenchant.util.DiexvSwordTargetZeroManager.mark(hit);
            // 直接命中伤害
            hit.invulnerableTime = 0;
            hit.hurt(hit.damageSources().playerAttack(owner), DIRECT_HIT_DAMAGE);
            hit.invulnerableTime = 0;
            // 命中后标记：60 tick 持续伤害
            DOT_TICKS.put(hit.getUUID(), HIT_DOT_TICKS);
        }
    }

    private static void tickDotDamage() {
        if (DOT_TICKS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Iterator<Map.Entry<UUID, Integer>> it = DOT_TICKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            Entity target = null;
            for (ServerLevel level : server.getAllLevels()) {
                target = level.getEntity(entry.getKey());
                if (target != null) break;
            }
            if (!(target instanceof LivingEntity living) || !living.isAlive()) {
                it.remove();
                continue;
            }
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(remaining);
            living.invulnerableTime = 0;
            Player owner = findOwnerOf(living, living.level().dimension());
            living.hurt(owner != null ? living.damageSources().playerAttack(owner)
                    : living.damageSources().generic(), DOT_DAMAGE);
            living.invulnerableTime = 0;
        }
    }

    /** 找到对某目标造成过直接命中的光束的 owner（用于 DoT 伤害源） */
    private static Player findOwnerOf(LivingEntity target, ResourceKey<Level> dim) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        ServerLevel level = server.getLevel(dim);
        if (level == null) return null;
        for (Beam beam : BEAMS) {
            if (beam.worldKey == dim && beam.hit.contains(target.getUUID())) {
                return level.getPlayerByUUID(beam.ownerId);
            }
        }
        return null;
    }

    private static void syncBeams() {
        List<LaserBeamData> sync = new ArrayList<>(BEAMS.size());
        for (Beam b : BEAMS) {
            sync.add(new LaserBeamData((float) b.progress,
                    (float) b.origin.x, (float) b.origin.y, (float) b.origin.z,
                    (float) b.dir.x, (float) b.dir.y, (float) b.dir.z));
        }
        DiexvSwordLaserNetwork.broadcastBeams(sync);
    }

    private static double distanceToRaySq(double px, double py, double pz, Vec3 origin, Vec3 dir) {
        double dx = px - origin.x, dy = py - origin.y, dz = pz - origin.z;
        double t = Math.max(0.0, dx * dir.x + dy * dir.y + dz * dir.z);
        double ox = px - (origin.x + dir.x * t);
        double oy = py - (origin.y + dir.y * t);
        double oz = pz - (origin.z + dir.z * t);
        return ox * ox + oy * oy + oz * oz;
    }

    private static boolean isOnBeam(Vec3 origin, Vec3 dir, double length, LivingEntity entity) {
        AABB box = entity.getBoundingBox().inflate(BEAM_HIT_RADIUS);
        Vec3 inv = new Vec3(dir.x == 0 ? Double.MAX_VALUE : 1.0 / dir.x,
                dir.y == 0 ? Double.MAX_VALUE : 1.0 / dir.y,
                dir.z == 0 ? Double.MAX_VALUE : 1.0 / dir.z);

        double t1 = (box.minX - origin.x) * inv.x;
        double t2 = (box.maxX - origin.x) * inv.x;
        double t3 = (box.minY - origin.y) * inv.y;
        double t4 = (box.maxY - origin.y) * inv.y;
        double t5 = (box.minZ - origin.z) * inv.z;
        double t6 = (box.maxZ - origin.z) * inv.z;

        double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        return tmax >= 0 && tmin <= tmax && tmin <= length;
    }

    private static final class Beam {
        final ResourceKey<Level> worldKey;
        final UUID ownerId;
        final Vec3 origin;
        final Vec3 dir;
        final Set<UUID> hit = new HashSet<>();
        double progress = 0.0;

        Beam(ResourceKey<Level> worldKey, UUID ownerId, Vec3 origin, Vec3 dir) {
            this.worldKey = worldKey;
            this.ownerId = ownerId;
            this.origin = origin;
            this.dir = dir;
        }
    }
}
