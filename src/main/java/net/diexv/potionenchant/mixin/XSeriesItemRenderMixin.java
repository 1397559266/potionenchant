package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.diexv.potionenchant.client.renderer.hyperlink.PolygonRenderer;
import net.diexv.potionenchant.client.renderer.hyperlink.XSeriesItemRenderer;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 为 X 系列装备添加物品特效渲染（旋转/闪烁/粒子）
 * 从 Hyperlink 的 ItemRendererMixin 移植
 */
@Mixin(ItemRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class XSeriesItemRenderMixin {

    @Unique
    private static final Random RANDOM = new Random();

    @Unique
    private final Set<XSeriesItemRenderer.Particle> xSeriesParticles = new HashSet<>();

    @Unique
    private int xSeriesColor = 0xFFFFFFFF;

    /**
     * 在渲染前注入变换（旋转/缩放）
     */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void onRenderPre(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int combinedLight, int combinedOverlay, BakedModel model,
                             CallbackInfo ci) {
        if (!isXSeries(stack)) return;

        long millis = Util.getMillis();
        double cycle = millis % 20000;

        // 随机缩放心跳
        if (cycle <= 200 || (6000 < cycle && cycle <= 6200) ||
            (10000 < cycle && cycle <= 10300) || (10400 < cycle && cycle <= 10450)) {
            float sc = RANDOM.nextFloat(0.7f, 1.5f);
            poseStack.scale(sc, sc, sc);
            if (10000 < cycle) {
                xSeriesColor = (0xFF000000) | RANDOM.nextInt(0xFFFFFF);
            } else {
                xSeriesColor = 0xFFFFFFFF;
            }
        } else {
            xSeriesColor = 0xFFFFFFFF;
        }

        // 持续旋转已禁用
    }

    /**
     * 重定向颜色渲染（闪烁变色）
     */
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
    private void onRenderModel(ItemRenderer instance, BakedModel model, ItemStack stack,
                                int combinedLight, int combinedOverlay,
                                PoseStack poseStack, VertexConsumer consumer) {
        if (isXSeries(stack) && xSeriesColor != 0xFFFFFFFF) {
            PolygonRenderer.model(model, poseStack, consumer, combinedLight, combinedOverlay,
                    quad -> xSeriesColor);
        } else {
            instance.renderModelLists(model, stack, combinedLight, combinedOverlay, poseStack, consumer);
        }
    }

    /**
     * 在渲染后注入粒子
     */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
            ordinal = 1))
    private void onRenderPost(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                              PoseStack poseStack, MultiBufferSource buffer,
                              int combinedLight, int combinedOverlay, BakedModel model,
                              CallbackInfo ci) {
        if (!isXSeries(stack)) return;

        // 生成粒子
        for (int count = 0; count < 2; count++) {
            if (RANDOM.nextInt(200) == 0) {
                xSeriesParticles.add(new XSeriesItemRenderer.Particle(stack));
            }
        }

        // 渲染粒子（保持在物品上方）
        PolygonRenderer.with(poseStack, () -> {
            if (context == ItemDisplayContext.GUI) {
                poseStack.translate(0, 0, 0.1);
            } else if (context == ItemDisplayContext.FIXED) {
                poseStack.translate(0, 0, -0.1);
            }
            xSeriesParticles.removeIf(p -> p.render(stack, context, poseStack, buffer, combinedLight, combinedOverlay));
        });
    }

    @Unique
    private static boolean isXSeries(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        return item == ModItems.X_SWORD.get()
            || item == ModItems.X_PICKAXE.get()
            || item == ModItems.X_AXE.get()
            || item == ModItems.X_SHOVEL.get()
            || item == ModItems.X_HOE.get()
            || item == ModItems.X_HELMET.get()
            || item == ModItems.X_CHESTPLATE.get()
            || item == ModItems.X_LEGGINGS.get()
            || item == ModItems.X_BOOTS.get();
    }
}
