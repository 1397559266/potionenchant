package net.diexv.potionenchant.client;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.renderer.BombEntityRenderer;
import net.diexv.potionenchant.client.renderer.RainbowLightningRenderer;
import net.diexv.potionenchant.client.renderer.XBlockEntityRenderer;
import net.diexv.potionenchant.client.renderer.model.CubeModel;
import net.diexv.potionenchant.entity.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

    @SuppressWarnings("removal")
    // 定义Bomb的模型层位置
    public static final ModelLayerLocation BOMB_LAYER = new ModelLayerLocation(
        new ResourceLocation(PotionEnchantMod.MODID, "bomb"), "main"
    );

    @SuppressWarnings("removal")
    // 定义XBlock的模型层位置（独立Layer）
    public static final ModelLayerLocation XBLOCK_LAYER = new ModelLayerLocation(
        new ResourceLocation(PotionEnchantMod.MODID, "xblock"), "main"
    );
    
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册Bomb的立方体模型层
        event.registerLayerDefinition(BOMB_LAYER, CubeModel::createBodyLayer);
        // 注册XBlock的立方体模型层（独立Layer）
        event.registerLayerDefinition(XBLOCK_LAYER, CubeModel::createBodyLayer);
    }
    
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BOMB.get(), BombEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.RAINBOW_LIGHTNING.get(), RainbowLightningRenderer::new);
        event.registerEntityRenderer(ModEntities.XBLOCK.get(), XBlockEntityRenderer::new);
    }
}