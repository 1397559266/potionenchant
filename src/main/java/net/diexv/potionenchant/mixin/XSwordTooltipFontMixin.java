package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.item.XSwordItem;
import net.diexv.potionenchant.util.font.DiexvFont3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphics.class)
public class XSwordTooltipFontMixin {

    @Redirect(
        method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderText(Lnet/minecraft/client/gui/Font;IILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"
        ),
        remap = true
    )
    private void redirectRenderText(ClientTooltipComponent component, Font originalFont, int x, int y,
                                     Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        Font fontToUse = originalFont;

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean isXSword = (mainHand.getItem() == ModItems.X_SWORD.get()) ||
                               (offHand.getItem() == ModItems.X_SWORD.get());

            if (isXSword && XSwordItem.isSupermode(player.getUUID())) {
                fontToUse = DiexvFont3.getFont();
            }
        }

        component.renderText(fontToUse, x, y, matrix, bufferSource);
    }
}