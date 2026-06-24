package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.EnchantmentRegistry;
import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.effect.FirmnessEffect;
import net.diexv.potionenchant.item.XSwordItem;
import net.diexv.potionenchant.event.ArmorXFeatureHandler;
import net.diexv.potionenchant.mixin.accessor.LivingEntityAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(value = {LivingEntity.class}, priority = 2147483647)
public abstract class LivingEntityMixin {

    @Shadow
    public abstract float getMaxHealth();

    // ==================== getHealth ====================

    @Inject(method = {"getHealth"}, at = {@At("RETURN")}, cancellable = true)
    private void getHealth(@NotNull CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;

        // 1. XSword 超级模式（死亡时返回0以允许重生）
        if (entity instanceof Player player && XSwordItem.isSupermode(player.getUUID())) {
            boolean isDead = false;
            try { isDead = ((LivingEntityAccessor)(Object)entity).dead(); } catch (Exception ignored) {}
            cir.setReturnValue(isDead ? 0.0F : getMaxHealth());
            return;
        }

        // 2. X护甲毁灭模式 —— 玩家满血
        if (entity instanceof Player armorPlayer && armorPlayer.getInventory() != null && ArmorXFeatureHandler.isWearingFullXArmor(armorPlayer) && ArmorXFeatureHandler.isDestructionModeEnabled(armorPlayer)) {
            cir.setReturnValue(getMaxHealth());
            return;
        }

        // 3. 生命链接 —— 最低血量保护
        if (entity instanceof Player player2) {
            if (player2.getInventory() == null) return;
            List<ItemStack> pieces = getLifeLinkPieces(player2);
            if (!pieces.isEmpty()) {
                float health = cir.getReturnValue();
                if (health < 1.0F) {
                    cir.setReturnValue(1.0F);
                }
            }
        }
    }

    // ==================== setHealth ====================

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        // 1. 生命链接
        if (entity instanceof Player player) {
            if (player.getInventory() != null) {
                List<ItemStack> pieces = getLifeLinkPieces(player);
                if (!pieces.isEmpty() && health < 1.0F) {
                    if (health < -10000.0F) return;
                    int totalCost = (int) Math.ceil(1.0F - health);
                    int remaining = distributeDamageEvenly(pieces, totalCost);
                    float newHealth = Math.max(0.0F, 1.0F - remaining);
                    player.getEntityData().set(LivingEntityAccessor.HEALTH(), newHealth);
                    if (newHealth > 0.0F) {
                        ci.cancel();
                    }
                    return;
                }
            }
        }

        // 2. 坚韧效果
        if (entity.hasEffect(EffectRegistry.FIRMNESS.get())) {
            UUID uuid = entity.getUUID();
            if (FirmnessEffect.isHealthLocked(uuid)) {
                float lockedHealth = FirmnessEffect.getLockedHealth(uuid);
                float currentHealth = entity.getEntityData().get(LivingEntityAccessor.HEALTH());
                if (health < currentHealth && health < lockedHealth) {
                    FirmnessEffect.reduceLockedHealth(uuid, lockedHealth - health);
                    entity.getEntityData().set(LivingEntityAccessor.HEALTH(), lockedHealth);
                    ci.cancel();
                    return;
                }
            }
        }

        if (!(entity instanceof Player player)) return;

        // 3. X护甲毁灭模式 —— 玩家满血
        if (player.getInventory() != null && ArmorXFeatureHandler.isWearingFullXArmor(player) && ArmorXFeatureHandler.isDestructionModeEnabled(player)) {
            float maxHealth = getMaxHealth();
            if (health != maxHealth) {
                entity.getEntityData().set(LivingEntityAccessor.HEALTH(), maxHealth);
                ci.cancel();
            }
            return;
        }

        // 4. XSword 超级模式
        if (XSwordItem.isSupermode(player.getUUID())) {
            float maxHealth = getMaxHealth();
            boolean isDead = false;
            try { isDead = ((LivingEntityAccessor)(Object)entity).dead(); } catch (Exception ignored) {}
            if (isDead && health <= 0.0F) {
                return;
            }
            if (health < maxHealth) {
                entity.getEntityData().set(LivingEntityAccessor.HEALTH(), maxHealth);
                ci.cancel();
            }
            return;
        }

        // 5. XSword 普通模式
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof XSwordItem) {
            if (health < player.getHealth()) {
                ci.cancel();
            }
            return;
        }
    }

    // ==================== Life Link helpers ====================

    @Unique
    private static List<ItemStack> getLifeLinkPieces(Player player) {
        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) continue;
            int level = EnchantmentHelper.getItemEnchantmentLevel(
                EnchantmentRegistry.LIFE_LINK.get(), stack);
            if (level > 0 && stack.getDamageValue() < stack.getMaxDamage()) {
                pieces.add(stack);
            }
        }
        return pieces;
    }

    @Unique
    private static int distributeDamageEvenly(List<ItemStack> pieces, int totalCost) {
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
        return remaining;
    }
}


