package net.diexv.potionenchant.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * 环绕粒子 - 用于药水瓶环绕中心旋转的效果
 */
public class OrbitParticle {
	public static final Map<ResourceLocation, int[]> RES_SIZE_CACHE = new HashMap<>();
	
	// 药水物品栈
	public ItemStack potionStack;
	// 当前角度（弧度）
	public float angle;
	// 角速度（弧度/tick）
	public float angularVelocity;
	// 尺寸
	public float width;
	public float height;
	// Y轴偏移（让不同瓶子在不同高度）
	public float yOffset;
	// 轨道半径
	public float radius;
	// 飞入动画相关
	public boolean isFlyingIn = false;
	public boolean isFlyingOut = false;
	public float flyInProgress = 0f; // 飞入进度 0-1
	public float flyOutProgress = 0f; // 飞出进度 0-1
	public float flyInStartX; // 飞入起始位置（屏幕左侧外）
	public float flyInTargetAngle; // 飞入目标角度
	public float flyOutStartAngle; // 飞出起始角度
	
	public OrbitParticle(ItemStack potionStack, float angle, float angularVelocity, float width, float height, float yOffset, float radius) {
		this.potionStack = potionStack;
		this.angle = angle;
		this.angularVelocity = angularVelocity;
		this.width = width;
		this.height = height;
		this.yOffset = yOffset;
		this.radius = radius;
	}
	
	/**
	 * 每 tick 调用，更新角度和飞入/飞出状态
	 */
	public void tick() {
		// 飞入/飞出/正常运动时都持续更新角度
		this.angle += this.angularVelocity;
		if (this.angle > Math.PI * 2) {
			this.angle -= Math.PI * 2;
		}
		if (this.angle < 0) {
			this.angle += Math.PI * 2;
		}
		
		// 更新飞入动画进度
		if (isFlyingIn) {
			flyInProgress += 0.05f; // 20 ticks (约1秒) 完成飞入 - 加快速度
			if (flyInProgress >= 1.0f) {
				flyInProgress = 1.0f;
				isFlyingIn = false;
			}
		}
		
		// 更新飞出动画进度
		if (isFlyingOut) {
			flyOutProgress += 0.05f; // 20 ticks (约1秒) 完成飞出 - 加快速度
			if (flyOutProgress >= 1.0f) {
				flyOutProgress = 1.0f;
			}
		}
	}
	
	/**
	 * 开始飞入动画（从屏幕左侧飞入）
	 */
	public void startFlyIn(float targetAngle, float screenW) {
		this.isFlyingIn = true;
		this.isFlyingOut = false;
		this.flyInProgress = 0f;
		this.flyInTargetAngle = targetAngle;
		this.flyInStartX = -screenW; // 从屏幕左侧外开始
		this.angle = targetAngle; // 设置目标角度
	}
	
	/**
	 * 开始飞出动画（向屏幕右侧飞出）
	 */
	public void startFlyOut(float screenW) {
		this.isFlyingOut = true;
		this.isFlyingIn = false;
		this.flyOutProgress = 0f;
		this.flyOutStartAngle = this.angle;
	}
	
	/**
	 * 判断是否在屏幕外（参考GuiParticle的实现）
	 */
	public boolean outOfView(Minecraft mc, int screenW, int screenH) {
		float halfW = width * 0.5f;
		float halfH = height * 0.5f;
		float left = -halfW; // 相对于中心的左边界
		float right = halfW; // 相对于中心的右边界
		float top = -halfH; // 相对于中心的上边界
		float bottom = halfH; // 相对于中心的下边界
		return right < -screenW/2 || left > screenW/2 || bottom < -screenH/2 || top > screenH/2;
	}
	
	/**
	 * 计算当前位置（相对于屏幕中心）
	 */
	public float[] getPosition(float centerX, float centerY) {
		float x = centerX + (float)(Math.cos(this.angle) * this.radius);
		float y = centerY + this.yOffset + (float)(Math.sin(this.angle) * this.radius * 0.3f); // Y轴压缩，形成椭圆轨道
		return new float[]{x, y};
	}
	
	/**
	 * 渲染粒子
	 * @param globalAlpha 全局透明度（0-1）
	 */
	public boolean draw(Minecraft mc, GuiGraphics graphics, float centerX, float centerY, float partialTicks, float globalAlpha) {
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();
		float dynamicRadius = Math.min(screenW, screenH) * 0.75f;
		
		float renderX, renderY;
		float renderAngle;
		
		// 插值角度（平滑渲染）
		float prevAngle = this.angle - this.angularVelocity;
		renderAngle = prevAngle + (this.angle - prevAngle) * partialTicks;
		
		// 处理飞入动画 - 从屏幕外飞入
		if (isFlyingIn) {
			// 使用平滑的缓动函数
			float t = flyInProgress;
			float easedProgress = t * t * (3.0f - 2.0f * t); // smoothstep
			
			// 飞入：从屏幕左侧外 (-screenW) 飞到轨道位置
			float orbitX = centerX + (float)(Math.cos(renderAngle) * dynamicRadius);
			float orbitY = centerY + this.yOffset + (float)(Math.sin(renderAngle) * dynamicRadius * 0.3f);
			
			// 起始位置：屏幕左侧外
			float startX = -50;
			float startY = orbitY;
			
			// 插值
			renderX = startX + (orbitX - startX) * easedProgress;
			renderY = startY + (orbitY - startY) * easedProgress * 0.3f; // Y轴缓慢过渡
		}
		// 处理飞出动画 - 飞向屏幕外
		else if (isFlyingOut) {
			// 使用平滑的缓动函数
			float t = flyOutProgress;
			float easedProgress = t * t * (3.0f - 2.0f * t); // smoothstep
			
			// 飞出：从轨道位置飞到屏幕右侧外
			float orbitX = centerX + (float)(Math.cos(renderAngle) * dynamicRadius);
			float orbitY = centerY + this.yOffset + (float)(Math.sin(renderAngle) * dynamicRadius * 0.3f);
			
			// 目标位置：屏幕右侧外
			float targetX = screenW + 50;
			float targetY = orbitY;
			
			// 插值
			renderX = orbitX + (targetX - orbitX) * easedProgress;
			renderY = orbitY + (targetY - orbitY) * easedProgress * 0.3f;
		}
		// 正常环绕动画
		else {
			// 计算轨道位置
			renderX = centerX + (float)(Math.cos(renderAngle) * dynamicRadius);
			renderY = centerY + this.yOffset + (float)(Math.sin(renderAngle) * dynamicRadius * 0.3f);
		}
		
		// 检查是否在屏幕外
		float halfW = width * 0.5f;
		float halfH = height * 0.5f;
		if (renderX + halfW < 0 || renderX - halfW > screenW || 
			renderY + halfH < 0 || renderY - halfH > screenH) {
			return false;
		}
		
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		
		// 移动到计算位置（使用极高Z轴确保在最顶层）
		poseStack.translate(renderX, renderY, 500); // Z=500相对于父级poseStack
		
		// 根据轨道位置计算缩放和透明度（模拟远近效果）
		// 飞入/飞出时使用固定透明度
		float depthScale;
		float alpha;
		if (isFlyingIn || isFlyingOut) {
			depthScale = 1.0f;
			alpha = 1.0f;
		} else {
			depthScale = 0.7f + 0.3f * (float)(Math.sin(renderAngle) + 1) / 2; // 0.7-1.0
			alpha = 0.6f + 0.4f * (float)(Math.sin(renderAngle) + 1) / 2; // 0.6-1.0
		}
		
		// 应用缩放
		poseStack.scale(depthScale, depthScale, 1.0F);
		
		// 绘制药水瓶（使用Minecraft的物品渲染系统）
		// 居中绘制
		poseStack.translate(-width / 2f, -height / 2f, 0);
		
		// 使用Minecraft的物品渲染系统绘制药水瓶
		// 依赖 ItemStack 中的 CustomPotionColor 标签，让游戏自动处理液体颜色，保持瓶身透明
		try {
			// 应用全局透明度
			com.mojang.blaze3d.systems.RenderSystem.enableBlend();
			com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
			com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, globalAlpha);
			
			// 直接渲染物品
			graphics.renderItem(this.potionStack, 0, 0);
			
			// 恢复颜色
			com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			com.mojang.blaze3d.systems.RenderSystem.disableBlend();
		} catch (Exception e) {
			// 如果渲染失败，绘制一个默认颜色的矩形
			int defaultColor = 0xFFA84C; // 默认药水颜色
			int colorAlpha = (int)(globalAlpha * 255);
			int colorWithAlpha = (colorAlpha << 24) | defaultColor;
			graphics.fill(0, 0, (int)width, (int)height, colorWithAlpha);
		}
		
		poseStack.popPose();
		return true;
	}
}
