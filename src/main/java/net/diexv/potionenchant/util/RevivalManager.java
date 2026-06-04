package net.diexv.potionenchant.util;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 复活管理器 — 替代原 agent 中的 RevivalHandler。
 * 不保存玩家状态，仅在死亡触发时简单复活。
 */
public class RevivalManager {

    private static boolean guard = false;

    /**
     * 当玩家即将死亡且有 Revival 效果时，执行复活
     * @return true 如果复活成功
     */
    public static boolean tryRevive(LivingEntity entity) {
        if (guard) return false;
        if (!(entity instanceof ServerPlayer player)) return false;
        if (player.level().isClientSide) return false;

        MobEffectInstance inst = player.getEffect(EffectRegistry.REVIVAL.get());
        if (inst == null) return false;

        guard = true;
        try {
            int amplifier = inst.getAmplifier();

            // 回满血
            player.setHealth(player.getMaxHealth());
            // 清除复活后短暂无敌帧，防止被立即再次击杀
            player.invulnerableTime = 0;

            // 消耗一级复活
            player.removeEffect(EffectRegistry.REVIVAL.get());
            if (amplifier > 0) {
                player.addEffect(new MobEffectInstance(
                    EffectRegistry.REVIVAL.get(), inst.getDuration(), amplifier - 1,
                    false, false, true));
            }

            // 视觉效果
            player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.5F);

            if (player.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 25; i++) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                        player.getX() + (Math.random() - 0.5) * 2.1,
                        player.getY() + player.getEyeHeight() + (Math.random() - 0.5) * 2.1,
                        player.getZ() + (Math.random() - 0.5) * 2.1,
                        1, 0, 0, 0, 0.05);
                }
            }

            return true;
        } finally {
            guard = false;
        }
    }
}
