package net.diexv.potionenchant.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * X套装闪电护甲渲染层
 * 参考Minecraft原版CreeperPowerLayer的实现
 * 当玩家穿着全套X护甲时显示闪电效果
 */
public class XArmorPowerLayer extends RenderLayer<AbstractClientPlayer, HumanoidModel<AbstractClientPlayer>> {

    @SuppressWarnings("removal")
    private static final ResourceLocation POWER_LOCATION =
            new ResourceLocation("textures/entity/creeper/creeper_armor.png");
    
    private final HumanoidModel<AbstractClientPlayer> model;

    public XArmorPowerLayer(RenderLayerParent<AbstractClientPlayer, HumanoidModel<AbstractClientPlayer>> renderer,
                           HumanoidModel<AbstractClientPlayer> model) {
        super(renderer);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                      AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        
        // 检查是否穿着全套X护甲
        if (!isWearingFullXArmor(player)) {
            return;
        }
        
        // 计算闪电效果的动画偏移（与苦力怕保持一致）
        float time = (player.tickCount + partialTicks) * 0.01F;
        
        // 设置模型属性
        this.getParentModel().copyPropertiesTo(this.model);
        this.model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        this.model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        
        // 获取顶点消费者，使用energySwirl渲染类型
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.energySwirl(POWER_LOCATION, time, time));
        
        // 渲染闪电效果
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, 
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
    
    /**
     * 检查玩家是否穿着全套X护甲
     */
    private boolean isWearingFullXArmor(AbstractClientPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        
        return !helmet.isEmpty() && helmet.getItem() == ModItems.X_HELMET.get() &&
               !chestplate.isEmpty() && chestplate.getItem() == ModItems.X_CHESTPLATE.get() &&
               !leggings.isEmpty() && leggings.getItem() == ModItems.X_LEGGINGS.get() &&
               !boots.isEmpty() && boots.getItem() == ModItems.X_BOOTS.get();
    }
}
