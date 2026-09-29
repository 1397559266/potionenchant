package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.diexv.potionenchant.client.compat.oculus.LateWorldBeamPass;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DiexvSwordLaserRenderEvents {

    /** Oculus 光影包下本帧是否已把激光光柱延后（避免重复绘制） */
    private static boolean beamDeferredForOculus = false;
    /** 延后绘制时用的投影矩阵（AFTER_LEVEL 时刻的世界投影） */
    private static final Matrix4f deferredProjection = new Matrix4f();

    private DiexvSwordLaserRenderEvents() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        // 跳过 Oculus 阴影 pass，避免光柱画进阴影贴图
        if (ItemShaderModCompat.isRenderingShadowPass()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!DiexvSwordLaserRenderer.hasActive()) return;

        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            // 光影包会在 renderLevel 末尾把世界 buffer 合成到主渲染目标，
            // 此刻写入的光柱会被覆盖 → 延后到合成之后再画
            beamDeferredForOculus = true;
            deferredProjection.set(RenderSystem.getProjectionMatrix());
            return;
        }

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

    /**
     * Oculus 光影包下的延迟光柱 pass：由 {@code CosmicAfterLevelMixin} 在光影包合成完成后
     * （GameRenderer.renderLevel 手部渲染之前 / 末尾兜底）调用，直接写入主渲染目标。
     */
    public static void renderOculusDeferredPass(float partialTick) {
        if (!beamDeferredForOculus) return;
        beamDeferredForOculus = false;
        if (!ItemShaderModCompat.isOculusShaderPackActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!DiexvSwordLaserRenderer.hasActive()) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return;
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack view = LateWorldBeamPass.begin(camera);
        try {
            RenderSystem.setProjectionMatrix(new Matrix4f(deferredProjection), VertexSorting.DISTANCE_TO_ORIGIN);
            DiexvSwordLaserRenderer.renderAll(view, bufferSource, partialTick);
            bufferSource.endBatch(PolygonRenderer.RenderTypes.UNLIT_ADDITIVE);
        } finally {
            LateWorldBeamPass.end();
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.DISTANCE_TO_ORIGIN);
        }
    }
}
