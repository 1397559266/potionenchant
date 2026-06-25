package net.diexv.potionenchant.network;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.data.PotionEnchantData;
import net.minecraft.resources.ResourceLocation;
import net.diexv.potionenchant.data.BonusEffect;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PotionEnchantTableNetwork {
    private static final String VER = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        PotionEnchantMod.rl("pe_table"), () -> VER, VER::equals, VER::equals);
    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, ApplyEffectPacket.class, ApplyEffectPacket::enc, ApplyEffectPacket::dec, ApplyEffectPacket::handle);
    }

    public static class ApplyEffectPacket {
        BlockPos pos; ItemStack target; MobEffect effect; int level;
        int cost;
        List<BonusEffect> bonuses;

        public ApplyEffectPacket(BlockPos p, ItemStack t, MobEffect e, int l, int cost,
                                  List<BonusEffect> b) {
            pos = p; target = t; effect = e; level = l; this.cost = cost; bonuses = b;
        }

        static void enc(ApplyEffectPacket p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos); b.writeItem(p.target);
            b.writeInt(MobEffect.getId(p.effect)); b.writeInt(p.level); b.writeInt(p.cost);
            b.writeInt(p.bonuses.size());
            for (var be : p.bonuses) {
                b.writeInt(MobEffect.getId(be.effect)); b.writeInt(be.level);
            }
        }

        static ApplyEffectPacket dec(FriendlyByteBuf b) {
            BlockPos pos = b.readBlockPos();
            ItemStack target = b.readItem();
            MobEffect eff = MobEffect.byId(b.readInt());
            int lvl = b.readInt();
            int cost = b.readInt();
            int count = b.readInt();
            List<BonusEffect> bonuses = new ArrayList<>();
            for (int i = 0; i < count; i++)
                bonuses.add(new BonusEffect(MobEffect.byId(b.readInt()), b.readInt()));
            return new ApplyEffectPacket(pos, target, eff, lvl, cost, bonuses);
        }

        static void handle(ApplyEffectPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || p.effect == null) return;

                // Validate and deduct XP on server
                if (!sp.isCreative()) {
                    if (sp.experienceLevel < p.cost) return;
                    sp.giveExperienceLevels(-p.cost);
                }

                // Blacklist check (server-side defense)
                ResourceLocation effectRL = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(p.effect);
                if (effectRL != null && PotionEnchantConfig.isEffectBlacklisted(effectRL)) return;
                
                // Level cap check
                int maxLvl = PotionEnchantConfig.SERVER.maxPotionEnchantLevel.get();
                if (p.level > maxLvl) return;

                // Check enchant limits
                List<PotionEnchantData> existing = PotionEnchantManager.getPotionEnchantments(p.target);
                for (PotionEnchantData ed : existing) {
                    if (ed.getEffect() == p.effect && ed.getAmplifier() + 1 >= p.level) return;
                }
                if (PotionEnchantConfig.SERVER.limitAllEnchants.get()) {
                    long count = existing.stream().map(PotionEnchantData::getEffect).distinct().count();
                    boolean has = existing.stream().anyMatch(en -> en.getEffect() == p.effect);
                    if (!has && count >= PotionEnchantConfig.SERVER.maxAllEnchants.get()) return;
                }

                ItemStack result = p.target.copy();

                // Apply main effect
                PotionEnchantManager.addPotionEnchantment(result,
                    new PotionEnchantData(p.effect, p.level - 1, true));

                // Apply bonus effects
                for (var be : p.bonuses) {
                    if (be.effect != null) {
                        PotionEnchantManager.addPotionEnchantment(result,
                            new PotionEnchantData(be.effect, be.level - 1, true));
                    }
                }

                // Replace in player inventory
                boolean replaced = false;
                for (int i = 0; i < sp.getInventory().items.size(); i++) {
                    ItemStack slot = sp.getInventory().getItem(i);
                    if (ItemStack.isSameItemSameTags(slot, p.target)) {
                        sp.getInventory().setItem(i, result);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) {
                    for (var es : net.minecraft.world.entity.EquipmentSlot.values()) {
                        if (es.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR || es == net.minecraft.world.entity.EquipmentSlot.OFFHAND) {
                            ItemStack slot = sp.getItemBySlot(es);
                            if (ItemStack.isSameItemSameTags(slot, p.target)) {
                                sp.setItemSlot(es, result);
                                break;
                            }
                        }
                    }
                }

                // Sound
                sp.level().playSound(null, p.pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, sp.level().random.nextFloat() * 0.1f + 0.9f);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
