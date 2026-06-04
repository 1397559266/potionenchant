package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class UltimatePotionAmuletHandler {

    private final Map<UUID, Map<String, Integer>> playerEffectTimers = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        UUID playerId = player.getUUID();

        // 初始化玩家的计时器映射
        if (!playerEffectTimers.containsKey(playerId)) {
            playerEffectTimers.put(playerId, new HashMap<>());
        }

        Map<String, Integer> timers = playerEffectTimers.get(playerId);

        // 收集所有终极药水护符上的效果并合并等级
        Map<String, MergedEffectData> mergedEffects = new HashMap<>();

        // 只检查玩家物品栏中的终极药水护符
        // 注意：Curios饰品栏中的护符由 CuriosEffectHandler 单独处理
        // 这样可以避免重复应用效果导致等级翻倍
        checkInventoryForAmulet(player, mergedEffects);

        // 应用合并后的效果
        for (MergedEffectData mergedEffect : mergedEffects.values()) {
            processMergedEffect(player, mergedEffect, timers);
        }

        // 清理不存在的效果计时器
        cleanupTimers(player, timers, mergedEffects);
    }

    // 检查玩家物品栏中的终极药水护符
    private void checkInventoryForAmulet(Player player, Map<String, MergedEffectData> mergedEffects) {
        // 使用 Set 记录已检查的物品栈，避免重复处理同一个物品
        // （主手/副手/快捷栏/背包中同一个物品可能有多处引用）
        java.util.Set<ItemStack> checkedStacks = new java.util.HashSet<>();

        // 检查主手
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            checkedStacks.add(mainHand);
            checkAmuletStackWithDedup(mainHand, mergedEffects, checkedStacks);
        }

        // 检查副手
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty()) {
            checkedStacks.add(offHand);
            checkAmuletStackWithDedup(offHand, mergedEffects, checkedStacks);
        }

        // 检查背包（槽位0-35）
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && !checkedStacks.contains(stack)) {
                checkedStacks.add(stack);
                checkAmuletStackWithDedup(stack, mergedEffects, checkedStacks);
            }
        }
    }

    // 检查单个物品栈（带去重）
    private void checkAmuletStackWithDedup(ItemStack stack, Map<String, MergedEffectData> mergedEffects, java.util.Set<ItemStack> checkedStacks) {
        if (!stack.isEmpty() && stack.getItem() == ModItems.ULTIMATE_POTION_AMULET.get()) {
            if (PotionEnchantManager.hasPotionEnchantments(stack)) {
                // 获取护符上的所有药水附魔
                List<PotionEnchantData> allEnchants = PotionEnchantManager.getPotionEnchantments(stack);

                for (PotionEnchantData enchant : allEnchants) {
                    String effectKey = enchant.getEffect().getDescriptionId();

                    // 合并相同效果的等级
                    if (!mergedEffects.containsKey(effectKey)) {
                        mergedEffects.put(effectKey, new MergedEffectData(enchant));
                    } else {
                        mergedEffects.get(effectKey).addEnchantment(enchant);
                    }
                }
            }
        }
    }

    // 处理合并后的效果
    private void processMergedEffect(Player player, MergedEffectData mergedEffect, Map<String, Integer> timers) {
        String effectKey = mergedEffect.getEffect().getDescriptionId() + "_" + mergedEffect.getAmplifier();
        MobEffectInstance currentEffect = player.getEffect(mergedEffect.getEffect());

        // 特殊处理夜视效果
        if (mergedEffect.getEffect() == net.minecraft.world.effect.MobEffects.NIGHT_VISION) {
            applyNightVisionEffect(player, mergedEffect, currentEffect, timers, effectKey);
            return;
        }

        // 检查是否需要重新给予效果（每5秒=100tick）
        int remainingTicks = timers.getOrDefault(effectKey, 0);
        boolean shouldReapply = false;
        
        if (currentEffect == null) {
            // 没有效果，立即给予
            shouldReapply = true;
        } else if (currentEffect.getAmplifier() != mergedEffect.getAmplifier()) {
            // 等级不匹配，重新给予正确等级的效果
            shouldReapply = true;
        } else if (remainingTicks <= 0) {
            // 计时器到期（5秒），重新给予效果
            shouldReapply = true;
        }
        
        if (shouldReapply) {
            applyStandardEffect(player, mergedEffect, timers, effectKey);
        } else {
            // 减少计时器
            timers.put(effectKey, remainingTicks - 1);
        }
    }

    // 应用夜视效果（特殊处理）
    private void applyNightVisionEffect(Player player, MergedEffectData mergedEffect, MobEffectInstance currentEffect, Map<String, Integer> timers, String effectKey) {
        int nightVisionDuration = 20 * 20; // 20秒
        int remainingTicks = timers.getOrDefault(effectKey, 0);

        if (currentEffect == null || remainingTicks <= 0) {
            // 没有夜视效果或计时器到期，给予20秒
            player.addEffect(createEffectInstance(mergedEffect, nightVisionDuration));
            timers.put(effectKey, 100); // 5秒后重新给予
        } else if (currentEffect.getDuration() <= 15 * 20) { // 剩余15秒时刷新
            // 夜视效果即将结束，刷新为20秒
            player.addEffect(createEffectInstance(mergedEffect, nightVisionDuration));
            timers.put(effectKey, 100); // 重置计时器
        } else {
            // 减少计时器
            timers.put(effectKey, remainingTicks - 1);
        }
    }

    // 应用标准效果
    private void applyStandardEffect(Player player, MergedEffectData mergedEffect, Map<String, Integer> timers, String effectKey) {
        int duration = 20 * 20; // 标准效果持续20秒
        // 直接添加效果，不会导致药水效果突然中断
        player.addEffect(createEffectInstance(mergedEffect, duration));

        // 设置5秒（100tick）后重新给予的计时器
        timers.put(effectKey, 100);
    }

    // 创建效果实例
    private MobEffectInstance createEffectInstance(MergedEffectData mergedEffect, int duration) {
        return new MobEffectInstance(
                mergedEffect.getEffect(),
                duration,
                mergedEffect.getAmplifier(),
                false, // 环境效果
                false, // 不显示粒子
                true   // 显示图标
        );
    }

    // 更新效果计时器
    private void updateEffectTimer(Map<String, Integer> timers, String effectKey, int remainingDuration) {
        timers.put(effectKey, remainingDuration);
    }

    // 清理过期的计时器
    private void cleanupTimers(Player player, Map<String, Integer> timers, Map<String, MergedEffectData> currentEffects) {
        timers.entrySet().removeIf(entry -> {
            String effectKey = entry.getKey();

            // 找到最后一个 "_" 的位置，以提取 effectId
            int lastUnderscore = effectKey.lastIndexOf('_');
            if (lastUnderscore <= 0) return true;

            String effectId = effectKey.substring(0, lastUnderscore);

            // 如果效果不再有来源，不立即移除，让效果自然消失
            if (!currentEffects.containsKey(effectId)) {
                return true;
            }

            // 检查玩家是否还有这个效果
            boolean hasEffect = false;
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getEffect().getDescriptionId().equals(effectId)) {
                    hasEffect = true;
                    break;
                }
            }

            // 如果玩家没有这个效果，移除计时器
            return !hasEffect;
        });
    }
    
    // 玩家登出时清理数据
    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        playerEffectTimers.remove(event.getEntity().getUUID());
    }

    // 玩家死亡时清理数据
    @SubscribeEvent
    public void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            playerEffectTimers.remove(event.getEntity().getUUID());
        }
    }

    // 玩家切换维度时清理数据
    @SubscribeEvent
    public void onPlayerChangeDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        playerEffectTimers.remove(event.getEntity().getUUID());
    }

    // 内部类：用于合并相同效果的等级
    private static class MergedEffectData {
        private final net.minecraft.world.effect.MobEffect effect;
        private int totalAmplifier;
        private int enchantmentCount;

        public MergedEffectData(PotionEnchantData firstEnchant) {
            this.effect = firstEnchant.getEffect();
            this.totalAmplifier = firstEnchant.getAmplifier() + 1; // 转换为实际等级
            this.enchantmentCount = 1;
        }

        public void addEnchantment(PotionEnchantData enchant) {
            // 确保是相同效果
            if (enchant.getEffect() == this.effect) {
                this.totalAmplifier += (enchant.getAmplifier() + 1); // 累加实际等级
                this.enchantmentCount++;
            }
        }

        public net.minecraft.world.effect.MobEffect getEffect() {
            return effect;
        }

        public int getAmplifier() {
            // 返回合并后的等级（从0开始，所以减1）
            return totalAmplifier - 1;
        }
    }
}