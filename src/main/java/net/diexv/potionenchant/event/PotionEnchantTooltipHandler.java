package net.diexv.potionenchant.event;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 在原版tooltip中添加药水附魔信息
 * 在最下方显示与药水颜色相同的药水名称+药水等级（数字）
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class PotionEnchantTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        // 检查是否有药水附魔
        if (!PotionEnchantManager.hasPotionEnchantments(stack)) {
            return;
        }

        // 如果启用了独立tooltip，则不在原版tooltip中添加信息
        if (PotionEnchantConfig.SERVER.enablePotionEnchantTooltip.get()) {
            return;
        }

        List<PotionEnchantData> enchantments = PotionEnchantManager.getPotionEnchantments(stack);
        if (enchantments.isEmpty()) {
            return;
        }

        // 在最下方添加药水附魔信息
        for (PotionEnchantData enchant : enchantments) {
            // 获取药水颜色（效果自身颜色）
            int color = enchant.getColor();

            // 获取药水名称和等级
            String effectName = enchant.getEffect().getDisplayName().getString(); // 裸效果名，不含等级
            int level = enchant.getAmplifier() + 1; // 转换为实际等级

            // 使用 Component style 设置颜色（比 §x 嵌入更可靠）
            Component line = Component.literal(effectName + " " + level)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF)));

            // 添加到tooltip最下方
            tooltip.add(line);
        }
    }
}