package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class VoidPowerEffect extends MobEffect {

    public VoidPowerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x000000); // 黑色
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
            // 检查攻击者是否有虚空之力效果
            if (attacker.hasEffect(EffectRegistry.VOID_POWER.get())) {
                MobEffectInstance effectInstance = attacker.getEffect(EffectRegistry.VOID_POWER.get());
                int amplifier = effectInstance.getAmplifier();

                // 计算虚空伤害加成：每级增加10%的额外伤害
                float voidDamageMultiplier = 0.1f * (amplifier + 1); // 等级0=10%，等级1=20%，以此类推

                // 获取当前总伤害（已经包含了所有其他加成）
                float totalDamage = event.getAmount();

                // 计算虚空伤害（基于总伤害的百分比）
                float voidDamage = totalDamage * voidDamageMultiplier;

                // 添加虚空伤害到总伤害中
                event.setAmount(totalDamage + voidDamage);
            }
        }
    }
}