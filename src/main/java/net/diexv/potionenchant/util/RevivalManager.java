package net.diexv.potionenchant.util;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.agent.DiexvBase;
import net.diexv.potionenchant.agent.DiexvSwordAgent;
import net.diexv.potionenchant.agent.RevivalHook;
import net.diexv.potionenchant.event.ArmorXFeatureHandler;
import net.diexv.potionenchant.mixin.accessor.LivingEntityAccessor;
import net.diexv.potionenchant.mixin.accessor.SynchedEntityDataAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 复活药水：真死亡 + 真重生（agent 字节码注入，不使用 Forge 事件 / Mixin）。
 *
 * 设计要点（对应"改血级 / 删除级秒杀"两类绕过方式）：
 *  1. **侧路状态注册表**：复活状态不再只依赖玩家实体上的药水效果。
 *     agent 把 RevivalHook.onPlayerTick 注入 ServerPlayer#tick 头部，每 tick 把
 *     "谁拥有复活效果、几级、剩余多久"写进并发注册表；死亡瞬间即使效果被清
 *     （例如不死图腾会调用 removeAllEffects），注册表里仍然记得。
 *     另有一个后台守护线程只读该注册表做超时/离线清理——它不触碰任何游戏对象。
 *  2. **三条接管路径**（任一命中即走真重生）：
 *     - ServerPlayer#die 头部            ：伤害致死
 *     - SynchedEntityData#set 血量写入处 ：ASM 改血级秒杀（setHealth(0)/直接写 DATA_HEALTH_ID）
 *     - Player#remove(KILLED) 头部       ：/kill 等删除级秒杀（Entity.kill() 直接删实体，不经过 hurt/die）
 *  3. **真重生**：PlayerList#respawn(原版新对象 + 原版重生包) → Entity#load(NBT 语义恢复)
 *     → Unsafe 搬运 NBT 覆盖不到的瞬态字段（abilities/itemCooldowns/useItem/attackStrengthTicker 等）
 *     → 补发客户端同步。
 *  4. **兜底**：tickDeath 阶段若发现重生任务卡住，原地恢复血量，保证玩家一定活下来。
 */
public final class RevivalManager {

    private static final Logger LOGGER = LogManager.getLogger("potionenchant/Revival");
    /**
     * 复活冷却：只用于挡住"同一次死亡流程里的重复触发"和"我们自己的 respawn 删除旧玩家"，
     * 不能设大——Boss 可能在 2 秒内二次击杀（实测 60t 会挡住第二次复活导致真死）。
     */
    private static final long COOLDOWN_TICKS = 10L;
    /** 效果消失后的兜底保留窗口：防"被剥离护甲/被清效果后立刻死亡"（Boss「彻底遗忘」类能力） */
    private static final long GRACE_MS = 8000L;
    /**
     * 【已删除】曾经的"复活保护期 / 连杀保护窗口"（原地保命 + 续期）。
     * 它等价于一段刻意制造的无敌，按需求彻底移除：现在每次致死都是真死亡 + 真重生。
     * 去重仍保留（同一 tick/同一波流程里不重复触发），但不再有免伤效果。
     */
    private static final long MONITOR_INTERVAL_MS = 500L;
    private static final long STALE_MS = 30_000L;
    private static final String HOOK_CLASS = "net.diexv.potionenchant.agent.RevivalHook";

    /** 侧路状态注册表：拥有复活效果的玩家（独立于实体生命周期与药水效果） */
    private static final Map<UUID, ReviveState> REGISTRY = new ConcurrentHashMap<>();
    /** 待真重生的玩家快照 */
    private static final Map<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();
    /** 复活冷却 */
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    /** 正在执行真重生的玩家（屏蔽整个重生流程里的钩子，包括 PlayerList.respawn 删旧玩家） */
    private static final Set<UUID> REVIVING = ConcurrentHashMap.newKeySet();
    /** 伤害结算前的血量缓存（hurt 注入点写入） */
    private static final Map<UUID, Float> PRE_HURT_HEALTH = new ConcurrentHashMap<>();
    /** 正在恢复数据的玩家（防止我们自己的写入再次触发复活） */
    private static final Set<UUID> RESTORING = ConcurrentHashMap.newKeySet();
    /**
     * 外部重生进行中（PlayerList#respawn 被任何人调用）。
     * 这个标记必须在所有"拦截删除"的判断之前生效：原版 respawn 内部会
     * removePlayerImmediately(old, DISCARDED)，如果我们把这次删除也吞掉，
     * 旧玩家会继续挂在 level 里 → addPlayer 撞 UUID → 新玩家注册失败 → 变幽灵实体。
     */
    private static final Set<UUID> RESPAWNING = ConcurrentHashMap.newKeySet();
    /** 外部重生后待还原的快照（下一 tick 灌回新玩家对象） */
    private static final Map<UUID, RestoreJob> PENDING_RESTORE = new ConcurrentHashMap<>();
    /** 上一次给玩家发"立即重生"包的时间（墙钟毫秒），用于周期性维持该标记 */
    private static final Map<UUID, Long> IMMEDIATE_RESPAWN_SENT = new ConcurrentHashMap<>();
    /** 立即重生标记的重发间隔：客户端只要收到过 health<=0 的包就会把 showDeathScreen 设回 true */
    private static final long IMMEDIATE_RESPAWN_REFRESH_MS = 5000L;
    /** "最近一次活着时的最大血量"：最大血量属性被攻击方压成负数时用来修回来 */
    private static final Map<UUID, Float> LAST_GOOD_MAX_HEALTH = new ConcurrentHashMap<>();
    /** 最大血量的兜底值（原版默认），只在没有任何"健康记录"时使用 */
    private static final float DEFAULT_MAX_HEALTH = 20.0F;
    /** 外部重生风暴判定：窗口内超过该次数就暂停发"立即重生"包（让客户端显示死亡界面，打断循环） */
    private static final int BURST_LIMIT = 6;
    private static final long BURST_WINDOW_MS = 2000L;
    private static final long IMMEDIATE_SUPPRESS_MS = 5000L;
    private static final Map<UUID, long[]> RESPAWN_BURST = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> IMMEDIATE_SUPPRESS = new ConcurrentHashMap<>();
    /** 上一次"放行"客户端重生请求的时间：用来把自动重生风暴节流成 0.5 次/秒 */
    private static final Map<UUID, Long> CLIENT_RESPAWN_ALLOWED = new ConcurrentHashMap<>();
    private static final long CLIENT_RESPAWN_MIN_GAP_MS = 2000L;
    /** 客户端指令包 action 读取器的缓存 */
    private static volatile Method PACKET_ACTION_GETTER;
    private static volatile Field LISTENER_PLAYER_FIELD;
    /** "命中注入点但未接管"的日志节流 */
    private static final Map<String, Long> SKIP_LOG = new ConcurrentHashMap<>();

    private static volatile boolean hookInstalled = false;
    private static volatile boolean monitorStarted = false;

    /**
     * 复活流程"卡住"判定：正常从 die 注入到收尾只有 1~3 tick，
     * 超过这个墙钟时间还挂着说明队列任务丢了 / 中转路径异常 → 兜底清理，
     * 否则注册表条目会把玩家永久变成"杀不死"（见 {@link #endReviveState}）。
     */
    private static final long REVIVE_STUCK_MS = 10_000L;

    /**
     * "掉落已被拦截"的承诺表（到期时间 = 墙钟毫秒）。
     *
     * 为什么需要：拦掉落（{@link #onDropLoot}）与"重生时保留背包"（{@link #onPlayerListRespawn}
     * 强制 keepEverything=true）是两次**互相独立**的回调，中间可能隔着 1~20 tick。
     * 如果这期间玩家的复活能力消失（药水刚好自然到期 / 被 Boss 剥离 / 护甲被换掉），
     * 重生就会走原版 keepEverything=false —— 掉落已经被我们吞掉、背包又不保留
     * = **物品凭空消失**。所以拦掉落时必须把"这次要保留背包"记下来，由重生回调消费。
     */
    private static final Map<UUID, Long> KEEP_INVENTORY_UNTIL = new ConcurrentHashMap<>();
    private static final long KEEP_INVENTORY_MS = 15_000L;

    private RevivalManager() {}

    // ==================== 侧路状态 ====================

    /** 侧路复活状态（只被服务端线程写；后台线程只读） */
    private static final class ReviveState {
        volatile int amplifier = -1;
        volatile int durationTicks = 0;
        volatile boolean ambient;
        volatile boolean visible;
        volatile boolean showIcon;
        /** 药水到期游戏刻（用于后台线程判超时） */
        volatile long expiresAtGameTime = Long.MAX_VALUE;
        /** 已触发复活的游戏刻（-1 = 未触发） */
        volatile long triggerTick = -1L;
        /** 已触发复活的墙钟毫秒（判定"流程卡住"用，游戏刻在卡顿/跨存档时不可靠） */
        volatile long triggerMillis = 0L;
        /** 最近一次刷新（墙钟毫秒，供后台线程判断离线/过期） */
        volatile long lastSeenMillis = System.currentTimeMillis();
        /** 效果消失后的兜底保留截止（墙钟毫秒，0 = 未进入兜底） */
        volatile long graceUntilMillis = 0L;
    }

    // ==================== 注入挂载 ====================

    /** 主模组初始化时调用：把回调绑定到 **注入代码真正会用到的那份** RevivalHook + 补注入 + 启动后台线程 */
    public static void installHook() {
        if (hookInstalled) {
            return;
        }
        hookInstalled = true;

        // 关键：注入代码（位于 net.minecraft 类的字节码里）解析到的是 **mod jar 内的那份** RevivalHook
        // （已被历史堆栈证实：~[potionenchant-....jar%23xxx!/]）。
        // 而 AgentLauncher 又用 defineClass 在 SystemClassLoader 里定义了另一份——
        // 两份静态字段互不相通，只绑 system 那份会导致注入点里回调恒为 null（完全静默）。
        // 因此两份都绑：谁被注入代码用到，谁就生效。
        try {
            bind(RevivalHook.class);
            LOGGER.info("[Revival] 回调已绑定 mod 副本：{}", RevivalHook.class.getClassLoader());
        } catch (Throwable t) {
            LOGGER.error("[Revival] 绑定 mod 副本失败：{}", t.toString());
        }
        try {
            Class<?> systemHook = Class.forName(HOOK_CLASS, true, ClassLoader.getSystemClassLoader());
            if (systemHook != RevivalHook.class) {
                bind(systemHook);
                LOGGER.info("[Revival] 回调已绑定 system 副本：{}", systemHook.getClassLoader());
            } else {
                LOGGER.info("[Revival] system 副本与 mod 副本是同一个类，无需重复绑定");
            }
        } catch (Throwable t) {
            LOGGER.info("[Revival] 未发现 system 副本（可忽略）：{}", t.toString());
        }

        try {
            DiexvSwordAgent.ensureRevivalRedefined();
        } catch (Throwable t) {
            LOGGER.error("[Revival] retransform 补注入失败：{}", t.toString());
        }
        startMonitor();
    }

    /** 把全部回调注册到指定的 RevivalHook 类副本上 */
    private static void bind(Class<?> hook) throws Exception {
        hook.getMethod("install", Predicate.class)
                .invoke(null, (Predicate<Object>) RevivalManager::onServerPlayerDeath);
        hook.getMethod("installHurt", BiConsumer.class)
                .invoke(null, (BiConsumer<Object, Float>) RevivalManager::onHurt);
        hook.getMethod("installLethalHealth", BiConsumer.class)
                .invoke(null, (BiConsumer<Object, Object>) RevivalManager::onLethalHealthWrite);
        hook.getMethod("installRemove", BiPredicate.class)
                .invoke(null, (BiPredicate<Object, Object>) RevivalManager::onRemove);
        hook.getMethod("installPlayerTick", Consumer.class)
                .invoke(null, (Consumer<Object>) RevivalManager::onPlayerTick);
        hook.getMethod("installTickDeath", Predicate.class)
                .invoke(null, (Predicate<Object>) RevivalManager::onTickDeath);
        hook.getMethod("setPlayerClass", Class.class).invoke(null, ServerPlayer.class);
        hook.getMethod("setHealthAccessor", Object.class).invoke(null, LivingEntityAccessor.HEALTH());
        hook.getMethod("installDropLoot", Predicate.class)
                .invoke(null, (Predicate<Object>) RevivalManager::onDropLoot);
        hook.getMethod("installPlayerListRespawn", BiFunction.class)
                .invoke(null, (BiFunction<Object, Boolean, Boolean>) RevivalManager::onPlayerListRespawn);
        hook.getMethod("installClientCommand", BiPredicate.class)
                .invoke(null, (BiPredicate<Object, Object>) RevivalManager::onClientCommand);
    }

    /** 注入点 LivingEntity#dropAllDeathLoot：复活药水持有者不掉落（防"掉落+背包"双份复制） */
    public static boolean onDropLoot(Object entity) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return false;
        }
        if (!canRevive(player)) {
            return false;
        }
        // 拦下掉落 = 同时承诺"这次重生的背包一定保留"：把承诺记下来，由 onPlayerListRespawn 消费
        // 并强制 keepEverything=true。否则若重生时复活能力刚好消失，就会出现
        // "掉落被吞 + 背包不保留" → 物品凭空消失（见 KEEP_INVENTORY_UNTIL 注释）。
        KEEP_INVENTORY_UNTIL.put(player.getUUID(), System.currentTimeMillis() + KEEP_INVENTORY_MS);
        return true;
    }

    /** 消费"掉落已被拦截"的承诺；过期或不存在返回 false */
    private static boolean takeKeepInventoryPromise(UUID id) {
        Long until = KEEP_INVENTORY_UNTIL.remove(id);
        return until != null && System.currentTimeMillis() <= until;
    }

    // ==================== 维度伪装（消除外维度复活时的加载界面） ====================

    /**
     * 告诉客户端："接下来这次真重生里的中转维度不要当成真正的维度切换"。
     *
     * 外维度死亡时原版 {@code PlayerList#respawn} 会先按重生点/主世界建号并发 respawn 包，
     * 客户端见维度不同就切关卡 + 弹 {@code ReceivingLevelScreen}，我们随后再拉回死亡维度
     * → 白切一次、区块重载、加载界面闪一下。置位后客户端会把那个中转包折叠成当前维度
     * （不切关卡、不弹界面），**真重生流程本身完全不变**。
     */
    private static void armDimensionSpoof(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            net.diexv.potionenchant.network.RevivalNetwork.sendSpoof(player, true);
        } catch (Throwable t) {
            LOGGER.warn("[Revival] 发送维度伪装包失败（忽略，只是会闪一次加载界面）：{}", t.toString());
        }
    }

    /** 解除维度伪装（一次性 + 客户端 3s TTL 兜底，漏发也不会影响后续正常维度切换） */
    private static void disarmDimensionSpoof(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            net.diexv.potionenchant.network.RevivalNetwork.sendSpoof(player, false);
        } catch (Throwable ignored) {
        }
    }

    // ==================== 外部重生（PlayerList#respawn 被任何人调用） ====================

    /**
     * 注入点 PlayerList#respawn(ServerPlayer, boolean) 头部，返回值写回 keepEverything 参数。
     *
     * 这条路是"状态被冲掉"的元凶：客户端收到死亡界面包后会自动回 PERFORM_RESPAWN，
     * 原版/别的 mod 也会直接调用。它对有复活能力的玩家执行时，新玩家是
     * **空背包 + 重生点坐标 + 视角 -180°**，我们上一轮辛苦恢复的背包/坐标全没。
     *
     * 处理：
     *  1) 若我们自己还排着队要真重生（SNAPSHOTS 里有快照），那就**认领这次重生**：
     *     取消自己排队的那个，直接用死亡瞬间的快照还原，避免"连着两次 respawn"撞 UUID 变幽灵实体；
     *  2) 否则现场抓一份快照；
     *  3) 标记 RESPAWNING（让本次原版删除放行）+ 下一 tick 把快照灌回新玩家；
     *  4) 返回 true → 强制原版走"保留全部数据"分支（背包/经验/饥饿/末影箱都留着，
     *     且 Forge PlayerEvent.Clone(wasDeath=false)，别的 mod 不会按真死亡清数据）。
     */
    public static boolean onPlayerListRespawn(Object oldPlayer, boolean keepEverything) {
        if (!(oldPlayer instanceof ServerPlayer player) || player.level().isClientSide()) {
            return keepEverything;
        }
        UUID id = player.getUUID();
        // 先消费"掉落已被拦截"的承诺：无论这次走哪条分支，都不能让它残留下来影响后面的**真死亡**
        boolean keepInventoryPromised = takeKeepInventoryPromise(id);
        if (REVIVING.contains(id) || RESTORING.contains(id)) {
            return true;   // 我们自己发起的真重生：本次就是它，强制保留数据
        }
        MinecraftServer server = player.getServer();
        String who = caller();
        if (server == null) {
            return keepEverything;
        }
        if (!canRevive(player)) {
            if (keepInventoryPromised) {
                // 玩家这次是**真死亡**（重生时复活能力已经消失），但我们此前确实吞掉了掉落：
                // 只强制保留背包（keepEverything=true），其余（重生点/死亡界面/统计）全部交给原版，
                // 绝不把这次真死亡当成复活来劫持坐标。
                LOGGER.warn("[Revival] 掉落已被拦截但当前已无复活能力 → 本次原版重生强制 keepEverything=true，"
                        + "避免物品凭空消失（调用方：{}）", who);
                return true;
            }
            logSkip("PlayerList.respawn 注入", player, "无复活状态（调用方：" + who + "）");
            clearImmediateRespawn(player);   // 恢复原版死亡界面行为
            return keepEverything;
        }
        // RESPAWNING 必须在任何判断之前置位：原版 respawn 内部立刻就会 removePlayerImmediately(old, DISCARDED)
        RESPAWNING.add(id);
        // 先武装"维度伪装"：让客户端不要把紧接着的中转维度 respawn 包当成真正的维度切换
        // （否则外维度复活会白切一次关卡 + 弹维度加载界面 + 重载区块）
        armDimensionSpoof(player);
        // 认领我们排队中的快照：这次外部重生就当作"真重生"，别再来一次
        Snapshot snap = SNAPSHOTS.remove(id);
        if (snap == null) {
            ReviveState st = REGISTRY.get(player.getUUID());
            snap = Snapshot.capture(player, st);
        }
        snap.triggerTick = player.level().getGameTime();
        PENDING_RESTORE.put(id, new RestoreJob(snap, player));
        noteRespawnBurst(id);
        sendImmediateRespawn(player);   // 原版"立即重生"机制：客户端不弹死亡界面，直接自动重生
        LOGGER.warn("[Revival] 检测到外部重生（调用方：{}，原 keepEverything={} → 强制 true）→ 已记录快照，下一 tick 原样还原：{}",
                who, keepEverything, describeExact(player));
        final UUID uuid = id;
        final MinecraftServer srv = server;
        srv.tell(new TickTask(srv.getTickCount(), () -> restoreAfterExternalRespawn(srv, uuid)));
        return true;
    }

    /**
     * 外部重生风暴计数：短时间被反复重生说明"客户端自动重生循环"已经起转，
     * 这时暂停发"立即重生"包，让客户端正常弹出死亡界面（玩家点一次即可），
     * 否则会像实测那样 20 次/秒无限自喂。玩家血量恢复正常后窗口自然结束。
     */
    private static void noteRespawnBurst(UUID id) {
        long now = System.currentTimeMillis();
        long[] burst = RESPAWN_BURST.computeIfAbsent(id, k -> new long[]{now, 0L});
        synchronized (burst) {
            if (now - burst[0] > BURST_WINDOW_MS) {
                burst[0] = now;
                burst[1] = 0L;
            }
            burst[1]++;
            if (burst[1] == BURST_LIMIT + 1L) {
                IMMEDIATE_SUPPRESS.put(id, now + IMMEDIATE_SUPPRESS_MS);
                LOGGER.error("[Revival] 检测到重生风暴（{}ms 内 {} 次外部重生）→ 暂停发送立即重生包 {}ms，"
                                + "改让客户端显示死亡界面以打断「死亡→重生」循环；根因通常是最大血量被压成负数",
                        BURST_WINDOW_MS, burst[1], IMMEDIATE_SUPPRESS_MS);
            }
        }
    }

    /**
     * 客户端 PERFORM_RESPAWN 请求拦截（注入 ServerGamePacketListenerImpl#handleClientCommand 头部）。
     *
     * 两种必须拦的情况：
     *  1) **本次死亡已经由我们接管**（真重生排队中 / 外部重生待还原 / 正在还原或重生）：
     *     放行会让原版先按"重生点/世界出生点"新建玩家并发一次传送包，我们随后再拉回死前位置
     *     → 玩家看到"先重生到重生点、再被拽回死前位置"。拦下来由我们一次放到位。
     *  2) **客户端自动重生风暴**：客户端只要处于"已死(health<=0) + showDeathScreen=false"，
     *     Minecraft#setScreen(null) 就会自动发 PERFORM_RESPAWN，而服务端重生又会让客户端再次
     *     setScreen(null) → 自己喂自己（实测 20 次/秒）。玩家其实活着时这种请求直接忽略（节流放行）。
     *
     * 真死亡（没有任何待处理的复活 + 血量 <= 0）一律**放行**，保持原版重生行为。
     */
    public static boolean onClientCommand(Object listener, Object packet) {
        try {
            if (listener == null || packet == null) {
                return false;
            }
            String action = packetAction(packet);
            if (!"PERFORM_RESPAWN".equals(action)) {
                return false;
            }
            ServerPlayer player = listenerPlayer(listener);
            if (player == null || player.level().isClientSide()) {
                return false;
            }
            UUID id = player.getUUID();
            // 【关键：接管期间忽略客户端的重生请求】
            // 这次死亡已经由我们接管（真重生已排队 / 外部重生待还原 / 正在还原 / 正在重生）时，
            // 客户端的 PERFORM_RESPAWN 绝不能放行 —— 放行会让**原版**先按"重生点/世界出生点"
            // 新建玩家并发一次传送包，我们随后（若队列任务迟到还会晚一个 tick）再把他拉回死前位置，
            // 玩家就会看到"先重生到重生点、再被拽回死前位置"的中间态。
            // 由我们的接管流程自己完成复活后，客户端在同一 tick 内只收到**最终落点**的传送，
            // 第一帧就已经在死前的位置与视角上（落点在抓快照时已定好，见 Snapshot.Placement）。
            boolean handling = SNAPSHOTS.containsKey(id) || PENDING_RESTORE.containsKey(id)
                    || RESPAWNING.contains(id) || REVIVING.contains(id) || RESTORING.contains(id);
            if (handling) {
                logClientRespawnIgnored(player, id, "本次死亡已由模组接管");
                return true;   // 忽略：让我们的真重生/还原流程把玩家放回死前位置
            }
            if (!canRevive(player)) {
                return false;   // 没有复活能力：正常死亡流程，放行
            }
            if (player.getHealth() <= 0.0F) {
                return false;   // 真死亡（没有任何待处理的复活）：放行，让原版正常重生
            }
            // 剩下只可能是"玩家活着、有复活能力、却没有待处理的复活"：客户端自动重生风暴的噪音请求。
            // 节流而不是全拦：2 秒内只放行一次，这样风暴被压成 0.5 次/秒，
            // 而玩家真在死亡界面上点"重生"时（请求间隔通常远大于 2s）永远能生效。
            long now = System.currentTimeMillis();
            Long lastAllowed = CLIENT_RESPAWN_ALLOWED.get(id);
            if (lastAllowed == null || now - lastAllowed >= CLIENT_RESPAWN_MIN_GAP_MS) {
                CLIENT_RESPAWN_ALLOWED.put(id, now);
                LOGGER.warn("[Revival] 放行客户端重生请求（{}，距上次 {}ms ≥ {}ms）",
                        player.getGameProfile().getName(),
                        lastAllowed == null ? -1L : now - lastAllowed, CLIENT_RESPAWN_MIN_GAP_MS);
                return false;
            }
            logClientRespawnIgnored(player, id, "存活状态下的自动重生噪音，节流中");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 忽略客户端重生请求的日志（节流 1s） */
    private static void logClientRespawnIgnored(ServerPlayer player, UUID id, String why) {
        long now = System.currentTimeMillis();
        String key = id + "|clientrespawn";
        Long last = SKIP_LOG.get(key);
        if (last == null || now - last >= 1000L) {
            SKIP_LOG.put(key, now);
            LOGGER.warn("[Revival] 已忽略客户端重生请求（{} → 交由模组接管）：{} {}", 
                    player.getGameProfile().getName(), why, describe(player));
        }
    }

    /** 反射读 ServerboundClientCommandPacket#getAction（SRG m_133850_，枚举常量名不受混淆影响） */
    private static String packetAction(Object packet) {
        try {
            Method m = PACKET_ACTION_GETTER;
            if (m == null) {
                for (String name : new String[]{"getAction", "m_133850_"}) {
                    try {
                        m = packet.getClass().getMethod(name);
                        break;
                    } catch (Throwable ignored) {
                    }
                }
                if (m == null) {
                    for (Method cand : packet.getClass().getMethods()) {
                        if (cand.getParameterCount() == 0 && Enum.class.isAssignableFrom(cand.getReturnType())) {
                            m = cand;
                            break;
                        }
                    }
                }
                PACKET_ACTION_GETTER = m;
            }
            if (m == null) {
                return null;
            }
            Object value = m.invoke(packet);
            return value instanceof Enum<?> e ? e.name() : String.valueOf(value);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 反射读 ServerGamePacketListenerImpl#player（SRG f_9743_） */
    private static ServerPlayer listenerPlayer(Object listener) {
        try {
            Field f = LISTENER_PLAYER_FIELD;
            if (f == null) {
                for (String name : new String[]{"player", "f_9743_"}) {
                    try {
                        f = listener.getClass().getField(name);
                        break;
                    } catch (Throwable ignored) {
                    }
                }
                LISTENER_PLAYER_FIELD = f;
            }
            if (f == null) {
                return null;
            }
            Object v = f.get(listener);
            return v instanceof ServerPlayer sp ? sp : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 原版"立即重生"（doImmediateRespawn 游戏规则）的客户端实现机制，照抄不改游戏规则：     *
     *   GameRules 里该规则的变更回调 = 对每个玩家发
     *     ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, value ? 1.0F : 0.0F)
     *   客户端 ClientPacketListener#handleGameEvent：
     *     IMMEDIATE_RESPAWN → player.setShowDeathScreen(f == 0.0F)
     *   于是 showDeathScreen=false 时，ClientPacketListener#handlePlayerCombatKill 走 else 分支
     *     player.respawn()   ← 自动回 PERFORM_RESPAWN，不弹界面、不需要点
     *
     * 所以这里只需要发同一个包（1.0F = 立即重生），不需要动游戏规则。
     * 但**只有当玩家真的活着时才发**：血量还是 0/负数时发这个包，客户端会进入
     * "已死 + 不显示死亡界面"状态，然后被 Minecraft#setScreen(null) 无限自动重生。
     */
    private static void sendImmediateRespawn(ServerPlayer player) {
        try {
            Long until = IMMEDIATE_SUPPRESS.get(player.getUUID());
            if (until != null) {
                if (System.currentTimeMillis() < until) {
                    return;
                }
                IMMEDIATE_SUPPRESS.remove(player.getUUID());
            }
            if (player.getHealth() <= 0.0F) {
                return;   // 玩家此刻还没活过来：不发，避免把客户端推进自动重生循环
            }
            player.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.IMMEDIATE_RESPAWN, 1.0F));
        } catch (Throwable t) {
            LOGGER.warn("[Revival] 发送立即重生包失败：{}", t.toString());
        }
    }

    /**
     * 收回"立即重生"标记（发 0.0F）。
     *
     * 这个标记是**客户端持久状态**：`setShowDeathScreen(false)` 之后不会自己变回来
     * （只有 health<=0 的健康包才会把它设回 true）。所以复活状态一旦彻底消失——
     * 效果自然到期 / 兜底窗口结束 / 护甲附魔也没了——必须主动收回，
     * 否则玩家"没有复活能力时死亡也不弹死亡界面、直接自动重生"。
     * 只在确实发过 1.0F 时才发 0.0F，避免每 tick 刷包。
     */
    private static void clearImmediateRespawn(ServerPlayer player) {
        try {
            if (IMMEDIATE_RESPAWN_SENT.remove(player.getUUID()) == null) {
                return;   // 没给这个玩家开过，不用收
            }
            IMMEDIATE_SUPPRESS.remove(player.getUUID());
            player.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.IMMEDIATE_RESPAWN, 0.0F));
            LOGGER.info("[Revival] {} 已失去复活能力 → 收回立即重生标记，恢复原版死亡界面行为",
                    player.getGameProfile().getName());
        } catch (Throwable t) {
            LOGGER.warn("[Revival] 收回立即重生标记失败：{}", t.toString());
        }
    }

    /**
     * 保证最大血量是正常的：攻击方（boss 的「彻底遗忘」/ 剑的封印）可能把
     * MAX_HEALTH 属性压成 0 或负数，这时 LivingEntity#setHealth 会被
     * Mth.clamp(f, 0, maxHealth) 钳成负数 —— 玩家永远活不过来，客户端一直
     * isDeadOrDying()，于是 20 次/秒自动重生。这里把基础值与负向修饰符修回来。
     */
    private static void ensureHealthyMaxHealth(ServerPlayer player) {
        try {
            var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (attr == null) {
                return;
            }
            float before = player.getMaxHealth();
            if (before >= 1.0F) {
                return;
            }
            Float lastGood = LAST_GOOD_MAX_HEALTH.get(player.getUUID());
            double target = lastGood != null && lastGood >= 1.0F ? lastGood : DEFAULT_MAX_HEALTH;
            for (var mod : new ArrayList<>(attr.getModifiers())) {
                if (mod.getAmount() < 0.0D) {
                    attr.removeModifier(mod);
                }
            }
            attr.setBaseValue(target);
            if (player.getMaxHealth() < 1.0F) {
                for (var mod : new ArrayList<>(attr.getModifiers())) {
                    attr.removeModifier(mod);
                }
                attr.setBaseValue(target);
            }
            LOGGER.warn("[Revival] {} 的最大血量异常（{}）→ 已修回基础值 {}，当前最大血量 {}",
                    player.getGameProfile().getName(), before, target, player.getMaxHealth());
        } catch (Throwable t) {
            LOGGER.warn("[Revival] 修复最大血量失败：{}", t.toString());
        }
    }

    /** 下一 tick：把外部重生前的快照灌回原版新建的玩家对象 */
    private static void restoreAfterExternalRespawn(MinecraftServer server, UUID id) {
        RestoreJob job = PENDING_RESTORE.remove(id);
        RESPAWNING.remove(id);
        if (job == null) {
            return;
        }
        ServerPlayer live = server.getPlayerList().getPlayer(id);
        if (live == null) {
            LOGGER.warn("[Revival] 外部重生后找不到玩家实体（可能已下线），放弃还原");
            return;
        }
        restoreInto(live, job.snap, job.oldPlayer, "外部重生还原");
    }

    /** 取调用栈里第一个非原版、非我们自己的帧，用来定位"谁触发了 respawn" */
    private static String caller() {
        try {
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                String cn = e.getClassName();
                if (cn.startsWith("java.") || cn.startsWith("jdk.") || cn.startsWith("sun.")
                        || cn.startsWith("net.diexv.potionenchant.")
                        || cn.startsWith("net.minecraft.")) {
                    continue;
                }
                return cn + "#" + e.getMethodName() + ":" + e.getLineNumber();
            }
        } catch (Throwable ignored) {
        }
        return "未知（原版链路）";
    }

    /** 一行状态摘要：坐标/视角/维度/背包件数/血量（坐标 6 位小数） */
    private static String describe(ServerPlayer player) {
        try {
            int items = 0;
            int slots = 0;
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!inv.getItem(i).isEmpty()) {
                    items++;
                    slots += inv.getItem(i).getCount();
                }
            }
            return String.format("pos=(%.6f,%.6f,%.6f) rot=(%.4f,%.4f) 头=%.4f 身=%.4f dim=%s 背包=%d格/%d件 血量=%.4f 最大血量=%.4f",
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot,
                    player.level().dimension().location(),
                    items, slots, player.getHealth(), player.getMaxHealth());
        } catch (Throwable t) {
            return "<状态读取失败：" + t + ">";
        }
    }

    /**
     * 全精度坐标/朝向（坐标 10 位小数 ≈ 0.1 纳米，朝向 6 位小数），
     * 另外附上所在方块坐标与方块内偏移，方便肉眼核对"小数点后的数据"。
     */
    private static String describeExact(ServerPlayer player) {
        try {
            return String.format(
                    "pos=(%.10f, %.10f, %.10f) block=(%d,%d,%d) 方块内偏移=(%.10f,%.10f,%.10f) "
                            + "rot=(%.6f, %.6f) 头=%.6f 身=%.6f 眼高=%.4f dim=%s",
                    player.getX(), player.getY(), player.getZ(),
                    player.getBlockX(), player.getBlockY(), player.getBlockZ(),
                    player.getX() - Math.floor(player.getX()),
                    player.getY() - Math.floor(player.getY()),
                    player.getZ() - Math.floor(player.getZ()),
                    player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot,
                    player.getEyeHeight(), player.level().dimension().location());
        } catch (Throwable t) {
            return "<状态读取失败：" + t + ">";
        }
    }

    /** 快照里的死亡瞬间坐标/朝向（全精度） */
    private static String describeSnapshot(Snapshot snap) {
        return String.format(
                "pos=(%.10f, %.10f, %.10f) block=(%d,%d,%d) rot=(%.6f, %.6f) 头=%.6f 身=%.6f 速度=(%.6f,%.6f,%.6f) dim=%s",
                snap.x, snap.y, snap.z,
                (int) Math.floor(snap.x), (int) Math.floor(snap.y), (int) Math.floor(snap.z),
                snap.yRot, snap.xRot, snap.yHeadRot, snap.yBodyRot,
                snap.motion.x, snap.motion.y, snap.motion.z, snap.dimension.location());
    }

    /** 还原误差：与快照逐项相减，任何取整/夹取都会在这里暴露出来 */
    private static String describeDrift(ServerPlayer player, Snapshot snap) {
        return String.format(
                "Δpos=(%.10f, %.10f, %.10f) Δrot=(%.6f, %.6f) Δ头=%.6f Δ身=%.6f",
                player.getX() - snap.x, player.getY() - snap.y, player.getZ() - snap.z,
                player.getYRot() - snap.yRot, player.getXRot() - snap.xRot,
                player.getYHeadRot() - snap.yHeadRot, player.yBodyRot - snap.yBodyRot);
    }

    /** 后台检测线程：只读并发注册表，做离线/过期清理；绝不触碰游戏对象 */
    private static void startMonitor() {
        if (monitorStarted) {
            return;
        }
        monitorStarted = true;
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(MONITOR_INTERVAL_MS);
                    long now = System.currentTimeMillis();
                    int stale = 0;
                    for (Map.Entry<UUID, ReviveState> e : REGISTRY.entrySet()) {
                        ReviveState st = e.getValue();
                        if (st.triggerTick >= 0) {
                            // 进行中的复活：正常 1~3 tick 内就会被 endReviveState 收尾；
                            // 挂了这么久说明队列任务丢了 → 兜底清理，绝不能永久留着把玩家变成"杀不死"
                            if (st.triggerMillis > 0L && now - st.triggerMillis > STALE_MS) {
                                REGISTRY.remove(e.getKey());
                                stale++;
                            }
                            continue;
                        }
                        if (now - st.lastSeenMillis > STALE_MS) {
                            REGISTRY.remove(e.getKey());
                            stale++;
                        }
                    }
                    if (stale > 0) {
                        LOGGER.info("[Revival] 后台检测：清理 {} 个失效复活状态，当前 {} 个", stale, REGISTRY.size());
                    }
                } catch (InterruptedException ie) {
                    return;
                } catch (Throwable t) {
                    // 后台线程永不因异常退出
                }
            }
        }, "potionenchant-revival-monitor");
        thread.setDaemon(true);
        thread.start();
        LOGGER.info("[Revival] 后台检测线程已启动");
    }

    // ==================== 注入点回调（服务端线程） ====================

    /** 注入点 ServerPlayer#tick：每 tick 刷新侧路注册表 */
    public static void onPlayerTick(Object obj) {
        if (!(obj instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        MobEffectInstance inst = player.getEffect(EffectRegistry.REVIVAL.get());
        // 记录"最近一次活着时的最大血量"，供最大血量被攻击方压坏时修回
        if (player.getHealth() > 0.0F && player.getMaxHealth() >= 1.0F) {
            LAST_GOOD_MAX_HEALTH.put(id, player.getMaxHealth());
        }
        if (inst != null) {
            // 最大血量被压成 0/负数时，玩家永远活不过来（setHealth 会被 clamp 成负数），
            // 客户端因此一直 isDeadOrDying() → 自动重生循环。这里每 tick 兜底修复。
            ensureHealthyMaxHealth(player);
        }
        if (inst != null) {
            ReviveState st = REGISTRY.computeIfAbsent(id, k -> new ReviveState());
            boolean first = st.amplifier < 0;
            st.amplifier = inst.getAmplifier();
            st.durationTicks = inst.getDuration();
            st.ambient = inst.isAmbient();
            st.visible = inst.isVisible();
            st.showIcon = inst.showIcon();
            st.expiresAtGameTime = player.level().getGameTime() + inst.getDuration();
            st.lastSeenMillis = System.currentTimeMillis();
            st.graceUntilMillis = 0L;
            // 周期性维持"立即重生"：只要身上有复活效果，客户端就一直处于"不弹死亡界面"状态，
            // 这样任何来源的死亡界面包都会走客户端的自动重生分支（原版 doImmediateRespawn 机制）。
            long nowMs = st.lastSeenMillis;
            Long lastSent = IMMEDIATE_RESPAWN_SENT.get(id);
            if (lastSent == null || nowMs - lastSent >= IMMEDIATE_RESPAWN_REFRESH_MS) {
                IMMEDIATE_RESPAWN_SENT.put(id, nowMs);
                sendImmediateRespawn(player);
            }
            if (first) {
                LOGGER.info("[Revival] tick 注入正常：{} 获得复活状态（等级 {}，剩余 {}t）",
                        player.getGameProfile().getName(), st.amplifier, st.durationTicks);
            }
        } else {
            ReviveState st = REGISTRY.get(id);
            long nowMs = System.currentTimeMillis();
            if (st != null) {
                if (st.triggerTick < 0) {
                    // 区分"自然到期"和"被外部剥离"：
                    //  - 之前记录的到期游戏刻已经过了 → 是药水自己走完时间 → 不该再保留任何复活能力；
                    //  - 还没到期效果就没了 → 被 Boss「彻底遗忘」这类能力剥掉 → 给一小段兜底窗口。
                    int dur = st.durationTicks;
                    boolean infinite = dur <= 0 || dur == MobEffectInstance.INFINITE_DURATION;
                    boolean naturalExpiry = !infinite && player.level().getGameTime() >= st.expiresAtGameTime;
                    if (naturalExpiry) {
                        REGISTRY.remove(id);
                        LOGGER.info("[Revival] {} 的复活效果自然到期 → 立即停止接管死亡（不保留兜底窗口）",
                                player.getGameProfile().getName());
                    } else if (st.graceUntilMillis == 0L) {
                        st.graceUntilMillis = nowMs + GRACE_MS;
                        LOGGER.info("[Revival] {} 复活效果被外部剥离（剩余 {}t），保留 {}s 兜底窗口",
                                player.getGameProfile().getName(), dur, GRACE_MS / 1000L);
                    } else if (nowMs > st.graceUntilMillis) {
                        REGISTRY.remove(id);
                        LOGGER.info("[Revival] {} 复活状态兜底窗口结束", player.getGameProfile().getName());
                    }
                } else if (st.triggerMillis > 0L && nowMs - st.triggerMillis > REVIVE_STUCK_MS) {
                    // 复活流程早该结束却还挂着（队列任务丢失 / 路径异常）：兜底清理。
                    // 不清理的话这个条目会让 canRevive() 恒真 → 玩家此后无效果也每次都被接管、
                    // 死亡界面永不弹出（"无法死亡"），且条目永远不会再被移除。
                    REGISTRY.remove(id);
                    LOGGER.warn("[Revival] {} 的复活流程状态超时（{}ms）→ 已清理注册表，恢复正常死亡/复活判定",
                            player.getGameProfile().getName(), nowMs - st.triggerMillis);
                }
            }
            // 效果没了、注册表也没了、护甲也没附魔 → 这个玩家已经**彻底**没有复活能力：
            // 立刻收回"立即重生"标记，让原版死亡界面/手动重生恢复正常行为。
            if (!canRevive(player)) {
                clearImmediateRespawn(player);
            }
        }

        // 队列任务迟迟未执行（或丢失）：由 tick 注入接管，保证一定完成重生。
        // 此时已不在 die/hurt/remove 的调用栈内（是本 tick 的实体 tick 起点），安全。
        Snapshot pending = SNAPSHOTS.get(id);
        RestoreJob external = PENDING_RESTORE.get(id);
        if (external != null && external.snap.triggerTick >= 0L
                && player.level().getGameTime() - external.snap.triggerTick >= 3L) {
            LOGGER.warn("[Revival] 外部重生还原任务未执行，改由 tick 注入接管 {}", player.getGameProfile().getName());
            MinecraftServer srv = player.getServer();
            if (srv != null) {
                restoreAfterExternalRespawn(srv, id);
            }
        } else if (pending != null && pending.triggerTick >= 0L
                && player.level().getGameTime() - pending.triggerTick >= 2L) {
            LOGGER.warn("[Revival] 重生任务 2t 内未执行，改由 tick 注入接管 {}", player.getGameProfile().getName());
            revive(player, pending);
        }
    }

    /** 注入点 LivingEntity#hurt：缓存伤害结算前的血量 */
    public static void onHurt(Object entity, float amount) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (!canRevive(player)) {
            return;
        }
        PRE_HURT_HEALTH.put(player.getUUID(), player.getHealth());
    }

    /** 注入点 ServerPlayer#die：true = 接管（原版 die 立即 return → 不掉落、不发死亡界面包） */
    public static boolean onServerPlayerDeath(Object entity) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return false;
        }
        UUID id = player.getUUID();
        if (REVIVING.contains(id) || RESTORING.contains(id)) {
            return false;   // 我们自己的真重生流程
        }
        if (RESPAWNING.contains(id) || PENDING_RESTORE.containsKey(id)) {
            // 外部重生（PlayerList.respawn）正在重建玩家：这条链上的删除必须放行，
            // 吞掉它会让旧玩家留在 level 里 → 新玩家撞 UUID 注册失败 → 幽灵实体 + 移动包 NPE。
            return true;
        }
        if (SNAPSHOTS.containsKey(id)) {
            return true;    // 同一 tick 内重复 die()：继续吞掉
        }
        logEnter("die 注入", player);
        if (!canRevive(player)) {
            logSkip("die 注入", player, "无复活状态（效果已消耗/过期，侧路注册表也为空）");
            clearImmediateRespawn(player);   // 已经彻底没有复活能力：恢复原版死亡界面行为
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            logSkip("die 注入", player, "server 为空");
            return false;
        }
        triggerRevive(player, "die 注入", server);
        return true;
    }

    /** 注入点 SynchedEntityData#set：血量被写入 &lt;= 0（ASM 改血级秒杀） */
    public static void onLethalHealthWrite(Object synched, Object value) {
        Entity entity;
        try {
            entity = ((SynchedEntityDataAccessor) synched).getEntity();
        } catch (Throwable t) {
            return;
        }
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        if (REVIVING.contains(id) || RESTORING.contains(id) || SNAPSHOTS.containsKey(id)) {
            return;
        }
        if (RESPAWNING.contains(id) || PENDING_RESTORE.containsKey(id)) {
            return;   // 外部重生流程中：血量由还原步骤统一写回
        }
        if (!canRevive(player)) {
            logSkip("血量写入注入", player, "无复活状态");
            return;
        }
        logEnter("血量写入注入", player);
        MinecraftServer server = player.getServer();
        if (server == null) {
            logSkip("血量写入注入", player, "server 为空");
            return;
        }
        triggerRevive(player, "血量写入注入", server);
    }

    /**
     * 注入点 Player#remove(RemovalReason)：删除级秒杀。
     * /kill → Entity.kill() → remove(KILLED)，完全不经过 hurt/setHealth/die，
     * 只有在这里拦才能救回来；其余 reason（DISCARDED/UNLOADED_* 等）一律放行。
     */
    public static boolean onRemove(Object obj, Object reason) {
        if (!(obj instanceof ServerPlayer player) || player.level().isClientSide()) {
            return false;
        }
        Entity.RemovalReason removalReason = reason instanceof Entity.RemovalReason r ? r : null;
        String reasonName = removalReason == null ? String.valueOf(reason) : removalReason.name();
        // KILLED：/kill、Entity.kill()、tickDeath；DISCARDED：别的 mod 用 discard()/删除实体来"秒杀"
        // 其余（UNLOADED_WITH_PLAYER 下线、CHANGED_DIMENSION 跨维度）一律放行。
        boolean killLike = removalReason == Entity.RemovalReason.KILLED
                || removalReason == Entity.RemovalReason.DISCARDED;
        if (!killLike) {
            return false;
        }
        UUID id = player.getUUID();
        if (REVIVING.contains(id) || RESTORING.contains(id) || SNAPSHOTS.containsKey(id)) {
            return false;
        }
        if (RESPAWNING.contains(id) || PENDING_RESTORE.containsKey(id)) {
            // 外部重生正在重建玩家对象：这次删除/丢弃是重建流程的一部分，必须放行
            return false;
        }
        if (!canRevive(player)) {
            logEnter("remove(" + reasonName + ") 注入", player);
            logSkip("remove(" + reasonName + ") 注入", player, "无复活状态");
            return false;
        }
        logEnter("remove(" + reasonName + ") 注入", player);
        MinecraftServer server = player.getServer();
        if (server == null) {
            logSkip("remove(" + reasonName + ") 注入", player, "server 为空");
            return false;
        }
        triggerRevive(player, "remove(" + reasonName + ") 注入", server);
        return true;   // 取消原版删除
    }

    /**
     * 注入点 LivingEntity#tickDeath：待复活玩家跳过死亡计时（否则 20t 后会被 remove(KILLED)）。
     * 若重生任务长时间未执行，则原地恢复，保证玩家一定活下来。
     *
     * 注意：这里跳过的只是"我们已经在处理的那一次死亡"的死亡计时（去重），
     * 不是无敌——重生一完成玩家立刻可以再被杀死。
     */
    public static boolean onTickDeath(Object obj) {
        if (!(obj instanceof ServerPlayer player)) {
            return false;
        }
        UUID id = player.getUUID();
        Snapshot snap = SNAPSHOTS.get(id);
        if (snap == null) {
            // 外部重生正在重建玩家：这次死亡计时会让原版把旧实体清掉，先跳过
            return PENDING_RESTORE.containsKey(id);
        }
        long now = player.level().getGameTime();
        if (snap.triggerTick >= 0L && now - snap.triggerTick > 5L) {
            LOGGER.warn("[Revival] 重生任务疑似卡住，原地恢复 {}", player.getGameProfile().getName());
            restoreInPlace(player, snap);
            SNAPSHOTS.remove(id);
            return false;
        }
        return true;
    }

    // ==================== 复活保护期（已删除） ====================
    // fix4 v9~v16 的"复活保护期/连杀保护"已按需求删除：不再有任何自制免伤与原地保命。
    // 现在每次致死都是真死亡 + 真重生；下面仅保留"我们已在处理的那次死亡"的去重判断。

    /**
     * 复活后是否保留原版重生自带的免伤。
     * 原版每次重生都会给新玩家 `spawnInvulnerableTime = 60`（3 秒完全免伤，
     * `ServerPlayer#hurt` 里大于 0 直接 return false）——那是原版为"重生保护"刻意做的。
     * 需求是"不需要刻意的无敌"，所以这里显式清零：
     * 复活后玩家立刻可以被再次杀死（每次都会走一遍真死亡 + 真重生）。
     */
    private static final boolean KEEP_VANILLA_RESPAWN_IMMUNITY = false;

    /** 按上面的策略处理原版重生免伤计数（int 字段，mojmap/SRG 双名查找） */
    private static void applyRespawnImmunityPolicy(ServerPlayer player) {
        if (KEEP_VANILLA_RESPAWN_IMMUNITY) {
            return;
        }
        try {
            Field f = findField(player.getClass(), "spawnInvulnerableTime", "f_8921_");
            if (f == null || UNSAFE == null) {
                return;
            }
            long offset = UNSAFE.objectFieldOffset(f);
            if (UNSAFE.getInt(player, offset) > 0) {
                UNSAFE.putInt(player, offset, 0);
            }
        } catch (Throwable t) {
            LOGGER.warn("[Revival] 清零原版重生免伤失败：{}", t.toString());
        }
    }

    /*
     * 【已移除】原地保命 / 连杀保护窗口。
     *
     * fix4 v9~v16 曾经在致死时"原地拉回血量并续期"，等价于给玩家一段刻意制造的无敌。
     * 真因（幽灵实体撞 UUID、最大血量被钳成负数、客户端自动重生风暴）都已修复，
     * 这层保护已按需求**彻底删除**：现在每一次致死都是真死亡 + 真重生，没有任何自制免伤。
     *
     * 也不再把 `invulnerableTime` 抬高：那是"受击冷却"，原版每次受击自己会设成 20，
     * 我们不再额外赠送。重生自带的原版免伤按上面的策略开关处理。
     */

    /** 外部重生待还原的任务体 */
    private static final class RestoreJob {
        final Snapshot snap;
        final ServerPlayer oldPlayer;

        RestoreJob(Snapshot snap, ServerPlayer oldPlayer) {
            this.snap = snap;
            this.oldPlayer = oldPlayer;
        }
    }

    // ==================== 通用判断 ====================

    /**
     * 是否拥有复活能力。
     * **主判定 = 玩家身上的 potionenchant:revival 药水效果**（护甲的药水附魔由
     * CombinedEffectHandler 持续刷成这个效果，所以穿护甲就等于一直有）。
     * 另两条只是兜底：
     *  1) 侧路注册表：每 tick 从实体效果同步，用于"死亡瞬间效果被清除"（例如不死图腾
     *     会 removeAllEffects）时仍能认出上一 tick 的状态；
     *  2) X护甲 NBT 等级：用于"护甲已应用附魔、但 CombinedEffectHandler 还没刷出效果"
     *     的同一 tick 竞态。只读，不扣减。
     *
     * 【修复】第 2 条必须同时要求**全套 X 护甲**：附魔 NBT 只存在头盔上，而效果只有全套时
     * 才会被刷（CombinedEffectHandler 里 isWearingFullXArmor 是前置条件）。以前只读头盔 NBT，
     * 导致"只戴一个 X 头盔"就能无限复活：没有效果、没有图标、也不消耗任何东西。
     */
    private static boolean canRevive(ServerPlayer player) {
        if (player.hasEffect(EffectRegistry.REVIVAL.get())) {
            return true;
        }
        if (REGISTRY.containsKey(player.getUUID())) {
            return true;
        }
        return ArmorXFeatureHandler.isWearingFullXArmor(player)
                && XArmorEnchantmentManager.getEnchantmentLevel(player, EffectRegistry.REVIVAL.get()) > 0;
    }

    /** 诊断日志（节流 5s）：区分"钩子没挂上"与"命中了但当时没有复活状态" */
    private static void logSkip(String hook, ServerPlayer player, String why) {
        long now = System.currentTimeMillis();
        String key = player.getUUID() + "|" + hook;
        Long last = SKIP_LOG.get(key);
        if (last != null && now - last < 5000L) {
            return;
        }
        SKIP_LOG.put(key, now);
        LOGGER.info("[Revival] {} 命中但未接管（{}）：{}", hook, player.getGameProfile().getName(), why);
    }

    /** 注入点入口诊断（节流 1s）：证明钩子确实被调用，并带上当时的复活状态 */
    private static void logEnter(String hook, ServerPlayer player) {
        long now = System.currentTimeMillis();
        String key = player.getUUID() + "|enter|" + hook;
        Long last = SKIP_LOG.get(key);
        if (last != null && now - last < 1000L) {
            return;
        }
        SKIP_LOG.put(key, now);
        LOGGER.info("[Revival] {} 被调用（{}）：注册表={} 实体效果={} X护甲等级={} 血量={} 冷却={}",
                hook, player.getGameProfile().getName(),
                REGISTRY.containsKey(player.getUUID()),
                player.hasEffect(EffectRegistry.REVIVAL.get()),
                XArmorEnchantmentManager.getEnchantmentLevel(player, EffectRegistry.REVIVAL.get()),
                player.getHealth(), isInRevivalCooldown(player));
    }

    public static boolean isInRevivalCooldown(Player player) {        if (player == null) {
            return false;
        }
        Long last = COOLDOWNS.get(player.getUUID());
        if (last == null) {
            return false;
        }
        MinecraftServer server = player.getServer();
        long now = server != null ? (long) server.getTickCount() : 0L;
        return (now - last) <= COOLDOWN_TICKS;
    }

    /** 统一入口：抓快照 → 记冷却 → 排队真重生（必须离开当前调用栈） */
    private static void triggerRevive(ServerPlayer player, String source, MinecraftServer server) {
        // 先告诉客户端"立即重生"：这样攻击方随后发的死亡界面包会走客户端的自动重生分支，
        // 既不弹界面也不需要玩家点（原版 doImmediateRespawn 的实现机制，见 sendImmediateRespawn）。
        sendImmediateRespawn(player);
        ReviveState st = REGISTRY.get(player.getUUID());
        Snapshot snap = Snapshot.capture(player, st);
        snap.triggerTick = player.level().getGameTime();
        SNAPSHOTS.put(player.getUUID(), snap);
        COOLDOWNS.put(player.getUUID(), (long) server.getTickCount());
        if (st != null) {
            st.triggerTick = snap.triggerTick;
            st.triggerMillis = System.currentTimeMillis();
        }
        // execute() 在服务端线程上会立即嵌套执行，必须用 tell(TickTask) 入队
        server.tell(new TickTask(server.getTickCount(), () -> revive(player, snap)));
        LOGGER.info("[Revival] 已接管 {} 的死亡（{}），排队真重生；死亡瞬间（全精度）：{}",
                player.getGameProfile().getName(), source, describeExact(player));
        LOGGER.info("[Revival] 已接管 {} 的死亡（{}）：死亡瞬间速度={} 坠落距离={} 脚下方块={} {}",
                player.getGameProfile().getName(), source, player.getDeltaMovement(),
                player.fallDistance, player.blockPosition().below(),
                player.level().getBlockState(player.blockPosition().below()));
    }

    // ==================== 玩家下线清理 ====================

    /**
     * 玩家下线：清掉所有按 UUID 记录的运行期状态（由 PlayerEvent.PlayerLoggedOutEvent 调用）。
     *
     * 【为什么必须清】这些 Map 以前从不清理。若在"复活窗口"内掉线/崩服，重登后标记还在：
     *  - `RESPAWNING` / `PENDING_RESTORE` 命中 → `ServerPlayer#die` 注入会当成"外部重生进行中"
     *    直接 **return true 吞掉真死亡**；
     *  - `LivingEntity#tickDeath` 注入也因 `PENDING_RESTORE` 返回 true → **死亡计时被跳过**；
     *  → 玩家卡在 0 血：不死、也不活（离线时 tick 救援根本不会跑，只能靠重登后 3 tick 救援）。
     * 另外这些表还会随玩家数无限增长（内存泄漏）。
     */
    public static void onPlayerLogout(Player player) {
        if (player == null) {
            return;
        }
        disarmDimensionSpoof(player instanceof ServerPlayer sp ? sp : null);
        UUID id = player.getUUID();
        REGISTRY.remove(id);
        SNAPSHOTS.remove(id);
        PENDING_RESTORE.remove(id);
        REVIVING.remove(id);
        RESTORING.remove(id);
        RESPAWNING.remove(id);
        COOLDOWNS.remove(id);
        PRE_HURT_HEALTH.remove(id);
        IMMEDIATE_RESPAWN_SENT.remove(id);
        IMMEDIATE_SUPPRESS.remove(id);
        RESPAWN_BURST.remove(id);
        CLIENT_RESPAWN_ALLOWED.remove(id);
        LAST_GOOD_MAX_HEALTH.remove(id);
        KEEP_INVENTORY_UNTIL.remove(id);
        String prefix = id + "|";
        SKIP_LOG.keySet().removeIf(k -> k.startsWith(prefix));
    }

    // ==================== 真重生 ====================

    private static void revive(ServerPlayer old, Snapshot snap) {
        MinecraftServer server = old.getServer();
        UUID id = old.getUUID();
        // 幂等：只有第一个到达者（队列任务 / tick 兜底）能取走快照
        if (SNAPSHOTS.remove(id) == null) {
            return;
        }
        if (PENDING_RESTORE.containsKey(id)) {
            // 外部重生（客户端的 PERFORM_RESPAWN / 别的 mod）已经在对这个玩家重生，
            // 并且认领了我们这份快照在还原。再自己 respawn 一次会连撞两次 UUID → 幽灵实体 + NPE。
            LOGGER.warn("[Revival] 已由外部重生接管本次复活，取消排队中的真重生 {}", old.getGameProfile().getName());
            // 【必须收尾】否则注册表条目会带着 triggerTick >= 0 永久留下 →
            // canRevive() 恒真 → 玩家此后无效果也每次都被接管、死亡界面永不弹出（无法死亡）。
            endReviveState(id);
            return;
        }
        if (server == null) {
            LOGGER.error("[Revival] server 为空，转为原地恢复");
            restoreInPlace(old, snap);   // restoreInPlace → restoreInto → endReviveState 收尾
            return;
        }

        ServerPlayer player;
        // 【先明确位置】respawn 之前就把旧玩家的坐标/视角钉到"落点"上（落点在抓快照时已算好）：
        // 这样原版 respawn 内部任何按"旧玩家当前状态"做的簿记与日志都和最终落点一致；
        // 新玩家对象的落点仍由 restoreInto 按同一个 Placement 精确写回（含最后那次 teleport）。
        try {
            snap.placement.applyTo(old);
        } catch (Throwable ignored) {
        }
        // 真重生全过程屏蔽钩子：PlayerList.respawn 内部会 remove(DISCARDED) 旧玩家、并写血量
        REVIVING.add(old.getUUID());
        // 武装维度伪装：紧跟其后的中转维度 respawn 包会被客户端折叠成当前（死亡）维度
        armDimensionSpoof(old);
        try {
            // keepEverything=true：走原版"保留全部数据"的重生分支。
            // 关键差别在 Forge 的 PlayerEvent.Clone(wasDeath)：false 传进去是 wasDeath=true，
            // 别的 mod（Curios 等）会按"真死亡"处理而清掉/丢掉自己的数据；true 则是普通换维度克隆。
            player = server.getPlayerList().respawn(old, true);
            LOGGER.info("[Revival] respawn 完成，新玩家对象已创建");
        } catch (Throwable t) {
            LOGGER.error("[Revival] 重生失败，转为原地恢复", t);
            restoreInPlace(old, snap);
            return;
        } finally {
            REVIVING.remove(old.getUUID());
        }
        if (player == null) {
            LOGGER.error("[Revival] 重生返回 null，转为原地恢复");
            restoreInPlace(old, snap);
            return;
        }

        // 连接持有者换人（原版只在 handleClientCommand(PERFORM_RESPAWN) 里做）
        player.connection.player = player;
        restoreInto(player, snap, old, "真重生");   // 内部 finally 会 endReviveState 收尾
        playReviveEffects(player);
        LOGGER.info("[Revival] {} 已完成真重生并恢复死亡前状态", player.getGameProfile().getName());
    }

    /**
     * 复活流程收尾（真重生成功 / 外部重生认领 / 原地恢复 / tickDeath 救援四个出口统一收尾）。
     *
     * 【历史 bug 修复】`ReviveState.triggerTick` 一旦被置为 >= 0，全工程再没有任何地方复位；
     * 而 `REGISTRY.remove` 以前只出现在"真重生成功"这一条路径上。于是另外三条路径都会把
     * 注册表条目**永久**留下 → `canRevive()` 因第 2 条判定恒真 →
     * 玩家此后即使完全没有效果/护甲，每次死亡也都被接管、且什么都不消耗（无限复活），
     * 同时 `clearImmediateRespawn` 永远不执行 → 死亡界面再也不出现（表现为"玩家无法死亡"）。
     *
     * 这里统一按"本次复活已结束"处理：直接移除条目。若玩家真的还有复活能力（药水还有等级、
     * 全套护甲附魔），下一 tick 的 {@link #onPlayerTick} 会立刻从实体效果重新写入注册表。
     */
    private static void endReviveState(UUID id) {
        KEEP_INVENTORY_UNTIL.remove(id);
        ReviveState st = REGISTRY.get(id);
        if (st == null || st.triggerTick < 0L) {
            // 不是"我们接管的这次复活"（例如别的 mod 对仍有复活能力的玩家调 respawn）：
            // 不动注册表，保留玩家原有的能力语义与效果被剥离时的兜底窗口。
            return;
        }
        SNAPSHOTS.remove(id);
        REGISTRY.remove(id);
    }

    /**
     * 把快照灌进玩家对象（真重生后的新对象 / 外部重生后的新对象 / 原地恢复共用）。
     * 步骤顺序有讲究：
     *  NBT 全量恢复 → 瞬态字段 → 维度切换 → 血量 → 载具 → 复活等级 → 客户端同步 → **最后**再补一次坐标朝向。
     * 最后一次 teleport 必须放在所有同步之后：客户端 handleRespawn 会把新玩家视角强制设成 -180°，
     * 只有"最后说话"才能压住它。
     */
    private static void restoreInto(ServerPlayer player, Snapshot snap, ServerPlayer old, String why) {
        MinecraftServer server = player.getServer();
        UUID id = player.getUUID();
        RESTORING.add(id);
        try {
            LOGGER.info("[Revival] [{}] 快照（死亡瞬间）: {}", why, describeSnapshot(snap));
            LOGGER.info("[Revival] [{}] 还原前：{}", why, describeExact(player));

            // 1) NBT 语义级全量恢复（Pos/Rotation/Inventory/EnderItems/XP/Food/Attributes/Effects/ForgeCaps…）
            try {
                player.load(snap.tag.copy());
            } catch (Throwable t) {
                LOGGER.error("[Revival] NBT 恢复失败", t);
            }

            // 2) NBT 覆盖不到的瞬态字段（abilities/itemCooldowns/useItem/attackStrengthTicker…）用 Unsafe 搬运
            if (old != null && old != player) {
                copyTransientFields(old, player);
            }

            // 3) 落点 + 视角：**用抓快照时就定好的 Placement**（不是现在才决定），
            //    连上一 tick 的位置也一起对齐（setOldPosAndRot），否则客户端会插值"滑"一下。
            Placement target = snap.placement;
            ServerLevel targetLevel = server != null ? server.getLevel(target.dimension) : null;
            boolean movedToSpawn = target.fallbackToSpawn;
            if (movedToSpawn) {
                // 死亡点在虚空/基岩以下：Placement 已经把坐标换成该维度的世界出生点，这里只补"抬到不卡墙的高度"
                target.applyTo(player);
                while (!player.serverLevel().noCollision(player)
                        && player.getY() < player.serverLevel().getMaxBuildHeight()) {
                    player.setPos(player.getX(), player.getY() + 1.0D, player.getZ());
                }
                LOGGER.warn("[Revival] [{}] 死亡点不安全(y={}, dim={})，改在世界出生点复活：{}",
                        why, snap.y, target.dimension.location(), describeExact(player));
            } else if (targetLevel != null && player.serverLevel() != targetLevel) {
                // 维度不同：先跨维度到落点维度，再统一钉坐标/视角
                player.teleportTo(targetLevel, target.x, target.y, target.z, target.yRot, target.xRot);
                target.applyTo(player);
            } else {
                target.applyTo(player);
            }

            // 4) 血量回到死亡前（先把被压坏的最大血量修回来，否则 setHealth 会被钳成负数）
            ensureHealthyMaxHealth(player);
            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            player.setHealth(Math.max(1.0F, Math.min(snap.preDeathHealth, maxHealth)));
            player.deathTime = 0;
            player.hurtTime = 0;
            // 不设 invulnerableTime（受击冷却）——那是原版每次受击自己会做的事，
            // 我们不再额外赠送无敌帧。原版重生自带的 3 秒免伤也按策略清零：
            applyRespawnImmunityPolicy(player);
            player.clearFire();
            player.fallDistance = 0.0F;

            // 5) 载具/乘客重挂
            try {
                if (snap.vehicle != null && !snap.vehicle.isRemoved() && player.getVehicle() == null) {
                    player.startRiding(snap.vehicle, true);
                }
            } catch (Throwable ignored) {
            }
            for (Entity passenger : snap.passengers) {
                try {
                    if (!passenger.isRemoved() && passenger.getVehicle() == null) {
                        passenger.startRiding(player, true);
                    }
                } catch (Throwable ignored) {
                }
            }

            // 6) 消耗一级复活
            applyRevivalLevel(player, snap);

            // 7) 客户端同步（血量/属性/背包全量）
            syncClient(player);

            // 7.5) 鼠标上"拿着的物品"：这是容器菜单状态（AbstractContainerMenu.carried），
            //      NBT 不保存，客户端重生后拿到的是全新菜单 → 必须补一次，否则复活后鼠标上的物品消失。
            //      containerId = -1 是原版约定的"设置 carried"（见 ClientPacketListener#handleContainerSetSlot）。
            if (!snap.carried.isEmpty()) {
                ItemStack carried = snap.carried.copy();
                player.containerMenu.setCarried(carried);
                player.connection.send(new ClientboundContainerSetSlotPacket(-1, 0, -1, carried.copy()));
            }

            // 8) 最后再"定死"一次坐标与视角：压住客户端 handleRespawn 强设的 -180°。
            //    落点用**预先定好的 Placement**（不是临时决定）；世界出生点分支只取实际抬高后的坐标。
            double fx = movedToSpawn ? player.getX() : target.x;
            double fy = movedToSpawn ? player.getY() : target.y;
            double fz = movedToSpawn ? player.getZ() : target.z;
            player.connection.teleport(fx, fy, fz, target.yRot, target.xRot);

            // 8.5) 血量校验：确认玩家真的活过来了。活不过来时**绝不能**发"立即重生"包，
            //      否则客户端进入"已死 + 不显示死亡界面"→ Minecraft#setScreen(null) 无限自动重生。
            float verified = player.getHealth();
            if (verified <= 0.0F) {
                ensureHealthyMaxHealth(player);
                float retry = Math.max(1.0F, Math.min(snap.preDeathHealth, Math.max(1.0F, player.getMaxHealth())));
                player.setHealth(retry);
                verified = player.getHealth();
                if (verified > 0.0F) {
                    syncClient(player);
                    player.connection.teleport(fx, fy, fz, target.yRot, target.xRot);
                }
            }
            if (verified > 0.0F) {
                // 9) 补一次"立即重生"标记：防止重生过程中某个 health=0 的包把客户端
                //    showDeathScreen 又设回 true（客户端 handleSetHealth 里 f==0 → true）
                sendImmediateRespawn(player);
            } else {
                LOGGER.error("[Revival] [{}] 还原后血量仍为 {}（最大血量 {}）→ 本次不发立即重生包，"
                                + "让客户端正常显示死亡界面，避免无限重生循环",
                        why, verified, player.getMaxHealth());
            }

            LOGGER.info("[Revival] [{}] 还原后：{}", why, describeExact(player));
            LOGGER.info("[Revival] [{}] 还原误差：{}", why, describeDrift(player, snap));
        } finally {
            RESTORING.remove(id);
            // 复活流程到此结束：解除维度伪装 + 统一收尾"进行中"状态
            // （修复注册表 triggerTick >= 0 永久泄漏）
            disarmDimensionSpoof(player);
            endReviveState(id);
        }
    }

    /**
     * 兜底：原地恢复（真重生失败或任务卡住时使用），保证玩家不死且客户端状态刷新。
     * 顺序要点：先发"重生包"让客户端重建玩家对象并关掉死亡界面，
     * **之后**再做血量/背包全量同步——反过来的话客户端刚收到的背包会被重生包重置掉。
     */
    private static void restoreInPlace(ServerPlayer player, Snapshot snap) {
        UUID id = player.getUUID();
        RESTORING.add(id);
        try {
            // 先关掉可能开着的容器：syncClient 里的 initInventoryMenu() 会把服务端 containerMenu
            // 换成背包菜单，若不发容器关闭包，客户端那一小段时间的点击会被服务端静默丢弃
            // （表现为"箱子点不动一下"）。原版真重生也有客户端侧 closeContainer 兜底，这里补服务端。
            closeOpenContainer(player);
            refreshClientLikeRespawn(player);   // 先让客户端重建玩家（并关闭死亡界面）
            restoreInto(player, snap, null, "原地恢复");
        } finally {
            RESTORING.remove(id);
            endReviveState(id);   // 幂等兜底（restoreInto 里已收尾；若它在早期抛异常这里补上）
        }
        LOGGER.warn("[Revival] {} 已原地恢复（未走真重生路径）", player.getGameProfile().getName());
    }

    /** 若玩家开着容器菜单则关闭（等价原版重生后客户端会做的 closeContainer，保证服务端菜单一致） */
    private static void closeOpenContainer(ServerPlayer player) {
        try {
            if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
        } catch (Throwable ignored) {
        }
    }

    /** 死亡点是否不安全（虚空/基岩以下）：避免复活后立刻再次致死 */
    private static boolean isUnsafeDeathSpot(ServerLevel level, double y) {
        return level != null && y < level.getMinBuildHeight() + 1.0D;
    }

    /**
     * 计算复活落点（**只在这里决定位置**）：默认原样保留死前坐标与视角；
     * 只有死亡点位于虚空/基岩以下（复活后会立刻再次致死）才改到该维度的世界出生点。
     */
    private static Placement resolvePlacement(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (isUnsafeDeathSpot(level, player.getY())) {
            BlockPos spawn = level.getSharedSpawnPos();
            return new Placement(level.dimension(),
                    spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot,
                    true);
        }
        return new Placement(level.dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot,
                false);
    }

    /**
     * 兜底：让客户端重新同步（等价于原版重生包），顺带关闭可能已打开的死亡界面
     * （客户端 handleRespawn 里 `if (screen instanceof DeathScreen) setScreen(null)`）。
     */
    private static void refreshClientLikeRespawn(ServerPlayer player) {
        try {
            player.connection.send(new ClientboundRespawnPacket(
                    player.level().dimensionTypeId(),
                    player.level().dimension(),
                    BiomeManager.obfuscateSeed(player.serverLevel().getSeed()),
                    player.gameMode.getGameModeForPlayer(),
                    player.gameMode.getPreviousGameModeForPlayer(),
                    player.level().isDebug(),
                    player.serverLevel().isFlat(),
                    ClientboundRespawnPacket.KEEP_ALL_DATA,
                    player.getLastDeathLocation(),
                    player.getPortalCooldown()));
            player.connection.teleport(player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        } catch (Throwable t) {
            LOGGER.error("[Revival] 兜底客户端刷新失败：{}", t.toString());
        }
    }

    /**
     * 消耗一次复活。
     * 判定与消耗都以**玩家药水效果**为准（本模组的设计：护甲带药水附魔 → 持续给玩家上效果）：
     * 效果等级 > 0 就降一级，等级为 0 就移除。
     * X护甲的附魔等级**不做扣减**——护甲在，效果就会被 CombinedEffectHandler 持续刷新，
     * 也就是"护甲在就能一直复活"，与模组本身的药水附魔语义一致。
     */
    private static void applyRevivalLevel(ServerPlayer player, Snapshot snap) {
        MobEffect revival = EffectRegistry.REVIVAL.get();
        player.removeEffect(revival);
        if (snap.amplifier > 0) {
            player.addEffect(new MobEffectInstance(
                    revival, snap.durationTicks, snap.amplifier - 1,
                    snap.ambient, snap.visible, snap.showIcon));
        }
    }

    /**
     * 恢复后的数据补发（血量/属性/背包/热键槽/经验/药水效果/能力）。
     *
     * 为什么必须要这一串：NBT 语义恢复（`Entity#load`）只改**服务端**对象，
     * 这些数据原版是在别处按需发包的，重生流程里根本不会补：
     *  - 背包/容器：只有登录与打开界面时发全量，重生不发 → 手动 `broadcastFullState`
     *  - 主手热键槽：`ClientboundSetCarriedItemPacket` 只在登录发（PlayerList L187）→
     *    **不补的话客户端就一直停在第 0 格**，而且客户端一用物品就会把 0 格回传给服务端
     *  - 药水效果：`LivingEntity#readAdditionalSaveData` 是直接往 `activeEffects` map 里塞
     *    （L710），不发任何包 → 客户端看不到图标也拿不到效果逻辑
     *  - 经验：重生包在恢复**之前**发过一次（值是旧的 0），这里补一次确保正确
     */
    private static void syncClient(ServerPlayer player) {
        try {
            player.connection.send(new ClientboundSetHealthPacket(
                    player.getHealth(),
                    player.getFoodData().getFoodLevel(),
                    player.getFoodData().getSaturationLevel()));
            player.connection.send(new ClientboundUpdateAttributesPacket(
                    player.getId(), player.getAttributes().getSyncableAttributes()));
            // 主手热键槽（修复"复活后回到第一格"）
            player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
            // 经验（重生包里那次是恢复前的旧值）
            player.connection.send(new ClientboundSetExperiencePacket(
                    player.experienceProgress, player.totalExperience, player.experienceLevel));
            // 药水效果（NBT 恢复不走 addEffect，所以必须逐条补发）
            for (MobEffectInstance effect : player.getActiveEffects()) {
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
            }
            // 先关掉可能开着的容器：initInventoryMenu() 会把服务端 containerMenu 换成背包菜单，
            // 不先关的话客户端那一小段会对着"已经不存在的容器"发点击包，被服务端静默丢弃
            closeOpenContainer(player);
            player.initInventoryMenu();
            player.inventoryMenu.broadcastFullState();
            resyncItemCooldowns(player);
            player.onUpdateAbilities();
        } catch (Throwable t) {
            LOGGER.error("[Revival] 客户端同步失败", t);
        }
    }

    /**
     * 补发物品冷却（末影珍珠/盾牌这类）。
     *
     * 我们的 Unsafe 瞬态搬运会把整个 `ItemCooldowns` 对象带过来（连同它自己的 tickCount），
     * 服务端冷却因此是连续的；但客户端那个新建的 LocalPlayer 冷却表是空的，
     * 于是"服务端还在冷却、客户端显示可用"。这里按剩余时长逐条补发。
     *
     * 全程反射 + try/catch：拿不到就静默跳过（只是显示问题，不影响服务端判定）。
     */
    private static void resyncItemCooldowns(ServerPlayer player) {
        try {
            Object cooldowns = player.getCooldowns();   // ItemCooldowns（服务端是 ServerItemCooldowns）
            if (cooldowns == null || UNSAFE == null) {
                return;
            }
            Field mapField = findField(cooldowns.getClass(), "cooldowns", "f_41515_");
            Field tickField = findField(cooldowns.getClass(), "tickCount", "f_41516_");
            if (mapField == null || tickField == null) {
                return;
            }
            Object mapObj = UNSAFE.getObject(cooldowns, UNSAFE.objectFieldOffset(mapField));
            if (!(mapObj instanceof Map<?, ?> map) || map.isEmpty()) {
                return;
            }
            int now = UNSAFE.getInt(cooldowns, UNSAFE.objectFieldOffset(tickField));
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof net.minecraft.world.item.Item item)) {
                    continue;
                }
                Object instance = entry.getValue();
                // CooldownInstance{startTime, endTime}：两个字段都读，取较大的那个当结束刻（对映射顺序不敏感）
                int end = 0;
                for (String[] names : new String[][]{{"startTime", "f_41533_"}, {"endTime", "f_41534_"}}) {
                    Field f = findField(instance.getClass(), names[0], names[1]);
                    if (f != null) {
                        end = Math.max(end, UNSAFE.getInt(instance, UNSAFE.objectFieldOffset(f)));
                    }
                }
                int remaining = end - now;
                if (remaining > 0) {
                    player.connection.send(new ClientboundCooldownPacket(item, remaining));
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[Revival] 补发物品冷却失败（忽略）：{}", t.toString());
        }
    }

    private static void playReviveEffects(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.5F);
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 25; i++) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        player.getX() + (Math.random() - 0.5) * 2.1,
                        player.getY() + player.getEyeHeight() + (Math.random() - 0.5) * 2.1,
                        player.getZ() + (Math.random() - 0.5) * 2.1,
                        1, 0.0, 0.0, 0.0, 0.05);
            }
        }
    }

    // ==================== Unsafe：NBT 覆盖不到的瞬态字段 ====================

    private static final Unsafe UNSAFE = DiexvBase.UNSAFE;

    /**
     * NBT（Entity#load）不包含的瞬态字段。
     * 每项是 {mojmap 名, SRG 名}：生产环境运行的是 SRG 名字段（f_xxxxx_），
     * 只按 mojmap 名反射会全部落空（实测 "搬运 0 个"）。
     */
    private static final String[][] TRANSIENT_FIELDS = {
            {"abilities", "f_36077_"},
            {"cooldowns", "f_36087_"},
            {"takeXpDelay", "f_36101_"},
            {"useItem", "f_20935_"},
            {"useItemRemaining", "f_20936_"},
            {"swinging", "f_20911_"},
            {"swingingArm", "f_20912_"},
            {"swingTime", "f_20913_"},
            {"attackStrengthTicker", "f_20922_"},
            {"autoSpinAttackTicks", "f_20938_"},
            {"lastHurt", "f_20898_"},
            {"hurtDir", "f_263750_"},
            {"lastHurtByPlayer", "f_20888_"},
            {"lastHurtByPlayerTime", "f_20889_"},
            {"lastHurtByMob", "f_20949_"},
            {"lastHurtByMobTimestamp", "f_20950_"},
            {"lastHurtMob", "f_20951_"},
            {"lastHurtMobTimestamp", "f_20952_"},
            {"noActionTime", "f_20891_"},
            {"attackAnim", "f_20921_"},
            {"oAttackAnim", "f_20920_"},
            {"walkDist", "f_19787_"},
            {"walkDistO", "f_19867_"},
            {"xo", "f_19854_"},
            {"yo", "f_19855_"},
            {"zo", "f_19856_"},
            {"yRotO", "f_19859_"},
            {"xRotO", "f_19860_"},
            {"fallDistance", "f_19789_"},
            // 注意：这里**故意不搬运** `spawnInvulnerableTime`（原版重生免伤计数）。
            // 搬运它会把旧玩家已经递减到 0 的值带过来，等于"顺手"取消了原版重生保护；
            // 这个字段现在由 applyRespawnImmunityPolicy() 按显式策略处理。
    };

    /** 用 Unsafe 把旧玩家对象上的瞬态字段直接搬到新玩家对象（含 final 字段；mojmap/SRG 双名查找） */
    private static void copyTransientFields(Object from, Object to) {
        if (from == null || to == null || UNSAFE == null) {
            LOGGER.warn("[Revival] Unsafe 不可用，跳过瞬态字段搬运");
            return;
        }
        int copied = 0;
        for (String[] names : TRANSIENT_FIELDS) {
            Field field = findField(from.getClass(), names[0], names[1]);
            if (field == null) {
                continue;
            }
            try {
                long offset = UNSAFE.objectFieldOffset(field);
                Class<?> type = field.getType();
                if (type == int.class) {
                    UNSAFE.putInt(to, offset, UNSAFE.getInt(from, offset));
                } else if (type == float.class) {
                    UNSAFE.putFloat(to, offset, UNSAFE.getFloat(from, offset));
                } else if (type == boolean.class) {
                    UNSAFE.putBoolean(to, offset, UNSAFE.getBoolean(from, offset));
                } else if (type == double.class) {
                    UNSAFE.putDouble(to, offset, UNSAFE.getDouble(from, offset));
                } else if (type == long.class) {
                    UNSAFE.putLong(to, offset, UNSAFE.getLong(from, offset));
                } else if (!type.isPrimitive()) {
                    UNSAFE.putObject(to, offset, UNSAFE.getObject(from, offset));
                } else {
                    continue;
                }
                copied++;
            } catch (Throwable t) {
                LOGGER.warn("[Revival] 搬运字段 {} 失败：{}", names[0], t.toString());
            }
        }
        LOGGER.info("[Revival] Unsafe 瞬态字段搬运完成：{} 个", copied);
    }

    /** 沿父类链按 mojmap / SRG 双名查找字段（生产环境字段名是 f_xxxxx_） */
    private static Field findField(Class<?> type, String mojang, String srg) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Field f = declaredField(c, mojang);
            if (f != null) {
                return f;
            }
            f = declaredField(c, srg);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    private static Field declaredField(Class<?> c, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            Field f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ==================== 快照 ====================

    /**
     * 复活落点：**在抓快照的那一刻就先定好**（维度 + 坐标 + 视角），之后 respawn / 还原只按它执行。
     *
     * 为什么提前定：原版 {@code PlayerList#respawn} 自己会先把新玩家摆到"重生点/世界出生点"并发一次
     * 客户端传送包，我们随后才把玩家拉回死前位置 —— 顺序上就是"先重生点、再拉回"。
     * 把落点提前算好并由本模组统一执行，可以让 respawn 之后**没有第二次位置决策**，
     * 客户端在同一 tick 内只看到最终落点（见 {@link #onClientCommand} 里"接管期间忽略客户端重生请求"）。
     */
    static final class Placement {
        final ResourceKey<Level> dimension;
        final double x;
        final double y;
        final double z;
        final float yRot;
        final float xRot;
        final float yHeadRot;
        final float yBodyRot;
        /** true = 死亡点不安全（虚空/基岩以下），落点已改成该维度的世界出生点 */
        final boolean fallbackToSpawn;

        Placement(ResourceKey<Level> dimension, double x, double y, double z,
                  float yRot, float xRot, float yHeadRot, float yBodyRot, boolean fallbackToSpawn) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.yHeadRot = yHeadRot;
            this.yBodyRot = yBodyRot;
            this.fallbackToSpawn = fallbackToSpawn;
        }

        /** 把落点写进实体（坐标 + 四个朝向 + 上一 tick 位置对齐），并把速度清零 */
        void applyTo(Entity entity) {
            entity.setPos(x, y, z);
            entity.setYRot(yRot);
            entity.setXRot(xRot);
            entity.setYHeadRot(yHeadRot);
            entity.setYBodyRot(yBodyRot);
            entity.setOldPosAndRot();
            entity.setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public String toString() {
            return String.format("dim=%s pos=(%.10f, %.10f, %.10f) rot=(%.6f, %.6f) 头=%.6f 身=%.6f%s",
                    dimension.location(), x, y, z, yRot, xRot, yHeadRot, yBodyRot,
                    fallbackToSpawn ? "（死亡点不安全 → 世界出生点）" : "");
        }
    }

    private static final class Snapshot {

        final CompoundTag tag;
        final ResourceKey<Level> dimension;
        final int amplifier;
        final int durationTicks;
        final boolean ambient;
        final boolean visible;
        final boolean showIcon;
        final float preDeathHealth;
        final Entity vehicle;
        final List<Entity> passengers;
        /** **预先定好**的复活落点（维度/坐标/视角）；还原时只按它执行 */
        final Placement placement;
        /**
         * 死亡瞬间鼠标上"拿着的物品"（{@code AbstractContainerMenu.carried}）。
         * 这是容器菜单状态、不进 NBT，客户端重生后拿到的是**全新菜单**（carried 为空）→
         * 不补发就会"复活后鼠标上的物品消失"。
         */
        final ItemStack carried;
        /**
         * 死亡瞬间的**精确**坐标与朝向，直接取自实体字段（double / float 全精度），
         * 不经过 NBT / 百分比格式化。还原时用它们"钉"一遍，避免任何中间环节的取整。
         */
        final double x;
        final double y;
        final double z;
        final float yRot;
        final float xRot;
        final float yHeadRot;
        final float yBodyRot;
        /** 死亡瞬间的速度（仅用于记录；还原时按安全策略清零，防落地摔伤） */
        final Vec3 motion;
        long triggerTick = -1L;

        private Snapshot(CompoundTag tag, ResourceKey<Level> dimension, int amplifier, int durationTicks,
                         boolean ambient, boolean visible, boolean showIcon, float preDeathHealth,
                         Entity vehicle, List<Entity> passengers, ItemStack carried, Placement placement,
                         double x, double y, double z,
                         float yRot, float xRot, float yHeadRot, float yBodyRot, Vec3 motion) {
            this.tag = tag;
            this.dimension = dimension;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.ambient = ambient;
            this.visible = visible;
            this.showIcon = showIcon;
            this.preDeathHealth = preDeathHealth;
            this.vehicle = vehicle;
            this.passengers = passengers;
            this.carried = carried;
            this.placement = placement;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.yHeadRot = yHeadRot;
            this.yBodyRot = yBodyRot;
            this.motion = motion;
        }

        static Snapshot capture(ServerPlayer player, ReviveState state) {
            CompoundTag tag = new CompoundTag();
            player.saveWithoutId(tag);

            // 血量：实时值优先（血量写入注入点在新值落库前回调），其次 hurt 缓存
            float live = player.getHealth();
            Float cached = PRE_HURT_HEALTH.remove(player.getUUID());
            float health = live > 0.0F ? live : (cached != null ? cached : player.getMaxHealth());
            if (!(health > 0.0F)) {
                health = player.getMaxHealth();
            }

            // 复活等级：侧路注册表优先（即使效果此刻已被清空也拿得到）
            MobEffectInstance inst = player.getEffect(EffectRegistry.REVIVAL.get());
            int amplifier = state != null ? state.amplifier : (inst != null ? inst.getAmplifier() : 0);
            int duration = state != null ? state.durationTicks : (inst != null ? inst.getDuration() : 0);
            boolean ambient = state != null ? state.ambient : (inst != null && inst.isAmbient());
            boolean visible = state != null ? state.visible : (inst != null && inst.isVisible());
            boolean showIcon = state != null ? state.showIcon : (inst != null && inst.showIcon());

            // 鼠标上拿着的物品（容器菜单状态；不抓的话复活后它会消失）
            ItemStack carried = ItemStack.EMPTY;
            try {
                if (player.containerMenu != null && !player.containerMenu.getCarried().isEmpty()) {
                    carried = player.containerMenu.getCarried().copy();
                }
            } catch (Throwable ignored) {
            }

            // 【先明确位置】落点在这里一次算清：安全就原地保留死前坐标/视角，落进虚空/基岩以下才退回世界出生点。
            // 之后 respawn 与还原都只按这个 Placement 执行，不再临时决定。
            Placement placement = resolvePlacement(player);

            return new Snapshot(tag, player.level().dimension(), amplifier, duration,
                    ambient, visible, showIcon, health,
                    player.getVehicle(), new ArrayList<>(player.getPassengers()), carried, placement,
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot,
                    player.getDeltaMovement());
        }
    }
}
