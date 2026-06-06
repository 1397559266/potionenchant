package net.diexv.potionenchant.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.diexv.potionenchant.client.renderer.hyperlink.PolygonRenderer;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * X套装 — Fumetsu（不滅）风格光环渲染层
 * 从 Hyperlink 的 FumetsuEntityRenderer.renderAura + FumetsuStormRenderer 移植
 *
 * 当玩家穿着全套X护甲时渲染：
 * - 基础能量漩涡（保留原版）
 * - 旋转的空心三角形光环（红+青）
 * - 半透明发光立方体包裹
 */
public class XArmorPowerLayer extends RenderLayer<AbstractClientPlayer, HumanoidModel<AbstractClientPlayer>> {

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

        if (!isWearingFullXArmor(player)) return;

        // ===== 1. 基础能量漩涡（保留原版） =====
        float swirlTime = (player.tickCount + partialTicks) * 0.01F;
        this.getParentModel().copyPropertiesTo(this.model);
        this.model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        this.model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        var swirlConsumer = buffer.getBuffer(RenderType.energySwirl(POWER_LOCATION, swirlTime, swirlTime));
        this.model.renderToBuffer(poseStack, swirlConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        // [多边形特效已禁用]
        float entityHeight = player.getBbHeight();
        float tick = player.tickCount + partialTicks;

        // ===== 3. 半透明发光立方体包裹 =====
        // 像 FumetsuStorm 一样在玩家周围渲染一个半透明立方体
        float pulse = 0.8f + 0.2f * Mth.sin(tick * 0.05f);
        AABB playerBox = new AABB(-0.6, 0, -0.6, 0.6, entityHeight, 0.6);

        PolygonRenderer.with(poseStack, () -> {
            poseStack.translate(0, -0.5, 0);
            
            // 半透明填充立方体（红色半透明）
            PolygonRenderer.cubeBox(
                    poseStack.last().pose(),
                    buffer.getBuffer(PolygonRenderer.RenderTypes.HIGHLIGHT),
                    playerBox,
                    (int)(0x30 * pulse) << 24 | 0xFF4444,
                    dir -> true);

            // 厚边框（青色）
            PolygonRenderer.thickLineBoxBox(
                    poseStack.last().pose(),
                    buffer.getBuffer(PolygonRenderer.RenderTypes.HIGHLIGHT),
                    playerBox.inflate(0.1f),
                    0.04f,
                    (int)(0x80 * pulse) << 24 | 0x44FFFF);
        });

        PolygonRenderer.endBatch(PolygonRenderer.RenderTypes.LIGHTNING_NO_CULL);
        PolygonRenderer.endBatch(PolygonRenderer.RenderTypes.HIGHLIGHT);
    }

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
