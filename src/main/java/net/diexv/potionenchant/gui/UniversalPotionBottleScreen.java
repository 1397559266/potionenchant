package net.diexv.potionenchant.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.network.UniversalBottlePacketHandler;
import net.diexv.potionenchant.gui.GuiZoom;
import net.diexv.potionenchant.util.PotionEnchantManager;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.diexv.potionenchant.gui.CategoryBar;

/**
 * 万能药水附魔瓶GUI
 * 显示所有药水效果，支持搜索和选择
 */
public class UniversalPotionBottleScreen extends Screen {
    
    protected final ItemStack targetItem;
    protected final ItemStack bottleItem;
    
    private EditBox searchBox;
    private final GuiZoom zoom = new GuiZoom("universal_potion_bottle");
    protected Button confirmButton;
    private Button cancelButton;
    
    // 等级输入编辑框（用于直接输入等级）
    private EditBox levelEditBox;
    private MobEffectInfo editingEffect; // 当前正在编辑等级的效果
    
    private List<MobEffectInfo> allEffects;
    private List<MobEffectInfo> filteredEffects;
    private MobEffectInfo selectedEffect;
    
    // 批量附魔：记录每个效果的调整等级（相对于已有等级的增量）
    protected java.util.Map<MobEffect, Integer> levelAdjustments = new java.util.HashMap<>();
    
    private int scrollOffset = 0;
    private final int MAX_VISIBLE = 10;
    private boolean isDragging = false;
    
    // 右侧描述面板的滚动
    private int descScrollOffset = 0;
    private boolean isDescDragging = false;
    private CategoryBar categoryBar;
    
    // 面板显示模式：true=显示单个药水描述，false=显示批量统计
    private boolean showSingleEffectMode = true;
    
    public UniversalPotionBottleScreen(ItemStack targetItem, ItemStack bottleItem) {
        super(Component.translatable("gui.potionenchant.universal_potion_bottle"));
        this.targetItem = targetItem;
        this.bottleItem = bottleItem;
        this.allEffects = new ArrayList<>();
        this.filteredEffects = new ArrayList<>();
        this.selectedEffect = null;
    }
    
    @Override
    protected void init() {
        // 加载所有药水效果
        loadAllEffects();
        zoom.init(font, width, height);
        
        // 初始化搜索框
        int searchBoxWidth = 200;
        int searchBoxX = (width - searchBoxWidth) / 2;
        searchBox = new EditBox(font, searchBoxX, 30, searchBoxWidth, 20, Component.translatable("gui.potionenchant.search"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(this::onSearchTextChanged);
        addRenderableWidget(searchBox);
        
        // 初始化确认按钮
        confirmButton = Button.builder(
            Component.translatable("gui.potionenchant.confirm"),
            button -> onConfirm()
        ).bounds((width - 200) / 2 - 55, height - 40, 100, 20).build();
        confirmButton.active = false;
        addRenderableWidget(confirmButton);
        
        // 初始化取消按钮
        cancelButton = Button.builder(
            Component.translatable("gui.potionenchant.cancel"),
            button -> onClose()
        ).bounds((width - 200) / 2 + 55, height - 40, 100, 20).build();
        addRenderableWidget(cancelButton);
        
        // 初始化等级输入编辑框（初始隐藏，点击+/-按钮时显示）
        levelEditBox = new EditBox(font, 0, 0, 30, 14, Component.translatable("gui.potionenchant.level_input"));
        levelEditBox.setMaxLength(10);
        levelEditBox.setVisible(false);
        levelEditBox.setResponder(this::onLevelInputChanged);
        // levelEditBox is rendered manually inside zoom - do not add as renderable widget

        categoryBar = new CategoryBar("gui.potionenchant.category",
            new String[]{"all","beneficial","harmful","neutral"}, 50, 16, 4);
        categoryBar.init(Math.max(2, (width - 450) / 2), 55);
    }
    
    /**
     * 获取某个效果的目标等级（用于显示和编辑）
     */
    private int getLevelAdjustment(MobEffect effect) {
        // 返回目标等级，而不是增量
        return levelAdjustments.getOrDefault(effect, getExistingEnchantmentLevel(effect));
    }
    
    /**
     * 当等级输入框内容改变时调用
     */
    private void onLevelInputChanged(String text) {
        if (editingEffect == null || text.isEmpty()) {
            return;
        }
        
        try {
            int newLevel = Integer.parseInt(text);
            // 不允许负数
            if (newLevel < 0) {
                newLevel = 0;
                levelEditBox.setValue("0");
            }
            
            // 检查是否超过等级上限
            boolean isUltimateAmulet = targetItem.getItem() == ModItems.ULTIMATE_POTION_AMULET.get();
            if (!isUltimateAmulet) {
                int maxLevel = PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get();
                if (newLevel > maxLevel) {
                    newLevel = maxLevel;
                    levelEditBox.setValue(String.valueOf(newLevel));
                }
            }
            
            // 直接设置目标等级
            setLevelAdjustment(editingEffect.effect, newLevel);
        } catch (NumberFormatException e) {
            // 忽略无效输入
        }
    }
    
    /**
     * 设置某个效果的目标等级
     */
    private void setLevelAdjustment(MobEffect effect, int targetLevel) {
        // targetLevel 是目标等级，0表示移除
        // 如果目标等级等于当前等级，则移除该调整
        int existingLevel = getExistingEnchantmentLevel(effect);
        if (targetLevel == existingLevel) {
            levelAdjustments.remove(effect);
        } else {
            levelAdjustments.put(effect, targetLevel);
        }
        updateConfirmButton();
    }
    
    /**
     * 计算总共需要的瓶子数量
     * 只计算升级的情况（最终等级 > 当前等级）
     */
    protected int getTotalRequiredBottles() {
        int total = 0;
        for (var entry : levelAdjustments.entrySet()) {
            MobEffect effect = entry.getKey();
            int targetLevel = entry.getValue(); // 这是目标等级
            int existingLevel = getExistingEnchantmentLevel(effect);
            
            // 只有当目标等级大于当前等级时才消耗瓶子
            if (targetLevel > existingLevel) {
                total += (targetLevel - existingLevel);
            }
        }
        return total;
    }

    /**
     * 获取玩家背包内所有万能药水附魔瓶的总数
     */
    protected int getTotalBottleCount() {
        if (minecraft == null || minecraft.player == null) return 0;
        int total = 0;
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (stack.getItem() == net.diexv.potionenchant.item.ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 检查是否有足够的瓶子
     */
    protected boolean hasEnoughBottles() {
        if (minecraft == null || minecraft.player == null) return false;
        if (minecraft.player.isCreative()) return true; // 创造模式不需要消耗
        
        int required = getTotalRequiredBottles();
        return getTotalBottleCount() >= required;
    }
    
    private void loadAllEffects() {
        allEffects.clear();
        
        ForgeRegistries.MOB_EFFECTS.getValues().stream()
            .sorted(Comparator.comparing(effect -> effect.getDisplayName().getString()))
            .forEach(effect -> {
                ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                if (key != null && !PotionEnchantConfig.getBlacklistedEffects().contains(key)) {
                    String name = effect.getDisplayName().getString();
                    String descriptionKey = "effect." + key.getNamespace() + "." + key.getPath() + ".description";
                    String description = net.minecraft.client.resources.language.I18n.get(descriptionKey, "");
                    
                    // 如果没有描述，使用默认描述
                    if (description.isEmpty() || description.equals(descriptionKey)) {
                        description = Component.translatable("gui.potionenchant.no_description").getString();
                    }
                    
                    // 判断是否为增益效果
                    boolean isBeneficial = effect.isBeneficial();
                    
                    allEffects.add(new MobEffectInfo(effect, key, name, description, isBeneficial));
                }
            });
        
        filteredEffects = allEffects.stream().filter(this::matchesCategory).collect(Collectors.toList());
    }
    
    private void onSearchTextChanged(String text) {
        if (text == null || text.trim().isEmpty()) {
            filteredEffects = allEffects.stream().filter(this::matchesCategory).collect(Collectors.toList());
        } else {
            String lowerText = text.toLowerCase();
            filteredEffects = allEffects.stream()
                .filter(info -> info.name.toLowerCase().contains(lowerText) || 
                               info.description.toLowerCase().contains(lowerText))
                .filter(this::matchesCategory)
                .collect(Collectors.toList());
        }
        scrollOffset = 0;
        selectedEffect = null;
        // 搜索时重置为批量统计模式
        showSingleEffectMode = false;
        descScrollOffset = 0;
        updateConfirmButton();
    }
    
    protected void updateConfirmButton() {
        // 有调整且瓶子足够才能确认
        confirmButton.active = !levelAdjustments.isEmpty() && hasEnoughBottles();
    }
    
    protected void onConfirm() {
        if (levelAdjustments.isEmpty() || minecraft == null || minecraft.player == null) {
            return;
        }
        
        if (!hasEnoughBottles()) {
            // 瓶子不足，显示提示
            minecraft.player.displayClientMessage(
                Component.translatable("gui.potionenchant.not_enough_bottles", 
                    getTotalRequiredBottles(), bottleItem.getCount()),
                true
            );
            return;
        }
        
        // 构建要发送的数据包
        CompoundTag enchantData = new CompoundTag();
        ListTag effectsList = new ListTag();
        
        int appliedCount = 0;
        
        // 收集所有调整的附魔数据（包括降级和升级）
        for (var entry : levelAdjustments.entrySet()) {
            MobEffect effect = entry.getKey();
            int targetLevel = entry.getValue(); // 目标等级
            int existingLevel = getExistingEnchantmentLevel(effect);
            
            // 跳过没有变化的
            if (targetLevel == existingLevel) continue;
            
            // 创建效果标签
            CompoundTag effectTag = new CompoundTag();
            ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
            if (effectId != null) {
                effectTag.putString("Effect", effectId.toString());
                effectTag.putInt("TargetLevel", targetLevel); // 使用TargetLevel字段，存储目标等级
                effectsList.add(effectTag);
                appliedCount++;
            }
        }
        
        enchantData.put("Effects", effectsList);
        enchantData.putInt("AppliedCount", appliedCount);
        enchantData.putInt("BottlesConsumed", getTotalRequiredBottles());
        
        // 确定目标物品的槽位（-1表示副手）
        int slotIndex = -1;
        
        // 发送数据包到服务器
        UniversalBottlePacketHandler.INSTANCE.sendToServer(
            new UniversalBottlePacketHandler.ApplyPotionEnchantPacket(enchantData, slotIndex)
        );
        
        // 关闭屏幕
        onClose();
    }
    
    /**
     * 获取目标物品上已有的药水附魔等级
     * @return 已有的等级（amplifier + 1），如果没有则返回 0
     */
    protected int getExistingEnchantmentLevel(net.minecraft.world.effect.MobEffect effect) {
        var enchantments = PotionEnchantManager.getPotionEnchantments(targetItem);
        for (var enchant : enchantments) {
            if (enchant.getEffect() == effect) {
                return enchant.getAmplifier() + 1; // 转换为等级（1-based）
            }
        }
        return 0;
    }
    
    /**
     * 检查是否可以继续提升等级
     * @param effect 药水效果
     * @param currentTargetLevel 当前目标等级
     * @return 是否可以继续提升
     */
    private boolean canIncreaseLevel(MobEffect effect, int currentTargetLevel) {
        // 检查目标物品是否是终极药水护符
        boolean isUltimateAmulet = targetItem.getItem() == ModItems.ULTIMATE_POTION_AMULET.get();
        
        // 终极药水护符不受等级限制
        if (isUltimateAmulet) {
            return true;
        }
        
        // 获取配置的最大等级限制
        int maxLevel = PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get();
        
        // 检查是否超过限制
        return currentTargetLevel < maxLevel;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int scrMX = mouseX, scrMY = mouseY;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 绘制标题
        guiGraphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        zoom.renderHeaderZoom(guiGraphics, font, width / 2 + 60, 6, 50, scrMX, scrMY, partialTick);
        
        // 绘制副手物品信息
        String itemInfo = Component.translatable("gui.potionenchant.target_item", targetItem.getHoverName().getString()).getString();
        
        // 分类按钮
        if (categoryBar != null) categoryBar.render(guiGraphics, font, mouseX, mouseY, width, height);
        
        zoom.push(guiGraphics, width, height);
        mouseX = (int) zoom.mx(mouseX, width);
        mouseY = (int) zoom.my(mouseY, height);
        
        PoseStack poseStack = guiGraphics.pose();
        
        // 计算布局：列表在左侧，描述在右侧
        int listX = Math.max(2, (width - 450) / 2);  // 列表左移，总宽度从250改为200
        int listY = 70 + (categoryBar != null ? categoryBar.getHeight() : 0);
        int listWidth = 200;  // 缩小列表宽度
        int listHeight = MAX_VISIBLE * 20 + 5;
        
        int descX = listX + listWidth + 20;  // 描述在列表右侧，间隔20px
        int descY = listY;
        guiGraphics.drawString(font, itemInfo, descX, descY - 15, 0xAAAAAA);
        int descWidth = width - descX - listX;  // 描述区域宽度
        int descHeight = listHeight;  // 与列表同高

        // 绘制列表背景
        guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);
        
        // 绘制可见的药水效果
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredEffects.size(); i++) {
            int index = i + scrollOffset;
            MobEffectInfo info = filteredEffects.get(index);
            int y = listY + 5 + i * 20;
            
            // 高亮选中的项
            if (info == selectedEffect) {
                guiGraphics.fill(listX + 2, y - 2, listX + listWidth - 2, y + 18, 0x40FFFFFF);
            }
            
            // 获取已有等级和目标等级
            int existingLevel = getExistingEnchantmentLevel(info.effect);
            int targetLevel = getLevelAdjustment(info.effect);
            
            // 检查鼠标是否悬停在当前行（不包括按钮区域）
            boolean isRowHovered = mouseX >= listX && mouseX <= listX + listWidth - 50 &&
                                  mouseY >= y && mouseY <= y + 20;
            
            // 高亮悬停的行
            if (isRowHovered) {
                guiGraphics.fill(listX, y, listX + listWidth - 50, y + 20, 0x20FFFFFF);
            }
            
            // 绘制药水名称（限制宽度）- 留出空间给 +/- 按钮
            String displayName = info.name;
            int nameMaxWidth = listWidth - 60; // 留出 +/- 和等级的空间
            if (font.width(displayName) > nameMaxWidth) {
                displayName = font.plainSubstrByWidth(displayName, nameMaxWidth - 10) + "...";
            }
            guiGraphics.drawString(font, displayName, listX + 5, y, 0xFFFFFF);
            
            // 绘制 - 按钮
            int minusBtnX = listX + listWidth - 50;
            int minusBtnY = y + 2;
            int btnSize = 14;
            
            // 检查是否悬停在 - 按钮上
            boolean isMinusHovered = mouseX >= minusBtnX && mouseX <= minusBtnX + btnSize &&
                                    mouseY >= minusBtnY && mouseY <= minusBtnY + btnSize;
            
            // 只要有等级就可以减少
            boolean canDecrease = targetLevel > 0;
            
            guiGraphics.fill(minusBtnX, minusBtnY, minusBtnX + btnSize, minusBtnY + btnSize, 
                canDecrease ? 0xFF5555 : 0x666666);
            
            // 悬停时绘制边框
            if (isMinusHovered) {
                guiGraphics.fill(minusBtnX - 1, minusBtnY - 1, minusBtnX + btnSize + 1, minusBtnY, 0xFFFFFFFF);
                guiGraphics.fill(minusBtnX - 1, minusBtnY + btnSize, minusBtnX + btnSize + 1, minusBtnY + btnSize + 1, 0xFFFFFFFF);
                guiGraphics.fill(minusBtnX - 1, minusBtnY, minusBtnX, minusBtnY + btnSize, 0xFFFFFFFF);
                guiGraphics.fill(minusBtnX + btnSize, minusBtnY, minusBtnX + btnSize + 1, minusBtnY + btnSize, 0xFFFFFFFF);
            }
            
            guiGraphics.drawString(font, "-", minusBtnX + 5, minusBtnY + 3, 0xFFFFFF);
            
            // 绘制等级输入框（如果正在编辑这个效果）
            int inputBoxX = listX + listWidth - 33;
            int inputBoxY = y + 2;
            if (editingEffect == info && levelEditBox.isVisible()) {
                // 更新编辑框位置
                levelEditBox.setX(inputBoxX);
                levelEditBox.setY(inputBoxY);
                levelEditBox.setWidth(18);
                levelEditBox.setHeight(14);
                // 渲染编辑框
                levelEditBox.render(guiGraphics, mouseX, mouseY, partialTick);
            } else {
                // 显示目标等级
                if (targetLevel > 0) {
                    String levelText = String.valueOf(targetLevel);
                    // 如果有调整，根据升降级显示不同颜色
                    int levelColor;
                    if (targetLevel > existingLevel) {
                        levelColor = 0xFFFF55; // 黄色（升级）
                    } else if (targetLevel < existingLevel) {
                        levelColor = 0xFF5555; // 红色（降级）
                    } else {
                        levelColor = 0xAAAAAA; // 灰色（无变化）
                    }
                    guiGraphics.drawString(font, levelText, inputBoxX + 2, y + 3, levelColor);
                }
            }
            
            // 绘制 + 按钮（检查是否达到等级上限）
            int plusBtnX = listX + listWidth - 12;
            int plusBtnY = y + 2;
            boolean canIncrease = canIncreaseLevel(info.effect, targetLevel);
            
            // 检查是否悬停在 + 按钮上
            boolean isPlusHovered = mouseX >= plusBtnX && mouseX <= plusBtnX + btnSize &&
                                   mouseY >= plusBtnY && mouseY <= plusBtnY + btnSize;
            
            guiGraphics.fill(plusBtnX, plusBtnY, plusBtnX + btnSize, plusBtnY + btnSize, 
                canIncrease ? 0x55FF55 : 0x666666);
            
            // 悬停时绘制边框
            if (isPlusHovered) {
                guiGraphics.fill(plusBtnX - 1, plusBtnY - 1, plusBtnX + btnSize + 1, plusBtnY, 0xFFFFFFFF);
                guiGraphics.fill(plusBtnX - 1, plusBtnY + btnSize, plusBtnX + btnSize + 1, plusBtnY + btnSize + 1, 0xFFFFFFFF);
                guiGraphics.fill(plusBtnX - 1, plusBtnY, plusBtnX, plusBtnY + btnSize, 0xFFFFFFFF);
                guiGraphics.fill(plusBtnX + btnSize, plusBtnY, plusBtnX + btnSize + 1, plusBtnY + btnSize, 0xFFFFFFFF);
            }
            
            guiGraphics.drawString(font, "+", plusBtnX + 4, plusBtnY + 3, 0xFFFFFF);
        }
        
        // 绘制滚动条（向右移动，避免覆盖药水列表点击区域）
        if (filteredEffects.size() > MAX_VISIBLE) {
            int scrollbarX = listX + listWidth + 2;  // 向右移动10像素，放在列表外面
            int scrollbarY = listY;
            int scrollbarWidth = 8;
            int scrollbarHeight = listHeight;
            
            int thumbHeight = Math.max(20, scrollbarHeight * MAX_VISIBLE / filteredEffects.size());
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / (filteredEffects.size() - MAX_VISIBLE);
            
            // 绘制滚动条背景
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0x40000000);
            
            // 绘制滚动条滑块
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFFFFFFF);
        }
        
        // 绘制右侧面板（根据模式显示不同内容）
        if (showSingleEffectMode && selectedEffect != null) {
            // 显示单个药水描述
            drawSingleEffectDescription(guiGraphics, descX, descY, descWidth, descHeight);
        } else {
            // 显示批量统计信息
            drawBatchStats(guiGraphics, descX, descY, descWidth, descHeight);
        }
        
        // 在列表下方显示瓶子数量
        int required = getTotalRequiredBottles();
        int owned = getTotalBottleCount();
        String bottleInfo = Component.translatable("gui.potionenchant.bottle_count", owned, required).getString();
        int bottleColor = hasEnoughBottles() ? 0x55FF55 : (required > 0 ? 0xFF5555 : 0xAAAAAA);
        guiGraphics.drawCenteredString(font, bottleInfo, listX + listWidth / 2, listY + listHeight + 10, bottleColor);
        zoom.pop(guiGraphics);
        zoom.renderPanel(guiGraphics, font, scrMX, scrMY, width, height);
        zoom.editBox.render(guiGraphics, scrMX, scrMY, partialTick);
    }
    
    /**
     * 绘制单个药水效果描述
     */
    private void drawSingleEffectDescription(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 检查目标物品是否已有该附魔
        int existingLevel = getExistingEnchantmentLevel(selectedEffect.effect);
        int targetLevel = getLevelAdjustment(selectedEffect.effect);
        
        // 根据效果类型设置颜色：增益=绿色，减益=红色，中性=黄色
        int nameColor = selectedEffect.isBeneficial ? 0x55FF55 : (selectedEffect.isHarmful ? 0xFF5555 : 0xFFFF00);
        guiGraphics.drawCenteredString(font, selectedEffect.name, x + width / 2, y + 5, nameColor);
        
        // 显示等级提示
        if (targetLevel > existingLevel && existingLevel > 0) {
            String levelHint = Component.translatable("gui.potionenchant.level_upgrade", existingLevel, targetLevel).getString();
            guiGraphics.drawCenteredString(font, levelHint, x + width / 2, y + 15, 0xFFAA00);
        } else if (targetLevel > existingLevel) {
            String levelHint = Component.translatable("gui.potionenchant.new_enchantment", targetLevel).getString();
            guiGraphics.drawCenteredString(font, levelHint, x + width / 2, y + 15, 0xAAAAFF);
        } else if (targetLevel < existingLevel) {
            String levelHint = Component.translatable("gui.potionenchant.level_downgrade", existingLevel, targetLevel).getString();
            guiGraphics.drawCenteredString(font, levelHint, x + width / 2, y + 15, 0xFF5555);
        } else if (existingLevel > 0) {
            String levelHint = Component.translatable("tooltip.potionenchant.level") + ": " + existingLevel;
            guiGraphics.drawCenteredString(font, levelHint, x + width / 2, y + 15, 0xAAAAAA);
        }
        
        // 自动换行显示描述（限制每行宽度）
        List<String> wrappedLines = wrapTextByWidth(selectedEffect.description, width - 10);
        int maxVisibleLines = (height - 40) / 10;  // 留出空间给标题和等级
        int totalLines = wrappedLines.size();
        
        // 计算最大滚动偏移
        int maxDescScroll = Math.max(0, totalLines - maxVisibleLines);
        descScrollOffset = Math.min(descScrollOffset, maxDescScroll);
        
        // 绘制可见的行
        int startY = y + 27;
        for (int i = 0; i < maxVisibleLines && (i + descScrollOffset) < totalLines; i++) {
            int lineIndex = i + descScrollOffset;
            guiGraphics.drawString(font, wrappedLines.get(lineIndex), x + 5, startY + i * 10, 0xCCCCCC);
        }
        
        // 绘制描述区域的滚动条
        if (totalLines > maxVisibleLines) {
            int scrollbarX = x + width - 8;
            int scrollbarY = y + 25;
            int scrollbarWidth = 6;
            int scrollbarHeight = height - 30;
            
            int thumbHeight = Math.max(15, scrollbarHeight * maxVisibleLines / totalLines);
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * descScrollOffset / maxDescScroll;
            
            // 绘制滚动条背景
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0x40000000);
            
            // 绘制滚动条滑块
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFFFFFFF);
        }
    }
    
    /**
     * 绘制批量统计信息
     */
    private void drawBatchStats(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 标题
        guiGraphics.drawCenteredString(font, Component.translatable("gui.potionenchant.batch_stats").getString(), 
            x + width / 2, y + 5, 0xFFFF55);
        
        // 获取目标物品当前的所有药水附魔
        List<PotionEnchantData> currentEnchantments = PotionEnchantManager.getPotionEnchantments(targetItem);
        
        // 收集所有有调整的附魔
        List<String> statsLines = new ArrayList<>();
        int totalBottles = 0;
        
        // 首先显示当前已有的附魔（没有调整时）
        if (levelAdjustments.isEmpty() && !currentEnchantments.isEmpty()) {
            for (PotionEnchantData enchant : currentEnchantments) {
                String effectName = enchant.getEffect().getDisplayName().getString();
                int level = enchant.getAmplifier() + 1; // amplifier是0-based
                String line = String.format("§e%s: Lv.%d", effectName, level);
                statsLines.add(line);
            }
        } else if (!levelAdjustments.isEmpty()) {
            // 有调整时，显示等级变化
            for (var entry : levelAdjustments.entrySet()) {
                MobEffect effect = entry.getKey();
                int targetLevel = entry.getValue(); // 目标等级
                int existingLevel = getExistingEnchantmentLevel(effect);
                
                // 跳过没有变化的
                if (targetLevel == existingLevel) continue;
                
                // 获取效果名称
                String effectName = effect.getDisplayName().getString();
                
                // 格式：效果名称: 已有 -> 目标 (消耗X瓶)
                String line;
                if (targetLevel > existingLevel) {
                    // 升级
                    int cost = targetLevel - existingLevel;
                    if (existingLevel > 0) {
                        line = String.format("%s: §eLv.%d§r → §aLv.%d§r (§a+%d§r瓶)", effectName, existingLevel, targetLevel, cost);
                    } else {
                        line = String.format("%s: §aLv.%d§r (§a新附魔, %d瓶§r)", effectName, targetLevel, cost);
                    }
                    totalBottles += cost;
                } else {
                    // 降级或移除
                    if (targetLevel <= 0) {
                        line = String.format("%s: §eLv.%d§r → §c移除§r", effectName, existingLevel);
                    } else {
                        line = String.format("%s: §eLv.%d§r → §eLv.%d§r (§c降级§r)", effectName, existingLevel, targetLevel);
                    }
                }
                
                statsLines.add(line);
            }
        }
        
        if (statsLines.isEmpty()) {
            // 没有任何附魔时显示提示
            guiGraphics.drawCenteredString(font, "§7当前装备没有药水附魔", 
                x + width / 2, y + height / 2 - 5, 0x888888);
            return;
        }
        
        // 计算滚动
        int maxVisibleLines = (height - 30) / 10;
        int totalLines = statsLines.size();
        int maxScroll = Math.max(0, totalLines - maxVisibleLines);
        descScrollOffset = Math.min(descScrollOffset, maxScroll);
        
        // 绘制可见的统计行
        int startY = y + 20;
        for (int i = 0; i < maxVisibleLines && (i + descScrollOffset) < totalLines; i++) {
            int lineIndex = i + descScrollOffset;
            String line = statsLines.get(lineIndex);
            
            // 如果文本过长，截断并添加...
            if (font.width(line) > width - 15) {
                line = font.plainSubstrByWidth(line, width - 25) + "...";
            }
            
            guiGraphics.drawString(font, line, x + 5, startY + i * 10, 0xFFFFFF);
        }
        
        // 绘制总计（只在有调整时显示）
        if (!levelAdjustments.isEmpty() && totalBottles > 0) {
            String totalLine = Component.translatable("gui.potionenchant.total_required", totalBottles).getString();
            guiGraphics.drawString(font, totalLine, x + 5, y + height - 12, 0xFFFF55);
        }
        
        // 绘制统计区域的滚动条
        if (totalLines > maxVisibleLines) {
            int scrollbarX = x + width - 8;
            int scrollbarY = y + 18;
            int scrollbarWidth = 6;
            int scrollbarHeight = height - 35;
            
            int thumbHeight = Math.max(15, scrollbarHeight * maxVisibleLines / totalLines);
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * descScrollOffset / maxScroll;
            
            // 绘制滚动条背景
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0x40000000);
            
            // 绘制滚动条滑块
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFFFFFFF);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= width - 32) {
            if (zoom.editBox.isMouseOver(mouseX, mouseY)) { setFocused(zoom.editBox); return zoom.editBox.mouseClicked(mouseX, mouseY, button); }
            if (mouseY >= 50 && mouseY <= height - 20) { zoom.dragging = true; zoom.updateFromMouse(mouseY, height); return true; }
            return true;
        }
        double origMX = mouseX, origMY = mouseY;
        if (zoom.headerEditBox.isMouseOver(mouseX, mouseY)) {
            setFocused(zoom.headerEditBox); return zoom.headerEditBox.mouseClicked(mouseX, mouseY, button);
        }
        mouseX = zoom.mx(mouseX, width);
        mouseY = zoom.my(mouseY, height);
        if (categoryBar != null && categoryBar.mouseClicked(origMX, origMY)) {
            scrollOffset = 0;
            onSearchTextChanged(searchBox.getValue());
            return true;
        }

        if (levelEditBox.isVisible() && levelEditBox.isMouseOver(mouseX, mouseY) == false) {
            levelEditBox.setVisible(false);
            editingEffect = null;
        }
        
        int listX = Math.max(2, (width - 450) / 2);
        int listY = 70 + (categoryBar != null ? categoryBar.getHeight() : 0);
        int listWidth = 200;
        int listHeight = MAX_VISIBLE * 20 + 5;
        
        // 先检查是否点击了滚动条（优先级更高）
        if (filteredEffects.size() > MAX_VISIBLE) {
            int scrollbarX = listX + listWidth + 2;  // 与绘制位置保持一致
            int scrollbarY = listY;
            int scrollbarWidth = 8;
            
            int thumbHeight = Math.max(20, listHeight * MAX_VISIBLE / filteredEffects.size());
            int thumbY = scrollbarY + (listHeight - thumbHeight) * scrollOffset / (filteredEffects.size() - MAX_VISIBLE);
            
            // 检查是否点击了滚动条滑块
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                isDragging = true;
                return true;
            }
            
            // 检查是否点击了滚动条轨道（快速跳转）
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + listHeight) {
                updateScrollFromMouse(mouseY, scrollbarY, listHeight, thumbHeight);
                return true;
            }
        }
        
        // 检查是否点击了列表项中的 +/- 按钮
        for (int i = 0; i < MAX_VISIBLE && (i + scrollOffset) < filteredEffects.size(); i++) {
            int index = i + scrollOffset;
            MobEffectInfo info = filteredEffects.get(index);
            int y = listY + 5 + i * 20;
            
            int minusBtnX = listX + listWidth - 50;  // 与渲染位置保持一致
            int minusBtnY = y + 2;
            int plusBtnX = listX + listWidth - 12;  // 与渲染位置保持一致
            int plusBtnY = y + 2;
            int btnSize = 14;
            
            // 检查 - 按钮
            if (mouseX >= minusBtnX && mouseX <= minusBtnX + btnSize &&
                mouseY >= minusBtnY && mouseY <= minusBtnY + btnSize) {
                int currentTargetLevel = getLevelAdjustment(info.effect);
                // 减少目标等级（最低到0）
                if (currentTargetLevel > 0) {
                    setLevelAdjustment(info.effect, currentTargetLevel - 1);
                }
                // 点击 +/- 按钮时切换到批量统计模式
                showSingleEffectMode = false;
                descScrollOffset = 0; // 重置滚动
                // 隐藏编辑框
                levelEditBox.setVisible(false);
                editingEffect = null;
                return true;
            }
            
            // 检查等级输入框区域（点击后显示编辑框）
            int inputBoxX = listX + listWidth - 33;
            int inputBoxY = y + 2;
            if (mouseX >= inputBoxX && mouseX <= inputBoxX + 18 &&
                mouseY >= inputBoxY && mouseY <= inputBoxY + 14) {
                // 显示编辑框并设置当前目标等级
                editingEffect = info;
                int targetLevel = getLevelAdjustment(info.effect);
                levelEditBox.setValue(String.valueOf(targetLevel));
                levelEditBox.setX(inputBoxX);
                levelEditBox.setY(inputBoxY);
                levelEditBox.setWidth(18);
                levelEditBox.setHeight(14);
                levelEditBox.setVisible(true);
                levelEditBox.setFocused(true);
                // 切换到批量统计模式
                showSingleEffectMode = false;
                descScrollOffset = 0;
                return true;
            }
            
            // 检查 + 按钮
            if (mouseX >= plusBtnX && mouseX <= plusBtnX + btnSize &&
                mouseY >= plusBtnY && mouseY <= plusBtnY + btnSize) {
                int currentTargetLevel = getLevelAdjustment(info.effect);
                // 检查是否可以继续提升等级
                if (canIncreaseLevel(info.effect, currentTargetLevel)) {
                    setLevelAdjustment(info.effect, currentTargetLevel + 1);
                    // 点击 +/- 按钮时切换到批量统计模式
                    showSingleEffectMode = false;
                    descScrollOffset = 0; // 重置滚动
                    // 隐藏编辑框
                    levelEditBox.setVisible(false);
                    editingEffect = null;
                } else {
                    // 达到等级上限，显示提示
                    if (minecraft != null && minecraft.player != null) {
                        int maxLevel = PotionEnchantConfig.COMMON.maxPotionEnchantLevel.get();
                        minecraft.player.displayClientMessage(
                            Component.translatable("gui.potionenchant.level_limit_reached", maxLevel),
                            true
                        );
                    }
                }
                return true;
            }
        }
        
        // 再检查是否点击了列表项（排除滚动条区域和按钮区域）
        if (mouseX >= listX && mouseX < listX + listWidth - 50 && 
            mouseY >= listY && mouseY <= listY + listHeight) {
            
            int index = (int)((mouseY - listY - 5) / 20) + scrollOffset;
            if (index >= 0 && index < filteredEffects.size()) {
                selectedEffect = filteredEffects.get(index);
                // 点击列表项时切换到单个药水描述模式
                showSingleEffectMode = true;
                descScrollOffset = 0; // 重置滚动
                updateConfirmButton();
                return true;
            }
        }
        
        // 检查是否点击了右侧面板的滚动条
        int descX = listX + listWidth + 20;
        int descY = listY;
        int descWidth = width - descX - listX;
        int descHeight = listHeight;
        
        // 计算右侧面板的最大滚动量
        int maxDescScroll;
        if (selectedEffect != null) {
            // 描述模式
            List<String> wrappedLines = wrapTextByWidth(selectedEffect.description, descWidth - 10);
            int maxVisibleLines = (descHeight - 40) / 10;
            maxDescScroll = Math.max(0, wrappedLines.size() - maxVisibleLines);
        } else {
            // 统计模式 - 计算实际显示的行数
            List<PotionEnchantData> currentEnchantments = PotionEnchantManager.getPotionEnchantments(targetItem);
            int totalLines = 0;
            
            if (levelAdjustments.isEmpty() && !currentEnchantments.isEmpty()) {
                // 没有调整时，显示当前附魔列表
                totalLines = currentEnchantments.size();
            } else if (!levelAdjustments.isEmpty()) {
                // 有调整时，计算有变化的行数
                for (var entry : levelAdjustments.entrySet()) {
                    MobEffect effect = entry.getKey();
                    int targetLevel = entry.getValue();
                    int existingLevel = getExistingEnchantmentLevel(effect);
                    // 只计算有变化的
                    if (targetLevel != existingLevel) {
                        totalLines++;
                    }
                }
            }
            
            int maxVisibleLines = (descHeight - 30) / 10;
            maxDescScroll = Math.max(0, totalLines - maxVisibleLines);
        }
        
        if (maxDescScroll > 0) {
            int scrollbarX = descX + descWidth - 8;
            int scrollbarY = descY + 18;
            int scrollbarWidth = 6;
            int scrollbarHeight = descHeight - 35;
            
            int thumbHeight = Math.max(15, scrollbarHeight * Math.min(10, maxDescScroll + 10) / (maxDescScroll + 10));
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * descScrollOffset / maxDescScroll;
            
            // 检查是否点击了滚动条滑块
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                isDescDragging = true;
                return true;
            }
            
            // 检查是否点击了滚动条轨道（快速跳转）
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                updateDescScrollFromMouse(mouseY, scrollbarY, scrollbarHeight, thumbHeight, maxDescScroll);
                return true;
            }
        }
        
        return super.mouseClicked(origMX, origMY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (zoom.dragging) { zoom.updateFromMouse(mouseY, height); return true; }
        double origMX = mouseX, origMY = mouseY;
        mouseX = zoom.mx(mouseX, width);
        mouseY = zoom.my(mouseY, height);
        int listX = Math.max(2, (width - 450) / 2);
        int listY = 70 + (categoryBar != null ? categoryBar.getHeight() : 0);
        int listWidth = 200;
        int listHeight = MAX_VISIBLE * 20 + 5;
        int descX = listX + listWidth + 20;
        int descY = listY;
        int descWidth = width - descX - listX;
        int descHeight = listHeight;
        
        // 检查是否拖动左侧列表滚动条
        if (isDragging && filteredEffects.size() > MAX_VISIBLE) {
            int scrollbarX = listX + listWidth + 2;  // 与绘制位置保持一致
            int thumbHeight = Math.max(20, listHeight * MAX_VISIBLE / filteredEffects.size());
            updateScrollFromMouse(mouseY, listY, listHeight, thumbHeight);
            return true;
        }
        
        // 检查是否拖动右侧面板滚动条
        if (isDescDragging) {
            // 计算右侧面板的最大滚动量
            int maxDescScroll;
            if (selectedEffect != null) {
                // 描述模式
                List<String> wrappedLines = wrapTextByWidth(selectedEffect.description, descWidth - 10);
                int maxVisibleLines = (descHeight - 40) / 10;
                maxDescScroll = Math.max(0, wrappedLines.size() - maxVisibleLines);
            } else {
                // 统计模式 - 计算实际显示的行数
                List<PotionEnchantData> currentEnchantments = PotionEnchantManager.getPotionEnchantments(targetItem);
                int totalLines = 0;
                
                if (levelAdjustments.isEmpty() && !currentEnchantments.isEmpty()) {
                    // 没有调整时，显示当前附魔列表
                    totalLines = currentEnchantments.size();
                } else if (!levelAdjustments.isEmpty()) {
                    // 有调整时，计算有变化的行数
                    for (var entry : levelAdjustments.entrySet()) {
                        MobEffect effect = entry.getKey();
                        int targetLevel = entry.getValue();
                        int existingLevel = getExistingEnchantmentLevel(effect);
                        // 只计算有变化的
                        if (targetLevel != existingLevel) {
                            totalLines++;
                        }
                    }
                }
                
                int maxVisibleLines = (descHeight - 30) / 10;
                maxDescScroll = Math.max(0, totalLines - maxVisibleLines);
            }
            
            if (maxDescScroll > 0) {
                int scrollbarX = descX + descWidth + 2;
                int thumbHeight = Math.max(20, descHeight * Math.min(10, maxDescScroll + 10) / (maxDescScroll + 10));
                updateDescScrollFromMouse(mouseY, descY, descHeight, thumbHeight, maxDescScroll);
                return true;
            }
        }
        
        return super.mouseDragged(origMX, origMY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        zoom.dragging = false;
        double origMX = mouseX, origMY = mouseY;
        mouseX = zoom.mx(mouseX, width);
        mouseY = zoom.my(mouseY, height);
        isDescDragging = false;
        if (categoryBar != null) categoryBar.mouseReleased();
        return super.mouseReleased(origMX, origMY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= width - 32) { zoom.scroll(delta); return true; }
        double origMX = mouseX, origMY = mouseY;
        mouseX = zoom.mx(mouseX, width);
        mouseY = zoom.my(mouseY, height);
        int listX = Math.max(2, (width - 450) / 2);
        int listY = 70 + (categoryBar != null ? categoryBar.getHeight() : 0);
        int listWidth = 200;
        int listHeight = MAX_VISIBLE * 20 + 5;
        int descX = listX + listWidth + 20;
        int descY = listY;
        int descWidth = width - descX - listX;
        int descHeight = listHeight;
        // 检查鼠标是否在右侧面板区域
        if (mouseX >= descX && mouseX <= descX + descWidth &&
            mouseY >= descY && mouseY <= descY + descHeight) {
            
            // 计算右侧面板的最大滚动量
            int maxDescScroll;
            if (selectedEffect != null) {
                // 描述模式
                List<String> wrappedLines = wrapTextByWidth(selectedEffect.description, descWidth - 10);
                int maxVisibleLines = (descHeight - 40) / 10;
                maxDescScroll = Math.max(0, wrappedLines.size() - maxVisibleLines);
            } else {
                // 统计模式 - 计算实际显示的行数
                List<PotionEnchantData> currentEnchantments = PotionEnchantManager.getPotionEnchantments(targetItem);
                int totalLines = 0;
                
                if (levelAdjustments.isEmpty() && !currentEnchantments.isEmpty()) {
                    // 没有调整时，显示当前附魔列表
                    totalLines = currentEnchantments.size();
                } else if (!levelAdjustments.isEmpty()) {
                    // 有调整时，计算有变化的行数
                    for (var entry : levelAdjustments.entrySet()) {
                        MobEffect effect = entry.getKey();
                        int targetLevel = entry.getValue();
                        int existingLevel = getExistingEnchantmentLevel(effect);
                        // 只计算有变化的
                        if (targetLevel != existingLevel) {
                            totalLines++;
                        }
                    }
                }
                
                int maxVisibleLines = (descHeight - 30) / 10;
                maxDescScroll = Math.max(0, totalLines - maxVisibleLines);
            }
            
            if (maxDescScroll > 0) {
                if (delta > 0) {
                    descScrollOffset = Math.max(0, descScrollOffset - 1);
                } else {
                    descScrollOffset = Math.min(maxDescScroll, descScrollOffset + 1);
                }
                return true;
            }
        }
        
        // 否则滚动左侧列表
        if (filteredEffects.size() > MAX_VISIBLE) {
            if (delta > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(filteredEffects.size() - MAX_VISIBLE, scrollOffset + 1);
            }
            return true;
        }
        
        return super.mouseScrolled(origMX, origMY, delta);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (zoom.editBox.isFocused()) return zoom.editBox.keyPressed(keyCode, scanCode, modifiers);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.keyPressed(keyCode, scanCode, modifiers);
                if (levelEditBox.isVisible() && levelEditBox.isFocused()) {
            if (keyCode == 256) { // ESC键
                levelEditBox.setVisible(false);
                editingEffect = null;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter键或Keypad Enter
                levelEditBox.setVisible(false);
                editingEffect = null;
                return true;
            }
            return levelEditBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (zoom.editBox.isFocused()) return zoom.editBox.charTyped(codePoint, modifiers);
        if (zoom.headerEditBox.isFocused()) return zoom.headerEditBox.charTyped(codePoint, modifiers);
                if (levelEditBox.isVisible() && levelEditBox.isFocused()) {
            return levelEditBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    private void updateScrollFromMouse(double mouseY, int listY, int listHeight, int thumbHeight) {
        int scrollRange = filteredEffects.size() - MAX_VISIBLE;
        
        if (scrollRange > 0) {
            // 计算鼠标在滚动条区域内的相对位置
            double ratio = (mouseY - listY - thumbHeight / 2.0) / (listHeight - thumbHeight);
            ratio = Math.max(0.0, Math.min(1.0, ratio)); // 限制在 0-1 之间
            scrollOffset = (int)(ratio * scrollRange);
            scrollOffset = Math.max(0, Math.min(scrollRange, scrollOffset));
        }
    }
    
    private void updateDescScrollFromMouse(double mouseY, int descY, int descHeight, int thumbHeight, int maxScroll) {
        if (maxScroll > 0) {
            // 计算鼠标在滚动条区域内的相对位置
            double ratio = (mouseY - descY - thumbHeight / 2.0) / (descHeight - thumbHeight);
            ratio = Math.max(0.0, Math.min(1.0, ratio)); // 限制在 0-1 之间
            descScrollOffset = (int)(ratio * maxScroll);
            descScrollOffset = Math.max(0, Math.min(maxScroll, descScrollOffset));
        }
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }
        
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");
        
        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * 按字符宽度自动换行（支持中英文）
     */
    private List<String> wrapTextByWidth(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }
        
        // 先按换行符分割
        String[] paragraphs = text.split("\\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            
            StringBuilder currentLine = new StringBuilder();
            
            for (int i = 0; i < paragraph.length(); i++) {
                char c = paragraph.charAt(i);
                String testLine = currentLine.toString() + c;
                
                if (font.width(testLine) <= maxWidth) {
                    currentLine.append(c);
                } else {
                    // 当前行已满，保存并换行
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine.setLength(0);
                    }
                    
                    // 如果单个字符就超过最大宽度，强制添加
                    if (font.width(String.valueOf(c)) <= maxWidth) {
                        currentLine.append(c);
                    } else {
                        lines.add(String.valueOf(c));
                    }
                }
            }
            
            // 添加最后一行
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
    
    private boolean matchesCategory(MobEffectInfo info) {
        if (categoryBar == null) return true;
        String f = categoryBar.getFilter();
        if (f.equals("all")) return true;
        switch (f) {
            case "beneficial": return info.isBeneficial;
            case "harmful": return info.isHarmful;
            case "neutral": return !info.isBeneficial && !info.isHarmful;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

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
        final boolean isHarmful;
        
        MobEffectInfo(MobEffect effect, ResourceLocation key, String name, String description, boolean isBeneficial) {
            this.effect = effect;
            this.key = key;
            this.name = name;
            this.description = description;
            this.isBeneficial = isBeneficial;
            // 减益效果：不是增益且不是中性
            this.isHarmful = !isBeneficial && effect.getCategory() != net.minecraft.world.effect.MobEffectCategory.NEUTRAL;
        }
    }
}