package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.client.compat.oculus.CosmicItemLateRenderQueue;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {

    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", ordinal = 0))
    private void onRenderLevelBeforeHand(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            CosmicItemLateRenderQueue.renderBeforeHand();
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelTail(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            CosmicItemLateRenderQueue.renderAfterHand();
        }
    }
}
