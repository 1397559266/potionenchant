package net.diexv.potionenchant.event;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EntityDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // 检查实体是否有圣洁效果
        if (entity.hasEffect(EffectRegistry.SANCTUARY.get())) {
            // 死亡时允许移除圣洁效果
            // 这里不需要做任何事，原版死亡逻辑会自动清除所有效果
        }
    }
}
