package net.diexv.potionenchant.mixin.accessor;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.concurrent.locks.ReadWriteLock;

@Mixin(SynchedEntityData.class)
public interface SyncEntityDataAccessor {
    @Accessor("entity")
    Entity caller();

    @Accessor("itemsById")
    Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById();

    @Accessor("lock")
    ReadWriteLock lock();

    @Accessor("isDirty")
    boolean getIsDirty();

    @Accessor("isDirty")
    void setIsDirty(boolean dirty);
}
