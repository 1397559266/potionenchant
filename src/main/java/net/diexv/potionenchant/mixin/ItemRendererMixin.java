package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.client.model.BlackHoleBakeModel;
import net.diexv.potionenchant.SkyRender.client.model.CosmicBakeModel;
import net.diexv.potionenchant.client.compat.oculus.ItemRenderCompatibilityContext;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRenderItem(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack mStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel modelIn, CallbackInfo ci) {
        ItemRenderCompatibilityContext.beginItemRender(context);
        ItemShaderModCompat.logCompatModeOnce();

        if (modelIn instanceof CosmicBakeModel iItemRenderer) {
            ci.cancel();
            mStack.pushPose();
            try {
                final CosmicBakeModel renderer = (CosmicBakeModel) ForgeHooksClient.handleCameraTransforms(mStack, iItemRenderer, context, leftHand);
                mStack.translate(-0.5D, -0.5D, -0.5D);
                renderer.renderItem(stack, context, mStack, buffers, packedLight, packedOverlay);
            } finally {
                mStack.popPose();
                ItemRenderCompatibilityContext.endItemRender();
            }
        } else if (modelIn instanceof BlackHoleBakeModel blackHoleRenderer) {
            ci.cancel();
            mStack.pushPose();
            try {
                final BlackHoleBakeModel renderer = (BlackHoleBakeModel) ForgeHooksClient.handleCameraTransforms(mStack, blackHoleRenderer, context, leftHand);
                mStack.translate(-0.5D, -0.5D, -0.5D);
                renderer.renderItem(stack, context, mStack, buffers, packedLight, packedOverlay);
            } finally {
                mStack.popPose();
                ItemRenderCompatibilityContext.endItemRender();
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void onRenderItemReturn(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack mStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel modelIn, CallbackInfo ci) {
        ItemRenderCompatibilityContext.endItemRender();
    }
}
