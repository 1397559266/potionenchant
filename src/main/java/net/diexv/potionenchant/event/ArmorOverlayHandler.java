package net.diexv.potionenchant.event;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "potionenchant", value = Dist.CLIENT)
public class ArmorOverlayHandler {

    @SuppressWarnings("removal")
    private static final ResourceLocation FULL_ICON =
            new ResourceLocation("potionenchant:textures/gui/all_layer.png");
    @SuppressWarnings("removal")
    private static final ResourceLocation HALF_ICON =
            new ResourceLocation("potionenchant:textures/gui/half_layer.png");
    @SuppressWarnings("removal")
    private static final ResourceLocation MAGIC_ICON =
            new ResourceLocation("potionenchant:textures/gui/magic_layer.png");
    @SuppressWarnings("removal")
    private static final ResourceLocation INVUL_ICON =
            new ResourceLocation("potionenchant:textures/gui/invulnerable_layer.png");

    private static final int ICON_SIZE = 9;
    private static final int INVUL_SIZE = 9;
    private static final int STEP = 8;
    // 额外护甲图标上限：覆盖 21~40 护甲值区间，共 10 个满图标
    private static final int MAX_EXTRA_ICONS = 10;

    @SubscribeEvent
    public static void onArmorPost(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (!PotionEnchantConfig.COMMON.enableArmorValueRender.get()) return;
        if (mc.player == null || mc.options.hideGui) return;

        ForgeGui gui = (ForgeGui) mc.gui;
        if (!gui.shouldDrawSurvivalElements()) return;

        int armorValue = mc.player.getArmorValue();
        int resistanceLevel = 0;
        int magicLevel = 0;

        MobEffectInstance res = mc.player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (res != null) resistanceLevel = res.getAmplifier() + 1;

        MobEffectInstance magic = mc.player.getEffect(EffectRegistry.MAGIC_RESISTANCE.get());
        if (magic != null) magicLevel = magic.getAmplifier() + 1;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int baseX = screenW / 2 - 91;

        int baseY = screenH - (gui.leftHeight - 10);

        GuiGraphics graphics = event.getGuiGraphics();

        // 1. 额外护甲覆盖（21~40，超过40不再渲染）
        if (armorValue > 20) {
            int extra = Math.min(armorValue - 20, MAX_EXTRA_ICONS * 2); // 最多20点额外护甲 = 10个图标
            int fullIcons = extra / 2;
            int halfIcons = extra % 2;
            for (int i = 0; i < fullIcons; i++) {
                graphics.blit(FULL_ICON, baseX + i * STEP, baseY,
                        0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
            if (halfIcons > 0) {
                graphics.blit(HALF_ICON, baseX + fullIcons * STEP, baseY,
                        0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
        }

        // 2. 魔法抗性覆盖（每级 1 格）
        if (magicLevel > 0) {
            for (int i = 0; i < magicLevel && i < 10; i++) {
                graphics.blit(MAGIC_ICON, baseX + i * STEP, baseY,
                        0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
        }

        // 3. 抗性边框（每级 2 格）
        if (resistanceLevel > 0) {
            int totalIcons = resistanceLevel * 2;
            int offset = (INVUL_SIZE - ICON_SIZE) / 2;
            for (int i = 0; i < totalIcons && i < 10; i++) {
                int x = baseX + i * STEP - offset;
                int y = baseY - offset;
                graphics.blit(INVUL_ICON, x, y,
                        0, 0, INVUL_SIZE, INVUL_SIZE, INVUL_SIZE, INVUL_SIZE);
            }
        }
    }
}
