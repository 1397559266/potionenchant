package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 苦力怕药水罐物品渲染（手持/掉落/GUI 显示方块本体苦力怕模型 + 罐内液体）。
 *
 * 与方块渲染完全同构：
 *  - 模型等比缩放系数与方块一致（MODEL_SCALE = 16/26，像素→格 = PX）
 *  - 液体直接调用方块的共享 DiexvCreeperTankBlockEntityRenderer.renderLiquid（同一几何分区）
 *  → 掉落物/手持中液体在模型内的相对大小与方块完全一致
 */
@OnlyIn(Dist.CLIENT)
public class DiexvCreeperTankItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("potionenchant", "textures/block/diexvcreeper.png");

    /** 与方块相同的等比缩放（模型总高 26px → 1 格） */
    private static final float SCALE = 16.0F / 26.0F;
    /** 像素→格 */
    private static final double PX = SCALE / 16.0D;
    /** 模型脚底(24px)贴物品网格底部（与方块一致） */
    private static final double ROOT_Y = 24.0D * PX;

    private final CreeperModel<Creeper> model;
    private static DiexvCreeperTankItemRenderer INSTANCE;

    public DiexvCreeperTankItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
        this.model = new CreeperModel<>(models.bakeLayer(ModelLayers.CREEPER));
    }

    public static DiexvCreeperTankItemRenderer instance() {
        if (INSTANCE == null) {
            Minecraft mc = Minecraft.getInstance();
            INSTANCE = new DiexvCreeperTankItemRenderer(
                    mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return INSTANCE;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        boolean ground = context == ItemDisplayContext.GROUND;
        pose.pushPose();
        if (ground) {
            // 掉落物整体上移避免嵌入地面
            pose.translate(0.0D, 0.3D, 0.0D);
        } else {
            // 手持/GUI：模型默认朝北（背面朝玩家），绕中心转 180° 让正面（脸）朝玩家
            pose.translate(0.5D, 0.0D, 0.5D);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            pose.translate(-0.5D, 0.0D, -0.5D);
        }
        // 罐内液体（与方块同款共享实现，几何/比例一致）
        CompoundTag d = stack.getTagElement("diexv_tank_data");
        if (d != null) {
            int amount = d.getInt("Amount");
            if (amount > 0) {
                int color = d.contains("Color") ? d.getInt("Color") : 0xFFFFFF;
                float frac = Mth.clamp(amount / (float) net.diexv.potionenchant.blockentity.DiexvCreeperTankBlockEntity.MAX_AMOUNT, 0.0F, 1.0F);
                float yLevel = Math.min(1.0F, frac * 0.95F);
                DiexvCreeperTankBlockEntityRenderer.renderLiquid(pose, buffer, yLevel, color);
            }
        }
        // 模型外壳（与方块同缩放/同坐标）
        pose.pushPose();
        pose.translate(0.5D, ROOT_Y, 0.5D);
        pose.scale(-SCALE, -SCALE, SCALE);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(pose, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        pose.popPose();
    }
}
