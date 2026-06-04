package net.diexv.potionenchant.SkyRender.api.shader;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block侧的可选Hook
 */
public interface ShaderLayerBlock {
    @Nullable
    ShaderLayerProperties getShaderLayer(BlockState state);
}
