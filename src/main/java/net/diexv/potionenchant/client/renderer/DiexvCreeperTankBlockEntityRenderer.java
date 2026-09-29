package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.diexv.potionenchant.blockentity.DiexvCreeperTankBlockEntity;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * DiexvCreeperTank 方块渲染器：
 *  - 苦力怕模型外壳（entityTranslucent 半透明），照原版实体渲染 scale(-1,-1,1) 翻转，
 *    绕 Y 转到 FACING 朝向；脚底对齐方块底（可整体微抬 Y_LIFT）
 *  - 罐内液体：不透明液柱（先画并写深度，模型外壳后叠加 → 近面可见、有体积感）
 *    + 独立亮色液面；颜色 = 容器混色，灌入后短暂显示本次灌入色（闪色）
 *  - 液面高度以整个模型高度为上限（0.06 → 0.9），随容量 amount/16 上升
 */
public class DiexvCreeperTankBlockEntityRenderer implements BlockEntityRenderer<DiexvCreeperTankBlockEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("potionenchant", "textures/block/diexvcreeper.png");

    /** 模型等比缩放（x/y/z 同一比例）：模型总高 26 像素（头顶 -2px → 腿底 24px）→ 缩成 1 格高 */
    private static final float MODEL_SCALE = 16.0F / 26.0F;
    /** 像素 → 格换算（等比缩放系数 /16） */
    private static final double PX = MODEL_SCALE / 16.0D;
    /** CreeperModel root 在"脖子"：脚底(腿底 24px) 在 root 下 24 像素 → root 放这里脚底贴方块底 */
    private static final double ROOT_Y = 24.0D * PX;
    /** 模型整体上移（每 1 方块像素 ≈ 0.0625；当前共上移 2 像素） */
    private static final double Y_OFFSET = 0.0D; // 模型精确 0~1 格（脚底0 头顶1.0）

    private final CreeperModel<Creeper> model;

    public DiexvCreeperTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER));
    }

    @Override
    public void render(DiexvCreeperTankBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        Direction facing = be.getBlockState().getValue(
                net.diexv.potionenchant.block.DiexvCreeperTankBlock.FACING);

        // ===== 罐内液体（不透明，只在"模型渲染体积内"按朝向分段填充：腿 / 身体 / 头） =====
        // 液体截面按"默认朝北"定义，绘制时施加与外壳相同的绕 Y 旋转 → 各朝向都贴合对应腿部/身/头
        int amount = be.getAmount();
        if (amount > 0) {
            pose.pushPose();
            pose.translate(0.5D, 0.0D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
            pose.translate(-0.5D, 0.0D, -0.5D);
            int liquidColor = be.getFillFlashTicks() > 0 && be.getFillFlashColor() >= 0
                    ? be.getFillFlashColor() : be.getColor();
            float frac = Math.min(1.0F, amount / (float) DiexvCreeperTankBlockEntity.MAX_AMOUNT);
            float y1 = frac * 0.95F; // 液面从脚底(y0)起，满容量 ≈ 0.95
            float wave = (float) (Math.sin(be.getLevel() == null ? 0 : net.minecraft.Util.getMillis() / 600.0D
                    + be.getBlockPos().getX() * 1.7D + be.getBlockPos().getZ() * 1.3D) * 0.012D);
            float yLevel = y1 + wave;
            if (yLevel > 1.0F) yLevel = 1.0F;
            int liqRgb = liquidColor; // 液柱与液面统一颜色
            
            // 截面取自 26px 等比渲染几何内部（腿 y0~0.23 / 身体 0.23~0.69 / 头 0.69~1.0），内缩防漏边
            float[] headSection = {0.38F, 0.62F, 0.38F, 0.62F, 0.71F, 0.94F};
            float[] bodySection = {0.38F, 0.62F, 0.45F, 0.55F, 0.25F, 0.71F};
            float[] legFR = {0.53F, 0.62F, 0.30F, 0.38F, 0.0F, 0.22F};
            float[] legFL = {0.38F, 0.47F, 0.30F, 0.38F, 0.0F, 0.22F};
            float[] legBR = {0.53F, 0.62F, 0.62F, 0.70F, 0.0F, 0.22F};
            float[] legBL = {0.38F, 0.47F, 0.62F, 0.70F, 0.0F, 0.22F};
            // 按 y 升序逐段填充；yLevel 落入的那段顶部用亮色液面
            float[][] order = {legFR, legFL, legBR, legBL, bodySection, headSection};
            for (float[] sec : order) {
                float secY0 = sec[4];
                float secY1 = sec[5];
                if (yLevel <= secY0) {
                    break; // 后面的段底更高，均未到
                }
                float top = Math.min(yLevel, secY1);
                boolean surface = top >= yLevel - 0.0001F; // 液面正落在本段
                PolygonRenderer.cubeBox(pose.last().pose(),
                        buffer.getBuffer(DiexvCreeperLiquidRenderTypes.LIQUID_SOLID),
                        new AABB(sec[0], secY0, sec[2], sec[1], top, sec[3]),
                        0xFF000000 | (liqRgb & 0xFFFFFF), face -> true);
            }
            pose.popPose();
        }
        // ===== 苦力怕模型外壳（半透明，后画叠加在液体上） =====
        pose.pushPose();
        pose.translate(0.5D, ROOT_Y + Y_OFFSET, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        pose.scale(-MODEL_SCALE, -MODEL_SCALE, MODEL_SCALE);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(pose, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    /** 共享罐内液体：模型局部 0~1（默认朝北）分区（腿 4 柱 + 身体 + 头）。
     *  方块与掉落物/手持调用同一实现，保证液体形状/大小完全一致。 */
    public static void renderLiquid(PoseStack pose, MultiBufferSource buffer, float yLevel, int liqRgb) {
        float[] headSection = {0.38F, 0.62F, 0.38F, 0.62F, 0.71F, 0.94F};
        float[] bodySection = {0.38F, 0.62F, 0.45F, 0.55F, 0.25F, 0.71F};
        float[] legFR = {0.53F, 0.62F, 0.30F, 0.38F, 0.0F, 0.22F};
        float[] legFL = {0.38F, 0.47F, 0.30F, 0.38F, 0.0F, 0.22F};
        float[] legBR = {0.53F, 0.62F, 0.62F, 0.70F, 0.0F, 0.22F};
        float[] legBL = {0.38F, 0.47F, 0.62F, 0.70F, 0.0F, 0.22F};
        float[][] order = {legFR, legFL, legBR, legBL, bodySection, headSection};
        com.mojang.blaze3d.vertex.VertexConsumer c = buffer.getBuffer(DiexvCreeperLiquidRenderTypes.LIQUID_SOLID);
        int argb = 0xFF000000 | (liqRgb & 0xFFFFFF);
        for (float[] sec : order) {
            if (yLevel <= sec[4]) break; // 后面的段底更高，均未到
            float top = Math.min(yLevel, sec[5]);
            if (top <= sec[4]) continue;
            PolygonRenderer.cubeBox(pose.last().pose(), c,
                    new AABB(sec[0], sec[4], sec[2], sec[1], top, sec[3]), argb, face -> true);
        }
    }

    private static int scaleColor(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    /** 与白色按比例混合（生成更亮的液面色） */
    private static int mixColor(int rgb, int white, float whiteRatio) {
        int r = (int) (((rgb >> 16) & 0xFF) * (1 - whiteRatio) + 255 * whiteRatio);
        int g = (int) (((rgb >> 8) & 0xFF) * (1 - whiteRatio) + 255 * whiteRatio);
        int b = (int) ((rgb & 0xFF) * (1 - whiteRatio) + 255 * whiteRatio);
        return (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }


    @Override
    public boolean shouldRenderOffScreen(DiexvCreeperTankBlockEntity be) {
        return true;
    }

    @Override
    public boolean shouldRender(DiexvCreeperTankBlockEntity be, Vec3 cameraPos) {
        return true;
    }
}
