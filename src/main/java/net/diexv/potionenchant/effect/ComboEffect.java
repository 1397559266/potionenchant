package net.diexv.potionenchant.effect;

import net.diexv.potionenchant.EffectRegistry;
import net.diexv.potionenchant.effect.PhaseLockEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber
public class ComboEffect extends MobEffect {

    // 存储玩家连招状态的映射
    private static final Map<UUID, ComboData> playerComboData = new HashMap<>();
    
    // 网络通道
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("potionenchant", "combo_attack"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public ComboEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x32CD32);
        
        // 注册网络消息
        int messageId = 0;
        NETWORK_CHANNEL.registerMessage(messageId++, ComboAttackPacket.class,
            ComboAttackPacket::encode,
            ComboAttackPacket::decode,
            ComboAttackPacket::handle
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;

            // 检查玩家是否有连招效果
            if (player.hasEffect(EffectRegistry.COMBO.get())) {
                handleComboAttack(player);
            } else {
                // 如果玩家没有连招效果，清除其连招数据
                playerComboData.remove(player.getUUID());
            }
        }
    }

    private static void handleComboAttack(Player player) {
        UUID playerId = player.getUUID();
        ComboData comboData = playerComboData.get(playerId);

        // 初始化连招数据
        if (comboData == null) {
            comboData = new ComboData();
            playerComboData.put(playerId, comboData);
        }

        // 在客户端检测鼠标左键状态并发送到服务端
        if (player.level().isClientSide()) {
            detectAndSendAttackState(player, comboData);
        }
        
        // 在服务端执行攻击逻辑
        if (!player.level().isClientSide()) {
            checkAndPerformAttack(player, comboData);
        }
    }

    // 在客户端检测攻击状态并发送到服务端
    private static void detectAndSendAttackState(Player player, ComboData comboData) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        boolean isAttacking = minecraft.options.keyAttack.isDown();
        
        // 只在状态改变时发送，减少网络流量
        if (isAttacking != comboData.wantsToAttack) {
            comboData.wantsToAttack = isAttacking;
            if (isAttacking) {
                comboData.attackTicks++;
            } else {
                comboData.attackTicks = 0;
            }
            
            // 发送网络包到服务端
            NETWORK_CHANNEL.sendTo(
                new ComboAttackPacket(isAttacking),
                ((net.minecraft.client.multiplayer.ClientPacketListener) minecraft.getConnection()).getConnection(),
                NetworkDirection.PLAY_TO_SERVER
            );
        }
    }
    
    // 在服务端检查并执行攻击
    private static void checkAndPerformAttack(Player player, ComboData comboData) {
        // 检查攻击冷却是否完成
        float attackStrength = player.getAttackStrengthScale(0.0F);
        if (attackStrength >= 1.0F && comboData.wantsToAttack) {
            performAutoAttack(player, comboData);
        }
    }

    private static void performAutoAttack(Player player, ComboData comboData) {
        // 获取玩家看向的实体
        net.minecraft.world.phys.EntityHitResult entityHit = getEntityLookingAt(player, 5.0D);

        if (entityHit != null && entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity target) {
            // 攻击目标
            player.attack(target);

            // 重置攻击冷却
            player.resetAttackStrengthTicker();

            // 增加连击计数
            comboData.comboCount++;

            // 播放攻击音效
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                    player.getSoundSource(), 1.0F, 1.0F);
        } else {
            // 如果没有瞄准任何实体，检查是否有相位锁定标记
            handlePhaseLockComboAttack(player, comboData);
        }
    }
    
    /**
     * 处理连招与相位锁定的联动：对标记实体造成伤害
     */
    private static void handlePhaseLockComboAttack(Player player, ComboData comboData) {
        // 检查玩家是否有相位锁定效果
        if (!player.hasEffect(EffectRegistry.PHASE_LOCK.get())) {
            return;
        }
        
        java.util.UUID markedTarget = PhaseLockEffect.getMarkedTarget(player.getUUID());
        if (markedTarget == null) {
            return;
        }
        
        // 查找被标记的实体
        net.minecraft.world.entity.LivingEntity markedEntity = PhaseLockEffect.findMarkedEntity(player, markedTarget);
        if (markedEntity != null) {
            // 使用相位锁定的伤害逻辑（基于最后记录的伤害）
            Float lastDamage = PhaseLockEffect.getLastMarkedDamage(player.getUUID());
            if (lastDamage != null && lastDamage > 0) {
                net.minecraft.world.effect.MobEffectInstance phaseLockEffect = player.getEffect(EffectRegistry.PHASE_LOCK.get());
                int amplifier = phaseLockEffect.getAmplifier();
                
                // 调用相位锁定的伤害方法
                PhaseLockEffect.dealPhaseLockDamageToEntity(markedEntity, player, amplifier, lastDamage);
                
                // 增加连击计数
                comboData.comboCount++;
                
                // 在目标实体位置播放吸取经验球音效
                player.level().playSound(null, markedEntity.getX(), markedEntity.getY(), markedEntity.getZ(),
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        markedEntity.getSoundSource(), 0.8F, 1.2F);
            }
        }
    }

    private static net.minecraft.world.phys.EntityHitResult getEntityLookingAt(Player player, double range) {
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
        net.minecraft.world.phys.Vec3 lookVec = player.getViewVector(1.0F);
        net.minecraft.world.phys.Vec3 endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox()
                .expandTowards(lookVec.scale(range))
                .inflate(1.0D, 1.0D, 1.0D);

        net.minecraft.world.phys.EntityHitResult entityHit = null;
        double closestDistance = range * range;

        for (net.minecraft.world.entity.Entity entity : player.level().getEntities(player, searchBox)) {
            if (entity.isPickable() && entity instanceof net.minecraft.world.entity.LivingEntity) {
                net.minecraft.world.phys.AABB entityBox = entity.getBoundingBox().inflate(0.3D);
                java.util.Optional<net.minecraft.world.phys.Vec3> clipResult = entityBox.clip(eyePos, endPos);

                if (clipResult.isPresent()) {
                    net.minecraft.world.phys.Vec3 hitLocation = clipResult.get();
                    double distance = eyePos.distanceToSqr(hitLocation);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        entityHit = new net.minecraft.world.phys.EntityHitResult(entity, hitLocation);
                    }
                }
            }
        }

        return entityHit;
    }

    // 连招数据内部类
    private static class ComboData {
        public boolean wantsToAttack = false;
        public int attackTicks = 0;
        public int comboCount = 0;

        public void reset() {
            wantsToAttack = false;
            attackTicks = 0;
            comboCount = 0;
        }
    }
    
    // 网络消息类
    public static class ComboAttackPacket {
        private final boolean isAttacking;
        
        public ComboAttackPacket(boolean isAttacking) {
            this.isAttacking = isAttacking;
        }
        
        public static void encode(ComboAttackPacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.isAttacking);
        }
        
        public static ComboAttackPacket decode(FriendlyByteBuf buf) {
            return new ComboAttackPacket(buf.readBoolean());
        }
        
        public static void handle(ComboAttackPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.hasEffect(EffectRegistry.COMBO.get())) {
                    UUID playerId = player.getUUID();
                    ComboData comboData = playerComboData.get(playerId);
                    if (comboData == null) {
                        comboData = new ComboData();
                        playerComboData.put(playerId, comboData);
                    }
                    comboData.wantsToAttack = msg.isAttacking;
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}