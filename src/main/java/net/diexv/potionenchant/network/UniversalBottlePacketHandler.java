package net.diexv.potionenchant.network;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.util.PotionEnchantManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 万能药水附魔瓶的网络数据包处理
 * 用于在客户端和服务器之间同步附魔操作
 */
public class UniversalBottlePacketHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocationHelper.fromNamespaceAndPath(PotionEnchantMod.MODID, "universal_bottle"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void register() {
        // 注册应用药水附魔的数据包
        INSTANCE.registerMessage(
            packetId++,
            ApplyPotionEnchantPacket.class,
            ApplyPotionEnchantPacket::encode,
            ApplyPotionEnchantPacket::decode,
            ApplyPotionEnchantPacket::handle
        );
    }
    
    /**
     * 应用药水附魔的数据包
     */
    public static class ApplyPotionEnchantPacket {
        private final CompoundTag enchantData;
        private final int slotIndex; // -1 for offhand, 0-35 for inventory
        
        public ApplyPotionEnchantPacket(CompoundTag enchantData, int slotIndex) {
            this.enchantData = enchantData;
            this.slotIndex = slotIndex;
        }
        
        public static void encode(ApplyPotionEnchantPacket msg, FriendlyByteBuf buf) {
            buf.writeNbt(msg.enchantData);
            buf.writeInt(msg.slotIndex);
        }
        
        public static ApplyPotionEnchantPacket decode(FriendlyByteBuf buf) {
            return new ApplyPotionEnchantPacket(buf.readNbt(), buf.readInt());
        }

        public static void handle(ApplyPotionEnchantPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    // 获取目标物品
                    ItemStack targetItem;
                    if (msg.slotIndex == -1) {
                        // 副手
                        targetItem = player.getOffhandItem();
                    } else if (msg.slotIndex >= 0 && msg.slotIndex < player.getInventory().getContainerSize()) {
                        // 背包
                        targetItem = player.getInventory().getItem(msg.slotIndex);
                    } else {
                        return;
                    }
                    
                    if (targetItem.isEmpty()) {
                        return;
                    }
                    
                    // 从NBT数据中读取附魔信息并应用
                    CompoundTag data = msg.enchantData;
                    if (data.contains("Effects")) {
                        ListTag effectsList = data.getList("Effects", 10);
                        for (int i = 0; i < effectsList.size(); i++) {
                            CompoundTag effectTag = effectsList.getCompound(i);
                            String effectName = effectTag.getString("Effect");
                            
                            // 获取药水效果
                            net.minecraft.world.effect.MobEffect effect = 
                                net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(
                                    ResourceLocationHelper.parse(effectName)
                                );
                            
                            if (effect != null) {
                                // 检查是否使用TargetLevel字段（新格式，支持直接设置目标等级）
                                if (effectTag.contains("TargetLevel")) {
                                    int targetLevel = effectTag.getInt("TargetLevel");
                                    int existingLevel = getExistingEnchantmentLevel(targetItem, effect);
                                    
                                    if (targetLevel > existingLevel) {
                                        // 升级：计算差值并添加
                                        int increaseAmount = targetLevel - existingLevel;
                                        int newAmplifier = existingLevel + increaseAmount - 1; // amplifier 从 0 开始
                                        PotionEnchantManager.addPotionEnchantment(
                                            targetItem, effect, newAmplifier, 6000
                                        );
                                    } else if (targetLevel < existingLevel && targetLevel >= 0) {
                                        // 降级：直接设置新等级
                                        if (targetLevel == 0) {
                                            // 移除附魔
                                            removePotionEnchantment(targetItem, effect);
                                        } else {
                                            // 设置新等级
                                            int newAmplifier = targetLevel - 1; // amplifier 从 0 开始
                                            setPotionEnchantmentLevel(targetItem, effect, newAmplifier);
                                        }
                                    }
                                    // targetLevel == existingLevel 的情况已经在客户端被跳过
                                } else if (effectTag.contains("Adjustment")) {
                                    // 旧格式：使用Adjustment字段（兼容）
                                    int adjustment = effectTag.getInt("Adjustment");
                                    
                                    if (adjustment > 0) {
                                        // 正数：升级或新增附魔
                                        // 获取已有等级
                                        int existingLevel = getExistingEnchantmentLevel(targetItem, effect);
                                        int newAmplifier = existingLevel + adjustment - 1; // amplifier 从 0 开始
                                        PotionEnchantManager.addPotionEnchantment(
                                            targetItem, effect, newAmplifier, 6000
                                        );
                                    } else if (adjustment < 0) {
                                        // 负数：降级已有附魔
                                        downgradePotionEnchantment(targetItem, effect, -adjustment);
                                    }
                                    // adjustment == 0 的情况已经在客户端被跳过
                                } else {
                                    // 更旧的格式：直接使用Amplifier
                                    int amplifier = effectTag.getInt("Amplifier");
                                    int duration = effectTag.getInt("Duration");
                                    PotionEnchantManager.addPotionEnchantment(
                                        targetItem, effect, amplifier, duration
                                    );
                                }
                            }
                        }
                    }
                    
                    // 消耗瓶子：从玩家背包所有格子中扣除
                    if (!player.isCreative()) {
                        int bottlesToConsume = data.getInt("BottlesConsumed");
                        if (bottlesToConsume > 0) {
                            for (int i = 0; i < player.getInventory().getContainerSize() && bottlesToConsume > 0; i++) {
                                ItemStack stack = player.getInventory().getItem(i);
                                if (stack.isEmpty()) continue;
                                if (stack.getItem() == net.diexv.potionenchant.item.ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
                                    int toTake = Math.min(stack.getCount(), bottlesToConsume);
                                    stack.shrink(toTake);
                                    bottlesToConsume -= toTake;
                                }
                            }
                        }
                    }
                    
                    // 发送消息给玩家
                    int appliedCount = data.getInt("AppliedCount");
                    int consumed = data.getInt("BottlesConsumed");
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "gui.potionenchant.batch_applied", appliedCount, consumed
                    ));
                }
            });
            context.setPacketHandled(true);
        }
        
        /**
         * 获取物品上已有的药水附魔等级
         * @return 已有的等级（amplifier + 1），如果没有则返回 0
         */
        private static int getExistingEnchantmentLevel(ItemStack item, net.minecraft.world.effect.MobEffect effect) {
            var enchantments = PotionEnchantManager.getPotionEnchantments(item);
            for (var enchant : enchantments) {
                if (enchant.getEffect() == effect) {
                    return enchant.getAmplifier() + 1; // 转换为等级（1-based）
                }
            }
            return 0;
        }
        
        /**
         * 降级药水附魔等级
         * @param item 目标物品
         * @param effect 要降级的药水效果
         * @param decreaseAmount 降级幅度（正数）
         */
        private static void downgradePotionEnchantment(ItemStack item, 
                                                       net.minecraft.world.effect.MobEffect effect,
                                                       int decreaseAmount) {
            if (item == null || effect == null) return;
            
            // 获取物品的NBT数据
            CompoundTag tag = item.getTag();
            if (tag == null) return;
            
            // 获取药水附魔列表
            ListTag enchantments = tag.getList("PotionEnchantments", 10);
            if (enchantments.isEmpty()) return;
            
            // 获取效果的注册表键字符串（与NBT中存储的格式一致）
            String targetEffectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();
            
            // 查找并修改对应的附魔
            for (int i = 0; i < enchantments.size(); i++) {
                CompoundTag enchantTag = enchantments.getCompound(i);
                String enchantEffectId = enchantTag.getString("Effect");
                
                if (enchantEffectId.equals(targetEffectId)) {
                    int currentAmplifier = enchantTag.getInt("Amplifier");
                    int newAmplifier = currentAmplifier - decreaseAmount;
                    
                    if (newAmplifier <= 0) {
                        // 等级降至0或以下，移除该附魔
                        enchantments.remove(i);
                        tag.put("PotionEnchantments", enchantments);
                    } else {
                        // 降低等级
                        enchantTag.putInt("Amplifier", newAmplifier);
                        enchantments.set(i, enchantTag);
                        tag.put("PotionEnchantments", enchantments);
                    }
                    break;
                }
            }
        }
        
        /**
         * 移除药水附魔
         * @param item 目标物品
         * @param effect 要移除的药水效果
         */
        private static void removePotionEnchantment(ItemStack item, 
                                                    net.minecraft.world.effect.MobEffect effect) {
            if (item == null || effect == null) return;
            
            // 获取物品的NBT数据
            CompoundTag tag = item.getTag();
            if (tag == null) return;
            
            // 获取药水附魔列表
            ListTag enchantments = tag.getList("PotionEnchantments", 10);
            if (enchantments.isEmpty()) return;
            
            // 获取效果的注册表键字符串（与NBT中存储的格式一致）
            String targetEffectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();
            
            // 查找并移除对应的附魔
            for (int i = 0; i < enchantments.size(); i++) {
                CompoundTag enchantTag = enchantments.getCompound(i);
                String enchantEffectId = enchantTag.getString("Effect");
                
                if (enchantEffectId.equals(targetEffectId)) {
                    enchantments.remove(i);
                    tag.put("PotionEnchantments", enchantments);
                    break;
                }
            }
        }
        
        /**
         * 设置药水附魔等级
         * @param item 目标物品
         * @param effect 药水效果
         * @param newAmplifier 新的amplifier值（从0开始）
         */
        private static void setPotionEnchantmentLevel(ItemStack item,
                                                      net.minecraft.world.effect.MobEffect effect,
                                                      int newAmplifier) {
            if (item == null || effect == null) return;
            
            // 获取物品的NBT数据
            CompoundTag tag = item.getTag();
            if (tag == null) return;
            
            // 获取药水附魔列表
            ListTag enchantments = tag.getList("PotionEnchantments", 10);
            if (enchantments.isEmpty()) return;
            
            // 获取效果的注册表键字符串（与NBT中存储的格式一致）
            String targetEffectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();
            
            // 查找并修改对应的附魔
            for (int i = 0; i < enchantments.size(); i++) {
                CompoundTag enchantTag = enchantments.getCompound(i);
                String enchantEffectId = enchantTag.getString("Effect");
                
                if (enchantEffectId.equals(targetEffectId)) {
                    enchantTag.putInt("Amplifier", newAmplifier);
                    enchantments.set(i, enchantTag);
                    tag.put("PotionEnchantments", enchantments);
                    break;
                }
            }
        }
    }
}