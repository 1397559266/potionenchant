package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class RevivalEffect extends MobEffect {

    private static final Map<UUID, Long> REVIVAL_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 60;

    public RevivalEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF69B4);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {}

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    // 安全网：万一 die() 被外部模组直接调用且 Agent 未拦截，这里兜底取消死亡
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().hasEffect(EffectRegistry.REVIVAL.get())) {
            event.setCanceled(true);
        }
    }

    public static boolean isInRevivalCooldown(Player player) {
        if (player == null) return false;
        Long last = REVIVAL_COOLDOWNS.get(player.getUUID());
        if (last == null) return false;
        return (player.level().getGameTime() - last) <= COOLDOWN_TICKS;
    }
}
