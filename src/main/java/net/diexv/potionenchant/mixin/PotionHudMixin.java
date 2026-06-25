package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.handlers.PotionHudScrollHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Mixin(Gui.class)
public class PotionHudMixin {
    
    // 缓存配置值，避免每帧读取
    private static boolean cachedCustomHudEnabled = true;
    private static boolean cachedShowLevel = true;
    private static boolean cachedShowBackground = false;
    private static int cachedMaxVisible = 10;
    private static boolean cachedShowScrollHint = true;
    private static boolean cachedHighPriority = true;
    private static long lastConfigCheck = 0;
    private static final long CONFIG_CHECK_INTERVAL = 500; // 每500ms检查一次配置
    
    // 拦截游戏中屏幕右上角的药水效果图标渲染，并替换为自定义文字
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, CallbackInfo ci) {
        // 定期更新缓存配置（减少配置读取频率）
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConfigCheck > CONFIG_CHECK_INTERVAL) {
            cachedCustomHudEnabled = PotionEnchantConfig.CLIENT.customPotionHud.get();
            cachedShowLevel = PotionEnchantConfig.CLIENT.showPotionLevel.get();
            cachedShowBackground = PotionEnchantConfig.CLIENT.showTextBackground.get();
            cachedMaxVisible = PotionEnchantConfig.CLIENT.maxVisibleEffects.get();
            cachedShowScrollHint = PotionEnchantConfig.CLIENT.showScrollHint.get();
            cachedHighPriority = PotionEnchantConfig.CLIENT.hudHighPriority.get();
            lastConfigCheck = currentTime;
        }
        
        // 如果启用了自定义HUD，取消原版渲染并渲染自定义HUD
        if (cachedCustomHudEnabled) {
            ci.cancel(); // 取消原版渲染
            
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            
            if (player == null) {
                return;
            }
            
            Collection<MobEffectInstance> effects = player.getActiveEffects();
            if (effects == null || effects.isEmpty()) {
                // 重置滚动偏移量
                PotionHudScrollHandler.resetScrollOffset();
                return;
            }
            
            int totalEffects = effects.size();
            int maxVisible = cachedMaxVisible;
            
            // 如果效果数量变化，可能需要重置滚动
            int scrollOffset = PotionHudScrollHandler.getScrollOffset();
            if (scrollOffset >= totalEffects) {
                PotionHudScrollHandler.resetScrollOffset();
                scrollOffset = 0;
            }
            
            // 将效果转换为列表并排序
            List<MobEffectInstance> sortedEffects = effects.stream()
                    .sorted(Comparator.comparingInt(effect -> MobEffect.getId(effect.getEffect())))
                    .toList();
            
            // 计算要显示的效果范围
            int startIndex = scrollOffset;
            int endIndex = Math.min(startIndex + maxVisible, totalEffects);
            
            // 保存当前的PoseStack状态，确保不影响其他渲染
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            
            try {
                // 不使用Z轴偏移，完全避免影响其他模组的渲染
                
                // 只渲染可见范围内的效果
                for (int i = startIndex; i < endIndex; i++) {
                    renderCustomEffect(guiGraphics, sortedEffects.get(i), mc, i - startIndex, totalEffects, maxVisible);
                }
                
                // 如果需要，绘制滚动指示器
                if (totalEffects > maxVisible) {
                    // 处理滚动条拖动
                    handleScrollbarDrag(guiGraphics, mc, totalEffects, maxVisible);
                    drawScrollIndicator(guiGraphics, mc, scrollOffset, totalEffects, maxVisible);
                }
            } finally {
                // 确保无论如何都恢复PoseStack状态
                poseStack.popPose();
            }
        }
    }
    
    // 渲染自定义药水效果文字
    private static void renderCustomEffect(GuiGraphics guiGraphics, MobEffectInstance effect, Minecraft mc, int visibleIndex, int totalEffects, int maxVisible) {
        MobEffect mobEffect = effect.getEffect();
        
        // 获取药水颜色
        int color = mobEffect.getColor();
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        
        // 获取效果名称
        Component effectName = mobEffect.getDisplayName();
        
        // 构建药水名称+等级文本
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(effectName.getString());
        
        // 如果启用了等级显示，使用阿拉伯数字
        if (cachedShowLevel) {
            int amplifier = effect.getAmplifier();
            nameBuilder.append(" ").append(amplifier + 1);
        }
        String nameText = nameBuilder.toString();
        
        // 格式化剩余时间（所有效果都正常显示倒计时）
        int duration = effect.getDuration();
        String timeString = formatTime(duration);
        String timeText = "(" + timeString + ")";
        
        // 计算显示位置（右上角对齐）
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int nameWidth = mc.font.width(nameText);
        
        // 为倒计时设置固定宽度，避免数字变化时左右跳动
        // 最大格式为 "(MM:SS)" 即 "(99:59)" = 7个字符
        int fixedTimeWidth = mc.font.width("(99:59)");
        int totalWidth = nameWidth + 4 + fixedTimeWidth; // 4像素间距
        
        // 右侧起始位置
        int rightX = screenWidth - 10;
        int xPos = rightX - totalWidth;
        
        // 垂直位置：根据可见索引计算
        int yPos = 10 + (visibleIndex * (mc.font.lineHeight + 3)); // 增加间距防止重叠
        
        // 如果启用了文本背景框
        if (cachedShowBackground) {
            // 绘制半透明背景（覆盖整个区域）
            int backgroundColor = 0x90000000; // 56%透明黑色（稍深一点）
            guiGraphics.fill(xPos - 3, yPos - 2, xPos + totalWidth + 3, yPos + mc.font.lineHeight + 2, backgroundColor);
        }
        
        // 绘制文字（使用药水颜色）
        int textColor = ((int) (red * 255) << 16) | ((int) (green * 255) << 8) | ((int) (blue * 255));
        
        // 分别绘制名称和倒计时，倒计时右对齐到固定宽度区域，避免整行闪烁
        guiGraphics.drawString(mc.font, nameText, xPos, yPos, textColor, true);
        // 倒计时在固定宽度区域内右对齐
        int timeX = xPos + nameWidth + 4 + (fixedTimeWidth - mc.font.width(timeText));
        guiGraphics.drawString(mc.font, timeText, timeX, yPos, textColor, true);
    }
    
    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.valueOf(seconds);
        }
    }
    
    // 绘制滚动指示器
    private static void drawScrollIndicator(GuiGraphics guiGraphics, Minecraft mc, int scrollOffset, int totalEffects, int maxVisible) {
        // 在右侧绘制一个可拖动的滚动条
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int scrollbarX = screenWidth - 8; // 距离右边界8像素
        
        // 计算滚动条的位置和高度
        int startY = 10;
        int totalHeight = maxVisible * (mc.font.lineHeight + 3);
        int scrollbarHeight = Math.max(20, totalHeight * maxVisible / totalEffects); // 滚动条高度
        
        // 计算滚动条的Y位置
        float scrollRatio = (float) scrollOffset / (totalEffects - maxVisible);
        int scrollbarY = startY + (int) (scrollRatio * (totalHeight - scrollbarHeight));
        
        // 绘制滚动条背景
        guiGraphics.fill(scrollbarX, startY, scrollbarX + 5, startY + totalHeight, 0x60FFFFFF);
        
        // 绘制滚动条（更宽更明显）
        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 5, scrollbarY + scrollbarHeight, 0xFFFFFFFF);
        
        // 绘制提示文本，放在滚动条上方，避免与药水文字重叠
        String hint = (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxVisible, totalEffects) + "/" + totalEffects;
        int hintWidth = mc.font.width(hint);
        // 将提示文本显示在滚动条上方，不占用药水显示区域
        guiGraphics.drawString(mc.font, hint, scrollbarX - hintWidth - 2, startY - mc.font.lineHeight - 2, 0xFFFFFFFF, true);
        
        // 显示Tab键提示（根据配置决定是否显示）
        if (cachedShowScrollHint) {
            String tabHint = "Tab+滚轮";
            int tabHintWidth = mc.font.width(tabHint);
            guiGraphics.drawString(mc.font, tabHint, scrollbarX - tabHintWidth - 2, startY + totalHeight + 2, 0xFF888888, true);
        }
    }
    
    /**
     * 处理滚动条拖动逻辑
     * 在渲染时检测鼠标状态
     */
    private static void handleScrollbarDrag(GuiGraphics guiGraphics, Minecraft mc, int totalEffects, int maxVisible) {
        if (totalEffects <= maxVisible) {
            return;
        }
        
        // 检测鼠标左键是否按下
        long window = mc.getWindow().getWindow();
        boolean isLeftMouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        
        if (isLeftMouseDown) {
            // 获取鼠标位置
            double mouseX = mc.mouseHandler.xpos();
            double mouseY = mc.mouseHandler.ypos();
            
            // 计算滚动条区域
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            double scale = mc.getWindow().getGuiScale();
            int guiMouseX = (int) (mouseX / scale);
            int guiMouseY = (int) (mouseY / scale);
            
            int scrollbarX = screenWidth - 8;
            int scrollbarWidth = 5;
            int startY = 10;
            int totalHeight = maxVisible * (mc.font.lineHeight + 3);
            int scrollbarHeight = Math.max(20, totalHeight * maxVisible / totalEffects);
            
            // 检查鼠标是否在滚动条区域内
            if (guiMouseX >= scrollbarX && guiMouseX <= scrollbarX + scrollbarWidth) {
                // 计算新的滚动偏移量
                float scrollRatio = Math.max(0, Math.min(1, 
                    (float) (guiMouseY - startY - scrollbarHeight / 2) / (totalHeight - scrollbarHeight)));
                int newScrollOffset = (int) (scrollRatio * (totalEffects - maxVisible));
                newScrollOffset = Math.max(0, Math.min(newScrollOffset, totalEffects - maxVisible));
                
                // 更新滚动偏移量
                PotionHudScrollHandler.setDragging(true);
                // 直接设置偏移量
                int oldOffset = PotionHudScrollHandler.getScrollOffset();
                if (oldOffset != newScrollOffset) {
                    // 通过调整方法来更新
                    PotionHudScrollHandler.adjustScrollOffset(newScrollOffset - oldOffset, totalEffects);
                }
            }
        } else {
            PotionHudScrollHandler.setDragging(false);
        }
    }
}