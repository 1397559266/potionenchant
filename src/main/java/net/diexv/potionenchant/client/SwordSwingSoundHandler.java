package net.diexv.potionenchant.client;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.renderer.CutterAttackAnimation;
import net.diexv.potionenchant.item.DiexvSwordItem;
import net.diexv.potionenchant.item.XSwordItem;
import net.diexv.potionenchant.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * X剑 / DiexvSword 左键挥砍音效（{@code potionenchant:sword_swing}）。
 *
 * <p>在客户端 tick 里判定"新的一刀"（与动画交替用的是同一套判据：{@code player.swinging} 起手沿，
 * 或连击时 {@code swingTime} 回落 —— 因为 {@code swing()} 重启只重置 swingTime、不清 swinging），
 * 每刀播放一次，并顺带推进正放/倒放交替（{@link CutterAttackAnimation#markNewSwing()}）——
 * 放在 tick 里而不是渲染里，第一人称和第三人称的推进才一致。
 *
 * <p>音调每次小幅随机（{@link #PITCH_MIN}~{@link #PITCH_MAX}），差别不大。
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SwordSwingSoundHandler {

    /** 音调随机范围：0.90 ~ 1.10（±10%） */
    private static final float PITCH_MIN = 0.90F;
    private static final float PITCH_MAX = 1.10F;
    /** 音量随机范围 */
    private static final float VOLUME_MIN = 0.85F;
    private static final float VOLUME_MAX = 1.00F;

    private static boolean wasSwinging = false;
    private static int lastSwingTime = 0;
    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            wasSwinging = false;
            return;
        }

        boolean newSwing;
        if (!player.swinging) {
            wasSwinging = false;
            newSwing = false;
        } else {
            newSwing = !wasSwinging || player.swingTime < lastSwingTime;
            wasSwinging = true;
            lastSwingTime = player.swingTime;
        }
        if (!newSwing) {
            return;
        }

        // 推进动画的正放/倒放交替（两个视角一致）
        CutterAttackAnimation.markNewSwing();

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof XSwordItem) && !(stack.getItem() instanceof DiexvSwordItem)) {
            return;
        }

        float pitch = PITCH_MIN + RANDOM.nextFloat() * (PITCH_MAX - PITCH_MIN);
        float volume = VOLUME_MIN + RANDOM.nextFloat() * (VOLUME_MAX - VOLUME_MIN);
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                ModSounds.SWORD_SWING.get(), SoundSource.PLAYERS, volume, pitch, false);
    }

    private SwordSwingSoundHandler() {}
}
