package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class FragilityEffect extends MobEffect {

    // 存储每个带有脆弱效果的实体的最后攻击者
    private static final Map<UUID, UUID> lastAttackerMap = new ConcurrentHashMap<>();

    public FragilityEffect() {
        super(MobEffectCategory.HARMFUL, 0x696969); // 深灰色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 每tick造成0.1点伤害
        if (!entity.level().isClientSide) {
            // 尝试获取最后攻击者作为伤害源
            UUID lastAttackerUUID = lastAttackerMap.get(entity.getUUID());
            if (lastAttackerUUID != null) {
                // 查找最后攻击者实体
                LivingEntity lastAttacker = findEntityByUUID(entity.level(), lastAttackerUUID);
                if (lastAttacker != null && lastAttacker.isAlive()) {
                    // 使用最后攻击者作为伤害源
                    entity.hurt(lastAttacker.damageSources().magic(), (float)(double)EffectConfigValues.CONFIG.fragilityDamagePerTick.get());
                    return;
                }
            }
            // 如果找不到最后攻击者，使用实体自身作为伤害源
            entity.hurt(entity.damageSources().magic(), (float)(double)EffectConfigValues.CONFIG.fragilityDamagePerTick.get());
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 需要每tick执行
        return true;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        // 检查实体是否有脆弱效果
        if (entity.hasEffect(EffectRegistry.FRAGILITY.get())) {
            // 移除实体的无敌帧
            removeInvulnerability(entity);
            
            // 记录最后攻击者
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                lastAttackerMap.put(entity.getUUID(), attacker.getUUID());
            }
        }
    }
    
    // 监听效果移除事件，清理数据
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null && 
            event.getEffectInstance().getEffect() instanceof FragilityEffect) {
            clearLastAttacker(event.getEntity().getUUID());
        }
    }
    
    // 监听效果过期事件，清理数据
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && 
            event.getEffectInstance().getEffect() instanceof FragilityEffect) {
            clearLastAttacker(event.getEntity().getUUID());
        }
    }

    // 移除实体的无敌帧
    private static void removeInvulnerability(LivingEntity entity) {
        // 将实体的无敌时间设置为0
        entity.invulnerableTime = 0;

        // 重置hurtTime，确保不会出现受伤动画和击退效果
        entity.hurtTime = 0;
    }
    
    // 根据UUID查找实体
    private static LivingEntity findEntityByUUID(net.minecraft.world.level.Level level, UUID uuid) {
        if (level.isClientSide) return null;
        
        // 在服务器端查找实体
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return (LivingEntity) serverLevel.getEntity(uuid);
        }
        return null;
    }
    
    // 清理事体数据（当效果被移除时）
    public static void clearLastAttacker(UUID entityUUID) {
        lastAttackerMap.remove(entityUUID);
    }
}