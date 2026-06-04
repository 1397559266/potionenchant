package net.diexv.potionenchant.mixin.accessor;

import com.mojang.blaze3d.shaders.Uniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(Uniform.class)
public interface UniformAccessor {
    @Accessor("intValues")
    IntBuffer getIntValues();

    @Accessor("intValues")
    void setIntValues(IntBuffer intValues);

    @Accessor("floatValues")
    FloatBuffer getFloatValues();

    @Accessor("floatValues")
    void setFloatValues(FloatBuffer floatValues);

    @Accessor("dirty")
    boolean getDirty();

    @Accessor("dirty")
    void setDirty(boolean dirty);
}