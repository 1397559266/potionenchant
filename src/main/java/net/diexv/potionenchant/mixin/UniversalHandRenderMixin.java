package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 统一手持渲染 Mixin（万能附魔书 + 万能药水附魔瓶）
 * 修改手持位置，使其像地图一样显示在玩家面前
 */
@Mixin(ItemInHandRenderer.class)
public class UniversalHandRenderMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onRenderUniversalItem(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {

        if (stack.getItem() != ModItems.UNIVERSAL_ENCHANTMENT_BOOK.get() &&
            stack.getItem() != ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
            return;
        }

        ci.cancel();
        poseStack.pushPose();

        poseStack.translate(0.0F, -0.2F, -1.6F);
        poseStack.scale(2.0F, 2.0F, 2.0F);

        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
            stack,
            ItemDisplayContext.GUI,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            player.level(),
            0
        );

        poseStack.popPose();
    }
}