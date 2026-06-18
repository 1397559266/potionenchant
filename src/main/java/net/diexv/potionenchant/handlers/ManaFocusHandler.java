package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.diexv.potionenchant.config.values.EnchantmentConfigValues;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class ManaFocusHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 处理受到的魔法伤害减少
        if (event.getEntity() instanceof Player player) {
            if (isMagicDamage(event.getSource())) {
                // 计算玩家所有盔甲上的魔力聚焦总等级
                int totalManaFocusLevel = getTotalManaFocusLevel(player);

                if (totalManaFocusLevel > 0) {
                    // 每级减少10%魔法伤害
                    float damageReduction = (float)(double)EnchantmentConfigValues.CONFIG.manaFocusReductionPerLevel.get() * totalManaFocusLevel;
                    float newDamage = event.getAmount() * (1.0f - damageReduction);
                    event.setAmount(newDamage);
                }
            }
        }

        // 处理造成的伤害转化为魔法伤害并提高伤害
        if (event.getSource().getDirectEntity() instanceof Player attacker) {
            // 计算攻击者所有盔甲上的魔力聚焦总等级
            int totalManaFocusLevel = getTotalManaFocusLevel(attacker);

            if (totalManaFocusLevel > 0) {
                // 每级提高25%魔法伤害
                float damageMultiplier = 1.0f + ((float)(double)EnchantmentConfigValues.CONFIG.manaFocusDamageIncreasePerLevel.get() * totalManaFocusLevel);
                float newDamage = event.getAmount() * damageMultiplier;
                event.setAmount(newDamage);

                // 将伤害类型标记为魔法伤害
                // 注意：在Minecraft中，我们无法直接修改伤害类型
                // 但我们可以通过其他方式模拟魔法伤害效果
            }
        }
    }

    // 检查是否为魔法伤害
    private static boolean isMagicDamage(DamageSource source) {
        return source.is(DamageTypes.MAGIC) ||
                source.is(DamageTypes.WITHER) ||
                source.is(DamageTypes.DRAGON_BREATH) ||
                source.is(DamageTypes.INDIRECT_MAGIC);
    }

    // 计算玩家所有盔甲上的魔力聚焦总等级
    private static int getTotalManaFocusLevel(Player player) {
        int totalLevel = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                if (!armor.isEmpty() && armor.getItem() instanceof ArmorItem) {
                    int level = EnchantmentHelper.getItemEnchantmentLevel(
                            PotionEnchantMod.MANA_FOCUS.get(), armor);
                    totalLevel += level;
                }
            }
        }

        return totalLevel;
    }
}
