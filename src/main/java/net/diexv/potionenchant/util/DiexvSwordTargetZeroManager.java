package net.diexv.potionenchant.util;

import net.diexv.potionenchant.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 追踪被 DiexvSword 归零机制标记的目标实体。
 *
 * 被标记的目标：SynchedEntityData.set/get 与 setHealth/getHealth 全部强制归 0
 * （由动态附加的 mixin 实现），目标完全瘫痪/死亡。
 *
 * 触发来源：剑左键点击目标、激光命中范围内的目标。
 * 持剑玩家自身（以及任何手持 diexvsword 的玩家）不会被标记。
 * 使用 WeakHashMap 自动 GC 清理（实体死亡移除后无引用即回收）。
 */
public final class DiexvSwordTargetZeroManager {

    private static final Map<Entity, Boolean> TARGETS = new WeakHashMap<>();

    private DiexvSwordTargetZeroManager() {}

    /** 是否为目标（归零对象） */
    public static boolean isTarget(Entity entity) {
        return entity != null && TARGETS.containsKey(entity);
    }

    /** 标记目标（自动排除手持 diexvsword 的玩家自身）；同时通知 agent 字节码注入归零 */
    public static void mark(Entity entity) {
        if (entity == null) return;
        if (isSwordBearer(entity)) return;
        // redefine 触发点：确保 agent 对 SynchedEntityData/LivingEntity 的归零注入已就位
        // （加载期已改写时为幂等空操作；否则此刻对已加载类执行 redefine）
        DiexvSwordAgentBridge.ensureRedefined();
        TARGETS.put(entity, true);
        DiexvSwordAgentBridge.markTarget(entity);
    }

    /** 取消标记 */
    public static void unmark(Entity entity) {
        if (entity != null) {
            TARGETS.remove(entity);
            DiexvSwordAgentBridge.unmarkTarget(entity);
        }
    }

    /** 是否手持 diexvsword（主手或副手） */
    public static boolean isSwordBearer(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return (main.getItem() == ModItems.DIEXV_SWORD.get())
                || (off.getItem() == ModItems.DIEXV_SWORD.get());
    }
}
