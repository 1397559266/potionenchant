package net.diexv.potionenchant.client.renderer.hyperlink;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Oculus 模式下延迟渲染的粒子队列
 * 粒子与星空一起延迟到 AFTER_LEVEL 阶段渲染
 */
public class DeferredParticleQueue {
    private static final List<Entry> QUEUE = new ArrayList<>();

    public static void enqueue(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                int packedLight, int packedOverlay,
                                Set<XSeriesItemRenderer.Particle> particles) {
        PoseStack copy = new PoseStack();
        copy.last().pose().set(poseStack.last().pose());
        copy.last().normal().set(poseStack.last().normal());
        QUEUE.add(new Entry(stack.copy(), context, copy, packedLight, packedOverlay, particles));
    }

    public static void renderAll(MultiBufferSource.BufferSource buffers) {
        if (QUEUE.isEmpty()) return;
        for (Entry entry : QUEUE) {
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
        }
        QUEUE.clear();
    }

    public static boolean isEmpty() {
        return QUEUE.isEmpty();
    }

    private record Entry(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                         int packedLight, int packedOverlay,
                         Set<XSeriesItemRenderer.Particle> particles) {}
}