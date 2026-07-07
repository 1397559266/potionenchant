package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.diexv.potionenchant.entity.XBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * XBlock 实体渲染器 — 使用 Hyperlink 风格的蓝色半透明立方体
 */
public class XBlockEntityRenderer extends EntityRenderer<XBlockEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("potionenchant", "textures/entity/bomb.png");

    public XBlockEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(XBlockEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float tick = entity.tickCount + partialTicks;

        PolygonRenderer.with(poseStack, () -> {
            float pulse = 0.8f + 0.2f * Mth.sin(tick * 0.05f);
            float scale = 10.0F * pulse;
            poseStack.scale(scale, scale, scale);

            AABB aabb = new AABB(-1, -1, -1, 1, 1, 1);

            // 蓝色半透明填充立方体
            PolygonRenderer.cubeBox(
                    poseStack.last().pose(),
                    buffer.getBuffer(PolygonRenderer.RenderTypes.LIGHTNING_NO_CULL),
                    aabb,
                    0x604444FF,
                    face -> true);

            // 亮蓝色线框
            PolygonRenderer.thickLineBoxBox(
                    poseStack.last().pose(),
                    buffer.getBuffer(PolygonRenderer.RenderTypes.LIGHTNING_NO_CULL),
                    aabb.inflate(0.05f),
                    0.03f,
                    0xAA8888FF);

            PolygonRenderer.endBatch(PolygonRenderer.RenderTypes.LIGHTNING_NO_CULL);
        });
    }

    @Override
    public ResourceLocation getTextureLocation(XBlockEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(XBlockEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}
