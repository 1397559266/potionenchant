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

    /**
     * 在主世界渲染完成后、渲染手之前，渲染延迟排队的宇宙物品（手部除外）
     */
    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", ordinal = 0))
    private void onRenderLevelBeforeHand(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            CosmicItemLateRenderQueue.renderBeforeHand();
        }
    }

    /**
     * 在主世界渲染完全结束后，渲染剩余的延迟队列（包括手部）
     */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelTail(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            CosmicItemLateRenderQueue.renderAfterHand();
        }
    }
}
