package net.diexv.potionenchant.handlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Tooltip事件处理器
 * 注意：此类包含客户端专用代码，仅在客户端环境中注册
 */
@OnlyIn(Dist.CLIENT)
public class TooltipEventHandler {
    
    private static final ResourceLocation SKYPOTION_TEXTURE = ResourceLocation.fromNamespaceAndPath("potionenchant", "textures/item/skypotion.png");
    
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // 为神秘的空瓶添加配方提示
        if (stack.getItem() == ModItems.MYSTERIOUS_EMPTY_BOTTLE.get()) {
            event.getToolTip().add(Component.translatable("item.potionenchant.mysterious_empty_bottle.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("item.potionenchant.mysterious_empty_bottle.tooltip2").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        
        // 检查是否为药水物品
        if (isPotionItem(stack) && PotionEnchantConfig.SERVER.enableCustomPotionTooltip.get()) {
            List<Component> tooltip = event.getToolTip();
            // 保留第一行（物品名称），避免删除所有行导致Tooltip为空引发崩溃
            Component firstLine = tooltip.isEmpty() ? null : tooltip.get(0);
            tooltip.removeIf(line -> line != firstLine && (line.getString().contains("effect.") || line.getString().contains("potion.whenDrank")));
        }
    }
    
    @SubscribeEvent
    public void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        
        // 只处理万能药水附魔瓶
        if (stack.getItem() != ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
            return;
        }
        
        // TooltipColorMixin 已经处理了背景颜色
        // 这里不需要做任何事情
    }
    
    /**
     * 检查物品是否为药水
     */
    private boolean isPotionItem(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.PotionItem ||
               stack.getItem() instanceof net.minecraft.world.item.TippedArrowItem ||
               stack.getItem() instanceof net.minecraft.world.item.SplashPotionItem ||
               stack.getItem() instanceof net.minecraft.world.item.LingeringPotionItem;
    }

}
