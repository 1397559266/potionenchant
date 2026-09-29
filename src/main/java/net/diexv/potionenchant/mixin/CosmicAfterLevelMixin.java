package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.client.compat.oculus.CosmicItemLateRenderQueue;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.diexv.potionenchant.client.compat.oculus.WorldRenderPhase;
import net.diexv.potionenchant.client.renderer.DiexvSwordLaserRenderEvents;
import net.diexv.potionenchant.client.renderer.SwordAuraThirdPerson;
import net.diexv.potionenchant.client.renderer.orbital.CreeperTankStrikeRenderEvents;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 参考 Adorable Armory 的 LolaCosmicAfterLevelMixin
 * 在 GameRenderer.renderLevel() 中使用 Mixin 精确控制渲染时机：
 * 1. 手部渲染前 → 渲染非手部物品的星空层 (renderBeforeHand)
 * 2. 全部渲染完成后 → 渲染手部物品的星空层 (renderAfterHand)
 * 这样能确保与 Oculus 光影完全兼容
 *
 * 世界空间光柱（轨道轰击 / 激光）同理：光影包在 renderLevel 末尾才把世界 buffer 合成到主渲染目标，
 * AFTER_LEVEL 里画的光柱会被覆盖而不可见，因此也在这里补一帧延迟绘制。
 *
 * 另外在这里维护 {@link WorldRenderPhase}（renderLevel 的 HEAD/TAIL 进出）：
 * 区分"世界内需要延迟的 GUI 上下文（手持书/瓶）"与"HUD/快捷栏不需要也无法延迟的 GUI 物品"。
 */
@Mixin(value = GameRenderer.class, priority = 500)
public abstract class CosmicAfterLevelMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevelEnter(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        WorldRenderPhase.enter();
        // 开启新的一帧：第三人称剑气的延迟绘制一帧只允许画一次
        // （下面「手部渲染前」与 TAIL 兜底两个注入点同帧都会触发）
        SwordAuraThirdPerson.beginFrame();
    }

    @Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z", ordinal = 0))
    private void onRenderLevelBeforeHand(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            CosmicItemLateRenderQueue.renderBeforeHand();
            // 光影合成后、手部渲染前：光柱画在手部之下，与无光影时的层叠关系一致
            CreeperTankStrikeRenderEvents.renderOculusDeferredPass(partialTick);
            DiexvSwordLaserRenderEvents.renderOculusDeferredPass(partialTick);
            // 第三人称月牙剑气：同理，不能在 AFTER_ENTITIES 画，否则会被光影合成覆盖
            SwordAuraThirdPerson.renderOculusDeferredPass(partialTick);
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelAfterShaderpackFinal(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        try {
            if (ItemShaderModCompat.isOculusShaderPackActive()) {
                // 兜底：手部渲染前的注入点未触发时，这里补画
                CreeperTankStrikeRenderEvents.renderOculusDeferredPass(partialTick);
                DiexvSwordLaserRenderEvents.renderOculusDeferredPass(partialTick);
                SwordAuraThirdPerson.renderOculusDeferredPass(partialTick);
                CosmicItemLateRenderQueue.renderAfterHand();
            }
        } finally {
            // 必须在所有延迟渲染之后才退出世界渲染阶段（之后就是 HUD/GUI，不再延迟）
            WorldRenderPhase.exit();
        }
    }
}
