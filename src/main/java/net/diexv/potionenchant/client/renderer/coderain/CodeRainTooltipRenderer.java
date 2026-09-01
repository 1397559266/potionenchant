package net.diexv.potionenchant.client.renderer.coderain;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * DiexvSword（diexv_sword 物品）的 tooltip 代码雨特效。
 *
 * 布局：tooltip 矩形区域内 N 列字符流，每列随机速度向下/向上滚动，
 * 头部字符最亮、拖尾渐隐、每字符独立闪烁——与物品粒子代码雨同一套
 * 5x7 点阵位图与蓝绿色系。
 *
 * 淡入淡出状态机：
 *  - markShowing()    ：tooltip 渲染中调用，记录 tick、目标 alpha=1
 *  - onClientTick()   ：每 tick 检测超过 2 tick 未渲染 → 目标 alpha=0（淡出），指数插值
 *  - hasContent()     ：alpha 仍 > 0.01 时在 ScreenEvent.Render.Post 继续绘制残留（关闭后的淡出帧）
 */
@OnlyIn(Dist.CLIENT)
public final class CodeRainTooltipRenderer {
    // ===== 淡入淡出状态 =====
    private static boolean showing = false;
    private static float alpha = 0.0F;
    private static float prevAlpha = 0.0F;
    private static float targetAlpha = 0.0F;
    private static int currentTick = 0;
    private static int lastRenderTick = -1000;
    /** 最后渲染的 tooltip 矩形（关闭后淡出帧用） */
    private static int lastX, lastY, lastW, lastH;
    private static boolean hasLastRect = false;
    private static long lastTime = 0L;

    // ===== 字符集与 5x7 点阵（与 CodeRainRenderer 一致） =====
    private static final int FONT_ROWS = 7;
    private static final int FONT_COLS = 5;
    private static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
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

    private CodeRainTooltipRenderer() {
    }

    // ===================== 状态机 =====================

    /** tooltip 渲染中被调用：标记正在显示，记录矩形与 tick */
    public static void markShowing(int x, int y, int w, int h) {
        showing = true;
        lastRenderTick = currentTick;
        targetAlpha = 1.0F;
        if (alpha < 0.1F) {
            alpha = 0.1F; // 确保淡入开始即可见
        }
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;
        hasLastRect = true;
    }

    /** 每客户端 tick 调用：检测关闭（>2 tick 无渲染）并平滑插值 alpha */
    public static void onClientTick() {
        currentTick++;
        if (currentTick - lastRenderTick > 2) {
            showing = false;
            targetAlpha = 0.0F;
        }
        prevAlpha = alpha; // 记录上一 tick 的 alpha（供 partialTicks 帧间插值）
        float alphaSpeed = 0.15F;
        alpha += (targetAlpha - alpha) * alphaSpeed;
        if (Math.abs(alpha - targetAlpha) < 0.001F) {
            alpha = targetAlpha;
        }
    }

    /** 渲染用 alpha：在 prevAlpha 与 alpha 之间按 partialTicks 插值，与帧率同步 */
    public static float getRenderAlpha(float partialTicks) {
        return prevAlpha + (alpha - prevAlpha) * partialTicks;
    }

    public static boolean hasContent() {
        return alpha > 0.01F;
    }

    public static float getAlpha() {
        return alpha;
    }

    public static boolean hasLastRect() {
        return hasLastRect;
    }

    public static int getLastX() { return lastX; }
    public static int getLastY() { return lastY; }
    public static int getLastW() { return lastW; }
    public static int getLastH() { return lastH; }

    // ===================== 渲染 =====================

    /** 全屏渲染字符代码雨（批量顶点提交，避免逐段 fill 的性能瓶颈） */
    public static void renderCodeRain(GuiGraphics g, int x, int y, int w, int h, float globalAlpha) {
        if (globalAlpha <= 0.01F || w < 8 || h < 8) return;

        long time = Util.getMillis();
        if (Minecraft.getInstance().isPaused()) {
            time = lastTime;
        } else {
            lastTime = time;
        }

        // 字符放大倍率：5x7 点阵 → 每像素 1px（字符 5x7）
        int scale = 1;
        int cw = FONT_COLS * scale;        // 5
        int ch = FONT_ROWS * scale;        // 7
        // 列间距加大（3 倍字宽）降低全屏密度，显著减少顶点数
        int spacing = cw * 3;
        int cols = Math.max(1, w / spacing);

        // 滚动速度：像素/毫秒（120px/s）
        float speedBase = 0.12F;

        // 批量渲染：只获取一次 buffer，所有矩形直接写顶点，最后一次性提交
        MultiBufferSource.BufferSource bufferSource = g.bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(RenderType.gui());
        org.joml.Matrix4f matrix = g.pose().last().pose();

        for (int col = 0; col < cols; col++) {
            float seed = col * 137.508F;
            float speed = speedBase * (0.6F + hash(seed) * 0.9F);
            float direction = (hash(seed + 3.0F) > 0.5F) ? 1.0F : -1.0F;
            float phase = hash(seed + 5.0F) * 2000.0F;

            int trailLen = 8 + (int) (hash(seed + 2.0F) * 11.0F); // 8~18 拖尾（加长字符串）
            // 循环周期 = 屏幕高 + 完整字符串长度：整串（含拖尾）完全穿过屏幕才重置，
            // 避免头部一出屏拖尾就瞬间消失
            float charStep = ch * 1.2F;
            float span = h + trailLen * charStep;
            // floor 取模：direction=1 时 scroll 单调递增（下落），direction=-1 时单调递减（上升），
            // 均平滑循环，无符号取模导致的跳变
            float raw = phase + time * speed * direction;
            float scroll = raw - (float) Math.floor(raw / span) * span;

            int cx = x + 1 + col * spacing;

            for (int c = 0; c < trailLen; c++) {
                float cy = y + scroll - c * charStep; // 字符间距等比：ch * 1.2
                if (cy < y - ch || cy > y + h) continue;

                float trailFrac = (float) c / (float) trailLen;
                // 加亮拖尾：衰减放缓（0.85→0.5），基础亮度 0.9→0.95，
                // 尾部 alpha 从约 0.18 提升到约 0.5，头部仍为 1.0
                float a = 0.95F * (1.0F - trailFrac * 0.5F);
                if (c == 0) a = 1.0F;
                // 平滑慢闪：正弦低频（约 0.5Hz），避免 hash 逐毫秒跳变造成的闪烁
                float flicker = 0.9F + 0.1F * (float) Math.sin((double) time * 0.0032 + (double) seed + (double) c * 0.7);
                a *= flicker * globalAlpha;
                if (a <= 0.02F) continue;

                char chChar = CHARS[(int) (hash(seed + (float) c * 47.0F) * CHARS.length)];
                drawCharTo(vc, matrix, chChar, (int) cx, (int) cy, scale, a);
            }
        }

        bufferSource.endBatch(RenderType.gui());
    }

    /** 按行合并连续亮像素段后写顶点（每段一次 quad，共 4 顶点） */
    private static void drawCharTo(VertexConsumer vc, org.joml.Matrix4f matrix,
                                   char ch, int x, int y, int scale, float alpha) {
        int idx = charToFontIndex(ch);
        int base = idx * FONT_ROWS;
        float gCol = (140.0F + 80.0F * alpha) / 255.0F;
        float bCol = (170.0F + 70.0F * alpha) / 255.0F;
        float aCol = Math.min(alpha, 1.0F);
        if (gCol > 1.0F) gCol = 1.0F;
        if (bCol > 1.0F) bCol = 1.0F;

        for (int row = 0; row < FONT_ROWS; row++) {
            int mask = FONT_DATA[base + row];
            int runStart = -1;
            for (int col = 0; col <= FONT_COLS; col++) {
                boolean lit = col < FONT_COLS && (((mask >> (FONT_COLS - 1 - col)) & 1) == 1);
                if (lit && runStart < 0) {
                    runStart = col; // 开始一段连续亮像素
                } else if (!lit && runStart >= 0) {
                    // 连续段结束：整段一个 quad（4 顶点）
                    float x0 = x + runStart * scale;
                    float x1 = x + col * scale;
                    float y0 = y + row * scale;
                    float y1 = y + (row + 1) * scale;
                    vc.vertex(matrix, x0, y0, 0.0F).color(0.0F, gCol, bCol, aCol).endVertex();
                    vc.vertex(matrix, x0, y1, 0.0F).color(0.0F, gCol, bCol, aCol).endVertex();
                    vc.vertex(matrix, x1, y1, 0.0F).color(0.0F, gCol, bCol, aCol).endVertex();
                    vc.vertex(matrix, x1, y0, 0.0F).color(0.0F, gCol, bCol, aCol).endVertex();
                    runStart = -1;
                }
            }
        }
    }

    private static int charToFontIndex(char ch) {
        if (ch >= '0' && ch <= '9') return ch - '0';
        if (ch >= 'A' && ch <= 'Z') return 10 + (ch - 'A');
        if (ch >= 'a' && ch <= 'z') return 10 + (ch - 'a');
        return 0;
    }

    private static float hash(float n) {
        float h = (float) (Math.sin((double) n * 127.1 + 311.7) * 43758.5453);
        return h - (float) Math.floor(h);
    }
}
