package net.diexv.potionenchant.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class UltimateEnchantTableBlockEntity extends BlockEntity {

    public int time;

    public UltimateEnchantTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ULTIMATE_ENCHANT_TABLE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UltimateEnchantTableBlockEntity be) {
        be.time++;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return super.getUpdateTag();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
    }
}
