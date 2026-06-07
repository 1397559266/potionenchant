package net.diexv.potionenchant.network;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.function.Supplier;

public class UltimateTableNetwork {
    private static final String VER = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        PotionEnchantMod.rl("ultimate_table"), () -> VER, VER::equals, VER::equals);
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, ApplyEnchantPacket.class,
            ApplyEnchantPacket::enc, ApplyEnchantPacket::dec, ApplyEnchantPacket::handle);
    }

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

                // Deduct XP points
                if (!sp.isCreative()) {
                    int totalXp = getTotalXp(sp);
                    if (totalXp < p.xpCost) return;
                    removeXpPoints(sp, p.xpCost);
                }

                ItemStack result = p.target.copy();

                // Apply enchantment (override level)
                Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(result);
                enchants.put(enchantment, p.level);
                EnchantmentHelper.setEnchantments(enchants, result);

                // Replace in inventory
                for (int i = 0; i < sp.getInventory().items.size(); i++) {
                    ItemStack slot = sp.getInventory().getItem(i);
                    if (ItemStack.isSameItemSameTags(slot, p.target)) {
                        sp.getInventory().setItem(i, result);
                        sp.level().playSound(null, p.pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f,
                            sp.level().random.nextFloat() * 0.1f + 0.9f);
                        return;
                    }
                }
                for (var es : net.minecraft.world.entity.EquipmentSlot.values()) {
                    if (es.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR || es == net.minecraft.world.entity.EquipmentSlot.OFFHAND) {
                        ItemStack slot = sp.getItemBySlot(es);
                        if (ItemStack.isSameItemSameTags(slot, p.target)) {
                            sp.setItemSlot(es, result);
                            sp.level().playSound(null, p.pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f,
                                sp.level().random.nextFloat() * 0.1f + 0.9f);
                            return;
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }

        private static int getTotalXp(ServerPlayer player) {
            int level = player.experienceLevel;
            int total = 0;
            for (int i = 0; i < level; i++) total += getXpNeeded(i);
            total += (int)(player.experienceProgress * getXpNeeded(level));
            return total;
        }

        private static int getXpNeeded(int level) {
            if (level >= 30) return 112 + (level - 30) * 9;
            if (level >= 15) return 37 + (level - 15) * 5;
            return 7 + level * 2;
        }

        private static void removeXpPoints(ServerPlayer player, int points) {
            int total = getTotalXp(player) - points;
            if (total <= 0) {
                player.experienceLevel = 0;
                player.experienceProgress = 0;
                return;
            }
            int newLevel = 0;
            int remain = total;
            while (true) {
                int need = getXpNeeded(newLevel);
                if (remain >= need) { remain -= need; newLevel++; }
                else break;
            }
            player.experienceLevel = newLevel;
            player.experienceProgress = (float)remain / (float)getXpNeeded(newLevel);
        }
    }
}
