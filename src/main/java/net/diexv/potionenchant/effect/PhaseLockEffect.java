package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class PhaseLockEffect extends MobEffect {

    // 存储每个玩家的标记目标
    private static final Map<UUID, UUID> markedTargets = new HashMap<>();
    
    // 存储标记时的攻击伤害值
    private static final Map<UUID, Float> markedDamage = new HashMap<>();
    
    // 存储玩家上次的攻击状态
    private static final Map<UUID, Boolean> wasAttacking = new HashMap<>();
    
    // 存储玩家上次攻击是否命中
    private static final Map<UUID, Boolean> lastAttackHit = new HashMap<>();

    public PhaseLockEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9932CC); // 深紫色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 这个效果不需要每tick执行特殊逻辑
        // 主要逻辑在攻击事件中处理
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
            // 检查攻击者是否有相位锁定效果
            if (attacker.hasEffect(EffectRegistry.PHASE_LOCK.get())) {
                Entity target = event.getEntity();
                
                // 如果是真实命中，标记目标并记录攻击伤害
                if (event.getAmount() > 0) {
                    markTarget(attacker, target.getUUID(), event.getAmount());
                    spawnMarkParticles(target);
                    
                    // 记录这次攻击命中了
                    lastAttackHit.put(attacker.getUUID(), true);
                    
                    // 目标每次受伤播放经验球音效
                    if (attacker.level() instanceof ServerLevel) {
                        playOrbSound(target);
                    }
                }
            }
        }
    }
    
    // 监听玩家tick，检测左键挥动
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        if (player.level().isClientSide) return;
        
        // 检查玩家是否有相位锁定效果
        if (!player.hasEffect(EffectRegistry.PHASE_LOCK.get())) return;
        
        UUID markedTarget = markedTargets.get(player.getUUID());
        if (markedTarget == null) return;
        
        // 检测是否正在挥动武器（左键攻击）
        boolean isAttacking = player.swinging;
        UUID playerUUID = player.getUUID();
        Boolean previousAttacking = wasAttacking.get(playerUUID);
        
        // 检测从左键未挥动到挥动的瞬间
        if (isAttacking && (previousAttacking == null || !previousAttacking)) {
            // 检查这次攻击是否命中（通过检查lastAttackHit标志）
            Boolean didHit = lastAttackHit.get(playerUUID);
            
            // 只有挥空时（没有命中任何实体）才对标记目标造成伤害
            if (didHit == null || !didHit) {
                // 尝试找到被标记的实体
                LivingEntity markedEntity = findMarkedEntity(player, markedTarget);
                if (markedEntity != null) {
                    MobEffectInstance effectInstance = player.getEffect(EffectRegistry.PHASE_LOCK.get());
                    int amplifier = effectInstance.getAmplifier();
                    
                    // 对被标记的实体造成伤害（使用标记时的攻击伤害）
                    Float lastDamage = markedDamage.get(playerUUID);
                    if (lastDamage != null && lastDamage > 0) {
                        dealPhaseLockDamageToEntity(markedEntity, player, amplifier, lastDamage);
                    }
                    
                    // 播放吸取经验球音效
                    playOrbSound(player);
                }
            }
            
            // 重置命中标志
            lastAttackHit.put(playerUUID, false);
        }
        
        // 更新攻击状态
        wasAttacking.put(playerUUID, isAttacking);
    }

    // 标记目标
    private static void markTarget(LivingEntity attacker, UUID targetUUID, float damage) {
        markedTargets.put(attacker.getUUID(), targetUUID);
        markedDamage.put(attacker.getUUID(), damage);
    }

    // 查找被标记的实体
    public static LivingEntity findMarkedEntity(Player player, UUID markedUUID) {
        if (player.level().isClientSide) return null;
        
        // 在玩家周围搜索被标记的实体
        double searchRadius = 32.0; // 搜索半径32格
        AABB searchBox = player.getBoundingBox().inflate(searchRadius);
        
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
            LivingEntity.class, 
            searchBox,
            entity -> entity.getUUID().equals(markedUUID)
        );
        
        return entities.isEmpty() ? null : entities.get(0);
    }
    
    // 获取被标记的目标UUID
    public static UUID getMarkedTarget(UUID playerUUID) {
        return markedTargets.get(playerUUID);
    }
    
    // 获取最后记录的伤害值
    public static Float getLastMarkedDamage(UUID playerUUID) {
        return markedDamage.get(playerUUID);
    }

    // 直接对被标记的实体造成伤害（使用标记时的攻击伤害）
    public static void dealPhaseLockDamageToEntity(LivingEntity target, Player attacker, int amplifier, float markedDamageValue) {
        if (attacker.level().isClientSide) return;
        
        // 计算相位锁定伤害（基础100%，每级增加25%）
        float damagePercent = 1.0f + 0.25f * amplifier;
        float phaseLockDamage = markedDamageValue * damagePercent;
        
        // 创建无击退的伤害源，使用 attacker 作为伤害来源
        target.hurt(attacker.damageSources().playerAttack(attacker), phaseLockDamage);
        
        // 如果主手物品存在药水附魔，则应用药水附魔效果
        applyMainHandPotionEnchantments(attacker, target);
        
        // 生成粒子效果
        spawnPhaseLockParticles(target);
        
        // 播放音效
        playPhaseLockSound(target);
    }

    // 获取基础攻击伤害
    @SuppressWarnings("unused")
    private static float getBaseAttackDamage(LivingEntity entity) {
        if (entity instanceof Player player) {
            // 获取玩家的攻击力属性
            return (float) player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue();
        }
        return 1.0f; // 默认值
    }
    
    // 应用主手物品的药水附魔效果
    private static void applyMainHandPotionEnchantments(Player attacker, LivingEntity target) {
        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.isEmpty() && PotionEnchantManager.hasPotionEnchantments(weapon)) {
            List<PotionEnchantData> toolEnchants = PotionEnchantManager.getPotionEnchantments(weapon);
            
            for (PotionEnchantData enchant : toolEnchants) {
                // 与主手物品击中实体时的逻辑一样
                enchant.applyToolEffect(target, enchant.getAmplifier() + 1);
            }
        }
    }

    // 生成标记粒子效果
    private static void spawnMarkParticles(Entity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            double x = target.getX();
            double y = target.getY() + target.getBbHeight() / 2.0;
            double z = target.getZ();

            // 生成紫色粒子
            for (int i = 0; i < 15; i++) {
                double offsetX = (Math.random() - 0.5) * 1.5;
                double offsetY = (Math.random() - 0.5) * 1.5;
                double offsetZ = (Math.random() - 0.5) * 1.5;

                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
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

    // 生成相位锁定粒子效果
    private static void spawnPhaseLockParticles(Entity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            double x = target.getX();
            double y = target.getY() + target.getBbHeight() / 2.0;
            double z = target.getZ();

            // 生成紫色螺旋粒子
            for (int i = 0; i < 20; i++) {
                double angle = i * Math.PI / 10;
                double radius = 0.5;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;

                serverLevel.sendParticles(
                        ParticleTypes.PORTAL,
                        x + offsetX,
                        y + (i * 0.1),
                        z + offsetZ,
                        1,
                        0, 0, 0,
                        0.1
                );
            }
        }
    }

    // 播放相位锁定音效
    private static void playPhaseLockSound(Entity target) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), 
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.5F);
    }
    
    // 播放吸取经验球音效
    private static void playOrbSound(Entity target) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), 
                net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    // 清除标记（当效果消失时）
    public static void clearMark(UUID playerUUID) {
        markedTargets.remove(playerUUID);
        markedDamage.remove(playerUUID);
        lastAttackHit.remove(playerUUID);
    }
}
