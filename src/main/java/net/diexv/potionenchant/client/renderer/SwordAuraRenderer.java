package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 月牙剑气：沿挥砍圆弧、位于刀身挥动方向<b>前方</b>的一段月牙形光刃。
 *
 * <h2>几何</h2>
 * 斩切者那六条链在"眼睛坐标系"里把物品沿一个圆推动：
 * <pre>
 *   圆心 C = ① 的摆位 (0.5, -0.4 + RISE, -0.5)     半径 ρ = |T2| = 0.7071
 *   平面 ⊥ â，â = Q·x̂（Q = ②③④）                   当前方向 d = Q·Rx(θ)·T2
 * </pre>
 * 月牙画在这个圆上，再整体外推 {@link #RADIUS_OUT} 倍、沿挥动方向前移 {@link #AHEAD} 度、
 * 张角 ±{@link #HALF_SPAN} 度，内外边随 λ 收拢成月牙（两端为尖）。
 *
 * <h2>两个使用场景</h2>
 * <ul>
 *   <li>第一人称：在 {@code renderItem} 里调用 {@link #renderFirstPerson}。
 *       此时 pose 已含 Forge 的 display 变换，故先乘它的逆，把坐标拉回眼睛坐标系。</li>
 *   <li>第三人称：由 {@link SwordAuraThirdPerson} 在 {@code RenderLevelStageEvent} 里
 *       用玩家眼睛位置 + 身体朝向近似搭一个坐标系，再调用 {@link #draw}。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class SwordAuraRenderer {

    /** 沿挥动方向前移的角度（度）——越大越靠刀刃那侧、离手柄越远 */
    private static final float AHEAD = 56.0F;
    /** 月牙张角的一半（度）——越大越长 */
    private static final float HALF_SPAN = 95.0F;
    /** 月牙最大半宽（格）——越大越粗 */
    private static final float BAND_WIDTH = 0.30F;
    /**
     * 加粗偏向内侧的比例：0 = 内外对称；0.45 = 内侧 1.45 倍、外侧 0.55 倍。
     * "内侧" = 半径更小的那一侧 = 靠身体/手柄那侧。
     */
    private static final float INNER_BIAS = 0.45F;
    /**
     * 圆弧半径外推倍数：越大越靠外（实测确认：调小会变靠内）。
     * 1.0 ≈ 贴着刀身自己的挥砍圆。
     */
    private static final float RADIUS_OUT = 4.00F;
    /** 圆心横向外移（格）：整体把月牙推向画面边缘，不影响深度 */
    private static final float CENTER_X_OUT = 0.35F;
    /** 分段数 */
    private static final int SEGMENTS = 36;
    /** 颜色与基础透明度 */
    private static final float RED = 0.78F;
    private static final float GREEN = 0.93F;
    private static final float BLUE = 1.00F;
    private static final float BASE_ALPHA = 0.85F;

    private static final float DEG = (float) Math.PI / 180.0F;
    private static final float RADIUS = 0.70710678F * RADIUS_OUT;

    /** display 变换参数（item/handheld firstperson_*_hand） */
    private static final float DISP_S = 0.68F;
    private static final float DISP_TX = 1.13F / 16.0F;
    private static final float DISP_TY = 3.2F / 16.0F;
    private static final float DISP_TZ = 1.13F / 16.0F;

    private static final Vector3f AXIS = new Vector3f();
    private static final Vector3f DIR = new Vector3f();
    private static final Vector3f TMP = new Vector3f();

    /** 第一人称：把 pose 拉回眼睛坐标系后绘制 */
    public static void renderFirstPerson(PoseStack poseStack, MultiBufferSource buffers) {
        boolean reversed = CutterAttackAnimation.isReversed();
        float t = Math.min(1.0F, CutterAttackAnimation.swingProcess() * CutterAttackAnimation.SPEED);
        float progress = (float) Math.pow((double) (reversed ? 1.0F - t : t), 0.5D);

        Matrix4f chain = chainMatrix(progress);
        Matrix4f disp = displayMatrix(CutterAttackAnimation.isLeftHand()).translate(-0.5F, -0.5F, -0.5F);

        poseStack.pushPose();
        poseStack.mulPoseMatrix(new Matrix4f(chain).mul(disp).invert());
        draw(poseStack, buffers, progress, reversed);
        poseStack.popPose();
    }

    /**
     * 在当前 pose（应为"眼睛坐标系"）里绘制月牙。
     *
     * @param progress 0..1（已含正放/倒放映射）
     * @param reversed 是否倒放版（挥动方向相反）
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float progress, boolean reversed) {
        float t = reversed ? 1.0F - progress * progress : progress * progress;
        float alphaBase = BASE_ALPHA * (float) Math.pow(Math.sin(Math.PI * Math.min(1.0F, Math.max(0.0F, t))), 0.6D);
        if (alphaBase <= 0.004F) {
            return;
        }

        float theta = (-180.0F * progress + 80.0F) * DEG;
        Quaternionf q = new Quaternionf().rotationY(-90.0F * DEG).rotateZ(90.0F * DEG).rotateY(-5.0F * DEG);
        AXIS.set(1.0F, 0.0F, 0.0F).rotate(q);
        DIR.set(0.0F, 0.5F, 0.5F).rotate(q).rotateAxis(theta, AXIS.x, AXIS.y, AXIS.z);
        Vector3f center = new Vector3f(0.5F + CENTER_X_OUT, -0.4F + CutterAttackAnimation.RISE, -0.5F);
        float sense = reversed ? 1.0F : -1.0F;

        VertexConsumer cons = buffers.getBuffer(PolygonRenderer.RenderTypes.UNLIT_ADDITIVE);
        Matrix4f mat = poseStack.last().pose();

        float prevInnerX = 0, prevInnerY = 0, prevInnerZ = 0;
        float prevOuterX = 0, prevOuterY = 0, prevOuterZ = 0;
        for (int i = 0; i <= SEGMENTS; i++) {
            float lambda = -1.0F + 2.0F * (float) i / (float) SEGMENTS;
            float ang = sense * (AHEAD + lambda * HALF_SPAN) * DEG;
            TMP.set(DIR).rotateAxis(ang, AXIS.x, AXIS.y, AXIS.z);
            float len = TMP.length();
            if (len < 1.0E-5F) {
                continue;
            }
            TMP.mul(RADIUS / len);
            float taper = (float) Math.pow(Math.cos(lambda * Math.PI * 0.5D), 0.7D);
            float half = BAND_WIDTH * taper;
            // 加粗偏向内侧：内半边 1+BIAS、外半边 1-BIAS（内侧 = 半径更小 = 靠身体那侧）
            float innerHalf = half * (1.0F + INNER_BIAS);
            float outerHalf = half * (1.0F - INNER_BIAS);
            float fade = (float) Math.pow(1.0D - Math.abs(lambda), 0.45D);
            int a = (int) (alphaBase * fade * 255.0F);
            if (a < 3) {
                a = 3;
            }
            float dirX = TMP.x, dirY = TMP.y, dirZ = TMP.z;
            float innerX = center.x + dirX - dirX * innerHalf / RADIUS;
            float innerY = center.y + dirY - dirY * innerHalf / RADIUS;
            float innerZ = center.z + dirZ - dirZ * innerHalf / RADIUS;
            float outerX = center.x + dirX + dirX * outerHalf / RADIUS;
            float outerY = center.y + dirY + dirY * outerHalf / RADIUS;
            float outerZ = center.z + dirZ + dirZ * outerHalf / RADIUS;

            if (i > 0) {
                cons.vertex(mat, prevInnerX, prevInnerY, prevInnerZ).color(RED, GREEN, BLUE, (float) a / 255.0F).endVertex();
                cons.vertex(mat, prevOuterX, prevOuterY, prevOuterZ).color(RED, GREEN, BLUE, (float) a / 255.0F).endVertex();
                cons.vertex(mat, outerX, outerY, outerZ).color(RED, GREEN, BLUE, (float) a / 255.0F).endVertex();
                cons.vertex(mat, innerX, innerY, innerZ).color(RED, GREEN, BLUE, (float) a / 255.0F).endVertex();
            }
            prevInnerX = innerX;
            prevInnerY = innerY;
            prevInnerZ = innerZ;
            prevOuterX = outerX;
            prevOuterY = outerY;
            prevOuterZ = outerZ;
        }
    }

    /** 六条链的矩阵（与 CutterAttackAnimation.apply 逐条对应） */
    public static Matrix4f chainMatrix(float progress) {
        Matrix4f m = new Matrix4f();
        m.translate(0.0F, CutterAttackAnimation.RISE, 0.0F);
        m.translate(0.5F, -0.4F, -0.5F);
        m.rotateY(-90.0F * DEG);
        m.rotateZ(90.0F * DEG);
        m.rotateY(-5.0F * DEG);
        m.rotateX((-180.0F * progress + 80.0F) * DEG);
        m.translate(0.0F, 0.5F, 0.5F);
        return m;
    }

    /** Forge 的 display 变换矩阵（CosmicBakeModel.applyTransform 的内容，不含最后那条 -0.5） */
    public static Matrix4f displayMatrix(boolean left) {
        Matrix4f m = new Matrix4f();
        m.translate(DISP_TX, DISP_TY, DISP_TZ);
        m.rotate(new Quaternionf().rotationXYZ(0.0F, (left ? 90.0F : -90.0F) * DEG, (left ? -25.0F : 25.0F) * DEG));
        m.scale(DISP_S, DISP_S, DISP_S);
        if (left) {
            m.rotateY(180.0F * DEG);
        }
        return m;
    }

    private SwordAuraRenderer() {}
}
