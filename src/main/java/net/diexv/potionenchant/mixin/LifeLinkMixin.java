package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Life Link + Fragility tick Mixin.
 * getHealth / setHealth logic merged into LivingEntityMixin.
 */
@Mixin(LivingEntity.class)
public class LifeLinkMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        // 脆弱效果：保持无敌时间为零
        try {
            if (entity.hasEffect(EffectRegistry.FRAGILITY.get())) {
                entity.invulnerableTime = 0;
            }
        } catch (Exception ignored) {}

        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        List<ItemStack> pieces = getLifeLinkPieces(player);
        if (pieces.isEmpty()) return;

        if (player.tickCount % 10 == 0) {
            healPlayer(player, pieces);
        }
        if (player.tickCount % 20 == 0) {
            balanceDurability(pieces);
        }
    }

    private static List<ItemStack> getLifeLinkPieces(Player player) {
        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) continue;
            int level = EnchantmentHelper.getItemEnchantmentLevel(
                PotionEnchantMod.LIFE_LINK.get(), stack);
            if (level > 0 && stack.getDamageValue() < stack.getMaxDamage()) {
                pieces.add(stack);
            }
        }
        return pieces;
    }

    private static void healPlayer(Player player, List<ItemStack> pieces) {
        float current = player.getHealth();
        float max = player.getMaxHealth();
        if (current >= max) return;

        float missing = max - current;
        int totalCost = (int) Math.ceil(missing);

        distributeDamageEvenly(pieces, totalCost);
        player.setHealth(max);
    }

    private static void balanceDurability(List<ItemStack> pieces) {
        if (pieces.size() < 2) return;

        int totalRemaining = 0;
        int totalMax = 0;
        for (ItemStack p : pieces) {
            totalRemaining += p.getMaxDamage() - p.getDamageValue();
            totalMax += p.getMaxDamage();
        }
        double targetPercent = 100.0 * totalRemaining / totalMax;

        for (ItemStack p : pieces) {
            int targetDur = (int)(p.getMaxDamage() * targetPercent / 100.0);
            targetDur = Math.max(1, Math.min(p.getMaxDamage(), targetDur));
            int currentDur = p.getMaxDamage() - p.getDamageValue();
            int delta = targetDur - currentDur;
            p.setDamageValue(p.getDamageValue() - delta);
        }
    }

    private static void distributeDamageEvenly(List<ItemStack> pieces, int totalCost) {
        int remaining = totalCost;
        List<ItemStack> active = new ArrayList<>(pieces);
        while (remaining > 0 && !active.isEmpty()) {
            int perPiece = Math.max(1, remaining / active.size());
            List<ItemStack> nextRound = new ArrayList<>();
            for (ItemStack piece : active) {
                int available = piece.getMaxDamage() - piece.getDamageValue() - 1;
                if (available <= 0) continue;
                int take = Math.min(perPiece, Math.min(remaining, available));
                piece.setDamageValue(piece.getDamageValue() + take);
                remaining -= take;
                if (piece.getMaxDamage() - piece.getDamageValue() - 1 > 0) {
                    nextRound.add(piece);
                }
            }
            active = nextRound;
        }
    }
}