package net.diexv.potionenchant.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 复活效果：只作为"持有复活次数"的标记（每级 = 一次复活机会）。
 *
 * 死亡接管与真重生由 RevivalManager + agent 字节码注入（ServerPlayer#die /
 * LivingEntity#hurt）完成，本效果不注册任何 Forge 事件处理器、不使用 Mixin。
 */
public class RevivalEffect extends MobEffect {

    public RevivalEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF69B4);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 无需周期逻辑：触发点在死亡注入处
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /** 兼容入口：复活冷却查询（真正的实现在 RevivalManager，靠注入点的 tick 记录） */
    public static boolean isInRevivalCooldown(Player player) {
        return net.diexv.potionenchant.util.RevivalManager.isInRevivalCooldown(player);
    }
}
