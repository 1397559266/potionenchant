package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 药水HUD滚动管理器 - 处理鼠标滚轮滚动和聊天框拖动
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PotionHudScrollHandler {
    
    // 当前滚动偏移量（显示从第几个效果开始）
    private static int scrollOffset = 0;
    
    // 是否正在拖动滚动条
    private static boolean isDraggingScrollbar = false;
    
    /**
     * 获取当前滚动偏移量
     */
    public static int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * 重置滚动偏移量
     */
    public static void resetScrollOffset() {
        scrollOffset = 0;
        isDraggingScrollbar = false;
    }
    
    /**
     * 设置拖动状态
     */
    public static void setDragging(boolean dragging) {
        isDraggingScrollbar = dragging;
    }
    
    /**
     * 检查是否正在拖动
     */
    public static boolean isDragging() {
        return isDraggingScrollbar;
    }
    
    /**
     * 调整滚动偏移量
     */
    public static void adjustScrollOffset(int delta, int totalEffects) {
        int maxVisible = PotionEnchantConfig.CLIENT.maxVisibleEffects.get();
        
        if (totalEffects <= maxVisible) {
            // 如果效果总数不超过最大显示数，不需要滚动
            scrollOffset = 0;
            return;
        }
        
        // 调整偏移量
        scrollOffset += delta;
        
        // 限制范围
        int maxOffset = totalEffects - maxVisible;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }
    
    /**
     * 监听鼠标滚轮事件
     * 只有在按住Tab键时才能使用滚轮滚动药水HUD
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        
        // 只在游戏内或聊天框中有效
        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            return;
        }
        
        // 检查是否启用了自定义HUD
        if (!PotionEnchantConfig.CLIENT.customPotionHud.get()) {
            return;
        }
        
        var player = mc.player;
        if (player == null) {
            return;
        }
        
        var effects = player.getActiveEffects();
        if (effects == null || effects.isEmpty()) {
            return;
        }
        
        int totalEffects = effects.size();
        int maxVisible = PotionEnchantConfig.CLIENT.maxVisibleEffects.get();
        
        // 只有当效果数量超过最大显示数时才处理滚动
        if (totalEffects > maxVisible) {
            // 检查是否按住了Tab键
            long window = mc.getWindow().getWindow();
            boolean isTabPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
            
            // 只有在按住Tab键时才允许滚轮滚动
            if (!isTabPressed) {
                return;
            }
            
            // 获取滚动方向（负值=向上滚动，正值=向下滚动）
            double scrollDelta = event.getScrollDelta();
            
            if (scrollDelta != 0) {
                // 调整滚动偏移量
                int direction = scrollDelta < 0 ? 1 : -1;
                adjustScrollOffset(direction, totalEffects);
                
                // 消耗事件，防止其他功能触发
                event.setCanceled(true);
            }
        }
    }
    
    // 注意：滚动条拖动逻辑已移至PotionHudMixin.handleScrollbarDrag()中处理
    // 该方法在每帧渲染时检测鼠标状态，无需额外的事件监听器
}
