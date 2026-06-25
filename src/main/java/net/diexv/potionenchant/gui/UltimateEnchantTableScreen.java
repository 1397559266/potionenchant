package net.diexv.potionenchant.gui;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.network.UltimateTableNetwork;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.stream.Collectors;

public class UltimateEnchantTableScreen extends Screen {
    private enum Mode { POTION, ENCHANT }
    private Mode currentMode = Mode.POTION;
    private final BlockPos blockPos;
    private final Player player;
    private final GuiZoom zoom = new GuiZoom("ultimate_enchant_table");
    private EditBox searchBox;
    private CategoryBar categoryBar, enchantCategoryBar;
    private record InvItem(ItemStack stack, int invIndex, EquipmentSlot equipSlot) {}
    private List<InvItem> invItems = new ArrayList<>();
    private int invScroll = 0;
    private ItemStack targetItem = ItemStack.EMPTY;
    private record EffectInfo(MobEffect effect, String name, boolean beneficial, boolean harmful, String effectId) {}
    private List<EffectInfo> allEffects = new ArrayList<>(), filteredEffects = new ArrayList<>();
    private EffectInfo selectedEffect;
    private Map<MobEffect, Integer> levelAdjustments = new HashMap<>();
    private int potionScroll = 0, potionDescScroll = 0;
    private EditBox potionLevelBox;
    private record EnchantInfo(Enchantment enchantment, String name) {}
    private List<EnchantInfo> allEnchants = new ArrayList<>(), filteredEnchants = new ArrayList<>();
    private EnchantInfo selectedEnchant;
    private Map<Enchantment, Integer> enchantLevelAdjustments = new HashMap<>();
    private int enchantScroll = 0, enchantDescScroll = 0;
    private EditBox enchantLevelBox;
    private boolean levelEditActive = false;
    private static final int GUI_W = 460;
    private int listX, listY, listW, listH, statsY, statsH, searchY, modeBtnY, categoryY;
    private static final int MAX_VISIBLE = 5;
    private int rightX, rightY, rightW, rightH;
    private String tooltipText = "";
    private int tooltipTimer = 0;

    public UltimateEnchantTableScreen(BlockPos pos) {
        super(Component.translatable("container.potionenchant.ultimate_enchant_table"));
        this.blockPos = pos; this.player = Minecraft.getInstance().player;
    }

    @Override
    protected void init() {
        super.init(); loadAllEffects(); loadAllEnchants(); zoom.init(font, width, height);
        listX = Math.max(2, (width - GUI_W) / 2); listW = 200; listH = MAX_VISIBLE * 22 + 5;
        searchY = 30; modeBtnY = 55; categoryY = 75; listY = categoryY + 22;
        statsY = listY + listH + 5; statsH = 100;
        rightX = listX + listW + 15; rightW = width - rightX - listX; rightY = 55; rightH = height - rightY - 50;
        searchBox = new EditBox(font, listX, searchY, listW, 20, Component.translatable("gui.potionenchant.search"));
        searchBox.setMaxLength(50); searchBox.setResponder(this::onSearchChanged); addRenderableWidget(searchBox);
        categoryBar = new CategoryBar("gui.potionenchant.category", new String[]{"all","beneficial","harmful","neutral"}, listW / 4, 14, 4);
        categoryBar.init(listX, categoryY);
        enchantCategoryBar = new CategoryBar("gui.potionenchant.category", new String[]{"all","weapon","armor","tool","curse"}, listW / 5, 14, 5);
        enchantCategoryBar.init(listX, categoryY);
        potionLevelBox = new EditBox(font, listX + 5, statsY + 18, 50, 14, Component.translatable("gui.potionenchant.search"));
        potionLevelBox.setMaxLength(5); potionLevelBox.setResponder(this::onPotionLevelChanged); potionLevelBox.setVisible(false);
        enchantLevelBox = new EditBox(font, listX + 5, statsY + 18, 50, 14, Component.translatable("gui.potionenchant.search"));
        enchantLevelBox.setMaxLength(5); enchantLevelBox.setResponder(this::onEnchantLevelChanged); enchantLevelBox.setVisible(false);
        loadInventory(); updateList();
    }

    private void loadInventory() {
        invItems.clear(); if (player == null) return;
        for (int i = 0; i < 36; i++) { ItemStack s = player.getInventory().getItem(i); if (!s.isEmpty()) invItems.add(new InvItem(s.copy(), i, null)); }
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot); if (!s.isEmpty()) invItems.add(new InvItem(s.copy(), -1, slot));
        }
        ItemStack oh = player.getOffhandItem(); if (!oh.isEmpty()) invItems.add(new InvItem(oh.copy(), -1, EquipmentSlot.OFFHAND));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g); int smx = mx, smy = my;
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, getTitle().getString(), width / 2, 10, 0xFFFFFF);
        zoom.renderHeaderZoom(g, font, width / 2 + 60, 6, 50, mx, my, pt);
        g.drawString(font,
            Component.translatable(currentMode == Mode.POTION ? "gui.potionenchant.search" : "gui.potionenchant.search_enchant"),
            listX, searchY - 12, 0xCCCCCC);
        renderModeButtons(g, mx, my);
        CategoryBar activeBar = currentMode == Mode.POTION ? categoryBar : enchantCategoryBar;
        if (activeBar != null) activeBar.render(g, font, mx, my, width, height);
        zoom.push(g, width, height); mx = (int) zoom.mx(mx, width); my = (int) zoom.my(my, height);
        g.fill(listX, listY, listX + listW, listY + listH, 0x80000000);
        g.fill(listX, statsY, listX + listW, statsY + statsH, 0x80000000);
        if (currentMode == Mode.POTION) { renderEffectList(g, mx, my); renderEffectStats(g, mx, my, pt); }
        else { renderEnchantList(g, mx, my); renderEnchantStats(g, mx, my, pt); }
        g.fill(rightX, rightY, rightX + rightW, rightY + rightH, 0x80000000);
        if (player != null) {
            g.drawString(font, Component.translatable("gui.potionenchant.current_xp", getTotalXp(player)).getString(), rightX + 6, rightY + 4, 0x80FF80);
        }
        g.drawString(font, Component.translatable("gui.potionenchant.target_item"), rightX + 6, rightY + 14, 0xAAAAAA);
        int tgtY = rightY + 28;
        g.fill(rightX + 6, tgtY, rightX + 30, tgtY + 24, 0x40000000);
        g.renderOutline(rightX + 6, tgtY, 24, 24, 0xFF444444);
        if (!targetItem.isEmpty()) {
            g.renderItem(targetItem, rightX + 7, tgtY + 1);
            String name = targetItem.getHoverName().getString();
            if (font.width(name) > rightW - 50) name = font.plainSubstrByWidth(name, rightW - 53) + "...";
            g.drawString(font, name, rightX + 36, tgtY + 8, 0xFFFFFF);
        }
        int cell = 18, cols = Math.min(9, (rightW - 16) / cell);
        int gridX = rightX + 6, gridY = tgtY + 30;
        int visRows = (rightY + rightH - gridY) / cell;
        for (int i = invScroll * cols, idx = 0; idx < visRows * cols && i < invItems.size(); i++, idx++) {
            int ix = idx % cols, iy = idx / cols;
            int sx = gridX + ix * cell, sy = gridY + iy * cell;
            g.fill(sx, sy, sx + cell, sy + cell, 0x80000000);
            g.renderOutline(sx, sy, cell, cell, 0xFF444444);
            InvItem item = invItems.get(i);
            if (item != null) {
                boolean isSelected = !targetItem.isEmpty() && ItemStack.isSameItemSameTags(item.stack, targetItem);
                if (isSelected) g.fill(sx, sy, sx + cell, sy + cell, 0x40FFAA00);
                g.renderItem(item.stack, sx + 1, sy + 1);
                if (isSelected) g.renderOutline(sx, sy, cell, cell, 0xFFFFAA00);
                if (mx >= sx && mx < sx + cell && my >= sy && my < sy + cell) g.renderTooltip(font, item.stack, mx, my);
            }
        }
        if ((invItems.size() + cols - 1) / cols > visRows) {
            int totalRows = (invItems.size() + cols - 1) / cols;
            int th = Math.max(15, visRows * cell * visRows / totalRows);
            int ty = gridY + (visRows * cell - th) * invScroll / (totalRows - visRows);
            g.fill(rightX + rightW - 6, gridY, rightX + rightW - 2, gridY + visRows * cell, 0x40000000);
            g.fill(rightX + rightW - 6, ty, rightX + rightW - 2, ty + th, 0xFFFFFFFF);
        }
        int btnY = height - 40;
        int totalXpCost = getTotalCost();
        boolean canAfford = player != null && (player.isCreative() || getTotalXp(player) >= totalXpCost);
        boolean hasSel = (currentMode == Mode.POTION && selectedEffect != null && levelAdjustments.containsKey(selectedEffect.effect))
            || (currentMode == Mode.ENCHANT && selectedEnchant != null && enchantLevelAdjustments.containsKey(selectedEnchant.enchantment));
        boolean canConfirm = !targetItem.isEmpty() && hasSel && canAfford && totalXpCost > 0;
        int btnW = 100, btnGap = 10, cx = width / 2 - btnW - btnGap / 2;
        g.fill(cx, btnY, cx + btnW, btnY + 20, canConfirm ? 0xFF226622 : 0xFF444444);
        g.drawCenteredString(font, Component.translatable("gui.potionenchant.confirm").getString(), cx + btnW / 2, btnY + 6, canConfirm ? 0xFFFFFF : 0xAAAAAA);
        int cx2 = width / 2 + btnGap / 2;
        g.fill(cx2, btnY, cx2 + btnW, btnY + 20, 0xFF555555);
        g.drawCenteredString(font, Component.translatable("gui.potionenchant.cancel").getString(), cx2 + btnW / 2, btnY + 6, 0xAAAAAA);
        if (player != null && totalXpCost > 0) {
            String info = Component.translatable("gui.potionenchant.current_xp", getTotalXp(player)).getString() + " | " + Component.translatable("gui.potionenchant.cost_xp", totalXpCost).getString();
            g.drawCenteredString(font, info, width / 2, btnY + 24, canAfford ? 0x80FF80 : 0xFF6060);
        }
        if (tooltipTimer > 0 && !tooltipText.isEmpty()) {
            int tw = font.width(tooltipText);
            int tx = (width - tw) / 2, ty2 = height / 2 - 30;
            g.fill(tx - 4, ty2 - 2, tx + tw + 4, ty2 + 12, 0xCC000000);
            g.drawString(font, tooltipText, tx, ty2, 0xFFFFAA);
            tooltipTimer--;
        }
        zoom.pop(g); zoom.renderPanel(g, font, smx, smy, width, height); zoom.editBox.render(g, smx, smy, pt);
    }

    private void renderModeButtons(GuiGraphics g, int mx, int my) {
        int btnW = 80, gap = 4, startX = listX;
        for (int i = 0; i < 2; i++) {
            int x = startX + i * (btnW + gap);
            boolean sel = (i == 0 && currentMode == Mode.POTION) || (i == 1 && currentMode == Mode.ENCHANT);
            boolean hover = mx >= x && mx < x + btnW && my >= modeBtnY && my < modeBtnY + 18;
            g.fill(x, modeBtnY, x + btnW, modeBtnY + 18, sel ? 0xFF3366AA : (hover ? 0xFF444444 : 0xFF333333));
            String t = Component.translatable(i == 0 ? "gui.potionenchant.mode_potion" : "gui.potionenchant.mode_enchant").getString();
            g.drawCenteredString(font, t, x + btnW / 2, modeBtnY + 5, sel ? 0xFFFFFF : 0x888888);
        }
    }

    private void loadAllEffects() {
        allEffects.clear();
        java.util.Set<ResourceLocation> blacklisted = PotionEnchantConfig.getBlacklistedEffects();
        ForgeRegistries.MOB_EFFECTS.getValues().forEach(e -> {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(e);
            if (id != null && blacklisted.contains(id)) return;
            String idStr = id != null ? id.toString() : "";
            allEffects.add(new EffectInfo(e, e.getDisplayName().getString(), e.isBeneficial(),
                e.getCategory() != MobEffectCategory.NEUTRAL && !e.isBeneficial(), idStr));
        });
        allEffects.sort(Comparator.comparing(a -> a.name)); filteredEffects = new ArrayList<>(allEffects);
    }

    private void renderEffectList(GuiGraphics g, int mx, int my) {
        int bs = 14, rowH = 22;
        for (int i = potionScroll; i < filteredEffects.size() && i < potionScroll + MAX_VISIBLE; i++) {
            var info = filteredEffects.get(i);
            int y = listY + (i - potionScroll) * rowH;
            int curLv = levelAdjustments.getOrDefault(info.effect, getExistingEffectLevel(info.effect));
            // Minus button (rightmost, red)
            int minusX = listX + listW - bs - 6;
            boolean mh = mx >= minusX && mx <= minusX + bs && my >= y + 4 && my <= y + 4 + bs;
            g.fill(minusX, y + 4, minusX + bs, y + 4 + bs, curLv > 0 ? 0xFFFF5555 : 0xFF666666);
            g.drawCenteredString(font, "-", minusX + bs / 2, y + 6, 0xFFFFFF);
            // Plus button (left of minus, green)
            int plusX = minusX - bs - 2;
            int maxLevelCfg = PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get(); boolean canInc = curLv < maxLevelCfg;
            boolean ph = mx >= plusX && mx <= plusX + bs && my >= y + 4 && my <= y + 4 + bs;
            g.fill(plusX, y + 4, plusX + bs, y + 4 + bs, canInc ? 0xFF55FF55 : 0xFF666666);
            g.drawCenteredString(font, "+", plusX + bs / 2, y + 6, 0xFFFFFF);
            // Level text between name and + button
            String lvStr = String.valueOf(curLv);
            // Edit box or level text
            if (info == selectedEffect && levelEditActive && potionLevelBox != null) {
                potionLevelBox.setVisible(true);
                potionLevelBox.setX(plusX - 44); potionLevelBox.setY(y + 2);
                potionLevelBox.setWidth(40); potionLevelBox.setHeight(14);
                potionLevelBox.render(g, mx, my, 0);
            } else {
                g.drawString(font, lvStr, plusX - font.width(lvStr) - 4, y + 5, 0xFFCCFFCC);
            }
            // Name (left, truncated)
            int nameMaxW = plusX - 50 - listX;
            String d = info.name;
            if (font.width(d) > nameMaxW) d = font.plainSubstrByWidth(d, Math.max(10, nameMaxW - 5)) + "...";
            boolean sel = selectedEffect == info;
            boolean hover = mx >= listX && mx < plusX && my >= y && my < y + rowH;
            if (sel) g.fill(listX + 2, y, plusX, y + rowH, 0x40FFAA00);
            else if (hover) g.fill(listX + 2, y, plusX, y + rowH, 0x30FFFFFF);
            if (sel) g.fill(listX + 2, y, listX + 4, y + rowH, 0xFFFFAA00);
            g.drawString(font, d, listX + 6, y + 6, sel ? 0xFFDD55 : (hover ? 0xFFFF55 : 0xFFFFFF));
            // 悬停显示效果ID
            if (hover && !info.effectId.isEmpty()) {
                g.drawString(font, info.effectId, listX + 6, y + rowH - 3, 0x80808080);
            }
        }
        if (filteredEffects.size() > MAX_VISIBLE) {
            int th = Math.max(15, listH * MAX_VISIBLE / filteredEffects.size());
            int ty = listY + (listH - th) * potionScroll / (filteredEffects.size() - MAX_VISIBLE);
            g.fill(listX + listW - 4, listY, listX + listW, listY + listH, 0x40000000);
            g.fill(listX + listW - 4, ty, listX + listW, ty + th, 0xFFFFFFFF);
        }
    }

    private void renderEffectStats(GuiGraphics g, int mx, int my, float pt) {
        java.util.List<String> slines = getPotionStatLines();
        if (slines.isEmpty()) {
            g.drawCenteredString(font,
                Component.translatable("gui.potionenchant.no_stats").getString(), listX + listW / 2, statsY + statsH / 2 - 5, 0x888888);
            return;
        }
        int maxVis = (statsH - 25) / 10;
        int maxSc = Math.max(0, slines.size() - maxVis);
        if (potionDescScroll > maxSc) potionDescScroll = maxSc;
        for (int li = potionDescScroll; li < slines.size() && li < potionDescScroll + maxVis; li++) {
            g.drawString(font, slines.get(li), listX + 4, statsY + 4 + (li - potionDescScroll) * 10, 0xCCCCCC);
        }
        if (slines.size() > maxVis) {
            int th = Math.max(10, (statsH - 25) * maxVis / slines.size());
            int ty = statsY + 4 + (statsH - 25 - th) * potionDescScroll / (slines.size() - maxVis);
            g.fill(listX + listW - 4, statsY + 4, listX + listW, statsY + 4 + (statsH - 25), 0x40000000);
            g.fill(listX + listW - 4, ty, listX + listW, ty + th, 0xFFFFFFFF);
        }
    }


    private void loadAllEnchants() {
        allEnchants.clear();
        ForgeRegistries.ENCHANTMENTS.getValues().forEach(e -> allEnchants.add(new EnchantInfo(e, Component.translatable(e.getDescriptionId()).getString())));
        allEnchants.sort(Comparator.comparing(a -> a.name)); filteredEnchants = new ArrayList<>(allEnchants);
    }

    private void renderEnchantList(GuiGraphics g, int mx, int my) {
        int bs = 14, rowH = 22;
        for (int i = enchantScroll; i < filteredEnchants.size() && i < enchantScroll + MAX_VISIBLE; i++) {
            var info = filteredEnchants.get(i);
            int y = listY + (i - enchantScroll) * rowH;
            int curLv = enchantLevelAdjustments.getOrDefault(info.enchantment, getExistingEnchantLevel(info.enchantment));
            int maxLv = info.enchantment.getMaxLevel();
            // Minus button (rightmost, red)
            int minusX = listX + listW - bs - 6;
            boolean mh = mx >= minusX && mx <= minusX + bs && my >= y + 4 && my <= y + 4 + bs;
            g.fill(minusX, y + 4, minusX + bs, y + 4 + bs, curLv > 0 ? 0xFFFF5555 : 0xFF666666);
            g.drawCenteredString(font, "-", minusX + bs / 2, y + 6, 0xFFFFFF);
            // Plus button (left of minus, green)
            int plusX = minusX - bs - 2;
            boolean canInc = curLv < maxLv;
            boolean ph = mx >= plusX && mx <= plusX + bs && my >= y + 4 && my <= y + 4 + bs;
            g.fill(plusX, y + 4, plusX + bs, y + 4 + bs, canInc ? 0xFF55FF55 : 0xFF666666);
            g.drawCenteredString(font, "+", plusX + bs / 2, y + 6, 0xFFFFFF);
            // Level text between name and + button
            String lvStr = String.valueOf(curLv);
            // Edit box or level text
            if (info == selectedEnchant && levelEditActive && enchantLevelBox != null) {
                enchantLevelBox.setVisible(true);
                enchantLevelBox.setX(plusX - 44); enchantLevelBox.setY(y + 2);
                enchantLevelBox.setWidth(40); enchantLevelBox.setHeight(14);
                enchantLevelBox.render(g, mx, my, 0);
            } else {
                g.drawString(font, lvStr, plusX - font.width(lvStr) - 4, y + 5, 0xFFCCFFCC);
            }
            // Name (left, truncated)
            int nameMaxW = plusX - 50 - listX;
            String d = info.name;
            if (font.width(d) > nameMaxW) d = font.plainSubstrByWidth(d, Math.max(10, nameMaxW - 5)) + "...";
            boolean sel = selectedEnchant == info;
            boolean hover = mx >= listX && mx < plusX && my >= y && my < y + rowH;
            if (sel) g.fill(listX + 2, y, plusX, y + rowH, 0x40FFAA00);
            else if (hover) g.fill(listX + 2, y, plusX, y + rowH, 0x30FFFFFF);
            if (sel) g.fill(listX + 2, y, listX + 4, y + rowH, 0xFFFFAA00);
            g.drawString(font, d, listX + 6, y + 6, sel ? 0xFFDD55 : (hover ? 0xFFFF55 : 0xFFFFFF));
        }
        if (filteredEnchants.size() > MAX_VISIBLE) {
            int th = Math.max(15, listH * MAX_VISIBLE / filteredEnchants.size());
            int ty = listY + (listH - th) * enchantScroll / (filteredEnchants.size() - MAX_VISIBLE);
            g.fill(listX + listW - 4, listY, listX + listW, listY + listH, 0x40000000);
            g.fill(listX + listW - 4, ty, listX + listW, ty + th, 0xFFFFFFFF);
        }
    }

    private void renderEnchantStats(GuiGraphics g, int mx, int my, float pt) {
        java.util.List<String> slines = getEnchantStatLines();
        if (slines.isEmpty()) {
            g.drawCenteredString(font,
                Component.translatable("gui.potionenchant.no_enchant_stats").getString(), listX + listW / 2, statsY + statsH / 2 - 5, 0x888888);
            return;
        }
        int maxVis = (statsH - 25) / 10;
        int maxSc = Math.max(0, slines.size() - maxVis);
        if (enchantDescScroll > maxSc) enchantDescScroll = maxSc;
        for (int li = enchantDescScroll; li < slines.size() && li < enchantDescScroll + maxVis; li++) {
            g.drawString(font, slines.get(li), listX + 4, statsY + 4 + (li - enchantDescScroll) * 10, 0xCCCCCC);
        }
        if (slines.size() > maxVis) {
            int th = Math.max(10, (statsH - 25) * maxVis / slines.size());
            int ty = statsY + 4 + (statsH - 25 - th) * enchantDescScroll / (slines.size() - maxVis);
            g.fill(listX + listW - 4, statsY + 4, listX + listW, statsY + 4 + (statsH - 25), 0x40000000);
            g.fill(listX + listW - 4, ty, listX + listW, ty + th, 0xFFFFFFFF);
        }
    }


    private int getExistingEffectLevel(MobEffect e) {
        if (targetItem.isEmpty()) return 0;
        for (PotionEnchantData ed : PotionEnchantManager.getPotionEnchantments(targetItem))
            if (ed.getEffect() == e) return ed.getAmplifier() + 1;
        return 0;
    }

    private int getExistingEnchantLevel(Enchantment e) {
        if (targetItem.isEmpty()) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(e, targetItem);
    }

    private int getTotalCost() {
        int cp = PotionEnchantConfig.COMMON.ultimateTableXpCostPerLevel.get(); int t = 0;
        if (currentMode == Mode.POTION) {
            for (var e : levelAdjustments.entrySet()) { int d = e.getValue() - getExistingEffectLevel(e.getKey()); if (d > 0) t += d * cp; }
        } else {
            for (var e : enchantLevelAdjustments.entrySet()) { int d = e.getValue() - getExistingEnchantLevel(e.getKey()); if (d > 0) t += d * cp; }
        }
        return t;
    }

    private int getTotalXp(Player p) {
        int l = p.experienceLevel; int t = 0;
        for (int i = 0; i < l; i++) t += xpFor(i);
        t += (int)(p.experienceProgress * xpFor(l)); return t;
    }

    private int xpFor(int l) {
        if (l >= 30) return 112 + (l - 30) * 9;
        if (l >= 15) return 37 + (l - 15) * 5;
        return 7 + l * 2;
    }

    private static String toRoman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n); };
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        // Screen-position elements (zoom panel, zoom drag bar) — use RAW screen coordinates
        if (mx >= width - 32) {
            if (zoom.editBox.isMouseOver(mx, my)) { setFocused(zoom.editBox); return zoom.editBox.mouseClicked(mx, my, btn); }
            if (zoom.headerEditBox.isMouseOver(mx, my)) { setFocused(zoom.headerEditBox); return zoom.headerEditBox.mouseClicked(mx, my, btn); }
            if (my >= 50 && my <= height - 20) { zoom.dragging = true; zoom.updateFromMouse(my, height); return true; }
            return true;
        }
        // Transform to zoom coordinates for zoomed content
        double zmx = zoom.mx(mx, width), zmy = zoom.my(my, height);
        // Screen-position elements (mode buttons, category bar rendered before zoom.push) - use RAW screen coords
        CategoryBar activeBar = (currentMode == Mode.POTION) ? categoryBar : enchantCategoryBar;
        if (activeBar.mouseClicked(mx, my)) { onSearchChanged(searchBox.getValue()); return true; }
        int btnW = 80, gap = 4, startX = listX;
        if (my >= modeBtnY && my < modeBtnY + 18) {
            if (mx >= startX && mx < startX + btnW) { switchMode(Mode.POTION); return true; }
            if (mx >= startX + btnW + gap && mx < startX + btnW * 2 + gap) { switchMode(Mode.ENCHANT); return true; }
        }
        if (zmx >= listX && zmx < listX + listW) {
            if (zmy >= listY && zmy < listY + listH) {
                int rowH = 22;
                int idx = (int)((zmy - listY) / rowH);
                int bs = 14;
                int rowY = listY + idx * rowH;
                // Check +/- button clicks first
                if (currentMode == Mode.POTION) {
                    int ri = potionScroll + idx;
                    if (ri < filteredEffects.size()) {
                        var info = filteredEffects.get(ri);
                        int curLv = levelAdjustments.getOrDefault(info.effect, getExistingEffectLevel(info.effect));
                        int minusX = listX + listW - bs - 6;
                        int plusX = minusX - bs - 2;
                        int maxLevelCfg2 = PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get();
                        // 右键点击复制效果ID
                        if (btn == 1 && zmx >= listX && zmx < plusX && zmy >= rowY && zmy < rowY + rowH) {
                            if (!info.effectId.isEmpty()) {
                                Minecraft.getInstance().keyboardHandler.setClipboard(info.effectId);
                                tooltipText = "\u00a7a" + Component.translatable("gui.potionenchant.copied", info.effectId).getString();
                                tooltipTimer = 80;
                            }
                            return true;
                        }
                        if (zmx >= plusX && zmx <= plusX + bs && zmy >= rowY + 4 && zmy <= rowY + 4 + bs && curLv < maxLevelCfg2) {
                            levelAdjustments.put(info.effect, curLv + 1); return true;
                        }
                        if (zmx >= minusX && zmx <= minusX + bs && zmy >= rowY + 4 && zmy <= rowY + 4 + bs && curLv > 0) {
                            levelAdjustments.put(info.effect, curLv - 1); return true;
                        }
                    }
                } else {
                    int ri = enchantScroll + idx;
                    if (ri < filteredEnchants.size()) {
                        var info = filteredEnchants.get(ri);
                        int curLv = enchantLevelAdjustments.getOrDefault(info.enchantment, getExistingEnchantLevel(info.enchantment));
                        int maxLv = info.enchantment.getMaxLevel();
                        int minusX = listX + listW - bs - 6;
                        int plusX = minusX - bs - 2;
                        // 右键点击复制附魔ID
                        if (btn == 1 && zmx >= listX && zmx < plusX && zmy >= rowY && zmy < rowY + rowH) {
                            ResourceLocation eid = ForgeRegistries.ENCHANTMENTS.getKey(info.enchantment);
                            if (eid != null) {
                                Minecraft.getInstance().keyboardHandler.setClipboard(eid.toString());
                                tooltipText = "\u00a7a" + Component.translatable("gui.potionenchant.copied", eid.toString()).getString();
                                tooltipTimer = 80;
                            }
                            return true;
                        }
                        if (zmx >= plusX && zmx <= plusX + bs && zmy >= rowY + 4 && zmy <= rowY + 4 + bs && curLv < maxLv) {
                            enchantLevelAdjustments.put(info.enchantment, curLv + 1); return true;
                        }
                        if (zmx >= minusX && zmx <= minusX + bs && zmy >= rowY + 4 && zmy <= rowY + 4 + bs && curLv > 0) {
                            enchantLevelAdjustments.put(info.enchantment, curLv - 1); return true;
                        }
                    }
                }
                // Then check name click for selection
                if (currentMode == Mode.POTION) {
                    int ri = potionScroll + idx;
                    if (ri < filteredEffects.size()) {
                        var info = filteredEffects.get(ri);
                        // Click on level text area -> activate edit box
                        int bs2 = 14;
                        int minusX2 = listX + listW - bs2 - 6;
                        int plusX2 = minusX2 - bs2 - 2;
                        int lvRight2 = plusX2 - 4;
                        int lvLeft2 = lvRight2 - 44;
                        if (zmx >= lvLeft2 && zmx <= lvRight2 && zmy >= rowY + 2 && zmy <= rowY + 20) {
                            int clv = levelAdjustments.getOrDefault(info.effect, getExistingEffectLevel(info.effect));
                            potionLevelBox.setValue(String.valueOf(clv));
                            potionLevelBox.setVisible(true);
                            potionLevelBox.setFocused(true);
                            setFocused(potionLevelBox);
                            levelEditActive = true;
                            selectedEffect = info; selectedEnchant = null;
                            enchantLevelBox.setVisible(false);
                            return true;
                        }
                        // Click on name area -> select
                        if (selectedEffect == info) { selectedEffect = null; potionLevelBox.setVisible(false); }
                        else { 
                            selectedEffect = info; selectedEnchant = null; 
                            enchantLevelBox.setVisible(false);
                            int lv = levelAdjustments.getOrDefault(info.effect, getExistingEffectLevel(info.effect));
                            potionLevelBox.setValue(String.valueOf(lv));
                            potionLevelBox.setVisible(true);
                            potionLevelBox.setFocused(false);
                        }
                        return true;
                    }
                } else {
                    int ri = enchantScroll + idx;
                    if (ri < filteredEnchants.size()) {
                        var info = filteredEnchants.get(ri);
                        if (selectedEnchant == info) { selectedEnchant = null; enchantLevelBox.setVisible(false); }
                        else { 
                        // Click on level text area -> activate edit box
                        int bs3 = 14;
                        int minusX3 = listX + listW - bs3 - 6;
                        int plusX3 = minusX3 - bs3 - 2;
                        int lvRight3 = plusX3 - 4;
                        int lvLeft3 = lvRight3 - 44;
                        if (zmx >= lvLeft3 && zmx <= lvRight3 && zmy >= rowY + 2 && zmy <= rowY + 20) {
                            int clv = enchantLevelAdjustments.getOrDefault(info.enchantment, getExistingEnchantLevel(info.enchantment));
                            enchantLevelBox.setValue(String.valueOf(clv));
                            enchantLevelBox.setVisible(true);
                            enchantLevelBox.setFocused(true);
                            setFocused(enchantLevelBox);
                            levelEditActive = true;
                            selectedEnchant = info; selectedEffect = null;
                            potionLevelBox.setVisible(false);
                            return true;
                        }
                        // Click on name area -> select
                            selectedEnchant = info; selectedEffect = null; 
                            potionLevelBox.setVisible(false);
                            int lv = enchantLevelAdjustments.getOrDefault(info.enchantment, getExistingEnchantLevel(info.enchantment));
                            enchantLevelBox.setValue(String.valueOf(lv));
                            enchantLevelBox.setVisible(true);
                            enchantLevelBox.setFocused(false);
                        }
                        return true;
                    }
                }
            }
            // +/- button clicks handled per-row in list section above
            
            // Scrollbar click on left panel
            if (zmx >= listX + listW - 4 && zmx < listX + listW) {
                if (currentMode == Mode.POTION && filteredEffects.size() > MAX_VISIBLE) {
                    int ms = filteredEffects.size() - MAX_VISIBLE;
                    potionScroll = Math.max(0, Math.min(ms, (int)((zmy - listY) / (double)listH * ms))); return true;
                }
                if (currentMode == Mode.ENCHANT && filteredEnchants.size() > MAX_VISIBLE) {
                    int ms = filteredEnchants.size() - MAX_VISIBLE;
                    enchantScroll = Math.max(0, Math.min(ms, (int)((zmy - listY) / (double)listH * ms))); return true;
                }
            }
        }
        // Scrollbar click on stats panel
        if (zmx >= listX + listW - 4 && zmx < listX + listW && zmy >= statsY + 4 && zmy < statsY + 4 + (statsH - 25)) {
            java.util.List<String> slines = currentMode == Mode.POTION ? getPotionStatLines() : getEnchantStatLines();
            int maxVis = (statsH - 25) / 10;
            int ms = Math.max(0, slines.size() - maxVis);
            if (ms > 0) {
                int nv = Math.max(0, Math.min(ms, (int)((zmy - (statsY + 4)) / (double)(statsH - 25) * ms)));
                if (currentMode == Mode.POTION) potionDescScroll = nv;
                else enchantDescScroll = nv;
                return true;
            }
        }
        if (zmx >= rightX && zmx < rightX + rightW && zmy >= rightY && zmy < rightY + rightH) {
            int tgtY = rightY + 28;
            if (zmy >= tgtY && zmy < tgtY + 24 && zmx >= rightX + 6 && zmx < rightX + 30) {
                targetItem = ItemStack.EMPTY; levelAdjustments.clear(); enchantLevelAdjustments.clear(); return true;
            }
            int cell = 18, cols = Math.min(9, (rightW - 16) / cell);
            int gridX = rightX + 6, gridY = tgtY + 30;
            int ix = (int)((zmx - gridX) / cell), iy = (int)((zmy - gridY) / cell);
            if (ix >= 0 && ix < cols && iy >= 0) {
                int li = invScroll * cols + iy * cols + ix;
                if (li >= 0 && li < invItems.size()) {
                    InvItem clicked = invItems.get(li);
                    if (clicked != null && !clicked.stack.isEmpty()) {
                        targetItem = clicked.stack.copy(); levelAdjustments.clear(); enchantLevelAdjustments.clear();
                        selectedEffect = null; selectedEnchant = null; return true;
                    }
                }
            }
        }
        int btnY = height - 40, btnW2 = 100, gap2 = 10, cx = width / 2 - btnW2 - gap2 / 2, cx2 = width / 2 + gap2 / 2;
        if (zmy >= btnY && zmy < btnY + 20) {
            if (zmx >= cx && zmx < cx + btnW2) { onConfirm(); return true; }
            if (zmx >= cx2 && zmx < cx2 + btnW2) { onClose(); return true; }
        }
        // Click outside edit box -> dismiss
        if (levelEditActive) {
            if (potionLevelBox == null || !potionLevelBox.isMouseOver(mx, my)) {
                levelEditActive = false;
                if (potionLevelBox != null) potionLevelBox.setVisible(false);
            }
            if (enchantLevelBox == null || !enchantLevelBox.isMouseOver(mx, my)) {
                levelEditActive = false;
                if (enchantLevelBox != null) enchantLevelBox.setVisible(false);
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void onConfirm() {
        if (targetItem.isEmpty() || player == null) return;
        int totalCost = getTotalCost(); if (totalCost <= 0) return;
        if (!player.isCreative() && getTotalXp(player) < totalCost) return;
        if (currentMode == Mode.POTION) {
            java.util.List<java.util.Map.Entry<MobEffect, Integer>> pending = new java.util.ArrayList<>();
            for (var e : levelAdjustments.entrySet()) {
                int ex = getExistingEffectLevel(e.getKey()); if (e.getValue() == ex) continue;
                pending.add(e);
            }
            if (!pending.isEmpty()) {
                int[] effectIds = new int[pending.size()];
                int[] levels = new int[pending.size()];
                for (int i = 0; i < pending.size(); i++) {
                    effectIds[i] = MobEffect.getId(pending.get(i).getKey());
                    levels[i] = pending.get(i).getValue();
                }
                UltimateTableNetwork.CHANNEL.sendToServer(
                    new UltimateTableNetwork.ApplyPotionBatchPacket(blockPos, targetItem, effectIds, levels, totalCost));
            }
        } else {
            for (var e : enchantLevelAdjustments.entrySet()) {
                int ex = getExistingEnchantLevel(e.getKey()); if (e.getValue() == ex) continue;
                UltimateTableNetwork.CHANNEL.sendToServer(
                    new UltimateTableNetwork.ApplyEnchantPacket(blockPos, targetItem, ForgeRegistries.ENCHANTMENTS.getKey(e.getKey()).toString(), e.getValue(), totalCost));
            }
        }
        levelAdjustments.clear(); enchantLevelAdjustments.clear(); selectedEffect = null; selectedEnchant = null;
    }

    private java.util.List<String> getPotionStatLines() {
        java.util.List<String> slines = new java.util.ArrayList<>();
        if (levelAdjustments.isEmpty() && !targetItem.isEmpty()) {
            var existing = net.diexv.potionenchant.util.PotionEnchantManager.getPotionEnchantments(targetItem);
            for (var ed : existing) {
                String nm = ed.getEffect().getDisplayName().getString();
                slines.add(nm + ": " + net.minecraft.network.chat.Component.translatable("gui.potionenchant.lv").getString() + (ed.getAmplifier() + 1));
            }
        }
        for (var e : levelAdjustments.entrySet()) {
            int ex = getExistingEffectLevel(e.getKey());
            int tg = e.getValue();
            String nm = e.getKey().getDisplayName().getString();
            if (tg > ex) {
                int cost = (tg - ex) * net.diexv.potionenchant.config.PotionEnchantConfig.COMMON.ultimateTableXpCostPerLevel.get();
                slines.add(nm + ": lv" + ex + " -> lv" + tg + " (" + net.minecraft.network.chat.Component.translatable("gui.potionenchant.cost_xp", cost).getString() + ")");
            } else if (tg <= 0) {
                slines.add(nm + ": " + net.minecraft.network.chat.Component.translatable("gui.potionenchant.removed").getString());
            } else {
                slines.add(nm + ": lv" + ex + " -> lv" + tg);
            }
        }
        return slines;
    }

    private java.util.List<String> getEnchantStatLines() {
        java.util.List<String> slines = new java.util.ArrayList<>();
        if (enchantLevelAdjustments.isEmpty() && !targetItem.isEmpty()) {
            var existing = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(targetItem);
            for (var entry : existing.entrySet()) {
                String nm = entry.getKey().getFullname(entry.getValue()).getString();
                slines.add(nm);
            }
        }
        for (var e : enchantLevelAdjustments.entrySet()) {
            String nm = e.getKey().getFullname(1).getString();
            int cost = (e.getValue() - getExistingEnchantLevel(e.getKey())) * net.diexv.potionenchant.config.PotionEnchantConfig.COMMON.ultimateTableXpCostPerLevel.get();
            if (cost > 0)
                slines.add(nm + ": lv" + e.getValue() + " (" + net.minecraft.network.chat.Component.translatable("gui.potionenchant.cost_xp", cost).getString() + ")");
            else
                slines.add(nm + ": lv" + e.getValue());
        }
        return slines;
    }

        private void switchMode(Mode mode) {
        if (currentMode == mode) return;
        currentMode = mode;
        potionScroll = 0; potionDescScroll = 0;
        enchantScroll = 0; enchantDescScroll = 0;
        selectedEffect = null; selectedEnchant = null;
        if (potionLevelBox != null) potionLevelBox.setVisible(false);
        if (enchantLevelBox != null) enchantLevelBox.setVisible(false);
        levelAdjustments.clear(); enchantLevelAdjustments.clear();
        updateList();
        searchBox.setValue("");
    }

    private void updateList() {
        if (currentMode == Mode.POTION) { categoryBar.setVisible(true); enchantCategoryBar.setVisible(false); }
        else { categoryBar.setVisible(false); enchantCategoryBar.setVisible(true); }
        searchBox.setMessage(Component.translatable(
            currentMode == Mode.POTION ? "gui.potionenchant.search" : "gui.potionenchant.search_enchant"));
        onSearchChanged(searchBox.getValue());
    }

    private void onSearchChanged(String s) {
        String lo = s.toLowerCase();
        if (currentMode == Mode.POTION) {
            String cat = categoryBar.getFilter();
            filteredEffects = allEffects.stream().filter(e -> {
                if (!"all".equals(cat)) { if ("beneficial".equals(cat) && !e.beneficial) return false; if ("harmful".equals(cat) && !e.harmful) return false; if ("neutral".equals(cat) && e.beneficial && !e.harmful) return false; }
                return lo.isEmpty() || e.name.toLowerCase().contains(lo);
            }).collect(Collectors.toList()); potionScroll = 0;
        } else {
            String cat = enchantCategoryBar.getFilter();
            filteredEnchants = allEnchants.stream().filter(e -> {
                if (!"all".equals(cat)) {
                    var ec = e.enchantment.category;
                    if ("weapon".equals(cat) && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.WEAPON && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.BOW && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.CROSSBOW && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.TRIDENT) return false;
                    if ("armor".equals(cat) && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_HEAD && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_CHEST && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_LEGS && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_FEET) return false;
                    if ("tool".equals(cat) && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.DIGGER && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.FISHING_ROD && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.BREAKABLE) return false;
                    if ("curse".equals(cat) && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.WEAPON && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_HEAD && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_CHEST && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_LEGS && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_FEET && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.DIGGER && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.FISHING_ROD && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.BREAKABLE && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.BOW && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.CROSSBOW && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.TRIDENT && ec != net.minecraft.world.item.enchantment.EnchantmentCategory.VANISHABLE) return false;
                }
                return lo.isEmpty() || e.name.toLowerCase().contains(lo);
            }).collect(Collectors.toList()); enchantScroll = 0;
        }
    }

    private void onPotionLevelChanged(String t) { if (selectedEffect != null && !t.isEmpty()) { try { int val = Integer.parseInt(t); int maxLv = net.diexv.potionenchant.config.PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get(); val = Math.max(0, Math.min(maxLv, val)); levelAdjustments.put(selectedEffect.effect, val); if (!potionLevelBox.getValue().equals(String.valueOf(val))) potionLevelBox.setValue(String.valueOf(val)); } catch (Exception ignored) {} } }
    private void onEnchantLevelChanged(String t) { if (selectedEnchant != null && !t.isEmpty()) { try { int val = Integer.parseInt(t); int maxLv = selectedEnchant.enchantment.getMaxLevel(); val = Math.max(0, Math.min(maxLv, val)); enchantLevelAdjustments.put(selectedEnchant.enchantment, val); if (!enchantLevelBox.getValue().equals(String.valueOf(val))) enchantLevelBox.setValue(String.valueOf(val)); } catch (Exception ignored) {} } }

    @Override public boolean mouseScrolled(double mx, double my, double d) {
        if (mx >= width - 32) { zoom.scroll(d); return true; }
        double zmx = zoom.mx(mx, width), zmy = zoom.my(my, height);
        int tgtY = rightY + 28;
        if (zmx >= rightX && zmx < rightX + rightW) {
            int cell = 18, cols = Math.min(9, (rightW - 16) / cell), gridY = tgtY + 30;
            int visRows = (rightY + rightH - gridY) / cell, totalRows = (invItems.size() + cols - 1) / cols;
            invScroll = net.minecraft.util.Mth.clamp(invScroll - (int)d, 0, Math.max(0, totalRows - visRows)); return true;
        }
        // Stats panel scroll
        if (zmx >= listX && zmx < listX + listW && zmy >= statsY && zmy < statsY + statsH) {
            java.util.List<String> slines = currentMode == Mode.POTION ? getPotionStatLines() : getEnchantStatLines();
            int maxVis = (statsH - 25) / 10;
            int maxSc = Math.max(0, slines.size() - maxVis);
            if (maxSc > 0) {
                int v = (currentMode == Mode.POTION ? potionDescScroll : enchantDescScroll) + (d > 0 ? -1 : 1);
                int nv = Math.max(0, Math.min(maxSc, v));
                if (currentMode == Mode.POTION) potionDescScroll = nv;
                else enchantDescScroll = nv;
                return true;
            }
        }
        CategoryBar activeBar = (currentMode == Mode.POTION) ? categoryBar : enchantCategoryBar;
        if (activeBar.mouseScrolled(mx, my, d)) return true;
        if (zmx >= listX && zmx < listX + listW && zmy >= listY && zmy < listY + listH) {
            if (currentMode == Mode.POTION && filteredEffects.size() > MAX_VISIBLE) potionScroll = net.minecraft.util.Mth.clamp(potionScroll - (int)d, 0, filteredEffects.size() - MAX_VISIBLE);
            else if (currentMode == Mode.ENCHANT && filteredEnchants.size() > MAX_VISIBLE) enchantScroll = net.minecraft.util.Mth.clamp(enchantScroll - (int)d, 0, filteredEnchants.size() - MAX_VISIBLE);
            return true;
        }
        return super.mouseScrolled(mx, my, d);
    }

    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (zoom.dragging) { zoom.updateFromMouse(my, height); return true; }
        if (currentMode == Mode.POTION ? categoryBar.mouseDragged(mx) : enchantCategoryBar.mouseDragged(mx)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int btn) {
        zoom.dragging = false;
        (currentMode == Mode.POTION ? categoryBar : enchantCategoryBar).mouseReleased();
        return super.mouseReleased(mx, my, btn);
    }
    @Override public boolean keyPressed(int kc, int sc, int mod) {
        if (zoom.editBox.isFocused()) return zoom.editBox.keyPressed(kc, sc, mod);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.keyPressed(kc, sc, mod);
        if (potionLevelBox.isFocused()) return potionLevelBox.keyPressed(kc, sc, mod);
        if (enchantLevelBox.isFocused()) return enchantLevelBox.keyPressed(kc, sc, mod);
        if (searchBox.isFocused()) return searchBox.keyPressed(kc, sc, mod);
        if (kc == 256 || kc == 257 || kc == 335) {
            if (potionLevelBox != null) { potionLevelBox.setFocused(false); potionLevelBox.setVisible(false); }
            if (enchantLevelBox != null) { enchantLevelBox.setFocused(false); enchantLevelBox.setVisible(false); }
            levelEditActive = false;
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }
    @Override public boolean charTyped(char cp, int mod) {
        if (zoom.editBox.isFocused()) return zoom.editBox.charTyped(cp, mod);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.charTyped(cp, mod);
        if (potionLevelBox.isFocused()) return potionLevelBox.charTyped(cp, mod);
        if (enchantLevelBox.isFocused()) return enchantLevelBox.charTyped(cp, mod);
        if (searchBox.isFocused()) return searchBox.charTyped(cp, mod);
        return super.charTyped(cp, mod);
    }
    @Override public void tick() { super.tick(); if (player != null && player.tickCount % 15 == 0) loadInventory(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { zoom.saveToConfig(); if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.closeContainer(); super.onClose(); }
    @Override public void removed() { zoom.saveToConfig(); super.removed(); }
}
