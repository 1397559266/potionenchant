package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber
public class CriticalStrikeEffect extends MobEffect {

    private static final Random RANDOM = new Random();

    public CriticalStrikeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // 金色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 这个效果不需要每tick执行什么特殊逻辑
        // 主要逻辑在攻击事件中处理
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 不需要每tick执行
        return false;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 检查伤害来源是否是生物
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
            // 检查攻击者是否有暴击效果
            if (attacker.hasEffect(EffectRegistry.CRITICAL_STRIKE.get())) {
                MobEffectInstance effectInstance = attacker.getEffect(EffectRegistry.CRITICAL_STRIKE.get());
                int amplifier = effectInstance.getAmplifier();

                // 计算暴击概率：基础概率 + 每级10%
                float baseChance = 0.2f; // 基础20%概率
                float chancePerLevel = 0.1f; // 每级增加10%
                float criticalChance = baseChance + (amplifier * chancePerLevel);

                // 限制最大概率为100%
                criticalChance = Mth.clamp(criticalChance, 0.0f, 1.0f);

                // 随机判断是否触发暴击
                if (RANDOM.nextFloat() < criticalChance) {
                    // 触发暴击
                    triggerCriticalStrike(event, attacker);
                }
            }
        }
    }

    // 触发暴击
    private static void triggerCriticalStrike(LivingHurtEvent event, LivingEntity attacker) {
        // 获取原始伤害
        float originalDamage = event.getAmount();

        // 计算暴击伤害（原版暴击是1.5倍伤害）
        float criticalDamage = originalDamage * 1.5f;

        // 设置新的伤害值
        event.setAmount(criticalDamage);

        // 生成暴击粒子效果
        spawnCriticalParticles(event.getEntity());

        // 播放暴击音效
        playCriticalSound(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ());

    }

    // 生成暴击粒子效果
    private static void spawnCriticalParticles(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            double x = target.getX();
            double y = target.getY() + target.getEyeHeight();
            double z = target.getZ();

            // 生成暴击粒子（类似原版暴击效果）
            for (int i = 0; i < 20; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * 2.0;
                double offsetY = (RANDOM.nextDouble() - 0.5) * 2.0;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 2.0;

                // 使用暴击粒子
                serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        x + offsetX,
                        y + offsetY,
                        z + offsetZ,
                        1,
                        0, 0, 0,
                        0.1
                );
            }
        }
    }


    // 播放暴击音效
    private static void playCriticalSound(Level level, double x, double y, double z) {
            // 播放原版暴击音效
            level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
