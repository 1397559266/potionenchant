package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.entity.RainbowLightningBolt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 彩虹闪电渲染器
 * 基于DiexvCreeper的RainbowLightningRender
 */
@OnlyIn(Dist.CLIENT)
public class RainbowLightningRenderer extends EntityRenderer<RainbowLightningBolt> {
    
    private final LightningBoltRenderer vanillaRenderer;
    
    public RainbowLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.vanillaRenderer = new LightningBoltRenderer(context);
    }
    
    @Override
    public void render(RainbowLightningBolt lightning, float entityYaw, float partialTicks, 
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // ID大于200000000的使用彩虹渲染
        if (lightning.getId() > 200000000 - 1) {
            renderRainbowLightning(lightning, partialTicks, poseStack, buffer, packedLight);
        } else {
            vanillaRenderer.render(lightning, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }
    
    private void renderRainbowLightning(RainbowLightningBolt lightning, float partialTicks, 
                                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float[] afloat = new float[8];
        float[] afloat1 = new float[8];
        float f = 0.0F;
        float f1 = 0.0F;
        RandomSource randomsource = RandomSource.create(lightning.seed);
        
        for(int i = 7; i >= 0; --i) {
            afloat[i] = f;
            afloat1[i] = f1;
            f += (float)(randomsource.nextInt(11) - 5);
            f1 += (float)(randomsource.nextInt(11) - 5);
        }
        
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix4f = poseStack.last().pose();
        
        RandomSource random = RandomSource.create(lightning.seed);
        
        // 从10种蓝色调中随机选择一种颜色
        float[] chosenColor = chooseBlueColor(random);
        float red = chosenColor[0];
        float green = chosenColor[1];
        float blue = chosenColor[2];
        
        for(int j = 0; j < 4; ++j) {
            RandomSource randomsource1 = RandomSource.create(lightning.seed);
            
            for(int k = 0; k < 3; ++k) {
                int l = 7;
                int i1 = 0;
                if (k > 0) {
                    l = 7 - k;
                }
                
                if (k > 0) {
                    i1 = l - 2;
                }
                
                float f2 = afloat[l] - f;
                float f3 = afloat1[l] - f1;
                
                for(int j1 = l; j1 >= i1; --j1) {
                    float f4 = f2;
                    float f5 = f3;
                    if (k == 0) {
                        f2 += (float)(randomsource1.nextInt(11) - 5);
                        f3 += (float)(randomsource1.nextInt(11) - 5);
                    } else {
                        f2 += (float)(randomsource1.nextInt(31) - 15);
                        f3 += (float)(randomsource1.nextInt(31) - 15);
                    }
                    
                    float f10 = 0.1F + (float)j * 0.2F;
                    if (k == 0) {
                        f10 *= (float)j1 * 0.1F + 1.0F;
                    }
                    
                    float f11 = 0.1F + (float)j * 0.2F;
                    if (k == 0) {
                        f11 *= ((float)j1 - 1.0F) * 0.1F + 1.0F;
                    }
                    
                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, red, green, blue, f10, f11, false, false, true, false);
                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, red, green, blue, f10, f11, true, false, true, true);
                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, red, green, blue, f10, f11, true, true, false, true);
                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, red, green, blue, f10, f11, false, true, false, false);
                }
            }
        }
    }
    
    private float[] chooseBlueColor(RandomSource random) {
        float[][] blueColors = {
            {0.0f, 0.0f, 0.8f},
            {0.0f, 0.0f, 1.0f},
            {0.2f, 0.2f, 1.0f},
            {0.4f, 0.4f, 1.0f},
            {0.6f, 0.6f, 1.0f},
            {0.0f, 0.3f, 1.0f},
            {0.0f, 0.5f, 1.0f},
            {0.2f, 0.7f, 1.0f},
            {0.4f, 0.8f, 1.0f},
            {0.6f, 0.9f, 1.0f}
        };
        
        int choice = random.nextInt(blueColors.length);
        return blueColors[choice];
    }
    
    private static void quad(Matrix4f p_253966_, VertexConsumer p_115274_, float p_115275_, float p_115276_, 
                            int p_115277_, float p_115278_, float p_115279_, float p_115280_, 
                            float p_115281_, float p_115282_, float p_115283_, float p_115284_, 
                            boolean p_115285_, boolean p_115286_, boolean p_115287_, boolean p_115288_) {
        p_115274_.vertex(p_253966_, p_115275_ + (p_115285_ ? p_115284_ : -p_115284_), 
                        (float)(p_115277_ * 16), 
                        p_115276_ + (p_115286_ ? p_115284_ : -p_115284_))
                .color(p_115280_, p_115281_, p_115282_, 0.3F).endVertex();
        p_115274_.vertex(p_253966_, p_115278_ + (p_115285_ ? p_115283_ : -p_115283_), 
                        (float)((p_115277_ + 1) * 16), 
                        p_115279_ + (p_115286_ ? p_115283_ : -p_115283_))
                .color(p_115280_, p_115281_, p_115282_, 0.3F).endVertex();
        p_115274_.vertex(p_253966_, p_115278_ + (p_115287_ ? p_115283_ : -p_115283_), 
                        (float)((p_115277_ + 1) * 16), 
                        p_115279_ + (p_115288_ ? p_115283_ : -p_115283_))
                .color(p_115280_, p_115281_, p_115282_, 0.3F).endVertex();
        p_115274_.vertex(p_253966_, p_115275_ + (p_115287_ ? p_115284_ : -p_115284_), 
                        (float)(p_115277_ * 16), 
                        p_115276_ + (p_115288_ ? p_115284_ : -p_115284_))
                .color(p_115280_, p_115281_, p_115282_, 0.3F).endVertex();
    }
    
    @Override
    public ResourceLocation getTextureLocation(RainbowLightningBolt p_115264_) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
