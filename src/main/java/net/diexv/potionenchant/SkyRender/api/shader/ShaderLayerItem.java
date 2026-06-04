package net.diexv.potionenchant.SkyRender.api.shader;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 在Item上实现此接口以使用ShaderLayer物品渲染
 */
public interface ShaderLayerItem {
    @Nullable
    ShaderLayerProperties getShaderLayer(ItemStack stack);
}
