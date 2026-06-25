package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.diexv.potionenchant.util.TooltipScrollState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MouseHandler.class)
public class TooltipScrollMouseMixin {

    @Unique
    private static long lastScrollTime = 0;

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;

        if (!PotionEnchantConfig.SERVER.enablePotionEnchantTooltip.get()) return;

        boolean shiftPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shiftPressed) return;

        ItemStack stack = getHoveredStack(mc);
        if (stack == null || stack.isEmpty()) return;

        if (!PotionEnchantManager.hasPotionEnchantments(stack)) return;

        List<net.diexv.potionenchant.data.PotionEnchantData> enchantments =
                PotionEnchantManager.getPotionEnchantments(stack);
        int totalEffects = enchantments.size();
        if (totalEffects <= TooltipScrollState.getMaxVisibleEffects()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScrollTime < 50) return;
        lastScrollTime = currentTime;

        double scrollDelta = -yOffset;
        if (scrollDelta != 0) {
            TooltipScrollState.setTotalEffectLines(totalEffects);
            int direction = scrollDelta > 0 ? 1 : -1;
            TooltipScrollState.adjustScrollOffset(direction);
        }
    }

    @Unique
    private static ItemStack getHoveredStack(Minecraft mc) {
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = containerScreen.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) {
                return slot.getItem();
            }
        }
        if (mc.player != null) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (!mainHand.isEmpty()) return mainHand;
            ItemStack offHand = mc.player.getOffhandItem();
            if (!offHand.isEmpty()) return offHand;
        }
        return ItemStack.EMPTY;
    }
}

