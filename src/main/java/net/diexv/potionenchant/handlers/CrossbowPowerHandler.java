package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.EnchantmentRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class CrossbowPowerHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // 检查新加入的实体是否是箭矢
        if (event.getEntity() instanceof AbstractArrow arrow) {
            // 检查箭矢的发射者是否是玩家
            if (arrow.getOwner() instanceof Player player) {
                // 检查玩家是否使用了带有高级力量附魔的弩
                ItemStack crossbow = player.getMainHandItem();
                if (crossbow.getItem() instanceof CrossbowItem) {
                    int advancedPowerLevel = EnchantmentHelper.getItemEnchantmentLevel(EnchantmentRegistry.ADVANCED_POWER.get(), crossbow);

                    if (advancedPowerLevel > 0) {
                        // 计算伤害加成
                        float damageBonus = AdvancedPowerHandler.getDamageBonus(advancedPowerLevel);
                        float baseDamage = (float) arrow.getBaseDamage();
                        float newDamage = baseDamage * (1.0f + damageBonus);

                        // 应用伤害加成
                        arrow.setBaseDamage(newDamage);
                    }
                }
            }
        }
    }
}


