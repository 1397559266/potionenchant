package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.compat.oculus.ItemShaderModCompat;
import net.diexv.potionenchant.client.compat.oculus.LateWorldBeamPass;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.diexv.potionenchant.item.DiexvSwordItem;
import net.diexv.potionenchant.item.XSwordItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 第三人称下的月牙剑气（近似位置）。
 *
 * <p>第一人称的挥砍动画来自 {@code IClientItemExtensions#applyForgeHandTransform}，那条接缝只在
 * 第一人称手持渲染里被调用；第三人称时物品跟的是原版手臂挥动，所以这里不复现动画，只用一个
 * <b>近似坐标系</b>把同一套月牙画出来：以玩家眼睛位置为原点、身体朝向为前向，
 * 然后复用 {@link SwordAuraRenderer#draw} 的几何（圆心/半径/张角全部一致）。
 *
 * <h2>Oculus 光影兼容</h2>
 * 无光影时在 {@code RenderLevelStageEvent.AFTER_ENTITIES} 直接画（此时仍在世界渲染内）。
 * <b>有光影包时不能这样画</b>：光影把世界渲染进自己的 buffer，并在 {@code GameRenderer.renderLevel}
 * 末尾才合成到主渲染目标，此时画进去的会被整套覆盖（与光柱当初的问题完全一致）。
 * 因此有光影时改为走 {@link #renderOculusDeferredPass}：由 {@code CosmicAfterLevelMixin} 在
 * 「光影合成之后、手部渲染之前」调用 {@link LateWorldBeamPass} 直接写主渲染目标。
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SwordAuraThirdPerson {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        // 光影包激活时交给延迟 pass，避免被合成覆盖（也避免画两遍）
        if (ItemShaderModCompat.isOculusShaderPackActive()) {
            return;
        }
        LocalPlayer player = SwingAuraState.eligible();
        if (player == null) {
            return;
        }
        float partial = event.getPartialTick();
        float progress = SwingAuraState.progress(player, partial);
        if (Float.isNaN(progress)) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        Vec3 eye = player.getEyePosition(partial);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(eye.x - cam.x, eye.y - cam.y, eye.z - cam.z);
        // 近似：抬头/低头不参与，只按身体朝向（MC 视角惯例 yaw=0 看向 +Z）
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - player.getViewYRot(partial)));

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        SwordAuraRenderer.draw(pose, buffers, progress, CutterAttackAnimation.isReversed());
        buffers.endBatch(PolygonRenderer.RenderTypes.UNLIT_ADDITIVE);
        pose.popPose();
    }

    /**
     * 本帧是否已经画过。
     * {@code CosmicAfterLevelMixin} 有「手部渲染前」与「renderLevel TAIL 兜底」两个注入点，
     * 同一帧两个都会触发，所以必须像光柱/激光那样只画一次（它们用的是同一个套路的一次性开关）。
     */
    private static boolean deferredDrawnThisFrame = false;

    /** 由 {@code CosmicAfterLevelMixin} 在 renderLevel 开头调用，开启新的一帧。 */
    public static void beginFrame() {
        deferredDrawnThisFrame = false;
    }

    /** 光影包下的延迟补画：由 {@code CosmicAfterLevelMixin} 在合成之后、手部渲染之前调用 */
    public static void renderOculusDeferredPass(float partialTick) {
        if (deferredDrawnThisFrame) {
            return;                                  // 同一帧已画过 → 不再画第二遍
        }
        LocalPlayer player = SwingAuraState.eligible();
        if (player == null) {
            return;
        }
        float progress = SwingAuraState.progress(player, partialTick);
        if (Float.isNaN(progress)) {
            return;
        }
        deferredDrawnThisFrame = true;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 eye = player.getEyePosition(partialTick);

        // begin() 返回的是「世界坐标 → 视图」的位姿：先按世界坐标移到眼睛，再按朝向旋转
        PoseStack view = LateWorldBeamPass.begin(camera);
        view.translate((float) eye.x, (float) eye.y, (float) eye.z);
        view.mulPose(Axis.YP.rotationDegrees(180.0F - player.getViewYRot(partialTick)));

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        SwordAuraRenderer.draw(view, buffers, progress, CutterAttackAnimation.isReversed());
        buffers.endBatch(PolygonRenderer.RenderTypes.UNLIT_ADDITIVE);   // 必须在 end() 之前
        LateWorldBeamPass.end();
    }

    /** 第三人称剑气的前置条件与进度（两条路径共用） */
    private static final class SwingAuraState {
        /** @return 可绘制时返回本地玩家，否则 null */
        static LocalPlayer eligible() {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) {
                return null;
            }
            if (mc.options.getCameraType().isFirstPerson()) {
                return null;                 // 第一人称由手持那条路径负责
            }
            if (!player.swinging) {
                return null;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof XSwordItem) && !(stack.getItem() instanceof DiexvSwordItem)) {
                return null;
            }
            return player;
        }

        /** @return 0..1 的动画进度；不适用时返回 NaN */
        static float progress(LocalPlayer player, float partialTick) {
            float swing = player.getAttackAnim(partialTick);
            if (swing <= 0.0F) {
                return Float.NaN;
            }
            boolean reversed = CutterAttackAnimation.isReversed();
            float t = Math.min(1.0F, swing * CutterAttackAnimation.SPEED);
            return (float) Math.pow((double) (reversed ? 1.0F - t : t), 0.5D);
        }

        private SwingAuraState() {}
    }

    private SwordAuraThirdPerson() {}
}
