package net.diexv.potionenchant.mixin;

// =====================================================================================
// 【已停用 / 已被平替】原 XSeriesItemRenderMixin —— 保留文件与历史实现，供查阅与回退。
//
// 为什么停用：它挂在 ItemRenderer.render 上做三件事（随机心跳缩放 / renderModelLists 重定向染色 /
// popPose 处生成漂浮粒子）。这与物品渲染走的是同一类 mixin 注入，而 Forge 本来就提供官方接缝：
//
//   Item#initializeClient(Consumer<IClientItemExtensions>) → IClientItemExtensions#getCustomRenderer()
//
// 现已把同样的两件事搬进自定义物品渲染器（原理一致，只是宿主从 mixin 注入点换成 renderByItem）：
//   net.diexv.potionenchant.client.renderer.CosmicItemRenderer      （BEWLR；宿主）
//   net.diexv.potionenchant.client.renderer.gl.XSeriesItemEffects   （两件事的本体）
//   CosmicBakeModel.renderItem                                      （基底贴图染色读取 baseTint）
//
// 【特殊说明 · 三件事全部删除，均不再移植】
//   * 随机心跳缩放（物品模型抖动）—— 多余效果，先被删；
//   * renderModelLists 重定向染色（不定时逐帧随机跳色）—— 观感像彩灯频闪，后被删；
//   * popPose 处生成漂浮贴图粒子（环绕彩色粒子）—— 与已有的雪花/代码雨重复、不需要，本次删除。
// 随之删除的实现类：XSeriesItemEffects / XSeriesItemRenderer / XItemExtensions / DeferredParticleQueue
// （四个类都已无任何引用）。物品渲染现在只剩"基底贴图 + 着色器 mask 层 + 代码雨/雪花"这一条主路径。
//
// 附带说明：本 mixin 与 ItemRendererMixin 同时挂在 ItemRenderer.render 上，
// 而 ItemRendererMixin 在 HEAD 就 cancel 了带着色器物品的渲染 —— 也就是说本 mixin 的注入
// 对带着色器物品【从来没生效过】；v19 改走 BEWLR 后这些特效才第一次生效，随后按需求逐项删除。
//
// 原实现（仅供查阅，勿直接启用；若确需回退，把下面注释解开并把本类加回 potionenchant.mixins.json）：
//
// @Mixin(ItemRenderer.class)
// @OnlyIn(Dist.CLIENT)
// public abstract class XSeriesItemRenderMixin {
//     @Unique private static final Random RANDOM = new Random();
//     @Unique private final Set<XSeriesItemRenderer.Particle> xSeriesParticles = new HashSet<>();
//     @Unique private int xSeriesColor = 0xFFFFFFFF;
//
//     @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
//     private void onRenderPre(...) { /* 20s 周期窗口内随机缩放 + 变色 */ }
//
//     @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(...)V"))
//     private void onRenderModel(...) { /* 染色：PolygonRenderer.model(..., quad -> xSeriesColor) */ }
//
//     @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 1))
//     private void onRenderPost(...) { /* 生成粒子 + 渲染（Oculus 下延迟到 AFTER_LEVEL） */ }
// }
// =====================================================================================

/** 停用后的占位：不再带 @Mixin 注解，因此 Mixin 不会处理它；保留类名避免构建脚本/文档引用失效。 */
final class XSeriesItemRenderMixin {
    private XSeriesItemRenderMixin() {
    }
}
