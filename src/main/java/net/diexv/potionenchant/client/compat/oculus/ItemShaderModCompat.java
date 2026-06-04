package net.diexv.potionenchant.client.compat.oculus;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.fml.ModList;

public final class ItemShaderModCompat {
    private static boolean loggedCompatMode;
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("oculus");

    public static boolean isOculusLoaded() {
        return OCULUS_LOADED;
    }

    public static boolean isOculusShaderPackActive() {
        if (!OCULUS_LOADED) return false;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object active = apiClass.getMethod("isShaderPackInUse").invoke(api);
            return Boolean.TRUE.equals(active);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean shouldDeferCosmicItemRendering() {
        return isOculusShaderPackActive();
    }

    public static void logCompatModeOnce() {
        if (!isOculusShaderPackActive() || loggedCompatMode) return;
        loggedCompatMode = true;
        PotionEnchantMod.LOGGER.warn("[SkyRender] Oculus shader pack detected. Enabling deferred cosmic item rendering for compatibility.");
    }

    private ItemShaderModCompat() {}
}
