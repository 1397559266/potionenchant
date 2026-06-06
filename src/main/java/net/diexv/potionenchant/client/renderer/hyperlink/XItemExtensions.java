package net.diexv.potionenchant.client.renderer.hyperlink;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * X 系列物品的客户端扩展
 * 提供自定义物品渲染器和第一人称手部姿态
 */
@OnlyIn(Dist.CLIENT)
public class XItemExtensions implements IClientItemExtensions {

    public static final XItemExtensions INSTANCE = new XItemExtensions();

    private static final XSeriesItemRenderer RENDERER = new XSeriesItemRenderer();

    @Override
    public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return RENDERER;
    }

    @Override
    public @Nullable ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
        if (entityLiving.isUsingItem() && itemStack.getUseAnimation() == UseAnim.BLOCK) {
            return ArmPose.BLOCK;
        }
        return null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player,
                                           HumanoidArm arm, ItemStack itemInHand,
                                           float partialTick, float equipProcess, float swingProcess) {
        if (player.isUsingItem() && itemInHand.getUseAnimation() == UseAnim.BLOCK) {
            int horizontal = (arm == HumanoidArm.RIGHT) ? 1 : -1;
            poseStack.translate((float) horizontal * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
            poseStack.translate(horizontal * -0.14142136F, 0.08F, 0.14142136F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
            poseStack.mulPose(Axis.YP.rotationDegrees(horizontal * 13.365F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(horizontal * 78.05F));
            float f1 = (float) Math.sin(Math.sqrt(swingProcess) * Math.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) Math.sin(swingProcess * swingProcess * Math.PI) * -20.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(f1 * -20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
            return true;
        }
        return false;
    }
}
