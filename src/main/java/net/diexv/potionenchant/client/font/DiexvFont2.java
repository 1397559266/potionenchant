package net.diexv.potionenchant.client.font;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.function.Function;

public class DiexvFont2 extends Font {
    public static final Minecraft mc = Minecraft.getInstance();

    public DiexvFont2(Function<ResourceLocation, FontSet> p_243253_, boolean p_243245_) {
        super(p_243253_, p_243245_);
    }

    public int drawInBatch(@NotNull FormattedCharSequence formattedCharSequence, float x, float y, int rgb, boolean b1, @NotNull Matrix4f matrix4f, @NotNull MultiBufferSource multiBufferSource, @NotNull Font.DisplayMode mode, int i, int i1) {
        StringBuilder stringBuilder = new StringBuilder();
        formattedCharSequence.accept((index, style, codePoint) -> {
            stringBuilder.appendCodePoint(codePoint);
            return true;
        });
        String text = ChatFormatting.stripFormatting(stringBuilder.toString());

        if (text != null) {
            for (int index = 0; index < text.length(); index++) {
                String s = String.valueOf(text.charAt(index));

                // 浅粉色到浅蓝色渐变计算（静态，基于字符位置）
                float progress = (index * 0.06f) % 1.0f;
                int color;

                if (progress < 0.5f) {
                    // 浅粉色到白色过渡 (0.0 - 0.5)
                    float phase = progress * 2.0f; // 0.0到1.0
                    float r = 1.0f; // 浅粉色R
                    float g = 0.85f + phase * 0.15f; // 浅粉色G到白色G
                    float b = 0.90f + phase * 0.10f; // 浅粉色B到白色B
                    color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                } else {
                    // 白色到浅蓝色过渡 (0.5 - 1.0)
                    float phase = (progress - 0.5f) * 2.0f; // 0.0到1.0
                    float r = 1.0f - phase * 0.3f; // 白色R到浅蓝色R
                    float g = 1.0f - phase * 0.2f; // 白色G到浅蓝色G
                    float b = 1.0f; // 浅蓝色B
                    color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                }

                // 主文本
                super.drawInBatch(s, x, y, color | 0xFF000000, b1, matrix4f, multiBufferSource, mode, i, i1);

                // 浅粉色和浅蓝色的柔和残影
                int lightPinkGlow = 0xFFFFE6F3 & 0x00FFFFFF | 0x18000000; // 很浅的粉色残影
                int lightBlueGlow = 0xFFE6F3FF & 0x00FFFFFF | 0x18000000; // 很浅的蓝色残影

                super.drawInBatch(s, x + 0.3F, y + 0.3F, lightPinkGlow, b1, matrix4f, multiBufferSource, mode, i, i1);
                super.drawInBatch(s, x - 0.2F, y - 0.2F, lightBlueGlow, b1, matrix4f, multiBufferSource, mode, i, i1);

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
