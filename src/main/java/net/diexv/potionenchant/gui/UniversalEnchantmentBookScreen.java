package net.diexv.potionenchant.gui;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.network.EnchantBookPacketHandler;
import net.diexv.potionenchant.gui.GuiZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class UniversalEnchantmentBookScreen extends Screen {

    private final ItemStack targetItem;
    private final ItemStack bookItem;
    private EditBox searchBox;
    private final GuiZoom zoom = new GuiZoom("universal_enchantment_book");
    private Button confirmButton, cancelButton;
    private Map<Enchantment, Integer> levelAdjustments = new HashMap<>();
    private EditBox levelEditBox;
    private Enchantment editingEnchant;
    private List<EnchantInfo> allEnchants, filteredEnchants;
    private EnchantInfo selectedEnchant;
    private int scrollOffset;
    private static final int MAX_VISIBLE = 10;
    private int descScrollOffset;
    private boolean isDescDragging, isDragging;
    private boolean showSingleEffectMode = true;
    private CategoryBar categoryBar;

    public UniversalEnchantmentBookScreen(ItemStack targetItem, ItemStack bookItem) {
        super(Component.translatable("gui.potionenchant.universal_enchantment_book"));
        this.targetItem = targetItem;
        this.bookItem = bookItem;
        this.allEnchants = new ArrayList<>();
        this.filteredEnchants = new ArrayList<>();
    }

    @Override
    protected void init() {
        loadAllEnchantments();
        zoom.init(font, width, height);
        int sw = 200;
        searchBox = new EditBox(font, (width - sw) / 2, 30, sw, 20, Component.translatable("gui.potionenchant.search_enchant"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(s -> updateFilter());
        addRenderableWidget(searchBox);
        confirmButton = Button.builder(Component.translatable("gui.potionenchant.confirm"), b -> onConfirm())
            .bounds((width - 200) / 2 - 55, height - 40, 100, 20).build();
        confirmButton.active = false;
        cancelButton = Button.builder(Component.translatable("gui.potionenchant.cancel"), b -> onClose())
            .bounds((width - 200) / 2 + 55, height - 40, 100, 20).build();
        addRenderableWidget(confirmButton);
        addRenderableWidget(cancelButton);
        levelEditBox = new EditBox(font, 0, 0, 30, 14, Component.translatable("gui.potionenchant.level_input"));
        levelEditBox.setMaxLength(10);
        levelEditBox.setVisible(false);
        levelEditBox.setResponder(this::onLevelInputChanged);
        // levelEditBox is rendered manually inside zoom - do not add as renderable widget
        categoryBar = new CategoryBar("gui.potionenchant.category",
            new String[]{"all", "weapon", "armor", "tool", "curse"}, 50, 16, 4);
        categoryBar.init(Math.max(2, (width - 450) / 2), 55);
        updateFilter();
    }

    private int getLevelAdjustment(Enchantment e) { return levelAdjustments.getOrDefault(e, getExistingLevel(e)); }
    private int getExistingLevel(Enchantment e) { return EnchantmentHelper.getItemEnchantmentLevel(e, targetItem); }
    private void setLevelAdjustment(Enchantment e, int lv) {
        int ex = getExistingLevel(e);
        if (lv == ex) levelAdjustments.remove(e);
        else levelAdjustments.put(e, lv);
        updateConfirmButton();
    }

    private int getTotalXpCost() {
        int totalXp = 0;
        int cpl = 1000;
        try { if (PotionEnchantConfig.COMMON != null && PotionEnchantConfig.COMMON.enchantBookXpCost != null) cpl = PotionEnchantConfig.COMMON.enchantBookXpCost.get(); } catch (Exception ignored) {}
        for (var e : levelAdjustments.entrySet()) {
            int tgt = e.getValue(), ex = getExistingLevel(e.getKey());
            if (tgt > ex) totalXp += Math.max(1, (tgt - ex) * cpl);
        }
        return totalXp;
    }

    private boolean hasEnoughXp() {
        if (minecraft == null || minecraft.player == null) return false;
        if (minecraft.player.isCreative()) return true;
        int cost = getTotalXpCost();
        return cost <= 0 || minecraft.player.totalExperience >= cost;
    }

    private void updateConfirmButton() {
        confirmButton.active = !levelAdjustments.isEmpty() && hasEnoughXp();
    }
    private boolean canInc(Enchantment e, int cur) {
        boolean ab = false;
        try { if (PotionEnchantConfig.COMMON != null && PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap != null) ab = PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap.get(); } catch (Exception ignored) {}
        return ab ? cur < Integer.MAX_VALUE : cur < Math.max(e.getMaxLevel(), 1);
    }
    private void onLevelInputChanged(String t) {
        if (editingEnchant == null || t.isEmpty()) return;
        try {
            int lv = Integer.parseInt(t);
            if (lv < 0) { lv = 0; levelEditBox.setValue("0"); }
            boolean ab = false;
            try { if (PotionEnchantConfig.COMMON != null && PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap != null) ab = PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap.get(); } catch (Exception ignored) {}
            if (!ab) {
                int cap = Math.max(editingEnchant.getMaxLevel(), 1);
                if (lv > cap) { lv = cap; levelEditBox.setValue(String.valueOf(cap)); }
            }
            setLevelAdjustment(editingEnchant, lv);
        } catch (NumberFormatException ignored) {}
    }

    private void loadAllEnchantments() {
        allEnchants.clear();
        boolean ab = false;
        try { if (PotionEnchantConfig.COMMON != null && PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap != null) ab = PotionEnchantConfig.COMMON.allowEnchantLevelBeyondCap.get(); } catch (Exception ignored) {}
        for (Enchantment e : ForgeRegistries.ENCHANTMENTS) {
            if (e == null) continue;
            allEnchants.add(new EnchantInfo(e, getExistingLevel(e), ab ? Integer.MAX_VALUE : Math.max(e.getMaxLevel(), 1)));
        }
        allEnchants.sort(Comparator.comparing(e -> e.enchantment.getFullname(1).getString()));
    }

    private void updateFilter() {
        String q = searchBox.getValue().toLowerCase().trim();
        if (q.isEmpty()) {
            filteredEnchants = new ArrayList<>();
            for (EnchantInfo ei : allEnchants) if (matchesCategory(ei)) filteredEnchants.add(ei);
        } else {
            filteredEnchants = new ArrayList<>();
            for (EnchantInfo ei : allEnchants) {
                if (!matchesCategory(ei)) continue;
                String n = ei.enchantment.getFullname(1).getString().toLowerCase();
                String id = ForgeRegistries.ENCHANTMENTS.getKey(ei.enchantment).toString().toLowerCase();
                if (n.contains(q) || id.contains(q)) filteredEnchants.add(ei);
            }
        }
        scrollOffset = Math.min(scrollOffset, Math.max(0, filteredEnchants.size() - MAX_VISIBLE));
        selectedEnchant = null;
        showSingleEffectMode = false;
        descScrollOffset = 0;
    }

    private boolean matchesCategory(EnchantInfo ei) {
        if (categoryBar == null) return true;
        String f = categoryBar.getFilter();
        if (f.equals("all")) return true;
        EnchantmentCategory ec = ei.enchantment.category;
        switch (f) {
            case "weapon": return ec == EnchantmentCategory.WEAPON || ec == EnchantmentCategory.BOW || ec == EnchantmentCategory.CROSSBOW || ec == EnchantmentCategory.TRIDENT;
            case "armor": return ec == EnchantmentCategory.ARMOR || ec == EnchantmentCategory.ARMOR_FEET || ec == EnchantmentCategory.ARMOR_LEGS || ec == EnchantmentCategory.ARMOR_CHEST || ec == EnchantmentCategory.ARMOR_HEAD || ec == EnchantmentCategory.WEARABLE;
            case "tool": return !(ec == EnchantmentCategory.WEAPON || ec == EnchantmentCategory.BOW || ec == EnchantmentCategory.CROSSBOW || ec == EnchantmentCategory.TRIDENT || ec == EnchantmentCategory.ARMOR || ec == EnchantmentCategory.ARMOR_FEET || ec == EnchantmentCategory.ARMOR_LEGS || ec == EnchantmentCategory.ARMOR_CHEST || ec == EnchantmentCategory.ARMOR_HEAD || ec == EnchantmentCategory.WEARABLE);
            case "curse": return ei.enchantment.isCurse();
        }
        return true;
    }

    private void onConfirm() {
        if (levelAdjustments.isEmpty() || minecraft == null || minecraft.player == null) return;
        for (var e : levelAdjustments.entrySet()) {
            int ex = getExistingLevel(e.getKey());
            if (e.getValue() == ex) continue;
            EnchantBookPacketHandler.INSTANCE.sendToServer(
                new EnchantBookPacketHandler.ApplyEnchantPacket(ForgeRegistries.ENCHANTMENTS.getKey(e.getKey()).toString(), e.getValue(), -1));
        }
        onClose();
    }

    private int listX() { return Math.max(2, (width - 450) / 2); }
    private int listW() { return 200; }
    private int listY() { return 70 + (categoryBar != null ? categoryBar.getHeight() : 0); }
    private int listH() { return MAX_VISIBLE * 20 + 5; }
    private int descX() { return listX() + listW() + 20; }
    private int descY() { return 70 + (categoryBar != null ? categoryBar.getHeight() : 0); }
    private int descW() { return width - descX() - listX(); }
    private int descH() { return listH(); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int scrMX = mx, scrMY = my;
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        zoom.renderHeaderZoom(g, font, width / 2 + 60, 6, 50, scrMX, scrMY, pt);
        g.drawString(font, Component.translatable("gui.potionenchant.target_item", targetItem.getHoverName().getString()), descX(), descY() - 15, 0xAAAAAA);
        if (categoryBar != null) categoryBar.render(g, font, mx, my, width, height);
        zoom.push(g, width, height);
        mx = (int) zoom.mx(mx, width);
        my = (int) zoom.my(my, height);
        renderList(g, mx, my, pt);
        if (showSingleEffectMode && selectedEnchant != null) renderSingle(g);
        else renderBatch(g);
        zoom.pop(g);
        zoom.renderPanel(g, font, scrMX, scrMY, width, height);
        zoom.editBox.render(g, scrMX, scrMY, pt);
    }

    private void renderList(GuiGraphics g, int mx, int my, float pt) {
        int lx = listX(), ly = listY(), lw = listW(), lh = listH();
        g.fill(lx, ly, lx + lw, ly + lh, 0x80000000);
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredEnchants.size(); i++) {
            int idx = i + scrollOffset;
            EnchantInfo ei = filteredEnchants.get(idx);
            int y = ly + 5 + i * 20, existing = ei.existingLevel, target = getLevelAdjustment(ei.enchantment);
            if (ei == selectedEnchant) g.fill(lx + 2, y - 2, lx + lw - 2, y + 18, 0x40FFFFFF);
            boolean rh = mx >= lx && mx <= lx + lw - 50 && my >= y && my <= y + 20;
            if (rh) g.fill(lx, y, lx + lw - 50, y + 20, 0x20FFFFFF);
            String name = ei.enchantment.getFullname(1).getString();
            if (font.width(name) > lw - 60) name = font.plainSubstrByWidth(name, lw - 70) + "...";
            g.drawString(font, name, lx + 5, y, 0xFFFFFF);
            int mxb = lx + lw - 50, myb = y + 2, bs = 14;
            boolean mh = mx >= mxb && mx <= mxb + bs && my >= myb && my <= myb + bs;
            g.fill(mxb, myb, mxb + bs, myb + bs, target > 0 ? 0xCC3333 : 0x666666);
            if (mh) drawBorder(g, mxb, myb, bs);
            g.drawString(font, "-", mxb + 5, myb + 3, 0xFFFFFF);
            int ix = lx + lw - 33, iy = y + 2;
            if (editingEnchant == ei.enchantment && levelEditBox.isVisible()) {
                levelEditBox.setX(ix); levelEditBox.setY(iy); levelEditBox.setWidth(18); levelEditBox.setHeight(14);
                levelEditBox.render(g, mx, my, pt);
            } else if (target > 0) {
                int c = target > existing ? 0xFFFF55 : target < existing ? 0xFF5555 : 0xAAAAAA;
                g.drawString(font, String.valueOf(target), ix + 2, y + 3, c);
            }
            int px = lx + lw - 12, py = y + 2;
            boolean ph = mx >= px && mx <= px + bs && my >= py && my <= py + bs;
            g.fill(px, py, px + bs, py + bs, canInc(ei.enchantment, target) ? 0x33AA33 : 0x666666);
            if (ph) drawBorder(g, px, py, bs);
            g.drawString(font, "+", px + 4, py + 3, 0xFFFFFF);
        }
        if (filteredEnchants.size() > MAX_VISIBLE) {
            int sX = lx + lw + 2, sH = lh;
            int tH = Math.max(20, sH * MAX_VISIBLE / filteredEnchants.size());
            int tY = listY() + (sH - tH) * scrollOffset / (filteredEnchants.size() - MAX_VISIBLE);
            g.fill(sX, listY(), sX + 8, listY() + sH, 0x40000000);
            g.fill(sX, tY, sX + 8, tY + tH, 0xFFFFFFFF);
        }
    }

    private void renderSingle(GuiGraphics g) {
        int px = descX(), py = descY(), pw = descW(), ph = descH();
        g.fill(px, py, px + pw, py + ph, 0x88000000);
        if (selectedEnchant == null) return;
        EnchantInfo ei = selectedEnchant;
        int y = py + 5;
        g.drawString(font, ei.enchantment.getFullname(1), px + 5, y, 0xFFD700); y += 16;
        String id = ForgeRegistries.ENCHANTMENTS.getKey(ei.enchantment).toString();
        if (font.width(id) > pw - 12) id = font.plainSubstrByWidth(id, pw - 25) + "...";
        g.drawString(font, id, px + 5, y, 0x777777); y += 14;
        g.fill(px + 5, y, px + pw - 5, y + 1, 0x55555555); y += 8;
        g.drawString(font, Component.translatable("gui.potionenchant.enchant_book.max_level").getString() + ": " + ei.maxLevel, px + 5, y, 0xCCCCCC); y += 14;
        g.drawString(font, Component.translatable("gui.potionenchant.enchant_book.current_level").getString() + ": " + ei.existingLevel, px + 5, y, ei.existingLevel > 0 ? 0x55FF55 : 0xCCCCCC); y += 14;
        int tgt = getLevelAdjustment(ei.enchantment);
        g.drawString(font, Component.translatable("gui.potionenchant.enchant_book.target_level").getString() + ": " + (tgt > 0 ? String.valueOf(tgt) : Component.translatable("gui.potionenchant.no_adjustments").getString()), px + 5, y, tgt > 0 ? 0x55FFFF : 0xCCCCCC); y += 14;
        if (tgt > 0) {
            int cpl = 1000;
            try { if (PotionEnchantConfig.COMMON != null && PotionEnchantConfig.COMMON.enchantBookXpCost != null) cpl = PotionEnchantConfig.COMMON.enchantBookXpCost.get(); } catch (Exception ignored) {}
            int cost = Math.max(1, (tgt - ei.existingLevel) * cpl);
            g.drawString(font, Component.translatable("gui.potionenchant.enchant_book.xp_cost").getString() + ": " + fmt(cost), px + 5, y, 0x55FFFF); y += 14;
            if (Minecraft.getInstance().player != null) g.drawString(font, Component.translatable("gui.potionenchant.enchant_book.total_xp").getString() + ": " + fmt(Minecraft.getInstance().player.totalExperience), px + 5, y, 0xAAAAAA);
        }
        int descStartY = y;
        String descKey = ei.enchantment.getDescriptionId() + ".desc";
        String descStr = Component.translatable(descKey).getString();
        if (!descStr.equals(descKey)) {
            g.fill(px + 5, descStartY, px + pw - 5, descStartY + 1, 0x55555555);
            descStartY += 6;
            List<String> descLines = wrapTextByWidth(descStr, pw - 14);
            for (String dl : descLines) {
                if (descStartY + 10 > py + ph - 22) break;
                g.drawString(font, dl, px + 5, descStartY, 0xAAAAAA);
                descStartY += 10;
            }
        }
        int cy = Math.max(descStartY + 6, py + ph - 18);
        g.fill(px + 5, cy, px + pw - 5, cy + 1, 0x55555555);
        if (!ei.enchantment.canEnchant(targetItem)) g.drawString(font, Component.translatable("gui.potionenchant.enchant_book.cannot_apply"), px + 5, cy + 5, 0xFF5555);
        else {
            boolean comp = true;
            for (Enchantment ex : EnchantmentHelper.getEnchantments(targetItem).keySet())
                if (ex != ei.enchantment && !ei.enchantment.isCompatibleWith(ex)) { comp = false; break; }
            g.drawString(font, comp ? Component.translatable("gui.potionenchant.enchant_book.compatible") : Component.translatable("gui.potionenchant.enchant_book.incompatible_hint"), px + 5, cy + 5, comp ? 0x55FF55 : 0xFFAA00);
        }
    }

    private void renderBatch(GuiGraphics g) {
        int px = descX(), py = descY(), pw = descW(), ph = descH();

        Map<Enchantment, Integer> cur = EnchantmentHelper.getEnchantments(targetItem);
        List<String> lines = new ArrayList<>();
        int totalXp = 0;
        int cpl = 1000;
        try { if (PotionEnchantConfig.COMMON != null && PotionEnchantConfig.COMMON.enchantBookXpCost != null) cpl = PotionEnchantConfig.COMMON.enchantBookXpCost.get(); } catch (Exception ignored) {}

        if (levelAdjustments.isEmpty() && !cur.isEmpty()) {
            for (var e : cur.entrySet())
                lines.add(String.format("§e%s: Lv.%d", e.getKey().getFullname(1).getString(), e.getValue()));
        } else if (!levelAdjustments.isEmpty()) {
            for (var e : levelAdjustments.entrySet()) {
                int tgt = e.getValue(), ex = cur.getOrDefault(e.getKey(), 0);
                if (tgt == ex) continue;
                String n = e.getKey().getFullname(1).getString();
                if (tgt > ex) {
                    int cost = (tgt - ex) * cpl;
                    totalXp += cost;
                    if (ex > 0)
                        lines.add(String.format("%s: §eLv.%d§r → §aLv.%d§r (§a+%s XP§r)", n, ex, tgt, fmt(cost)));
                    else
                        lines.add(String.format("%s: §aLv.%d§r (§a+%s XP§r)", n, tgt, fmt(cost)));
                } else if (tgt <= 0) {
                    lines.add(String.format("%s: §eLv.%d§r → §c%s§r", n, ex,
                        Component.translatable("gui.potionenchant.removed").getString()));
                } else {
                    lines.add(String.format("%s: §eLv.%d§r → §eLv.%d§r", n, ex, tgt));
                }
            }
        }

        // Calculate actual content height to avoid oversized background
        int maxVisible = (ph - 35) / 10;
        int totalLines = lines.size();
        int visible = Math.min(maxVisible, totalLines);
        int contentH = visible > 0 ? 20 + visible * 10 + 15 : 30;
        if (!levelAdjustments.isEmpty() && totalXp > 0) contentH += 12;
        contentH = Math.min(contentH, ph);

        g.fill(px, py, px + pw, py + contentH, 0x80000000);
        g.drawCenteredString(font, Component.translatable("gui.potionenchant.batch_stats").getString(), px + pw / 2, py + 5, 0xFFFF55);

        if (lines.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.potionenchant.no_enchant_stats").getString(),
                px + pw / 2, py + ph / 2 - 5, 0x888888);
            return;
        }

        int maxScroll = Math.max(0, totalLines - maxVisible);
        descScrollOffset = Math.min(descScrollOffset, maxScroll);

        for (int i = 0; i < maxVisible && (i + descScrollOffset) < totalLines; i++) {
            String line = lines.get(i + descScrollOffset);
            if (font.width(line) > pw - 15)
                line = font.plainSubstrByWidth(line, pw - 25) + "...";
            g.drawString(font, line, px + 5, py + 20 + i * 10, 0xFFFFFF);
        }

        if (!levelAdjustments.isEmpty() && totalXp > 0)
            g.drawString(font, Component.translatable("gui.potionenchant.total_xp_cost", fmt(totalXp)).getString(),
                px + 5, py + contentH - 12, 0xFFFF55);

        if (totalLines > maxVisible) {
            int scrollbarX = px + pw - 8;
            int scrollbarY = py + 18;
            int scrollbarWidth = 6;
            int scrollbarHeight = contentH - 35;
            int thumbHeight = Math.max(15, scrollbarHeight * maxVisible / totalLines);
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * descScrollOffset / maxScroll;
            g.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0x40000000);
            g.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFFFFFFF);
        }
    }

    private void drawBorder(GuiGraphics g, int x, int y, int s) {
        g.fill(x - 1, y - 1, x + s + 1, y, 0xFFFFFFFF);
        g.fill(x - 1, y + s, x + s + 1, y + s + 1, 0xFFFFFFFF);
        g.fill(x - 1, y, x, y + s, 0xFFFFFFFF);
        g.fill(x + s, y, x + s + 1, y + s, 0xFFFFFFFF);
    }

    private static String fmt(int v) {
        if (v >= 1000000) return String.format("%.1fM", v / 1000000.0);
        if (v >= 1000) return String.format("%.1fK", v / 1000.0);
        return String.valueOf(v);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx >= width - 32) {
            if (zoom.editBox.isMouseOver(mx, my)) { setFocused(zoom.editBox); return zoom.editBox.mouseClicked(mx, my, btn); }
            if (my >= 50 && my <= height - 20) { zoom.dragging = true; zoom.updateFromMouse(my, height); return true; }
            return true;
        }
        if (zoom.headerEditBox.isMouseOver(mx, my)) {
            setFocused(zoom.headerEditBox); return zoom.headerEditBox.mouseClicked(mx, my, btn);
        }
        double origMX = mx, origMY = my;
        mx = zoom.mx(mx, width);
        my = zoom.my(my, height);
        if (levelEditBox.isVisible() && !levelEditBox.isMouseOver(mx, my)) { levelEditBox.setVisible(false); editingEnchant = null; }
        if (categoryBar != null && categoryBar.mouseClicked(origMX, origMY)) { scrollOffset = 0; updateFilter(); return true; }
        int lx = listX(), ly = listY(), lw = listW(), lh = listH();
        if (filteredEnchants.size() > MAX_VISIBLE) {
            int sX = lx + lw + 2, sH = lh, tH = Math.max(20, sH * MAX_VISIBLE / filteredEnchants.size()), tY = ly + (sH - tH) * scrollOffset / (filteredEnchants.size() - MAX_VISIBLE);
            if (mx >= sX && mx <= sX + 8 && my >= tY && my <= tY + tH) { isDragging = true; return true; }
            if (mx >= sX && mx <= sX + 8 && my >= ly && my <= ly + sH) {
                double r = (my - ly - tH / 2.0) / (sH - tH);
                scrollOffset = (int)(Math.max(0, Math.min(1, r)) * (filteredEnchants.size() - MAX_VISIBLE));
                return true;
            }
        }
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredEnchants.size(); i++) {
            int idx = i + scrollOffset;
            EnchantInfo ei = filteredEnchants.get(idx);
            int y = ly + 5 + i * 20, mxb = lx + lw - 50, myb = y + 2, px = lx + lw - 12, py = y + 2, bs = 14;
            if (mx >= mxb && mx <= mxb + bs && my >= myb && my <= myb + bs) {
                int t = getLevelAdjustment(ei.enchantment);
                if (t > 0) setLevelAdjustment(ei.enchantment, t - 1);
                showSingleEffectMode = false; descScrollOffset = 0;
                levelEditBox.setVisible(false); editingEnchant = null;
                return true;
            }
            int ix = lx + lw - 33;
            if (mx >= ix && mx <= ix + 18 && my >= myb && my <= myb + 14) {
                editingEnchant = ei.enchantment;
                levelEditBox.setValue(String.valueOf(getLevelAdjustment(ei.enchantment)));
                levelEditBox.setX(ix); levelEditBox.setY(myb); levelEditBox.setWidth(18); levelEditBox.setHeight(14);
                levelEditBox.setVisible(true); levelEditBox.setFocused(true);
                showSingleEffectMode = false; descScrollOffset = 0;
                return true;
            }
            if (mx >= px && mx <= px + bs && my >= py && my <= py + bs) {
                int t = getLevelAdjustment(ei.enchantment);
                if (canInc(ei.enchantment, t)) setLevelAdjustment(ei.enchantment, t + 1);
                showSingleEffectMode = false; descScrollOffset = 0;
                levelEditBox.setVisible(false); editingEnchant = null;
                return true;
            }
        }
        if (mx >= lx && mx < lx + lw - 50 && my >= ly && my <= ly + lh) {
            int idx = (int)((my - ly - 5) / 20) + scrollOffset;
            if (idx >= 0 && idx < filteredEnchants.size()) {
                selectedEnchant = filteredEnchants.get(idx);
                showSingleEffectMode = true; descScrollOffset = 0;
                return true;
            }
        }
        int px = descX(), py = descY(), pw = descW(), ph = descH();
        if (!showSingleEffectMode) {
            List<String> sl = buildStatsLines();
            int mv = (ph - 35) / 10, tl = sl.size();
            if (tl > mv) {
                int sH = ph - 35, ms = tl - mv, tH = Math.max(15, sH * mv / tl), tY = py + 18 + (sH - tH) * descScrollOffset / ms;
                if (mx >= px + pw - 8 && mx <= px + pw - 2 && my >= tY && my <= tY + tH) { isDescDragging = true; return true; }
                if (mx >= px + pw - 8 && mx <= px + pw - 2 && my >= py + 18 && my <= py + 18 + sH) {
                    double r = (my - py - 18 - tH / 2.0) / (sH - tH);
                    descScrollOffset = (int)(Math.max(0, Math.min(1, r)) * ms);
                    return true;
                }
            }
        }
        return super.mouseClicked(origMX, origMY, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        zoom.dragging = false;
        double origMX = mx, origMY = my;
        mx = zoom.mx(mx, width);
        my = zoom.my(my, height);
        isDragging = false; isDescDragging = false;
        if (categoryBar != null) categoryBar.mouseReleased();
        return super.mouseReleased(origMX, origMY, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (zoom.dragging) { zoom.updateFromMouse(my, height); return true; }
        double origMX = mx, origMY = my;
        mx = zoom.mx(mx, width);
        my = zoom.my(my, height);
        if (categoryBar != null && categoryBar.mouseDragged(origMX)) return true;
        if (isDragging) {
            int lh = listH(), tH = Math.max(20, lh * MAX_VISIBLE / filteredEnchants.size()), rng = filteredEnchants.size() - MAX_VISIBLE;
            if (rng > 0) { double ra = (my - listY() - tH / 2.0) / (lh - tH); scrollOffset = (int)(Math.max(0, Math.min(1, ra)) * rng); }
            return true;
        }
        if (isDescDragging && !showSingleEffectMode) {
            int ph = descH();
            List<String> sl = buildStatsLines();
            int mv = (ph - 35) / 10, ms = Math.max(0, sl.size() - mv);
            if (ms > 0) { int sH = ph - 35, tH = Math.max(15, sH * mv / sl.size()); double ra = (my - descY() - 18 - tH / 2.0) / (sH - tH); descScrollOffset = (int)(Math.max(0, Math.min(1, ra)) * ms); }
            return true;
        }
        return super.mouseDragged(origMX, origMY, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= width - 32) { zoom.scroll(delta); return true; }
        double origMX = mx, origMY = my;
        mx = zoom.mx(mx, width);
        my = zoom.my(my, height);
        if (categoryBar != null && categoryBar.mouseScrolled(origMX, origMY, delta)) return true;
        int lx = listX(), ly = listY(), lw = listW(), lh = listH();
        if (mx >= lx && mx <= lx + lw && my >= ly && my <= ly + lh) {
            scrollOffset = (int)Math.max(0, Math.min(scrollOffset - delta, Math.max(0, filteredEnchants.size() - MAX_VISIBLE)));
            return true;
        }
        int px = descX(), py = descY(), pw = descW(), ph = descH();
        if (mx >= px && mx <= px + pw && my >= py && my <= py + ph) {
            int st = buildStatsLines().size(), mv = (ph - 35) / 10;
            descScrollOffset = (int)Math.max(0, Math.min(descScrollOffset - delta, Math.max(0, st - mv)));
            return true;
        }
        return super.mouseScrolled(origMX, origMY, delta);
    }

    private List<String> buildStatsLines() {
        List<String> l = new ArrayList<>();
        Map<Enchantment, Integer> cur = EnchantmentHelper.getEnchantments(targetItem);
        if (levelAdjustments.isEmpty() && !cur.isEmpty()) {
            for (var e : cur.entrySet())
                l.add(String.format("§e%s: Lv.%d", e.getKey().getFullname(1).getString(), e.getValue()));
        } else {
            for (var e : levelAdjustments.entrySet()) {
                int t = e.getValue(), ex = cur.getOrDefault(e.getKey(), 0);
                if (t == ex) continue;
                String n = e.getKey().getFullname(1).getString();
                if (t > ex)
                    l.add(String.format("%s: §eLv.%d§r → §aLv.%d§r", n, ex, t));
                else if (t <= 0)
                    l.add(String.format("%s: §eLv.%d§r → §c%s§r", n, ex,
                        Component.translatable("gui.potionenchant.removed").getString()));
                else
                    l.add(String.format("%s: §eLv.%d§r → §eLv.%d§r", n, ex, t));
            }
        }
        return l;
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        if (zoom.editBox.isFocused()) return zoom.editBox.keyPressed(kc, sc, mod);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.keyPressed(kc, sc, mod);
        if (levelEditBox.isVisible() && levelEditBox.isFocused()) {
            if (kc == 256 || kc == 257 || kc == 335) { levelEditBox.setVisible(false); editingEnchant = null; return true; }
            return levelEditBox.keyPressed(kc, sc, mod);
        }
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public boolean charTyped(char cp, int mod) {
        if (zoom.editBox.isFocused()) return zoom.editBox.charTyped(cp, mod);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.charTyped(cp, mod);
        if (levelEditBox.isVisible() && levelEditBox.isFocused()) return levelEditBox.charTyped(cp, mod);
        return super.charTyped(cp, mod);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        zoom.saveToConfig();
        super.onClose();
    }
    @Override
    public void removed() { zoom.saveToConfig(); super.removed(); }

    private List<String> wrapTextByWidth(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) { lines.add(""); return lines; }
        String[] paragraphs = text.split("\\\\n");
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) { lines.add(""); continue; }
            StringBuilder currentLine = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char c = paragraph.charAt(i);
                String testLine = currentLine.toString() + c;
                if (font.width(testLine) <= maxWidth) {
                    currentLine.append(c);
                } else {
                    if (currentLine.length() > 0) { lines.add(currentLine.toString()); currentLine.setLength(0); }
                    if (font.width(String.valueOf(c)) <= maxWidth) currentLine.append(c);
                    else lines.add(String.valueOf(c));
                }
            }
            if (currentLine.length() > 0) lines.add(currentLine.toString());
        }
        return lines;
    }

    private static class EnchantInfo {
        final Enchantment enchantment;
        final int existingLevel, maxLevel;
        EnchantInfo(Enchantment e, int ex, int mx) { enchantment = e; existingLevel = ex; maxLevel = mx; }
    }
}


