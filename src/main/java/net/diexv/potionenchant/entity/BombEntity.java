package net.diexv.potionenchant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.diexv.potionenchant.event.ArmorXFeatureHandler;
import net.diexv.potionenchant.util.XSwordTargetTracker;
import net.diexv.potionenchant.mixin.accessor.LivingEntityAccessor;
import net.minecraft.world.entity.ExperienceOrb;

import java.util.List;

public class BombEntity extends AbstractArrow implements PowerableMob {

    private static final EntityDataAccessor<Integer> DATA_ORBIT_INDEX = SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_ATTACKING = SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(BombEntity.class, EntityDataSerializers.INT);

    private LivingEntity attackTarget;
    private float swelling;
    private int oldSwelling;
    private float swellAmount;
    private float oSwellAmount;
    private double traveledDistance = 0;
    private boolean firedByXBlock = false; // XBlock发射的标记
    private Vec3 lastPos = null;

    public BombEntity(PlayMessages.SpawnEntity packet, Level world) {
        super(ModEntities.BOMB.get(), world);
    }

    public BombEntity(EntityType<? extends BombEntity> type, Level world) {
        super(type, world);
    }

    public BombEntity(EntityType<? extends BombEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
    }

    public BombEntity(EntityType<? extends BombEntity> type, LivingEntity owner, int orbitIndex, Level world) {
        super(type, owner, world);
        this.setOwner(owner);
        this.setOrbitIndex(orbitIndex);
        this.setNoGravity(true);
        this.pickup = Pickup.DISALLOWED;
        this.noPhysics = true;
    }

    @Override
    public boolean isPowered() { return true; }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ORBIT_INDEX, 0);
        this.entityData.define(DATA_IS_ATTACKING, false);
        this.entityData.define(DATA_OWNER_ID, 0);
        this.entityData.define(DATA_TARGET_ID, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("OrbitIndex", this.getOrbitIndex());
        compound.putBoolean("IsAttacking", this.isAttacking());
        compound.putInt("OwnerId", this.getOwnerId());
        compound.putInt("TargetId", this.getTargetId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setOrbitIndex(compound.getInt("OrbitIndex"));
        this.setAttacking(compound.getBoolean("IsAttacking"));
        this.setOwnerId(compound.getInt("OwnerId"));
        this.setTargetId(compound.getInt("TargetId"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public ItemStack getPickupItem() { return ItemStack.EMPTY; }

    @Override
    public void tick() {
        super.tick();
        if (this.inGround) { this.inGround = false; }

        if (!level().isClientSide) {
            LivingEntity owner = this.getOwner();
            if (owner == null || !owner.isAlive()) { this.discard(); return; }
            if (owner.level() != this.level()) { this.discard(); return; }

            if (owner instanceof Player player) {
                if (!net.diexv.potionenchant.event.ArmorXFeatureHandler.isWearingFullXArmor(player) ||
                    !net.diexv.potionenchant.event.ArmorXFeatureHandler.isRangedAttackEnabled(player)) {
                    this.discard(); return;
                }
            }

            // 碰撞检测（排除 XBlockEntity）
            List<Entity> entities = level().getEntities(this,
                this.getBoundingBox().inflate(0.5),
                e -> !(e instanceof BombEntity) &&
                     !(e instanceof Player) &&
                     !(e instanceof XBlockEntity) &&
                     !e.getType().equals(net.minecraft.world.entity.EntityType.ITEM) && !(e instanceof ExperienceOrb) &&
                     (owner == null || e != owner));

            if (!entities.isEmpty()) {
                affectEntitiesInRadius(this.position(), 5.0);
                spawnVisualLightning(this.position());
                this.discard(); return;
            }
        }

        // 追踪飞行距离
        if (lastPos != null) {
            traveledDistance += this.position().distanceTo(lastPos);
        }
        lastPos = this.position();

        if (this.isAttacking()) {
            trackAndAttack();
        } else {
            updateOrbitPosition();
        }
    }

    @Override
    public void onHitEntity(EntityHitResult entityHitResult) {
        if (!level().isClientSide) {
            Entity hitEntity = entityHitResult.getEntity();
            LivingEntity owner = this.getOwner();
            if (hitEntity instanceof Player ||
                hitEntity instanceof XBlockEntity ||
                (owner != null && hitEntity == owner) ||
                hitEntity.getType().equals(net.minecraft.world.entity.EntityType.ITEM)) {
                return;
            }
            affectEntitiesInRadius(entityHitResult.getLocation(), 5.0);
            spawnVisualLightning(entityHitResult.getLocation());
            this.discard();
        }
    }

    @Override
    public void onHitBlock(BlockHitResult blockHitResult) {
        if (!level().isClientSide) { this.inGround = false; }
    }

    private void affectEntitiesInRadius(Vec3 center, double radius) {
        AABB area = new AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius);
        LivingEntity owner = this.getOwner();

        List<Entity> entities = level().getEntitiesOfClass(Entity.class, area,
            e -> !(e instanceof BombEntity) &&
                 !(e instanceof Player) &&
                 !(e instanceof XBlockEntity) &&
                 !(e instanceof net.minecraft.world.entity.item.ItemEntity) && !(e instanceof ExperienceOrb) &&
                 (owner == null || e != owner));

        for (Entity entity : entities) {
            if (!entity.isAlive()) continue;
            spawnVisualLightning(entity.position());
            if (!(entity instanceof LivingEntity livingEntity)) continue;
                DamageSource damageSource = new DamageSource(
                    entity.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD));
                float damageMultiplier = 1.0f;
                if (owner instanceof Player player2) {
                    damageMultiplier = net.diexv.potionenchant.event.ArmorXFeatureHandler.getRangedDamageMultiplier(player2);
                }
                // 毁灭模式：秒杀
                if (owner instanceof Player destructionPlayer && ArmorXFeatureHandler.isDestructionModeEnabled(destructionPlayer)) {
                    XSwordTargetTracker.mark(livingEntity);
                    livingEntity.getEntityData().set(LivingEntityAccessor.HEALTH(), 0.0F);
                    livingEntity.die(damageSource);
                } else {
                    entity.hurt(damageSource, 10.0f * damageMultiplier);
                }
        }
    }
    private void trackAndAttack() {
        // XBlock发射的Bomb：飞行10格后扫描半径50格怪物
        if (firedByXBlock && traveledDistance >= 10.0 && !level().isClientSide) {
            scanAndAttackMonsters();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            Vec3 direction = target.position().subtract(this.position()).normalize();
            double playerSpeed = 0;
            LivingEntity owner = this.getOwner();
            if (owner != null) playerSpeed = owner.getDeltaMovement().length();
            this.setDeltaMovement(direction.scale(2.0 + playerSpeed));
        } else if (firedByXBlock) {
            // XBlock发射的无目标Bomb：保持运动方向，飞行10格后扫描
        } else {
            // 玩家发射但目标丢失：销毁
            this.discard();
        }
    }

    private void scanAndAttackMonsters() {
        Vec3 center = this.position();
        AABB area = new AABB(
            center.x - 50, center.y - 50, center.z - 50,
            center.x + 50, center.y + 50, center.z + 50);
        List<LivingEntity> monsters = level().getEntitiesOfClass(LivingEntity.class, area,
            e -> e instanceof Monster && e.isAlive() && !(e instanceof XBlockEntity) && e != this.getOwner());

        if (!monsters.isEmpty()) {
            LivingEntity nearest = monsters.get(0);
            double nearestDist = center.distanceToSqr(nearest.position());
            for (int i = 1; i < monsters.size(); i++) {
                double d = center.distanceToSqr(monsters.get(i).position());
                if (d < nearestDist) { nearest = monsters.get(i); nearestDist = d; }
            }
            Vec3 dir = nearest.position().subtract(center).normalize();
            double playerSpeed = 0;
            LivingEntity owner = this.getOwner();
            if (owner != null) playerSpeed = owner.getDeltaMovement().length();
            this.setDeltaMovement(dir.scale(2.0 + playerSpeed));
        } else {
            this.discard();
        }
    }

    private void spawnVisualLightning(Vec3 position) {
        if (level() instanceof ServerLevel serverLevel) {
            RainbowLightningBolt lightningBolt = ModEntities.RAINBOW_LIGHTNING.get().create(serverLevel);
            if (lightningBolt != null) {
                lightningBolt.moveTo(position);
                lightningBolt.setVisualOnly(true);
                lightningBolt.setCause(null);
                serverLevel.addFreshEntity(lightningBolt);
            }
        }
    }

    private void updateOrbitPosition() {
        if (!level().isClientSide) {
            LivingEntity owner = this.getOwner();
            if (owner != null) {
                this.setPos(owner.getX(), owner.getY() + 3.5, owner.getZ());
            }
        }
    }

    private void updateSwelling() {
        this.oldSwelling = (int)this.swelling;
        if (this.isPowered()) {
            this.swellAmount += (1.0F - this.swellAmount) * 0.4F;
        } else {
            this.swellAmount += (0.0F - this.swellAmount) * 0.25F;
        }
        this.oSwellAmount = this.swellAmount;
        this.swelling += this.swellAmount;
        if (this.swelling > 100.0F) { this.swelling = 0.0F; }
    }

    public float getSwelling(float partialTicks) {
        return Mth.lerp(partialTicks, this.oldSwelling, this.swelling);
    }

    public LivingEntity getOwner() {
        int ownerId = this.entityData.get(DATA_OWNER_ID);
        if (ownerId == 0) return null;
        Entity entity;
        if (level().isClientSide) { entity = level().getEntity(ownerId); }
        else { entity = ((ServerLevel) level()).getEntity(ownerId); }
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    public void setOwner(LivingEntity owner) {
        if (owner != null) { this.entityData.set(DATA_OWNER_ID, owner.getId()); }
        else { this.entityData.set(DATA_OWNER_ID, 0); }
    }

    public int getOwnerId() { return this.entityData.get(DATA_OWNER_ID); }
    public void setOwnerId(int ownerId) { this.entityData.set(DATA_OWNER_ID, ownerId); }

    public LivingEntity getTarget() {
        int targetId = this.entityData.get(DATA_TARGET_ID);
        if (targetId == 0) return null;
        Entity entity;
        if (level().isClientSide) { entity = level().getEntity(targetId); }
        else { entity = ((ServerLevel) level()).getEntity(targetId); }
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    public int getTargetId() { return this.entityData.get(DATA_TARGET_ID); }
    public void setTargetId(int targetId) { this.entityData.set(DATA_TARGET_ID, targetId); }

    public void setTarget(LivingEntity target) {
        if (target != null) { this.entityData.set(DATA_TARGET_ID, target.getId()); this.attackTarget = target; }
        else { this.entityData.set(DATA_TARGET_ID, 0); this.attackTarget = null; }
    }

    public int getOrbitIndex() { return this.entityData.get(DATA_ORBIT_INDEX); }
    public void setOrbitIndex(int index) { this.entityData.set(DATA_ORBIT_INDEX, index); }
    public boolean isAttacking() { return this.entityData.get(DATA_IS_ATTACKING); }
    public void setAttacking(boolean attacking) { this.entityData.set(DATA_IS_ATTACKING, attacking); }

    public void setFiredByXBlock(boolean v) { this.firedByXBlock = v; }
    public boolean isFiredByXBlock() { return firedByXBlock; }

    public void startAttack(LivingEntity target) {
        this.setTarget(target);
        this.setAttacking(true);
    }
}










