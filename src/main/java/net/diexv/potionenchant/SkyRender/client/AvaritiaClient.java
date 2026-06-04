package net.diexv.potionenchant.SkyRender.client;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.client.model.BlackHoleModelLoader;
import net.diexv.potionenchant.SkyRender.client.model.CosmicModelLoader;
import net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AvaritiaClient {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRegisterShaders(RegisterShadersEvent event) {
        AvaritiaShaders.onRegisterShaders(event);
    }

    @SubscribeEvent
    public static void registerLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("cosmic", CosmicModelLoader.INSTANCE);
        event.register("black_hole", BlackHoleModelLoader.INSTANCE);
    }
}