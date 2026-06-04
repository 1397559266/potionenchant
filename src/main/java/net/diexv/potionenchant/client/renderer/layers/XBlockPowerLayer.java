package net.diexv.potionenchant.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.ClientEventHandler;
import net.diexv.potionenchant.client.renderer.model.CubeModel;
import net.diexv.potionenchant.entity.XBlockEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class XBlockPowerLayer extends EnergySwirlLayer<XBlockEntity, CubeModel<XBlockEntity>> {
    private static final ResourceLocation POWER_LOCATION =
            new ResourceLocation("minecraft", "textures/entity/creeper/creeper_armor.png");

    private final CubeModel<XBlockEntity> model;

    public XBlockPowerLayer(RenderLayerParent<XBlockEntity, CubeModel<XBlockEntity>> renderer,
                            net.minecraft.client.model.geom.EntityModelSet modelSet) {
        super(renderer);
        // 使用相同的模型
        this.model = new CubeModel<>(modelSet.bakeLayer(ClientEventHandler.XBLOCK_LAYER));
    }

    @Override
    protected float xOffset(float partialTick) {
        return partialTick * 0.01F;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return POWER_LOCATION;
    }

    @Override
    protected EntityModel<XBlockEntity> model() {
        return this.model;
    }
    
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       XBlockEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isPowered()) {
            poseStack.pushPose();
            
            // EnergySwirlLayer 会在 Y=-1.5 的位置渲染，需要修正到中心
            poseStack.translate(0.0D, 1.5D, 0.0D);
            
            this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            
            super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount,
                        partialTick, ageInTicks, netHeadYaw, headPitch);
            
            poseStack.popPose();
        }
    }
}
