package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public final class CutterAttackAnimation {

    public static final float SPEED = 1.5F;

    public static final float RISE = 0.15F;

    // 交替状态

    private static boolean reversed = true;

    // 挥砍状态（给月牙剑气用）

    private static volatile boolean swinging = false;
    private static volatile float lastSwingProcess = 0.0F;
    private static volatile boolean leftHand = false;

    public static float currentProgress() {
        float t = Math.min(1.0F, lastSwingProcess * SPEED);
        return (float) Math.pow((double) (reversed ? 1.0F - t : t), 0.5D);
    }

    public static boolean isSwinging() {
        return swinging;
    }

    public static float swingProcess() {
        return lastSwingProcess;
    }

    public static boolean isReversed() {
        return reversed;
    }

    public static boolean isLeftHand() {
        return leftHand;
    }

    private static float animTime(float swingProcess) {
        float t = swingProcess * SPEED;
        return t > 1.0F ? 1.0F : t;
    }

    public static void markNewSwing() {
        reversed = !reversed;
    }

    public static boolean apply(PoseStack poseStack, float swingProcess, boolean isLeftHand) {
        leftHand = isLeftHand;

        if (swingProcess <= 0.0F) {
            swinging = false;
            return false;
        }
        swinging = true;
        lastSwingProcess = swingProcess;

        // 前进：sqrt(t)；倒放：sqrt(1 - t)；两者都按 SPEED 倍速推进
        float progress = (float) Math.pow((double) (reversed ? 1.0F - animTime(swingProcess) : animTime(swingProcess)), 0.5D);

        // 高度
        if (RISE != 0.0F) {
            poseStack.translate(0.0D, (double) RISE, 0.0D);
        }

        poseStack.translate(0.5D, -0.4D, -0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-5.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F * progress + 80.0F));
        poseStack.translate(0.0D, 0.5D, 0.5D);
        return true;
    }

    private CutterAttackAnimation() {}
}
