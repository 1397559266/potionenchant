package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.diexv.potionenchant.config.values.EffectConfigValues;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ArmorBreakEffect extends MobEffect {

    public ArmorBreakEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B4513); // 棕色，代表破坏护甲
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            // 检查攻击者是否有碎甲效果
            if (attacker.hasEffect(EffectRegistry.ARMOR_BREAK.get())) {
                LivingEntity target = event.getEntity();
                int amplifier = attacker.getEffect(EffectRegistry.ARMOR_BREAK.get()).getAmplifier();

                // 计算无视护甲比例 (基础10% + 每级10%)
                float armorIgnorePercent = (float)(double)EffectConfigValues.CONFIG.armorBreakIgnorePerLevel.get() + (amplifier * (float)(double)EffectConfigValues.CONFIG.armorBreakIgnorePerLevel.get());

                // 计算基础伤害（不考虑护甲）
                float baseDamage = event.getAmount();

                // 如果目标有护甲值，计算无视护甲后的伤害
                float armorValue = target.getArmorValue();
                if (armorValue > 0) {
                    // 计算被无视的护甲值
                    float ignoredArmor = armorValue * armorIgnorePercent;
                    // 调整实际伤害（简化计算）
                    float bonusDamage = baseDamage * (ignoredArmor * 0.04f); // 每点护甲约减少4%伤害
                    event.setAmount(event.getAmount() + bonusDamage);
                }

                // 扣除目标盔甲耐久 (基础1% + 每级1%)
                float durabilityDamagePercent = (float)(double)EffectConfigValues.CONFIG.armorBreakDurabilityPerLevel.get() + (amplifier * (float)(double)EffectConfigValues.CONFIG.armorBreakDurabilityPerLevel.get());
                damageArmorDurability(target, durabilityDamagePercent);
            }
        }
    }

    private static void damageArmorDurability(LivingEntity entity, float damagePercent) {
        // 遍历所有装备槽位
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armorStack = entity.getItemBySlot(slot);
                if (!armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                    // 计算耐久伤害值
                    int maxDurability = armorStack.getMaxDamage();
                    int damageAmount = Math.max(1, (int)(maxDurability * damagePercent));

                    // 扣除耐久
                    armorStack.hurtAndBreak(damageAmount, entity,
                            (livingEntity) -> livingEntity.broadcastBreakEvent(slot));
                }
            }
        }
    }
}
