package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EnchantmentEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            LivingEntity target = event.getEntity();

            // 检查攻击者主手物品是否有烈焰附加附魔
            int blazeAspectLevel = EnchantmentHelper.getEnchantmentLevel(PotionEnchantMod.BLAZE_ASPECT.get(), attacker);

            if (blazeAspectLevel > 0 && target.isOnFire()) {
                // 每级提高50%伤害
                float damageMultiplier = 1.0f + (0.5f * blazeAspectLevel);
                float newDamage = event.getAmount() * damageMultiplier;
                event.setAmount(newDamage);
            }
        }
    }
}
