package net.diexv.potionenchant.network;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.entity.BombEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.diexv.potionenchant.util.helper.ResourceLocationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * X护甲控制网络数据包处理器
 */
public class ArmorXPacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocationHelper.fromNamespaceAndPath(PotionEnchantMod.MODID, "armorx_control"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    public static void register() {
        int packetId = 0;
        
        // 注册应用药水效果的数据包（客户端 -> 服务器）
        INSTANCE.registerMessage(
            packetId++,
            ApplyArmorXEffectPacket.class,
            ApplyArmorXEffectPacket::encode,
            ApplyArmorXEffectPacket::decode,
            ApplyArmorXEffectPacket::handle
        );
        
        // 注册切换护甲功能的数据包（客户端 -> 服务器）
        INSTANCE.registerMessage(
            packetId++,
            ToggleArmorFeaturePacket.class,
            ToggleArmorFeaturePacket::encode,
            ToggleArmorFeaturePacket::decode,
            ToggleArmorFeaturePacket::handle
        );
        
        // 注册发射远程攻击的数据包（客户端 -> 服务器）
        INSTANCE.registerMessage(
            packetId++,
            LaunchRangedAttackPacket.class,
            LaunchRangedAttackPacket::encode,
            LaunchRangedAttackPacket::decode,
            LaunchRangedAttackPacket::handle
        );
        
        // 注册生成XBlock的数据包（客户端 -> 服务器）
        INSTANCE.registerMessage(
            packetId++,
            SpawnXBlockPacket.class,
            SpawnXBlockPacket::encode,
            SpawnXBlockPacket::decode,
            SpawnXBlockPacket::handle
        );
        
                // 注册X护甲升级数据包（客户端 -> 服务器）
        INSTANCE.registerMessage(
            packetId++,
            UpgradeArmorPacket.class,
            UpgradeArmorPacket::encode,
            UpgradeArmorPacket::decode,
            UpgradeArmorPacket::handle
        );
        
        // 注册生成追踪XBlock的数据包（客户端 -> 服务器）
        INSTANCE.registerMessage(
            packetId++,
            SpawnTrackingXBlockPacket.class,
            SpawnTrackingXBlockPacket::encode,
            SpawnTrackingXBlockPacket::decode,
            SpawnTrackingXBlockPacket::handle
        );
    }
    
    /**
     * 应用X护甲药水效果的数据包
     */
    public static class ApplyArmorXEffectPacket {
        private final CompoundTag enchantData;
        
        public ApplyArmorXEffectPacket(CompoundTag enchantData) {
            this.enchantData = enchantData;
        }
        
        public static void encode(ApplyArmorXEffectPacket packet, FriendlyByteBuf buf) {
            buf.writeNbt(packet.enchantData);
        }
        
        public static ApplyArmorXEffectPacket decode(FriendlyByteBuf buf) {
            return new ApplyArmorXEffectPacket(buf.readNbt());
        }
        
        public static void handle(ApplyArmorXEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    applyEffects(player, packet.enchantData);
                }
            });
            context.setPacketHandled(true);
        }

        private static void applyEffects(ServerPlayer player, CompoundTag enchantData) {
            ListTag effectsList = enchantData.getList("Effects", 10);
            
            // 检查是否穿着全套X护甲
            if (!net.diexv.potionenchant.event.ArmorXFeatureHandler.isWearingFullXArmor(player)) {
                return;
            }
            
            // 收集所有要设置的附魔效果和目标等级
            java.util.Map<net.minecraft.world.effect.MobEffect, Integer> enchantmentsToSet = new java.util.HashMap<>();
            
            // 处理每个效果的调整
            for (int i = 0; i < effectsList.size(); i++) {
                CompoundTag effectTag = effectsList.getCompound(i);
                String effectId = effectTag.getString("Effect");
                int targetLevel = effectTag.getInt("Adjustment"); // 这里存储的是目标等级
                
                // 获取药水效果
                net.minecraft.world.effect.MobEffect effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocationHelper.parse(effectId));
                if (effect != null) {
                    // targetLevel >= 0 都有效（0表示移除）
                    enchantmentsToSet.put(effect, targetLevel);
                }
            }
            
            // 使用X护甲独立附魔系统批量设置
            net.diexv.potionenchant.util.XArmorEnchantmentManager.setXArmorEnchantments(player, enchantmentsToSet);
            player.inventoryMenu.broadcastChanges();
            
            // 发送成功消息给玩家
            int appliedCount = enchantData.getInt("AppliedCount");
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "gui.potionenchant.armorx.applied", appliedCount
            ));
        }
    }
    
    /**
     * 切换护甲功能的数据包
     */
    public static class ToggleArmorFeaturePacket {
        private final String featureKey;
        private final boolean enabled;
        
        public ToggleArmorFeaturePacket(String featureKey, boolean enabled) {
            this.featureKey = featureKey;
            this.enabled = enabled;
        }
        
        public static void encode(ToggleArmorFeaturePacket packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.featureKey);
            buf.writeBoolean(packet.enabled);
        }
        
        public static ToggleArmorFeaturePacket decode(FriendlyByteBuf buf) {
            return new ToggleArmorFeaturePacket(buf.readUtf(), buf.readBoolean());
        }
        
        public static void handle(ToggleArmorFeaturePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    toggleFeature(player, packet.featureKey, packet.enabled);
                }
            });
            context.setPacketHandled(true);
        }
        
        private static void toggleFeature(ServerPlayer player, String featureKey, boolean enabled) {
            // 获取所有X护甲
            net.minecraft.world.item.ItemStack[] armors = new net.minecraft.world.item.ItemStack[4];
            armors[0] = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            armors[1] = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            armors[2] = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
            armors[3] = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
            
            // 在所有X护甲上设置功能状态
            for (net.minecraft.world.item.ItemStack armor : armors) {
                if (!armor.isEmpty() && isXArmor(armor)) {
                    CompoundTag tag = armor.getOrCreateTag();
                    CompoundTag featuresTag = tag.getCompound("ArmorFeatures");
                    featuresTag.putBoolean(featureKey, enabled);
                    tag.put("ArmorFeatures", featuresTag);
            player.inventoryMenu.broadcastChanges();
                }
            }
            
            // 如果是远程攻击功能，生成或清除环绕Bomb
            if ("ranged_attack".equals(featureKey)) {
                if (enabled) {
                    // 开启功能，生成环绕Bomb
                    spawnOrbitingBombs(player);
                } else {
                    // 关闭功能，清除环绕Bomb和XBlock
                    clearOrbitingBombs(player);
                    net.diexv.potionenchant.event.ArmorXFeatureHandler.removeAllXBlocksForPlayer(player);
                }
            }
            
            // 如果是飞行模式，更新玩家飞行能力
            if ("flight_mode".equals(featureKey)) {
                updatePlayerFlightAbility(player, enabled);
            }
        }
        
        private static boolean isXArmor(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() == net.diexv.potionenchant.item.ModItems.X_HELMET.get() ||
                   stack.getItem() == net.diexv.potionenchant.item.ModItems.X_CHESTPLATE.get() ||
                   stack.getItem() == net.diexv.potionenchant.item.ModItems.X_LEGGINGS.get() ||
                   stack.getItem() == net.diexv.potionenchant.item.ModItems.X_BOOTS.get();
        }
        
        /**
         * 生成环绕Bomb
         */
        private static void spawnOrbitingBombs(ServerPlayer player) {
            // 清除旧的Bomb
            clearOrbitingBombs(player);
            
            // 生成5个新的环绕Bomb
            for (int i = 0; i < 5; i++) {
                BombEntity bomb = new BombEntity(
                    net.diexv.potionenchant.entity.ModEntities.BOMB.get(),
                    player,
                    i,
                    player.level()
                );
                
                // 设置初始位置（玩家上方3.5格，半径3格）
                bomb.setPos(
                    player.getX() + Math.cos(i * 72 * Math.PI / 180) * 3.0,
                    player.getY() + 3.5,
                    player.getZ() + Math.sin(i * 72 * Math.PI / 180) * 3.0
                );
                
                player.level().addFreshEntity(bomb);
            }
        }
        
        /**
         * 清除环绕Bomb
         */
        private static void clearOrbitingBombs(ServerPlayer player) {
            // 查找并清除玩家附近的OrbitingBomb
            java.util.List<BombEntity> orbitingBombs =
                player.level().getEntitiesOfClass(
                    BombEntity.class,
                    player.getBoundingBox().inflate(10)
                );
            
            for (BombEntity bomb : orbitingBombs) {
                if (!bomb.isRemoved()) {
                    bomb.discard();
                }
            }
        }
        
        /**
         * 更新玩家飞行能力
         */
        private static void updatePlayerFlightAbility(ServerPlayer player, boolean enableFlight) {
            // 创造模式和旁观者模式不受影响
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            
            if (enableFlight) {
                // 检查是否穿着全套X护甲且开启了飞行模式
                if (net.diexv.potionenchant.event.ArmorXFeatureHandler.isWearingFullXArmor(player)) {
                    // 检查是否开启了飞行模式
                    net.minecraft.world.item.ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
                    if (!helmet.isEmpty()) {
                        var tag = helmet.getTag();
                        if (tag != null) {
                            var featuresTag = tag.getCompound("ArmorFeatures");
                            if (featuresTag.getBoolean("flight_mode")) {
                                player.getAbilities().flying = false; // 重置飞行状态
                                player.getAbilities().mayfly = true; // 允许飞行
                                player.onUpdateAbilities(); // 同步到客户端
                            }
                        }
                    }
                }
            } else {
                // 关闭飞行模式
                player.getAbilities().flying = false; // 立即停止飞行
                player.getAbilities().mayfly = false; // 不允许飞行
                player.onUpdateAbilities(); // 同步到客户端
            }
        }
    }
    
    /**
     * 发射远程攻击的数据包
     */
    
    /**
     * X护甲升级数据包（消耗药水瓶提升远程攻击增伤等级）
     */
    public static class UpgradeArmorPacket {
        private final int bottlesToConsume;
        
        public UpgradeArmorPacket(int bottlesToConsume) {
            this.bottlesToConsume = bottlesToConsume;
        }
        
        public static void encode(UpgradeArmorPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.bottlesToConsume);
        }
        
        public static UpgradeArmorPacket decode(FriendlyByteBuf buf) {
            return new UpgradeArmorPacket(buf.readInt());
        }
        
        public static void handle(UpgradeArmorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    upgradeArmor(player, packet.bottlesToConsume);
                }
            });
            context.setPacketHandled(true);
        }
        
        private static void upgradeArmor(ServerPlayer player, int bottlesToConsume) {
            if (bottlesToConsume <= 0) return;
            // 统计背包中的药水瓶
            int totalBottles = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.getItem() == net.diexv.potionenchant.item.ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
                    totalBottles += stack.getCount();
                }
            }
            int actual = Math.min(bottlesToConsume, totalBottles);
            if (actual <= 0) return;
            // 消耗药水瓶
            int remaining = actual;
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.getItem() == net.diexv.potionenchant.item.ModItems.UNIVERSAL_POTION_BOTTLE.get()) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
            // 增加X护甲升级等级（存储在头盔NBT中）
            var helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (helmet.getItem() == net.diexv.potionenchant.item.ModItems.X_HELMET.get()) {
                var tag = helmet.getOrCreateTag();
                int currentLevel = tag.getInt("ArmorUpgradeLevel");
                tag.putInt("ArmorUpgradeLevel", currentLevel + actual);
            player.inventoryMenu.broadcastChanges();
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "gui.potionenchant.armorx_upgraded", actual));
        }
    }
public static class LaunchRangedAttackPacket {
        
        public LaunchRangedAttackPacket() {
        }
        
        public static void encode(LaunchRangedAttackPacket packet, FriendlyByteBuf buf) {
        }
        
        public static LaunchRangedAttackPacket decode(FriendlyByteBuf buf) {
            return new LaunchRangedAttackPacket();
        }
        
        public static void handle(LaunchRangedAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    launchRangedAttack(player);
                }
            });
            context.setPacketHandled(true);
        }
        
        private static void launchRangedAttack(ServerPlayer player) {
            // 调用ArmorXFeatureHandler的发射逻辑
            net.diexv.potionenchant.event.ArmorXFeatureHandler.launchRangedAttack(player);
        }
    }
    
    /**
     * 生成XBlock的数据包
     */
    public static class SpawnXBlockPacket {
        
        public SpawnXBlockPacket() {
        }
        
        public static void encode(SpawnXBlockPacket packet, FriendlyByteBuf buf) {
        }
        
        public static SpawnXBlockPacket decode(FriendlyByteBuf buf) {
            return new SpawnXBlockPacket();
        }
        
        public static void handle(SpawnXBlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    spawnXBlock(player);
                }
            });
            context.setPacketHandled(true);
        }
        
        private static void spawnXBlock(ServerPlayer player) {
            // 检查是否穿着全套X护甲
            if (!net.diexv.potionenchant.event.ArmorXFeatureHandler.isWearingFullXArmor(player)) {
                return;
            }
            
            // 检查是否开启了远程攻击功能
            if (!net.diexv.potionenchant.event.ArmorXFeatureHandler.isRangedAttackEnabled(player)) {
                return;
            }
            
            // 调用ArmorXFeatureHandler的生成逻辑
            net.diexv.potionenchant.event.ArmorXFeatureHandler.spawnXBlock(player);
        }
    }
    
    /**
     * 生成追踪XBlock的数据包
     */
    public static class SpawnTrackingXBlockPacket {
        private final int targetEntityId;
        
        public SpawnTrackingXBlockPacket(int targetEntityId) {
            this.targetEntityId = targetEntityId;
        }
        
        public static void encode(SpawnTrackingXBlockPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.targetEntityId);
        }
        
        public static SpawnTrackingXBlockPacket decode(FriendlyByteBuf buf) {
            return new SpawnTrackingXBlockPacket(buf.readInt());
        }
        
        public static void handle(SpawnTrackingXBlockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    spawnTrackingXBlock(player, packet.targetEntityId);
                }
            });
            context.setPacketHandled(true);
        }
        
        private static void spawnTrackingXBlock(ServerPlayer player, int targetEntityId) {
            // 检查权限
            if (!net.diexv.potionenchant.event.ArmorXFeatureHandler.isWearingFullXArmor(player)) {
                return;
            }
            
            if (!net.diexv.potionenchant.event.ArmorXFeatureHandler.isRangedAttackEnabled(player)) {
                return;
            }
            
            // 调用ArmorXFeatureHandler的生成逻辑
            net.diexv.potionenchant.event.ArmorXFeatureHandler.spawnTrackingXBlock(player, targetEntityId);
        }
    }

}