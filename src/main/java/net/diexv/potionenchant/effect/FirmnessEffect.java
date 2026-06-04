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
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class FirmnessEffect extends MobEffect {

    private static final Map<UUID, HealthLockData> healthLocks = new HashMap<>();
    // 重入保护：防止 %HP 伤害 buff 在同一事件链中递归触发 hurt
    private static final ThreadLocal<Set<UUID>> processing = ThreadLocal.withInitial(HashSet::new);

    public FirmnessEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4682B4);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /**
     * Cap damage at a percentage of max health.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        UUID uuid = entity.getUUID();

        // 重入保护：同一实体正在处理中，跳过
        if (!processing.get().add(uuid)) return;
        try {
            if (!entity.hasEffect(EffectRegistry.FIRMNESS.get())) return;

            MobEffectInstance effectInstance = entity.getEffect(EffectRegistry.FIRMNESS.get());
            int amplifier = effectInstance.getAmplifier();

            float maxHealth = entity.getMaxHealth();

            // Calculate max damage percentage: 90% - 10% per level, minimum 1%
            float maxDamagePercent = 0.90f - (amplifier * 0.10f);
            maxDamagePercent = Math.max(maxDamagePercent, 0.01f);
            float maxDamage = maxHealth * maxDamagePercent;

            if (event.getAmount() > maxDamage) {
                event.setAmount(maxDamage);
            }

            // Health lock: record current health as floor for the next few ticks
            if (!entity.level().isClientSide) {
                float lockDuration = 1.0f + (amplifier * 0.5f);
                lockDuration = Math.min(lockDuration, 10.0f);
                int lockTicks = (int) (lockDuration * 20);
                // Use raw entity data health to avoid getHealth mixin interference
                float currentHealth = entity.getEntityData().get(
                    net.diexv.potionenchant.mixin.accessor.LivingEntityAccessor.HEALTH());
                float healthFloor = currentHealth - maxDamage;
                if (healthFloor < 1.0f) healthFloor = 1.0f;
                setHealthLock(entity, healthFloor, lockTicks);
            }

            // Particles
            spawnFirmnessParticles(entity);
        } finally {
            processing.get().remove(uuid);
        }
    }

    private static void setHealthLock(LivingEntity entity, float healthFloor, int ticks) {
        UUID uuid = entity.getUUID();
        HealthLockData existing = healthLocks.get(uuid);
        if (existing != null && existing.remainingTicks > 0 && existing.lockedHealth > healthFloor) {
            // Don't lower the floor if current lock is higher
            return;
        }
        healthLocks.put(uuid, new HealthLockData(healthFloor, ticks));
    }

    public static void reduceLockedHealth(UUID uuid, float amount) {
        HealthLockData lock = healthLocks.get(uuid);
        if (lock != null) {
            lock.lockedHealth -= amount;
            if (lock.lockedHealth < 1.0f) lock.lockedHealth = 1.0f;
        }
    }

    public static boolean isHealthLocked(UUID uuid) {
        HealthLockData lock = healthLocks.get(uuid);
        return lock != null && lock.isLocked();
    }

    public static float getLockedHealth(UUID uuid) {
        HealthLockData lock = healthLocks.get(uuid);
        return lock != null ? lock.lockedHealth : 0;
    }

    public static void clearHealthLock(UUID uuid) {
        healthLocks.remove(uuid);
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            healthLocks.entrySet().removeIf(entry -> {
                entry.getValue().remainingTicks--;
                return entry.getValue().remainingTicks <= 0;
            });
        }
    }

    private static void spawnFirmnessParticles(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            double x = entity.getX();
            double y = entity.getY() + entity.getBbHeight() / 2.0;
            double z = entity.getZ();
            for (int i = 0; i < 10; i++) {
                double ox = (Math.random() - 0.5) * 2.0;
                double oy = (Math.random() - 0.5) * 2.0;
                double oz = (Math.random() - 0.5) * 2.0;
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x + ox, y + oy, z + oz, 1, 0, 0, 0, 0.1);
            }
            entity.level().playSound(null, x, y, z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.3F, 1.0F);
        }
    }

    private static class HealthLockData {
        float lockedHealth;
        int remainingTicks;

        HealthLockData(float health, int ticks) {
            this.lockedHealth = health;
            this.remainingTicks = ticks;
        }

        boolean isLocked() {
            return remainingTicks > 0 && lockedHealth > 0;
        }
    }
}
