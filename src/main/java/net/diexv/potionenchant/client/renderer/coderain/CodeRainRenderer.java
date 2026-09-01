package net.diexv.potionenchant.client.renderer.coderain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * DiexvSword 的 GL 粒子特效：代码雨字符（与 mask 层着色器同一套 5x7 点阵位图）。
 *
 * 布局逻辑沿用原矩形版本（36 条流沿角度分布、滚动、拖尾、闪烁），
 * 渲染改为逐像素小方块拼出字符（POSITION_COLOR + NO_TEXTURE 管线），
 * 单个字符大小与原来的矩形相同（0.025 x 0.04）。
 *
 * 不使用 font.drawInBatch：物品渲染（非 GUI 上下文）下字体纹理/shader 绑定不可靠，
 * 点阵字符走 PolygonRenderer.HIGHLIGHT 管线保证可见。
 */
@OnlyIn(Dist.CLIENT)
public class CodeRainRenderer {
    private static final int STREAM_COUNT = 36;
    private static final int MAX_TRAIL_LENGTH = 15;
    private static final float CHAR_WIDTH = 0.025F;
    private static final float CHAR_HEIGHT = 0.04F;
    private static final float RADIUS = 0.65F;
    private static final float HEIGHT_RANGE = 0.8F;

    /** 字符集：0-9 + A-Z（36 个），索引与 FONT_DATA 前 36 个字符对应（0-9=索引0-9，A-Z=索引10-35） */
    private static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /** 5x7 点阵位图：每字符 7 行，每行 5 bit（bit4=最左） */
    private static final int FONT_ROWS = 7;
    private static final int FONT_COLS = 5;
    private static final int[] FONT_DATA = new int[]{
            14,17,19,21,25,17,14,   4,12,4,4,4,4,14,   14,17,1,2,4,8,31,    14,17,1,6,1,17,14,
            2,6,10,18,31,2,2,      31,16,30,1,1,17,14, 14,16,16,30,17,17,14, 31,1,2,4,8,8,8,
            14,17,17,14,17,17,14,  14,17,17,15,1,2,12, 4,10,17,17,31,17,17,  30,17,17,30,17,17,30,
            14,17,16,16,16,17,14,  30,17,17,17,17,17,30,31,16,16,30,16,16,31,31,16,16,30,16,16,16,
            14,17,16,23,17,17,14,  17,17,17,31,17,17,17,14,4,4,4,4,4,14,     7,2,2,2,2,18,12,
            17,18,20,24,20,18,17,  16,16,16,16,16,16,31,17,27,21,21,17,17,17,17,17,25,21,19,17,17,
            14,17,17,17,17,17,14,  30,17,17,30,16,16,16,14,17,17,17,21,18,13,30,17,17,30,20,18,17,
            14,17,16,14,1,17,14,   31,4,4,4,4,4,4,      17,17,17,17,17,17,14, 17,17,17,17,17,10,4,
            17,17,17,21,21,27,17,  17,17,10,4,10,17,17, 17,17,10,4,4,4,4,    31,1,2,4,8,16,31,
            0,0,14,1,15,17,15,     16,16,22,25,17,17,30,0,0,14,16,16,17,14,  1,1,13,19,17,17,15,
            0,0,14,17,31,16,14,    6,9,8,28,8,8,8,      0,0,15,17,15,1,14,   16,16,22,25,17,17,17,
            4,0,12,4,4,4,14,       2,0,6,2,2,18,12,     16,16,18,20,24,20,18,12,4,4,4,4,4,14,
            0,0,26,21,21,17,17,    0,0,22,25,17,17,17,  0,0,14,17,17,17,14,  0,0,30,17,30,16,16,
            0,0,15,17,15,1,1,      0,0,22,25,16,16,16,  0,0,14,16,14,1,30,   8,8,28,8,8,9,6,
            0,0,17,17,17,19,13,    0,0,17,17,17,10,4,   0,0,17,17,21,21,10,  0,0,17,10,4,10,17,
            0,0,17,17,15,1,14,     0,0,31,2,4,8,31
    };

    public static float OFFSET_X = 0.5F;
    public static float OFFSET_Y = 0.5F;
    public static float OFFSET_Z = 0.5F;
    private static long lastTime = 0L;

    private CodeRainRenderer() {
    }

    public static void renderCodeRain(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay) {
        long time = Util.getMillis();
        if (Minecraft.getInstance().isPaused()) {
            time = lastTime;
        } else {
            lastTime = time;
        }

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(PolygonRenderer.RenderTypes.HIGHLIGHT);

        float halfW = CHAR_WIDTH / 2.0F;
        float halfH = CHAR_HEIGHT / 2.0F;
        float pixelW = CHAR_WIDTH / FONT_COLS;
        float pixelH = CHAR_HEIGHT / FONT_ROWS;

        for (int s = 0; s < STREAM_COUNT; ++s) {
            float seed = (float) s * 137.508F;
            float angle = seed * 2.399963F % ((float) Math.PI * 2F);
            float dirX = Mth.cos(angle);
            float dirZ = Mth.sin(angle);
            float speed = 0.4F + hash(seed) * 0.8F;
            float phase = hash(seed + 1.0F) * 100.0F;
            float scroll = ((float) time / 1000.0F * speed + phase) % 1.6F;
            if (scroll > 0.8F) {
                --scroll;
            }

            int trailLen = 6 + (int) (hash(seed + 2.0F) * 10.0F);
            if (trailLen > MAX_TRAIL_LENGTH) trailLen = MAX_TRAIL_LENGTH;

            for (int c = 0; c < trailLen; c++) {
                float charY = scroll - (float) c * CHAR_HEIGHT * 1.2F;
                if (charY < -HEIGHT_RANGE || charY > HEIGHT_RANGE) continue;
                float x = dirX * RADIUS + OFFSET_X;
                float z = dirZ * RADIUS + OFFSET_Z;
                float y = charY + OFFSET_Y;

                float trailFrac = (float) c / (float) trailLen;
                float alpha = 0.7F * (1.0F - trailFrac * 0.8F);
                if (c == 0) {
                    alpha = 0.95F;
                }
                float flicker = 0.7F + 0.3F * hash(seed + (float) c + (float) time * 0.001F);
                alpha *= flicker;
                int a = (int) (alpha * 255.0F);
                // 蓝绿色系：绿色分量偏亮、蓝色分量补充，形成蓝绿字符
                int g = (int) (140.0F + 80.0F * alpha);
                int b = (int) (170.0F + 70.0F * alpha);
                if (a > 255) a = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
                float cr = 0.0F;
                float cg = g / 255.0F;
                float cb = b / 255.0F;
                float ca = a / 255.0F;

                int chIdx = charToFontIndex(CHARS[(int) (hash(seed + (float) c * 47.0F) * CHARS.length)]);
                drawChar(matrix, consumer, chIdx, x, y, z, dirX, dirZ,
                        halfW, halfH, pixelW, pixelH, cr, cg, cb, ca);
            }
        }
    }

    /** 把字符映射到 FONT_DATA 索引：0-9 → 0-9，A-Z → 10-35 */
    private static int charToFontIndex(char ch) {
        if (ch >= '0' && ch <= '9') return ch - '0';
        if (ch >= 'A' && ch <= 'Z') return 10 + (ch - 'A');
        if (ch >= 'a' && ch <= 'z') return 10 + (ch - 'a');
        return 0;
    }

    /**
     * 逐像素画一个 5x7 点阵字符（每个亮像素一个小方块 quad）。
     * 面板朝向：面板平面沿 Y 轴与径向垂直方向展开，法线朝径向 (dirX,0,dirZ)。
     * 宽度单位向量 w=(dirZ,0,-dirX)，高度单位向量 h=(0,1,0)。
     */
    private static void drawChar(Matrix4f matrix, VertexConsumer consumer, int chIdx,
                                 float cx, float cy, float cz, float dirX, float dirZ,
                                 float halfW, float halfH, float pixelW, float pixelH,
                                 float r, float g, float b, float a) {
        // 面板局部基：宽度方向 w，高度方向 h（法线 = w × h = 径向）
        float wx = dirZ;
        float wz = -dirX;
        float hx = 0.0F;
        float hy = 1.0F;
        float hz = 0.0F;

        int base = chIdx * FONT_ROWS;
        for (int row = 0; row < FONT_ROWS; row++) {
            int bitmask = FONT_DATA[base + row];
            for (int col = 0; col < FONT_COLS; col++) {
                if (((bitmask >> (FONT_COLS - 1 - col)) & 1) == 0) continue;
                // 像素中心在面板局部坐标 (u, v)
                float u = -halfW + (col + 0.5F) * pixelW;
                float v = -halfH + (row + 0.5F) * pixelH;
                float du = pixelW / 2.0F;
                float dv = pixelH / 2.0F;
                // 世界坐标 = 中心 + u*w + v*h
                float pxc = cx + u * wx;
                float pyc = cy + v * hy;
                float pzc = cz + u * wz;
                quad(matrix, consumer,
                        pxc, pyc, pzc,
                        wx, wz, hx, hy, hz,
                        du, dv, r, g, b, a);
            }
        }
    }

    /** 沿面板基向量的单个像素小方块（法线朝径向，NO_CULL 下双面可见） */
    private static void quad(Matrix4f matrix, VertexConsumer consumer,
                             float cx, float cy, float cz,
                             float wx, float wz, float hx, float hy, float hz,
                             float du, float dv, float r, float g, float b, float a) {
        // 四角：中心 ± du*w ± dv*h（从法线方向看逆时针）
        consumer.vertex(matrix, cx - du * wx - dv * hx, cy - dv * hy, cz - du * wz - dv * hz).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, cx + du * wx - dv * hx, cy - dv * hy, cz + du * wz - dv * hz).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, cx + du * wx + dv * hx, cy + dv * hy, cz + du * wz + dv * hz).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, cx - du * wx + dv * hx, cy + dv * hy, cz - du * wz + dv * hz).color(r, g, b, a).endVertex();
    }

    private static float hash(float n) {
        // Math.sin 可能为负，Java 的 % 保留负号 → 结果落在 [-1,1) 会让字符索引变负数。
        // 用 floor 取小数部分，恒返回 [0,1)。
        float h = (float) (Math.sin((double) n * 127.1 + 311.7) * 43758.5453);
        return h - (float) Math.floor(h);
    }
}
