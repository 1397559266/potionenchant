package net.diexv.potionenchant.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.diexv.potionenchant.blockentity.UltimateEnchantTableBlockEntity;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class UltimateEnchantTableRenderer implements BlockEntityRenderer<UltimateEnchantTableBlockEntity> {

    private final ItemRenderer itemRenderer;

    public UltimateEnchantTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(UltimateEnchantTableBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        float time = (blockEntity.time + partialTick) * 0.05f;

        // Render 3 orbiting Universal Potion Bottles
        ItemStack bottleStack = new ItemStack(ModItems.UNIVERSAL_POTION_BOTTLE.get());
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.translate(0.5, 1.1, 0.5);

            float bob = (float) Math.sin(time * 2.0f + i * 2.0f) * 0.08f;
            poseStack.translate(0, bob, 0);

            float orbitAngle = time * 60f + i * 120f;
            poseStack.mulPose(Axis.YP.rotationDegrees(orbitAngle));
            poseStack.translate(1.0, 0, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-orbitAngle));

            float selfSpin = time * 120f + i * 60f;
            poseStack.mulPose(Axis.YP.rotationDegrees(selfSpin));

            poseStack.scale(0.6f, 0.6f, 0.6f);

            this.itemRenderer.renderStatic(
                bottleStack, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, blockEntity.getLevel(), 0
            );
            poseStack.popPose();
        }

        // Render rotating Universal Enchantment Book at center
        ItemStack bookStack = new ItemStack(ModItems.UNIVERSAL_ENCHANTMENT_BOOK.get());
        poseStack.pushPose();
        poseStack.translate(0.5, 1.3, 0.5);

        float bookBob = (float) Math.sin(time * 2.0f + 0.5f) * 0.06f;
        poseStack.translate(0, bookBob, 0);

        float rotAngle = time * 45f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotAngle));

        float tilt = (float) Math.sin(time * 1.5f) * 0.08f;
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt));

        poseStack.scale(0.65f, 0.65f, 0.65f);

        this.itemRenderer.renderStatic(
            bookStack, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY,
            poseStack, bufferSource, blockEntity.getLevel(), 0
        );
        poseStack.popPose();
    }
}
