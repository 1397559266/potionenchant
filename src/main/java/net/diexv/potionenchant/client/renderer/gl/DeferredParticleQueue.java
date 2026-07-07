package net.diexv.potionenchant.client.renderer.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.diexv.potionenchant.client.compat.oculus.LateShaderLayerState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Oculus 模式下延迟渲染的粒子队列
 * 粒子与星空一起延迟到 AFTER_LEVEL 阶段渲染
 * 保存渲染时的投影/模型视图矩阵，在延迟阶段正确恢复
 */
public class DeferredParticleQueue {
    private static final List<Entry> QUEUE = new ArrayList<>();

    public static void enqueue(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                int packedLight, int packedOverlay,
                                Set<XSeriesItemRenderer.Particle> particles) {
        PoseStack copy = new PoseStack();
        copy.last().pose().set(poseStack.last().pose());
        copy.last().normal().set(poseStack.last().normal());
        QUEUE.add(new Entry(stack.copy(), context, copy, packedLight, packedOverlay, particles,
                new Matrix4f(RenderSystem.getModelViewMatrix()),
                new Matrix4f(RenderSystem.getProjectionMatrix())));
    }

    public static void renderAll(MultiBufferSource.BufferSource buffers) {
        if (QUEUE.isEmpty()) return;
        
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        try {
            LateShaderLayerState.prepareMainTargetPass();
            
            for (Entry entry : QUEUE) {
                modelViewStack.last().pose().set(entry.modelView());
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f(entry.projection()), VertexSorting.DISTANCE_TO_ORIGIN);
                
                ItemDisplayContext ctx = entry.context();
                PolygonRenderer.with(entry.poseStack(), () -> {
                    PoseStack ps = entry.poseStack();
                    if (ctx == ItemDisplayContext.GUI) {
                        ps.translate(0, 0, 0.1);
                    } else if (ctx == ItemDisplayContext.FIXED) {
                        ps.translate(0, 0, -0.1);
                    }
                    entry.particles().removeIf(p -> p.render(entry.stack(), ctx, ps, buffers, entry.packedLight(), entry.packedOverlay()));
                });
                
                buffers.endBatch();
            }
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.DISTANCE_TO_ORIGIN);
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
            LateShaderLayerState.finishMainTargetPass();
        }
        QUEUE.clear();
    }

    public static boolean isEmpty() {
        return QUEUE.isEmpty();
    }

    private record Entry(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                         int packedLight, int packedOverlay,
                         Set<XSeriesItemRenderer.Particle> particles,
                         Matrix4f modelView, Matrix4f projection) {}
}
