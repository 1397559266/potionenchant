package net.diexv.potionenchant.network;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.function.Supplier;

public class EnchantBookPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocationHelper.fromNamespaceAndPath(PotionEnchantMod.MODID, "enchant_book"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
            packetId++,
            ApplyEnchantPacket.class,
            ApplyEnchantPacket::encode,
            ApplyEnchantPacket::decode,
            ApplyEnchantPacket::handle
        );
    }

    public static class ApplyEnchantPacket {
        private final String enchantId;
        private final int targetLevel;
        private final int slotIndex;

        public ApplyEnchantPacket(String enchantId, int targetLevel, int slotIndex) {
            this.enchantId = enchantId;
            this.targetLevel = targetLevel;
            this.slotIndex = slotIndex;
        }

        public static void encode(ApplyEnchantPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.enchantId);
            buf.writeInt(msg.targetLevel);
            buf.writeInt(msg.slotIndex);
        }

        public static ApplyEnchantPacket decode(FriendlyByteBuf buf) {
            return new ApplyEnchantPacket(buf.readUtf(), buf.readInt(), buf.readInt());
        }

        public static void handle(ApplyEnchantPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                ItemStack targetItem;
                boolean isXArmorMode = (msg.slotIndex == -2);
                if (msg.slotIndex == -1) {
                    targetItem = player.getOffhandItem();
                } else if (msg.slotIndex >= 0 && msg.slotIndex < player.getInventory().getContainerSize()) {
                    targetItem = player.getInventory().getItem(msg.slotIndex);
                } else if (isXArmorMode) {
                    targetItem = player.getOffhandItem(); // placeholder, handled below
                } else {
                    return;
                }

                if (!isXArmorMode && targetItem.isEmpty()) return;

                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(
                    ResourceLocationHelper.parse(msg.enchantId));
                if (enchantment == null) return;

                // X护甲模式：应用到所有兼容的护甲部位
                if (isXArmorMode) {
                    applyEnchantToXArmor(player, enchantment, msg.targetLevel);
                    return;
                }

                Map<Enchantment, Integer> existingEnchants = EnchantmentHelper.getEnchantments(targetItem);

                // 处理移除附魔（目标等级<=0）
                if (msg.targetLevel <= 0) {
                    existingEnchants.remove(enchantment);
                    EnchantmentHelper.setEnchantments(existingEnchants, targetItem);
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.removed",
                            enchantment.getFullname(1))
                            .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                    return;
                }

                int existingLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment, targetItem);
                int levelDiff = msg.targetLevel - existingLevel;

                // 降低等级（不消耗/返还经验）
                if (levelDiff < 0) {
                    existingEnchants.put(enchantment, msg.targetLevel);
                    EnchantmentHelper.setEnchantments(existingEnchants, targetItem);
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.reduced",
                            enchantment.getFullname(msg.targetLevel), msg.targetLevel)
                            .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                    return;
                }

                // 升级操作需要计算经验消耗
                int xpCost = levelDiff * PotionEnchantConfig.SERVER.enchantBookXpCost.get();

                // 检查经验是否足够
                if (player.totalExperience < xpCost && !player.isCreative()) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.no_xp")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
                    return;
                }

                // 检查附魔兼容性（超级附魔模式下跳过）
                boolean superMode = PotionEnchantConfig.SERVER.superEnchantMode.get();
                if (!superMode) {
                    for (Enchantment existing : existingEnchants.keySet()) {
                        if (existing != enchantment && !enchantment.isCompatibleWith(existing)) {
                            player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.incompatible")
                                    .withStyle(net.minecraft.ChatFormatting.RED), true);
                            return;
                        }
                    }
                }

                // 检查物品能否接受此附魔（超级附魔模式下跳过）
                if (!superMode && !enchantment.canEnchant(targetItem)) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.cannot_enchant")
                            .withStyle(net.minecraft.ChatFormatting.RED), true);
                    return;
                }

                // 应用附魔
                existingEnchants.put(enchantment, msg.targetLevel);
                EnchantmentHelper.setEnchantments(existingEnchants, targetItem);

                // 消耗经验（创造模式不消耗）
                if (!player.isCreative()) {
                    player.giveExperiencePoints(-xpCost);
                }

                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.applied",
                        enchantment.getFullname(msg.targetLevel), msg.targetLevel)
                        .withStyle(net.minecraft.ChatFormatting.GREEN), true);
            });
            context.setPacketHandled(true);
        }

        private static void applyEnchantToXArmor(ServerPlayer player, Enchantment enchantment, int targetLevel) {
            net.minecraft.world.entity.EquipmentSlot[] slots;
            net.minecraft.world.item.enchantment.EnchantmentCategory cat = enchantment.category;
            if (cat == net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_HEAD) {
                slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD};
            } else if (cat == net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_CHEST) {
                slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.CHEST};
            } else if (cat == net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_LEGS) {
                slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.LEGS};
            } else if (cat == net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_FEET) {
                slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.FEET};
            } else {
                slots = new net.minecraft.world.entity.EquipmentSlot[]{
                    net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
            }
            for (net.minecraft.world.entity.EquipmentSlot slot : slots) {
                ItemStack armor = player.getItemBySlot(slot);
                if (armor.isEmpty()) continue;
                if (armor.getItem() != net.diexv.potionenchant.item.ModItems.X_HELMET.get()
                    && armor.getItem() != net.diexv.potionenchant.item.ModItems.X_CHESTPLATE.get()
                    && armor.getItem() != net.diexv.potionenchant.item.ModItems.X_LEGGINGS.get()
                    && armor.getItem() != net.diexv.potionenchant.item.ModItems.X_BOOTS.get()) continue;
                boolean superModeArmor = PotionEnchantConfig.SERVER.superEnchantMode.get();
                if (!superModeArmor && !enchantment.canEnchant(armor)) continue;
                Map<Enchantment, Integer> existingEnchants = EnchantmentHelper.getEnchantments(armor);
                if (targetLevel <= 0) {
                    existingEnchants.remove(enchantment);
                } else {
                    existingEnchants.put(enchantment, targetLevel);
                }
                EnchantmentHelper.setEnchantments(existingEnchants, armor);
                    player.inventoryMenu.broadcastChanges();
            }
            if (targetLevel <= 0) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.removed",
                        enchantment.getFullname(1)).withStyle(net.minecraft.ChatFormatting.YELLOW), true);
            } else {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.potionenchant.enchant_book.applied",
                        enchantment.getFullname(targetLevel), targetLevel).withStyle(net.minecraft.ChatFormatting.GREEN), true);
            }
        }
    }
}