package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 万能药水附魔瓶 Tooltip 自定义背景 Mixin
 * 鼠标悬停时,在屏幕中央显示 skypotion.png 图片
 */
@Mixin(GuiGraphics.class)
public class UniversalPotionBottleTooltipBackgroundMixin {
    
    @Unique
    private static final ResourceLocation SKYPOTION_TEXTURE = ResourceLocation.fromNamespaceAndPath("potionenchant", "textures/item/skypotion.png");
    
    @Unique
    private static final int IMAGE_WIDTH = 200;
    
    @Unique
    private static final int IMAGE_HEIGHT = 200;

    // GuiGraphics 当前正在渲染tooltip的物品（仅物品型tooltip会被设置）
    @Shadow(remap = false)
    private ItemStack tooltipStack;
    
    /**
     * 在方法最开始注入
     */
    @Inject(
        method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
        at = @At("HEAD"),
        remap = true
    )
    private void onRenderTooltipBackground(
        Font font,
        List<ClientTooltipComponent> components,
        int mouseX,
        int mouseY,
        net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner,
        CallbackInfo ci
    ) {
        // 获取当前正在渲染tooltip的物品（不再通过容器槽位猜测，
        // 避免在其他物品的tooltip上误渲染万能药水瓶背景图）
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = this.tooltipStack;

        // 只处理万能药水附魔瓶
        if (stack == null || stack.getItem() != ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
            return;
        }
        
        GuiGraphics guiGraphics = (GuiGraphics) (Object) this;
        
        // 计算屏幕中央位置
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int x = (screenWidth - IMAGE_WIDTH) / 2;
        int y = (screenHeight - IMAGE_HEIGHT) / 2;
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 500);
        
        // 在屏幕中央绘制 skypotion.png
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        guiGraphics.blit(
            SKYPOTION_TEXTURE,
            x,
            y,
            0,
            0,
            IMAGE_WIDTH,
            IMAGE_HEIGHT,
            1694,
            1739
        );
        
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}
