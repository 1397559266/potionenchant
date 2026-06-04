package net.diexv.potionenchant.client.compat.oculus;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 后备清理：在 RenderLevelStageEvent.AFTER_LEVEL 时清理未处理的延迟队列
 * 主要处理逻辑在 CosmicAfterLevelMixin 中（更精确的注入点）
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CosmicRenderEvents {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            // 仅清理队列（主要渲染逻辑在 Mixin 中）
            if (ItemShaderModCompat.isOculusShaderPackActive()) {
                CosmicItemLateRenderQueue.renderAfterLevel();
            }
        }
    }

    private CosmicRenderEvents() {}
}
