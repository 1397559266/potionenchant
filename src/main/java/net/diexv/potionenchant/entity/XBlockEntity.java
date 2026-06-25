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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.diexv.potionenchant.util.XSwordTargetTracker;
import net.diexv.potionenchant.mixin.accessor.LivingEntityAccessor;
import net.diexv.potionenchant.event.ArmorXFeatureHandler;

import java.util.List;

public class XBlockEntity extends Monster implements PowerableMob {

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(XBlockEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(XBlockEntity.class, EntityDataSerializers.INT);

    private float swelling;
    private int oldSwelling;
    private float swellAmount;
    private float oSwellAmount;

    private static final double DAMAGE_RADIUS = 5.0;

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, Double.MAX_VALUE)
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            .add(Attributes.ATTACK_DAMAGE, 0.0D)
            .add(Attributes.ARMOR, Double.MAX_VALUE)
            .add(Attributes.FOLLOW_RANGE, 0.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public XBlockEntity(PlayMessages.SpawnEntity packet, Level world) {
        super(ModEntities.XBLOCK.get(), world);
    }

    public XBlockEntity(EntityType<? extends XBlockEntity> type, Level world) {
        super(type, world);
    }

    public XBlockEntity(EntityType<? extends XBlockEntity> type, LivingEntity owner, Level world) {
        super(type, world);
        if (type == null || world == null) return;
        try {
            this.setOwner(owner);
            this.setNoGravity(true);
            this.setInvulnerable(true);
            this.setCustomNameVisible(false);
        } catch (Exception ignored) {}
    }

    @Override public boolean isPowered() { return true; }
    @Override public float getScale() { return 1.0F; }
    @Override public boolean isNoGravity() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isInvulnerable() { return true; }
    @Override public boolean fireImmune() { return true; }
    @Override public float getHealth() { return Float.MAX_VALUE; }
    @Override protected void registerGoals() {}
    @Override public boolean shouldRenderAtSqrDistance(double d) { return true; }

    @Override
    public AABB getBoundingBoxForCulling() {
        double renderRadius = DAMAGE_RADIUS * 15;
        return this.getBoundingBox().inflate(renderRadius);
    }

    @Override
    protected AABB makeBoundingBox() {
        double d0 = this.getX() - 0.5;
        double d1 = this.getY();
        double d2 = this.getZ() - 0.5;
        return new AABB(d0, d1, d2, d0 + 1.0, d1 + 1.0, d2 + 1.0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("OwnerId", this.getOwnerId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        this.setOwnerId(compound.getInt("OwnerId"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_ID, 0);
        this.entityData.define(DATA_TARGET_ID, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getOwner() == null) {
            this.discard(); return;
        }
        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            this.discard(); return;
        }
        if (!owner.level().equals(this.level())) {
            this.discard(); return;
        }

        // 追踪目标
        LivingEntity target = this.getTarget();
        if (target != null) {
            if (!target.isAlive()) {
                this.setTarget(null);
            } else {
                this.setPos(target.getX(), target.getY(), target.getZ());
            }
        }

        if (!level().isClientSide) {
            dealDamageToNearbyMobs();
        }

        updateSwelling();
    }

    private void dealDamageToNearbyMobs() {
        Vec3 center = this.position();
        AABB area = new AABB(
            center.x - DAMAGE_RADIUS, center.y - DAMAGE_RADIUS, center.z - DAMAGE_RADIUS,
            center.x + DAMAGE_RADIUS, center.y + DAMAGE_RADIUS, center.z + DAMAGE_RADIUS);
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, area,
            e -> !(e instanceof Player) && e.isAlive());

        LivingEntity blockOwner = this.getOwner();

        for (LivingEntity entity : entities) {
            if (!isHostileMob(entity)) continue;
            DamageSource damageSource;
            if (blockOwner instanceof net.minecraft.world.entity.player.Player p) {
                damageSource = p.damageSources().playerAttack(p);
            } else if (blockOwner != null) {
                damageSource = blockOwner.damageSources().mobAttack(blockOwner);
            } else {
                damageSource = entity.damageSources().magic();
            }
            float damageMultiplier = 1.0f;
            if (blockOwner instanceof Player player2) {
                damageMultiplier = net.diexv.potionenchant.event.ArmorXFeatureHandler.getRangedDamageMultiplier(player2);
            }
            // 毁灭模式：秒杀
            if (blockOwner instanceof Player destructionPlayer && ArmorXFeatureHandler.isDestructionModeEnabled(destructionPlayer)) {
                XSwordTargetTracker.mark(entity);
                entity.hurt(damageSource, Float.MAX_VALUE);
            } else {
                entity.hurt(damageSource, 1.0f * damageMultiplier);
            }
            entity.invulnerableTime = 0;
        }
    }

    private boolean isHostileMob(LivingEntity entity) {
        return entity.getType().getCategory() == MobCategory.MONSTER || entity instanceof Monster;
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

    public void setTarget(LivingEntity target) {
        if (target != null) { this.entityData.set(DATA_TARGET_ID, target.getId()); }
        else { this.entityData.set(DATA_TARGET_ID, 0); }
    }
}

