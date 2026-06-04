package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 恩怨效果的事件处理器
 * 伤害反射 + 治疗共享
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class SymbiosisHandler {

    // 重入保护：防止两个恩怨实体互殴导致无限递归
    private static final ThreadLocal<Set<UUID>> processing = ThreadLocal.withInitial(HashSet::new);

    /**
     * 伤害反射：持有恩怨效果的实体受到伤害时，攻击者也受到同等伤害
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!victim.hasEffect(EffectRegistry.SYMBIOSIS.get())) return;

        UUID victimUUID = victim.getUUID();
        // 重入保护：同一实体正在处理中，跳过
        if (!processing.get().add(victimUUID)) return;

        try {
            Entity attacker = event.getSource().getEntity();
            if (attacker instanceof LivingEntity && attacker != victim) {
                // 清除无敌帧，确保反射伤害必定命中
                ((LivingEntity) attacker).invulnerableTime = 0;
                ((LivingEntity) attacker).hurt(event.getSource(), event.getAmount());
            }
        } finally {
            processing.get().remove(victimUUID);
        }
    }

    /**
     * 治疗共享：当喷溅/滞留药水命中持有恩怨效果的实体时
     * 检查药水是否有治疗效果，如有则治疗药水投掷者
     */
    @SubscribeEvent
    public static void onPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion potion)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity target)) return;
        if (target.level().isClientSide) return;
        if (!target.hasEffect(EffectRegistry.SYMBIOSIS.get())) return;

        // 检查药水是否包含治疗效果
        boolean hasHealing = false;
        for (MobEffectInstance effect : PotionUtils.getMobEffects(potion.getItem())) {
            if (effect.getEffect() == MobEffects.HEAL || effect.getEffect() == MobEffects.HEALTH_BOOST) {
                hasHealing = true;
                break;
            }
        }
        if (!hasHealing) return;

        // 治疗药水投掷者
        Entity thrower = potion.getOwner();
        if (thrower instanceof LivingEntity && thrower != target) {
            float healAmount = 8.0f;
            ((LivingEntity) thrower).heal(healAmount);
        }
    }
}
