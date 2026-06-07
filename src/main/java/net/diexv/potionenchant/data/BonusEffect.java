package net.diexv.potionenchant.data;

import net.minecraft.world.effect.MobEffect;

public class BonusEffect {
    public final MobEffect effect;
    public final int level;

    public BonusEffect(MobEffect effect, int level) {
        this.effect = effect;
        this.level = level;
    }
}
