package net.diexv.potionenchant.client.font;

import net.diexv.potionenchant.mixin.accessor.FontAccessor;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.function.Function;

/**
 * DiexvSword 专用字体（移植自源项目 DiexvDreamItem 的 DiexvFont）。
 *
 * 与源项目一致的蓝色调 HSV 渐变：
 *  - hue = 0.58（蓝），饱和度随时间递减 0.6 * (1 - progress)
 *  - 逐字符正弦偏移 + 粉色/蓝色残影
 *
 * 独立类，避免影响本模组 X 系列物品使用的 DiexvFont（浅粉→浅蓝渐变）。
 */
@OnlyIn(Dist.CLIENT)
public class DiexvSwordFont extends Font {

    public static int nametick = 0;
    private static int mode;
    public static final Minecraft mc = Minecraft.getInstance();

    public DiexvSwordFont(Function<ResourceLocation, FontSet> p_243253_, boolean p_243245_) {
        super(p_243253_, p_243245_);
    }

    @NotNull
    public static DiexvSwordFont getFont() {
        return new DiexvSwordFont(((FontAccessor) Minecraft.getInstance().font).getFonts(), false);
    }

    public static double rangeRemap(double value, double low1, double high1, double low2, double high2) {
        return low2 + (value - low1) * (high2 - low2) / (high1 - low1);
    }

    public static long milliTime() {
        return System.nanoTime() / 1000000L;
    }

    @Override
    public int drawInBatch(@NotNull FormattedCharSequence formattedCharSequence, float x, float y, int rgb, boolean b1, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource multiBufferSource, @NotNull DisplayMode mode, int i, int i1) {
        StringBuilder stringBuilder = new StringBuilder();
        formattedCharSequence.accept((indexx, style, codePoint) -> {
            stringBuilder.appendCodePoint(codePoint);
            return true;
        });
        String text = ChatFormatting.stripFormatting(stringBuilder.toString());
        long time = Util.getMillis();
        float cycleSpeed = 0.001F;
        if (text != null) {
            for (int index = 0; index < text.length(); ++index) {
                String s = String.valueOf(text.charAt(index));
                float xOffset = (float) (Math.sin((double) ((float) time / 200.0F + (float) index)) * 1.0D);
                float yOffset = (float) (Math.cos((double) ((float) time / 200.0F + (float) index)) * 1.0D);
                float progress = ((float) time * cycleSpeed + (float) index * 0.06F) % 1.0F;
                // 渐变颜色（与源项目一致）：蓝色调 hue=0.58，饱和度随时间递减
                float saturation = 0.6F * (1.0F - progress);
                int color = Mth.hsvToRgb(0.58F, saturation, 1.0F);

                super.drawInBatch(s, x + xOffset, y + yOffset, color | -16777216, b1, matrix4f, multiBufferSource, mode, i, i1);
                int lightPinkGlow = 0x18FFE6F3;
                int lightBlueGlow = 0x18E6F3FF;
                super.drawInBatch(s, x + xOffset + 0.3F, y + yOffset + 0.3F, lightPinkGlow, b1, matrix4f, multiBufferSource, mode, i, i1);
                super.drawInBatch(s, x + xOffset - 0.2F, y + yOffset - 0.2F, lightBlueGlow, b1, matrix4f, multiBufferSource, mode, i, i1);
                x += (float) this.width(s);
            }
        }
        return (int) x;
    }

    @Override
    public int drawInBatch(@NotNull String string, float x, float y, int rgb, boolean b, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource source, @NotNull DisplayMode mode, int i, int i1) {
        return this.drawInBatch(Component.literal(string).getVisualOrderText(), x, y, rgb, b, matrix4f, source, mode, i, i1);
    }

    @Override
    public int drawInBatch(@NotNull Component component, float x, float y, int rgb, boolean b, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource source, @NotNull DisplayMode mode, int i, int i1) {
        return this.drawInBatch(component.getVisualOrderText(), x, y, rgb, b, matrix4f, source, mode, i, i1);
    }
}
