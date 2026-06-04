package net.diexv.potionenchant.client.renderer.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class CubeModel<T extends Entity> extends EntityModel<T> {
    private final ModelPart cube;

    public CubeModel(ModelPart root) {
        this.cube = root.getChild("cube");
    }

    // 创建完美正方体模型层定义
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 创建16x16x16的正方体，但通过渲染缩放保持为1:1:1
        CubeDeformation deformation = CubeDeformation.NONE;

        // 主立方体部分 - 创建完美正方体（中心对齐）
        // 立方体：16x16x16，中心在原点
        partdefinition.addOrReplaceChild("cube",
                CubeListBuilder.create()
                        .texOffs(0, 0)  // 纹理偏移
                        .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, deformation),
                PartPose.offset(0.0F, 0.0F, 0.0F)); // 完全居中

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 添加简单的浮动动画（基于中心点）
        float floatSpeed = 0.05F;
        float floatHeight = 0.1F;

        this.cube.y = Mth.sin(ageInTicks * floatSpeed) * floatHeight;

        // 添加缓慢的旋转动画
        //this.cube.yRot = ageInTicks * 0.02F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        cube.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    // 获取主模型部分（用于渲染层）
    public ModelPart getCubePart() {
        return cube;
    }
}
