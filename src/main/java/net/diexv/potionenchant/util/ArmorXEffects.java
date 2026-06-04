package net.diexv.potionenchant.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * X护甲特效工具类
 */
public class ArmorXEffects {
    
    /**
     * 在实体头顶生成锁定标记粒子效果
     */
    public static void spawnLockMarker(Level level, LivingEntity target) {
        if (level.isClientSide) {
            Vec3 pos = target.position();
            double x = pos.x;
            double y = pos.y + target.getBbHeight() + 0.5;
            double z = pos.z;
            
            // 生成红色粒子光环
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2;
                double radius = 0.5;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                
                level.addParticle(ParticleTypes.CRIT, px, y, pz, 0, 0, 0);
            }
        }
    }
    
    /**
     * 在玩家周围生成环绕粒子效果
     */
    public static void spawnOrbitParticles(Level level, net.minecraft.world.entity.player.Player player, float partialTicks) {
        if (!level.isClientSide) {
            return;
        }
        
        Vec3 playerPos = player.position();
        double centerX = playerPos.x;
        double centerY = playerPos.y + 2.0; // 玩家上方2格
        double centerZ = playerPos.z;
        
        // 生成5个环绕点
        for (int i = 0; i < 5; i++) {
            double angle = (i / 5.0) * Math.PI * 2 + (player.tickCount + partialTicks) * 0.05;
            double radius = 2.0;
            
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin((player.tickCount + partialTicks) * 0.1) * 0.5; // 上下浮动
            double z = centerZ + Math.sin(angle) * radius;
            
            // 生成蓝色粒子
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0, 0);
        }
    }
}
