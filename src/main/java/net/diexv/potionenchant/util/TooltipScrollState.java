package net.diexv.potionenchant.util;

/**
 * 药水附魔tooltip滚动状态管理器
 * 用于在mixin和事件处理器之间共享滚动状态
 */
public class TooltipScrollState {

    // 当前滚动偏移量
    private static int currentScrollOffset = 0;

    // 总效果行数
    private static int totalEffectLines = 0;

    // 最大可见效果数量
    private static final int MAX_VISIBLE_EFFECTS = 10;

    // 工具提示是否正在显示
    private static boolean tooltipVisible = false;

    // 工具提示的屏幕区域（用于鼠标检测）
    private static int tooltipX = 0, tooltipY = 0, tooltipWidth = 0, tooltipHeight = 0;

    // 是否正在拖动滚动条
    private static boolean isDraggingScrollbar = false;

    /**
     * 获取当前滚动偏移量
     */
    public static int getCurrentScrollOffset() {
        return currentScrollOffset;
    }

    /**
     * 设置当前滚动偏移量
     */
    public static void setCurrentScrollOffset(int offset) {
        currentScrollOffset = offset;
        clampOffset();
    }

    /**
     * 获取总效果行数
     */
    public static int getTotalEffectLines() {
        return totalEffectLines;
    }

    /**
     * 设置总效果行数
     */
    public static void setTotalEffectLines(int lines) {
        totalEffectLines = lines;
        clampOffset();
    }

    /**
     * 调整滚动偏移量
     */
    public static void adjustScrollOffset(int delta) {
        if (totalEffectLines <= MAX_VISIBLE_EFFECTS) {
            currentScrollOffset = 0;
            return;
        }
        currentScrollOffset += delta;
        clampOffset();
    }

    /**
     * 限制偏移量范围
     */
    private static void clampOffset() {
        if (totalEffectLines > MAX_VISIBLE_EFFECTS) {
            int maxOffset = totalEffectLines - MAX_VISIBLE_EFFECTS;
            currentScrollOffset = Math.max(0, Math.min(currentScrollOffset, maxOffset));
        } else {
            currentScrollOffset = 0;
        }
    }

    /**
     * 重置滚动偏移量
     */
    public static void resetScrollOffset() {
        currentScrollOffset = 0;
        totalEffectLines = 0;
        isDraggingScrollbar = false;
        tooltipVisible = false;
    }

    /**
     * 获取最大可见效果数量
     */
    public static int getMaxVisibleEffects() {
        return MAX_VISIBLE_EFFECTS;
    }

    // ---------- 工具提示可见性与区域 ----------

    public static boolean isTooltipVisible() {
        return tooltipVisible;
    }

    public static void setTooltipVisible(boolean visible) {
        tooltipVisible = visible;
    }

    public static void setTooltipBounds(int x, int y, int width, int height) {
        tooltipX = x;
        tooltipY = y;
        tooltipWidth = width;
        tooltipHeight = height;
    }

    public static int getTooltipX() { return tooltipX; }
    public static int getTooltipY() { return tooltipY; }
    public static int getTooltipWidth() { return tooltipWidth; }
    public static int getTooltipHeight() { return tooltipHeight; }

    // ---------- 滚动条拖动状态 ----------

    public static boolean isDraggingScrollbar() {
        return isDraggingScrollbar;
    }

    public static void setDraggingScrollbar(boolean dragging) {
        isDraggingScrollbar = dragging;
    }
}