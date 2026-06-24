package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TitleScreen.class)
public class TitleScreenBackgroundMixin {

    @Redirect(method = "render", 
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    private void cancelPanorama(PanoramaRenderer instance, float f, float g) {
        if (!PotionEnchantConfig.COMMON.enableCustomMainMenu.get()) {
            instance.render(f, g);
        }
    }
}
