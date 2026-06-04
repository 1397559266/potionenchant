package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class BarrageHandler {

    // 存储玩家的万箭齐发状态
    private static final Map<UUID, BarrageData> playerBarrageData = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        UUID playerId = player.getUUID();

        // 只在服务端处理
        if (player.level().isClientSide) {
            return;
        }

        // 获取玩家手中的弓
        ItemStack bow = player.getMainHandItem();
        int barrageLevel = bow.getEnchantmentLevel(PotionEnchantMod.BARRAGE.get());

        // 检查弓是否有万箭齐发附魔
        if (barrageLevel > 0) {
            // 检查玩家是否正在使用弓（拉弓状态）
            if (player.isUsingItem() && player.getUseItem().getItem() == Items.BOW) {
                // 确保玩家有万箭齐发数据
                BarrageData data = playerBarrageData.computeIfAbsent(playerId, id -> new BarrageData());

                // 处理万箭齐发效果
                handleBarrageEffect(player, bow, data);
            } else {
                // 玩家没有使用弓，清除数据
                playerBarrageData.remove(playerId);
            }
        } else {
            // 弓没有万箭齐发附魔，清除数据
            playerBarrageData.remove(playerId);
        }
    }

    private static void handleBarrageEffect(Player player, ItemStack bow, BarrageData data) {
        // 获取拉弓进度（0.0 - 1.0）
        int useDuration = player.getTicksUsingItem();
        float pullProgress = getPullProgress(useDuration);

        // 只有在拉弓进度达到一定程度后才开始发射
        if (pullProgress < 0.0f) {
            data.lastFireTick = player.tickCount;
            return;
        }

        // 检查发射冷却
        int currentTick = player.tickCount;
        if (currentTick - data.lastFireTick < data.fireRate) {
            return;
        }

        // 检查玩家是否有箭矢
        if (!hasArrows(player, bow)) {
            return;
        }

        // 发射箭矢（无视无敌帧版本）
        fireBarrageArrowIgnoreInvulnerability(player, bow, pullProgress);

        // 更新最后发射时间
        data.lastFireTick = currentTick;

        // 只有在没有无限附魔时才消耗箭矢
        int infinityLevel = bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS);
        if (infinityLevel == 0) {
            consumeArrow(player);
        }

        // 减少弓的耐久度
        if (!player.getAbilities().instabuild) {
            bow.hurtAndBreak(1, player, (p) -> {
                p.broadcastBreakEvent(player.getUsedItemHand());
            });
        }
    }

    private static float getPullProgress(int useTicks) {
        float progress = (float)useTicks / 20.0F;
        if (progress > 1.0F) {
            progress = 1.0F;
        }
        return progress;
    }

    private static boolean hasArrows(Player player, ItemStack bow) {
        // 创造模式或有无限附魔时，只需要至少一支箭矢
        if (player.getAbilities().instabuild || bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS) > 0) {
            // 检查是否至少有一支箭矢
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof ArrowItem) {
                    return true;
                }
            }
            return false;
        }

        // 生存模式且没有无限附魔时，正常检查箭矢
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ArrowItem) {
                return true;
            }
        }

        return false;
    }

    private static void consumeArrow(Player player) {
        // 消耗一支箭矢
        if (player.getAbilities().instabuild) {
            return;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ArrowItem) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }
    }

    // 无视无敌帧的箭矢发射方法
    private static void fireBarrageArrowIgnoreInvulnerability(Player player, ItemStack bow, float pullProgress) {
        Level level = player.level();

        // 创建箭矢
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ((ArrowItem)Items.ARROW).createArrow(level, arrowStack, player);

        // 设置箭矢属性
        float velocity = 3.0F * pullProgress;
        if (velocity < 0.1F) {
            velocity = 0.1F;
        }

        // 关键修改：检查弓是否有追踪附魔，如果有则给箭矢添加追踪标记
        int trackingLevel = bow.getEnchantmentLevel(PotionEnchantMod.TRACKING_ARROW.get());
        if (trackingLevel > 0) {
            // 为箭矢添加追踪标记，让万箭齐发的箭矢也能传送和追踪
            arrow.getPersistentData().putBoolean("plentyofenchant:has_tracking", true);
        }

        // 计算随机散布
        float spread = 5.0F; // 散布角度

        // 计算发射方向（添加随机散布）
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float randomYaw = yaw + (level.random.nextFloat() - 0.5F) * spread;
        float randomPitch = pitch + (level.random.nextFloat() - 0.5F) * spread;

        // 设置箭矢运动
        arrow.shootFromRotation(player, randomPitch, randomYaw, 0.0F, velocity, spread);

        // 应用弓的附魔效果
        int powerLevel = bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS);
        if (powerLevel > 0) {
            arrow.setBaseDamage(arrow.getBaseDamage() + (double)powerLevel * 0.5D + 0.5D);
        }

        int punchLevel = bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.PUNCH_ARROWS);
        if (punchLevel > 0) {
            arrow.setKnockback(punchLevel);
        }

        int flameLevel = bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FLAMING_ARROWS);
        if (flameLevel > 0) {
            arrow.setSecondsOnFire(100);
        }

        // 检查弓是否有无限附魔，如果有则设置箭矢不可捡起
        int infinityLevel = bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS);
        if (infinityLevel > 0) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }

        // 关键修改：为箭矢添加自定义标签，标记为无视无敌帧的箭矢
        arrow.getPersistentData().putBoolean("plentyofenchant:ignore_invulnerability", true);

        // 播放射箭音效
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ARROW_SHOOT,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F, 1.0F / (level.random.nextFloat() * 0.4F + 1.2F) + pullProgress * 0.5F);

        // 将箭矢添加到世界
        level.addFreshEntity(arrow);

        // 播放粒子效果
        if (!level.isClientSide) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
            for (int i = 0; i < 3; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
                double offsetY = level.random.nextDouble() * 0.5;
                double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        player.getX(),
                        player.getY() + player.getEyeHeight(),
                        player.getZ(),
                        1, offsetX, offsetY, offsetZ, 0.1);
            }
        }
    }

    // 万箭齐发数据类
    private static class BarrageData {
        public int lastFireTick = 0;
        public int fireRate = 1; // 每1tick发射一支箭
    }
}
