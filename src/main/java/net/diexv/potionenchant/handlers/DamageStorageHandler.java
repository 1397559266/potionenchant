package net.diexv.potionenchant.handlers;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.diexv.potionenchant.config.values.EnchantmentConfigValues;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID)
public class DamageStorageHandler {

    // 存储玩家的伤害储存数据
    private static final Map<UUID, PlayerDamageStorageData> playerDataMap = new HashMap<>();

    // 玩家伤害储存数据结构
    public static class PlayerDamageStorageData {
        public float storedDamage = 0.0f;
        public long lastDamageTime = 0;
        public int maxStorageLevel = 0;
        public static float getMaxStorageMultiplier() {
        return (float)(double)EnchantmentConfigValues.CONFIG.damageStorageMaxMultiplier.get();
    } // 最大储存量为玩家最大生命值的10倍
        public long lastActionBarTime = 0; // 最后显示动作栏的时间
        public static final long ACTION_BAR_COOLDOWN = 500; // 动作栏冷却时间0.5秒
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 处理玩家受到伤害，储存伤害
        if (event.getEntity() instanceof Player player) {
            // 获取玩家所有盔甲上的伤害储存等级总和
            int totalStorageLevel = getTotalStorageLevel(player);

            if (totalStorageLevel > 0) {
                // 获取或创建玩家数据
                PlayerDamageStorageData data = playerDataMap.computeIfAbsent(
                        player.getUUID(), k -> new PlayerDamageStorageData());

                // 更新最大储存等级
                data.maxStorageLevel = totalStorageLevel;

                // 计算最大储存量（玩家最大生命值的10倍）
                int totalLvl = getTotalStorageLevel(player);
                float maxStorage = player.getMaxHealth() * getMaxStorageMultiplier() * Math.max(1, totalLvl);

                // 清理超过5秒的伤害记录（重置储存）
                long currentTime = System.currentTimeMillis();
                if (currentTime - data.lastDamageTime > EnchantmentConfigValues.CONFIG.damageStorageDecaySeconds.get() * 1000L) { // 60秒
                    data.storedDamage = 0.0f;
                }

                // 计算储存比例（1级100%，2级200%，3级300%）
                float storageMultiplier = 1.0f * totalStorageLevel;

                // 计算可以储存的伤害量
                float damageToStore = event.getAmount() * storageMultiplier;

                // 储存前的伤害量（用于显示变化）
                float previousStoredDamage = data.storedDamage;

                // 检查是否超过最大储存量
                if (data.storedDamage + damageToStore <= maxStorage) {
                    data.storedDamage += damageToStore;
                } else {
                    // 超过最大储存量，只储存到最大量
                    data.storedDamage = maxStorage;
                }

                // 更新最后受伤时间
                data.lastDamageTime = currentTime;

                // 发送动作栏消息给玩家，显示储存的伤害量和进度条
                sendActionBarMessage(player, data.storedDamage, maxStorage, damageToStore, data);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        // 处理玩家造成伤害，释放储存的伤害
        if (event.getSource().getDirectEntity() instanceof Player attacker) {
            // 检查攻击者是否有伤害储存附魔
            PlayerDamageStorageData data = playerDataMap.get(attacker.getUUID());

            if (data != null && data.storedDamage > 0) {
                // 记录释放的伤害量
                float releasedDamage = data.storedDamage;

                // 释放所有储存的伤害
                event.setAmount(event.getAmount() + releasedDamage);

                // 发送动作栏消息，显示释放的伤害
                Component releaseMessage = Component.translatable("message.plentyofenchant.damage_storage.released",
                        String.format("%.1f", releasedDamage)).withStyle(ChatFormatting.GOLD);
                attacker.displayClientMessage(releaseMessage, true); // true表示显示在动作栏

                // 重置储存的伤害
                data.storedDamage = 0.0f;

                // 可选：添加视觉效果或声音
                // attacker.level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                //     SoundEvents.LIGHTNING_BOLT_THUNDER, attacker.getSoundSource(), 1.0F, 1.0F);
            }
        }
    }

    // 发送动作栏消息
    private static void sendActionBarMessage(Player player, float storedDamage, float maxStorage,
                                             float damageStored, PlayerDamageStorageData data) {
        long currentTime = System.currentTimeMillis();

        // 检查动作栏冷却时间
        if (currentTime - data.lastActionBarTime < PlayerDamageStorageData.ACTION_BAR_COOLDOWN) {
            return;
        }

        // 更新最后动作栏时间
        data.lastActionBarTime = currentTime;

        // 计算进度百分比
        float progress = maxStorage > 0 ? storedDamage / maxStorage : 0;

        // 创建进度条
        String progressBar = createProgressBar(progress, 20);

        // 创建动作栏消息
        Component actionBarMessage = Component.literal("")
                .append(Component.translatable("message.plentyofenchant.damage_storage.stored").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(Component.literal(String.format("%.1f", storedDamage)).withStyle(ChatFormatting.RED))
                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.1f", maxStorage)).withStyle(ChatFormatting.RED))
                .append(Component.literal(" "))
                .append(Component.literal(progressBar).withStyle(getProgressBarColor(progress)));

        // 如果储存了新的伤害，添加增加量提示
        if (damageStored > 0) {
            actionBarMessage = actionBarMessage.copy()
                    .append(Component.literal(" "))
                    .append(Component.literal("(+").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(String.format("%.1f", damageStored)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(")").withStyle(ChatFormatting.GREEN));
        }

        // 发送动作栏消息
        player.displayClientMessage(actionBarMessage, true); // true表示显示在动作栏
    }

    // 根据进度获取进度条颜色
    private static ChatFormatting getProgressBarColor(float progress) {
        if (progress < 0.3f) {
            return ChatFormatting.GREEN;
        } else if (progress < 0.7f) {
            return ChatFormatting.YELLOW;
        } else {
            return ChatFormatting.RED;
        }
    }

    // 创建进度条
    private static String createProgressBar(float progress, int length) {
        int filled = (int) (progress * length);
        filled = Math.min(filled, length);
        int empty = length - filled;

        StringBuilder bar = new StringBuilder();
        bar.append("[");

        for (int i = 0; i < filled; i++) {
            bar.append("|");
        }

        for (int i = 0; i < empty; i++) {
            bar.append(" ");
        }

        bar.append("]");

        return bar.toString();
    }

    // 获取玩家所有盔甲上的伤害储存等级总和
    private static int getTotalStorageLevel(Player player) {
        int totalLevel = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                if (!armor.isEmpty() && armor.getItem() instanceof ArmorItem) {
                    int level = EnchantmentHelper.getItemEnchantmentLevel(
                            PotionEnchantMod.DAMAGE_STORAGE.get(), armor);
                    totalLevel += level;
                }
            }
        }

        return totalLevel;
    }

    // 新增：获取玩家储存的伤害量
    public static float getStoredDamage(Player player) {
        PlayerDamageStorageData data = playerDataMap.get(player.getUUID());
        if (data != null) {
            // 检查是否超过60秒，如果是则重置
            long currentTime = System.currentTimeMillis();
            if (currentTime - data.lastDamageTime > EnchantmentConfigValues.CONFIG.damageStorageDecaySeconds.get() * 1000L) {
                data.storedDamage = 0.0f;
            }
            return data.storedDamage;
        }
        return 0.0f;
    }

    // 新增：获取玩家的最大储存量
    public static float getMaxStorage(Player player) {
        return player.getMaxHealth() * DamageStorageHandler.getMaxStorageMultiplier();
    }

    // 新增：获取玩家的伤害储存等级
    public static int getStorageLevel(Player player) {
        return getTotalStorageLevel(player);
    }
}



