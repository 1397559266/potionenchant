package net.diexv.potionenchant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * 彩虹闪电实体
 * 基于DiexvCreeper的RainbowLightningBoltEntity
 */
public class RainbowLightningBolt extends LightningBolt {
    
    private boolean visualOnly = false;
    
    public RainbowLightningBolt(EntityType<? extends LightningBolt> type, Level level) {
        super(type, level);
    }
    
    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
    }
    
    @Override
    public int getId() {
        // 确保ID大于200000000以触发彩虹渲染
        return 200000000 + super.getId();
    }
    
    public void setVisualOnly(boolean visualOnly) {
        this.visualOnly = visualOnly;
    }
    
    public boolean isVisualOnly() {
        return this.visualOnly;
    }
    
    @Override
    public void tick() {
        if (this.visualOnly) {
            // 仅视觉效果：不执行任何游戏逻辑（不起火、不伤害）
            // 只让实体自然消亡
            if (!this.level().isClientSide) {
                // 增加年龄，让实体在一定时间后自动消失
                this.tickCount++;
                if (this.tickCount > 100) { // 5秒后消失
                    this.discard();
                }
            }
            return;
        }
        super.tick();
    }
}
