package net.diexv.potionenchant.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class GuiZoom {

    /** 右侧面板缩放等级（滑动条 + editBox） */
    public float level = 1.0f;
    /** 顶部标题栏独立缩放等级（headerEditBox） */
    public float headerLevel = 1.0f;
    public boolean dragging;
    public EditBox editBox;
    public EditBox headerEditBox;
    private boolean updatingFromText;

    private static final float MIN = 0.1f, MAX = 5.0f;
    private static final int PANEL = 32;

    public void init(Font font, int screenW, int screenH) {
        // 右侧面板缩放输入框
        editBox = new EditBox(font, screenW - PANEL + 3, 30, PANEL - 6, 14,
            Component.translatable("gui.potionenchant.zoom"));
        editBox.setMaxLength(4);
        editBox.setValue((int)(level * 100) + "%");
        editBox.setResponder(s -> {
            if (updatingFromText) return;
            try { float v = Float.parseFloat(s.replace("%", "")); level = clamp(v / 100f); }
            catch (NumberFormatException ignored) {}
        });

        // 顶部标题栏独立缩放输入框——与面板缩放完全独立
        headerEditBox = new EditBox(font, 0, 0, 40, 14,
            Component.translatable("gui.potionenchant.zoom"));
        headerEditBox.setMaxLength(4);
        headerEditBox.setValue((int)(headerLevel * 100) + "%");
        headerEditBox.setResponder(s -> {
            if (updatingFromText) return;
            try { float v = Float.parseFloat(s.replace("%", "")); headerLevel = clamp(v / 100f); }
            catch (NumberFormatException ignored) {}
        });
    }

    private float clamp(float z) { return Math.max(MIN, Math.min(MAX, z)); }

    // ---- 面板缩放（影响 push/pop 内容） ----
    public double mx(double mouseX, int w) { return (mouseX - w / 2.0) / level + w / 2.0; }
    public double my(double mouseY, int h) { return mouseY / level; }

    public void push(GuiGraphics g, int w, int h) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(w / 2.0, 0, 0);
        pose.scale(level, level, 1);
        pose.translate(-w / 2.0, 0, 0);
    }

    public void pop(GuiGraphics g) { g.pose().popPose(); }

    // ---- 标题栏缩放（独立于面板） ----
    public double headerMx(double mouseX, int w) { return (mouseX - w / 2.0) / headerLevel + w / 2.0; }
    public double headerMy(double mouseY, int h) { return mouseY / headerLevel; }

    public void pushHeader(GuiGraphics g, int w, int h) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(w / 2.0, 0, 0);
        pose.scale(headerLevel, headerLevel, 1);
        pose.translate(-w / 2.0, 0, 0);
    }

    public void popHeader(GuiGraphics g) { g.pose().popPose(); }

    // ---- 面板滑动条 ----
    public void scroll(double delta) {
        level = clamp(level + (float)(delta * 0.05f));
        syncPanelEditBox();
    }

    public void renderPanel(GuiGraphics g, Font font, int mx, int my, int w, int h) {
        int zx = w - PANEL;
        g.fill(zx, 0, w, h, 0x90000000);
        g.drawCenteredString(font, Component.translatable("gui.potionenchant.zoom"), zx + PANEL / 2, 12, 0xCCCCCC);

        int sbX = zx + 6, sbY = 50, sbW = PANEL - 12, sbH = h - 70;
        g.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x40FFFFFF);

        float ratio = (level - MIN) / (MAX - MIN);
        int th = Math.max(25, sbH / 10);
        int ty = sbY + (int)((sbH - th) * ratio);
        boolean hover = mx >= sbX && mx <= sbX + sbW && my >= ty && my <= ty + th;
        g.fill(sbX, ty, sbX + sbW, ty + th, hover ? 0xFFAAAAFF : 0xFF6666AA);
    }

    public void updateFromMouse(double my, int h) {
        int sbY = 50, sbH = h - 70, th = Math.max(25, sbH / 10);
        float ratio = (float)Math.max(0, Math.min(1, (my - sbY - th / 2.0) / (sbH - th)));
        level = clamp(MIN + ratio * (MAX - MIN));
        syncPanelEditBox();
    }

    // ---- 内部同步（仅面板 EditBox，不影响 headerEditBox） ----
    private void syncPanelEditBox() {
        updatingFromText = true;
        if (editBox != null) editBox.setValue((int)(level * 100) + "%");
        updatingFromText = false;
    }

    /** 同步 headerEditBox 显示（仅在外部修改 headerLevel 时调用） */
    public void syncHeaderEditBox() {
        updatingFromText = true;
        if (headerEditBox != null) headerEditBox.setValue((int)(headerLevel * 100) + "%");
        updatingFromText = false;
    }

    /** 在非缩放的标题栏区域渲染独立的缩放输入框 */
    public void renderHeaderZoom(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY, float pt) {
        headerEditBox.setX(x);
        headerEditBox.setY(y);
        headerEditBox.setWidth(w);
        headerEditBox.setHeight(14);
        headerEditBox.render(g, mouseX, mouseY, pt);
    }
}
