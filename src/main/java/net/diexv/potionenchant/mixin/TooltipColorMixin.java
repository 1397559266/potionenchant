package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.item.XSwordItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.joml.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

/**
 * 统一 Tooltip 颜色 Mixin
 * - 药水物品：白色边框 + 透明背景
 * - XSword supermode：彩虹边框 + 半透明彩虹背景
 */
@Mixin(RenderTooltipEvent.Color.class)
public abstract class TooltipColorMixin {

    @Unique
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    @Unique
    private static final int TRANSPARENT_COLOR = 0x00000000;
    @Unique
    private static final Random RANDOM = new Random();

    // ==================== Border ====================

    @Inject(method = "getBorderStart", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideBorderStart(CallbackInfoReturnable<Integer> cir) {
        RenderTooltipEvent.Color event = (RenderTooltipEvent.Color)(Object)this;
        ItemStack stack = event.getItemStack();
        if (stack == null) return;

        // XSword supermode: 彩虹边框
        if (stack.getItem() == ModItems.X_SWORD.get()) {
            Player player = Minecraft.getInstance().player;
            if (player != null && XSwordItem.isSupermode(player.getUUID())) {
                cir.setReturnValue(rainbowColor() | 0xFF000000);
                return;
            }
        }

        // 药水物品: 白色边框
        if (isPotionItem(stack)) {
            cir.setReturnValue(WHITE_COLOR);
        }
    }

    @Inject(method = "getBorderEnd", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideBorderEnd(CallbackInfoReturnable<Integer> cir) {
        RenderTooltipEvent.Color event = (RenderTooltipEvent.Color)(Object)this;
        ItemStack stack = event.getItemStack();
        if (stack == null) return;

        if (stack.getItem() == ModItems.X_SWORD.get()) {
            Player player = Minecraft.getInstance().player;
            if (player != null && XSwordItem.isSupermode(player.getUUID())) {
                cir.setReturnValue(rainbowColor() | 0xFF000000);
                return;
            }
        }

        if (isPotionItem(stack)) {
            cir.setReturnValue(WHITE_COLOR);
        }
    }

    // ==================== Background ====================

    @Inject(method = "getBackgroundStart", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideBackgroundStart(CallbackInfoReturnable<Integer> cir) {
        RenderTooltipEvent.Color event = (RenderTooltipEvent.Color)(Object)this;
        ItemStack stack = event.getItemStack();
        if (stack == null) return;

        if (stack.getItem() == ModItems.X_SWORD.get()) {
            Player player = Minecraft.getInstance().player;
            if (player != null && XSwordItem.isSupermode(player.getUUID())) {
                cir.setReturnValue((rainbowColor() & 0x00FFFFFF) | 0x77000000);
                return;
            }
        }

        if (isPotionItem(stack)) {
            cir.setReturnValue(TRANSPARENT_COLOR);
        }
    }

    @Inject(method = "getBackgroundEnd", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideBackgroundEnd(CallbackInfoReturnable<Integer> cir) {
        RenderTooltipEvent.Color event = (RenderTooltipEvent.Color)(Object)this;
        ItemStack stack = event.getItemStack();
        if (stack == null) return;

        if (stack.getItem() == ModItems.X_SWORD.get()) {
            Player player = Minecraft.getInstance().player;
            if (player != null && XSwordItem.isSupermode(player.getUUID())) {
                cir.setReturnValue((rainbowColor() & 0x00FFFFFF) | 0x55000000);
                return;
            }
        }

        if (isPotionItem(stack)) {
            cir.setReturnValue(TRANSPARENT_COLOR);
        }
    }

    // ==================== Helpers ====================

    @Unique
    private static boolean isPotionItem(ItemStack stack) {
        return stack.getItem() == ModItems.ULTIMATE_POTION_AMULET.get() ||
               stack.getItem() == ModItems.MYSTERIOUS_EMPTY_BOTTLE.get() ||
               stack.getItem() == ModItems.UNIVERSAL_POTION_BOTTLE.get();
    }

    @Unique
    private static int rainbowColor() {
        float hue = RANDOM.nextFloat();
        return Color.HSBtoRGB(hue, 0.9f, 0.9f);
    }
}