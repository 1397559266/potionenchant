package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.effect.RevivalEffect;
import net.diexv.potionenchant.mixin.accessor.MobEffectInstanceAccessor;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
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
public class CombinedEffectHandler {

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
        Map<String, MergedEffectData> mergedEffects = collectAndMergeAllEffects(player);

        for (MergedEffectData mergedEffect : mergedEffects.values()) {
            processMergedEffect(player, mergedEffect, timers);
        }

        cleanupTimers(player, timers, mergedEffects);
    }

    private Map<String, MergedEffectData> collectAndMergeAllEffects(LivingEntity entity) {
        Map<String, MergedEffectData> mergedEffects = new HashMap<>();

        for (ItemStack armor : entity.getArmorSlots()) {
            if (!armor.isEmpty() && PotionEnchantManager.hasPotionEnchantments(armor)) {
                List<PotionEnchantData> armorEnchants = PotionEnchantManager.getPotionEnchantments(armor);
                mergeEffects(mergedEffects, armorEnchants);
            }
        }

        if (entity instanceof Player player && net.diexv.potionenchant.event.ArmorXFeatureHandler.isWearingFullXArmor(player)) {
            java.util.Map<net.minecraft.world.effect.MobEffect, Integer> xArmorEnchants =
                net.diexv.potionenchant.util.XArmorEnchantmentManager.getXArmorEnchantments(player);

            for (java.util.Map.Entry<net.minecraft.world.effect.MobEffect, Integer> entry : xArmorEnchants.entrySet()) {
                net.minecraft.world.effect.MobEffect effect = entry.getKey();
                int level = entry.getValue();

                if (level > 0) {
                    PotionEnchantData data = new PotionEnchantData(
                        effect,
                        level - 1,
                        true,
                        effect.getColor()
                    );

                    String effectKey = effect.getDescriptionId();
                    if (!mergedEffects.containsKey(effectKey)) {
                        mergedEffects.put(effectKey, new MergedEffectData(data));
                    } else {
                        mergedEffects.get(effectKey).addEnchantment(data);
                    }
                }
            }
        }

        return mergedEffects;
    }

    private void mergeEffects(Map<String, MergedEffectData> mergedEffects, List<PotionEnchantData> enchantments) {
        for (PotionEnchantData enchant : enchantments) {
            String effectKey = enchant.getEffect().getDescriptionId();
            if (!mergedEffects.containsKey(effectKey)) {
                mergedEffects.put(effectKey, new MergedEffectData(enchant));
            } else {
                mergedEffects.get(effectKey).addEnchantment(enchant);
            }
        }
    }

    private void processMergedEffect(LivingEntity entity, MergedEffectData mergedEffect, Map<String, Integer> timers) {
        String effectKey = mergedEffect.getEffect().getDescriptionId() + "_" + mergedEffect.getAmplifier();
        MobEffectInstance currentEffect = entity.getEffect(mergedEffect.getEffect());

        int remainingTicks = timers.getOrDefault(effectKey, 0);
        boolean shouldReapply = false;

        if (currentEffect == null) {
            if (mergedEffect.getEffect().getCategory() == MobEffectCategory.HARMFUL
                    && entity instanceof Player revivalPlayer
                    && RevivalEffect.isInRevivalCooldown(revivalPlayer)) {
                shouldReapply = false;
            } else {
                shouldReapply = true;
            }
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

        public net.minecraft.world.effect.MobEffect getEffect() { return effect; }
        public int getAmplifier() { return totalAmplifier - 1; }
        public int getEnchantmentCount() { return enchantmentCount; }
    }
}
