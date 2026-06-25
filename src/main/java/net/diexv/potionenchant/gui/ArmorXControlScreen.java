package net.diexv.potionenchant.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.network.ArmorXPacketHandler;
import net.diexv.potionenchant.network.EnchantBookPacketHandler;
import net.diexv.potionenchant.gui.GuiZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.diexv.potionenchant.util.PinyinHelper;

public class ArmorXControlScreen extends Screen {

    private enum PanelMode { POTION, ENCHANT }
    private PanelMode currentMode = PanelMode.POTION;
    private EditBox searchBox;
    private final GuiZoom zoom = new GuiZoom("armor_x_control");
    private Button confirmButton, cancelButton;

    private List<MobEffectInfo> allEffects = new ArrayList<>();
    private List<MobEffectInfo> filteredEffects = new ArrayList<>();
    private MobEffectInfo selectedEffect;
    private Map<MobEffect, Integer> levelAdjustments = new HashMap<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE = 5;
    private boolean isDragging = false;
    private int descScrollOffset = 0;
    private CategoryBar categoryBar;

    private List<EnchantInfo> allEnchants = new ArrayList<>();
    private List<EnchantInfo> filteredEnchants = new ArrayList<>();
    private EnchantInfo selectedEnchant;
    private Map<Enchantment, Integer> enchantLevelAdjustments = new HashMap<>();
    private int enchantScrollOffset = 0;
    private int enchantDescScrollOffset = 0;
    private CategoryBar enchantCategoryBar;

    private Map<String, Boolean> armorFeatures = new HashMap<>();
    private int featureScrollOffset = 0;
    private boolean featuresModified = false;

    private int bottleCount = 0;
    private EditBox bottleEditBox;
    private EditBox potionLevelBox;
    private EditBox enchantLevelBox;
    private int currentUpgradeLevel = 0;
    private boolean levelEditActive = false;
    private String tooltipText = "";
    private int tooltipTimer = 0;

    private static final int GUI_WIDTH = 450;
    private int listX, listY, listWidth, listHeight;
    private int statsY, statsHeight;
    private int featureX, featureWidth, featurePanelHeight;
    private int upgradeY, upgradeHeight;
    private int modeBtnY, categoryY, searchY;

    public ArmorXControlScreen() {
        super(Component.translatable("gui.potionenchant.armorx_control"));
    }

    @Override
    protected void init() {
        loadAllEffects();
        loadAllEnchantments();
        loadArmorFeaturesFromNBT();
        loadUpgradeLevelFromNBT();

        zoom.init(font, width, height);

        listX = Math.max(2, (width - GUI_WIDTH) / 2);
        listWidth = 200;
        listHeight = MAX_VISIBLE * 20 + 5;
        featureX = listX + listWidth + 20;
        featureWidth = width - featureX - listX;

        searchY = 30;
        modeBtnY = 55;
        categoryY = 75;
        listY = categoryY + 22;

        String[] fk = {"ranged_attack","auto_ranged_attack","destruction_mode","flight_mode"};
        featurePanelHeight = fk.length * 25 + 25;
        upgradeY = listY + featurePanelHeight + 5;
        upgradeHeight = 60;
        statsY = listY + listHeight + 8;
        statsHeight = 110;

        searchBox = new EditBox(font, listX, searchY, listWidth, 20, Component.translatable("gui.potionenchant.search"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(this::onPotionSearchTextChanged);
        addRenderableWidget(searchBox);

        confirmButton = Button.builder(Component.translatable("gui.potionenchant.confirm"), b -> onConfirm())
            .bounds((width - 200) / 2 - 55, height - 40, 100, 20).build();
        confirmButton.active = false;
        addRenderableWidget(confirmButton);

        cancelButton = Button.builder(Component.translatable("gui.potionenchant.cancel"), b -> onClose())
            .bounds((width - 200) / 2 + 55, height - 40, 100, 20).build();
        addRenderableWidget(cancelButton);

        categoryBar = new CategoryBar("gui.potionenchant.category",
            new String[]{"all", "beneficial"}, 50, 16, 4);
        categoryBar.init(listX, categoryY);

        enchantCategoryBar = new CategoryBar("gui.potionenchant.category",
            new String[]{"all", "weapon", "armor", "tool", "curse"}, 50, 16, 4);
        enchantCategoryBar.init(listX, categoryY);

        bottleEditBox = new EditBox(font, featureX + 23, upgradeY + 39, 40, 14, Component.translatable("gui.potionenchant.bottle_count"));
        bottleEditBox.setMaxLength(10);
        bottleEditBox.setValue("0");
        bottleEditBox.setResponder(this::onBottleInputChanged);
        // bottleEditBox rendered manually inside zoom - do not add as renderable widget

        // 药水等级输入框
        potionLevelBox = new EditBox(font, listX + 5, statsY + 18, 50, 14, Component.translatable("gui.potionenchant.level_input"));
        potionLevelBox.setMaxLength(5);
        potionLevelBox.setResponder(this::onPotionLevelInputChanged);
        potionLevelBox.setVisible(false);
        // potionLevelBox rendered manually inside zoom - do not add as renderable widget

        // 附魔等级输入框
        enchantLevelBox = new EditBox(font, listX + 5, statsY + 18, 50, 14, Component.translatable("gui.potionenchant.level_input"));
        enchantLevelBox.setMaxLength(5);
        enchantLevelBox.setResponder(this::onEnchantLevelInputChanged);
        enchantLevelBox.setVisible(false);
        // enchantLevelBox rendered manually inside zoom - do not add as renderable widget

        recalcBottleCount();
        updateFilter();
    }

    private void switchMode(PanelMode mode) {
        if (currentMode == mode) return;
        currentMode = mode;
        scrollOffset = 0; descScrollOffset = 0;
        enchantScrollOffset = 0; enchantDescScrollOffset = 0;
        selectedEffect = null; selectedEnchant = null;
        if (potionLevelBox != null) potionLevelBox.setVisible(false);
        if (enchantLevelBox != null) enchantLevelBox.setVisible(false);
        updateFilter();
        searchBox.setResponder(currentMode == PanelMode.POTION
            ? this::onPotionSearchTextChanged : this::onEnchantSearchTextChanged);
        searchBox.setMessage(Component.translatable(
            currentMode == PanelMode.POTION ? "gui.potionenchant.search" : "gui.potionenchant.search_enchant"));
        searchBox.setValue("");
        updateConfirmButton();
    }

    private void loadUpgradeLevelFromNBT() {
        currentUpgradeLevel = 0;
        if (minecraft == null || minecraft.player == null) return;
        ItemStack helmet = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            CompoundTag tag = helmet.getTag();
            if (tag != null) currentUpgradeLevel = tag.getInt("ArmorUpgradeLevel");
        }
    }

    private void recalcBottleCount() {
        if (minecraft == null || minecraft.player == null) { bottleCount = 0; return; }
        int max = getTotalAvailableBottles();
        if (bottleCount > max) bottleCount = max;
        if (bottleEditBox != null) bottleEditBox.setValue(String.valueOf(bottleCount));
    }

    private boolean isPotionBottle(ItemStack stack) {
        return stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION
            || stack.getItem() == Items.LINGERING_POTION
            || stack.getItem() == net.diexv.potionenchant.item.ModItems.UNIVERSAL_POTION_BOTTLE.get();
    }

    private int getTotalAvailableBottles() {
        if (minecraft == null || minecraft.player == null) return 0;
        int total = 0;
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (isPotionBottle(stack)) total += stack.getCount();
        }
        return total;
    }

    private void onBottleInputChanged(String text) {
        try {
            int val = text.isEmpty() ? 0 : Integer.parseInt(text);
            if (val < 0) val = 0;
            int max = getTotalAvailableBottles();
            if (val > max) val = max;
            bottleCount = val;
            updateConfirmButton();
        } catch (NumberFormatException ignored) {}
    }


    private void onPotionLevelInputChanged(String text) {
        if (selectedEffect == null) return;
        try {
            int val = text.isEmpty() ? 0 : Integer.parseInt(text);
            if (val < 0) val = 0;
            int maxLevel = PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get();
            if (val > maxLevel) val = maxLevel;
            setLevelAdjustment(selectedEffect.effect, val);
            if (potionLevelBox != null && !potionLevelBox.getValue().equals(String.valueOf(val)))
                potionLevelBox.setValue(String.valueOf(val));
        } catch (NumberFormatException ignored) {}
    }

    private void onEnchantLevelInputChanged(String text) {
        if (selectedEnchant == null) return;
        try {
            int val = text.isEmpty() ? 0 : Integer.parseInt(text);
            if (val < 0) val = 0;
            if (val > selectedEnchant.maxLevel) val = selectedEnchant.maxLevel;
            setEnchantLevelAdjustment(selectedEnchant.enchantment, val);
            if (enchantLevelBox != null && !enchantLevelBox.getValue().equals(String.valueOf(val)))
                enchantLevelBox.setValue(String.valueOf(val));
        } catch (NumberFormatException ignored) {}
    }
    private void loadArmorFeaturesFromNBT() {
        armorFeatures.clear();
        String[] keys = {"ranged_attack","auto_ranged_attack","destruction_mode","flight_mode"};
        for (String k : keys) armorFeatures.put(k, false);
        if (minecraft == null || minecraft.player == null) return;
        ItemStack helmet = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return;
        CompoundTag tag = helmet.getTag();
        if (tag == null) return;
        CompoundTag ft = tag.getCompound("ArmorFeatures");
        for (String k : keys) armorFeatures.put(k, ft.getBoolean(k));
    }

    private void loadAllEffects() {
        allEffects.clear();
        ForgeRegistries.MOB_EFFECTS.getValues().stream()
            .sorted(Comparator.comparing(e -> e.getDisplayName().getString()))
            .forEach(effect -> {
                ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                if (key != null) {
                    String name = effect.getDisplayName().getString();
                    String dk = "effect." + key.getNamespace() + "." + key.getPath() + ".description";
                    String desc = net.minecraft.client.resources.language.I18n.get(dk, "");
                    if (desc.isEmpty() || desc.equals(dk))
                        desc = Component.translatable("gui.potionenchant.no_description").getString();
                    allEffects.add(new MobEffectInfo(effect, key, name, desc, effect.isBeneficial()));
                }
            });
        filteredEffects = allEffects.stream().filter(this::matchesPotionCategory).collect(Collectors.toList());
    }

    private void onPotionSearchTextChanged(String text) {
        String lt = (text == null ? "" : text).toLowerCase().trim();
        filteredEffects = allEffects.stream()
            .filter(this::matchesPotionCategory)
            .filter(i -> lt.isEmpty() || PinyinHelper.matchesSearch(i.name, i.name, "", i.key.toString(), lt))
            .collect(Collectors.toList());
        scrollOffset = 0; selectedEffect = null;
        updateConfirmButton();
    }

    private boolean matchesPotionCategory(MobEffectInfo info) {
        if (categoryBar == null) return true;
        return !"beneficial".equals(categoryBar.getFilter()) || info.isBeneficial;
    }

    private int getCurrentPotionLevel(MobEffect effect) {
        if (minecraft == null || minecraft.player == null) return 0;
        return net.diexv.potionenchant.util.XArmorEnchantmentManager.getXArmorEnchantments(minecraft.player)
            .getOrDefault(effect, 0);
    }

    private void setLevelAdjustment(MobEffect effect, int targetLevel) {
        int current = getCurrentPotionLevel(effect);
        if (targetLevel == current) levelAdjustments.remove(effect);
        else levelAdjustments.put(effect, targetLevel);
        updateConfirmButton();
    }

    private void loadAllEnchantments() {
        allEnchants.clear();
        boolean ab = false;
        try {
            if (PotionEnchantConfig.SERVER != null && PotionEnchantConfig.SERVER.allowEnchantLevelBeyondCap != null)
                ab = PotionEnchantConfig.SERVER.allowEnchantLevelBeyondCap.get();
        } catch (Exception ignored) {}
        for (Enchantment e : ForgeRegistries.ENCHANTMENTS) {
            if (e == null) continue;
            allEnchants.add(new EnchantInfo(e, ab ? Integer.MAX_VALUE : Math.max(e.getMaxLevel(), 1)));
        }
        allEnchants.sort(Comparator.comparing(e -> e.enchantment.getFullname(1).getString()));
        filteredEnchants = new ArrayList<>(allEnchants);
    }

    private void onEnchantSearchTextChanged(String text) {
        String q = (text == null ? "" : text).toLowerCase().trim();
        filteredEnchants = allEnchants.stream()
            .filter(this::matchesEnchantCategory)
            .filter(ei -> {
                if (q.isEmpty()) return true;
                String n = ei.enchantment.getFullname(1).getString();
                String displayName = net.minecraft.network.chat.Component.translatable(ei.enchantment.getDescriptionId()).getString();
                return PinyinHelper.matchesSearch(n, displayName, n, ei.id, q);
            })
            .collect(Collectors.toList());
        enchantScrollOffset = 0; selectedEnchant = null;
        updateConfirmButton();
    }

    private boolean matchesEnchantCategory(EnchantInfo ei) {
        if (enchantCategoryBar == null) return true;
        String f = enchantCategoryBar.getFilter();
        if ("all".equals(f)) return true;
        EnchantmentCategory ec = ei.enchantment.category;
        switch (f) {
            case "weapon": return ec == EnchantmentCategory.WEAPON || ec == EnchantmentCategory.BOW
                || ec == EnchantmentCategory.CROSSBOW || ec == EnchantmentCategory.TRIDENT;
            case "armor": return ec == EnchantmentCategory.ARMOR || ec == EnchantmentCategory.ARMOR_FEET
                || ec == EnchantmentCategory.ARMOR_LEGS || ec == EnchantmentCategory.ARMOR_CHEST
                || ec == EnchantmentCategory.ARMOR_HEAD || ec == EnchantmentCategory.WEARABLE;
            case "tool": return !isWeapon(ec) && !isArmor(ec);
            case "curse": return ei.enchantment.isCurse();
        }
        return true;
    }

    private boolean isWeapon(EnchantmentCategory ec) {
        return ec == EnchantmentCategory.WEAPON || ec == EnchantmentCategory.BOW
            || ec == EnchantmentCategory.CROSSBOW || ec == EnchantmentCategory.TRIDENT;
    }

    private boolean isArmor(EnchantmentCategory ec) {
        return ec == EnchantmentCategory.ARMOR || ec == EnchantmentCategory.ARMOR_FEET
            || ec == EnchantmentCategory.ARMOR_LEGS || ec == EnchantmentCategory.ARMOR_CHEST
            || ec == EnchantmentCategory.ARMOR_HEAD || ec == EnchantmentCategory.WEARABLE;
    }

    private EquipmentSlot[] getCompatibleSlots(Enchantment e) {
        EnchantmentCategory cat = e.category;
        if (cat == EnchantmentCategory.ARMOR_HEAD) return new EquipmentSlot[]{EquipmentSlot.HEAD};
        if (cat == EnchantmentCategory.ARMOR_CHEST) return new EquipmentSlot[]{EquipmentSlot.CHEST};
        if (cat == EnchantmentCategory.ARMOR_LEGS) return new EquipmentSlot[]{EquipmentSlot.LEGS};
        if (cat == EnchantmentCategory.ARMOR_FEET) return new EquipmentSlot[]{EquipmentSlot.FEET};
        return new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    }

    private void setEnchantLevelAdjustment(Enchantment e, int lv) {
        if (lv <= 0) enchantLevelAdjustments.remove(e);
        else enchantLevelAdjustments.put(e, lv);
        updateConfirmButton();
    }

    private void updateConfirmButton() {
        boolean hasChanges;
        if (currentMode == PanelMode.POTION)
            hasChanges = !levelAdjustments.isEmpty() || featuresModified || bottleCount > 0;
        else
            hasChanges = !enchantLevelAdjustments.isEmpty() || featuresModified || bottleCount > 0;
        confirmButton.active = hasChanges;
    }

    private void onConfirm() {
        if (minecraft == null || minecraft.player == null) return;
        if (bottleCount > 0) {
            ArmorXPacketHandler.INSTANCE.sendToServer(new ArmorXPacketHandler.UpgradeArmorPacket(bottleCount));
            bottleCount = 0;
            if (bottleEditBox != null) bottleEditBox.setValue("0");
        }
        if (currentMode == PanelMode.POTION && !levelAdjustments.isEmpty()) {
            CompoundTag enchantData = new CompoundTag();
            ListTag effectsList = new ListTag();
            int count = 0;
            for (var entry : levelAdjustments.entrySet()) {
                ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(entry.getKey());
                if (key == null) continue;
                CompoundTag effectTag = new CompoundTag();
                effectTag.putString("Effect", key.toString());
                effectTag.putInt("Adjustment", entry.getValue());
                effectsList.add(effectTag);
                count++;
            }
            enchantData.put("Effects", effectsList);
            enchantData.putInt("AppliedCount", count);
            ArmorXPacketHandler.INSTANCE.sendToServer(
                new ArmorXPacketHandler.ApplyArmorXEffectPacket(enchantData));
            levelAdjustments.clear();
        }
        if (currentMode == PanelMode.ENCHANT && !enchantLevelAdjustments.isEmpty()) {
            for (var entry : enchantLevelAdjustments.entrySet()) {
                ResourceLocation key = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
                if (key == null) continue;
                EnchantBookPacketHandler.INSTANCE.sendToServer(
                    new EnchantBookPacketHandler.ApplyEnchantPacket(key.toString(), entry.getValue(), -2));
            }
            enchantLevelAdjustments.clear();
        }
        featuresModified = false;
        updateConfirmButton();
        loadUpgradeLevelFromNBT();
    }

    private void updateFilter() {
        if (currentMode == PanelMode.POTION) {
            onPotionSearchTextChanged(searchBox.getValue());
        } else {
            onEnchantSearchTextChanged(searchBox.getValue());
        }
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int scrMX = mouseX, scrMY = mouseY;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font,
            Component.translatable("gui.potionenchant.armorx_control"), width / 2, 10, 0xFFFF55);
        zoom.renderHeaderZoom(guiGraphics, font, width / 2 + 60, 6, 50, scrMX, scrMY, partialTick);
        guiGraphics.drawString(font,
            Component.translatable(currentMode == PanelMode.POTION ? "gui.potionenchant.search" : "gui.potionenchant.search_enchant"),
            listX, searchY - 12, 0xCCCCCC);
        renderModeButtons(guiGraphics, mouseX, mouseY, listX, modeBtnY, listWidth);
        CategoryBar activeBar = currentMode == PanelMode.POTION ? categoryBar : enchantCategoryBar;
        if (activeBar != null) activeBar.render(guiGraphics, font, mouseX, mouseY, width, height);
        zoom.push(guiGraphics, width, height);
        mouseX = (int) zoom.mx(mouseX, width);
        mouseY = (int) zoom.my(mouseY, height);
        if (currentMode == PanelMode.POTION) {
            renderPotionList(guiGraphics, mouseX, mouseY, listX, listY, listWidth, listHeight, partialTick);
            if (selectedEffect != null && potionLevelBox != null && potionLevelBox.isVisible() && !isEffectVisible(selectedEffect)) potionLevelBox.setVisible(false);
            renderPotionStats(guiGraphics, listX, statsY, listWidth, statsHeight, partialTick);
        } else {
            renderEnchantList(guiGraphics, mouseX, mouseY, listX, listY, listWidth, listHeight, partialTick);
            if (selectedEnchant != null && enchantLevelBox != null && enchantLevelBox.isVisible() && !isEnchantVisible(selectedEnchant)) enchantLevelBox.setVisible(false);
            renderEnchantStats(guiGraphics, listX, statsY, listWidth, statsHeight, partialTick);
        }
        renderFeaturePanel(guiGraphics, mouseX, mouseY, featureX, listY, featureWidth, featurePanelHeight);
        renderUpgradePanel(guiGraphics, mouseX, mouseY, featureX, upgradeY, featureWidth, upgradeHeight, partialTick);
        zoom.pop(guiGraphics);
        zoom.renderPanel(guiGraphics, font, scrMX, scrMY, width, height);
        zoom.editBox.render(guiGraphics, scrMX, scrMY, partialTick);
        // 显示复制提示
        if (tooltipTimer > 0 && !tooltipText.isEmpty()) {
            int tw = font.width(tooltipText);
            int tx = (width - tw) / 2;
            int ty2 = height - 30;
            guiGraphics.fill(tx - 4, ty2 - 2, tx + tw + 4, ty2 + 12, 0xCC000000);
            guiGraphics.drawString(font, tooltipText, tx, ty2, 0xFFFFFF);
            tooltipTimer--;
        }
    }

    private void renderModeButtons(GuiGraphics g, int mx, int my, int x, int y, int w) {
        int bw = (w - 6) / 2; int bh = 16;
        int px = x + 2; boolean ph = mx >= px && mx <= px + bw && my >= y && my <= y + bh;
        int pcol = currentMode == PanelMode.POTION ? 0xFF5555AA : (ph ? 0xFF6666BB : 0xFF444488);
        g.fill(px, y, px + bw, y + bh, pcol);
        if (ph) drawHoverBorder(g, px, y, bw, bh);
        g.drawCenteredString(font, Component.translatable("gui.potionenchant.mode.potion"), px + bw / 2, y + 4, 0xFFFFFF);
        int ex = x + bw + 6; boolean eh = mx >= ex && mx <= ex + bw && my >= y && my <= y + bh;
        int ecol = currentMode == PanelMode.ENCHANT ? 0xFF5555AA : (eh ? 0xFF6666BB : 0xFF444488);
        g.fill(ex, y, ex + bw, y + bh, ecol);
        if (eh) drawHoverBorder(g, ex, y, bw, bh);
        g.drawCenteredString(font, Component.translatable("gui.potionenchant.mode.enchant"), ex + bw / 2, y + 4, 0xFFFFFF);
    }

    private void renderPotionList(GuiGraphics g, int mx, int my, int lx, int ly, int lw, int lh, float partialTick) {
        g.fill(lx, ly, lx + lw, ly + lh, 0x80000000);
        int maxLevel = PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get();
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredEffects.size(); i++) {
            int idx = i + scrollOffset;
            MobEffectInfo info = filteredEffects.get(idx);
            int y = ly + 5 + i * 20;
            int bs = 14;
            if (info == selectedEffect)
                g.fill(lx + 2, y - 2, lx + lw - 2, y + 18, 0x40FFFFFF);
            int curLv = levelAdjustments.containsKey(info.effect)
                ? levelAdjustments.get(info.effect) : getCurrentPotionLevel(info.effect);
            int minusX = lx + lw - bs - 4;
            boolean mh = mx >= minusX && mx <= minusX + bs && my >= y + 2 && my <= y + 2 + bs;
            g.fill(minusX, y + 2, minusX + bs, y + 2 + bs, curLv > 0 ? 0xFFFF5555 : 0xFF666666);
            if (mh) drawHoverBorder(g, minusX, y + 2, bs, bs);
            g.drawCenteredString(font, "-", minusX + bs / 2, y + 4, 0xFFFFFF);
            int plusX = minusX - bs - 2;
            boolean ph = mx >= plusX && mx <= plusX + bs && my >= y + 2 && my <= y + 2 + bs;
            boolean canInc = curLv < maxLevel;
            g.fill(plusX, y + 2, plusX + bs, y + 2 + bs, canInc ? 0xFF55FF55 : 0xFF666666);
            if (ph) drawHoverBorder(g, plusX, y + 2, bs, bs);
            g.drawCenteredString(font, "+", plusX + bs / 2, y + 4, 0xFFFFFF);
            int nameRight = plusX - 2;
            boolean rh = mx >= lx && mx <= nameRight && my >= y && my <= y + 20;
            if (rh) g.fill(lx, y, nameRight, y + 20, 0x20FFFFFF);
            String dn = info.name;
            int maxNameW = nameRight - lx - 55;
            if (font.width(dn) > maxNameW) dn = font.plainSubstrByWidth(dn, maxNameW - 5) + "...";
            g.drawString(font, dn, lx + 5, y, 0xFFFFFF);
            String lvStr = String.valueOf(curLv);
            if (!(info == selectedEffect && levelEditActive)) {
                g.drawString(font, lvStr, nameRight - font.width(lvStr) - 2, y + 3, 0xFFCCFFCC);
            }
            if (info == selectedEffect && levelEditActive && potionLevelBox != null) {
                potionLevelBox.setVisible(true);
                potionLevelBox.setX(nameRight - 50);
                potionLevelBox.setY(y + 2);
                potionLevelBox.setWidth(44);
                potionLevelBox.setHeight(14);
                potionLevelBox.render(g, mx, my, partialTick);
            }
        }
        if (filteredEffects.size() > MAX_VISIBLE)
            renderScrollbar(g, lx + lw + 2, ly, 8, lh, scrollOffset,
                filteredEffects.size() - MAX_VISIBLE, filteredEffects.size(), MAX_VISIBLE);
    }

    private void renderEnchantList(GuiGraphics g, int mx, int my, int lx, int ly, int lw, int lh, float partialTick) {
        g.fill(lx, ly, lx + lw, ly + lh, 0x80000000);
        for (int i = 0; i < MAX_VISIBLE && (i + enchantScrollOffset) < filteredEnchants.size(); i++) {
            int idx = i + enchantScrollOffset;
            EnchantInfo info = filteredEnchants.get(idx);
            int y = ly + 5 + i * 20;
            int bs = 14;
            if (info == selectedEnchant)
                g.fill(lx + 2, y - 2, lx + lw - 2, y + 18, 0x40FFFFFF);
            int curLv = enchantLevelAdjustments.getOrDefault(info.enchantment, 0);
            int minusX = lx + lw - bs - 4;
            boolean mh = mx >= minusX && mx <= minusX + bs && my >= y + 2 && my <= y + 2 + bs;
            g.fill(minusX, y + 2, minusX + bs, y + 2 + bs, curLv > 0 ? 0xFFFF5555 : 0xFF666666);
            if (mh) drawHoverBorder(g, minusX, y + 2, bs, bs);
            g.drawCenteredString(font, "-", minusX + bs / 2, y + 4, 0xFFFFFF);
            int plusX = minusX - bs - 2;
            boolean ph = mx >= plusX && mx <= plusX + bs && my >= y + 2 && my <= y + 2 + bs;
            boolean canInc = curLv < info.maxLevel;
            g.fill(plusX, y + 2, plusX + bs, y + 2 + bs, canInc ? 0xFF55FF55 : 0xFF666666);
            if (ph) drawHoverBorder(g, plusX, y + 2, bs, bs);
            g.drawCenteredString(font, "+", plusX + bs / 2, y + 4, 0xFFFFFF);
            int nameRight = plusX - 2;
            boolean rh = mx >= lx && mx <= nameRight && my >= y && my <= y + 20;
            if (rh) g.fill(lx, y, nameRight, y + 20, 0x20FFFFFF);
            String dn = info.enchantment.getFullname(1).getString();
            int maxNameW = nameRight - lx - 55;
            if (font.width(dn) > maxNameW) dn = font.plainSubstrByWidth(dn, maxNameW - 5) + "...";
            g.drawString(font, dn, lx + 5, y, 0xFFFFFF);
            if (curLv > 0) {
                String lvStr = String.valueOf(curLv);
                if (!(info == selectedEnchant && levelEditActive)) {
                    g.drawString(font, lvStr, nameRight - font.width(lvStr) - 2, y + 3, 0xFFCCFFCC);
                }
            if (info == selectedEnchant && levelEditActive && enchantLevelBox != null) {
                enchantLevelBox.setVisible(true);
                enchantLevelBox.setX(nameRight - 50);
                enchantLevelBox.setY(y + 2);
                enchantLevelBox.setWidth(44);
                enchantLevelBox.setHeight(14);
                enchantLevelBox.render(g, mx, my, partialTick);
            }
            }
        }
        if (filteredEnchants.size() > MAX_VISIBLE)
            renderScrollbar(g, lx + lw + 2, ly, 8, lh, enchantScrollOffset,
                filteredEnchants.size() - MAX_VISIBLE, filteredEnchants.size(), MAX_VISIBLE);
    }

    private void renderPotionStats(GuiGraphics g, int x, int y, int w, int h, float partialTick) {
        g.fill(x, y, x + w, y + h, 0x80000000);
        g.drawCenteredString(font,
            Component.translatable("gui.potionenchant.no_stats").getString(), x + w / 2, y + 5, 0xFFFF55);
        List<String> lines = getPotionStatsLines();
        if (lines.isEmpty()) {
            g.drawCenteredString(font,
                Component.translatable("gui.potionenchant.no_stats").getString(), x + w / 2, y + h / 2 - 5, 0x888888);
            return;
        }
        renderScrollableLines(g, x, y, w, h, lines, descScrollOffset);
    }

    private List<String> getPotionStatsLines() {
        List<String> lines = new ArrayList<>();
        Map<MobEffect, Integer> cur = net.diexv.potionenchant.util.XArmorEnchantmentManager
            .getXArmorEnchantments(minecraft != null && minecraft.player != null ? minecraft.player : null);
        if (levelAdjustments.isEmpty()) {
            if (cur != null) {
                for (var e : cur.entrySet())
                    lines.add(e.getKey().getDisplayName().getString() + ": " +
                        Component.translatable("gui.potionenchant.lv").getString() + e.getValue());
            }
        } else {
            for (var e : levelAdjustments.entrySet()) {
                int ex = cur != null ? cur.getOrDefault(e.getKey(), 0) : 0;
                int tg = e.getValue();
                String nm = e.getKey().getDisplayName().getString();
                if (tg > ex) lines.add(nm + ": " + Component.translatable("gui.potionenchant.lv").getString()
                    + ex + " " + Component.translatable("gui.potionenchant.arrow").getString() + " "
                    + Component.translatable("gui.potionenchant.lv").getString() + tg);
                else if (tg <= 0) lines.add(nm + ": " + Component.translatable("gui.potionenchant.removed").getString());
                else lines.add(nm + ": " + Component.translatable("gui.potionenchant.lv").getString()
                    + ex + " " + Component.translatable("gui.potionenchant.arrow").getString() + " "
                    + Component.translatable("gui.potionenchant.lv").getString() + tg);
            }
        }
        return lines;
    }

    private void renderEnchantStats(GuiGraphics g, int x, int y, int w, int h, float partialTick) {
        g.fill(x, y, x + w, y + h, 0x80000000);
        g.drawCenteredString(font,
            Component.translatable("gui.potionenchant.enchant_stats").getString(), x + w / 2, y + 5, 0xFFFF55);
        List<String> lines = getEnchantStatsLines();
        if (lines.isEmpty()) {
            g.drawCenteredString(font,
                Component.translatable("gui.potionenchant.no_enchant_stats").getString(), x + w / 2, y + h / 2 - 5, 0x888888);
            return;
        }
        renderScrollableLines(g, x, y, w, h, lines, enchantDescScrollOffset);
    }

    private List<String> getEnchantStatsLines() {
        List<String> lines = new ArrayList<>();
        for (var e : enchantLevelAdjustments.entrySet()) {
            String nm = e.getKey().getFullname(1).getString();
            EquipmentSlot[] slots = getCompatibleSlots(e.getKey());
            StringBuilder sb = new StringBuilder();
            for (EquipmentSlot s : slots) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(Component.translatable("gui.potionenchant.slot." + s.getName()).getString());
            }
            lines.add(nm + ": " + Component.translatable("gui.potionenchant.lv").getString()
                + e.getValue() + " [" + sb + "]");
        }
        return lines;
    }

    private void renderScrollableLines(GuiGraphics g, int x, int y, int w, int h, List<String> lines, int so) {
        int maxVis = (h - 25) / 10;
        int tot = lines.size();
        int maxSc = Math.max(0, tot - maxVis);
        int co = Math.min(so, maxSc);
        int sy = y + 18;
        for (int i = 0; i < maxVis && (i + co) < tot; i++) {
            String ln = lines.get(i + co);
            if (font.width(ln) > w - 15) ln = font.plainSubstrByWidth(ln, w - 25) + "...";
            g.drawString(font, ln, x + 5, sy + i * 10, 0xFFFFFF);
        }
        if (tot > maxVis) {
            int sx = x + w - 8; int sry = y + 16; int sw = 6; int sh = h - 30;
            int th = Math.max(15, sh * maxVis / tot);
            int ty = sry + (maxSc > 0 ? (sh - th) * co / maxSc : 0);
            g.fill(sx, sry, sx + sw, sry + sh, 0x40000000);
            g.fill(sx, ty, sx + sw, ty + th, 0xFFFFFFFF);
        }
    }

    private void renderFeaturePanel(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x80000000);
        g.drawCenteredString(font,
            Component.translatable("gui.potionenchant.armor_features").getString(), x + w / 2, y + 5, 0xFFFF55);
        String[] keys = {"ranged_attack","auto_ranged_attack","destruction_mode","flight_mode"};
        int fy = y + 20; int fh = 20; int gap = 5;
        int maxVis = Math.max(1, (h - 25) / (fh + gap));
        int maxFs = Math.max(0, keys.length - maxVis);
        featureScrollOffset = Math.min(featureScrollOffset, maxFs);
        for (int i = 0; i < maxVis && (i + featureScrollOffset) < keys.length; i++) {
            int idx = i + featureScrollOffset;
            String key = keys[idx];
            boolean en = armorFeatures.getOrDefault(key, false);
            int cy = fy + i * (fh + gap);
            boolean hv = mx >= x && mx <= x + w && my >= cy && my <= cy + fh;
            if (hv) g.fill(x + 2, cy, x + w - 2, cy + fh, 0x20FFFFFF);
            int cx = x + 10; int cby = cy + 3; int cs = 14;
            g.fill(cx, cby, cx + cs, cby + cs, en ? 0xFF55FF55 : 0xFF444444);
            if (en) g.drawString(font, "\u2713", cx + 3, cby + 2, 0xFFFFFF);
            if (hv) drawHoverBorder(g, cx, cby, cs, cs);
            g.drawString(font, Component.translatable("gui.potionenchant.feature." + key).getString(), cx + cs + 8, cy + 5, 0xFFFFFF);
        }
        if (keys.length > maxVis)
            renderScrollbar(g, x + w - 8, y + 18, 6, h - 30, featureScrollOffset, maxFs, keys.length, maxVis);
    }

    private void renderUpgradePanel(GuiGraphics g, int mx, int my, int x, int y, int w, int h, float partialTick) {
        g.fill(x, y, x + w, y + h, 0x80000000);
        g.drawCenteredString(font,
            Component.translatable("gui.potionenchant.upgrade.title").getString(), x + w / 2, y + 5, 0xFFFF55);
        int tot = getTotalAvailableBottles();
        g.drawString(font,
            Component.translatable("gui.potionenchant.upgrade.bottles_available", tot).getString(),
            x + 5, y + 22, 0xCCCCCC);
        int bs = 14; int iy = y + 38;
        int minusX = x + 5;
        boolean mh = mx >= minusX && mx <= minusX + bs && my >= iy && my <= iy + bs;
        g.fill(minusX, iy, minusX + bs, iy + bs, bottleCount > 0 ? 0xFFFF5555 : 0xFF666666);
        if (mh) drawHoverBorder(g, minusX, iy, bs, bs);
        g.drawCenteredString(font, "-", minusX + bs / 2, iy + 3, 0xFFFFFF);
        bottleEditBox.setX(minusX + bs + 4);
        bottleEditBox.setY(iy + 1);
        bottleEditBox.setWidth(40);
        bottleEditBox.setHeight(14);
        int plusX = minusX + bs + 48;
        boolean ph = mx >= plusX && mx <= plusX + bs && my >= iy && my <= iy + bs;
        boolean canInc = bottleCount < tot;
        g.fill(plusX, iy, plusX + bs, iy + bs, canInc ? 0xFF55FF55 : 0xFF666666);
        if (ph) drawHoverBorder(g, plusX, iy, bs, bs);
        g.drawCenteredString(font, "+", plusX + bs / 2, iy + 3, 0xFFFFFF);
        int saX = plusX + bs + 6;
        boolean sh = mx >= saX && mx <= saX + 36 && my >= iy && my <= iy + bs;
        g.fill(saX, iy, saX + 36, iy + bs, sh ? 0xFF8888FF : 0xFF6666AA);
        g.drawString(font,
            Component.translatable("gui.potionenchant.upgrade.select_all").getString(), saX + 3, iy + 3, 0xFFFFFF);
        // 渲染药水瓶数量编辑框（位于-和+按钮之间）
        bottleEditBox.render(g, mx, my, partialTick);
        // 绘制编辑框边框，使其更明显
        int bex = bottleEditBox.getX(), bey = bottleEditBox.getY();
        int bew = bottleEditBox.getWidth(), beh = bottleEditBox.getHeight();
        g.fill(bex - 1, bey - 1, bex + bew + 1, bey, 0xFF444444);
        g.fill(bex - 1, bey + beh, bex + bew + 1, bey + beh + 1, 0xFF444444);
        g.fill(bex - 1, bey, bex, bey + beh, 0xFF444444);
        g.fill(bex + bew, bey, bex + bew + 1, bey + beh, 0xFF444444);
        int bonus = currentUpgradeLevel * 10;
        String li = Component.translatable("gui.potionenchant.upgrade.level_info", currentUpgradeLevel, bonus).getString();
        g.drawString(font, li, x + w - font.width(li) - 5, y + 22, 0xFFAA55);
        if (bottleCount > 0) {
            int nl = currentUpgradeLevel + bottleCount;
            int nb = nl * 10;
            String pv = Component.translatable("gui.potionenchant.upgrade.preview", nl, nb).getString();
            g.drawString(font, pv, x + w - font.width(pv) - 5, iy + 3, 0xFF55FF55);
        }
    }

    private void drawHoverBorder(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y, 0xFFFFFFFF);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFFFFFFFF);
        g.fill(x - 1, y, x, y + h, 0xFFFFFFFF);
        g.fill(x + w, y, x + w + 1, y + h, 0xFFFFFFFF);
    }

    private void renderScrollbar(GuiGraphics g, int x, int y, int w, int h, int off, int maxSc, int tot, int vis) {
        if (tot <= vis) return;
        int th = Math.max(15, h * vis / tot);
        int ty = y + (maxSc > 0 ? (h - th) * off / maxSc : 0);
        g.fill(x, y, x + w, y + h, 0x40000000);
        g.fill(x, ty, x + w, ty + th, 0xFFFFFFFF);
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
        String[] fk = {"ranged_attack","auto_ranged_attack","destruction_mode","flight_mode"};
        int bw = (listWidth - 6) / 2;
        if (origMY >= modeBtnY && origMY <= modeBtnY + 16) {
            if (origMX >= listX + 2 && origMX <= listX + 2 + bw) { switchMode(PanelMode.POTION); return true; }
            if (origMX >= listX + bw + 6 && origMX <= listX + bw + 6 + bw) { switchMode(PanelMode.ENCHANT); return true; }
        }
        if (searchBox.isMouseOver(origMX, origMY)) {
            setFocused(searchBox);
            searchBox.setResponder(currentMode == PanelMode.POTION
                ? this::onPotionSearchTextChanged : this::onEnchantSearchTextChanged);
            return searchBox.mouseClicked(origMX, origMY, btn);
        }
        CategoryBar ab = currentMode == PanelMode.POTION ? categoryBar : enchantCategoryBar;
        if (ab != null && ab.mouseClicked(origMX, origMY)) {
            scrollOffset = 0; enchantScrollOffset = 0;
            selectedEffect = null; selectedEnchant = null;
            updateFilter(); return true;
        }
        if (currentMode == PanelMode.POTION) {
            if (handlePotionListClick(mx, my, btn)) return true;
        } else {
            if (handleEnchantListClick(mx, my, btn)) return true;
        }
        if (mx >= featureX && mx <= featureX + featureWidth && my >= listY && my <= listY + featurePanelHeight) {
            int maxVisF = Math.max(1, (featurePanelHeight - 25) / 25);
            for (int i = 0; i < maxVisF && (i + featureScrollOffset) < fk.length; i++) {
                int idxx = i + featureScrollOffset;
                int cy = listY + 20 + i * 25;
                if (my >= cy && my <= cy + 20) {
                    String key = fk[idxx];
                    boolean nv = !armorFeatures.getOrDefault(key, false);
                    armorFeatures.put(key, nv);
                    featuresModified = true;
                    ArmorXPacketHandler.INSTANCE.sendToServer(
                        new ArmorXPacketHandler.ToggleArmorFeaturePacket(key, nv));
                    updateConfirmButton(); return true;
                }
            }
        }
        if (origMX >= featureX && origMX <= featureX + featureWidth && origMY >= upgradeY && origMY <= upgradeY + upgradeHeight) {
            int iy = upgradeY + 38; int bs2 = 14;
            if (bottleEditBox.isMouseOver(origMX, origMY)) { setFocused(bottleEditBox); return bottleEditBox.mouseClicked(origMX, origMY, btn); }
            int minusX = featureX + 5;
            int plusX = minusX + bs2 + 48;
            int saX = plusX + bs2 + 6;
            if (origMX >= minusX && origMX <= minusX + bs2 && origMY >= iy && origMY <= iy + bs2) {
                bottleCount = Math.max(0, bottleCount - 1);
                bottleEditBox.setValue(String.valueOf(bottleCount));
                updateConfirmButton(); return true;
            }
            if (origMX >= plusX && origMX <= plusX + bs2 && origMY >= iy && origMY <= iy + bs2) {
                int maxB = getTotalAvailableBottles();
                bottleCount = Math.min(maxB, bottleCount + 1);
                bottleEditBox.setValue(String.valueOf(bottleCount));
                updateConfirmButton(); return true;
            }
            if (origMX >= saX && origMX <= saX + 36 && origMY >= iy && origMY <= iy + bs2) {
                bottleCount = getTotalAvailableBottles();
                bottleEditBox.setValue(String.valueOf(bottleCount));
                updateConfirmButton(); return true;
            }
        }
        // Click outside EditBox -> hide it
        if (levelEditActive) {
            boolean outsidePotion = potionLevelBox == null || !potionLevelBox.isMouseOver(origMX, origMY);
            boolean outsideEnchant = enchantLevelBox == null || !enchantLevelBox.isMouseOver(origMX, origMY);
            if (outsidePotion && outsideEnchant) {
                levelEditActive = false;
                if (potionLevelBox != null) { potionLevelBox.setVisible(false); potionLevelBox.setFocused(false); }
                if (enchantLevelBox != null) { enchantLevelBox.setVisible(false); enchantLevelBox.setFocused(false); }
            }
        }
        if (mx >= listX && mx <= listX + listWidth && my >= statsY && my <= statsY + statsHeight) {
            isDragging = true; return true;
        }
        return super.mouseClicked(origMX, origMY, btn);
    }

    private boolean handlePotionListClick(double mx, double my, int btn) {
        int bs = 14;
        int maxLevel = PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get();
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredEffects.size(); i++) {
            int idx = i + scrollOffset;
            MobEffectInfo info = filteredEffects.get(idx);
            int y = listY + 5 + i * 20;
            int minusX = listX + listWidth - bs - 4;
            int plusX = minusX - bs - 2;
            if (mx >= minusX && mx <= minusX + bs && my >= y + 2 && my <= y + 2 + bs) {
                int curLv = levelAdjustments.containsKey(info.effect)
                    ? levelAdjustments.get(info.effect) : getCurrentPotionLevel(info.effect);
                if (curLv > 0) { setLevelAdjustment(info.effect, curLv - 1); if (potionLevelBox != null && selectedEffect == info) potionLevelBox.setValue(String.valueOf(curLv - 1)); }
                return true;
            }
            if (mx >= plusX && mx <= plusX + bs && my >= y + 2 && my <= y + 2 + bs) {
                int curLv = levelAdjustments.containsKey(info.effect)
                    ? levelAdjustments.get(info.effect) : getCurrentPotionLevel(info.effect);
                if (curLv < maxLevel) { setLevelAdjustment(info.effect, curLv + 1); if (potionLevelBox != null && selectedEffect == info) potionLevelBox.setValue(String.valueOf(curLv + 1)); }
                return true;
            }
            int nameRight = plusX - 2;
            // Right-click on name area -> copy effect ID
            if (btn == 1 && mx >= listX && mx <= nameRight && my >= y && my <= y + 20) {
                if (info.key != null) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(info.key.toString());
                    tooltipText = "\u00a7a" + Component.translatable("gui.potionenchant.copied", info.key.toString()).getString();
                    tooltipTimer = 80;
                }
                return true;
            }
            if (mx >= listX && mx <= nameRight - 55 && my >= y && my <= y + 20) {
                selectedEffect = info; selectedEnchant = null;
                levelEditActive = false;
                if (potionLevelBox != null) potionLevelBox.setVisible(false);
                return true;
            }
            // Click on level number -> open level edit box
            if (mx >= nameRight - 55 && mx <= nameRight && my >= y && my <= y + 20) {
                selectedEffect = info; selectedEnchant = null;
                if (potionLevelBox != null) {
                    int clv = levelAdjustments.containsKey(info.effect)
                        ? levelAdjustments.get(info.effect) : getCurrentPotionLevel(info.effect);
                    potionLevelBox.setValue(String.valueOf(clv));
                    potionLevelBox.setVisible(true);
                    potionLevelBox.setFocused(true);
                    setFocused(potionLevelBox);
                    levelEditActive = true;
                }
                return true;
            }
        }
        if (filteredEffects.size() > MAX_VISIBLE) {
            int sX = listX + listWidth + 2;
            int th = Math.max(20, listHeight * MAX_VISIBLE / filteredEffects.size());
            int ty = listY + (filteredEffects.size() > MAX_VISIBLE
                ? (listHeight - th) * scrollOffset / (filteredEffects.size() - MAX_VISIBLE) : 0);
            if (mx >= sX && mx <= sX + 8 && my >= ty && my <= ty + th) { isDragging = true; return true; }
            if (mx >= sX && mx <= sX + 8 && my >= listY && my <= listY + listHeight) {
                updateScrollFromMouse(my, listY, listHeight, th); return true;
            }
        }
        return false;
    }

    private boolean handleEnchantListClick(double mx, double my, int btn) {
        int bs = 14;
        for (int i = 0; i < MAX_VISIBLE && (i + enchantScrollOffset) < filteredEnchants.size(); i++) {
            int idx = i + enchantScrollOffset;
            EnchantInfo info = filteredEnchants.get(idx);
            int y = listY + 5 + i * 20;
            int minusX = listX + listWidth - bs - 4;
            int plusX = minusX - bs - 2;
            if (mx >= minusX && mx <= minusX + bs && my >= y + 2 && my <= y + 2 + bs) {
                int curLv = enchantLevelAdjustments.getOrDefault(info.enchantment, 0);
                if (curLv > 0) { setEnchantLevelAdjustment(info.enchantment, curLv - 1); if (enchantLevelBox != null && selectedEnchant == info) enchantLevelBox.setValue(String.valueOf(curLv - 1)); }
                return true;
            }
            if (mx >= plusX && mx <= plusX + bs && my >= y + 2 && my <= y + 2 + bs) {
                int curLv = enchantLevelAdjustments.getOrDefault(info.enchantment, 0);
                if (curLv < info.maxLevel) { setEnchantLevelAdjustment(info.enchantment, curLv + 1); if (enchantLevelBox != null && selectedEnchant == info) enchantLevelBox.setValue(String.valueOf(curLv + 1)); }
                return true;
            }
            int nameRight = plusX - 2;
            // Right-click on name area -> copy enchant ID
            if (btn == 1 && mx >= listX && mx <= nameRight && my >= y && my <= y + 20) {
                ResourceLocation eid = ForgeRegistries.ENCHANTMENTS.getKey(info.enchantment);
                if (eid != null) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(eid.toString());
                    tooltipText = "\u00a7a" + Component.translatable("gui.potionenchant.copied", eid.toString()).getString();
                    tooltipTimer = 80;
                }
                return true;
            }
            if (mx >= listX && mx <= nameRight - 55 && my >= y && my <= y + 20) {
                selectedEnchant = info; selectedEffect = null;
                levelEditActive = false;
                if (enchantLevelBox != null) enchantLevelBox.setVisible(false);
                return true;
            }
            // Click on level number -> open level edit box
            if (mx >= nameRight - 55 && mx <= nameRight && my >= y && my <= y + 20) {
                selectedEnchant = info; selectedEffect = null;
                if (enchantLevelBox != null) {
                    int clv = enchantLevelAdjustments.getOrDefault(info.enchantment, 0);
                    enchantLevelBox.setValue(String.valueOf(clv));
                    enchantLevelBox.setVisible(true);
                    enchantLevelBox.setFocused(true);
                    setFocused(enchantLevelBox);
                    levelEditActive = true;
                }
                return true;
            }
        }
        if (filteredEnchants.size() > MAX_VISIBLE) {
            int sX = listX + listWidth + 2;
            int th = Math.max(20, listHeight * MAX_VISIBLE / filteredEnchants.size());
            int ty = listY + (filteredEnchants.size() > MAX_VISIBLE
                ? (listHeight - th) * enchantScrollOffset / (filteredEnchants.size() - MAX_VISIBLE) : 0);
            if (mx >= sX && mx <= sX + 8 && my >= ty && my <= ty + th) { isDragging = true; return true; }
            if (mx >= sX && mx <= sX + 8 && my >= listY && my <= listY + listHeight) {
                updateScrollFromMouse(my, listY, listHeight, th); return true;
            }
        }
        return false;
    }

    private void updateScrollFromMouse(double my, int ly, int lh, int th) {
        List<?> src = currentMode == PanelMode.POTION ? (List<?>)filteredEffects : (List<?>)filteredEnchants;
        if (src.size() <= MAX_VISIBLE) return;
        int maxSc = src.size() - MAX_VISIBLE;
        if (maxSc <= 0) return;
        double ratio = Math.max(0, Math.min(1, (my - ly - th / 2.0) / (lh - th)));
        int nv = (int)Math.round(ratio * maxSc);
        if (currentMode == PanelMode.POTION) scrollOffset = Math.max(0, Math.min(maxSc, nv));
        else enchantScrollOffset = Math.max(0, Math.min(maxSc, nv));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (zoom.dragging) { zoom.updateFromMouse(my, height); return true; }
        double origMX = mx, origMY = my;
        mx = zoom.mx(mx, width);
        my = zoom.my(my, height);
        if (isDragging) {
            List<?> src = currentMode == PanelMode.POTION ? (List<?>)filteredEffects : (List<?>)filteredEnchants;
            if (src.size() > MAX_VISIBLE) {
                int th = Math.max(20, listHeight * MAX_VISIBLE / src.size());
                updateScrollFromMouse(my, listY, listHeight, th);
            }
            return true;
        }
        CategoryBar ab = currentMode == PanelMode.POTION ? categoryBar : enchantCategoryBar;
        if (ab != null && ab.mouseDragged(origMX)) return true;
        return super.mouseDragged(origMX, origMY, btn, dx, dy);
    }
    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        zoom.dragging = false;
        double origMX = mx, origMY = my;
        mx = zoom.mx(mx, width);
        my = zoom.my(my, height);
        CategoryBar ab = currentMode == PanelMode.POTION ? categoryBar : enchantCategoryBar;
        if (ab != null) ab.mouseReleased();
        return super.mouseReleased(origMX, origMY, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= width - 32) { zoom.scroll(delta); return true; }
        double origMX = mx, origMY = my;
        mx = zoom.mx(mx, width);
        my = zoom.my(my, height);
        String[] fk = {"ranged_attack","auto_ranged_attack","destruction_mode","flight_mode"};
        if (mx >= listX && mx <= listX + listWidth && my >= statsY && my <= statsY + statsHeight) {
            List<String> lines = currentMode == PanelMode.POTION
                ? getPotionStatsLines() : getEnchantStatsLines();
            int maxVis = (statsHeight - 25) / 10;
            int maxSc = Math.max(0, lines.size() - maxVis);
            if (maxSc > 0) {
                int v = (currentMode == PanelMode.POTION ? descScrollOffset : enchantDescScrollOffset) + (delta > 0 ? -1 : 1);
                int nv = Math.max(0, Math.min(maxSc, v));
                if (currentMode == PanelMode.POTION) descScrollOffset = nv;
                else enchantDescScrollOffset = nv;
                return true;
            }
        }
        if (mx >= featureX && mx <= featureX + featureWidth && my >= listY && my <= listY + featurePanelHeight) {
            int maxVisF = Math.max(1, (featurePanelHeight - 25) / 25);
            int maxFs = Math.max(0, fk.length - maxVisF);
            if (maxFs > 0) {
                featureScrollOffset = Math.max(0, Math.min(maxFs,
                    featureScrollOffset + (delta > 0 ? -1 : 1)));
                return true;
            }
        }
        List<?> src = currentMode == PanelMode.POTION ? (List<?>)filteredEffects : (List<?>)filteredEnchants;
        if (src.size() > MAX_VISIBLE) {
            int v = (currentMode == PanelMode.POTION ? scrollOffset : enchantScrollOffset) + (delta > 0 ? -1 : 1);
            int nv = Math.max(0, Math.min(src.size() - MAX_VISIBLE, v));
            if (currentMode == PanelMode.POTION) scrollOffset = nv;
            else enchantScrollOffset = nv;
            return true;
        }
        CategoryBar ab = currentMode == PanelMode.POTION ? categoryBar : enchantCategoryBar;
        if (ab != null && ab.mouseScrolled(origMX, origMY, delta)) return true;
        return super.mouseScrolled(origMX, origMY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (zoom.editBox.isFocused()) return zoom.editBox.keyPressed(keyCode, scanCode, modifiers);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
            if (potionLevelBox != null) { potionLevelBox.setVisible(false); }
            if (enchantLevelBox != null) { enchantLevelBox.setVisible(false); }
            levelEditActive = false;
            return true;
        }
        if (searchBox.isFocused()) return searchBox.keyPressed(keyCode, scanCode, modifiers);
        if (bottleEditBox.isFocused()) return bottleEditBox.keyPressed(keyCode, scanCode, modifiers);
        if (potionLevelBox != null && potionLevelBox.isFocused()) return potionLevelBox.keyPressed(keyCode, scanCode, modifiers);
        if (enchantLevelBox != null && enchantLevelBox.isFocused()) return enchantLevelBox.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char cp, int mod) {
        if (zoom.editBox.isFocused()) return zoom.editBox.charTyped(cp, mod);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.charTyped(cp, mod);
        if (searchBox.isFocused()) return searchBox.charTyped(cp, mod);
        if (bottleEditBox.isFocused()) return bottleEditBox.charTyped(cp, mod);
        if (potionLevelBox != null && potionLevelBox.isFocused()) return potionLevelBox.charTyped(cp, mod);
        if (enchantLevelBox != null && enchantLevelBox.isFocused()) return enchantLevelBox.charTyped(cp, mod);
        return super.charTyped(cp, mod);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        zoom.saveToConfig();
        super.onClose();
    }
    @Override public void removed() { zoom.saveToConfig(); super.removed(); }

    private static class MobEffectInfo {
        final MobEffect effect;
        final ResourceLocation key;
        final String name;
        final String description;
        final boolean isBeneficial;
        MobEffectInfo(MobEffect effect, ResourceLocation key, String name, String description, boolean isBeneficial) {
            this.effect = effect; this.key = key; this.name = name;
            this.description = description; this.isBeneficial = isBeneficial;
        }
    }

    private static class EnchantInfo {
        final Enchantment enchantment;
        final int maxLevel;
        final String id;
        EnchantInfo(Enchantment e, int max) {
            this.enchantment = e; this.maxLevel = max;
            net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(e);
            this.id = rl != null ? rl.toString() : "";
        }
    }

    private int getCurrentEnchantLevel(Enchantment e) {
        if (minecraft == null || minecraft.player == null) return 0;
        net.minecraft.world.item.ItemStack helmet = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return 0;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(e, helmet);
    }

    private boolean isEffectVisible(MobEffectInfo info) {
        int idx = filteredEffects.indexOf(info);
        return idx >= scrollOffset && idx < scrollOffset + MAX_VISIBLE;
    }

    private boolean isEnchantVisible(EnchantInfo info) {
        int idx = filteredEnchants.indexOf(info);
        return idx >= enchantScrollOffset && idx < enchantScrollOffset + MAX_VISIBLE;
    }
}
