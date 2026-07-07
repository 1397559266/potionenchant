package net.diexv.potionenchant.client.renderer.gl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class PolygonRenderer {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Direction[] QUAD_FACES = Arrays.copyOf(Direction.values(), Direction.values().length + 1);
    public static final String MODID = "potionenchant";

    private PolygonRenderer() {}

    public static void with(PoseStack poseStack, Runnable runnable) {
        poseStack.pushPose();
        runnable.run();
        poseStack.popPose();
    }

    public static MultiBufferSource.BufferSource bufferSource() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static VertexConsumer getBuffer(RenderType type) {
        return bufferSource().getBuffer(type);
    }

    public static void endBatch(RenderType type) {
        bufferSource().endBatch(type);
    }

    // ========== 正方体渲染 ==========

    public static void cube(Matrix4f matrix, VertexConsumer builder,
                            float startX, float startY, float startZ,
                            float endX, float endY, float endZ,
                            int color, Predicate<Direction> predicate) {
        float alpha = FastColor.ARGB32.alpha(color) / 255f;
        float red = FastColor.ARGB32.red(color) / 255f;
        float green = FastColor.ARGB32.green(color) / 255f;
        float blue = FastColor.ARGB32.blue(color) / 255f;

        if (predicate.test(Direction.DOWN)) {
            builder.vertex(matrix, startX, startY, startZ).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();
            builder.vertex(matrix, endX, startY, startZ).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();
            builder.vertex(matrix, endX, startY, endZ).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();
            builder.vertex(matrix, startX, startY, endZ).color(red, green, blue, alpha).normal(0, -1, 0).endVertex();
        }
        if (predicate.test(Direction.UP)) {
            builder.vertex(matrix, startX, endY, startZ).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
            builder.vertex(matrix, startX, endY, endZ).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
            builder.vertex(matrix, endX, endY, endZ).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
            builder.vertex(matrix, endX, endY, startZ).color(red, green, blue, alpha).normal(0, 1, 0).endVertex();
        }
        if (predicate.test(Direction.NORTH)) {
            builder.vertex(matrix, startX, startY, startZ).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();
            builder.vertex(matrix, startX, endY, startZ).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();
            builder.vertex(matrix, endX, endY, startZ).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();
            builder.vertex(matrix, endX, startY, startZ).color(red, green, blue, alpha).normal(0, 0, -1).endVertex();
        }
        if (predicate.test(Direction.SOUTH)) {
            builder.vertex(matrix, startX, startY, endZ).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();
            builder.vertex(matrix, endX, startY, endZ).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();
            builder.vertex(matrix, endX, endY, endZ).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();
            builder.vertex(matrix, startX, endY, endZ).color(red, green, blue, alpha).normal(0, 0, 1).endVertex();
        }
        if (predicate.test(Direction.WEST)) {
            builder.vertex(matrix, startX, startY, startZ).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();
            builder.vertex(matrix, startX, startY, endZ).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();
            builder.vertex(matrix, startX, endY, endZ).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();
            builder.vertex(matrix, startX, endY, startZ).color(red, green, blue, alpha).normal(-1, 0, 0).endVertex();
        }
        if (predicate.test(Direction.EAST)) {
            builder.vertex(matrix, endX, startY, startZ).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
            builder.vertex(matrix, endX, endY, startZ).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
            builder.vertex(matrix, endX, endY, endZ).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
            builder.vertex(matrix, endX, startY, endZ).color(red, green, blue, alpha).normal(1, 0, 0).endVertex();
        }
    }

    // ========== AABB 立方体渲染（填充）==========

    public static void cubeBox(Matrix4f matrix, VertexConsumer builder,
                                AABB box, int color, Predicate<Direction> predicate) {
        cube(matrix, builder, (float)box.minX, (float)box.minY, (float)box.minZ,
             (float)box.maxX, (float)box.maxY, (float)box.maxZ, color, predicate);
    }

    // ========== 空心三角形渲染 ==========

    public static void hollowTriangle(Matrix4f matrix, VertexConsumer builder,
                                       float size, float thickness, int color) {
        float h = size * 0.866f;
        float half = size * 0.5f;
        float t = thickness;
        float r = FastColor.ARGB32.red(color) / 255f;
        float g = FastColor.ARGB32.green(color) / 255f;
        float b = FastColor.ARGB32.blue(color) / 255f;
        float a = FastColor.ARGB32.alpha(color) / 255f;

        float ax = 0, ay = h/2;
        float bx = -half, by = -h/2;
        float cx = half, cy = -h/2;

        float abx = bx - ax, aby = by - ay;
        float abLen = (float)Math.sqrt(abx*abx + aby*aby);
        float abNx = -aby / abLen, abNy = abx / abLen;
        float bcx = cx - bx, bcy = cy - by;
        float bcLen = (float)Math.sqrt(bcx*bcx + bcy*bcy);
        float bcNx = -bcy / bcLen, bcNy = bcx / bcLen;
        float cax = ax - cx, cay = ay - cy;
        float caLen = (float)Math.sqrt(cax*cax + cay*cay);
        float caNx = -cay / caLen, caNy = cax / caLen;

        builder.vertex(matrix, ax + abNx*t, ay + abNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, bx + abNx*t, by + abNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, bx - abNx*t, by - abNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, ax - abNx*t, ay - abNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, bx + bcNx*t, by + bcNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, cx + bcNx*t, cy + bcNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, cx - bcNx*t, cy - bcNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, bx - bcNx*t, by - bcNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, cx + caNx*t, cy + caNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, ax + caNx*t, ay + caNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, ax - caNx*t, ay - caNy*t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, cx - caNx*t, cy - caNy*t, 0).color(r,g,b,a).endVertex();
    }

    // ========== 空心四边形渲染 ==========

    public static void hollowQuad(Matrix4f matrix, VertexConsumer builder,
                                   float size, float thickness, int color) {
        float half = size * 0.5f;
        float t = thickness;
        float r = FastColor.ARGB32.red(color) / 255f;
        float g = FastColor.ARGB32.green(color) / 255f;
        float b = FastColor.ARGB32.blue(color) / 255f;
        float a = FastColor.ARGB32.alpha(color) / 255f;

        builder.vertex(matrix, -half, half, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix,  half, half, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix,  half, half-t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, -half, half-t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, -half, -half, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix,  half, -half, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix,  half, -half+t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, -half, -half+t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, -half, half-t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, -half, -half+t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, -half+t, -half+t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, -half+t, half-t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, half-t, half-t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, half-t, -half+t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, half, -half+t, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, half, half-t, 0).color(r,g,b,a).endVertex();
    }

    // ========== 闪电渲染 ==========

    public static void lightningBolt(Matrix4f matrix, VertexConsumer builder,
                                      float length, float thickness, int color) {
        float r = FastColor.ARGB32.red(color) / 255f;
        float g = FastColor.ARGB32.green(color) / 255f;
        float b = FastColor.ARGB32.blue(color) / 255f;
        float a = FastColor.ARGB32.alpha(color) / 255f;
        float h = length;
        float t = thickness;

        float p0x = 0, p0y = -h/2;
        float p1x = -h*0.18f, p1y = -h*0.08f;
        float p2x = h*0.12f, p2y = h*0.15f;
        float p3x = 0, p3y = h/2;

        drawThickSegment(matrix, builder, p0x, p0y, p1x, p1y, t, r, g, b, a);
        drawThickSegment(matrix, builder, p1x, p1y, p2x, p2y, t, r, g, b, a);
        drawThickSegment(matrix, builder, p2x, p2y, p3x, p3y, t, r, g, b, a);
    }

    private static void drawThickSegment(Matrix4f matrix, VertexConsumer builder,
                                          float x1, float y1, float x2, float y2,
                                          float thickness, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len < 0.0001f) return;
        float nx = -dy / len * thickness;
        float ny = dx / len * thickness;

        builder.vertex(matrix, x1 - nx, y1 - ny, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, x1 + nx, y1 + ny, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, x2 + nx, y2 + ny, 0).color(r,g,b,a).endVertex();
        builder.vertex(matrix, x2 - nx, y2 - ny, 0).color(r,g,b,a).endVertex();
    }

    // ========== 厚线框长方体渲染 ==========

    public static void thickLineBoxBox(Matrix4f matrix, VertexConsumer builder,
                                        AABB box, float thickness, int color) {
        float minX = (float)box.minX, minY = (float)box.minY, minZ = (float)box.minZ;
        float maxX = (float)box.maxX, maxY = (float)box.maxY, maxZ = (float)box.maxZ;
        float t = thickness;
        float r = FastColor.ARGB32.red(color) / 255f;
        float g = FastColor.ARGB32.green(color) / 255f;
        float b = FastColor.ARGB32.blue(color) / 255f;
        float a = FastColor.ARGB32.alpha(color) / 255f;

        thickEdge(matrix, builder, minX, minY, minZ, maxX, minY, minZ, t, r, g, b, a);
        thickEdge(matrix, builder, maxX, minY, minZ, maxX, minY, maxZ, t, r, g, b, a);
        thickEdge(matrix, builder, maxX, minY, maxZ, minX, minY, maxZ, t, r, g, b, a);
        thickEdge(matrix, builder, minX, minY, maxZ, minX, minY, minZ, t, r, g, b, a);
        thickEdge(matrix, builder, minX, maxY, minZ, maxX, maxY, minZ, t, r, g, b, a);
        thickEdge(matrix, builder, maxX, maxY, minZ, maxX, maxY, maxZ, t, r, g, b, a);
        thickEdge(matrix, builder, maxX, maxY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
        thickEdge(matrix, builder, minX, maxY, maxZ, minX, maxY, minZ, t, r, g, b, a);
        thickEdge(matrix, builder, minX, minY, minZ, minX, maxY, minZ, t, r, g, b, a);
        thickEdge(matrix, builder, maxX, minY, minZ, maxX, maxY, minZ, t, r, g, b, a);
        thickEdge(matrix, builder, maxX, minY, maxZ, maxX, maxY, maxZ, t, r, g, b, a);
        thickEdge(matrix, builder, minX, minY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
    }

    private static void thickEdge(Matrix4f matrix, VertexConsumer builder,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float thickness, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.0001f) return;

        float ax, ay, az;
        if (Math.abs(dx) < Math.abs(dy) && Math.abs(dx) < Math.abs(dz)) {
            ax = 1; ay = 0; az = 0;
        } else if (Math.abs(dy) < Math.abs(dz)) {
            ax = 0; ay = 1; az = 0;
        } else {
            ax = 0; ay = 0; az = 1;
        }
        float nx1 = dy*az - dz*ay;
        float ny1 = dz*ax - dx*az;
        float nz1 = dx*ay - dy*ax;
        float nl1 = (float)Math.sqrt(nx1*nx1 + ny1*ny1 + nz1*nz1);
        if (nl1 < 0.0001f) return;
        nx1 /= nl1; ny1 /= nl1; nz1 /= nl1;
        float nx2 = ny1*dz - nz1*dy;
        float ny2 = nz1*dx - nx1*dz;
        float nz2 = nx1*dy - ny1*dx;
        float nl2 = (float)Math.sqrt(nx2*nx2 + ny2*ny2 + nz2*nz2);
        if (nl2 < 0.0001f) return;
        nx2 /= nl2; ny2 /= nl2; nz2 /= nl2;

        float ht = thickness * 0.5f;
        float ox1 = nx1*ht, oy1 = ny1*ht, oz1 = nz1*ht;
        float ox2 = nx2*ht, oy2 = ny2*ht, oz2 = nz2*ht;

        float p1x = x1 - ox1 - ox2, p1y = y1 - oy1 - oy2, p1z = z1 - oz1 - oz2;
        float p2x = x1 + ox1 - ox2, p2y = y1 + oy1 - oy2, p2z = z1 + oz1 - oz2;
        float p3x = x2 + ox1 + ox2, p3y = y2 + oy1 + oy2, p3z = z2 + oz1 + oz2;
        float p4x = x2 - ox1 + ox2, p4y = y2 - oy1 + oy2, p4z = z2 - oz1 + oz2;

        builder.vertex(matrix, p1x, p1y, p1z).color(r,g,b,a).endVertex();
        builder.vertex(matrix, p2x, p2y, p2z).color(r,g,b,a).endVertex();
        builder.vertex(matrix, p3x, p3y, p3z).color(r,g,b,a).endVertex();
        builder.vertex(matrix, p4x, p4y, p4z).color(r,g,b,a).endVertex();
    }

    // ========== 模型渲染 ==========

    public static void model(BakedModel model, ItemStack stack, PoseStack poseStack,
                              VertexConsumer consumer, int light, int overlay,
                              Function<BakedQuad, Integer> colorizer) {
        var modelData = net.minecraftforge.client.model.data.ModelData.EMPTY;
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (var face : QUAD_FACES) {
                List<BakedQuad> quads = pass.getQuads(null, face, RANDOM, modelData, null);
                if (!quads.isEmpty()) {
                    for (BakedQuad quad : quads) {
                        int c = colorizer.apply(quad);
                        float r = FastColor.ARGB32.red(c) / 255f;
                        float g = FastColor.ARGB32.green(c) / 255f;
                        float b = FastColor.ARGB32.blue(c) / 255f;
                        float a = FastColor.ARGB32.alpha(c) / 255f;
                        consumer.putBulkData(poseStack.last(), quad, r, g, b, a, light, overlay, true);
                    }
                }
            }
        }
    }

    public static void model(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                              int light, int overlay, Function<BakedQuad, Integer> colorizer) {
        model(model, ItemStack.EMPTY, poseStack, consumer, light, overlay, colorizer);
    }

    public static void model(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                              int light, int overlay) {
        model(model, poseStack, consumer, light, overlay, quad -> 0xFFFFFFFF);
    }

    // ========== 自定义 RenderType ==========

    public static class RenderTypes extends RenderType {
        private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        }

        public static final RenderType HIGHLIGHT = create(
                MODID + ":highlight",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 256, true, true,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(NO_TEXTURE)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        public static final RenderType LIGHTNING_NO_CULL = create(
                MODID + ":lightning_no_cull",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                        .setWriteMaskState(COLOR_WRITE)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setOutputState(WEATHER_TARGET)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));

        public static RenderType additiveEntityTranslucent(ResourceLocation texture) {
            return create(MODID + ":additive_entity_translucent",
                    DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setOutputState(MAIN_TARGET)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true));
        }

        /**
         * 标准 alpha 透明渲染（使用 TRANSLUCENT_TRANSPARENCY）
         * 参考 cosmic 着色器的 GL_SRC_ALPHA / GL_ONE_MINUS_SRC_ALPHA 混合模式
         * 必须保留 LIGHTMAP/OVERLAY，否则 entity_translucent 着色器采样的纹理单元未绑定会返回黑色
         */
        public static RenderType entityTranslucent(ResourceLocation texture) {
            return create(MODID + ":entity_translucent",
                    DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true));
        }

    }

    public static VertexConsumer colorize(VertexConsumer consumer, int color) {
        return new net.minecraftforge.client.model.pipeline.VertexConsumerWrapper(consumer) {
            @Override
            public VertexConsumer color(int r, int g, int b, int a) {
                return super.color(
                        FastColor.ARGB32.red(color),
                        FastColor.ARGB32.green(color),
                        FastColor.ARGB32.blue(color),
                        FastColor.ARGB32.alpha(color));
            }
        };
    }
}
