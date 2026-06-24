package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.EnchantmentRegistry;
import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class ElementalAffinityHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;

        // 只在服务端执行
        if (player.level().isClientSide) return;

        // 检查玩家是否穿戴了元素亲和附魔的盔甲
        if (hasElementalAffinity(player)) {
            applyElementalEffects(player);
        }
    }

    private static boolean hasElementalAffinity(Player player) {
        // 检查玩家穿戴的盔甲是否附有元素亲和附魔
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                if (!armor.isEmpty() && armor.getItem() instanceof ArmorItem) {
                    int level = EnchantmentHelper.getItemEnchantmentLevel(
                            EnchantmentRegistry.ELEMENTAL_AFFINITY.get(), armor);
                    if (level > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void applyElementalEffects(Player player) {
        // 检查玩家是否着火或在岩浆中
        if (player.isOnFire() || player.isInLava()) {
            // 给予防火效果，持续时间5秒（100 ticks），不显示粒子效果
            // 检查是否已经有防火效果，避免重复添加
            if (!player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
            }
        }

        // 检查玩家是否在水中
        if (player.isEyeInFluid(FluidTags.WATER)) {
            // 给予潮涌能量效果，持续时间10秒（200 ticks），不显示粒子效果
            // 检查是否已经有潮涌能量效果，避免重复添加
            if (!player.hasEffect(MobEffects.CONDUIT_POWER)) {
                player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 200, 0, false, false));
            }
        }

        // 检查玩家是否在细雪中
        if (isInPowderSnow(player)) {
            // 防止玩家在细雪中冰冻
            player.setTicksFrozen(0);

            // 给予缓慢效果免疫，防止细雪的减速效果
            // 检查是否已经有速度效果，避免重复添加
            if (!player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false)); // 2秒持续时间
            }
        }
    }

    private static boolean isInPowderSnow(Player player) {
        // 检查玩家是否站在细雪中
        BlockPos pos = player.blockPosition();
        BlockState blockState = player.level().getBlockState(pos);

        // 检查玩家所在的方块是否是细雪
        if (blockState.is(Blocks.POWDER_SNOW)) {
            return true;
        }

        // 检查玩家是否在细雪方块内（针对玩家陷入细雪的情况）
        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        BlockState eyeBlockState = player.level().getBlockState(eyePos);

        return eyeBlockState.is(Blocks.POWDER_SNOW);
    }
}



