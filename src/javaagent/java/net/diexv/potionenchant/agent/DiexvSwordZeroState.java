package net.diexv.potionenchant.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * agent 自包含的"目标归零"状态与 helper（bootstrap 可见，被字节码注入引用）。
 *
 * 平移自 DiexvSword V6 的"百分比伤害 redefine"实现（DiexvAgent）：
 * SynchedEntityData.set 方法头注入三段逻辑，调用本类三个 helper——
 *  - shouldApplyPercentageDamage(synched)：目标实体判定
 *  - getEntityDamageAmount(synched)：返回 Float.MAX_VALUE——伤害无穷大，
 *    注入的 value = max(value - MAX, 0) 必然归 0
 *  - consumePercentageDamage(synched)：无操作（目标已归零）
 *
 * 参数用 Object（agent 不编译依赖 Minecraft 类；SynchedEntityData 实例是 Object 子类，
 * 注入描述符与运行时栈类型兼容）。主模组通过反射桥 DiexvSwordAgentBridge 把目标
 * UUID 加入本集合。
 */
public final class DiexvSwordZeroState {

    private static final Set<UUID> TARGETS = ConcurrentHashMap.newKeySet();

    private DiexvSwordZeroState() {}

    // ==================== 目标集合 ====================

    private static volatile long lastAddLogMs = 0L;

    /** 主模组反射桥调用：标记目标（节流日志，避免连续标记刷屏） */
    public static void add(UUID uuid) {
        if (uuid != null) {
            TARGETS.add(uuid);
            long now = System.currentTimeMillis();
            if (now - lastAddLogMs >= 2000L) {
                lastAddLogMs = now;
                System.out.println("[DiexvSwordZeroState] 添加目标: " + uuid + " (总数=" + TARGETS.size() + ")");
            }
        }
    }

    /** 主模组反射桥调用：取消标记 */
    public static void remove(UUID uuid) {
        if (uuid != null) {
            TARGETS.remove(uuid);
        }
    }

    // ==================== 百分比伤害 redefine 的三个 helper（V6 结构，语义=目标归零） ====================

    /** 注入代码调用（SynchedEntityData.set 方法头）：是否对当前数据项应用"归零"（目标实体） */
    public static boolean shouldApplyPercentageDamage(Object synched) {
        return isTargetEntity(entityOf(synched));
    }


    // ==================== 内部反射（双名 + 缓存） ====================

    private static final ConcurrentHashMap<Class<?>, Field> ENTITY_FIELDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> UUID_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Object, UUID> UUID_CACHE = new ConcurrentHashMap<>();

    /** SynchedEntityData 中持有实体的字段：entity / f_135344_（dev / prod） */
    private static Object entityOf(Object synched) {
        if (synched == null) return null;
        Class<?> c = synched.getClass();
        Field f = ENTITY_FIELDS.get(c);
        if (f == null && !ENTITY_FIELDS.containsKey(c)) {
            f = findField(c, "entity", "f_135344_");
            ENTITY_FIELDS.put(c, f);
        }
        if (f == null) return null;
        try {
            return f.get(synched);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isTargetEntity(Object entity) {
        if (entity == null) return false;
        // 快速路径：无任何标记目标 -> 零开销直接返回
        if (TARGETS.isEmpty()) return false;
        // UUID 判定（服务端/客户端实体的 UUID 一致，跨端可靠）；实例->UUID 缓存（无锁）
        UUID uuid = UUID_CACHE.get(entity);
        if (uuid == null) {
            uuid = uuidOf(entity);
            if (uuid != null) {
                UUID_CACHE.put(entity, uuid);
            }
        }
        return uuid != null && TARGETS.contains(uuid);
    }

    /** Entity.getUUID()：getUUID / m_20148_（dev / prod） */
    private static UUID uuidOf(Object entity) {
        Class<?> c = entity.getClass();
        Method m = UUID_METHODS.get(c);
        if (m == null && !UUID_METHODS.containsKey(c)) {
            m = findMethod(c, "getUUID", "m_20148_");
            UUID_METHODS.put(c, m);
        }
        if (m == null) return null;
        try {
            return (UUID) m.invoke(entity);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field findField(Class<?> c, String mojang, String srg) {
        try {
            Field f = c.getDeclaredField(mojang);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e1) {
            try {
                Field f = c.getDeclaredField(srg);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e2) {
                return null;
            }
        }
    }

    private static Method findMethod(Class<?> c, String mojang, String srg) {
        try {
            return c.getMethod(mojang);
        } catch (NoSuchMethodException e1) {
            try {
                return c.getMethod(srg);
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }
}