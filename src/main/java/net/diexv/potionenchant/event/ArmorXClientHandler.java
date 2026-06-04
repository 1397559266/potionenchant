package net.diexv.potionenchant.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.network.ArmorXPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * X护甲客户端功能处理器
 * 处理目标锁定和渲染
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
@OnlyIn(Dist.CLIENT)
public class ArmorXClientHandler {
    
    private static int tickCounter = 0;
    
    /**
     * 客户端tick事件 - 更新锁定目标
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        // 每5tick更新一次锁定目标（减少性能开销）
        tickCounter++;
        if (tickCounter >= 5) {
            tickCounter = 0;
            ArmorXFeatureHandler.updateLockedTargets(player);
        }
    }
    
    /**
     * 监听鼠标点击事件，发射远程攻击或生成XBlock
     */
    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        // 左键（button=0）- 远程攻击
        if (event.getButton() == 0) {
            java.util.List<java.util.UUID> lockedTargets = ArmorXFeatureHandler.getLockedTargets(player);
            if (!lockedTargets.isEmpty()) {
                // 发送数据包到服务端执行攻击
                ArmorXPacketHandler.INSTANCE.sendToServer(
                    new ArmorXPacketHandler.LaunchRangedAttackPacket()
                );
            }
        }
        // 右键（button=1）- 生成XBlock
        else if (event.getButton() == 1) {
            // 检测是否点击了实体
            net.minecraft.world.phys.HitResult hitResult = mc.hitResult;
            if (hitResult != null && hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                net.minecraft.world.phys.EntityHitResult entityHitResult = (net.minecraft.world.phys.EntityHitResult) hitResult;
                net.minecraft.world.entity.Entity targetEntity = entityHitResult.getEntity();
                
                if (targetEntity instanceof LivingEntity livingTarget) {
                    // 发送数据包到服务端生成追踪XBlock
                    ArmorXPacketHandler.INSTANCE.sendToServer(
                        new ArmorXPacketHandler.SpawnTrackingXBlockPacket(targetEntity.getId())
                    );
                    return;
                }
            }
            
            // 如果没有点击实体，生成普通XBlock
            ArmorXPacketHandler.INSTANCE.sendToServer(
                new ArmorXPacketHandler.SpawnXBlockPacket()
            );
        }
    }
    
    /**
     * 渲染世界事件 - 绘制锁定标记
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) {
            return;
        }
        
        // 获取锁定目标
        List<UUID> lockedTargets = ArmorXFeatureHandler.getLockedTargets(player);
        
        if (lockedTargets.isEmpty()) {
            return;
        }
        
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        
        // 遍历所有锁定目标，绘制标记
        for (UUID targetUUID : lockedTargets) {
            LivingEntity target = findEntityByUUID(mc.level, targetUUID);
            
            if (target != null && target.isAlive()) {
                renderLockMarker(poseStack, target, event.getPartialTick());
            }
        }
        
        poseStack.popPose();
    }
    
    /**
     * 根据UUID查找实体
     */
    private static LivingEntity findEntityByUUID(net.minecraft.world.level.Level level, UUID uuid) {
        // 遍历所有玩家
        for (net.minecraft.world.entity.Entity entity : level.players()) {
            if (entity.getUUID().equals(uuid) && entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }
        
        // 如果不是玩家，需要从所有实体中查找
        // Minecraft 1.20.1没有直接的UUID查找方法，需要遍历
        if (level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            for (net.minecraft.world.entity.Entity entity : clientLevel.entitiesForRendering()) {
                if (entity.getUUID().equals(uuid) && entity instanceof LivingEntity livingEntity) {
                    return livingEntity;
                }
            }
        }
        
        return null;
    }
    
    // 存储每个玩家的锁定目标（用于客户端渲染标记）
    // private static final java.util.Map<UUID, java.util.List<net.diexv.potionenchant.entity.OrbitingBomb>> ORBITING_BOMBS = new java.util.HashMap<>();
    
    /**
     * 渲染锁定标记（使用原版发光效果，红色）
     */
    private static void renderLockMarker(PoseStack poseStack, LivingEntity target, float partialTicks) {
        // 发光效果已经在updateLockedTargets中设置
        // 这里不需要做任何事情，Minecraft会自动渲染发光轮廓
    }
}
