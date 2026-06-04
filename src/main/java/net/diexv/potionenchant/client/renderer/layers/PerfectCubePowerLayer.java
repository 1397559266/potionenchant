package net.diexv.potionenchant.client.renderer.layers;

import net.diexv.potionenchant.client.renderer.model.CubeModel;
import net.diexv.potionenchant.entity.BombEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class PerfectCubePowerLayer extends EnergySwirlLayer<BombEntity, CubeModel<BombEntity>> {
    private static final ResourceLocation POWER_LOCATION =
            new ResourceLocation("textures/entity/creeper/creeper_armor.png");

    private final CubeModel<BombEntity> model;

    public PerfectCubePowerLayer(RenderLayerParent<BombEntity, CubeModel<BombEntity>> renderer,
                                 net.minecraft.client.model.geom.EntityModelSet modelSet) {
        super(renderer);
        // 从modelSet中获取模型层并创建模型
        this.model = new CubeModel<>(modelSet.bakeLayer(net.diexv.potionenchant.client.ClientEventHandler.BOMB_LAYER));
    }

    @Override
    public float xOffset(float partialTick) {
        return partialTick * 0.01F;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return POWER_LOCATION;
    }

    @Override
    public EntityModel<BombEntity> model() {
        return this.model;
    }
}