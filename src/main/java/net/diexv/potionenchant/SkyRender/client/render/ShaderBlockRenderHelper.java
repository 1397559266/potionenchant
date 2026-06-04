package net.diexv.potionenchant.SkyRender.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class ShaderBlockRenderHelper {
    private static final float WORLD_BLOCK_COSMIC_SCALE = 0.35F;

    public static void renderCosmicBlock(BlockState blockState, PoseStack poseStack, MultiBufferSource buffers,
                                          int packedLight, int packedOverlay, ItemStack stack) {
        renderCosmicBlock(blockState, poseStack, buffers, packedLight, packedOverlay, stack, AvaritiaShaders.COSMIC_RENDER_TYPE);
    }

    public static void renderCosmicBlock(BlockState blockState, PoseStack poseStack, MultiBufferSource buffers,
                                          int packedLight, int packedOverlay, ItemStack stack, RenderType renderType) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !isCosmicShaderReady()) return;

        float yaw = 0.0F, pitch = 0.0F;
        float scale = AvaritiaShaders.inventoryRender ? 100 : WORLD_BLOCK_COSMIC_SCALE;
        if (!AvaritiaShaders.inventoryRender && mc.player != null) {
            yaw = (float) (mc.player.getYRot() * 2.0F * Math.PI / 360.0);
            pitch = -(float) (mc.player.getXRot() * 2.0F * Math.PI / 360.0);
        }

        AvaritiaShaders.cosmicTime.set((System.currentTimeMillis() - AvaritiaShaders.renderTime) / 2000.0F);
        AvaritiaShaders.cosmicYaw.set(yaw);
        AvaritiaShaders.cosmicPitch.set(pitch);
        AvaritiaShaders.cosmicExternalScale.set(scale);
        AvaritiaShaders.cosmicOpacity.set(0.60F);

        for (int i = 0; i < 25; ++i) {
            TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(PotionEnchantMod.rl("item/cosmic_" + i));
            AvaritiaShaders.COSMIC_UVS[i * 4] = sprite.getU0();
            AvaritiaShaders.COSMIC_UVS[i * 4 + 1] = sprite.getV0();
            AvaritiaShaders.COSMIC_UVS[i * 4 + 2] = sprite.getU1();
            AvaritiaShaders.COSMIC_UVS[i * 4 + 3] = sprite.getV1();
        }
        AvaritiaShaders.cosmicUVs.setMatrix2x2Array(AvaritiaShaders.COSMIC_UVS, 25);

        VertexConsumer consumer = buffers.getBuffer(renderType);
        BakedModel model = mc.getBlockRenderer().getBlockModel(blockState);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            quads.addAll(model.getQuads(blockState, direction, mc.level.random));
        }
        mc.getItemRenderer().renderQuadList(poseStack, consumer, quads, stack, packedLight, packedOverlay);

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(renderType);
        }
    }

    private static boolean isCosmicShaderReady() {
        return AvaritiaShaders.cosmicShader != null
            && AvaritiaShaders.cosmicTime != null
            && AvaritiaShaders.cosmicYaw != null
            && AvaritiaShaders.cosmicPitch != null
            && AvaritiaShaders.cosmicExternalScale != null
            && AvaritiaShaders.cosmicOpacity != null
            && AvaritiaShaders.cosmicUVs != null;
    }

    private ShaderBlockRenderHelper() {}
}
