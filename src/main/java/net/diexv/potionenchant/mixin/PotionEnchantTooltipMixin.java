package net.diexv.potionenchant.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.util.font.DiexvFont;
import net.diexv.potionenchant.util.TooltipScrollState;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 药水附魔 Tooltip 自定义渲染 Mixin
 * 包含完整的滚动条功能：绘制、鼠标拖动、Shift+滚轮控制
 */
@Mixin(GuiGraphics.class)
public class PotionEnchantTooltipMixin {

    @Shadow
    @Final
    private PoseStack pose;

    @Unique
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    @Unique
    private static final int TOOLTIP_TEXT_COLOR = -1;
    @Unique
    private static final int MAX_TOOLTIP_WIDTH = 200;
    @Unique
    private static final String COLUMN_SEPARATOR = " ";
    @Unique
    private static final int MAX_VISIBLE_EFFECTS = 10;  // 与 TooltipScrollState 一致
    @Unique
    private static final int SCROLLBAR_WIDTH = 6;
    @Unique
    private static final int SCROLLBAR_MARGIN = 2;

    // 缓存上一次悬停的物品，用于重置滚动
    @Unique
    private static ItemStack lastHoveredStack = null;

    // 总效果行数（由 limitTooltipLines 设置，用于滚动计算）
    @Unique
    private static int totalEffectLines = 0;

    /**
     * 在 renderTooltipInternal 方法的 popPose() 调用之后注入
     * 此时原版 Tooltip 已经渲染完成
     */
    @Inject(
            method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.AFTER),
            remap = true
    )
    private void onRenderTooltipInternal(Font font, List<Component> tooltipLines, int mouseX, int mouseY, net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = null;

        // 尝试从容器界面获取物品
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen =
                    (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) mc.screen;
            var slot = containerScreen.getSlotUnderMouse();
            if (slot != null) {
                stack = slot.getItem();
            }
        }

        // 如果没有从容器界面获取到物品，尝试从玩家手中获取
        if (stack == null || stack.isEmpty()) {
            if (mc.player != null) {
                stack = mc.player.getMainHandItem();
                if (stack.isEmpty()) {
                    stack = mc.player.getOffhandItem();
                }
            }
        }

        // 检查物品是否切换，如果切换则重置滚动位置
        if (stack == null || stack.isEmpty()) {
            TooltipScrollState.resetScrollOffset();
            lastHoveredStack = null;
            return;
        }

        if (lastHoveredStack == null || !ItemStack.matches(lastHoveredStack, stack)) {
            TooltipScrollState.resetScrollOffset();
            lastHoveredStack = stack;
        }

        GuiGraphics guiGraphics = (GuiGraphics) (Object) this;

        List<Component> customTooltipLines = null;

        // 检查是否有药水附魔且配置启用独立tooltip
        if (PotionEnchantManager.hasPotionEnchantments(stack) && PotionEnchantConfig.COMMON.enablePotionEnchantTooltip.get()) {
            customTooltipLines = buildPotionEnchantTooltip(stack, font);
        }

        if (customTooltipLines == null || customTooltipLines.isEmpty()) {
            // 无自定义内容时隐藏工具提示区域
            TooltipScrollState.setTooltipVisible(false);
            return;
        }

        // 限制显示效果数量，并更新 totalEffectLines
        List<Component> limitedTooltipLines = limitTooltipLines(customTooltipLines);

        // 计算tooltip尺寸
        DiexvFont diexvFont = DiexvFont.getFont();
        int maxWidth = calculateMaxWidth(limitedTooltipLines, font, diexvFont);

        // 限制tooltip最大宽度，避免超过屏幕
        int maxAllowedWidth = mc.getWindow().getGuiScaledWidth() - 20;
        maxWidth = Math.min(maxWidth, maxAllowedWidth);

        // 检查是否需要滚动条
        boolean needsScrollbar = totalEffectLines > MAX_VISIBLE_EFFECTS;

        int tooltipWidth = maxWidth + 8;
        if (needsScrollbar) {
            tooltipWidth += SCROLLBAR_WIDTH + SCROLLBAR_MARGIN * 2;
        }

        int tooltipHeight = limitedTooltipLines.size() * 10 + 8;

        // 智能定位
        int x = calculateTooltipX(mouseX, tooltipWidth, mc.getWindow().getGuiScaledWidth());
        int y = calculateTooltipY(mouseY, tooltipHeight, mc.getWindow().getGuiScaledHeight());

        // 处理滚动条拖动（鼠标左键拖拽）
        if (needsScrollbar) {
            handleScrollbarDrag(mc, x, y, tooltipWidth, tooltipHeight);
        }

        // 更新 TooltipScrollState 中的区域信息，供滚轮事件检测使用
        TooltipScrollState.setTooltipBounds(x, y, tooltipWidth, tooltipHeight);
        TooltipScrollState.setTooltipVisible(true);

        // 渲染自定义 Tooltip
        renderCustomTooltipSafe(guiGraphics, x, y, limitedTooltipLines, font, diexvFont, mc, needsScrollbar, totalEffectLines);

        // 渲染结束后，保持 tooltipVisible 为 true 直到下一帧（由下一帧的渲染决定是否清除）
        // 注意：不能在这里立即设为 false，否则滚轮事件可能收不到。下一帧如果没有 tooltip 会被覆盖。
        // 但为了避免残留，可以在下一帧开始前（onRenderTooltipInternal 开头）设置 false。
        // 为简单起见，我们不在本方法末尾清除，而是在下一次进入时清除（见方法开头）。
        // 但为了保险，在方法最后将 visible 保留，由下一次进入时覆盖。
    }

    // 在方法开头清除 visible 状态（如果本次没有 tooltip 会设为 false，有则覆盖为 true）
    // 我们可以添加一个预处理：在 onRenderTooltipInternal 开头先将 visible 设为 false，后面有 tooltip 再设为 true。
    // 但注意：该方法可能在多帧中连续调用，请勿在开头直接设为 false，否则会导致上一帧的 visible 被清除。
    // 更好的办法：不在开头清除，而是依赖每次渲染时 setTooltipVisible(true)。如果没有新的 tooltip，那么上一个 tooltip 消失后，
    // 下一次进入该方法时会因为没有 customTooltipLines 而执行 setTooltipVisible(false)。
    // 但为了在 tooltip 消失后立即清除，我们可以在 "if (customTooltipLines == null)" 处调用 setTooltipVisible(false)。
    // 已经做了。所以无需额外操作。

    @Unique
    private List<Component> buildPotionEnchantTooltip(ItemStack stack, Font font) {
        List<PotionEnchantData> enchantments = PotionEnchantManager.getPotionEnchantments(stack);
        if (enchantments.isEmpty()) {
            return null;
        }

        List<Component> lines = new ArrayList<>();

        for (PotionEnchantData enchant : enchantments) {
            int color = enchant.getColor();

            String effectName = enchant.getEffect().getDisplayName().getString();
            int level = enchant.getAmplifier() + 1;
            String levelText = String.valueOf(level);

            // 使用 Component style 设置颜色（比 §x 嵌入更可靠）
            Component line = Component.literal(effectName + " " + levelText)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF)));

            lines.add(line);
        }

        return lines;
    }

    @Unique
    private int calculateTooltipX(int mouseX, int tooltipWidth, int screenWidth) {
        final int MARGIN = 4;
        final int OFFSET = 12;

        int x = mouseX - tooltipWidth - OFFSET;

        if (x < MARGIN) {
            x = mouseX + OFFSET;
        }

        if (x + tooltipWidth > screenWidth - MARGIN) {
            int leftSpace = mouseX - MARGIN;
            int rightSpace = screenWidth - mouseX - MARGIN;

            if (leftSpace >= rightSpace && leftSpace >= tooltipWidth) {
                x = mouseX - tooltipWidth - OFFSET;
            } else if (rightSpace >= tooltipWidth) {
                x = mouseX + OFFSET;
            } else {
                x = leftSpace >= rightSpace ? MARGIN : screenWidth - tooltipWidth - MARGIN;
            }
        }

        return Math.max(MARGIN, Math.min(x, screenWidth - tooltipWidth - MARGIN));
    }

    @Unique
    private int calculateTooltipY(int mouseY, int tooltipHeight, int screenHeight) {
        final int MARGIN = 4;

        int y = mouseY;

        if (y + tooltipHeight > screenHeight - MARGIN) {
            y = screenHeight - tooltipHeight - MARGIN;
        }

        if (y < MARGIN) {
            y = MARGIN;
        }

        return y;
    }

    @Unique
    private int calculateMaxWidth(List<Component> lines, Font font, DiexvFont diexvFont) {
        int maxWidth = 0;
        for (Component line : lines) {
            int width = font.width(line);
            maxWidth = Math.max(maxWidth, width);
        }
        return maxWidth;
    }

    @Unique
    private void renderCustomTooltipSafe(GuiGraphics guiGraphics, int x, int y, List<Component> lines, Font font, DiexvFont diexvFont, Minecraft mc, boolean needsScrollbar, int totalEffectLines) {
        if (lines.isEmpty()) return;

        int maxWidth = calculateMaxWidth(lines, font, diexvFont);
        int tooltipWidth = maxWidth + 8;
        int tooltipHeight = lines.size() * 10 + 8;

        if (needsScrollbar) {
            tooltipWidth += SCROLLBAR_WIDTH + SCROLLBAR_MARGIN * 2;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);

        // 绘制白色边框
        guiGraphics.fill(x - 3, y - 3 + 1, x - 3 + 1, y + tooltipHeight + 3 - 1, WHITE_COLOR);
        guiGraphics.fill(x + tooltipWidth + 2, y - 3 + 1, x + tooltipWidth + 3, y + tooltipHeight + 3 - 1, WHITE_COLOR);
        guiGraphics.fill(x - 3, y - 3, x + tooltipWidth + 3, y - 3 + 1, WHITE_COLOR);
        guiGraphics.fill(x - 3, y + tooltipHeight + 2, x + tooltipWidth + 3, y + tooltipHeight + 3, WHITE_COLOR);

        // 绘制文本
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int textY = y + 4 + i * 10;
            guiGraphics.drawString(font, line, x + 4, textY, TOOLTIP_TEXT_COLOR);
        }

        // 绘制滚动条
        if (needsScrollbar) {
            renderScrollbar(guiGraphics, x, y, tooltipWidth, tooltipHeight, totalEffectLines);
        }

        guiGraphics.pose().popPose();
    }

    @Unique
    private void renderScrollbar(GuiGraphics guiGraphics, int x, int y, int tooltipWidth, int tooltipHeight, int totalLines) {
        int scrollbarX = x + tooltipWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        int scrollbarY = y + 2;
        int scrollbarHeight = tooltipHeight - 4;

        // 计算滚动条滑块高度
        float visibleRatio = (float) MAX_VISIBLE_EFFECTS / totalLines;
        int thumbHeight = Math.max(10, (int) (scrollbarHeight * visibleRatio));

        // 计算滚动条滑块位置（使用 TooltipScrollState 的状态）
        int maxOffset = totalLines - MAX_VISIBLE_EFFECTS;
        float scrollRatio = maxOffset > 0 ? (float) TooltipScrollState.getCurrentScrollOffset() / maxOffset : 0;
        int thumbY = scrollbarY + (int) ((scrollbarHeight - thumbHeight) * scrollRatio);

        // 绘制滚动条背景（深灰色）
        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF555555);

        // 绘制滚动条滑块（根据是否拖动改变颜色）
        int thumbColor = TooltipScrollState.isDraggingScrollbar() ? 0xFFCCCCCC : 0xFFAAAAAA;
        guiGraphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, thumbColor);
    }

    /**
     * 处理滚动条拖动（使用 TooltipScrollState 的状态）
     */
    @Unique
    private void handleScrollbarDrag(Minecraft mc, int tooltipX, int tooltipY, int tooltipWidth, int tooltipHeight) {
        long window = mc.getWindow().getWindow();
        boolean isLeftMouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (isLeftMouseDown) {
            double mouseX = mc.mouseHandler.xpos();
            double mouseY = mc.mouseHandler.ypos();
            double scale = mc.getWindow().getGuiScale();
            int guiMouseX = (int) (mouseX / scale);
            int guiMouseY = (int) (mouseY / scale);

            // 计算滚动条区域
            int scrollbarX = tooltipX + tooltipWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
            int scrollbarY = tooltipY + 2;
            int scrollbarHeight = tooltipHeight - 4;

            // 检查鼠标是否在滚动条区域内
            boolean mouseOnScrollbar = guiMouseX >= scrollbarX && guiMouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                    guiMouseY >= scrollbarY && guiMouseY <= scrollbarY + scrollbarHeight;

            if (mouseOnScrollbar || TooltipScrollState.isDraggingScrollbar()) {
                // 计算新的滚动偏移量
                int maxOffset = totalEffectLines - MAX_VISIBLE_EFFECTS;
                float scrollRatio = Math.max(0, Math.min(1,
                        (float) (guiMouseY - scrollbarY - 5) / (scrollbarHeight - 10)));
                int newOffset = (int) (scrollRatio * maxOffset);
                newOffset = Math.max(0, Math.min(newOffset, maxOffset));

                TooltipScrollState.setDraggingScrollbar(true);
                TooltipScrollState.setCurrentScrollOffset(newOffset);
                return;
            }
        }

        // 鼠标未按下时，清除拖动状态
        TooltipScrollState.setDraggingScrollbar(false);
    }

    /**
     * 限制tooltip行数（使用 TooltipScrollState 的状态）
     */
    @Unique
    private List<Component> limitTooltipLines(List<Component> lines) {
        if (lines.isEmpty()) {
            totalEffectLines = 0;
            TooltipScrollState.setTotalEffectLines(0);
            return lines;
        }

        totalEffectLines = lines.size();
        TooltipScrollState.setTotalEffectLines(totalEffectLines);

        if (totalEffectLines > MAX_VISIBLE_EFFECTS) {
            int maxOffset = totalEffectLines - MAX_VISIBLE_EFFECTS;
            // 使用 TooltipScrollState 的滚动偏移量
            int currentOffset = TooltipScrollState.getCurrentScrollOffset();
            currentOffset = Math.min(currentOffset, maxOffset);
            TooltipScrollState.setCurrentScrollOffset(currentOffset);

            int startEffect = currentOffset;
            int endEffect = Math.min(startEffect + MAX_VISIBLE_EFFECTS, lines.size());

            List<Component> limitedLines = new ArrayList<>();
            for (int i = startEffect; i < endEffect; i++) {
                limitedLines.add(lines.get(i));
            }

            // 添加当前位置/总数提示（类似HUD格式）
            String countHint = "§7" + (startEffect + 1) + "-" + endEffect + "/" + totalEffectLines;
            limitedLines.add(Component.literal(countHint));
            // 添加操作提示
            String scrollHint = "§7Shift+滚轮";
            limitedLines.add(Component.literal(scrollHint));
            return limitedLines;
        } else {
            return lines;
        }
    }
}