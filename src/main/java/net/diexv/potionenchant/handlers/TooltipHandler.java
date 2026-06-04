package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class TooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        // 检查物品是否是盔甲并且有伤害储存附魔
        if (player != null && stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
            int storageLevel = EnchantmentHelper.getItemEnchantmentLevel(PotionEnchantMod.DAMAGE_STORAGE.get(), stack);

            if (storageLevel > 0) {
                // 获取玩家储存的伤害量
                float storedDamage = DamageStorageHandler.getStoredDamage(player);
                float maxStorage = DamageStorageHandler.getMaxStorage(player);

                // 添加工具提示行
                event.getToolTip().add(Component.literal(""));
                event.getToolTip().add(Component.translatable("tooltip.plentyofenchant.damage_storage.stored")
                        .withStyle(ChatFormatting.GRAY));

                // 显示储存的伤害量和最大储存量
                String damageText = String.format("%.1f / %.1f", storedDamage, maxStorage);
                event.getToolTip().add(Component.literal(damageText)
                        .withStyle(ChatFormatting.RED));

                // 显示进度条（可选）
                if (maxStorage > 0) {
                    float progress = storedDamage / maxStorage;
                    String progressBar = createProgressBar(progress, 10);
                    event.getToolTip().add(Component.literal(progressBar)
                            .withStyle(ChatFormatting.DARK_RED));
                }
            }
        }
    }

    // 创建进度条
    static String createProgressBar(float progress, int length) {
        int filled = (int) (progress * length);
        filled = Math.min(filled, length);
        int empty = length - filled;

        StringBuilder bar = new StringBuilder();
        bar.append(ChatFormatting.RED);

        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }

        bar.append(ChatFormatting.DARK_GRAY);

        for (int i = 0; i < empty; i++) {
            bar.append("█");
        }

        return bar.toString();
    }
}

