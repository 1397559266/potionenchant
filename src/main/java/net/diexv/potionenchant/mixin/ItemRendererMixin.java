package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.SkyRender.client.model.BlackHoleBakeModel;
import net.diexv.potionenchant.SkyRender.client.model.CosmicBakeModel;
import net.diexv.potionenchant.client.renderer.hyperlink.PolygonRenderer;
import net.diexv.potionenchant.client.renderer.hyperlink.XSeriesItemRenderer;
import net.diexv.potionenchant.client.compat.oculus.ItemRenderCompatibilityContext;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.diexv.potionenchant.client.renderer.hyperlink.DeferredParticleQueue;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

import java.util.Random;
import java.util.Set;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Unique
    private static final Random RANDOM = new Random();

    @Unique
    private final Set<XSeriesItemRenderer.Particle> xSeriesParticles = new HashSet<>();

    // Oculus 延迟粒子队列：当光影开启时，粒子与星空一起延迟渲染

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRenderItem(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack mStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel modelIn, CallbackInfo ci) {
        ItemRenderCompatibilityContext.beginItemRender(context);
        ItemShaderModCompat.logCompatModeOnce();

        if (modelIn instanceof CosmicBakeModel iItemRenderer) {
            ci.cancel();
            mStack.pushPose();
            try {
                final CosmicBakeModel renderer = (CosmicBakeModel) ForgeHooksClient.handleCameraTransforms(mStack, iItemRenderer, context, leftHand);
                mStack.translate(-0.5D, -0.5D, -0.5D);

                boolean cosmicDeferred = shouldDeferCosmic();
                renderer.renderItem(stack, context, mStack, buffers, packedLight, packedOverlay);

                // X 系列粒子特效
                if (isXSeries(stack)) {
                    for (int count = 0; count < 2; count++) {
                        if (RANDOM.nextInt(200) == 0) {
                            xSeriesParticles.add(new XSeriesItemRenderer.Particle(stack));
                        }
                    }

                    if (cosmicDeferred) {
                        // Oculus 模式下：粒子入延迟队列，与星空一起渲染
                        DeferredParticleQueue.enqueue(
                                stack, context, mStack, packedLight, packedOverlay,
                                new HashSet<>(xSeriesParticles));
                    } else {
                        // 正常模式：立即渲染粒子
                        PolygonRenderer.with(mStack, () -> {
                            if (context == ItemDisplayContext.GUI) {
                                mStack.translate(0, 0, 0.1);
                            } else if (context == ItemDisplayContext.FIXED) {
                                mStack.translate(0, 0, -0.1);
                            }
                            xSeriesParticles.removeIf(p -> p.render(stack, context, mStack, buffers, packedLight, packedOverlay));
                        });
                    }
                }
            } finally {
                mStack.popPose();
            }

            ItemRenderCompatibilityContext.endItemRender();
        } else if (modelIn instanceof BlackHoleBakeModel blackHoleRenderer) {
            ci.cancel();
            mStack.pushPose();
            try {
                final BlackHoleBakeModel renderer = (BlackHoleBakeModel) ForgeHooksClient.handleCameraTransforms(mStack, blackHoleRenderer, context, leftHand);
                mStack.translate(-0.5D, -0.5D, -0.5D);
                renderer.renderItem(stack, context, mStack, buffers, packedLight, packedOverlay);
            } finally {
                mStack.popPose();
                ItemRenderCompatibilityContext.endItemRender();
            }
        }
    }

    @Unique
    private static boolean shouldDeferCosmic() {
        return ItemShaderModCompat.isOculusShaderPackActive();
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void onRenderItemReturn(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack mStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel modelIn, CallbackInfo ci) {
        ItemRenderCompatibilityContext.endItemRender();
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