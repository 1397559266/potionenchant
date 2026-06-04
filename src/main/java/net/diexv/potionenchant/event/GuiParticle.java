package net.diexv.potionenchant.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class GuiParticle {
	public static final Map<ResourceLocation, int[]> RES_SIZE_CACHE = new HashMap<>();
	// 纹理
	public ResourceLocation res;
	// 逻辑位置
	public float x;
	public float y;
	// 上一 tick 的位置（用于插值）
	public float prevX;
	public float prevY;
	// 逻辑速度
	public float xM;
	public float yM;
	// 尺寸
	public int imgWidth;
	public int imgHeight;
	public float width;
	public float height;
	// 旋转角度
	public float angle;
	public float prevAngle;
	public float angleM;
	// 帧动画 index
	public int index;

	public GuiParticle(ResourceLocation res, float x, float y, float xM, float yM, float width, float height, float angleM, int index) {
		this.res = res;
		int[] imgSize = RES_SIZE_CACHE.computeIfAbsent(this.res, (r) -> DiexvEventCore.getPngDimensions(r));
		this.imgWidth = imgSize[0];
		this.imgHeight = imgSize[1];
		set(x, y, xM, yM, width, height, angleM, index);
	}

	public void set(float x, float y, float xM, float yM, float width, float height, float angleM, int index) {
		this.x = x;
		this.y = y;
		this.xM = xM;
		this.yM = yM;
		this.width = width;
		this.height = height;
		this.angleM = angleM;
		this.index = index % (imgHeight / imgWidth);
		// 初始化 prev 状态
		this.prevX = this.x;
		this.prevY = this.y;
		this.prevAngle = this.angle;
	}

	/**
	 * 每 tick（20Hz）调用，用来更新粒子的逻辑位置。
	 */
	public void tick() {
		this.prevX = this.x;
		this.prevY = this.y;
		this.prevAngle = this.angle;
		this.x += this.xM;
		this.y += this.yM;
		this.angle += this.angleM;
	}

	/**
	 * 判断是否在屏幕外
	 */
	public boolean outOfView(Minecraft mc) {
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();
		float halfW = width * 0.5f;
		float halfH = height * 0.5f;
		float left = x - halfW;
		float right = x + halfW;
		float top = y - halfH;
		float bottom = y + halfH;
		return right < 0 || left > screenW || bottom < 0 || top > screenH;
	}

	/**
	 * 使用 partialTicks 进行渲染插值，让粒子平滑移动
	 * @param globalAlpha 全局透明度（0-1）
	 */
	public boolean draw(Minecraft mc, GuiGraphics graphics, float partialTicks, float globalAlpha) {
		if (outOfView(mc))
			return false;
		// 插值渲染位置
		float renderX = prevX + (x - prevX) * partialTicks;
		float renderY = prevY + (y - prevY) * partialTicks;
		float renderAngle = prevAngle + (angle - prevAngle) * partialTicks;
		PoseStack poseStack = graphics.pose();
		int frameSize = imgWidth;
		int v = index * frameSize;
		poseStack.pushPose();
		// 移动到插值位置（使用极高Z轴确保在最顶层）
		poseStack.translate(renderX, renderY, 500); // Z=500相对于父级poseStack
		// 旋转（插倿）
		poseStack.mulPose(Axis.ZP.rotationDegrees(renderAngle));
		// 缩放
		poseStack.scale(width / frameSize, height / frameSize, 1.0F);
		// 原点校正
		poseStack.translate(-frameSize / 2f, -frameSize / 2f, 0);
			
		// 绘制 - 注意：blit方法本身不支持透明度参数
		// 我们通过RenderSystem来实现透明度效果
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, globalAlpha);
			
		// 绘制
		graphics.blit(res, 0, 0, 0, v, frameSize, frameSize, imgWidth, imgHeight);
			
		// 恢复颜色
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		com.mojang.blaze3d.systems.RenderSystem.disableBlend();
			
		poseStack.popPose();
		return true;
	}
}
