package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RenderTooltipEvent.Color.class})
public abstract class TooltipColorMixin2 {
   @Unique
   private static final int BLUE_COLOR = -11549705;
   @Unique
   private static final int WHITE_COLOR = -1;
   @Unique
   private static final long COLOR_CYCLE_TIME = 3000L;

   @Unique
   private int getCurrentGradientColor() {
      float progress = (float)(System.currentTimeMillis() % 3000L) / 3000.0F;
      float lerpFactor = (float)Math.sin((double)progress * Math.PI * (double)2.0F) * 0.5F + 0.5F;
      return this.lerpColor(-11549705, -1, lerpFactor);
   }

   @Unique
   private int lerpColor(int startColor, int endColor, float factor) {
      int r = (int)((float)(startColor >> 16 & 255) * (1.0F - factor) + (float)(endColor >> 16 & 255) * factor);
      int g = (int)((float)(startColor >> 8 & 255) * (1.0F - factor) + (float)(endColor >> 8 & 255) * factor);
      int b = (int)((float)(startColor & 255) * (1.0F - factor) + (float)(endColor & 255) * factor);
      return -16777216 | r << 16 | g << 8 | b;
   }

   @Inject(
      method = {"getBorderStart"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void overrideBorderStart(CallbackInfoReturnable<Integer> cir) {
      if (this.isTargetItem()) {
         cir.setReturnValue(this.getCurrentGradientColor());
      }

   }

   @Inject(
      method = {"getBorderEnd"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void overrideBorderEnd(CallbackInfoReturnable<Integer> cir) {
      if (this.isTargetItem()) {
         long offsetTime = (System.currentTimeMillis() + 750L) % 3000L;
         float progress = (float)offsetTime / 3000.0F;
         float lerpFactor = (float)Math.sin((double)progress * Math.PI * (double)2.0F) * 0.5F + 0.5F;
         cir.setReturnValue(this.lerpColor(-11549705, -1, lerpFactor));
      }

   }

   @Inject(
      method = {"getBackgroundStart"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void overrideBackgroundStart(CallbackInfoReturnable<Integer> cir) {
      if (this.isTargetItem()) {
         cir.setReturnValue(this.getCurrentGradientColor() & 2013265919);
      }

   }

   @Inject(
      method = {"getBackgroundEnd"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void overrideBackgroundEnd(CallbackInfoReturnable<Integer> cir) {
      if (this.isTargetItem()) {
         cir.setReturnValue(this.getCurrentGradientColor() & 1442840575);
      }

   }

   @Unique
   private boolean isTargetItem() {
      RenderTooltipEvent.Color event = (RenderTooltipEvent.Color)(Object)this;
      ItemStack stack = event.getItemStack();
      return stack != null && ( stack.getItem() == ModItems.DIEXV_SWORD.get());
   }
}
