package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenBackgroundMixin {

    @Inject(method = "render", 
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"),
            cancellable = true)
    private void onPanoramaRender(CallbackInfo ci) {
        if (PotionEnchantConfig.COMMON.enableCustomMainMenu.get()) {
            ci.cancel();
        }
    }
}
