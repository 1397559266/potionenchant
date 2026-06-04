package net.diexv.potionenchant.util;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 追踪被 XSword 毁灭模式标记的实体。
 * 被标记的实体所有 setHealth/getHealth/SynchedEntityData 将被强制返回 0。
 * 使用 WeakHashMap 自动 GC 清理。
 */
public class XSwordTargetTracker {

    private static final Map<Entity, Boolean> MARKED = new WeakHashMap<>();

    public static void mark(Entity entity) {
        MARKED.put(entity, true);
    }

    public static boolean isMarked(Entity entity) {
        return MARKED.containsKey(entity);
    }

    public static void unmark(Entity entity) {
        MARKED.remove(entity);
    }
}
