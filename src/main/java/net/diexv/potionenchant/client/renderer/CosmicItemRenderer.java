package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.SkyRender.client.model.CosmicBakeModel;
import net.diexv.potionenchant.client.compat.oculus.ItemRenderCompatibilityContext;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 全模组「带着色器物品」的统一物品渲染器 —— 用 Forge 官方接缝完全平替原来的 {@code ItemRendererMixin}。
 *
 * <p>接缝：{@code Item#initializeClient(Consumer<IClientItemExtensions>)} →
 * {@code IClientItemExtensions#getCustomRenderer()}。这不是事件，而是 Forge 打在
 * {@code ItemRenderer} 里的接口查询 —— 已从 Forge 版 {@code ItemRenderer} 字节码确认：
 * <pre>
 *   112: invokestatic  ForgeHooksClient.handleCameraTransforms(pose, model, ctx, leftHand)
 *   113: ldc -0.5f ×3 → PoseStack.translate(-0.5,-0.5,-0.5)
 *   114: invokeinterface BakedModel.isCustomRenderer()
 *   151: invokestatic  IClientItemExtensions.of(stack) → invokeinterface getCustomRenderer()
 *        → BlockEntityWithoutLevelRenderer.renderByItem(stack, ctx, pose, buffers, light, overlay)
 * </pre>
 * 也就是说：**相机变换与 -0.5 平移由 Forge 自己已经做完**，本类里绝对不能再补一遍
 * （原 Mixin 之所以要手动补，是因为它在 HEAD 就把原版渲染 cancel 掉了，绕过了这一段）。
 *
 * <p>另外，Forge 默认实现返回的是原版那个硬编码 BEWLR（只处理盾/三叉戟/望远镜等），
 * 所以对"没有声明本接缝却把模型标成 isCustomRenderer 的物品"会退化成什么都不画 ——
 * 为此 {@link CosmicBakeModel#getQuads} 增加了"回落到被包裹模型"的安全网，
 * 保证任何情况下都不会画成空白。
 *
 * <p>X 系列附加特效（随机心跳缩放 / 变色闪烁 / 漂浮贴图粒子）已按需求全部删除，
 * 本类只负责"基底贴图 + 着色器 mask 层 + 代码雨/雪花"这条主路径。
 */
public final class CosmicItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static volatile CosmicItemRenderer INSTANCE;

    /** 惰性单例：不要在类初始化阶段碰 Minecraft 实例 */
    public static CosmicItemRenderer get() {
        CosmicItemRenderer local = INSTANCE;
        if (local == null) {
            synchronized (CosmicItemRenderer.class) {
                local = INSTANCE;
                if (local == null) {
                    Minecraft mc = Minecraft.getInstance();
                    local = new CosmicItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    private CosmicItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers,
                             int packedLight, int packedOverlay) {
        // 【物品展示框兼容】先把"已经排队的几何体"刷出去，再画我们的物品。
        // 原因：展示框本体（以及它前面的实体几何）先入队了，如果我们的物品和它在同一批里画，
        // 正视图与框体重叠的部分会被框体压住 —— 表现为"侧面能看到、正面看不到"。
        // 先 endBatch() 让框体这一批先落地，我们的物品就落在自己后一趟批次里，重叠处按我们的绘制顺序取胜。
        // （这也是原版 BEWLR（盾/三叉戟等）在 renderByItem 内部自己 endBatch 的同一个道理。）
        if (buffers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }

        ItemRenderCompatibilityContext.beginItemRender(ctx);
        try {
            ItemShaderModCompat.logCompatModeOnce();
            BakedModel model = resolveCosmicModel(stack);
            if (model instanceof CosmicBakeModel cosmic) {
                // 变换已由 Forge 施加，这里只画。
                // push/pop 只保证渲染器内部的 pose 变更不泄漏到 Forge 的栈。
                // 注：原 XSeriesItemRenderMixin 的三件套（随机心跳缩放 / 变色闪烁 / 漂浮贴图粒子）
                // 已按需求全部删除 —— 抖动多余、彩色环绕粒子不需要，这里不再有任何 X 系列附加特效。
                pose.pushPose();
                try {
                    cosmic.renderItem(stack, ctx, pose, buffers, packedLight, packedOverlay);
                } finally {
                    pose.popPose();
                }
                return;
            }
            // 兜底：模型管理器没给出 CosmicBakeModel 时按原版方式把模型画出来，绝不空手。
            // 【绝不能回调 ItemRenderer.render】—— 那会再次进到本方法形成死循环。
            if (model != null) {
                for (BakedModel pass : model.getRenderPasses(stack, true)) {
                    for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                        Minecraft.getInstance().getItemRenderer().renderModelLists(
                                pass, stack, packedLight, packedOverlay, pose, buffers.getBuffer(renderType));
                    }
                }
            }
        } finally {
            ItemRenderCompatibilityContext.endItemRender();
        }
    }

    /**
     * 找回 CosmicBakeModel：优先"已解析"的模型（这样 x_sword 的 supermode 覆盖模型也拿得到，
     * 基底贴图与 mask 层才是一致的超模式外观），再退回物品模型管理器里的那份。
     */
    private static BakedModel resolveCosmicModel(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        try {
            BakedModel byContext = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
            if (byContext instanceof CosmicBakeModel) {
                return byContext;
            }
            BakedModel shaper = mc.getItemRenderer().getItemModelShaper().getItemModel(stack);
            if (shaper instanceof CosmicBakeModel) {
                return shaper;
            }
            return byContext != null ? byContext : shaper;
        } catch (Throwable ignored) {
            try {
                return mc.getItemRenderer().getItemModelShaper().getItemModel(stack);
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }
}
