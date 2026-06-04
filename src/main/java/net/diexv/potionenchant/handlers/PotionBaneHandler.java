package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.enchantments.PotionBaneEnchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class PotionBaneHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 检查伤害来源是否是玩家
        if (event.getSource().getDirectEntity() instanceof Player attacker) {
            // 检查目标是否有药水效果
            if (PotionBaneEnchantment.hasPotionEffects(event.getEntity())) {
                // 检查攻击者武器是否有药剂克星附魔
                ItemStack weapon = attacker.getMainHandItem();
                int potionBaneLevel = EnchantmentHelper.getItemEnchantmentLevel(
                        PotionEnchantMod.POTION_BANE.get(), weapon);

                if (potionBaneLevel > 0) {
                    // 计算伤害加成
                    float damageMultiplier = 1.0f + PotionBaneEnchantment.getDamageBonus(potionBaneLevel);
                    float newDamage = event.getAmount() * damageMultiplier;
                    event.setAmount(newDamage);
                }
            }
        }
    }
}
