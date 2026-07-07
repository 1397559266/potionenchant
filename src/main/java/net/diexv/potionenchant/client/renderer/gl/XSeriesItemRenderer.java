package net.diexv.potionenchant.client.renderer.gl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;

import java.util.Random;

/**
 * X 系列装备物品特效渲染器
 * 从 Hyperlink 的 GashatItemRenderer + GameOrbRenderer 移植
 * 
 * 提供：
 * - 物品悬浮/旋转动画
 * - 随机闪烁变色
 * - 漂浮三角形粒子特效
 * - Halo 光晕效果
 */
@OnlyIn(Dist.CLIENT)
public class XSeriesItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final Random RANDOM = new Random();
    private static final java.util.Set<Particle> PARTICLES = new java.util.HashSet<>();

    private final long delay = RANDOM.nextLong(0, 10000000);

    public XSeriesItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
              Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);

        // ---- Halo 光晕（仅在 GUI 和掉落物形态）----
        if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED) {
            VertexConsumer haloConsumer = ItemRenderer.getFoilBuffer(
                    buffer, Sheets.translucentCullBlockSheet(), true, stack.hasFoil());

            PolygonRenderer.with(poseStack, () -> {
                if (context == ItemDisplayContext.GUI) {
                    poseStack.translate(0, 0, -0.25);
                } else {
                    poseStack.translate(0, 0, 0.03125);
                }
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(1.3f, 1.3f, 1.3f);
                poseStack.translate(-0.5, -0.5, -0.5);

                float pulse = 0.6f + 0.4f * Mth.cos((Util.getMillis() % 2000) / 1000f * Mth.PI * 2);
                int haloColor = (int)(0x80 * pulse) << 24 | 0x60A0FF;
                renderHalo(poseStack, haloConsumer, context, packedLight, packedOverlay, haloColor);
            });
        }

        // ---- 主物品渲染（带旋转/闪烁）----
        PolygonRenderer.with(poseStack, () -> {
            renderMainModel(poseStack, buffer, context, packedLight, packedOverlay, stack, model);
        });

        // ---- 漂浮三角形粒子 ----
        for (int count = 0; count < 2; count++) {
            if (RANDOM.nextInt(200) == 0) {
                PARTICLES.add(new Particle(stack));
            }
        }
        // Oculus光影兼容：延迟渲染粒子到 AFTER_LEVEL 阶段
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            if (!PARTICLES.isEmpty()) {
                DeferredParticleQueue.enqueue(stack, context, poseStack, packedLight, packedOverlay, new java.util.HashSet<>(PARTICLES));
                PARTICLES.clear();
            }
        } else {
            // Try direct render first, defer to queue if fails
            int before = PARTICLES.size();
            PARTICLES.removeIf(p -> p.render(stack, context, poseStack, buffer, packedLight, packedOverlay));
            // If direct render didn't consume all particles (e.g. Oculus not detected), try deferred
            if (!PARTICLES.isEmpty()) {
                DeferredParticleQueue.enqueue(stack, context, poseStack, packedLight, packedOverlay, new java.util.HashSet<>(PARTICLES));
                PARTICLES.clear();
            }
        }
    }

    private void renderMainModel(PoseStack poseStack, MultiBufferSource buffer,
                                  ItemDisplayContext context, int light, int overlay,
                                  ItemStack stack, BakedModel model) {
        VertexConsumer consumer = ItemRenderer.getFoilBuffer(
                buffer, Sheets.translucentCullBlockSheet(), true, stack.hasFoil());

        // 获取已缓存的模型
        BakedModel baked = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);

        poseStack.translate(0.5, 0.5, 0.5);

        // ---- 动画效果 ----
        long time = Util.getMillis();
        long delayed = (this.delay + time);
        double cycle = delayed % 20000;

        // 持续旋转
        float cos = Mth.cos(delayed / 800f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(cos * 6));
        poseStack.mulPose(Axis.XP.rotationDegrees(cos * 2));
        poseStack.mulPose(Axis.YP.rotationDegrees(cos * 3));

        poseStack.translate(-0.5, -0.5, -0.5);

        // 渲染模型
        int tintColor = 0xFFFFFFFF;
        if (10000 < cycle && cycle <= 10300) {
            tintColor = (0xFF000000) | RANDOM.nextInt(0xFFFFFF);
        }
        final int finalColor = tintColor;
        PolygonRenderer.model(baked, poseStack, consumer, light, overlay,
                quad -> finalColor);
    }

    /**
     * 渲染光晕效果
     */
    public static void renderHalo(PoseStack poseStack, VertexConsumer consumer,
                                   ItemDisplayContext context, int light, int overlay,
                                   int color) {
        double delta = ((1 + Math.cos(Util.getMillis() / 800d)) / 2);

        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            poseStack.translate(0.1, 0, -0.15);
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(Axis.YN.rotationDegrees(15));
            poseStack.mulPose(Axis.XN.rotationDegrees(20));
            poseStack.translate(-0.5, -0.5, -0.5);
        } else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            poseStack.translate(-0.1, 0, -0.15);
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(15));
            poseStack.mulPose(Axis.XN.rotationDegrees(20));
            poseStack.translate(-0.5, -0.5, -0.5);
        }

        float size = (float) Mth.lerp(delta, 1.25f, 2);
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(size, size, size);
        poseStack.translate(-0.5, -0.5, -0.5);

        // 使用模型渲染光晕
        BakedModel haloModel = Minecraft.getInstance().getModelManager()
                .getModel(net.minecraft.resources.ResourceLocation.withDefaultNamespace("builtin/entity"));
        PolygonRenderer.model(haloModel, poseStack, consumer, light, overlay,
                quad -> color);
    }

    // ========== 贴图粒子类 ==========

    public static class Particle {
        private final ItemStack stack;
        private final long made;
        private final float age;
        private final long particleDelay;
        private final int tintColor;
        private final double x;
        private final double y;
        private final float xRot;
        private final float yRot;
        private final float zRot;
        private final ResourceLocation texture;

        private static final java.util.List<ResourceLocation> TEXTURES = new java.util.ArrayList<>();
        private static boolean texturesLoaded = false;

        /** 获取粒子贴图池（供主菜单使用） */
        public static java.util.List<ResourceLocation> getParticleTextures() { return TEXTURES; }

        public static void ensureTexturesLoaded() {
            if (texturesLoaded) return;
            texturesLoaded = true;
            try {
                var resourceManager = Minecraft.getInstance().getResourceManager();
                // 搜索 textures/particle/ 下所有 PNG（含子目录）
                var resources = resourceManager.listResources("textures/particle",
                    s -> s.getPath().endsWith(".png"));
                for (var entry : resources.entrySet()) {
                    var loc = entry.getKey();
                    if ("potionenchant".equals(loc.getNamespace())) {
                        TEXTURES.add(loc);
                    }
                }
            } catch (Exception e) {
                // 没有贴图时静默处理
            }
        }

        /**
         * 确保粒子贴图已加载（供主菜单等外部调用）
         */
        public static void ensureLoaded() {
            ensureTexturesLoaded();
        }

        public Particle(ItemStack stack) {
            this.stack = stack;
            this.made = Util.getMillis();
            this.age = RANDOM.nextInt(500, 1000);
            this.particleDelay = RANDOM.nextLong(0, 10000000);
            this.tintColor = generateBluePinkColor(RANDOM);
            double angle = Math.toRadians(RANDOM.nextInt(360));
            this.x = Math.cos(angle) * 0.4;
            this.y = Math.sin(angle) * 0.4;
            this.xRot = RANDOM.nextFloat(-180, 180);
            this.yRot = RANDOM.nextFloat(-180, 180);
            this.zRot = RANDOM.nextFloat(-180, 180);
            // 从文件夹随机选一张贴图
            ensureTexturesLoaded();
            if (!TEXTURES.isEmpty()) {
                this.texture = TEXTURES.get(RANDOM.nextInt(TEXTURES.size()));
            } else {
                this.texture = null;
            }
        }

        public Particle(ItemStack stack, ResourceLocation tex) {
            this.stack = stack;
            this.made = Util.getMillis();
            this.age = RANDOM.nextInt(500, 1000);
            this.particleDelay = RANDOM.nextLong(0, 10000000);
            this.tintColor = generateBluePinkColor(RANDOM);
            double angle = Math.toRadians(RANDOM.nextInt(360));
            this.x = Math.cos(angle) * 0.4;
            this.y = Math.sin(angle) * 0.4;
            this.xRot = RANDOM.nextFloat(-180, 180);
            this.yRot = RANDOM.nextFloat(-180, 180);
            this.zRot = RANDOM.nextFloat(-180, 180);
            this.texture = tex;
        }


        public boolean render(ItemStack stack, ItemDisplayContext context,
                              PoseStack poseStack, MultiBufferSource bufferSource,
                              int light, int overlay) {
            if (Util.getMillis() - this.made >= this.age) return true;
            if (this.stack != stack) return false;

            PolygonRenderer.with(poseStack, () -> {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.translate(this.x, this.y, 0);

                float angle = (this.particleDelay + Util.getMillis()) / 1000f;
                poseStack.mulPose(Axis.XP.rotationDegrees(this.xRot + angle * 30));
                poseStack.mulPose(Axis.YP.rotationDegrees(this.yRot + angle * 20));
                poseStack.mulPose(Axis.ZP.rotationDegrees(this.zRot + angle * 40));

                float life = (float)(Util.getMillis() - this.made) / this.age;
                float scale = 1.0f - life * 0.3f;
                if (scale < 0.1f) scale = 0.1f;
                poseStack.scale(scale * 0.4f, scale * 0.4f, scale * 0.4f);

                float alpha = 0.5f * (1.0f - life * 0.7f);
                int a = (int)(alpha * 255);
                float boost = 1.8f;
                int r = Math.min(255, (int)(FastColor.ARGB32.red(tintColor) * boost));
                int g = Math.min(255, (int)(FastColor.ARGB32.green(tintColor) * boost));
                int b = Math.min(255, (int)(FastColor.ARGB32.blue(tintColor) * boost));

                if (texture != null) {
                    var type = PolygonRenderer.RenderTypes.additiveEntityTranslucent(texture);
                    var consumer = bufferSource.getBuffer(type);
                    var matrix = poseStack.last().pose();
                    var normal = poseStack.last().normal();
                    consumer.vertex(matrix, -0.5f, -0.5f, 0).color(r, g, b, a).uv(0, 0).overlayCoords(overlay).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                    consumer.vertex(matrix,  0.5f, -0.5f, 0).color(r, g, b, a).uv(1, 0).overlayCoords(overlay).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                    consumer.vertex(matrix,  0.5f,  0.5f, 0).color(r, g, b, a).uv(1, 1).overlayCoords(overlay).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                    consumer.vertex(matrix, -0.5f,  0.5f, 0).color(r, g, b, a).uv(0, 1).overlayCoords(overlay).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                }
            });

            return false;
        }
    }

    /**
     * 生成蓝-粉色调之间的随机颜色
     */
    private static int generateBluePinkColor(Random rand) {
        // Hyperlink 风格：完全随机 24位颜色
        return rand.nextInt(0xFFFFFF);
    }
}

