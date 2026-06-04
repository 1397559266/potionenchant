package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.UUID;

/**
 * 统一 Player.tick Mixin（Combo + 高级经验修补）
 */
@Mixin(Player.class)
public class PlayerTickMixin {

    private static final Random RANDOM = new Random();

    @Unique
    private static final UUID COMBO_ATTACK_SPEED_ID = UUID.fromString("87654321-4321-4321-4321-210987654321");

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTick(CallbackInfo ci) {
        Player player = (Player)(Object)this;

        if (player.level().isClientSide || player.isSpectator()) return;

        // Combo: 攻击速度加成
        ItemStack weapon = player.getMainHandItem();
        int comboLevel = weapon.getEnchantmentLevel(PotionEnchantMod.COMBO.get());
        if (comboLevel > 0) {
            applyComboAttackSpeed(player, comboLevel);
        } else {
            removeComboAttackSpeed(player);
        }

        // 高级经验修补: 每10tick用经验修复物品
        if (player.tickCount % 10 == 0) {
            ItemStack damagedItem = findDamagedAdvancedMendingItem(player);
            if (!damagedItem.isEmpty() && player.totalExperience > 0) {
                repairWithPlayerExperience(player, damagedItem);
            }
        }
    }

    // ==================== Combo helpers ====================

    @Unique
    private void applyComboAttackSpeed(Player player, int comboLevel) {
        var attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute != null) {
            attribute.removeModifier(COMBO_ATTACK_SPEED_ID);
            attribute.addPermanentModifier(new AttributeModifier(
                COMBO_ATTACK_SPEED_ID, "Combo Attack Speed", comboLevel,
                AttributeModifier.Operation.ADDITION));
        }
    }

    @Unique
    private void removeComboAttackSpeed(Player player) {
        var attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute != null) {
            attribute.removeModifier(COMBO_ATTACK_SPEED_ID);
        }
    }

    // ==================== Advanced Mending helpers ====================

    private ItemStack findDamagedAdvancedMendingItem(Player player) {
        for (ItemStack stack : player.getAllSlots()) {
            if (!stack.isEmpty() && stack.isDamaged() &&
                stack.getEnchantmentLevel(PotionEnchantMod.ADVANCED_MENDING.get()) > 0) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void repairWithPlayerExperience(Player player, ItemStack item) {
        int currentDamage = item.getDamageValue();
        if (currentDamage <= 0) return;

        int availableExperience = player.totalExperience;
        if (availableExperience <= 0) return;

        int maxRepair = availableExperience * 4;
        int actualRepair = Math.min(maxRepair, currentDamage);

        if (actualRepair > 0) {
            item.setDamageValue(currentDamage - actualRepair);
            if (!player.isCreative()) {
                int experienceUsed = (actualRepair + 3) / 4;
                player.giveExperiencePoints(-experienceUsed);
            }
        }
    }
}