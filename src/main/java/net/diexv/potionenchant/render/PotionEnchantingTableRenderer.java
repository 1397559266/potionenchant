package net.diexv.potionenchant.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.diexv.potionenchant.blockentity.PotionEnchantingTableBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PotionEnchantingTableRenderer implements BlockEntityRenderer<PotionEnchantingTableBlockEntity> {

    private final ItemRenderer itemRenderer;

    public PotionEnchantingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PotionEnchantingTableBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        ItemStack bottleStack = new ItemStack(Items.GLASS_BOTTLE);
        float time = (blockEntity.time + partialTick) * 0.05f;

        // 3 bottles orbiting around center
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();

            // Base position: center of block, 1 block above table
            poseStack.translate(0.5, 1.1, 0.5);

            // Gentle bobbing
            float bob = (float) Math.sin(time * 2.0f + i * 2.0f) * 0.08f;
            poseStack.translate(0, bob, 0);

            // Orbit rotation: each bottle 120 degrees apart
            float orbitAngle = time * 60f + i * 120f;
            poseStack.mulPose(Axis.YP.rotationDegrees(orbitAngle));

            // Push out to orbit radius of 1 block
            poseStack.translate(1.0, 0, 0);

            // Each bottle faces inward (counter-rotate)
            poseStack.mulPose(Axis.YP.rotationDegrees(-orbitAngle));

            // Self-spin
            float selfSpin = time * 120f + i * 60f;
            poseStack.mulPose(Axis.YP.rotationDegrees(selfSpin));

            // Scale
            poseStack.scale(0.6f, 0.6f, 0.6f);

            this.itemRenderer.renderStatic(
                bottleStack,
                ItemDisplayContext.FIXED,
                15728880,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
            );

            poseStack.popPose();
        }
    }
}
