package net.diexv.potionenchant.network;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

public class UltimateTableNetwork {
    private static final String VER = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        PotionEnchantMod.rl("ultimate_table"), () -> VER, VER::equals, VER::equals);
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, ApplyEnchantPacket.class,
            ApplyEnchantPacket::enc, ApplyEnchantPacket::dec, ApplyEnchantPacket::handle);
        CHANNEL.registerMessage(id++, ApplyPotionBatchPacket.class,
            ApplyPotionBatchPacket::enc, ApplyPotionBatchPacket::dec, ApplyPotionBatchPacket::handle);
    }

    // ====== 单个附魔应用（原逻辑）=======
    public static class ApplyEnchantPacket {
        BlockPos pos; ItemStack target; String enchantId; int level; int xpCost;

        public ApplyEnchantPacket(BlockPos p, ItemStack t, String e, int l, int xp) {
            pos = p; target = t; enchantId = e; level = l; xpCost = xp;
        }

        static void enc(ApplyEnchantPacket p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos); b.writeItem(p.target);
            b.writeUtf(p.enchantId); b.writeInt(p.level); b.writeInt(p.xpCost);
        }

        static ApplyEnchantPacket dec(FriendlyByteBuf b) {
            return new ApplyEnchantPacket(
                b.readBlockPos(), b.readItem(), b.readUtf(), b.readInt(), b.readInt());
        }

        static void handle(ApplyEnchantPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;

                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(
                    ResourceLocationHelper.parse(p.enchantId));
                if (enchantment == null) return;

                ItemStack actualTarget = findItemInInventory(sp, p.target);
                if (actualTarget == null || actualTarget.isEmpty()) return;

                boolean superMode = PotionEnchantConfig.SERVER.superEnchantMode.get();
                if (!superMode) {
                    if (!enchantment.canEnchant(actualTarget)) return;
                    Map<Enchantment, Integer> cur = EnchantmentHelper.getEnchantments(actualTarget);
                    for (Enchantment ex : cur.keySet()) {
                        if (ex != enchantment && !enchantment.isCompatibleWith(ex)) return;
                    }
                }

                if (!sp.isCreative()) {
                    int totalXp = getTotalXp(sp);
                    if (totalXp < p.xpCost) return;
                    removeXpPoints(sp, p.xpCost);
                }

                ItemStack result = actualTarget.copy();
                Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(result);
                enchants.put(enchantment, p.level);
                EnchantmentHelper.setEnchantments(enchants, result);

                replaceStackInInventory(sp, p.target, result);
                sp.level().playSound(null, p.pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f,
                    sp.level().random.nextFloat() * 0.1f + 0.9f);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ====== 批量药水效果应用（修复多个效果同时附魔的bug）=======
    public static class ApplyPotionBatchPacket {
        BlockPos pos;
        ItemStack target;  // 原始物品（用于匹配）
        int[] effectIds;   // MobEffect.getId() 数组
        int[] levels;      // 对应的等级（1-based）
        int xpCost;

        public ApplyPotionBatchPacket(BlockPos p, ItemStack t, int[] eIds, int[] lvls, int xp) {
            pos = p; target = t; effectIds = eIds; levels = lvls; xpCost = xp;
        }

        static void enc(ApplyPotionBatchPacket p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeItem(p.target);
            b.writeVarInt(p.effectIds.length);
            for (int i = 0; i < p.effectIds.length; i++) {
                b.writeInt(p.effectIds[i]);
                b.writeInt(p.levels[i]);
            }
            b.writeInt(p.xpCost);
        }

        static ApplyPotionBatchPacket dec(FriendlyByteBuf b) {
            BlockPos pos = b.readBlockPos();
            ItemStack target = b.readItem();
            int len = b.readVarInt();
            int[] eIds = new int[len];
            int[] lvls = new int[len];
            for (int i = 0; i < len; i++) {
                eIds[i] = b.readInt();
                lvls[i] = b.readInt();
            }
            int xp = b.readInt();
            return new ApplyPotionBatchPacket(pos, target, eIds, lvls, xp);
        }

        static void handle(ApplyPotionBatchPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || p.effectIds.length == 0) return;

                // 获取目标物品在背包中的实际引用
                ItemStack actualTarget = findItemInInventory(sp, p.target);
                if (actualTarget == null || actualTarget.isEmpty()) return;

                // 验证每个效果：黑名单检查 + 等级上限
                int maxLevel = PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get();
                List<PotionEnchantData> toApply = new ArrayList<>();
                for (int i = 0; i < p.effectIds.length; i++) {
                    MobEffect effect = MobEffect.byId(p.effectIds[i]);
                    if (effect == null) continue;

                    // 黑名单检查（服务端防御）
                    ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                    if (effectId != null && PotionEnchantConfig.isEffectBlacklisted(effectId)) continue;

                    // 等级上限检查
                    int level = Math.min(p.levels[i], maxLevel);
                    if (level <= 0) continue;

                    toApply.add(new PotionEnchantData(effect, level - 1, true));
                }
                if (toApply.isEmpty()) return;

                // 扣除经验值
                if (!sp.isCreative()) {
                    int totalXp = getTotalXp(sp);
                    if (totalXp < p.xpCost) return;
                    removeXpPoints(sp, p.xpCost);
                }

                // 应用到同一个物品
                ItemStack result = actualTarget.copy();
                for (PotionEnchantData data : toApply) {
                    PotionEnchantManager.addPotionEnchantment(result, data);
                }

                // 替换物品
                replaceStackInInventory(sp, p.target, result);
                sp.level().playSound(null, p.pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f,
                    sp.level().random.nextFloat() * 0.1f + 0.9f);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ====== 共享工具方法 ======

    private static boolean replaceInInventory(ServerPlayer sp, ItemStack original, ItemStack result) {
        for (int i = 0; i < sp.getInventory().items.size(); i++) {
            ItemStack slot = sp.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(slot, original)) {
                sp.getInventory().setItem(i, result);
                return true;
            }
        }
        for (var es : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (es.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR || es == net.minecraft.world.entity.EquipmentSlot.OFFHAND) {
                ItemStack slot = sp.getItemBySlot(es);
                if (ItemStack.isSameItemSameTags(slot, original)) {
                    sp.setItemSlot(es, result);
                    return true;
                }
            }
        }
        return false;
    }

    // 宽松匹配：仅根据物品类型和数量匹配，不依赖NBT一致（用于批量操作时中间状态物品）
    private static void replaceStackInInventory(ServerPlayer sp, ItemStack original, ItemStack result) {
        // 先尝试精确匹配
        if (replaceInInventory(sp, original, result)) return;
        // 精确匹配失败时，按物品ID+槽位逐个尝试
        for (int i = 0; i < sp.getInventory().items.size(); i++) {
            ItemStack slot = sp.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.getItem() == original.getItem() && slot.getCount() == original.getCount()) {
                sp.getInventory().setItem(i, result);
                return;
            }
        }
        for (var es : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (es.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR || es == net.minecraft.world.entity.EquipmentSlot.OFFHAND) {
                ItemStack slot = sp.getItemBySlot(es);
                if (!slot.isEmpty() && slot.getItem() == original.getItem() && slot.getCount() == original.getCount()) {
                    sp.setItemSlot(es, result);
                    return;
                }
            }
        }
    }

    // 在背包中查找匹配的物品（宽松匹配）
    private static ItemStack findItemInInventory(ServerPlayer sp, ItemStack target) {
        for (int i = 0; i < sp.getInventory().items.size(); i++) {
            ItemStack slot = sp.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.getItem() == target.getItem() && slot.getCount() == target.getCount()) {
                return slot;
            }
        }
        for (var es : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (es.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR || es == net.minecraft.world.entity.EquipmentSlot.OFFHAND) {
                ItemStack slot = sp.getItemBySlot(es);
                if (!slot.isEmpty() && slot.getItem() == target.getItem() && slot.getCount() == target.getCount()) {
                    return slot;
                }
            }
        }
        return null;
    }

    static int getTotalXp(ServerPlayer player) {
        int level = player.experienceLevel;
        int total = 0;
        for (int i = 0; i < level; i++) total += getXpNeeded(i);
        total += (int)(player.experienceProgress * getXpNeeded(level));
        return total;
    }

    static int getXpNeeded(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    static void removeXpPoints(ServerPlayer player, int points) {
        player.giveExperiencePoints(-points);
    }
}