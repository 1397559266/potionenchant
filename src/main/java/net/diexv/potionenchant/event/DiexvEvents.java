package net.diexv.potionenchant.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端事件处理器
 * 注意：此类仅用于客户端渲染效果
 */
@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class DiexvEvents {
	// 神秘空瓶粒子
	private static final List<GuiParticle> MYSTERIOUS_PARTICLES = new ArrayList<>();
	@SuppressWarnings("removal")
	private static final ResourceLocation MYSTERIOUS_TEXTURE = new ResourceLocation(PotionEnchantMod.MODID, "textures/gui/potion.png");
	
	// 万能药水附魔瓶环绕粒子
	private static final List<OrbitParticle> ORBIT_PARTICLES = new ArrayList<>();
// 存储所有药水瓶的ItemStack
	private static final List<ItemStack> POTION_ITEMS = new ArrayList<>();
	// 当前使用的药水瓶索引（用于循环）
	private static int currentPotionIndex = 0;
	// 上次飞入/飞出的tick时间
	private static int lastSwapTick = 0;
	// 飞入/飞出间隔（20 ticks = 1秒）
	private static final int SWAP_INTERVAL = 20;
	// 最大粒子数
	private static final int MAX_PARTICLES = 50;
	
	

	// Tooltip显示状态追踪
	private static boolean isMysteriousTooltipShowing = false;
	private static boolean isUniversalTooltipShowing = false;
	// 全局透明度（用于淡入淡出）
	private static float mysteriousTooltipAlpha = 0f;
	private static float universalTooltipAlpha = 0f;
	private static float mysteriousTargetAlpha = 0f;
	private static float universalTargetAlpha = 0f;
	// 客户端tick计数器（用于检测tooltip关闭）
	private static int currentClientTick = 0;
	private static int lastMysteriousTooltipRenderTick = -1000;
	private static int lastUniversalTooltipRenderTick = -1000;

	@SubscribeEvent
	public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
		// 检测tooltip显示状态
		if (event.getItemStack().is(ModItems.MYSTERIOUS_EMPTY_BOTTLE.get())) {
			isMysteriousTooltipShowing = true;
			lastMysteriousTooltipRenderTick = currentClientTick;
			mysteriousTargetAlpha = 1.0f;
			// 如果当前透明度太低，立即设置为一个最小可见值，确保淡入动画开始时就能看到
			if (mysteriousTooltipAlpha < 0.1f) {
				mysteriousTooltipAlpha = 0.1f;
			}
		}
		
		if (event.getItemStack().is(ModItems.UNIVERSAL_POTION_BOTTLE.get())) {
			isUniversalTooltipShowing = true;
			lastUniversalTooltipRenderTick = currentClientTick;
			universalTargetAlpha = 1.0f;
			// 如果当前透明度太低，立即设置为一个最小可见值，确保淡入动画开始时就能看到
			if (universalTooltipAlpha < 0.1f) {
				universalTooltipAlpha = 0.1f;
			}
		}
	}

	@SubscribeEvent
	public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
		// 始终渲染粒子效果（包括tooltip显示时的淡入和关闭后的淡出）
		Minecraft mc = Minecraft.getInstance();
		
		if (mysteriousTooltipAlpha > 0.01f || universalTooltipAlpha > 0.01f) {
			GuiGraphics graphics = event.getGuiGraphics();
			float partialTicks = mc.getFrameTime();
			
			// 先 flush 已有批次，确保屏幕内容已提交到 GPU
			graphics.flush();
			// 禁用深度测试，让粒子无视 z-level 直接画在最顶层
			com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
			graphics.pose().pushPose();
			graphics.pose().translate(0, 0, 0);
			
			if (mysteriousTooltipAlpha > 0.01f) {
				for (GuiParticle particle : MYSTERIOUS_PARTICLES) {
					particle.draw(mc, graphics, partialTicks, mysteriousTooltipAlpha);
				}
			}
			
			if (universalTooltipAlpha > 0.01f) {
				renderUniversalBottleAnimation(mc, graphics, partialTicks, universalTooltipAlpha);
			}
			
			com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
			graphics.pose().popPose();
			graphics.flush();
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			Minecraft mc = Minecraft.getInstance();
			int winW = mc.getWindow().getGuiScaledWidth();
			int winH = mc.getWindow().getGuiScaledHeight();
			
			// 增加客户端tick计数器
			currentClientTick++;
			
			// 检测tooltip是否已关闭（超过2个tick没有渲染）
			if (currentClientTick - lastMysteriousTooltipRenderTick > 2) {
				isMysteriousTooltipShowing = false;
				mysteriousTargetAlpha = 0.0f;
			}
			
			if (currentClientTick - lastUniversalTooltipRenderTick > 2) {
				isUniversalTooltipShowing = false;
				universalTargetAlpha = 0.0f;
			}
			
			// 平滑更新透明度（淡入淡出效果）
			// 使用插值让透明度平滑过渡
			float alphaSpeed = 0.15f; // 调整这个值可以改变淡入淡出速度
			mysteriousTooltipAlpha += (mysteriousTargetAlpha - mysteriousTooltipAlpha) * alphaSpeed;
			universalTooltipAlpha += (universalTargetAlpha - universalTooltipAlpha) * alphaSpeed;
			
			// 防止浮点误差，当非常接近目标值时直接设置
			if (Math.abs(mysteriousTooltipAlpha - mysteriousTargetAlpha) < 0.001f) {
				mysteriousTooltipAlpha = mysteriousTargetAlpha;
			}
			if (Math.abs(universalTooltipAlpha - universalTargetAlpha) < 0.001f) {
				universalTooltipAlpha = universalTargetAlpha;
			}
			
			// 更新神秘空瓶粒子
			updateMysteriousParticles(mc, winW, winH);
			
			// 更新万能药水附魔瓶环绕粒子
			updateOrbitParticles(mc, winW, winH);
			
			
		}
	}
	
	/**
	 * 更新神秘空瓶粒子
	 */
	private static void updateMysteriousParticles(Minecraft mc, int winW, int winH) {
		if (MYSTERIOUS_PARTICLES.isEmpty()) {
			for (int i = 0; i < 50; i++) {
				MYSTERIOUS_PARTICLES.add(new GuiParticle(
					MYSTERIOUS_TEXTURE,
					DiexvEventCore.randfloat(0, winW),
					DiexvEventCore.randfloat(0, winH),
					DiexvEventCore.randfloat(-1F, 1F),
					DiexvEventCore.randfloat(2F, 3F),
					16, 16,
					DiexvEventCore.randfloat(-0.8F, 0.8F),
					DiexvEventCore.RANDOM.nextInt(43)
				));
			}
		}
		for (GuiParticle particle : MYSTERIOUS_PARTICLES) {
			particle.tick();
			if (particle.outOfView(mc)) {
				particle.set(
					DiexvEventCore.randfloat(0, winW),
					0,
					DiexvEventCore.randfloat(-1F, 1F),
					DiexvEventCore.randfloat(2F, 3F),
					16, 16,
					DiexvEventCore.randfloat(-0.8F, 0.8F),
					DiexvEventCore.RANDOM.nextInt(43)
				);
			}
		}
	}
	
	/**
	 * 更新万能药水附魔瓶环绕粒子
	 */
	private static void updateOrbitParticles(Minecraft mc, int winW, int winH) {
		if (ORBIT_PARTICLES.isEmpty()) {
			// 收集所有原版药水瓶物品
			collectAllPotions();
			
			// 初始化50个药水瓶粒子
			float dynamicRadius = Math.min(winW, winH) * 0.75f;
			for (int i = 0; i < MAX_PARTICLES && i < POTION_ITEMS.size(); i++) {
				ItemStack potionStack = POTION_ITEMS.get(i % POTION_ITEMS.size());
				float angle = DiexvEventCore.randfloat(0, (float)(Math.PI * 2));
				float angularVelocity = 0.02f + DiexvEventCore.randfloat(-0.005f, 0.005f);
				float yOffset = DiexvEventCore.randfloat(-30f, 30f);
				
				OrbitParticle particle = new OrbitParticle(
					potionStack,
					angle,
					angularVelocity,
					24, 24,
					yOffset,
					dynamicRadius
				);
				ORBIT_PARTICLES.add(particle);
			}
			
			currentPotionIndex = MAX_PARTICLES % POTION_ITEMS.size();
			lastSwapTick = 0;
			return;
		}
		
		// 获取当前tick数
		int currentTick = (int)(mc.level != null ? mc.level.getGameTime() : 0);
		
		// 检查是否需要飞入/飞出（每秒一次）
		if (currentTick - lastSwapTick >= SWAP_INTERVAL) {
			lastSwapTick = currentTick;
			
			// 找到距离屏幕右侧最近的粒子（角度接近 0 或 2π 的粒子）
			OrbitParticle particleToFlyOut = null;
			float minDistanceToRight = Float.MAX_VALUE;
			
			for (OrbitParticle particle : ORBIT_PARTICLES) {
				// 只选择不在飞入/飞出状态的粒子
				if (!particle.isFlyingIn && !particle.isFlyingOut) {
					// 计算粒子距离右侧的距离（角度越接近0，距离右侧越近）
					float angle = particle.angle;
					float distanceToRight = Math.abs(angle); // 角度接近0表示在右侧
					
					if (distanceToRight < minDistanceToRight) {
						minDistanceToRight = distanceToRight;
						particleToFlyOut = particle;
					}
				}
			}
			
			// 如果有粒子可以飞出
			if (particleToFlyOut != null) {
				// 开始飞出动画
				particleToFlyOut.startFlyOut(winW);
				
				// 准备新粒子飞入
				if (!POTION_ITEMS.isEmpty()) {
					// 优先选择环内没有的药水
					ItemStack nextPotion = getNextUniquePotion();
					
					// 在左侧创建新粒子（角度为 π，即左侧）
					float flyInAngle = (float)Math.PI;
					float angularVelocity = 0.02f + DiexvEventCore.randfloat(-0.005f, 0.005f);
					float yOffset = DiexvEventCore.randfloat(-30f, 30f);
					float dynamicRadius = Math.min(winW, winH) * 0.75f;
					
					OrbitParticle newParticle = new OrbitParticle(
						nextPotion,
						flyInAngle,
						angularVelocity,
						24, 24,
						yOffset,
						dynamicRadius
					);
					
					// 开始飞入动画
					newParticle.startFlyIn(flyInAngle, winW);
					ORBIT_PARTICLES.add(newParticle);
				}
			}
		}
		
		// 更新所有环绕粒子
		List<OrbitParticle> particlesToRemove = new ArrayList<>();
		
		for (OrbitParticle particle : ORBIT_PARTICLES) {
			particle.tick();
			
			// 如果飞出动画完成，标记为移除
			if (particle.isFlyingOut && particle.flyOutProgress >= 1.0f) {
				particlesToRemove.add(particle);
			}
		}
		
		// 移除飞出完成的粒子
		ORBIT_PARTICLES.removeAll(particlesToRemove);
	}
	
	/**
	 * 获取下一个环内没有的药水（优先选择不重复的）
	 */
	private static ItemStack getNextUniquePotion() {
		if (POTION_ITEMS.isEmpty()) return null;
		
		// 获取当前环内所有药水的描述 ID
		java.util.Set<String> currentPotionIds = new java.util.HashSet<>();
		for (OrbitParticle particle : ORBIT_PARTICLES) {
			if (!particle.isFlyingOut && particle.potionStack != null) {
				currentPotionIds.add(particle.potionStack.getDescriptionId());
			}
		}
		
		// 优先选择环内没有的药水
		for (int i = 0; i < POTION_ITEMS.size(); i++) {
			int index = (currentPotionIndex + i) % POTION_ITEMS.size();
			ItemStack potion = POTION_ITEMS.get(index);
			if (!currentPotionIds.contains(potion.getDescriptionId())) {
				currentPotionIndex = (index + 1) % POTION_ITEMS.size();
				return potion.copy();
			}
		}
		
		// 如果所有药水都在环内了，随机选择一个
		currentPotionIndex = (currentPotionIndex + 1) % POTION_ITEMS.size();
		return POTION_ITEMS.get(currentPotionIndex).copy();
	}
	
	/**
	 * 收集所有原版药水瓶物品
	 */
	private static void collectAllPotions() {
		if (!POTION_ITEMS.isEmpty()) return;
		
		int potionCount = 0;
		// 遍历所有注册的药水效果（包括本模组的特殊效果）
		for (net.minecraft.world.effect.MobEffect effect : net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValues()) {
			try {
				// 为每种效果创建一种等级的药水（颜色与等级无关）
				int amplifier = 0; // 只需要一个等级
				
				// 只创建普通药水（不需要喷溅和滞留）
				ItemStack potion = createColoredPotion(effect, amplifier);
				if (potion != null) {
					POTION_ITEMS.add(potion);
					potionCount++;
				}
			} catch (Exception e) {
				// 跳过无法处理的药水
			}
		}
	}
	
	/**
	 * 创建带有指定效果的彩色药水
	 */
	private static ItemStack createColoredPotion(net.minecraft.world.effect.MobEffect effect, int amplifier) {
		try {
			ItemStack stack = new ItemStack(net.minecraft.world.item.Items.POTION);
			// 添加药水效果
			net.minecraft.world.effect.MobEffectInstance effectInstance = 
				new net.minecraft.world.effect.MobEffectInstance(effect, 3600, amplifier);
			net.minecraft.world.item.alchemy.PotionUtils.setCustomEffects(stack, java.util.Collections.singletonList(effectInstance));
			
			// 直接从效果获取颜色，而不是从 ItemStack 获取
			int color = effect.getColor();
			
			// 将颜色存储到 ItemStack 的 NBT 中，用于渲染时参考
			var tag = stack.getOrCreateTag();
			tag.putInt("CustomPotionColor", color);
			
			return stack;
		} catch (Exception e) {
			return null;
		}
	}
	


	
	/**
	 * 渲染万能药水附魔瓶动画
	 */
	private static void renderUniversalBottleAnimation(Minecraft mc, GuiGraphics graphics, float partialTicks, float globalAlpha) {
		int winW = mc.getWindow().getGuiScaledWidth();
		int winH = mc.getWindow().getGuiScaledHeight();
		float centerX = winW / 2f;
		float centerY = winH / 2f;
		
		PoseStack poseStack = graphics.pose();
		
		// 1. 渲染环绕的药水瓶
		for (OrbitParticle particle : ORBIT_PARTICLES) {
			particle.draw(mc, graphics, centerX, centerY, partialTicks, globalAlpha);
		}
		

	}
}
