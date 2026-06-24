// ReforgeMixin.java
package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.EnchantmentRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ItemStack.class)
public class ReforgeMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void onHurt(int amount, net.minecraft.util.RandomSource random, net.minecraft.server.level.ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;

        if (!stack.isDamageableItem()) {
            return;
        }

        int reforgeLevel = stack.getEnchantmentLevel(EnchantmentRegistry.REFORGE.get());

        if (reforgeLevel > 0 && stack.getDamageValue() + amount >= stack.getMaxDamage()) {
            // 触发重铸效果
            triggerReforgeMixin(stack, reforgeLevel, player);
            cir.setReturnValue(false); // 取消耐久度消耗
        }
    }

    private void triggerReforgeMixin(ItemStack stack, int currentLevel, net.minecraft.server.level.ServerPlayer player) {
        // 保存完整的 NBT 数据
        CompoundTag originalTag = stack.getTag() != null ? stack.getTag().copy() : new CompoundTag();

        // 保存所有附魔（除了重铸）
        Map<Enchantment, Integer> enchantments = new java.util.HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : stack.getAllEnchantments().entrySet()) {
            if (entry.getKey() != EnchantmentRegistry.REFORGE.get()) {
                enchantments.put(entry.getKey(), entry.getValue());
            }
        }

        // 回满耐久度
        stack.setDamageValue(0);

        // 附魔等级-1
        int newLevel = currentLevel - 1;

        // 方法1：完全保留所有NBT数据，只修改耐久度和附魔
        // 先恢复所有原始NBT数据
        if (stack.getTag() == null) {
            stack.setTag(new CompoundTag());
        }

        // 复制所有原始NBT数据
        for (String key : originalTag.getAllKeys()) {
            stack.getTag().put(key, originalTag.get(key).copy());
        }

        // 然后修改耐久度（确保耐久度被正确设置为0）
        stack.setDamageValue(0);

        // 清除所有附魔
        stack.removeTagKey("Enchantments");

        // 重新添加所有附魔（除了重铸）
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            stack.enchant(entry.getKey(), entry.getValue());
        }

        // 如果新等级大于0，添加新的重铸附魔等级
        if (newLevel > 0) {
            stack.enchant(EnchantmentRegistry.REFORGE.get(), newLevel);
        }

        // 播放音效和粒子效果
        if (player != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ANVIL_USE,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0F, 1.0F);

            for (int i = 0; i < 5; i++) {
                double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2;
                double y = player.getY() + player.getRandom().nextDouble() * 2;
                double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2;

                player.level().addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        x, y, z, 0, 0, 0);
            }

            // 发送消息给玩家
            if (newLevel > 0) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.plentyofenchant.reforge_triggered",
                                stack.getHoverName().getString(), newLevel),
                        true
                );
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.plentyofenchant.reforge_final",
                                stack.getHoverName().getString()),
                        true
                );
            }
        }
    }
}


