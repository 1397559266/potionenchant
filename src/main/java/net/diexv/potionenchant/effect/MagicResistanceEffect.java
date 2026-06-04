package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MagicResistanceEffect extends MobEffect {

    public MagicResistanceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xBA55D3); // 淡紫色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 魔法抗性是被动效果，不需要每tick执行
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        if (!entity.hasEffect(EffectRegistry.MAGIC_RESISTANCE.get())) return;

        DamageSource source = event.getSource();

        // 使用 getMsgId() 判断魔法伤害（药水、守护者、唤魔者尖牙等）
        if ("magic".equals(source.getMsgId()) || "indirectMagic".equals(source.getMsgId())) {
            MobEffectInstance effectInstance = entity.getEffect(EffectRegistry.MAGIC_RESISTANCE.get());
            int amplifier = effectInstance.getAmplifier();
            // 计算减免比例：每级减少10%，最高90%
            float reduction = Math.min(0.9F, (amplifier + 1) * 0.1F);

            // 应用减免
            float originalDamage = event.getAmount();
            float reducedDamage = originalDamage * (1.0F - reduction);
            event.setAmount(Math.max(0.0F, reducedDamage));
        }
    }

    // 计算魔法伤害减免（供外部调用）
    public static float getMagicDamageReduction(int amplifier) {
        return Math.min(0.9F, (amplifier + 1) * 0.1F);
    }
}
