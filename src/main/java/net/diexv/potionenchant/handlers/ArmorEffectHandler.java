package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.mixin.accessor.MobEffectInstanceAccessor;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArmorEffectHandler {

    private final Map<UUID, Map<String, Integer>> entityEffectTimers = new HashMap<>();

    @SubscribeEvent
    public void onLivingEntityTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity instanceof Player) {
            return;
        }

        UUID entityId = entity.getUUID();

        if (!entityEffectTimers.containsKey(entityId)) {
            entityEffectTimers.put(entityId, new HashMap<>());
        }

        Map<String, Integer> timers = entityEffectTimers.get(entityId);
        Map<String, MergedEffectData> mergedEffects = new HashMap<>();

        for (ItemStack armor : entity.getArmorSlots()) {
            if (!armor.isEmpty() && PotionEnchantManager.hasPotionEnchantments(armor)) {
                List<PotionEnchantData> armorEnchants = PotionEnchantManager.getPotionEnchantments(armor);

                for (PotionEnchantData enchant : armorEnchants) {
                    String effectKey = enchant.getEffect().getDescriptionId();
                    if (!mergedEffects.containsKey(effectKey)) {
                        mergedEffects.put(effectKey, new MergedEffectData(enchant));
                    } else {
                        mergedEffects.get(effectKey).addEnchantment(enchant);
                    }
                }
            }
        }

        for (MergedEffectData mergedEffect : mergedEffects.values()) {
            processMergedEffect(entity, mergedEffect, timers);
        }

        cleanupTimers(entity, timers, mergedEffects);
    }

    private void processMergedEffect(LivingEntity entity, MergedEffectData mergedEffect, Map<String, Integer> timers) {
        String effectKey = mergedEffect.getEffect().getDescriptionId() + "_" + mergedEffect.getAmplifier();
        MobEffectInstance currentEffect = entity.getEffect(mergedEffect.getEffect());

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
            refreshEffect(entity, mergedEffect, currentEffect, timers, effectKey);
        } else {
            timers.put(effectKey, remainingTicks - 1);
        }
    }

    // 刷新效果：已有同等级效果→只延长持续时间（无副作用），否则 addEffect
    private void refreshEffect(LivingEntity entity, MergedEffectData mergedEffect, MobEffectInstance currentEffect,
                               Map<String, Integer> timers, String effectKey) {
        int effectDuration = 20 * 20;

        if (currentEffect != null && currentEffect.getAmplifier() == mergedEffect.getAmplifier()) {
            // 已有同等级效果 → 直接延长持续时间，不触发 onEffectUpdated 的副作用
            ((MobEffectInstanceAccessor) currentEffect).setDuration(effectDuration);
            if (!entity.level().isClientSide) {
                ((ServerLevel)entity.level()).getChunkSource().broadcastAndSend(entity,
                    new ClientboundUpdateMobEffectPacket(entity.getId(), currentEffect));
            }
        } else {
            // 新效果或升等级 → 正常 addEffect
            entity.addEffect(new MobEffectInstance(
                mergedEffect.getEffect(),
                effectDuration,
                mergedEffect.getAmplifier(),
                false,
                false,
                true
            ));
        }

        timers.put(effectKey, 100);
    }

    private void cleanupTimers(LivingEntity entity, Map<String, Integer> timers, Map<String, MergedEffectData> currentEffects) {
        timers.entrySet().removeIf(entry -> {
            String effectKey = entry.getKey();
            int lastUnderscore = effectKey.lastIndexOf('_');
            if (lastUnderscore <= 0) return true;

            String effectId = effectKey.substring(0, lastUnderscore);
            boolean hasEffect = false;
            for (MobEffectInstance effect : entity.getActiveEffects()) {
                if (effect.getEffect().getDescriptionId().equals(effectId)) {
                    hasEffect = true;
                    break;
                }
            }

            return !hasEffect || !currentEffects.containsKey(effectId);
        });
    }

    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        entityEffectTimers.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        entityEffectTimers.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        entityEffectTimers.remove(event.getEntity().getUUID());
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

        public net.minecraft.world.effect.MobEffect getEffect() { return effect; }
        public int getAmplifier() { return totalAmplifier - 1; }
        public int getEnchantmentCount() { return enchantmentCount; }
    }
}
