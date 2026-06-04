package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraft.world.entity.player.Player;

public class MilkDrinkHandler {

    @SubscribeEvent
    public void onMilkDrink(LivingEntityUseItemEvent.Finish event) {
        // 检查是否是玩家饮用牛奶
        if (event.getEntity() instanceof Player player) {
            // 检查饮用的是否是牛奶
            if (event.getItem().getItem() == Items.MILK_BUCKET) {
                // 检查副手物品是否有药水附魔
                ItemStack offhandItem = player.getOffhandItem();
                if (!offhandItem.isEmpty() && PotionEnchantManager.hasPotionEnchantments(offhandItem)) {
                    // 清除副手物品的药水附魔
                    PotionEnchantManager.clearPotionEnchantments(offhandItem);
                }
            }
        }
    }
}