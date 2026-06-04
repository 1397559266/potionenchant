package net.diexv.potionenchant.SkyRender.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelState;

/**
 * 记录宇宙渲染属性：模型变换状态和渲染类型
 */
public record CosmicRenderProperties(ModelState modelState, RenderType renderType) {
}
