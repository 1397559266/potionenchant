package net.diexv.potionenchant.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * 用于在tooltip中渲染skypotion.png图片的自定义组件
 */
public class SkyPotionImageTooltipComponent implements ClientTooltipComponent, TooltipComponent {
    
    private static final ResourceLocation SKYPOTION_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("potionenchant", "textures/item/skypotion.png");
    
    private final ItemStack itemStack;
    private final int imageWidth;
    private final int imageHeight;
    
    public SkyPotionImageTooltipComponent(ItemStack itemStack) {
        this(itemStack, 32, 32); // 默认32x32大小
    }
    
    public SkyPotionImageTooltipComponent(ItemStack itemStack, int width, int height) {
        this.itemStack = itemStack;
        this.imageWidth = width;
        this.imageHeight = height;
    }
    
    @Override
    public int getWidth(Font font) {
        return imageWidth;
    }
    
    @Override
    public int getHeight() {
        return imageHeight;
    }
    
    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        // 保存当前OpenGL状态
        guiGraphics.pose().pushPose();
        
        // 启用透明混合以支持PNG的alpha通道
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 设置Z轴层级,确保图片在其他元素之上
        guiGraphics.pose().translate(0, 0, 400);
        
        // 渲染图片
        // blit参数说明:
        // - texture: 纹理位置
        // - x, y: 屏幕坐标
        // - uOffset, vOffset: UV起始坐标(通常为0)
        // - width, height: 渲染的宽高
        // - textureWidth, textureHeight: 原始纹理的宽高
        guiGraphics.blit(
            SKYPOTION_TEXTURE,
            x,
            y,
            0, 0,           // UV起始坐标
            imageWidth, 
            imageHeight,
            imageWidth, 
            imageHeight     // 假设纹理大小与渲染大小相同
        );
        
        // 恢复OpenGL状态
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }
}
