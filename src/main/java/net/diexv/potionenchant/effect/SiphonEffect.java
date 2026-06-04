package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class SiphonEffect extends MobEffect {

    public SiphonEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00008B);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (!attacker.hasEffect(EffectRegistry.SIPHON.get())) return;

            int amplifier = attacker.getEffect(EffectRegistry.SIPHON.get()).getAmplifier();

            // Use target''s actual health loss (capped at remaining health, not overkill)
            float damage = event.getAmount();
            float targetHealth = event.getEntity().getHealth();
            float actualDamage = Math.min(damage, targetHealth);

            if (actualDamage <= 0) return;

            // Lifesteal: 5% base + 5% per level
            float lifeStealPercent = 0.05f + (amplifier * 0.05f);
            float lifeStealAmount = actualDamage * lifeStealPercent;

            // Heal attacker
            attacker.heal(lifeStealAmount);

            // Player bonus: food + XP
            if (attacker instanceof Player player) {
                FoodData foodData = player.getFoodData();
                if (foodData.getFoodLevel() < 20) {
                    foodData.eat((int) lifeStealAmount, lifeStealAmount * 0.05f);
                } else {
                    float newSat = foodData.getSaturationLevel() + lifeStealAmount;
                    foodData.setSaturation(Math.min(newSat, 20.0F));
                }
                int expAmount = (int) (actualDamage * (0.1 + amplifier * 0.1));
                player.giveExperiencePoints(expAmount);
            }
        }
    }
}
