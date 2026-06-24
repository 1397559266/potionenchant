package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.EnchantmentRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class AdvancedPowerHandler {

    // 原版力量附魔每级增加25%伤害，高级力量是5倍，所以每级增加125%伤害
    private static final float DAMAGE_BONUS_PER_LEVEL = 1.25f;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // 检查新加入的实体是否是箭矢
        if (event.getEntity() instanceof AbstractArrow arrow) {
            // 检查箭矢的发射者是否是玩家
            if (arrow.getOwner() instanceof Player player) {
                // 检查玩家是否使用了带有高级力量附魔的弓或弩
                ItemStack weapon = player.getMainHandItem();
                if ((weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem)) {
                    int advancedPowerLevel = EnchantmentHelper.getItemEnchantmentLevel(EnchantmentRegistry.ADVANCED_POWER.get(), weapon);

                    if (advancedPowerLevel > 0) {
                        // 计算伤害加成
                        float damageBonus = getDamageBonus(advancedPowerLevel);
                        float baseDamage = (float) arrow.getBaseDamage();
                        float newDamage = baseDamage * (1.0f + damageBonus);

                        // 应用伤害加成
                        arrow.setBaseDamage(newDamage);
                    }
                }
            }
        }
    }

    // 获取高级力量附魔的伤害加成
    public static float getDamageBonus(int level) {
        if (level <= 0) return 0.0f;
        return DAMAGE_BONUS_PER_LEVEL * level;
    }

    // 获取高级力量附魔的总伤害倍数
    public static float getDamageMultiplier(int level) {
        if (level <= 0) return 1.0f;
        return 1.0f + (DAMAGE_BONUS_PER_LEVEL * level);
    }
}


