package net.diexv.potionenchant.client.renderer.coderain;

import com.mojang.blaze3d.systems.RenderSystem;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * diexv_sword 全屏代码雨特效（tooltip 触发，淡入淡出）。
 *
 * 机制：
 *  - RenderTooltipEvent.Pre ：悬停 diexv_sword 物品 → 标记显示（markShowing，淡入）
 *  - TickEvent.ClientTick   ：每 tick 检测超过 2 tick 无渲染 → 目标 alpha=0（淡出），指数插值
 *  - ScreenEvent.Render.Post：在屏幕渲染后，全屏叠加代码雨（覆盖整个屏幕，
 *                             关闭 tooltip 后的淡出帧也持续绘制）
 *
 * 不拦截原 tooltip：vanilla 的 tooltip 内容完整保留。
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public final class CodeRainTooltipEvents {

    private CodeRainTooltipEvents() {}

    @SubscribeEvent
    public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
        if (event.getItemStack() == null || event.getItemStack().getItem() != ModItems.DIEXV_SWORD.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CodeRainTooltipRenderer.markShowing(
                0, 0,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CodeRainTooltipRenderer.onClientTick();
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        // 淡入淡出期间（含关闭后的淡出帧）持续全屏绘制代码雨
        if (!CodeRainTooltipRenderer.hasContent() || !CodeRainTooltipRenderer.hasLastRect()) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        float partialTicks = mc.getFrameTime();

        graphics.flush();
        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500); // 最顶层

        // 全屏代码雨：覆盖整个 GUI 缩放区域，alpha 按 partialTicks 帧间插值
        CodeRainTooltipRenderer.renderCodeRain(graphics,
                0, 0,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                CodeRainTooltipRenderer.getRenderAlpha(partialTicks));

        RenderSystem.enableDepthTest();
        graphics.pose().popPose();
        graphics.flush();
    }
}
