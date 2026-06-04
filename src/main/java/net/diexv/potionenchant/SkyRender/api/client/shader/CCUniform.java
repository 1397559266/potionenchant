package net.diexv.potionenchant.SkyRender.api.client.shader;

import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import net.diexv.potionenchant.mixin.accessor.UniformAccessor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;

public abstract class CCUniform extends Uniform implements ICCUniform {
    protected final UniformType type;

    @SuppressWarnings("ConstantConditions")
    protected CCUniform(String name, UniformType type, int count, @Nullable Shader parent) {
        super(name, type.getVanillaType(), count, parent);
        this.type = type;
        if (intValues != null) {
            MemoryUtil.memFree(intValues);
            intValues = null;
        }
        if (floatValues != null) {
            MemoryUtil.memFree(floatValues);
            floatValues = null;
        }
    }

    static CCUniform makeUniform(String name, UniformType type, int count, @Nullable Shader parent) {
        if (count % type.getSize() != 0)
            throw new IllegalArgumentException("Expected count to be a multiple of the uniform type size: " + type.getSize());
        return switch (type.getCarrier()) {
            case INT, U_INT -> new IntUniform(name, type, count, parent);
            case FLOAT, MATRIX -> new FloatUniform(name, type, count, parent);
            case DOUBLE, D_MATRIX -> new DoubleUniform(name, type, count, parent);
        };
    }

    private static abstract class UniformEntry<T> extends CCUniform {
        @Nullable
        protected T cache;
        protected boolean transpose;

        public UniformEntry(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
        }

        @Override
        public void set(float f0) {
            glUniformF(false, f0);
        }

        @Override
        public void set(float f0, float f1) {
            glUniformF(false, f0, f1);
        }

        @Override
        public void set(int i, float f) {
            throw new UnsupportedOperationException("Unable to set specific index.");
        }

        @Override
        public void set(float f0, float f1, float f2) {
            glUniformF(false, f0, f1, f2);
        }

        @Override
        public void set(Vector3f vec) {
            glUniformF(false, vec.x(), vec.y(), vec.z());
        }

        @Override
        public void set(float f0, float f1, float f2, float f3) {
            glUniformF(false, f0, f1, f2, f3);
        }

        @Override
        public void set(Vector4f vec) {
            glUniformF(false, vec.x(), vec.y(), vec.z(), vec.w());
        }

        @Override
        public void set(int i0) {
            glUniformI(i0);
        }

        @Override
        public void set(int i0, int i1) {
            glUniformI(i0, i1);
        }

        @Override
        public void set(int i0, int i1, int i2) {
            glUniformI(i0, i1, i2);
        }

        @Override
        public void set(int i0, int i1, int i2, int i3) {
            glUniformI(i0, i1, i2, i3);
        }

        @Override
        public void set(float[] p_85632_) {
            glUniformF(false, p_85632_);
        }

        @Override
        public void setMat2x2(float m00, float m01, float m10, float m11) {
            glUniformF(true, m00, m01, m10, m11);
        }

        @Override
        public void setMat2x3(float m00, float m01, float m02, float m10, float m11, float m12) {
            glUniformF(true, m00, m01, m02, m10, m11, m12);
        }

        @Override
        public void setMat2x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {
            glUniformF(true, m00, m01, m02, m03, m10, m11, m12, m13);
        }

        @Override
        public void setMat3x2(float m00, float m01, float m10, float m11, float m20, float m21) {
            glUniformF(true, m00, m01, m10, m11, m20, m21);
        }

        @Override
        public void setMat3x3(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
            glUniformF(true, m00, m01, m02, m10, m11, m12, m20, m21, m22);
        }

        @Override
        public void setMat3x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23) {
            glUniformF(true, m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
        }
        @Override
        public void setMatrix2x2Array(float[] values, int count) {
            if (type != UniformType.MAT2) {
                throw new IllegalStateException("Uniform '" + getName() + "' is not of type MAT2.");
            }
            if (values.length != count * 4) {
                throw new IllegalArgumentException("Invalid size for mat2 array. Expected " + (count * 4) + " floats, got " + values.length + ".");
            }
            glUniformF(false, values);
        }

        @Override
        public void setMat4x2(float m00, float m01, float m10, float m11, float m20, float m21, float m30, float m31) {
            glUniformF(true, m00, m01, m10, m11, m20, m21, m30, m31);
        }

        @Override
        public void setMat4x3(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22, float m30, float m31, float m32) {
            glUniformF(true, m00, m01, m02, m10, m11, m12, m20, m21, m22, m30, m31, m32);
        }

        @Override
        public void setMat4x4(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33) {
            glUniformF(true, m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33);
        }

        @Override
        public void set(Matrix4f mat) {
            glUniformF(false,
                mat.m00(), mat.m01(), mat.m02(), mat.m03(),
                mat.m10(), mat.m11(), mat.m12(), mat.m13(),
                mat.m20(), mat.m21(), mat.m22(), mat.m23(),
                mat.m30(), mat.m31(), mat.m32(), mat.m33()
            );
        }

        @Override
        public void set(Matrix3f mat) {
            glUniformF(false,
                mat.m00(), mat.m01(), mat.m02(),
                mat.m10(), mat.m11(), mat.m12(),
                mat.m20(), mat.m21(), mat.m22()
            );
        }

        @Override
        public void setSafe(float f0, float f1, float f2, float f3) {
            this.set(f0, f1, f2, f3);
        }

        @Override
        public void setSafe(int i0, int i1, int i2, int i3) {
            this.set(i0, i1, i2, i3);
        }

        protected void markDirtyInternal() {
            try {
                UniformAccessor accessor = (UniformAccessor) this;
                if (!accessor.getDirty()) {
                    accessor.setDirty(true);
                }
            } catch (ClassCastException e) {
                // Mixin hasn't woven UniformAccessor in this classloader context
                // (e.g. TRANSFORMER layer). Fall back to reflection.
                try {
                    java.lang.reflect.Field f = com.mojang.blaze3d.shaders.Uniform.class.getDeclaredField("dirty");
                    f.setAccessible(true);
                    f.setBoolean(this, true);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void upload() {
            UniformAccessor accessor = (UniformAccessor) this;
            if (!accessor.getDirty()) {
                return;
            }
            accessor.setDirty(false);
            flush();
        }

        public abstract void glUniformF(boolean transpose, float... values);
        public abstract void glUniformI(int... values);

        public void glUniformD(boolean transpose, double... values) {
            throw new UnsupportedOperationException("Double uniforms not supported by this type.");
        }
        public abstract void flush();
        public abstract int len(T cache);
        public abstract boolean equals(T a, T b);
    }

    private static class IntUniform extends UniformEntry<int[]> {
        public IntUniform(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
            assert type.getCarrier() == UniformType.Carrier.INT || type.getCarrier() == UniformType.Carrier.U_INT;
        }

        @Override
        public void glUniformF(boolean transpose, float... values) {
            int[] intValues = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                intValues[i] = (int) values[i];
            }
            glUniformI(intValues);
        }

        @Override
        public void glUniformI(int... values) {
            if (cache == null || !equals(cache, values)) {
                cache = values.clone();
                markDirtyInternal();
            }
        }

        @Override
        public void flush() {
            assert cache != null;
            switch (type) {
                case INT -> GL20.glUniform1iv(getLocation(), cache);
                case U_INT -> GL30.glUniform1uiv(getLocation(), cache);
                case I_VEC2, B_VEC2 -> GL20.glUniform2iv(getLocation(), cache);
                case U_VEC2 -> GL30.glUniform2uiv(getLocation(), cache);
                case I_VEC3, B_VEC3 -> GL20.glUniform3iv(getLocation(), cache);
                case U_VEC3 -> GL30.glUniform3uiv(getLocation(), cache);
                case I_VEC4, B_VEC4 -> GL20.glUniform4iv(getLocation(), cache);
                case U_VEC4 -> GL30.glUniform4uiv(getLocation(), cache);
                default -> throw new IllegalStateException("Unhandled uniform type for IntUniform: " + type);
            }
        }

        @Override
        public int len(int[] cache) {
            return cache.length;
        }

        @Override
        public boolean equals(int @Nullable [] a, int[] b) {
            return Arrays.equals(a, b);
        }
    }

    private static class FloatUniform extends UniformEntry<float[]> {
        public FloatUniform(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
            assert type.getCarrier() == UniformType.Carrier.FLOAT || type.getCarrier() == UniformType.Carrier.MATRIX;
        }

        @Override
        public void glUniformF(boolean transpose, float... values) {
            this.transpose = transpose;
            if (cache == null || !equals(cache, values)) {
                cache = values.clone();
                markDirtyInternal();
            }
        }

        @Override
        public void glUniformI(int... values) {
            float[] floatValues = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                floatValues[i] = (float) values[i];
            }
            glUniformF(false, floatValues);
        }

        @Override
        public void flush() {
            assert cache != null;
            switch (type) {
                case FLOAT -> GL20.glUniform1fv(getLocation(), cache);
                case VEC2 -> GL20.glUniform2fv(getLocation(), cache);
                case VEC3 -> GL20.glUniform3fv(getLocation(), cache);
                case VEC4 -> GL20.glUniform4fv(getLocation(), cache);
                case MAT2 -> GL20.glUniformMatrix2fv(getLocation(), transpose, cache);
                case MAT2x3 -> GL21.glUniformMatrix2x3fv(getLocation(), transpose, cache);
                case MAT2x4 -> GL21.glUniformMatrix2x4fv(getLocation(), transpose, cache);
                case MAT3 -> GL20.glUniformMatrix3fv(getLocation(), transpose, cache);
                case MAT3x2 -> GL21.glUniformMatrix3x2fv(getLocation(), transpose, cache);
                case MAT3x4 -> GL21.glUniformMatrix3x4fv(getLocation(), transpose, cache);
                case MAT4 -> GL20.glUniformMatrix4fv(getLocation(), transpose, cache);
                case MAT4x2 -> GL21.glUniformMatrix4x2fv(getLocation(), transpose, cache);
                case MAT4x3 -> GL21.glUniformMatrix4x3fv(getLocation(), transpose, cache);
                default -> throw new IllegalStateException("Unhandled uniform type for FloatUniform: " + type);
            }
        }

        @Override
        public int len(float[] cache) {
            return cache.length;
        }

        @Override
        public boolean equals(float @Nullable [] a, float[] b) {
            return Arrays.equals(a, b);
        }
    }

    private static class DoubleUniform extends UniformEntry<double[]> {
        public DoubleUniform(String name, UniformType type, int count, @Nullable Shader parent) {
            super(name, type, count, parent);
            assert type.getCarrier() == UniformType.Carrier.DOUBLE || type.getCarrier() == UniformType.Carrier.D_MATRIX;
        }

        @Override
        public void glUniformF(boolean transpose, float... values) {
            this.transpose = transpose;
            double[] doubleValues = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                doubleValues[i] = values[i];
            }
            if (cache == null || !equals(cache, doubleValues)) {
                cache = doubleValues;
                markDirtyInternal();
            }
        }

        @Override
        public void glUniformI(int... values) {
            double[] doubleValues = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                doubleValues[i] = values[i];
            }
            if (cache == null || !equals(cache, doubleValues)) {
                cache = doubleValues;
                markDirtyInternal();
            }
        }

        @Override
        public void flush() {
            assert cache != null;
            switch (type) {
                case DOUBLE -> GL40.glUniform1dv(getLocation(), cache);
                case D_VEC2 -> GL40.glUniform2dv(getLocation(), cache);
                case D_VEC3 -> GL40.glUniform3dv(getLocation(), cache);
                case D_VEC4 -> GL40.glUniform4dv(getLocation(), cache);
                case D_MAT2 -> GL40.glUniformMatrix2dv(getLocation(), transpose, cache);
                case D_MAT2x3 -> GL40.glUniformMatrix2x3dv(getLocation(), transpose, cache);
                case D_MAT2x4 -> GL40.glUniformMatrix2x4dv(getLocation(), transpose, cache);
                case D_MAT3 -> GL40.glUniformMatrix3dv(getLocation(), transpose, cache);
                case D_MAT3x2 -> GL40.glUniformMatrix3x2dv(getLocation(), transpose, cache);
                case D_MAT3x4 -> GL40.glUniformMatrix3x4dv(getLocation(), transpose, cache);
                case D_MAT4 -> GL40.glUniformMatrix4dv(getLocation(), transpose, cache);
                case D_MAT4x2 -> GL40.glUniformMatrix4x2dv(getLocation(), transpose, cache);
                case D_MAT4x3 -> GL40.glUniformMatrix4x3dv(getLocation(), transpose, cache);
                default -> throw new IllegalStateException("Unhandled uniform type for DoubleUniform: " + type);
            }
        }

        @Override
        public int len(double[] cache) {
            return cache.length;
        }

        @Override
        public boolean equals(double @Nullable [] a, double[] b) {
            return Arrays.equals(a, b);
        }
    }
}
