package net.diexv.potionenchant.util.font;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.diexv.potionenchant.mixin.accessor.FontAccessor;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.function.Function;

public class DiexvFont3 extends Font {
    public static int nametick = 0;
    private static int mode;
    public static final Minecraft mc = Minecraft.getInstance();

    // 可配置的渐变速度参数（毫秒）
    private static final float COLOR_SPEED = 1100f; // 值越大变化越慢

    public DiexvFont3(Function<ResourceLocation, FontSet> p_243253_, boolean p_243245_) {
        super(p_243253_, p_243245_);
    }

    @NotNull
    public static DiexvFont3 getFont() {
        return new DiexvFont3(((FontAccessor) Minecraft.getInstance().font).getFonts(), false);
    }

    public static double rangeRemap(double value, double low1, double high1, double low2, double high2) {
        return low2 + (value - low1) * (high2 - low2) / (high1 - low1);
    }

    public static long milliTime() {
        return System.nanoTime() / 1000000L;
    }

    // 生成彩虹色（带速度控制）
    private int getRainbowColor(long time, int index, int totalLength) {
        // 使用COLOR_SPEED控制渐变速度
        float hue = ((time / COLOR_SPEED + (totalLength - index) * 0.1f) % 1f);
        return Mth.hsvToRgb(hue, 0.8f, 1.0f);
    }

    // 生成互补色（用于残影效果）
    private int getComplementaryColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float[] hsv = new float[3];
        java.awt.Color.RGBtoHSB(r, g, b, hsv);
        hsv[0] = (hsv[0] + 0.5f) % 1.0f;

        return java.awt.Color.HSBtoRGB(hsv[0], hsv[1], hsv[2] * 0.5f);
    }

    public int drawInBatch(@NotNull FormattedCharSequence formattedCharSequence, float x, float y, int rgb, boolean b1, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource multiBufferSource, @NotNull Font.DisplayMode mode, int i, int i1) {
        StringBuilder stringBuilder = new StringBuilder();
        formattedCharSequence.accept((index, style, codePoint) -> {
            stringBuilder.appendCodePoint(codePoint);
            return true;
        });
        String text = ChatFormatting.stripFormatting(stringBuilder.toString());
        long time = Util.getMillis();

        if (text != null) {
            int length = text.length();
            for (int index = 0; index < length; index++) {
                String s = String.valueOf(text.charAt(index));
                float xOffset = (float)(Math.sin((time / 200.0F + index)) * 1.0F);
                float yOffset = (float)(Math.cos((time / 200.0F + index)) * 1.0F);

                int color = getRainbowColor(time, index, length);
                int shadowColor = getComplementaryColor(color) & 0x33FFFFFF;

                super.drawInBatch(s, x, y + yOffset, color, b1, matrix4f, multiBufferSource, mode, i, i1);
                super.drawInBatch(s, x + 0.2F, y + 0.2F, shadowColor, b1, matrix4f, multiBufferSource, mode, i, i1);

                x += width(s);
            }
        }
        return (int)x;
    }

    public int drawInBatch(@NotNull String string, float x, float y, int rgb, boolean b, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource source, @NotNull Font.DisplayMode mode, int i, int i1) {
        return drawInBatch(Component.literal(string).getVisualOrderText(), x, y, rgb, b, matrix4f, source, mode, i, i1);
    }

    public int drawInBatch(@NotNull Component component, float x, float y, int rgb, boolean b, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource source, @NotNull Font.DisplayMode mode, int i, int i1) {
        return drawInBatch(component.getVisualOrderText(), x, y, rgb, b, matrix4f, source, mode, i, i1);
    }
}