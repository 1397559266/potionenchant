package net.diexv.potionenchant.gui;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.network.PotionEnchantTableNetwork;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class PotionEnchantingTableScreen extends Screen {

    private final BlockPos blockPos;
    private final Player player;
    private CategoryBar categoryBar;
    private final PotionOption[] options = new PotionOption[3];
    private int selectedSlot = -1;
    private int power = 0;
    private boolean cannotEnchant = false;
    private int optionRollCounter = 0;

    private record InvItem(ItemStack stack, int invIndex, EquipmentSlot equipSlot) {}
    private List<InvItem> invItems = new ArrayList<>();
    private int invScroll = 0;
    private ItemStack targetItem = ItemStack.EMPTY;

    private int leftX, leftY, leftW, leftH;
    private int rightX, rightY, rightW, rightH;
    private int btnY, confirmX, cancelX;
    private final GuiZoom zoom = new GuiZoom("potion_enchanting_table");

    private static class PotionOption { MobEffect effect; int level; int cost; String clue; }

    public PotionEnchantingTableScreen(BlockPos pos) {
        super(Component.translatable("container.potionenchant.potion_enchanting"));
        this.blockPos = pos;
        this.player = Minecraft.getInstance().player;
        for (int i = 0; i < 3; i++) options[i] = null;
    }

    @Override
    protected void init() {
        super.init();
        zoom.init(font, width, height);
        int panelW = 220;
        int panelH = 200;
        leftX = (width / 2) - panelW - 5;
        leftY = 75;
        leftW = panelW;
        leftH = panelH;
        rightX = width / 2 + 5;
        rightY = 75;
        rightW = panelW;
        rightH = panelH;
        categoryBar = new CategoryBar("gui.potionenchant.category",
            new String[]{"all","beneficial","harmful","neutral"}, leftW / 4, 14, 2);
        categoryBar.init(leftX, leftY - 50);
        btnY = leftY + leftH + 10;
        confirmX = width / 2 + 10;
        cancelX = width / 2 - 10 - 80;
        selectedSlot = -1;
        loadInventory();
        regenerateOptions();
    }

    private void loadInventory() {
        invItems.clear();
        if (player == null) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) invItems.add(new InvItem(stack.copy(), i, null));
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) invItems.add(new InvItem(stack.copy(), -1, slot));
        }
        ItemStack oh = player.getOffhandItem();
        if (!oh.isEmpty()) invItems.add(new InvItem(oh.copy(), -1, EquipmentSlot.OFFHAND));
    }

    private boolean cannotEnchantItem(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getCount() > 1) return true;
        List<PotionEnchantData> existing = PotionEnchantManager.getPotionEnchantments(stack);
        return existing != null && !existing.isEmpty();
    }

    private void regenerateOptions() {
        for (int i = 0; i < 3; i++) options[i] = null;
        selectedSlot = -1;
        optionRollCounter++;
        int pwr = 0;
        if (player != null && player.level() != null) {
            for (var bp : net.minecraft.world.level.block.EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
                var state = player.level().getBlockState(blockPos.offset(bp));
                var block = state.getBlock();
                if (block == net.minecraft.world.level.block.Blocks.CAULDRON
                    || block == net.minecraft.world.level.block.Blocks.WATER_CAULDRON
                    || block == net.minecraft.world.level.block.Blocks.LAVA_CAULDRON
                    || block == net.minecraft.world.level.block.Blocks.POWDER_SNOW_CAULDRON
                    || block == net.minecraft.world.level.block.Blocks.BREWING_STAND) { pwr++; if (pwr >= 3) break; }
            }
        }
        this.power = pwr;
        cannotEnchant = cannotEnchantItem(targetItem);
        if (cannotEnchant) return;

        Random rand = new Random(blockPos.hashCode() * 31L + optionRollCounter * 2654435761L + System.nanoTime() % 1000000);
        List<MobEffect> available = getFilteredEffects();
        if (available.isEmpty()) return;
        for (int slot = 0; slot < 3; slot++) {
            if (pwr < slot + 1) continue;
            int level = slot == 0 ? 1 : (slot == 1 ? (pwr >= 2 ? 2 : 1) : (pwr >= 3 ? 3 : (pwr >= 2 ? 2 : 1)));
            MobEffect effect = available.get(rand.nextInt(available.size()));
            PotionOption opt = new PotionOption();
            opt.effect = effect; opt.level = level; opt.cost = level; opt.clue = getClueText(rand);
            options[slot] = opt;
        }
    }

    private String getClueText(Random rand) {
        String[] clues = {"air earth fire water","ball lightning mind control","scrolls of the elder","the green pill","inside the well","many worlds one","the physical realm","time space reality","wyrm soul bound","death decay void","eternal flame frost","phantom grasp swift","crystal dream night"};
        return clues[rand.nextInt(clues.length)];
    }

    private List<MobEffect> getFilteredEffects() {
        String cat = categoryBar != null ? categoryBar.getFilter() : "all";
        Set<ResourceLocation> blacklisted = PotionEnchantConfig.getBlacklistedEffects();
        return ForgeRegistries.MOB_EFFECTS.getValues().stream().filter(e -> {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(e);
            if (id == null || blacklisted.contains(id)) return false;
            if (!"all".equals(cat)) {
                MobEffectCategory mc = e.getCategory();
                if ("beneficial".equals(cat) && mc != MobEffectCategory.BENEFICIAL) return false;
                if ("harmful".equals(cat) && mc != MobEffectCategory.HARMFUL) return false;
                if ("neutral".equals(cat) && mc != MobEffectCategory.NEUTRAL) return false;
            }
            return true;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int scrMX = mouseX, scrMY = mouseY;
        g.drawCenteredString(font, getTitle().getString(), width / 2, 10, 0xFFFFFF);
        zoom.renderHeaderZoom(g, font, width / 2 + 60, 6, 50, mouseX, mouseY, partialTick);
        categoryBar.render(g, font, mouseX, mouseY, width, height);
        zoom.push(g, width, height);
        mouseX = (int) zoom.mx(mouseX, width);
        mouseY = (int) zoom.my(mouseY, height);
        // LEFT PANEL
        g.fill(leftX, leftY, leftX + leftW, leftY + leftH, 0x80000000);
        String powerText = Component.translatable("gui.potionenchant.power", power).getString();
        g.drawString(font, powerText, leftX + 6, leftY + 4, 0xAAAAAA);
        int slotY = leftY + 18, slotH = (leftH - 22) / 3;
        for (int slot = 0; slot < 3; slot++) {
            PotionOption opt = options[slot];
            int sy = slotY + slot * slotH;
            boolean hover = mouseX >= leftX + 4 && mouseX < leftX + leftW - 4 && mouseY >= sy && mouseY < sy + slotH;
            boolean selected = slot == selectedSlot;
            int bg = selected ? 0x40FFAA00 : (hover && opt != null ? 0x30FFFFFF : 0x10FFFFFF);
            g.fill(leftX + 4, sy, leftX + leftW - 4, sy + slotH, bg);
            if (selected) g.renderOutline(leftX + 4, sy, leftW - 8, slotH, 0xFFFFAA00);
            else if (hover && opt != null) g.renderOutline(leftX + 4, sy, leftW - 8, slotH, 0xAAFFFFFF);
            if (opt != null) {
                if (targetItem.isEmpty()) {
                    g.drawCenteredString(font, Component.translatable("gui.potionenchant.unknown").getString(), leftX + leftW / 2, sy + slotH / 2 - 4, 0x888888);
                } else {
                    String name = opt.effect.getDisplayName().getString();
                    int maxNameW = leftW - 24;
                    if (font.width(name) > maxNameW) name = font.plainSubstrByWidth(name, maxNameW - 4) + "...";
                    g.drawString(font, name, leftX + 10, sy + 6, selected ? 0xFFDD55 : (hover ? 0xFFFF55 : 0xFFFFFF));
                    g.drawString(font, toRoman(opt.level) + "...", leftX + 10, sy + 18, 0xCCCCCC);
                    int clueColor = selected ? 0xBBFFBB : (hover ? 0xAAFFAA : 0x668866);
                    String clue = opt.clue;
                    int maxClueW = leftW - 90;
                    if (font.width(clue) > maxClueW) clue = font.plainSubstrByWidth(clue, maxClueW - 4) + ".";
                    g.drawString(font, clue, leftX + leftW - font.width(clue) - 10, sy + 10, clueColor);
                    boolean canAfford = player.isCreative() || player.experienceLevel >= opt.cost;
                    int xpColor = canAfford ? 0x80FF20 : 0xFF6060;
                    String costStr = Component.translatable("gui.potionenchant.xp_cost", opt.cost).getString();
                    g.drawString(font, costStr, leftX + 10, sy + slotH - 14, xpColor);
                }
            } else if (cannotEnchant) {
                g.drawCenteredString(font, Component.translatable("gui.potionenchant.already_enchanted"), leftX + leftW / 2, sy + slotH / 2 - 4, 0xFF5555);
            } else {
                g.drawCenteredString(font, Component.translatable("gui.potionenchant.slot_locked"), leftX + leftW / 2, sy + slotH / 2 - 6, 0x666666);
                String req = Component.translatable("gui.potionenchant.need_power", slot + 1).getString();
                g.drawCenteredString(font, req, leftX + leftW / 2, sy + slotH / 2 + 8, 0x555555);
            }
        }
        // RIGHT PANEL
        g.fill(rightX, rightY, rightX + rightW, rightY + rightH, 0x80000000);
        g.drawCenteredString(font, Component.translatable("gui.potionenchant.target_item").getString(), rightX + rightW / 2, rightY + 5, 0xFFFF55);
        int tgtY = rightY + 16;
        drawSlot(g, rightX + 6, tgtY + 10, 24, targetItem, mouseX, mouseY, true);
        // Refresh button
        int refreshX = rightX + rightW - 52, refreshY = tgtY + 10;
        boolean canRefresh = !player.isCreative() ? player.experienceLevel >= 3 : true;
        boolean refreshHover = mouseX >= refreshX && mouseX < refreshX + 46 && mouseY >= refreshY && mouseY < refreshY + 24;
        int refreshBg = canRefresh ? (refreshHover ? 0xFF335588 : 0xFF224466) : 0xFF333333;
        g.fill(refreshX, refreshY, refreshX + 46, refreshY + 24, refreshBg);
        g.renderOutline(refreshX, refreshY, 46, 24, refreshHover ? 0xFFFFFFFF : 0xAA666666);
        String refreshText = Component.translatable("gui.potionenchant.refresh").getString();
        g.drawCenteredString(font, refreshText, refreshX + 23, refreshY + 2, canRefresh ? 0xFFFFFF : 0xFF666666);
        String costText = Component.translatable("gui.potionenchant.refresh_cost").getString();
        g.drawCenteredString(font, costText, refreshX + 23, refreshY + 14, canRefresh ? 0xAAFFAA : 0xFF444444);
        if (!targetItem.isEmpty()) {
            g.renderItem(targetItem, rightX + 10, tgtY + 14);
            g.renderItemDecorations(font, targetItem, rightX + 10, tgtY + 14);
            String itemName = targetItem.getHoverName().getString();
            if (font.width(itemName) > rightW - 50) itemName = font.plainSubstrByWidth(itemName, rightW - 53) + "...";
            g.drawString(font, itemName, rightX + 36, tgtY + 18, 0xFFFFFF);
        }
        if (!targetItem.isEmpty()) {
            String xpStr = Component.translatable("gui.potionenchant.player_level", player.experienceLevel).getString();
            g.drawString(font, xpStr, rightX + 6, tgtY + 38, 0x80FF20);
        }
        // Inventory grid
        int gridY = tgtY + 52, cell = 18, cols = Math.min(9, (rightW - 16) / cell);
        int visRows = (rightY + rightH - gridY) / cell;
        for (int i = invScroll * cols; i < Math.min(invItems.size(), (invScroll + visRows) * cols); i++) {
            InvItem item = invItems.get(i);
            int idx = i - invScroll * cols;
            int sx = rightX + 6 + (idx % cols) * cell, sy = gridY + (idx / cols) * cell;
            boolean isTarget = !targetItem.isEmpty() && ItemStack.isSameItemSameTags(targetItem, item.stack);
            drawSlot(g, sx, sy, cell, item.stack, mouseX, mouseY, isTarget);
            g.renderItem(item.stack, sx + 1, sy + 1);
            g.renderItemDecorations(font, item.stack, sx + 1, sy + 1);
            if (mouseX >= sx && mouseX < sx + cell && mouseY >= sy && mouseY < sy + cell)
                g.renderTooltip(font, item.stack, mouseX, mouseY);
            if (cannotEnchantItem(item.stack))
                g.fill(sx + 1, sy + 1, sx + cell - 1, sy + cell - 1, 0x80FF0000);
        }
        renderScrollbar(g, rightX + rightW - 6, gridY, visRows * cell, (invItems.size() + cols - 1) / cols, visRows, invScroll);
        // BOTTOM BUTTONS
        renderButton(g, cancelX, btnY, 80, 20, Component.translatable("gui.potionenchant.cancel"), mouseX, mouseY, 0xFF555555, 0xFF777777);
        boolean canConfirm = selectedSlot >= 0 && !targetItem.isEmpty();
        renderButton(g, confirmX, btnY, 80, 20, Component.translatable("gui.potionenchant.confirm"), mouseX, mouseY,
            canConfirm ? 0xFF226622 : 0xFF444444, canConfirm ? 0xFF33AA33 : 0xFF555555);
                zoom.pop(g);
        zoom.renderPanel(g, font, scrMX, scrMY, width, height);
        zoom.editBox.render(g, scrMX, scrMY, partialTick);
    }

    private void drawSlot(GuiGraphics g, int x, int y, int size, ItemStack stack, int mx, int my, boolean highlight) {
        int color = highlight ? 0xFFFFAA00 : 0xFF444444;
        g.fill(x, y, x + size, y + size, 0x80000000);
        g.renderOutline(x, y, size, size, color);
    }

    private void renderButton(GuiGraphics g, int x, int y, int w, int h, Component text, int mx, int my, int bg, int hoverBg) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hover ? hoverBg : bg);
        g.drawCenteredString(font, text.getString(), x + w / 2, y + (h - 8) / 2, hover ? 0xFFFFFF : 0xAAAAAA);
    }

    private void renderScrollbar(GuiGraphics g, int x, int y, int visH, int totalRows, int visRows, int offset) {
        if (totalRows <= visRows) return;
        int th = Math.max(15, visH * visRows / totalRows);
        int ty = y + (visH - th) * offset / (totalRows - visRows);
        g.fill(x, y, x + 4, y + visH, 0x40000000);
        g.fill(x, ty, x + 4, ty + th, 0xFFFFFFFF);
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
        double zmx = zoom.mx(mx, width);
        double zmy = zoom.my(my, height);
        // Confirm
        if (zmx >= confirmX && zmx < confirmX + 80 && zmy >= btnY && zmy < btnY + 20) {
            if (selectedSlot >= 0 && !targetItem.isEmpty()) applyOption(selectedSlot);
            return true;
        }
        // Cancel
        if (zmx >= cancelX && zmx < cancelX + 80 && zmy >= btnY && zmy < btnY + 20) { onClose(); return true; }
        // LEFT: select option
        if (zmx >= leftX && zmx < leftX + leftW && zmy >= leftY && zmy < leftY + leftH) {
            int slotH = (leftH - 22) / 3;
            int slot = (int)(zmy - leftY - 18) / slotH;
            if (slot >= 0 && slot < 3 && options[slot] != null) selectedSlot = slot;
            return true;
        }
        // RIGHT: inventory
        int cell = 18, cols = Math.min(9, (rightW - 16) / cell);
        int tgtY = rightY + 16, gridY = tgtY + 52;
        if (zmx >= rightX + 6 && zmx < rightX + rightW - 6 && zmy >= gridY && zmy < rightY + rightH) {
            int col = (int)(zmx - rightX - 6) / cell;
            int row = (int)(zmy - gridY) / cell;
            int idx = (invScroll + row) * cols + col;
            if (col >= 0 && col < cols && idx >= 0 && idx < invItems.size()) {
                InvItem clicked = invItems.get(idx);
                if (cannotEnchantItem(clicked.stack)) return true;
                targetItem = clicked.stack.copy();
                selectedSlot = -1;
                regenerateOptions();
            }
            return true;
        }
        // Refresh button
        int refreshX = rightX + rightW - 52, refreshY = tgtY + 10;
        if (zmx >= refreshX && zmx < refreshX + 46 && zmy >= refreshY && zmy < refreshY + 24) {
            boolean cr = !player.isCreative() ? player.experienceLevel >= 3 : true;
            if (cr) { if (!player.isCreative()) player.giveExperienceLevels(-3); regenerateOptions(); }
            return true;
        }
        // Target slot click -> clear
        if (zmx >= rightX + 6 && zmx < rightX + 30 && zmy >= tgtY + 10 && zmy < tgtY + 34) {
            targetItem = ItemStack.EMPTY; selectedSlot = -1; regenerateOptions(); return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void applyOption(int slot) {
        if (targetItem.isEmpty()) return;
        PotionOption opt = options[slot];
        if (opt == null) return;
        if (!player.isCreative() && player.experienceLevel < opt.cost) return;
        List<PotionEnchantData> existing = PotionEnchantManager.getPotionEnchantments(targetItem);
        for (PotionEnchantData ed : existing)
            if (ed.getEffect() == opt.effect && ed.getAmplifier() + 1 >= opt.level) return;
        if (PotionEnchantConfig.COMMON.limitAllEnchants.get()) {
            long count = existing.stream().map(PotionEnchantData::getEffect).distinct().count();
            boolean has = existing.stream().anyMatch(e -> e.getEffect() == opt.effect);
            if (!has && count >= PotionEnchantConfig.COMMON.maxAllEnchants.get()) return;
        }
        int existingLvl = getExistingLevel(opt.effect);
        int newLvl = Math.max(opt.level, existingLvl + 1);
        List<BonusEffect> bonuses = new ArrayList<>();
        Random rand = new Random(player.getEnchantmentSeed() ^ blockPos.hashCode() ^ slot ^ (int)System.currentTimeMillis());
        float bonusChance = power / 12f;
        if (rand.nextFloat() < bonusChance) {
            List<MobEffect> available = getFilteredEffects();
            available.remove(opt.effect);
            if (!available.isEmpty()) {
                MobEffect bonusEff = available.get(rand.nextInt(available.size()));
                bonuses.add(new BonusEffect(bonusEff, rand.nextInt(opt.level) + 1));
                if (power >= 3 && rand.nextFloat() < bonusChance * 0.5f) {
                    available.remove(bonusEff);
                    if (!available.isEmpty()) bonuses.add(new BonusEffect(available.get(rand.nextInt(available.size())), rand.nextInt(opt.level) + 1));
                }
            }
        }
        PotionEnchantTableNetwork.CHANNEL.sendToServer(
            new PotionEnchantTableNetwork.ApplyEffectPacket(blockPos, targetItem, opt.effect, newLvl, opt.cost, bonuses));
        selectedSlot = -1;
        regenerateOptions();
    }

    private int getExistingLevel(MobEffect effect) {
        if (targetItem.isEmpty()) return 0;
        for (PotionEnchantData ed : PotionEnchantManager.getPotionEnchantments(targetItem))
            if (ed.getEffect() == effect) return ed.getAmplifier() + 1;
        return 0;
    }

    @Override public void onClose() {
        if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.closeContainer();
        zoom.saveToConfig();
        super.onClose();
    }
    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= width - 32) { zoom.scroll(delta); return true; }
        double zmx = zoom.mx(mx, width);
        double zmy = zoom.my(my, height);
        if (categoryBar.mouseScrolled(mx, my, delta)) return true;
        int cell = 18, cols = Math.min(9, (rightW - 16) / cell);
        int gridY = rightY + 16 + 52;
        if (zmx >= rightX && zmx < rightX + rightW) {
            int visRows = (rightY + rightH - gridY) / cell;
            int totalRows = (invItems.size() + cols - 1) / cols;
            invScroll = Mth.clamp(invScroll - (int)delta, 0, Math.max(0, totalRows - visRows));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (zoom.dragging) { zoom.updateFromMouse(my, height); return true; }
        double zmx = zoom.mx(mx, width);
        double zmy = zoom.my(my, height);
        return categoryBar.mouseDragged(mx) || super.mouseDragged(mx, my, btn, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int btn) {
        zoom.dragging = false;
        double zmx = zoom.mx(mx, width);
        double zmy = zoom.my(my, height);
        categoryBar.mouseReleased(); return super.mouseReleased(mx, my, btn);
    }
    @Override public boolean keyPressed(int kc, int sc, int mod) {
        if (zoom.editBox.isFocused()) return zoom.editBox.keyPressed(kc, sc, mod);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.keyPressed(kc, sc, mod);
        return super.keyPressed(kc, sc, mod);
    }
    @Override public boolean charTyped(char cp, int mod) {
        if (zoom.editBox.isFocused()) return zoom.editBox.charTyped(cp, mod);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.charTyped(cp, mod);
        return super.charTyped(cp, mod);
    }
    @Override public void tick() {
        super.tick();
        if (player != null && player.tickCount % 15 == 0) loadInventory();
    }
    @Override public boolean isPauseScreen() { return false; }

    private static String toRoman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n); };
    }

    public static class BonusEffect {
        public final MobEffect effect; public final int level;
        public BonusEffect(MobEffect e, int l) { effect = e; level = l; }
    }
}
