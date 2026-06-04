package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.item.XSwordItem;
import net.diexv.potionenchant.util.font.DiexvFont3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphics.class)
public class XSwordTooltipFontMixin {

    @Shadow
    private ItemStack tooltipStack;

    @Inject(
        method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void onRenderTooltipInternal(Font font, List<ClientTooltipComponent> components,
                                          int x, int y, ClientTooltipPositioner positioner,
                                          CallbackInfo ci) {
        // 检查 XSword 超模模式
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean isXSword = (mainHand.getItem() == ModItems.X_SWORD.get()) ||
                           (offHand.getItem() == ModItems.X_SWORD.get());
        if (!isXSword || !XSwordItem.isSupermode(player.getUUID())) return;

        if (components.isEmpty()) return;

        Font customFont = DiexvFont3.getFont();
        GuiGraphics self = (GuiGraphics)(Object)this;

        // 借助 Forge 事件系统获取渲染上下文，再覆盖字体
        RenderTooltipEvent.Pre preEvent = ForgeHooksClient.onRenderTooltipPre(
            tooltipStack, self, x, y, self.guiWidth(), self.guiHeight(),
            components, font, positioner);
        if (preEvent.isCanceled()) return;
        preEvent.setFont(customFont);

        // 计算 tooltip 尺寸和位置（同 vanilla）
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent comp : components) {
            int w = comp.getWidth(preEvent.getFont());
            if (w > width) width = w;
            height += comp.getHeight();
        }

        Vector2ic pos = positioner.positionTooltip(
            self.guiWidth(), self.guiHeight(),
            preEvent.getX(), preEvent.getY(),
            width, height);
        int tx = pos.x();
        int ty = pos.y();

        int finalWidth = width;
        int finalHeight = height;

        ci.cancel();

        // 渲染背景
        self.pose().pushPose();
        self.drawManaged(() -> {
            RenderTooltipEvent.Color colorEvent = ForgeHooksClient.onRenderTooltipColor(
                tooltipStack, self, tx, ty, preEvent.getFont(), components);
            TooltipRenderUtil.renderTooltipBackground(
                self, tx, ty, finalWidth, finalHeight, 400,
                colorEvent.getBackgroundStart(), colorEvent.getBackgroundEnd(),
                colorEvent.getBorderStart(), colorEvent.getBorderEnd());
        });
        self.pose().translate(0.0F, 0.0F, 400.0F);

        // 渲染文本（使用自定义字体）
        int currentY = ty;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent comp = components.get(i);
            comp.renderText(preEvent.getFont(), tx, currentY,
                self.pose().last().pose(), self.bufferSource());
            currentY += comp.getHeight() + (i == 0 ? 2 : 0);
        }

        // 渲染图片
        currentY = ty;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent comp = components.get(i);
            comp.renderImage(preEvent.getFont(), tx, currentY, self);
            currentY += comp.getHeight() + (i == 0 ? 2 : 0);
        }

        self.pose().popPose();
    }
}
