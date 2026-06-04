package net.diexv.potionenchant.mixin.accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> HEALTH() {
        return null;
    }

    @Accessor("dead")
    boolean dead();

    @Accessor("dead")
    void setDead(boolean dead);

    @Accessor("deathScore")
    int deathScore();
}