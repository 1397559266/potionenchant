package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.renderer.model.CubeModel;
import net.diexv.potionenchant.entity.BombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class BombEntityRenderer extends EntityRenderer<BombEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PotionEnchantMod.MODID, "textures/entity/bomb.png");
    private static final ResourceLocation ARMOR_TEXTURE =
            new ResourceLocation("textures/entity/creeper/creeper_armor.png");

    private final CubeModel<BombEntity> bombModel;
    private final CubeModel<BombEntity> armorModel;
    private static final float ARMOR_SCALE = 2.0F; // 闪电护甲放大倍数

    public BombEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 基础模型（立方体模型）
        this.bombModel = new CubeModel<>(
                context.bakeLayer(net.diexv.potionenchant.client.ClientEventHandler.BOMB_LAYER)
        );
        // 闪电护甲模型（使用相同的立方体模型）
        this.armorModel = new CubeModel<>(
                context.bakeLayer(net.diexv.potionenchant.client.ClientEventHandler.BOMB_LAYER)
        );
    }

    @Override
    public void render(BombEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        // 如果Bomb处于环绕模式，在客户端重新计算平滑位置
        if (!entity.isAttacking()) {
            renderOrbitingBomb(entity, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        // 攻击模式：使用默认渲染
        poseStack.pushPose();

        // 调整基础模型大小和位置（当前大小的1.5倍：0.5 * 1.5 = 0.75）
        poseStack.scale(-0.75F, -0.75F, 0.75F);

        // 更新基础模型动画
        this.bombModel.prepareMobModel(entity, 0, 0, partialTicks);
        this.bombModel.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);

        // 渲染基础模型（正常大小）
        VertexConsumer vertexconsumer = buffer.getBuffer(this.bombModel.renderType(this.getTextureLocation(entity)));
        this.bombModel.renderToBuffer(poseStack, vertexconsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        // 如果实体有闪电效果，渲染护甲层（放大版本）
        if (entity.isPowered()) {
            float f = entity.getSwelling(partialTicks);
            float offset = (entity.tickCount + partialTicks) * 0.01F;

            // 更新护甲模型动画
            this.armorModel.prepareMobModel(entity, 0, 0, partialTicks);
            this.armorModel.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);

            poseStack.pushPose();

            float armorScale = 1.5F;
            poseStack.scale(armorScale, armorScale, armorScale);

            VertexConsumer armorConsumer = buffer.getBuffer(
                    RenderType.energySwirl(this.getArmorTextureLocation(), offset % 1.0F, f % 1.0F)
            );

            this.armorModel.renderToBuffer(poseStack, armorConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            poseStack.popPose();
        }

        poseStack.popPose();
    }
    
    /**
     * 渲染环绕模式的Bomb（客户端直接计算位置，最大化流畅度）
     */
    private void renderOrbitingBomb(BombEntity entity, float partialTicks,
                                    PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 获取所有者（玩家）
        net.minecraft.world.entity.LivingEntity owner = entity.getOwner();
        if (owner == null) {
            // 如果没有所有者，使用默认渲染
            renderDefault(entity, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        
        // 在客户端直接计算环绕位置（不依赖服务端同步）
        double ownerX = owner.xo + (owner.getX() - owner.xo) * partialTicks;
        double ownerY = owner.yo + (owner.getY() - owner.yo) * partialTicks;
        double ownerZ = owner.zo + (owner.getZ() - owner.zo) * partialTicks;
        
        // 计算环绕中心（玩家上方3.5格）
        double centerX = ownerX;
        double centerY = ownerY + 3.5;
        double centerZ = ownerZ;
        
        // 计算角度（使用partialTicks实现帧级平滑）
        int orbitIndex = entity.getOrbitIndex();
        // 使用客户端世界的全局时间，避免重新进入世界后tickCount重置导致的卡死
        float ageInTicks = (float)(entity.level().getGameTime() % 10000) + partialTicks;
        double angle = (orbitIndex / 5.0) * Math.PI * 2 + ageInTicks * 0.05;
        double radius = 3.0;
        
        // 计算目标位置
        double targetX = centerX + Math.cos(angle) * radius;
        double targetY = centerY + Math.sin(ageInTicks * 0.1) * 0.5; // 上下浮动
        double targetZ = centerZ + Math.sin(angle) * radius;
        
        // 转换到相机坐标
        var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        double camX = targetX - camera.getPosition().x;
        double camY = targetY - camera.getPosition().y;
        double camZ = targetZ - camera.getPosition().z;
        
        poseStack.pushPose();
        poseStack.translate(camX, camY, camZ);
        
        // 调整基础模型大小和位置
        poseStack.scale(-0.75F, -0.75F, 0.75F);
        
        // 更新基础模型动画
        this.bombModel.prepareMobModel(entity, 0, 0, partialTicks);
        this.bombModel.setupAnim(entity, 0, 0, ageInTicks, 0, 0);
        
        // 渲染基础模型
        VertexConsumer vertexconsumer = buffer.getBuffer(this.bombModel.renderType(this.getTextureLocation(entity)));
        this.bombModel.renderToBuffer(poseStack, vertexconsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        // 渲染闪电护甲层
        if (entity.isPowered()) {
            float f = entity.getSwelling(partialTicks);
            float offset = ageInTicks * 0.01F;
            
            this.armorModel.prepareMobModel(entity, 0, 0, partialTicks);
            this.armorModel.setupAnim(entity, 0, 0, ageInTicks, 0, 0);
            
            poseStack.pushPose();
            poseStack.scale(1.5F, 1.5F, 1.5F);
            
            VertexConsumer armorConsumer = buffer.getBuffer(
                    RenderType.energySwirl(this.getArmorTextureLocation(), offset % 1.0F, f % 1.0F)
            );
            
            this.armorModel.renderToBuffer(poseStack, armorConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
    
    /**
     * 默认渲染（用于攻击模式或无所有者情况）
     */
    private void renderDefault(BombEntity entity, float partialTicks,
                               PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(-0.75F, -0.75F, 0.75F);
        
        this.bombModel.prepareMobModel(entity, 0, 0, partialTicks);
        this.bombModel.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);
        
        VertexConsumer vertexconsumer = buffer.getBuffer(this.bombModel.renderType(this.getTextureLocation(entity)));
        this.bombModel.renderToBuffer(poseStack, vertexconsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        if (entity.isPowered()) {
            float f = entity.getSwelling(partialTicks);
            float offset = (entity.tickCount + partialTicks) * 0.01F;
            
            this.armorModel.prepareMobModel(entity, 0, 0, partialTicks);
            this.armorModel.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);
            
            poseStack.pushPose();
            poseStack.scale(1.5F, 1.5F, 1.5F);
            
            VertexConsumer armorConsumer = buffer.getBuffer(
                    RenderType.energySwirl(this.getArmorTextureLocation(), offset % 1.0F, f % 1.0F)
            );
            
            this.armorModel.renderToBuffer(poseStack, armorConsumer, packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BombEntity entity) {
        return TEXTURE;
    }

    protected ResourceLocation getArmorTextureLocation() {
        return ARMOR_TEXTURE;
    }
}