package net.diexv.potionenchant.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 共生效果
 * 持有此效果的实体受到伤害时，攻击者也会受到同等伤害；
 * 持有者被治疗时，治疗者也会获得同等治疗。
 */
public class SymbiosisEffect extends MobEffect {

    public SymbiosisEffect() {
        super(MobEffectCategory.NEUTRAL, 0xDC143C);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
