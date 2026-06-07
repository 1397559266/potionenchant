package net.diexv.potionenchant.event;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.entity.BombEntity;
import net.diexv.potionenchant.entity.XBlockEntity;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.player.Inventory;

/**
 * X护甲功能事件处理器
 * 处理护甲功能的触发逻辑
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorXFeatureHandler {
    
    // 存储每个玩家的锁定目标UUID列表（最多5个）
    private static final java.util.Map<UUID, List<UUID>> LOCKED_TARGETS = new java.util.HashMap<>();
    
    // 存储每个玩家的XBlock实体ID
    private static final java.util.Map<UUID, Integer> PLAYER_XBLOCK_IDS = new java.util.HashMap<>();
    
    // 存储每个玩家的右键点击计数（用于防重复触发）
    private static final java.util.Map<UUID, Integer> RIGHT_CLICK_COUNTERS = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && isXArmorPiece(armor)) {
                CompoundTag tag = armor.getOrCreateTag();
                CompoundTag ft = tag.getCompound("ArmorFeatures");
                tag.put("ArmorFeatures", ft);
            }
        }
        removeAllXBlocksForPlayer(player);

    }
    
    /**
     * 监听玩家攻击事件，处理远程攻击功能
     */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        
        // 检查是否穿着全套X护甲
        if (!isWearingFullXArmor(player)) {
            return;
        }
        
        // 检查是否开启了远程攻击功能
        if (!isRangedAttackEnabled(player)) {
            return;
        }
        
        launchRangedAttack(player);
    }
    
    /**
     * 监听玩家右键事件，生成XBlock
     */
    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        
        // 检查是否穿着全套X护甲
        if (!isWearingFullXArmor(player)) {
            return;
        }
        
        // 检查是否开启了远程攻击功能
        if (!isRangedAttackEnabled(player)) {
            return;
        }
        
        // 只在服务端生成
        if (player.level().isClientSide) {
            return;
        }
        
        // 获取并更新右键点击计数
        UUID playerUUID = player.getUUID();
        int clickCount = RIGHT_CLICK_COUNTERS.getOrDefault(playerUUID, 0);
        clickCount++;
        RIGHT_CLICK_COUNTERS.put(playerUUID, clickCount);
        
        // 只在奇数次点击时生成XBlock（偶数次不生效）
        if (clickCount % 2 == 1) {
            spawnXBlock(player);
        }
    }
    
        @SubscribeEvent
    public static void onPlayerRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!isWearingFullXArmor(player)) return;
        if (!isRangedAttackEnabled(player)) return;
        if (player.level().isClientSide) return;

        Entity target = event.getTarget();
        if (target instanceof LivingEntity living && !(target instanceof Player)) {
            spawnTrackingXBlock(player, target.getId());
        }
    }

    /**
     * 在玩家准星指向的位置生成XBlock（无视距离，直接生成在方块表面）
     */
    @SuppressWarnings("removal")
    public static void spawnXBlock(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        
        // 获取玩家视线方向，使用BlockHitResult来精确获取方块位置
        // 使用1.0F作为tickDelta，确保与服务端tick同步
        HitResult hitResult = serverPlayer.pick(100.0, 1.0F, false);
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hitResult;
            
            try {
                // 先移除玩家之前生成的XBlock
                removePlayerXBlock(player);
                
                // 获取方块的坐标
                net.minecraft.core.BlockPos blockPos = blockHit.getBlockPos();
                
                // 在方块中心生成XBlock
                double x = blockPos.getX() + 0.5;
                double y = blockPos.getY() + 0.5;
                double z = blockPos.getZ() + 0.5;
                
                // 获取实体类型和Level，避免null引用
                var entityType = net.diexv.potionenchant.entity.ModEntities.XBLOCK.get();
                if (entityType == null) {
                    return;
                }
                
                Level level = player.level();
                if (level == null) {
                    return;
                }
                
                XBlockEntity xblock = new XBlockEntity(
                    entityType,
                    player,
                    level
                );
                
                xblock.setPos(x, y, z);
                player.level().addFreshEntity(xblock);
                
                // 记录这个XBlock的ID
                PLAYER_XBLOCK_IDS.put(player.getUUID(), xblock.getId());
            } catch (Exception e) {
                // 静默处理异常
            }
        } else if (hitResult.getType() == HitResult.Type.MISS) {
            // 如果没有指向任何方块，不提示（保持静默）
        }
    }
    
    /**
     * 在目标实体处生成追踪XBlock
     */
    @SuppressWarnings("removal")
    public static void spawnTrackingXBlock(Player player, int targetEntityId) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        
        // 查找目标实体
        net.minecraft.world.entity.Entity targetEntity = player.level().getEntity(targetEntityId);
        if (targetEntity == null || !(targetEntity instanceof LivingEntity livingTarget)) {
            return; // 无效目标，静默返回
        }
        
        // 先移除玩家之前生成的XBlock
        removePlayerXBlock(player);
        
        // 获取实体类型和Level，避免null引用
        var entityType = net.diexv.potionenchant.entity.ModEntities.XBLOCK.get();
        if (entityType == null) {
            return;
        }
        
        Level level = player.level();
        if (level == null) {
            return;
        }
        
        // 在目标实体位置生成XBlock
        XBlockEntity xblock = new XBlockEntity(
            entityType,
            player,
            level
        );
        
        // 设置初始位置为目标实体的位置
        xblock.setPos(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ());
        
        // 设置追踪目标
        xblock.setTarget(livingTarget);
        
        player.level().addFreshEntity(xblock);
        
        // 记录这个XBlock的ID
        PLAYER_XBLOCK_IDS.put(player.getUUID(), xblock.getId());
    }
    
    /**
     * 移除玩家之前生成的XBlock
     */
    @SuppressWarnings("removal")
    private static void removePlayerXBlock(Player player) {
        Integer xblockId = PLAYER_XBLOCK_IDS.get(player.getUUID());
        
        if (xblockId != null) {
            // 查找并移除旧的XBlock
            java.util.List<XBlockEntity> existingXBlocks =
                player.level().getEntitiesOfClass(
                    XBlockEntity.class,
                    player.getBoundingBox().inflate(100),
                    xblock -> xblock.getId() == xblockId
                );
            
            for (XBlockEntity xblock : existingXBlocks) {
                xblock.discard();
            }
            
            // 清除记录
            PLAYER_XBLOCK_IDS.remove(player.getUUID());
        }
    }

    /**
     * 公开方法：移除玩家的所有XBlock实体
     */
    public static void removeAllXBlocksForPlayer(Player player) {
        removePlayerXBlock(player);
    }

    
    /**
     * 发射远程攻击
     */
    public static void launchRangedAttack(Player player) {
        // 获取锁定的目标
        List<UUID> lockedTargets = LOCKED_TARGETS.getOrDefault(player.getUUID(), new ArrayList<>());
        
        if (lockedTargets.isEmpty()) {
            return; // 没有锁定目标，静默返回
        }
        
        // 获取玩家的环绕Bomb列表（只获取未攻击的）
        java.util.List<BombEntity> orbitingBombs =
            player.level().getEntitiesOfClass(
                BombEntity.class,
                player.getBoundingBox().inflate(10),
                bomb -> !bomb.isAttacking() // 只使用待命的Bomb
            );
        
        if (orbitingBombs.isEmpty()) {
            return; // 没有可用Bomb，静默返回
        }
        
        // 为每个锁定目标分配一个Bomb进行攻击
        int attackIndex = 0;
        int launchedCount = 0;
        
        for (UUID targetUUID : lockedTargets) {
            if (attackIndex >= orbitingBombs.size()) break;
            
            // 查找目标实体
            net.minecraft.world.entity.LivingEntity livingTarget = findEntityByUUID(player.level(), targetUUID);
            
            if (livingTarget != null && livingTarget.isAlive()) {
                // 让Bomb启动攻击
                BombEntity bomb = orbitingBombs.get(attackIndex);
                bomb.startAttack(livingTarget);
                launchedCount++;
                attackIndex++;
            }
        }
        
        if (launchedCount > 0) {
            // 立即重新生成5个新的环绕Bomb
            respawnBombs(player);
        }
        
        // 清除已发射的目标锁定
        LOCKED_TARGETS.remove(player.getUUID());
    }
    
    /**
     * 重新生成5个新的环绕Bomb
     */
    @SuppressWarnings("removal")
    private static void respawnBombs(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        
        // 先清除所有待命的Bomb（不包括正在攻击的）
        java.util.List<BombEntity> existingBombs =
            player.level().getEntitiesOfClass(
                BombEntity.class,
                player.getBoundingBox().inflate(10),
                bomb -> !bomb.isAttacking()
            );
        
        for (BombEntity bomb : existingBombs) {
            bomb.discard();
        }
        
        // 计算正在攻击的Bomb数量，只补充到最多5个
        java.util.List<BombEntity> attackingBombs =
            player.level().getEntitiesOfClass(
                BombEntity.class,
                player.getBoundingBox().inflate(10),
                bomb -> bomb.isAttacking()
            );
        int attackingCount = attackingBombs.size();
        int toSpawn = 5 - attackingCount;
        if (toSpawn < 0) toSpawn = 0;
        
        // 生成新的环绕Bomb（只补充到5个总数）
        for (int i = 0; i < toSpawn; i++) {
            int orbitIndex = i;
            
            BombEntity bomb = new BombEntity(
                net.diexv.potionenchant.entity.ModEntities.BOMB.get(),
                player,
                orbitIndex,
                player.level()
            );
            
            // 设置初始位置（玩家上方3.5格，半径3格）
            double angle = (orbitIndex / (double)toSpawn) * Math.PI * 2;
            bomb.setPos(
                player.getX() + Math.cos(angle) * 3.0,
                player.getY() + 3.5,
                player.getZ() + Math.sin(angle) * 3.0
            );
            
            player.level().addFreshEntity(bomb);
        }
    }
    
    /**
     * 清除环绕Bomb
     */
    @SuppressWarnings("removal")
    private static void clearOrbitingBombs(Player player) {
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
     * 根据UUID查找实体
     */
    private static net.minecraft.world.entity.LivingEntity findEntityByUUID(
        net.minecraft.world.level.Level level, UUID uuid) {
        
        // 先检查是否是玩家
        for (net.minecraft.world.entity.Entity entity : level.players()) {
            if (entity.getUUID().equals(uuid) && entity instanceof net.minecraft.world.entity.LivingEntity) {
                return (net.minecraft.world.entity.LivingEntity) entity;
            }
        }
        
        // 如果不是玩家，遍历附近所有活体生物
        java.util.List<net.minecraft.world.entity.LivingEntity> nearbyEntities = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                level.players().iterator().hasNext() ? 
                    level.players().iterator().next().getBoundingBox().inflate(100) :
                    new net.minecraft.world.phys.AABB(0, 0, 0, 0, 0, 0)
            );
        
        for (net.minecraft.world.entity.LivingEntity entity : nearbyEntities) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        
        return null;
    }
    
    /**
     * 玩家tick事件 - 在服务端更新锁定目标和发光效果
     */
    @SubscribeEvent
    public static void onPlayerTickForTargeting(TickEvent.PlayerTickEvent event) {
        // 只在服务端执行
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        
        Player player = event.player;
        
        // 每5tick更新一次（优化性能）
        if (player.tickCount % 5 == 0) {
            updateLockedTargets(player);
        }
        
        // 持续移除锁定目标的无敌帧（每tick执行）
        removeInvulnerableTimeForLockedTargets(player);
    }
    
    /**
     * 玩家登录事件 - 重进存档后重新生成Bomb并同步飞行状态
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        
        // 重置右键点击计数器
        RIGHT_CLICK_COUNTERS.remove(player.getUUID());
        
        // 检查是否穿着全套X护甲且开启了远程攻击功能
        if (isWearingFullXArmor(player) && isRangedAttackEnabled(player)) {
            // 延迟1tick生成Bomb（确保玩家完全加载）
            player.getServer().execute(() -> {
                spawnOrbitingBombsForPlayer(player);
            });
        }
        
        // 同步飞行状态到客户端
        syncFlightStateToClient(player);
    }
    
    /**
     * 玩家登出事件 - 清理数据
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        // 清理右键点击计数器
        RIGHT_CLICK_COUNTERS.remove(player.getUUID());
    }
    
    /**
     * 玩家死亡事件 - 清理数据
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            // 清理右键点击计数器
            RIGHT_CLICK_COUNTERS.remove(player.getUUID());
        }
    }
    
    /**
     * 为玩家生成环绕Bomb
     */
    @SuppressWarnings("removal")
    private static void spawnOrbitingBombsForPlayer(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        
        // 先清除现有的Bomb
        java.util.List<BombEntity> existingBombs =
            player.level().getEntitiesOfClass(
                BombEntity.class,
                player.getBoundingBox().inflate(10),
                bomb -> !bomb.isAttacking()
            );
        
        for (BombEntity bomb : existingBombs) {
            bomb.discard();
        }
        
        // 生成5个新的环绕Bomb
        for (int i = 0; i < 5; i++) {
            int orbitIndex = i;
            
            BombEntity bomb = new BombEntity(
                net.diexv.potionenchant.entity.ModEntities.BOMB.get(),
                player,
                orbitIndex,
                player.level()
            );
            
            // 设置初始位置（玩家上方3.5格，半径3格）
            double angle = (orbitIndex / 5.0) * Math.PI * 2;
            bomb.setPos(
                player.getX() + Math.cos(angle) * 3.0,
                player.getY() + 3.5,
                player.getZ() + Math.sin(angle) * 3.0
            );
            
            player.level().addFreshEntity(bomb);
        }
    }
    
    /**
     * 更新玩家的锁定目标（由服务端tick调用）
     */
    public static void updateLockedTargets(Player player) {
        if (!isWearingFullXArmor(player) || !isRangedAttackEnabled(player)) {
            // 清除之前的锁定目标和发光效果
            clearGlowingForPreviousTargets(player);
            LOCKED_TARGETS.remove(player.getUUID());
            return;
        }
        
        // 获取视野内的所有敌对生物
        List<LivingEntity> nearbyEntities = getNearbyHostileEntities(player);
        
        // 按距离排序，选择最近的5个
        List<LivingEntity> closestTargets = nearbyEntities.stream()
            .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
            .limit(5)
            .toList();
        
        // 存储锁定目标的UUID
        List<UUID> lockedUUIDs = new ArrayList<>();
        for (LivingEntity entity : closestTargets) {
            lockedUUIDs.add(entity.getUUID());
        }
        
        // 清除之前锁定目标的发光效果和无敌帧移除
        clearGlowingForPreviousTargets(player);
        
        // 为新锁定的目标设置发光效果并移除无敌帧（只在服务端执行）
        if (!player.level().isClientSide) {
            for (LivingEntity entity : closestTargets) {
                entity.setGlowingTag(true);
                // 移除无敌帧，使目标可以连续受到伤害
                entity.invulnerableTime = 0;
            }
        }
        
        LOCKED_TARGETS.put(player.getUUID(), lockedUUIDs);
    }
    
    /**
     * 清除之前锁定目标的发光效果
     */
    private static void clearGlowingForPreviousTargets(Player player) {
        List<UUID> previousTargets = LOCKED_TARGETS.getOrDefault(player.getUUID(), new ArrayList<>());
        
        for (UUID targetUUID : previousTargets) {
            LivingEntity target = findEntityByUUID(player.level(), targetUUID);
            if (target != null) {
                target.setGlowingTag(false);
            }
        }
    }
    
    /**
     * 持续移除锁定目标的无敌帧
     */
    private static void removeInvulnerableTimeForLockedTargets(Player player) {
        // 只在服务端执行
        if (player.level().isClientSide) {
            return;
        }
        
        // 获取当前锁定的目标
        List<UUID> lockedTargets = LOCKED_TARGETS.getOrDefault(player.getUUID(), new ArrayList<>());
        
        for (UUID targetUUID : lockedTargets) {
            LivingEntity target = findEntityByUUID(player.level(), targetUUID);
            if (target != null && target.isAlive()) {
                // 持续移除无敌帧，使目标可以连续受到伤害
                target.invulnerableTime = 0;
            }
        }
    }
    
    /**
     * 获取附近的敌对实体
     */
    private static List<LivingEntity> getNearbyHostileEntities(Player player) {
        double range = 50.0; // 检测范围
        AABB searchBox = player.getBoundingBox().inflate(range);
        
        return player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
            entity -> entity != player && 
                     !(entity instanceof net.minecraft.world.entity.player.Player) &&
                     entity.isAlive() &&
                     isHostileToPlayer(entity) // 检查是否是敌对生物
        );
    }
    
    /**
     * 检查实体是否对玩家敌对
     */
    private static boolean isHostileToPlayer(LivingEntity entity) {
        // 检查是否是怪物类型
        if (entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
            return true;
        }
        
        // 检查是否是Monster接口实现
        // XBlock 是玩家友方单位，不锁定为目标
        if (entity instanceof net.diexv.potionenchant.entity.XBlockEntity) {
            return false;
        }
        
        if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            return true;
        }
        
        // 其他情况视为中立/友好生物，不锁定
        return false;
    }
    
    /**
     * 获取玩家的锁定目标列表
     */
    public static List<UUID> getLockedTargets(Player player) {
        return LOCKED_TARGETS.getOrDefault(player.getUUID(), new ArrayList<>());
    }
    
    /**
     * 清除玩家的锁定目标
     */
    public static void clearLockedTargets(Player player) {
        LOCKED_TARGETS.remove(player.getUUID());
    }
    
    /**
     * 检查玩家是否穿着全套X护甲
     */
        /**
     * 获取X护甲升级等级对应的远程攻击伤害倍率
     */
    public static float getRangedDamageMultiplier(Player player) {
        if (player == null) return 1.0f;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || helmet.getItem() != ModItems.X_HELMET.get()) return 1.0f;
        CompoundTag tag = helmet.getTag();
        if (tag == null) return 1.0f;
        int level = tag.getInt("ArmorUpgradeLevel");
        // 每级增加10%伤害
        return 1.0f + level * 0.1f;
    }

    public static boolean isWearingFullXArmor(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        
        return !helmet.isEmpty() && helmet.getItem() == ModItems.X_HELMET.get() &&
               !chestplate.isEmpty() && chestplate.getItem() == ModItems.X_CHESTPLATE.get() &&
               !leggings.isEmpty() && leggings.getItem() == ModItems.X_LEGGINGS.get() &&
               !boots.isEmpty() && boots.getItem() == ModItems.X_BOOTS.get();
    }

    private static boolean isXArmorPiece(ItemStack stack) {
        return stack.getItem() == ModItems.X_HELMET.get()
            || stack.getItem() == ModItems.X_CHESTPLATE.get()
            || stack.getItem() == ModItems.X_LEGGINGS.get()
            || stack.getItem() == ModItems.X_BOOTS.get();
    }
    
    /**
     * 检查是否开启了远程攻击功能
     */
    public static boolean isRangedAttackEnabled(Player player) {
        // 从头盔读取功能状态（所有X护甲都应该有相同的状态）
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || helmet.getItem() != ModItems.X_HELMET.get()) {
            return false;
        }
        
        var tag = helmet.getTag();
        if (tag == null) {
            return false;
        }
        
        var featuresTag = tag.getCompound("ArmorFeatures");
        return featuresTag.getBoolean("ranged_attack");
    }

    /**
     * 检查是否开启了毁灭模式
     */
    public static boolean isDestructionModeEnabled(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || helmet.getItem() != ModItems.X_HELMET.get()) {
            return false;
        }
        
        var tag = helmet.getTag();
        if (tag == null) {
            return false;
        }
        
        var featuresTag = tag.getCompound("ArmorFeatures");
        return featuresTag.getBoolean("destruction_mode");
    }
    /**
     * 监听玩家受伤事件（自定义飞行模式下不需要处理）
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        // 自定义飞行系统不受受伤影响，无需处理
    }
    
    /**
     * 监听玩家落地事件（自定义飞行模式下不需要处理）
     */
    @SubscribeEvent
    public static void onPlayerFall(LivingFallEvent event) {
        // 自定义飞行系统不受落地影响，无需处理
    }
    /**
     * 监听玩家Tick事件，持续保持飞行能力
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        net.minecraft.world.entity.player.Player player = event.player;
        boolean isCreativeOrSpectator = player.isCreative() || player.isSpectator();
        
        // 检查是否穿着全套X护甲
        boolean wearingFull = isWearingFullXArmor(player);
        
        if (!wearingFull) {
            return;
        }
        
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        var tag = helmet.getTag();
        if (tag == null) {
            return;
        }
        
        var featuresTag = tag.getCompound("ArmorFeatures");
        
        // 飞行模式：仅在服务端处理能力（仅影响穿戴本护甲的玩家，创造/旁观跳过）
        if (!isCreativeOrSpectator && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            boolean flightModeEnabled = featuresTag.getBoolean("flight_mode");
            if (flightModeEnabled) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().flying = false;
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            } else {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().flying = false;
                    player.getAbilities().mayfly = false;
                    player.onUpdateAbilities();
                }
            }
        }
    }
    /**
     * 监听玩家穿戴装备事件，更新飞行能力
     */
    @SubscribeEvent
    public static void onPlayerEquip(net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        net.minecraft.world.entity.LivingEntity entity = event.getEntity();
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        
        // 创造模式和旁观者模式不受影响
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        
        // 只关注头部装备变化
        if (event.getSlot() != EquipmentSlot.HEAD) {
            return;
        }
        
        // 检查是否穿着全套X护甲
        boolean wearingFullXArmor = isWearingFullXArmor(player);
        
        if (!wearingFullXArmor) {
            // 脱下X护甲，关闭飞行
            if (player.getAbilities().mayfly) {
                player.getAbilities().flying = false;
                player.getAbilities().mayfly = false;
                player.onUpdateAbilities();
            }
            return;
        }
        
        // 穿着全套X护甲，检查是否开启了飞行模式
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        var tag = helmet.getTag();
        if (tag == null) {
            return;
        }
        var featuresTag = tag.getCompound("ArmorFeatures");
            if (featuresTag.getBoolean("flight_mode")) {
                // 开启飞行模式
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().flying = false;
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            } else {
                // 未开启飞行模式
                if (player.getAbilities().mayfly) {
                    player.getAbilities().flying = false;
                    player.getAbilities().mayfly = false;
                    player.onUpdateAbilities();
                }
            }
    }

    /**
     * 同步飞行状态到客户端
     */
    private static void syncFlightStateToClient(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        
        // 创造模式和旁观者模式不需要处理
        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
            return;
        }
        
        // 检查飞行模式
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || helmet.getItem() != ModItems.X_HELMET.get()) {
            return;
        }
        
        var tag = helmet.getTag();
        if (tag == null) {
            return;
        }
        var featuresTag = tag.getCompound("ArmorFeatures");
        boolean flightEnabled = featuresTag.getBoolean("flight_mode");
        
        player.getAbilities().mayfly = flightEnabled;
        player.getAbilities().flying = flightEnabled && player.getAbilities().flying;
        player.onUpdateAbilities();
    }

}

