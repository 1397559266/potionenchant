package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当自定义标题贴图文件存在时，取消原版 Logo 渲染
 */
@Mixin(LogoRenderer.class)
public class LogoRendererMixin {

    private static final ResourceLocation CUSTOM_LOGO_CHECK = new ResourceLocation(PotionEnchantMod.MODID, "textures/gui/main_menu_logo_2.png");

    // 缓存检测结果
    private static Boolean hasCustomLogo = null;

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("HEAD"), cancellable = true)
    private void onRenderLogo(GuiGraphics guiGraphics, int screenWidth, float alpha, int heightOffset, CallbackInfo ci) {
        // 仅配置启用 + 自定义标题文件存在时，才取消原版 Logo
        if (PotionEnchantConfig.COMMON.enableCustomMainMenu.get() && checkLogoExists()) {
            ci.cancel();
        }
    }

    private static boolean checkLogoExists() {
        if (hasCustomLogo == null) {
            try {
                hasCustomLogo = Minecraft.getInstance().getResourceManager().getResource(CUSTOM_LOGO_CHECK).isPresent();
            } catch (Exception e) {
                hasCustomLogo = false;
            }
        }
        return hasCustomLogo;
    }
}