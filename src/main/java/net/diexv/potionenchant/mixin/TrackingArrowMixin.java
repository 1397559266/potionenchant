package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.PotionEnchantMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class TrackingArrowMixin {

    @Unique
    private LivingEntity trackingTarget = null;

    @Unique
    private boolean hasTracking = false;

    @Unique
    private int trackingCooldown = 0;

    // 在箭矢被射出时检查是否有追踪附魔
    @Inject(method = "shoot", at = @At("HEAD"))
    public void onShoot(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;

        // 只在服务端处理
        if (arrow.level().isClientSide) {
            return;
        }

        // 检查箭矢的发射者
        if (arrow.getOwner() instanceof LivingEntity owner) {
            // 检查发射者手中的弓或弩是否有追踪附魔
            ItemStack weapon = owner.getMainHandItem();
            int trackingLevel = weapon.getEnchantmentLevel(PotionEnchantMod.TRACKING_ARROW.get());

            if (trackingLevel > 0) {
                hasTracking = true;
                // 设置初始追踪冷却
                trackingCooldown = 5;
            }
        }
    }

    // 每tick更新箭矢的追踪行为
    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;

        // 只在服务端处理追踪逻辑
        if (arrow.level().isClientSide || !hasTracking) {
            return;
        }

        // 箭矢存在时间超过一定ticks后开始追踪
        if (arrow.tickCount < 1) {
            return;
        }

        // 使用公共方法检查箭矢是否应该停止追踪
        if (shouldStopTracking(arrow)) {
            return;
        }

        // 更新追踪冷却
        if (trackingCooldown > 0) {
            trackingCooldown--;
            return;
        }

        // 如果还没有目标，寻找目标
        if (trackingTarget == null || !trackingTarget.isAlive() || trackingTarget.isRemoved()) {
            findTrackingTarget(arrow);
        }

        // 如果有目标，进行追踪
        if (trackingTarget != null && trackingTarget.isAlive() && !trackingTarget.isRemoved()) {
            trackTarget(arrow);
            trackingCooldown = 3; // 重置冷却
        }
    }

    @Unique
    private boolean shouldStopTracking(AbstractArrow arrow) {
        // 使用公共方法检查箭矢状态
        // 检查箭矢是否已经击中实体
        if (arrow.getPierceLevel() > 0) {
            return true;
        }

        // 检查箭矢的移动速度是否接近零（可能已击中物体）
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.length() < 0.1) {
            return true;
        }

        // 检查箭矢是否已经存在很长时间（防止无限追踪）
        if (arrow.tickCount > 200) { // 10秒后停止追踪
            return true;
        }

        return false;
    }

    @Unique
    private void findTrackingTarget(AbstractArrow arrow) {
        // 获取箭矢位置
        Vec3 arrowPos = arrow.position();

        // 搜索半径
        double searchRadius = 100.0D;

        // 寻找最近的生物目标
        LivingEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        // 使用更宽松的搜索条件
        for (LivingEntity entity : arrow.level().getEntitiesOfClass(LivingEntity.class,
                arrow.getBoundingBox().inflate(searchRadius))) {

            // 排除箭矢的发射者和已死亡的目标
            if (entity == arrow.getOwner() || !entity.isAlive() || entity.isSpectator() || entity.isRemoved()) {
                continue;
            }

            // 计算距离
            double distance = arrowPos.distanceToSqr(entity.position());

            // 检查是否在视线范围内（更宽松的条件）
            Vec3 toTarget = entity.getEyePosition().subtract(arrowPos).normalize();
            Vec3 arrowDirection = arrow.getDeltaMovement().normalize();
            double dot = arrowDirection.dot(toTarget);

            // 放宽追踪条件：只要目标在箭矢前方半球形区域内
            if (dot > 0.5 && distance < closestDistance) { // 进一步放宽到0.5
                closestDistance = distance;
                closestTarget = entity;
            }
        }

        trackingTarget = closestTarget;
    }

    @Unique
    private void trackTarget(AbstractArrow arrow) {
        // 计算指向目标的向量
        Vec3 targetPos = trackingTarget.getBoundingBox().getCenter();
        Vec3 arrowPos = arrow.position();
        Vec3 toTarget = targetPos.subtract(arrowPos);

        // 如果目标太远，放弃追踪（防止无限追踪）
        if (toTarget.length() > 300.0D) {
            trackingTarget = null;
            return;
        }

        // 计算当前箭矢方向
        Vec3 currentMotion = arrow.getDeltaMovement();
        double currentSpeed = currentMotion.length();

        // 如果箭矢几乎停止，放弃追踪
        if (currentSpeed < 0.1) {
            return;
        }

        // 计算理想方向（指向目标）
        Vec3 idealDirection = toTarget.normalize();

        // 计算当前方向
        Vec3 currentDirection = currentMotion.normalize();

        // 计算两个方向之间的角度
        double dot = currentDirection.dot(idealDirection);

        // 如果角度很小，说明已经很接近目标方向，不需要调整
        if (dot > 0.995) {
            return;
        }

        // 计算转向角度（更平滑的转向）
        double maxTurnAngle = Math.PI / 8; // 增加到22.5度，提高追踪能力

        // 使用球面线性插值平滑转向
        double theta = Math.acos(Math.max(-1, Math.min(1, dot))); // 限制dot在[-1,1]范围内

        // 计算插值因子
        double t = Math.min(maxTurnAngle / theta, 1.0);

        // 计算新的方向
        Vec3 newDirection;
        if (theta < 0.0001) {
            // 如果角度非常小，直接使用目标方向
            newDirection = idealDirection;
        } else {
            // 使用球面线性插值
            double sinTheta = Math.sin(theta);
            double w1 = Math.sin((1 - t) * theta) / sinTheta;
            double w2 = Math.sin(t * theta) / sinTheta;
            newDirection = currentDirection.scale(w1).add(idealDirection.scale(w2)).normalize();
        }

        // 应用新的方向，保持原有速度
        arrow.setDeltaMovement(newDirection.scale(currentSpeed));

        // 更新箭矢的旋转以匹配新的方向
        arrow.setYRot((float)(Math.atan2(newDirection.x, newDirection.z) * (180 / Math.PI)));
        arrow.setXRot((float)(Math.asin(newDirection.y) * (180 / Math.PI)));
        arrow.yRotO = arrow.getYRot();
        arrow.xRotO = arrow.getXRot();

        // 播放追踪粒子效果
        if (arrow.tickCount % 3 == 0) {
            spawnTrackingParticles(arrow);
        }
    }

    @Unique
    private void spawnTrackingParticles(AbstractArrow arrow) {
        // 在服务端生成粒子效果
        if (!arrow.level().isClientSide) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) arrow.level();

            for (int i = 0; i < 2; i++) {
                double offsetX = (arrow.level().random.nextDouble() - 0.5) * 0.3;
                double offsetY = arrow.level().random.nextDouble() * 0.3;
                double offsetZ = (arrow.level().random.nextDouble() - 0.5) * 0.3;

                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        arrow.getX() + offsetX,
                        arrow.getY() + offsetY,
                        arrow.getZ() + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }
    }
}
