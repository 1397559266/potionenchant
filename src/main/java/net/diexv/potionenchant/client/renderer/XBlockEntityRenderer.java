package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.ClientEventHandler;
import net.diexv.potionenchant.client.renderer.layers.XBlockPowerLayer;
import net.diexv.potionenchant.client.renderer.model.CubeModel;
import net.diexv.potionenchant.entity.XBlockEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class XBlockEntityRenderer extends MobRenderer<XBlockEntity, CubeModel<XBlockEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(PotionEnchantMod.MODID, "textures/entity/bomb.png");

    // 【修复】使用系统时间而非tick计数，确保绝对平滑
    private long lastRenderTime = 0;
    private float animationTime = 0.0f;

    public XBlockEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new CubeModel<>(context.bakeLayer(ClientEventHandler.XBLOCK_LAYER)), 0.5F);
        
        // 添加闪电护甲层
        this.addLayer(new XBlockPowerLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(XBlockEntity entity) {
        return TEXTURE;
    }
    
    @Override
    public void scale(XBlockEntity entity, PoseStack poseStack, float partialTickTime) {
        // 【关键修复】使用系统时间计算动画，避免tick累积导致的跳变
        long currentTime = System.nanoTime();
        if (lastRenderTime == 0) {
            lastRenderTime = currentTime;
        }
        
        // 计算时间差（秒）
        float deltaTime = (currentTime - lastRenderTime) / 1_000_000_000.0f;
        lastRenderTime = currentTime;
        
        // 累加动画时间（不会溢出，因为float可以表示很大的值）
        animationTime += deltaTime;
        
        // 基础缩放
        float baseScale = 10.0F;

        // 计算动态缩放因子（推荐使用弹性效果）
        float dynamicScale = getElasticScale();

        // 最终缩放 = 基础缩放 × 动态缩放因子
        float finalScale = baseScale * dynamicScale;
        poseStack.scale(finalScale, finalScale, finalScale);
    }
    
    /**
     * 【新增】弹性缩放效果（推荐）
     * 效果：像弹簧一样有弹性的脉动，绝对平滑无跳变
     */
    private float getElasticScale() {
        // 使用多个正弦波叠加，产生自然的弹性效果
        float time = animationTime;
        
        // 主脉动：慢速大幅变化（周期约4秒）
        float mainPulse = (float) Math.sin(time * 1.5f) * 0.15f;
        
        // 次要脉动：中速小幅变化（周期约1.3秒）
        float subPulse = (float) Math.sin(time * 4.8f) * 0.05f;
        
        // 微小抖动：快速极小变化（周期约0.5秒）
        float microPulse = (float) Math.sin(time * 12.0f) * 0.02f;
        
        // 基础大小 + 所有脉动
        return 1.0f + mainPulse + subPulse + microPulse;
    }
    
    @Override
    public boolean shouldRender(XBlockEntity livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        // 强制渲染，无视视锥体剔除
        // 这对于大缩放的实体很重要
        return true;
    }
}
