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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
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
        public static final LightmapStateShard LIGHT_MAP = RenderStateShard.LIGHTMAP;
        public static final TransparencyStateShard TRANSLUCENT_TRANSPARENCY = RenderStateShard.TRANSLUCENT_TRANSPARENCY;
        private static final TextureStateShard BLOCK_SHEET_MIPPED = RenderStateShard.BLOCK_SHEET_MIPPED;
        public static final CullStateShard NO_CULL = RenderStateShard.NO_CULL;
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

    // ===== Cosmic Shader (25 textures) =====
    public static final int COSMIC_TEXTURE_COUNT = 25;
    public static final float[] COSMIC_UVS = new float[COSMIC_TEXTURE_COUNT * 4]; // 25 * 4 = 100

    public static boolean inventoryRender = false;
    // ===== 旧 tick 计数器（保留兼容；着色器/特效时间已全部改走下面的墙钟 API） =====
    public static int renderTime;
    public static float tick;
    public static float renderFrame;

    // ===== 统一时间基准：墙钟（毫秒）=====
    /**
     * 【统一时间基准：墙钟】暂停也继续走（与 GL 粒子一致）。
     *
     * 为什么要有这个：原来同一个 time uniform 有多处写入、两种基准 ——
     *   AvaritiaShaders / DiexvSwordShaders 用 (renderTime + renderFrame)（tick 计数，20 单位/秒）
     *   ShaderBlockRenderHelper / DiexvSwordShaders 非延迟路径却用
     *   (System.currentTimeMillis() - renderTime) / 2000
     * 后者把"tick 计数器"当成"纪元毫秒"去减，结果 ≈ 8.6e8，float 精度（间隔约 64）把每帧变化
     * 完全吃掉 → 着色器动画静止；只有在着色器 onApply 时被写入正确值的那一瞬间才会动 →
     * 表现就是"有时候动、有时候静止"。现在全部只准调用下面几个函数。
     */
    private static final long WALL_EPOCH_MILLIS = System.currentTimeMillis();

    /** 统一时间（tick 等价单位，20 单位/秒）—— 只有 tick 单位的着色器才能用这个：
     *  剑 cosmic（代码雨，脚本与参考工程完全一致，参考侧喂的就是 tick）、光柱/激光等特效时钟。
     *  注意：星体 cosmic / black hole / 体素 mesh 的 time 是【秒】，请用 {@link #cosmicTimeSeconds()}。 */
    public static float cosmicTimeTicks() {
        return (System.currentTimeMillis() - WALL_EPOCH_MILLIS) / 50.0F;
    }

    /** 统一时间（秒，1 单位/秒）—— 必须给"time 单位=秒"的着色器用：
     *  星体 cosmic、black hole、体素网格 meshTime（它们原本就是 (renderTime+renderFrame)/20 = 秒，
     *  误喂 tick 会快 20 倍 —— 星体位置变化速度就是被这个拉飞的）。
     *  见 {@link #cosmicTimeTicks()}。 */
    public static float cosmicTimeSeconds() {
        return (System.currentTimeMillis() - WALL_EPOCH_MILLIS) / 1000.0F;
    }

    /** 统一时间（墙钟毫秒，纪元 = 本模组加载）—— 脉冲/闪烁相位用这个 */
    public static long cosmicTimeMillis() {
        return System.currentTimeMillis() - WALL_EPOCH_MILLIS;
    }

    /** 把统一时间写进宇宙（星体）着色器 uniform（所有路径唯一入口；单位=秒） */
    public static void applyCosmicTime() {
        if (cosmicTime != null) {
            cosmicTime.set(cosmicTimeSeconds());
        }
    }

    /** 把统一时间写进黑洞着色器 uniform（所有路径唯一入口；单位=秒） */
    public static void applyBlackHoleTime() {
        if (blackHoleTime != null) {
            blackHoleTime.set(cosmicTimeSeconds());
        }
    }

    public static CCShaderInstance cosmicShader;
    public static CCUniform cosmicTime;
    public static CCUniform cosmicYaw;
    public static CCUniform cosmicPitch;
    public static CCUniform cosmicExternalScale;
    public static CCUniform cosmicOpacity;
    public static CCUniform cosmicUVs;

    // ===== Black Hole Shader =====
    public static CCShaderInstance blackHoleShader;
    public static CCUniform blackHoleTime;
    public static CCUniform blackHoleYaw;
    public static CCUniform blackHolePitch;
    public static CCUniform blackHoleZoom;
    public static CCUniform blackHoleScreenSize;

    // 普通渲染类型（无光影 / GUI时使用）
    public static final RenderType COSMIC_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":cosmic",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicShader))
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
                    .setCullState(RenderStateShardAccess.NO_CULL)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(true));

    // 延迟渲染类型（光影兼容，非手部物品在世界中显示）
    public static final RenderType COSMIC_ITEM_AFTER_LEVEL_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":cosmic_item_after_level",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicShader))
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
                    .setLayeringState(RenderStateShardAccess.SHADER_LAYER_DEPTH_BIAS)
                    .setOutputState(RenderStateShardAccess.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(true));

    // 延迟渲染类型（光影兼容，第一人称手部物品）
    public static final RenderType COSMIC_HAND_AFTER_LEVEL_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":cosmic_hand_after_level",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicShader))
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setLightmapState(RenderStateShardAccess.LIGHT_MAP)
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
                    .setLayeringState(RenderStateShardAccess.SHADER_LAYER_DEPTH_BIAS)
                    .setOutputState(RenderStateShardAccess.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(true));

    // Black Hole 渲染类型
    public static final RenderType BLACK_HOLE_RENDER_TYPE = RenderType.create(
            PotionEnchantMod.MODID + ":black_hole",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> blackHoleShader))
                    .setTransparencyState(RenderStateShardAccess.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShardAccess.NO_CULL)
                    .setDepthTestState(RenderStateShardAccess.LEQUAL_DEPTH_TEST)
                    .setTextureState(RenderStateShardAccess.BLOCK_SHEET_MIPPED)
                    .setCullState(RenderStateShardAccess.NO_CULL)
                    .setWriteMaskState(RenderStateShardAccess.COLOR_WRITE)
                    .createCompositeState(true));

    @SuppressWarnings("removal")
    public static void onRegisterShaders(RegisterShadersEvent event) {
        event.registerShader(CCShaderInstance.create(event.getResourceProvider(),
                new ResourceLocation(PotionEnchantMod.MODID, "cosmic"),
                DefaultVertexFormat.BLOCK), e -> {
            cosmicShader = (CCShaderInstance) e;
            cosmicTime = Objects.requireNonNull(cosmicShader.getUniform("time"));
            cosmicYaw = Objects.requireNonNull(cosmicShader.getUniform("yaw"));
            cosmicPitch = Objects.requireNonNull(cosmicShader.getUniform("pitch"));
            cosmicExternalScale = Objects.requireNonNull(cosmicShader.getUniform("externalScale"));
            cosmicOpacity = Objects.requireNonNull(cosmicShader.getUniform("opacity"));
            cosmicUVs = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs"));

            cosmicExternalScale.set(1.0F);
            cosmicOpacity.set(0.78F);
            applyCosmicTime();

            cosmicShader.onApply(AvaritiaShaders::applyCosmicTime);
        });

        // Register black_hole shader
        event.registerShader(CCShaderInstance.create(event.getResourceProvider(),
                new ResourceLocation(PotionEnchantMod.MODID, "black_hole"),
                DefaultVertexFormat.BLOCK), e -> {
            blackHoleShader = (CCShaderInstance) e;
            blackHoleTime = Objects.requireNonNull(blackHoleShader.getUniform("time"));
            blackHoleYaw = Objects.requireNonNull(blackHoleShader.getUniform("yaw"));
            blackHolePitch = Objects.requireNonNull(blackHoleShader.getUniform("pitch"));
            blackHoleZoom = Objects.requireNonNull(blackHoleShader.getUniform("iZoom"));
            blackHoleScreenSize = Objects.requireNonNull(blackHoleShader.getUniform("screenSize"));

            applyBlackHoleTime();
            blackHoleShader.onApply(AvaritiaShaders::applyBlackHoleTime);
        });
    }

    /**
     * 刷新 cosmic UV 数据：从方块图集中读取所有 cosmic 纹理的 UV 坐标
     */
    public static void refreshCosmicUVs() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < COSMIC_TEXTURE_COUNT; ++i) {
            TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(PotionEnchantMod.rl("item/cosmic_" + i));
            COSMIC_UVS[i * 4]     = sprite.getU0();
            COSMIC_UVS[i * 4 + 1] = sprite.getV0();
            COSMIC_UVS[i * 4 + 2] = sprite.getU1();
            COSMIC_UVS[i * 4 + 3] = sprite.getV1();
        }
    }

    /**
     * 上传所有 cosmic uniform 到着色器（yaw, pitch, scale, opacity, UVs）
     * 在每帧渲染前调用以确保 uniform 更新
     */
    /**
     * 上传所有 cosmic uniform 到着色器
     * 每帧先从方块图集重新读取 UV（确保图集加载完成后拿到正确数据）
     */
    public static void uploadCosmicUniforms() {
        if (cosmicShader == null) return;

        // 每帧重新从图集读取 UV（参考 Adorable Armory 的做法）
        refreshCosmicUVs();

        Minecraft mc = Minecraft.getInstance();
        float yaw = 0.0F;
        float pitch = 0.0F;
        if (mc.player != null) {
            yaw = (float) (mc.player.getYRot() * Math.PI / 180.0F);
            pitch = -(float) (mc.player.getXRot() * Math.PI / 180.0F);
        }

        applyCosmicTime();
        if (cosmicYaw != null) cosmicYaw.set(yaw);
        if (cosmicPitch != null) cosmicPitch.set(pitch);
        if (cosmicExternalScale != null) cosmicExternalScale.set(1.0F);
        if (cosmicOpacity != null) cosmicOpacity.set(0.78F);
        if (cosmicUVs != null) cosmicUVs.setMatrix2x2Array(COSMIC_UVS, COSMIC_TEXTURE_COUNT);
    }

    /**
     * TextureStitchEvent.Post 时确保 UV 重新刷新
     * 此时方块图集已完全准备好，所有 cosmic 纹理都在正确位置
     */

    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            refreshCosmicUVs();
            PotionEnchantMod.LOGGER.info("[AvaritiaShaders] Block atlas stitched, refreshed cosmic UVs");
        }
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
        inventoryRender = true;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void drawScreenPost(final ScreenEvent.Render.Post e) {
        inventoryRender = false;
    }

    private AvaritiaShaders() {}
}