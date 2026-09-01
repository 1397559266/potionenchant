package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DiexvSwordLaserRenderEvents {

    private DiexvSwordLaserRenderEvents() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        // 跳过 Oculus 阴影 pass，避免光柱画进阴影贴图
        if (ItemShaderModCompat.isRenderingShadowPass()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!DiexvSwordLaserRenderer.hasActive()) return;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        // AFTER_LEVEL 的 event.getPoseStack() 是投影矩阵而非视图矩阵，重建视图矩阵。
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.last().pose().identity();
        RenderSystem.applyModelViewMatrix();
        try {
            PoseStack view = new PoseStack();
            view.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            view.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
            view.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
            DiexvSwordLaserRenderer.renderAll(view, bufferSource, event.getPartialTick());
            bufferSource.endBatch(PolygonRenderer.RenderTypes.UNLIT_ADDITIVE);
        } finally {
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
