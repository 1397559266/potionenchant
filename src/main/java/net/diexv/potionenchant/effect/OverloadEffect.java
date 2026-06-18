package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber
public class OverloadEffect extends MobEffect {

    private static final Random RANDOM = new Random();

    // 标记负载爆炸正在进行，用于 ExplosionEvent.Detonate 过滤物品
    private static boolean overloadExploding = false;
    
    // 防止递归调用：存储正在处理的实体
    private static final java.util.Set<java.util.UUID> processingEntities = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    
    // 存储每个带有负载效果的实体的最后攻击者（用于追溯伤害源到玩家）
    private static final java.util.Map<java.util.UUID, java.util.UUID> lastAttackerMap = new java.util.concurrent.ConcurrentHashMap<>();

    public OverloadEffect() {
        super(MobEffectCategory.HARMFUL, 0x00FFFF); // 青色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 这个效果不需要每tick执行什么特殊逻辑
        // 主要逻辑在受伤事件中处理
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 不需要每tick执行
        return false;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        // 检查实体是否有负载效果
        if (entity.hasEffect(EffectRegistry.OVERLOAD.get())) {
            // 记录最后攻击者
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                lastAttackerMap.put(entity.getUUID(), attacker.getUUID());
            }
            
            // 防止递归调用：如果该实体正在被处理，跳过
            if (processingEntities.contains(entity.getUUID())) {
                return;
            }
            
            // 标记为正在处理
            processingEntities.add(entity.getUUID());
            
            try {
                MobEffectInstance effectInstance = entity.getEffect(EffectRegistry.OVERLOAD.get());
                int currentAmplifier = effectInstance.getAmplifier();
                int currentDuration = effectInstance.getDuration();

                // 每次受伤时等级+1
                int newAmplifier = currentAmplifier + 1;

                // 在实体位置生成粒子
                spawnFlashParticles(entity);
                
                // 对半径5格内的实体造成范围伤害（随着等级提升）
                dealAreaDamage(entity, newAmplifier);

                // 移除旧效果
                entity.removeEffect(EffectRegistry.OVERLOAD.get());

                // 检查是否达到自爆条件（等级>=9，因为等级从0开始）
                if (newAmplifier >= EffectConfigValues.CONFIG.overloadMaxAmplifierBeforeExplosion.get()) {
                    // 触发自爆
                    triggerExplosion(entity);
                } else {
                    // 添加新等级的效果
                    entity.addEffect(new MobEffectInstance(
                            EffectRegistry.OVERLOAD.get(),
                            currentDuration,
                            newAmplifier,
                            false, // 环境效果
                            false, // 不显示粒子
                            true   // 显示图标
                    ));
                }
            } finally {
                // 无论是否发生异常，都要移除标记
                processingEntities.remove(entity.getUUID());
            }
        }
    }

    // 生成粒子效果
    private static void spawnFlashParticles(LivingEntity entity) {
        Level level = entity.level();

        if (level instanceof ServerLevel serverLevel) {
            // 使用闪光粒子效果
            double x = entity.getX();
            double y = entity.getY() + entity.getEyeHeight();
            double z = entity.getZ();

            // 生成多个闪光粒子
            for (int i = 0; i < 15; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * 2.0;
                double offsetY = (RANDOM.nextDouble() - 0.5) * 2.0;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 2.0;

                // 使用白色闪光粒子
                serverLevel.sendParticles(
                        ParticleTypes.FLASH, // 粒子类型
                        x + offsetX,         // X坐标
                        y + offsetY,         // Y坐标
                        z + offsetZ,         // Z坐标
                        1,                   // 粒子数量
                        0, 0, 0,             // 额外偏移
                        0                    // 速度
                );

                // 添加一些电火花粒子增强效果
                serverLevel.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK, // 电火花粒子
                        x + offsetX * 0.5,
                        y + offsetY * 0.5,
                        z + offsetZ * 0.5,
                        1,
                        (RANDOM.nextDouble() - 0.5) * 0.1,
                        (RANDOM.nextDouble() - 0.5) * 0.1,
                        (RANDOM.nextDouble() - 0.5) * 0.1,
                        0.1
                );
            }
        }
    }
    
    // 对半径 5 格内的实体造成范围伤害
    private static void dealAreaDamage(LivingEntity sourceEntity, int amplifier) {
        Level level = sourceEntity.level();
        if (level.isClientSide) return;
            
        double radius = (double)EffectConfigValues.CONFIG.overloadAreaDamageRadius.get();
            
        // 计算伤害：每级造成生命上限的 5% 伤害
        float damagePercent = 0.05f * (amplifier + 1);
        float damage = sourceEntity.getMaxHealth() * damagePercent;
        
        // 尝试获取最后攻击者作为伤害源
        LivingEntity damageSource = findLastAttacker(sourceEntity);
        final LivingEntity finalDamageSource = (damageSource != null) ? damageSource : sourceEntity;
            
        // 查找范围内的所有实体，排除 sourceEntity 和 damageSource（攻击者）
        net.minecraft.world.phys.AABB searchBox = sourceEntity.getBoundingBox().inflate(radius);
        java.util.List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> entity != sourceEntity && entity != finalDamageSource && entity.isAlive()
        );
            
        // 对每个范围内的实体造成爆炸伤害并应用 FLASH 粒子
        for (LivingEntity target : nearbyEntities) {
            // 造成爆炸伤害，使用最后攻击者（如果有）或 sourceEntity 作为伤害来源
            target.hurt(finalDamageSource.damageSources().explosion(finalDamageSource, finalDamageSource), damage);
                
            // 在目标实体位置生成 FLASH 粒子效果
            if (level instanceof ServerLevel serverLevel) {
                spawnFlashParticlesOnEntity(serverLevel, target);
            }
        }
    }
        
    // 在实体位置生成 FLASH 粒子
    private static void spawnFlashParticlesOnEntity(ServerLevel level, LivingEntity entity) {
        double x = entity.getX();
        double y = entity.getY() + entity.getEyeHeight();
        double z = entity.getZ();
            
        // 生成 FLASH 粒子效果
        for (int i = 0; i < 8; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 2.0;
            double offsetY = (RANDOM.nextDouble() - 0.5) * 2.0;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 2.0;
                
            level.sendParticles(
                ParticleTypes.FLASH,
                x + offsetX,
                y + offsetY,
                z + offsetZ,
                1,
                0, 0, 0,
                0
            );
        }
    }

    // 触发自爆
    private static void triggerExplosion(LivingEntity entity) {
        Level level = entity.level();

        if (!level.isClientSide) {
            // 创建爆炸
            float explosionPower = (float)(double)EffectConfigValues.CONFIG.overloadExplosionPower.get(); // 爆炸威力

            // 在实体位置创建爆炸（标记以便过滤物品实体）
            overloadExploding = true;
            try {
                level.explode(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    explosionPower,
                    Level.ExplosionInteraction.NONE
            );

            // 生成10道仅有视觉效果的闪电
            spawnVisualLightningBolts(level, entity.getX(), entity.getY(), entity.getZ());

            // 生成大量闪光粒子作为爆炸效果
            if (level instanceof ServerLevel serverLevel) {
                spawnExplosionParticles(serverLevel, entity.getX(), entity.getY(), entity.getZ());
            }

            // 尝试获取最后攻击者作为伤害源
            LivingEntity damageSource = findLastAttacker(entity);
            
            // 造成数值为生命上限的伤害
            // 如果有最后攻击者，使用攻击者作为伤害源；否则使用魔法伤害源（无法被免疫）
            if (damageSource != null && damageSource != entity) {
                // 使用最后攻击者作为伤害源
                entity.hurt(damageSource.damageSources().explosion(damageSource, damageSource), entity.getMaxHealth());
            } else {
                // 使用魔法伤害源，确保伤害能够生效
                entity.hurt(entity.damageSources().magic(), entity.getMaxHealth());
            }
            
            // 清理数据
            lastAttackerMap.remove(entity.getUUID());
            } finally {
                overloadExploding = false;
            }
        }
    }

    // 生成爆炸粒子效果
    private static void spawnExplosionParticles(ServerLevel level, double x, double y, double z) {
        // 生成爆炸粒子
        level.sendParticles(
                ParticleTypes.EXPLOSION,
                x,
                y,
                z,
                5,
                2.0, 2.0, 2.0,
                0.5
        );
    }

    // 生成仅有视觉效果的闪电
    private static void spawnVisualLightningBolts(Level level, double x, double y, double z) {
        for (int i = 0; i < 10; i++) {
            // 计算随机偏移位置（在半径5格范围内）
            double offsetX = (RANDOM.nextDouble() - 0.5) * 10;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 10;

            // 创建闪电实体
            LightningBolt lightningBolt = new LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
            lightningBolt.setPos(x + offsetX, y, z + offsetZ);

            // 设置闪电为仅有视觉效果（不造成伤害）
            lightningBolt.setVisualOnly(true);

            // 添加到世界
            level.addFreshEntity(lightningBolt);
        }
    }
    
    // 阻止负载爆炸摧毁物品实体
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (overloadExploding) {
            event.getAffectedEntities().removeIf(entity -> entity instanceof ItemEntity);
        }
    }
    
    // 查找最后攻击者
    private static LivingEntity findLastAttacker(LivingEntity entity) {
        java.util.UUID attackerUUID = lastAttackerMap.get(entity.getUUID());
        if (attackerUUID == null) return null;
        
        // 在服务器端查找实体
        if (entity.level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity attacker = serverLevel.getEntity(attackerUUID);
            if (attacker instanceof LivingEntity livingAttacker && livingAttacker.isAlive()) {
                return livingAttacker;
            }
        }
        return null;
    }
    
    // 监听效果移除事件，清理数据
    @SubscribeEvent
    public static void onEffectRemoved(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null && 
            event.getEffectInstance().getEffect() instanceof OverloadEffect) {
            lastAttackerMap.remove(event.getEntity().getUUID());
        }
    }
    
    // 监听效果过期事件，清理数据
    @SubscribeEvent
    public static void onEffectExpired(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && 
            event.getEffectInstance().getEffect() instanceof OverloadEffect) {
            lastAttackerMap.remove(event.getEntity().getUUID());
        }
    }

    // 获取显示名称（显示实际等级）
    @Override
    public String getDescriptionId() {
        return "effect.potionenchant.overload";
    }
}
