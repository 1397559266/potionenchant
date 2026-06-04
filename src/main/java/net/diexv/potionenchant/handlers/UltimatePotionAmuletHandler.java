package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.item.ModItems;
import net.diexv.potionenchant.mixin.accessor.MobEffectInstanceAccessor;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
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

        if (!playerEffectTimers.containsKey(playerId)) {
            playerEffectTimers.put(playerId, new HashMap<>());
        }

        Map<String, Integer> timers = playerEffectTimers.get(playerId);

        Map<String, MergedEffectData> mergedEffects = new HashMap<>();

        checkInventoryForAmulet(player, mergedEffects);

        for (MergedEffectData mergedEffect : mergedEffects.values()) {
            processMergedEffect(player, mergedEffect, timers);
        }

        cleanupTimers(player, timers, mergedEffects);
    }

    private void checkInventoryForAmulet(Player player, Map<String, MergedEffectData> mergedEffects) {
        java.util.Set<ItemStack> checkedStacks = new java.util.HashSet<>();

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            checkedStacks.add(mainHand);
            checkAmuletStackWithDedup(mainHand, mergedEffects, checkedStacks);
        }

        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty()) {
            checkedStacks.add(offHand);
            checkAmuletStackWithDedup(offHand, mergedEffects, checkedStacks);
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && !checkedStacks.contains(stack)) {
                checkedStacks.add(stack);
                checkAmuletStackWithDedup(stack, mergedEffects, checkedStacks);
            }
        }
    }

    private void checkAmuletStackWithDedup(ItemStack stack, Map<String, MergedEffectData> mergedEffects, java.util.Set<ItemStack> checkedStacks) {
        if (!stack.isEmpty() && stack.getItem() == ModItems.ULTIMATE_POTION_AMULET.get()) {
            if (PotionEnchantManager.hasPotionEnchantments(stack)) {
                List<PotionEnchantData> allEnchants = PotionEnchantManager.getPotionEnchantments(stack);

                for (PotionEnchantData enchant : allEnchants) {
                    String effectKey = enchant.getEffect().getDescriptionId();

                    if (!mergedEffects.containsKey(effectKey)) {
                        mergedEffects.put(effectKey, new MergedEffectData(enchant));
                    } else {
                        mergedEffects.get(effectKey).addEnchantment(enchant);
                    }
                }
            }
        }
    }

    private void processMergedEffect(LivingEntity entity, MergedEffectData mergedEffect, Map<String, Integer> timers) {
        String effectKey = mergedEffect.getEffect().getDescriptionId() + "_" + mergedEffect.getAmplifier();
        MobEffectInstance currentEffect = entity.getEffect(mergedEffect.getEffect());

        if (mergedEffect.getEffect() == MobEffects.NIGHT_VISION) {
            applyNightVisionEffect(entity, mergedEffect, currentEffect, timers, effectKey);
            return;
        }

        int remainingTicks = timers.getOrDefault(effectKey, 0);
        boolean shouldReapply = false;

        if (currentEffect == null) {
            shouldReapply = true;
        } else if (currentEffect.getAmplifier() < mergedEffect.getAmplifier()) {
            shouldReapply = true;
        } else if (remainingTicks <= 0) {
            shouldReapply = true;
        }

        if (shouldReapply) {
            applyStandardEffect(entity, mergedEffect, currentEffect, timers, effectKey);
        } else {
            timers.put(effectKey, remainingTicks - 1);
        }
    }

    private void applyNightVisionEffect(LivingEntity entity, MergedEffectData mergedEffect, MobEffectInstance currentEffect, Map<String, Integer> timers, String effectKey) {
        int nightVisionDuration = 20 * 20;
        int remainingTicks = timers.getOrDefault(effectKey, 0);

        if (currentEffect == null || remainingTicks <= 0) {
            refreshDirect(entity, mergedEffect, currentEffect, nightVisionDuration);
            timers.put(effectKey, 100);
        } else if (currentEffect.getDuration() <= 15 * 20) {
            refreshDirect(entity, mergedEffect, currentEffect, nightVisionDuration);
            timers.put(effectKey, 100);
        } else {
            timers.put(effectKey, remainingTicks - 1);
        }
    }

    private void applyStandardEffect(LivingEntity entity, MergedEffectData mergedEffect, MobEffectInstance currentEffect, Map<String, Integer> timers, String effectKey) {
        int duration = 20 * 20;
        refreshDirect(entity, mergedEffect, currentEffect, duration);
        timers.put(effectKey, 100);
    }

    // 统一的刷新逻辑：已有同等级效果→只延长持续时间（无副作用），否则 addEffect
    private void refreshDirect(LivingEntity entity, MergedEffectData mergedEffect, MobEffectInstance currentEffect, int duration) {
        if (currentEffect != null && currentEffect.getAmplifier() == mergedEffect.getAmplifier()) {
            // 已有同等级效果 → 直接延长持续时间，不触发 onEffectUpdated 的副作用
            ((MobEffectInstanceAccessor) currentEffect).setDuration(duration);
            if (!entity.level().isClientSide) {
                ((ServerLevel)entity.level()).getChunkSource().broadcastAndSend(entity,
                    new ClientboundUpdateMobEffectPacket(entity.getId(), currentEffect));
            }
        } else {
            // 新效果或升等级 → 正常 addEffect
            entity.addEffect(createEffectInstance(mergedEffect, duration));
        }
    }

    private MobEffectInstance createEffectInstance(MergedEffectData mergedEffect, int duration) {
        return new MobEffectInstance(
                mergedEffect.getEffect(),
                duration,
                mergedEffect.getAmplifier(),
                false,
                false,
                true
        );
    }

    private void cleanupTimers(LivingEntity entity, Map<String, Integer> timers, Map<String, MergedEffectData> currentEffects) {
        timers.entrySet().removeIf(entry -> {
            String effectKey = entry.getKey();

            int lastUnderscore = effectKey.lastIndexOf('_');
            if (lastUnderscore <= 0) return true;

            String effectId = effectKey.substring(0, lastUnderscore);

            if (!currentEffects.containsKey(effectId)) {
                return true;
            }

            boolean hasEffect = false;
            for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
                if (effect.getEffect().getDescriptionId().equals(effectId)) {
                    hasEffect = true;
                    break;
                }
            }

            return !hasEffect;
        });
    }

    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        playerEffectTimers.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        playerEffectTimers.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        playerEffectTimers.remove(event.getEntity().getUUID());
    }

    private static class MergedEffectData {
        private final net.minecraft.world.effect.MobEffect effect;
        private int totalAmplifier;
        private int enchantmentCount;

        public MergedEffectData(PotionEnchantData firstEnchant) {
            this.effect = firstEnchant.getEffect();
            this.totalAmplifier = firstEnchant.getAmplifier() + 1;
            this.enchantmentCount = 1;
        }

        public void addEnchantment(PotionEnchantData enchant) {
            if (enchant.getEffect() == this.effect) {
                this.totalAmplifier += (enchant.getAmplifier() + 1);
                this.enchantmentCount++;
            }
        }

        public net.minecraft.world.effect.MobEffect getEffect() {
            return effect;
        }

        public int getAmplifier() {
            return totalAmplifier - 1;
        }
    }
}
