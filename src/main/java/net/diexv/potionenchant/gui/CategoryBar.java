package net.diexv.potionenchant.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

public class CategoryBar {
    private boolean visible = true;
    private final String[] categories;
    private final String prefix;
    private String currentFilter;
    private int scrollOffset;
    private final int[][] btnPositions; // [i][x,y,w,h]
    private int scrollX, scrollY, scrollW, scrollH;
    private boolean dragging;
    private final int btnW, btnH, gap;

    public CategoryBar(String prefix, String[] categories, int btnW, int btnH, int gap) {
        this.prefix = prefix;
        this.categories = categories;
        this.btnW = btnW;
        this.btnH = btnH;
        this.gap = gap;
        this.currentFilter = categories[0];
        this.btnPositions = new int[categories.length][4];
    }

    public void init(int baseX, int baseY) {
        scrollX = baseX;
        scrollY = baseY;
        scrollW = categories.length > 4 ? 4 * btnW + 3 * gap : categories.length * btnW + (categories.length - 1) * gap;
        scrollH = btnH;
        for (int i = 0; i < categories.length; i++) {
            btnPositions[i][0] = scrollX + i * (btnW + gap);
            btnPositions[i][1] = scrollY;
            btnPositions[i][2] = btnW;
            btnPositions[i][3] = btnH;
        }
    }

    public void setVisible(boolean v) { this.visible = v; }
    public boolean isVisible() { return visible; }

    public String getFilter() { return currentFilter; }
    public void setFilter(String f) { currentFilter = f; scrollOffset = 0; }
    public void reset() { currentFilter = categories[0]; scrollOffset = 0; }

    public int getHeight() { return btnH + 4; }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, int screenW, int screenH) {
        g.fill(scrollX, scrollY - 2, scrollX + scrollW, scrollY + scrollH + 2, 0x40000000);

        int totalW = categories.length * (btnW + gap) - gap;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        RenderSystem.enableScissor(
            (int)(scrollX * guiScale),
            (int)((screenH - scrollY - scrollH - 2) * guiScale),
            (int)(scrollW * guiScale),
            (int)((scrollH + 4) * guiScale));

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(-scrollOffset, 0, 0);

        for (int i = 0; i < categories.length; i++) {
            int x = btnPositions[i][0], y = btnPositions[i][1];
            int w = btnPositions[i][2], h = btnPositions[i][3];
            boolean hovered = (mouseX + scrollOffset) >= x && (mouseX + scrollOffset) <= x + w
                && mouseY >= y && mouseY <= y + h;
            boolean selected = currentFilter.equals(categories[i]);
            int color = selected ? 0xFFAA00 : (hovered ? 0x666666 : 0x444444);
            g.fill(x, y, x + w, y + h, color);
            if (selected) {
                g.fill(x - 1, y - 1, x + w + 1, y, 0xFFFFAA00);
                g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFFFFAA00);
                g.fill(x - 1, y, x, y + h, 0xFFFFAA00);
                g.fill(x + w, y, x + w + 1, y + h, 0xFFFFAA00);
            }
            if (hovered) {
                g.fill(x - 1, y - 1, x + w + 1, y, 0xFFFFFFFF);
                g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFFFFFFFF);
                g.fill(x - 1, y, x, y + h, 0xFFFFFFFF);
                g.fill(x + w, y, x + w + 1, y + h, 0xFFFFFFFF);
            }
            String text = I18n.get(prefix + "." + categories[i], categories[i]);
            g.drawCenteredString(font, text, x + w / 2, y + 4, 0xFFFFFF);
        }
        pose.popPose();
        RenderSystem.disableScissor();

        if (totalW > scrollW) {
            int sbW = Math.max(20, scrollW * scrollW / totalW);
            int maxOff = totalW - scrollW;
            int sbX = scrollX + (scrollW - sbW) * scrollOffset / maxOff;
            g.fill(scrollX, scrollY + scrollH + 1, scrollX + scrollW, scrollY + scrollH + 3, 0x30FFFFFF);
            g.fill(sbX, scrollY + scrollH + 1, sbX + sbW, scrollY + scrollH + 3, 0xFFFFFFFF);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (mouseY < scrollY - 2 || mouseY > scrollY + scrollH + 3) return false;
        if (mouseX < scrollX || mouseX > scrollX + scrollW) return false;

        int adjX = (int)mouseX + scrollOffset;
        for (int i = 0; i < categories.length; i++) {
            if (adjX >= btnPositions[i][0] && adjX <= btnPositions[i][0] + btnPositions[i][2]
                && mouseY >= btnPositions[i][1] && mouseY <= btnPositions[i][1] + btnPositions[i][3]) {
                currentFilter = categories[i];
                return true;
            }
        }

        int totalW = categories.length * (btnW + gap) - gap;
        if (totalW > scrollW) {
            int sbW = Math.max(20, scrollW * scrollW / totalW);
            int maxOff = totalW - scrollW;
            int sbX = scrollX + (scrollW - sbW) * scrollOffset / maxOff;
            if (mouseY >= scrollY + scrollH && mouseY <= scrollY + scrollH + 4
                && mouseX >= sbX - 2 && mouseX <= sbX + sbW + 2) {
                dragging = true;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX) {
        if (!dragging) return false;
        int totalW = categories.length * (btnW + gap) - gap;
        if (totalW > scrollW) {
            int maxOff = totalW - scrollW;
            double ratio = (mouseX - scrollX) / scrollW;
            scrollOffset = (int)(Math.max(0, Math.min(1, ratio)) * maxOff);
        }
        return true;
    }

    public void mouseReleased() { dragging = false; }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY < scrollY - 2 || mouseY > scrollY + scrollH + 4) return false;
        if (mouseX < scrollX || mouseX > scrollX + scrollW) return false;
        int totalW = categories.length * (btnW + gap) - gap;
        if (totalW > scrollW) {
            int maxOff = totalW - scrollW;
            scrollOffset = (int)Math.max(0, Math.min(maxOff, scrollOffset - delta * 30));
        }
        return true;
    }

    public boolean isOver(double mouseX, double mouseY) {
        return mouseX >= scrollX && mouseX <= scrollX + scrollW
            && mouseY >= scrollY - 2 && mouseY <= scrollY + scrollH + 4;
    }
}
