package net.diexv.potionenchant.mixin;

// =====================================================================================
// 【已停用 / 已被平替】原 ItemRendererMixin —— 保留文件与历史实现，供查阅与回退。
//
// 为什么停用：它用 @Inject(HEAD, cancellable) 抢在 ItemRenderer.render 之前把原版渲染 cancel 掉，
// 然后自己补 ForgeHooksClient.handleCameraTransforms + translate(-0.5,-0.5,-0.5) 再画。
// 但这套东西 Forge 本来就提供官方接缝，不需要 Mixin：
//
//   Item#initializeClient(Consumer<IClientItemExtensions>) → IClientItemExtensions#getCustomRenderer()
//
// 已从 Forge 版 ItemRenderer 字节码确认（不是推测）：
//   112: invokestatic  ForgeHooksClient.handleCameraTransforms(pose, model, ctx, leftHand)
//   113: ldc -0.5f ×3 → PoseStack.translate(-0.5, -0.5, -0.5)
//   114: invokeinterface BakedModel.isCustomRenderer()
//   151: invokestatic  IClientItemExtensions.of(stack) → invokeinterface getCustomRenderer()
// 也就是说：相机变换与 -0.5 平移【Forge 在调用自定义渲染器之前就已经做完】，
// 自定义渲染器只需在被变换过的坐标系里画即可（原版自带的盾/三叉戟 BEWLR 就是这个契约）。
//
// 现在的替代实现（全模组「带着色器物品」统一走这条路，不再有 ItemRenderer 的 mixin）：
//   net.diexv.potionenchant.client.renderer.CosmicItemRenderer   （BlockEntityWithoutLevelRenderer 子类）
//   net.diexv.potionenchant.client.CosmicClientItemExtensions    （字体 + getCustomRenderer 接缝）
//   net.diexv.potionenchant.client.DiexvClientItemExtensions     （同上 + BLOCK 手臂姿态，Code/DiexvSword/XSword 用）
//   各物品的 initializeClient（ModItems 的 X 装备/工具、药水瓶/书/护符/空瓶等）
// 同时 potionenchant.mixins.json 里已移除本类；CosmicBakeModel.getQuads 增加了"回落到被包裹模型"的安全网，
// 避免"空四边形 + isCustomRenderer"在没有接缝的路径上画成空白。
//
// 原实现（仅供查阅，勿直接启用；若确需回退，把下面注释解开并把本类加回 potionenchant.mixins.json）：
//
// import com.mojang.blaze3d.vertex.PoseStack;
// import net.diexv.potionenchant.SkyRender.client.model.CosmicBakeModel;
// import net.diexv.potionenchant.client.compat.oculus.ItemRenderCompatibilityContext;
// import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.entity.ItemRenderer;
// import net.minecraft.client.resources.model.BakedModel;
// import net.minecraft.world.item.ItemDisplayContext;
// import net.minecraft.world.item.ItemStack;
// import net.minecraftforge.client.ForgeHooksClient;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
// @Mixin(ItemRenderer.class)
// public abstract class ItemRendererMixin {
//
//     @Inject(method = "render", at = @At("HEAD"), cancellable = true)
//     public void onRenderItem(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack mStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel modelIn, CallbackInfo ci) {
//         ItemRenderCompatibilityContext.beginItemRender(context);
//         ItemShaderModCompat.logCompatModeOnce();
//
//         if (modelIn instanceof CosmicBakeModel iItemRenderer) {
//             ci.cancel();
//             mStack.pushPose();
//             try {
//                 final CosmicBakeModel renderer = (CosmicBakeModel) ForgeHooksClient.handleCameraTransforms(mStack, iItemRenderer, context, leftHand);
//                 mStack.translate(-0.5D, -0.5D, -0.5D);
//
//                 renderer.renderItem(stack, context, mStack, buffers, packedLight, packedOverlay);
//             } finally {
//                 mStack.popPose();
//             }
//
//             ItemRenderCompatibilityContext.endItemRender();
//         }
//     }
//
//     @Inject(method = "render", at = @At("RETURN"))
//     public void onRenderItemReturn(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack mStack, MultiBufferSource buffers, int packedLight, int packedOverlay, BakedModel modelIn, CallbackInfo ci) {
//         ItemRenderCompatibilityContext.endItemRender();
//     }
// }
// =====================================================================================

/** 停用后的占位：不再带 @Mixin 注解，因此 Mixin 不会处理它；保留类名避免构建脚本/文档引用失效。 */
final class ItemRendererMixin {
    private ItemRendererMixin() {
    }
}
