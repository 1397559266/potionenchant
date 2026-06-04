package net.diexv.potionenchant.SkyRender.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.api.client.shader.CCShaderInstance;
import net.diexv.potionenchant.SkyRender.api.client.shader.CCUniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AvaritiaShaders {
    private static class RenderStateShardAccess extends RenderStateShard {
        private static final DepthTestStateShard EQUAL_DEPTH_TEST = RenderStateShard.EQUAL_DEPTH_TEST;
        private static final DepthTestStateShard LEQUAL_DEPTH_TEST = RenderStateShard.LEQUAL_DEPTH_TEST;
        private static final DepthTestStateShard NO_DEPTH_TEST = RenderStateShard.NO_DEPTH_TEST;
        private static final LightmapStateShard LIGHT_MAP = RenderStateShard.LIGHTMAP;
        private static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = RenderStateShard.TRANSLUCENT_TRANSPARENCY;
        private static final TextureStateShard BLOCK_SHEET_MIPPED = RenderStateShard.BLOCK_SHEET_MIPPED;
        private static final CullStateShard NO_CULL = RenderStateShard.NO_CULL;
        public static final WriteMaskStateShard COLOR_WRITE = RenderStateShard.COLOR_WRITE;
        public static final WriteMaskStateShard COLOR_DEPTH_WRITE = RenderStateShard.COLOR_DEPTH_WRITE;
        public static final OutputStateShard MAIN_TARGET = RenderStateShard.MAIN_TARGET;
        public static final LayeringStateShard SHADER_LAYER_DEPTH_BIAS = new LayeringStateShard("potionenchant_shader_layer_depth_bias", () -> {
            RenderSystem.polygonOffset(-1.0F, -32.0F);
            RenderSystem.enablePolygonOffset();
        }, () -> {
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
        });

        private RenderStateShardAccess(String pName, Runnable pSetupState, Runnable pClearState) {
            super(pName, pSetupState, pClearState);
        }
    }

    public static final float[] COSMIC_UVS = new float[100];
    public static boolean inventoryRender = false;
    public static int renderTime;
    public static float tick;
    public static float renderFrame;
    public static CCShaderInstance cosmicShader;
    public static CCUniform cosmicTime;
    public static CCUniform cosmicYaw;
    public static CCUniform cosmicPitch;
    public static CCUniform cosmicExternalScale;
    public static CCUniform cosmicOpacity;
    public static CCUniform cosmicUVs;
    
    // Black Hole Shader
    public static CCShaderInstance blackHoleShader;
    public static CCUniform blackHoleTime;
    public static CCUniform blackHoleYaw;
    public static CCUniform blackHolePitch;
    public static CCUniform blackHoleZoom;
    public static CCUniform blackHoleScreenSize;

    // 普通渲染类型（非光影时使用）
    public static final RenderType COSMIC_RENDER_TYPE = RenderType.create(PotionEnchantMod.MODID + ":cosmic", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false, RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicShader))
            .setDepthTestState(RenderStateShardAccess.EQUAL_DEPTH_TEST)
            .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
            .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
            .createCompositeState(true));
    
    // 延迟渲染类型（光影兼容，主世界渲染完成后使用）
    public static final RenderType COSMIC_AFTER_LEVEL_RENDER_TYPE = RenderType.create(PotionEnchantMod.MODID + ":cosmic_after_level", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false, RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicShader))
            .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
            .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
            .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
            .setLayeringState(RenderStateShardAccess.SHADER_LAYER_DEPTH_BIAS)
            .setOutputState(RenderStateShardAccess.MAIN_TARGET)
            .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
            .createCompositeState(false));

    // 延迟渲染类型（第一人称手部，光影兼容）
    public static final RenderType COSMIC_HAND_AFTER_LEVEL_RENDER_TYPE = RenderType.create(PotionEnchantMod.MODID + ":cosmic_hand_after_level", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false, RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicShader))
            .setDepthTestState(RenderStateShardAccess.NO_DEPTH_TEST)
            .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
            .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
            .setOutputState(RenderStateShardAccess.MAIN_TARGET)
            .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
            .createCompositeState(false));

    public static final RenderType BLACK_HOLE_RENDER_TYPE = RenderType.create(PotionEnchantMod.MODID + ":black_hole", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, true, false, RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(() -> blackHoleShader))
            .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderStateShardAccess.NO_CULL)
            .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
            .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
            .createCompositeState(true));

    @SuppressWarnings("removal")
    public static void onRegisterShaders(RegisterShadersEvent event) {
        event.registerShader(CCShaderInstance.create(event.getResourceProvider(), new ResourceLocation(PotionEnchantMod.MODID, "cosmic"), DefaultVertexFormat.BLOCK), e -> {
            cosmicShader = (CCShaderInstance) e;
            cosmicTime = Objects.requireNonNull(cosmicShader.getUniform("time"));
            cosmicYaw = Objects.requireNonNull(cosmicShader.getUniform("yaw"));
            cosmicPitch = Objects.requireNonNull(cosmicShader.getUniform("pitch"));
            cosmicExternalScale = Objects.requireNonNull(cosmicShader.getUniform("externalScale"));
            cosmicOpacity = Objects.requireNonNull(cosmicShader.getUniform("opacity"));
            cosmicUVs = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs"));
            cosmicTime.set((float) renderTime + renderFrame);
            cosmicShader.onApply(() -> cosmicTime.set((float) renderTime + renderFrame));
        });
        
        // Register black_hole shader
        event.registerShader(CCShaderInstance.create(event.getResourceProvider(), new ResourceLocation(PotionEnchantMod.MODID, "black_hole"), DefaultVertexFormat.BLOCK), e -> {
            blackHoleShader = (CCShaderInstance) e;
            blackHoleTime = Objects.requireNonNull(blackHoleShader.getUniform("time"));
            blackHoleYaw = Objects.requireNonNull(blackHoleShader.getUniform("yaw"));
            blackHolePitch = Objects.requireNonNull(blackHoleShader.getUniform("pitch"));
            blackHoleZoom = Objects.requireNonNull(blackHoleShader.getUniform("iZoom"));
            blackHoleScreenSize = Objects.requireNonNull(blackHoleShader.getUniform("screenSize"));
            blackHoleTime.set((float) renderTime + renderFrame);
            blackHoleShader.onApply(() -> blackHoleTime.set((float) renderTime + renderFrame));
        });
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (!Minecraft.getInstance().isPaused() && event.phase == TickEvent.Phase.END) {
            ++renderTime;
            tick += 1F;
            if (tick >= 720.0f) {
                tick = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (!Minecraft.getInstance().isPaused() && event.phase == TickEvent.Phase.START) {
            renderFrame = event.renderTickTime;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void drawScreenPre(final ScreenEvent.Render.Pre e) {
        AvaritiaShaders.inventoryRender = true;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void drawScreenPost(final ScreenEvent.Render.Post e) {
        AvaritiaShaders.inventoryRender = false;
    }
}
