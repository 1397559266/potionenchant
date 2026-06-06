package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.client.renderer.hyperlink.PolygonRenderer;
import net.diexv.potionenchant.entity.BombEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bomb 实体渲染器 — 使用 Hyperlink 风格的蓝色半透明立方体
 * 替代原 CubeModel 纹理渲染
 */
public class BombEntityRenderer extends EntityRenderer<BombEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("potionenchant", "textures/entity/bomb.png");

    private static final float SIZE = 0.5f;

    public BombEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BombEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        // 环绕模式：在玩家周围计算位置
        if (!entity.isAttacking()) {
            renderOrbiting(entity, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        renderCube(entity, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderCube(BombEntity entity, float partialTicks,
                            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float tick = entity.tickCount + partialTicks;

        PolygonRenderer.with(poseStack, () -> {
            poseStack.translate(0, SIZE, 0);
            poseStack.scale(SIZE, SIZE, SIZE);

            float pulse = 0.9f + 0.1f * Mth.sin(tick * 0.1f);
            AABB aabb = new AABB(-1, -1, -1, 1, 1, 1);

            // 蓝色半透明填充立方体
            PolygonRenderer.cubeBox(
                    poseStack.last().pose(),
                    buffer.getBuffer(PolygonRenderer.RenderTypes.LIGHTNING_NO_CULL),
                    aabb,
                    (int)(0x60 * pulse) << 24 | 0x4488FF,
                    face -> true);

            // 亮蓝色线框
            PolygonRenderer.thickLineBoxBox(
                    poseStack.last().pose(),
                    buffer.getBuffer(PolygonRenderer.RenderTypes.LIGHTNING_NO_CULL),
                    aabb.inflate(0.05f),
                    0.03f,
                    (int)(0xAA * pulse) << 24 | 0x44CCFF);

            PolygonRenderer.endBatch(PolygonRenderer.RenderTypes.LIGHTNING_NO_CULL);
        });
    }

    private void renderOrbiting(BombEntity entity, float partialTicks,
                                PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity owner = entity.getOwner();
        if (owner == null) {
            renderCube(entity, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        // 客户端计算环绕位置
        double ownerX = Mth.lerp(partialTicks, owner.xo, owner.getX());
        double ownerY = Mth.lerp(partialTicks, owner.yo, owner.getY());
        double ownerZ = Mth.lerp(partialTicks, owner.zo, owner.getZ());

        double centerX = ownerX;
        double centerZ = ownerZ;
        double centerY = ownerY + 3.5;
        float ageInTicks = (float)(entity.level().getGameTime() % 10000) + partialTicks;
        int orbitIndex = entity.getOrbitIndex();
        double angle = (orbitIndex / 5.0) * Math.PI * 2 + ageInTicks * 0.05;
        double radius = 3.0;

        double targetX = centerX + Math.cos(angle) * radius;
        double targetY = centerY + Math.sin(ageInTicks * 0.1) * 0.5;
        double targetZ = centerZ + Math.sin(angle) * radius;

        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double camX = targetX - camera.getPosition().x;
        double camY = targetY - camera.getPosition().y;
        double camZ = targetZ - camera.getPosition().z;

        poseStack.pushPose();
        poseStack.translate(camX, camY, camZ);
        renderCube(entity, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BombEntity entity) {
        return TEXTURE;
    }
}
