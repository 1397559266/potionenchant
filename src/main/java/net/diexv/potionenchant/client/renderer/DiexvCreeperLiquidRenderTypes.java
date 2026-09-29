package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * 苦力怕药水罐"液体"共用渲染层：
 * POSITION_COLOR + 不透明（写深度、无混合）→ 方块/手持/掉落液体都一致，
 * 外壳（半透明苦力怕）后画叠加在其上，避免透明排序导致液面不可见/透视错乱。
 */
public final class DiexvCreeperLiquidRenderTypes extends RenderType {

    public static final RenderType LIQUID_SOLID = create("potionenchant:liquid_solid",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    private DiexvCreeperLiquidRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                          boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState,
                                          Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
