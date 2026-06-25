package net.diexv.potionenchant.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import com.mojang.math.Axis;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.config.PotionEnchantConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor;
import net.diexv.potionenchant.client.renderer.hyperlink.PolygonRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = PotionEnchantMod.MODID, value = Dist.CLIENT)
public class CustomMainMenuHandler {

    private static final Random RANDOM = new Random();

    private static final ResourceLocation CUSTOM_BG = new ResourceLocation(PotionEnchantMod.MODID, "textures/gui/main_menu_bg.png");
    private static final ResourceLocation CUSTOM_LOGO = new ResourceLocation(PotionEnchantMod.MODID, "textures/gui/main_menu_logo.png");

    // 物品贴图池（直接 blit 渲染，跳过 cosmic 模型加载器）
    private static List<ResourceLocation> ITEM_TEXTURES = null;
    /** X 系列物品粒子贴图池缓存（随机访问用） */
    private static List<ResourceLocation> PARTICLE_TEXTURES = null;
    /** 每张贴图的帧数（从纹理尺寸自动检测，不依赖 mcmeta） */
    private static final Map<ResourceLocation, Integer> PARTICLE_FRAME_COUNTS = new HashMap<>();

    private static final long FADE_DURATION = 2000L;

    private static long openTime = 0L;
    private static boolean isActive = false;

    private static final List<MenuParticle> particles = new ArrayList<>();
    private static final List<MouseTrailParticle> mouseTrail = new ArrayList<>();
    /** 动画贴图缓存 <贴图路径, 动画数据> */
    private static final Map<ResourceLocation, AnimationData> ANIMATION_CACHE = new HashMap<>();

    private static Boolean hasBackgroundFile = null;
    private static Boolean hasLogoFile = null;

    // ====== 视差鼠标追踪 ======
    /** 鼠标归一化坐标 [-1, 1]，相对于屏幕中心，正方向为右下 */
    private static float mouseNormX = 0f;
    private static float mouseNormY = 0f;
    /** 平滑目标值（用于插值） */
    private static float targetNormX = 0f;
    private static float targetNormY = 0f;
    /** 平滑系数（越小越平滑） */
    private static final float SMOOTH_FACTOR = 0.35f;
    /** 帧时间追踪（用于帧率无关插值） */
    private static long lastFrameTime = 0L;

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen) {
            if (!PotionEnchantConfig.CLIENT.enableCustomMainMenu.get()) return;
            isActive = true;
            openTime = System.currentTimeMillis();
            particles.clear();
            mouseTrail.clear();
            hasBackgroundFile = null;
            hasLogoFile = null;
            mouseNormX = 0f;
            mouseNormY = 0f;
            initTextures();
            for (int i = 0; i < 15; i++) particles.add(new MenuParticle());
            PotionEnchantMod.LOGGER.info("[CustomMainMenu] enabled [bg] [logo] textures:{}", ITEM_TEXTURES.size());
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof TitleScreen && isActive) {
            isActive = false;
            particles.clear();
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed event) {
        if (!(event.getScreen() instanceof TitleScreen) || !isActive) return;
        if (!PotionEnchantConfig.CLIENT.enableCustomMainMenu.get()) return;
        int count = PotionEnchantConfig.CLIENT.mouseTrailClickCount.get();
        if (count <= 0) return;
        double mx = event.getMouseX();
        double my = event.getMouseY();
        for (int i = 0; i < count; i++) {
            mouseTrail.add(new MouseTrailParticle((int)mx, (int)my, 0, 0, true));
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render event) {
        if (!(event.getScreen() instanceof TitleScreen) || !isActive) return;
        if (event instanceof net.minecraftforge.client.event.ScreenEvent.Render.Post) return;
        if (!PotionEnchantConfig.CLIENT.enableCustomMainMenu.get()) {
            if (isActive) { isActive = false; particles.clear(); }
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        float fadeAlpha = Mth.clamp((System.currentTimeMillis() - openTime) / (float)FADE_DURATION, 0.0f, 1.0f);
        int w = event.getScreen().width, h = event.getScreen().height;
        // 更新鼠标目标值（帧率无关平滑插值）
        if (PotionEnchantConfig.CLIENT.enableMenuParallax.get()) {
            long now = System.nanoTime();
            if (lastFrameTime == 0L) lastFrameTime = now;
            float deltaSec = (now - lastFrameTime) / 1_000_000_000f;
            lastFrameTime = now;
            targetNormX = Mth.clamp((float)(event.getMouseX() - w / 2.0) / (w / 2.0f), -1.0f, 1.0f);
            targetNormY = Mth.clamp((float)(event.getMouseY() - h / 2.0) / (h / 2.0f), -1.0f, 1.0f);
            float rate = 6.0f;
            float t = 1.0f - (float)Math.exp(-rate * deltaSec);
            t = Mth.clamp(t, 0.0f, 0.9f);
            mouseNormX += (targetNormX - mouseNormX) * t;
            mouseNormY += (targetNormY - mouseNormY) * t;
        } else {
            lastFrameTime = 0L;
            mouseNormX = 0f;
            mouseNormY = 0f;
        }
        if (checkBackgroundExists()) {
            g.fill(0, 0, w, h, 0xFF000000);
            drawCustomBackground(g, w, h);
            // 小按钮/图标按钮完整渲染
            drawButtonTextAndIcons(g, event.getScreen(), event.getMouseX(), event.getMouseY());
            // 大按钮阴影+文本（合并到同一循环确保文本在阴影之上）
            drawButtonShadowsAndText(g, event.getScreen(), event.getMouseX(), event.getMouseY());
            // Forge版本信息（左下角）
            drawFogGradient(g, w, h);
            drawForgeBranding(g);
        }
        if (checkLogoExists()) drawCustomLogo(g, w, h, fadeAlpha);
        updateAndDrawParticles(g, fadeAlpha);
        updateMouseTrail(g, fadeAlpha, event.getMouseX(), event.getMouseY());
    }

    // ====== 贴图池初始化 ======

    private static void initTextures() {
        if (ITEM_TEXTURES != null) return;
        ITEM_TEXTURES = new ArrayList<>();

        // 直接从 model JSON 中的 layer0 路径构造 ResourceLocation（跳过 cosmic loader）
        // 神秘空瓶 / 万能药水附魔瓶 / 终极药水护符 / 万能附魔书
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/mysterious_empty_bottle.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/universal_potion_bottle.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/ultimate_potion_amulet.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/universal_enchantment_book.png"));
        // X 工具
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/tool/swordx.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/tool/pickaxex.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/tool/axex.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/tool/shovelx.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/tool/hoex.png"));
        // X 护甲
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/armor/armorx_leggings.png"));
        ITEM_TEXTURES.add(new ResourceLocation("potionenchant", "textures/item/armor/armorx_boots.png"));
        // 额外物品
        // 粒子贴图（直接扫描 textures/particle/，和 X 系列物品粒子一样的逻辑）
        if (PARTICLE_TEXTURES == null) {
            PARTICLE_TEXTURES = new ArrayList<>();
            PARTICLE_FRAME_COUNTS.clear();
            try {
                var resMgr = Minecraft.getInstance().getResourceManager();
                var resources = resMgr.listResources("textures/particle",
                    s -> s.getPath().endsWith(".png"));
                for (var entry : resources.entrySet()) {
                    var loc = entry.getKey();
                    if ("potionenchant".equals(loc.getNamespace())) {
                        PARTICLE_TEXTURES.add(loc);
                        // 自动检测纹理帧数（从高度/宽度，不依赖 mcmeta）
                        int fc = 1;
                        try {
                            var res = resMgr.getResource(loc);
                            if (res.isPresent()) {
                                try (var img = com.mojang.blaze3d.platform.NativeImage.read(res.get().open())) {
                                    int w = img.getWidth();
                                    int h = img.getHeight();
                                    if (h > w && w > 0) fc = h / w;
                                }
                            }
                        } catch (Exception ignored) {}
                        PARTICLE_FRAME_COUNTS.put(loc, fc);
                    }
                }
            } catch (Exception e) {
                PotionEnchantMod.LOGGER.warn("[CustomMainMenu] failed to scan particle textures", e);
            }
            PotionEnchantMod.LOGGER.info("[CustomMainMenu] loaded {} particle textures", PARTICLE_TEXTURES.size());
        }
        PotionEnchantMod.LOGGER.info("[CustomMainMenu] initialized {} item textures", ITEM_TEXTURES.size());
    }

    // ====== 文件检查 ======

    private static boolean checkBackgroundExists() {
        if (hasBackgroundFile != null) return hasBackgroundFile;
        try {
            var mc = Minecraft.getInstance();
            hasBackgroundFile = mc.getResourceManager().getResource(CUSTOM_BG).isPresent();
        } catch (Exception e) {
            hasBackgroundFile = false;
        }
        return hasBackgroundFile;
    }

    private static boolean checkLogoExists() {
        if (hasLogoFile != null) return hasLogoFile;
        try {
            var mc = Minecraft.getInstance();
            hasLogoFile = mc.getResourceManager().getResource(CUSTOM_LOGO).isPresent();
        } catch (Exception e) {
            hasLogoFile = false;
        }
        return hasLogoFile;
    }

    // ====== 背景绘制（含视差偏移） ======

    private static void drawButtonTextAndIcons(GuiGraphics g, Screen s, int mx, int my) {
        var font = Minecraft.getInstance().font;
        for (var r : s.renderables) {
            if (!(r instanceof net.minecraft.client.gui.components.AbstractWidget w)) continue;
            int x = w.getX(), y = w.getY(), bw = w.getWidth(), bh = w.getHeight();
            // 小按钮（≤40px，如无障碍图标）或ImageButton → 完整渲染保留图案
            if ((bw <= 40 || bh <= 15) || r instanceof net.minecraft.client.gui.components.ImageButton) {
                w.render(g, mx, my, 0f);
            }
        }
    }

    // ====== Forge 品牌信息 ======

        private static void drawForgeBranding(GuiGraphics g) {
        try {
            var mc = Minecraft.getInstance();
            var font = mc.font;
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            // 左下角：Forge 版本信息 + 模组数量（匹配 TitleScreen.render 的计算方式）
            net.minecraftforge.internal.BrandingControl.forEachLine(true, true, (brdline, brd) -> {
                g.drawString(font, brd, 2, sh - (10 + brdline * (font.lineHeight + 1)), 0xFFFFFFFF);
            });
            // 右下角：状态行（版权信息上方，匹配 TitleScreen.render）
            net.minecraftforge.internal.BrandingControl.forEachAboveCopyrightLine((brdline, brd) -> {
                int x = sw - font.width(brd);
                g.drawString(font, brd, x, sh - (10 + (brdline + 1) * (font.lineHeight + 1)), 0xFFFFFFFF);
            });
        } catch (Exception e) {
            // Forge branding not available
        }
    }

    private static void drawCustomBackground(GuiGraphics g, int w, int h) {
        RenderSystem.enableBlend();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (PotionEnchantConfig.CLIENT.enableMenuParallax.get()) {
            int maxOff = PotionEnchantConfig.CLIENT.menuParallaxMaxOffset.get();
            // 每个方向独立限制偏移量，[-maxOff, +maxOff]
            float offX = mouseNormX * maxOff;
            float offY = mouseNormY * maxOff;
            // 将背景放大 2*maxOff 并反向偏移，产生视差效果
            // 画面外扩 + 移动，微小的拉伸在视觉上可忽略
            // 使用 10 参数 blit：dest 位置/大小 + src 区域/大小 + 纹理总尺寸
            // 使用 float UV blit（无整数截断，子像素平滑）
            g.blit(CUSTOM_BG, 0, 0, maxOff + offX, maxOff + offY, w, h, w + 2*maxOff, h + 2*maxOff);
        } else {
            g.blit(CUSTOM_BG, 0, 0, 0, 0, w, h, w, h);
        }

        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    // ====== 边缘白色雾化渐变 ======

    private static void drawFogGradient(GuiGraphics g, int w, int h) {
        if (!PotionEnchantConfig.CLIENT.enableMenuVignette.get()) return;

        int rangePct = PotionEnchantConfig.CLIENT.menuFogRange.get();
        int fogH = h * rangePct / 100;
        int fogW = w * rangePct / 100;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int edgeColor = 0xBBFFFFFF;
        int centerColor = 0x00FFFFFF;

        // 上下边缘
        g.fillGradient(0, 0, w, fogH, edgeColor, centerColor);
        g.fillGradient(0, h - fogH, w, h, centerColor, edgeColor);

        // 左右边缘（旋转 PoseStack）
        var pose = g.pose();

        pose.pushPose();
        pose.translate(w / 2.0F, h / 2.0F, 0.0F);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
        g.fillGradient(-h / 2, w / 2 - fogW, h / 2, w / 2, centerColor, edgeColor);
        pose.popPose();

        pose.pushPose();
        pose.translate(w / 2.0F, h / 2.0F, 0.0F);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
        g.fillGradient(-h / 2, -w / 2, h / 2, -w / 2 + fogW, edgeColor, centerColor);
        pose.popPose();

        RenderSystem.disableBlend();
    }

    // ====== 边缘白色雾化渐变 ======




    // ====== Logo 绘制 ======



    // ====== Logo 绘制 ======

    
    // ====== 底部信息区域（Forge 信息 + 版权，防止残留） ======

    private static void drawCustomLogo(GuiGraphics g, int sw, int h, float a) {
        RenderSystem.enableBlend();
        g.setColor(1.0F, 1.0F, 1.0F, a);
        g.blit(CUSTOM_LOGO, sw/2 - 128, 30, 0, 0, 256, 44, 256, 44);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    // ====== 粒子更新与绘制 ======

    private static long lastParticleTickTime = 0L;
    private static float spawnAccumulator = 0f;

    private static void updateAndDrawParticles(GuiGraphics g, float fadeAlpha) {
        // 帧率无关的 delta-time 计算
        long now = System.nanoTime();
        if (lastParticleTickTime == 0L) lastParticleTickTime = now;
        float deltaSec = (now - lastParticleTickTime) / 1_000_000_000f;
        lastParticleTickTime = now;
        if (deltaSec > 0.1f) deltaSec = 0.1f;

        // 时间驱动的生成率
        spawnAccumulator += PotionEnchantConfig.CLIENT.particleSpawnRate.get() * deltaSec;
        while (spawnAccumulator >= 1.0f) {
            particles.add(new MenuParticle());
            spawnAccumulator -= 1.0f;
        }
        while (particles.size() > PotionEnchantConfig.CLIENT.particleMaxCount.get()) particles.remove(0);

        g.flush();
        RenderSystem.disableDepthTest();
        PoseStack pose = g.pose();
        pose.pushPose();

        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        List<MenuParticle> rem = new ArrayList<>();
        for (MenuParticle p : particles) {
            p.tick(deltaSec * 1000f);
            if (p.isDead()) { rem.add(p); continue; }
            p.draw(pose, bufferSource, fadeAlpha);
        }
        particles.removeAll(rem);

        pose.popPose();
        RenderSystem.enableDepthTest();
        g.flush();
    }
    // ====== 鼠标拖尾（动量追踪） ======

    private static long lastMouseTrailTime = 0L;
    private static int prevMouseX = 0, prevMouseY = 0;
    private static float mouseVelX = 0f, mouseVelY = 0f;
    private static long lastMouseUpdateTime = 0L;

    private static void updateMouseTrail(GuiGraphics g, float fadeAlpha, int mx, int my) {
        g.flush();
        PoseStack pose = g.pose();
        var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        long now = System.currentTimeMillis();

        // 计算鼠标速度（像素/秒），指数平滑
        if (lastMouseUpdateTime == 0L) lastMouseUpdateTime = now;
        float dtSec = (now - lastMouseUpdateTime) / 1000f;
        if (dtSec > 0.5f) dtSec = 0.5f;
        if (dtSec > 0.001f) {
            float rawVx = (mx - prevMouseX) / dtSec;
            float rawVy = (my - prevMouseY) / dtSec;
            float smooth = 0.3f;
            mouseVelX = mouseVelX * (1 - smooth) + rawVx * smooth;
            mouseVelY = mouseVelY * (1 - smooth) + rawVy * smooth;
        }
        prevMouseX = mx;
        prevMouseY = my;
        lastMouseUpdateTime = now;

        // 只在鼠标移动时生成拖尾
        float mouseSpeed = (float)Math.sqrt(mouseVelX * mouseVelX + mouseVelY * mouseVelY);
        int interval = PotionEnchantConfig.CLIENT.mouseTrailSpawnInterval.get();
        if (mouseSpeed > 20 && now - lastMouseTrailTime >= interval) {
            mouseTrail.add(new MouseTrailParticle(mx, my, mouseVelX, mouseVelY, false));
            lastMouseTrailTime = now;
        }

        // 更新粒子位置
        float deltaMs = dtSec * 1000f;
        for (var p : mouseTrail) {
            p.tick(deltaMs);
        }
        mouseTrail.removeIf(MouseTrailParticle::isDead);

        for (var p : mouseTrail) {
            p.draw(pose, bufferSource, fadeAlpha);
        }

        g.flush();
    }

    private static class MouseTrailParticle {
        final ResourceLocation tex;
        final int tintColor;
        final long bornTime = System.currentTimeMillis();
        final int lifetimeMs;
        float x, y;
        float vx, vy;
        final float size;
        final float xRot, yRot, zRot;

        MouseTrailParticle(int mx, int my, float mouseVx, float mouseVy, boolean isFirework) {
            if (!PARTICLE_TEXTURES.isEmpty()) {
                this.tex = PARTICLE_TEXTURES.get(RANDOM.nextInt(PARTICLE_TEXTURES.size()));
            } else {
                this.tex = ITEM_TEXTURES.get(RANDOM.nextInt(ITEM_TEXTURES.size()));
            }
            this.tintColor = RANDOM.nextInt(0xFFFFFF);
            this.x = mx;
            this.y = my;
            float spread;
            if (isFirework) {
                // 烟花模式：全方向均匀扩散
                double a = Math.toRadians(RANDOM.nextInt(360));
                spread = RANDOM.nextFloat() * 150 + 80;
                this.vx = (float)Math.cos(a) * spread;
                this.vy = (float)Math.sin(a) * spread;
            } else {
                // 拖尾模式：鼠标速度方向 + 随机扩散
                float speed = (float)Math.sqrt(mouseVx * mouseVx + mouseVy * mouseVy);
                spread = RANDOM.nextFloat() * 80 + 20;
                float dirAngle = (float)Math.atan2(mouseVy, mouseVx);
                if (speed > 20) {
                    dirAngle += (RANDOM.nextFloat() - 0.5f) * 1.2f;
                    this.vx = (float)Math.cos(dirAngle) * (spread + speed * 0.2f);
                    this.vy = (float)Math.sin(dirAngle) * (spread + speed * 0.2f);
                } else {
                    double a = Math.toRadians(RANDOM.nextInt(360));
                    this.vx = (float)Math.cos(a) * spread;
                    this.vy = (float)Math.sin(a) * spread;
                }
            }
            this.lifetimeMs = PotionEnchantConfig.CLIENT.mouseTrailLifetime.get() + RANDOM.nextInt(-100, 101);
            int bs = PotionEnchantConfig.CLIENT.mouseTrailSize.get();
            this.size = RANDOM.nextFloat() * 8 + bs;
            this.xRot = RANDOM.nextFloat() * 360 - 180;
            this.yRot = RANDOM.nextFloat() * 360 - 180;
            this.zRot = RANDOM.nextFloat() * 360 - 180;
        }

        void tick(float deltaMs) {
            float dt = deltaMs / 1000f;
            x += vx * dt;
            y += vy * dt;
            float friction = 0.92f;
            float f = (float)Math.pow(friction, dt * 60f);
            vx *= f;
            vy *= f;
        }

        boolean isDead() {
            return System.currentTimeMillis() - bornTime >= lifetimeMs;
        }

        void draw(PoseStack pose, MultiBufferSource bufferSource, float ga) {
            float lifeFrac = (float)(System.currentTimeMillis() - bornTime) / lifetimeMs;
            if (lifeFrac >= 1) return;

            // 淡出延迟：在生命周期前 fadeDelay 百分比内保持全透明，之后线性淡出
            int fadeDelayPct = PotionEnchantConfig.CLIENT.mouseTrailFadeDelay.get();
            float fadeStart = fadeDelayPct / 100f;
            float fadeAlpha;
            if (fadeDelayPct >= 100) {
                fadeAlpha = 1.0f; // 永不淡出
            } else if (lifeFrac < fadeStart) {
                fadeAlpha = 1.0f;
            } else {
                fadeAlpha = 1.0f - (lifeFrac - fadeStart) / (1.0f - fadeStart);
            }
            float alpha = fadeAlpha * 0.7f * ga;
            int a = Mth.clamp((int)(alpha * 255), 0, 255);
            if (a <= 1) return;

            float boost = 1.8f;
            int r = Math.min(255, (int)(FastColor.ARGB32.red(tintColor) * boost));
            int g = Math.min(255, (int)(FastColor.ARGB32.green(tintColor) * boost));
            int b = Math.min(255, (int)(FastColor.ARGB32.blue(tintColor) * boost));

            pose.pushPose();
            pose.translate(x, y, 400);

            float time = (System.currentTimeMillis() % 10000) / 1000f;
            pose.mulPose(Axis.XP.rotationDegrees(xRot + time * 30));
            pose.mulPose(Axis.YP.rotationDegrees(yRot + time * 20));
            pose.mulPose(Axis.ZP.rotationDegrees(zRot + time * 40));

            pose.scale(size * 0.3f, size * 0.3f, 1);

            if (tex != null) {
                var type = PolygonRenderer.RenderTypes.additiveEntityTranslucent(tex);
                var consumer = bufferSource.getBuffer(type);
                var matrix = pose.last().pose();
                var normal = pose.last().normal();

                float vEnd = 1.0f / PARTICLE_FRAME_COUNTS.getOrDefault(tex, 1);
                consumer.vertex(matrix, -0.5f, -0.5f, 0).color(r, g, b, a).uv(0, 0).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                consumer.vertex(matrix,  0.5f, -0.5f, 0).color(r, g, b, a).uv(1, 0).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                consumer.vertex(matrix,  0.5f,  0.5f, 0).color(r, g, b, a).uv(1, vEnd).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                consumer.vertex(matrix, -0.5f,  0.5f, 0).color(r, g, b, a).uv(0, vEnd).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
            }

            pose.popPose();
        }
    }

    // ====== 按钮阴影 ======

    private static void drawButtonShadowsAndText(GuiGraphics g, Screen s, int mx, int my) {
        var font = Minecraft.getInstance().font;
        for (var r : s.renderables) {
            if (!(r instanceof net.minecraft.client.gui.components.AbstractWidget w)) continue;
            int x = w.getX(), y = w.getY(), bw = w.getWidth(), bh = w.getHeight();
            if (bw < 10 || bh < 10) continue;
            if (bw <= 40 || r instanceof net.minecraft.client.gui.components.ImageButton) continue;
            // 阴影背景（GuiGraphics.fill自动管理混合）
            g.fill(x-2, y-2, x+bw+2, y+bh+2, 0x60000000);
            // 悬停发光边框
            if (mx >= x && mx <= x+bw && my >= y && my <= y+bh) {
                int c1 = 0xCCFFAA00, c2 = 0xAAFFDD44;
                g.fill(x-2, y-2, x-1, y+bh+2, c1); g.fill(x+bw+1, y-2, x+bw+2, y+bh+2, c1);
                g.fill(x-2, y-2, x+bw+2, y-1, c1); g.fill(x-2, y+bh+1, x+bw+2, y+bh+2, c1);
                g.fill(x-1, y-1, x, y+bh+1, c2);   g.fill(x+bw, y-1, x+bw+1, y+bh+1, c2);
                g.fill(x-1, y-1, x+bw+1, y, c2);   g.fill(x-1, y+bh, x+bw+1, y+bh+1, c2);
            }
            // 按钮文本（同一循环中绘制，确保在阴影之上）
            var msg = w.getMessage();
            if (msg != null && !msg.getString().isEmpty()) {
                int color = 0xFFFFFFFF;
                g.drawString(font, msg, x + (bw - font.width(msg)) / 2, y + (bh - 8) / 2, color);
            }
        }
    }

    // ====== 粒子类 ======

    // ====== 动态贴图动画支持（mcmeta） ======

    private static class AnimationData {
        final int frameCount;
        final int frametimeMs;
        final int[] frames;

        AnimationData(int frameCount, int frametimeMs, int[] frames) {
            this.frameCount = frameCount;
            this.frametimeMs = frametimeMs;
            this.frames = frames;
        }

        int getCurrentFrame() {
            if (frameCount <= 1) return 0;
            int animLen = (frames != null) ? frames.length : frameCount;
            long now = Util.getMillis();
            int frameIdx = (int)((now / frametimeMs) % animLen);
            return (frames != null) ? frames[frameIdx] : frameIdx;
        }
    }

    private static AnimationData loadAnimation(ResourceLocation tex) {
        if (ANIMATION_CACHE.containsKey(tex)) {
            return ANIMATION_CACHE.get(tex);
        }

        AnimationData data = null;
        try {
            ResourceLocation metaLoc = new ResourceLocation(tex.getNamespace(), tex.getPath() + ".mcmeta");
            var optRes = Minecraft.getInstance().getResourceManager().getResource(metaLoc);
            if (optRes.isPresent()) {
                var reader = new java.io.InputStreamReader(optRes.get().open());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                reader.close();

                if (json.has("animation")) {
                    JsonObject anim = json.getAsJsonObject("animation");
                    int frametime = anim.has("frametime") ? anim.get("frametime").getAsInt() : 1;
                    int frameCount = 1;
                    int[] frames = null;

                    if (anim.has("frames")) {
                        var ja = anim.getAsJsonArray("frames");
                        frames = new int[ja.size()];
                        int maxF = 0;
                        for (int i = 0; i < ja.size(); i++) {
                            frames[i] = ja.get(i).getAsInt();
                            if (frames[i] > maxF) maxF = frames[i];
                        }
                        frameCount = maxF + 1;
                    } else {
                        // 从图片尺寸推导帧数：高度 / 宽度（每帧为正方形）
                        var imgOpt = Minecraft.getInstance().getResourceManager().getResource(tex);
                        if (imgOpt.isPresent()) {
                            NativeImage img = NativeImage.read(imgOpt.get().open());
                            frameCount = img.getHeight() / img.getWidth();
                            img.close();
                        }
                    }

                    if (frameCount > 1) {
                        data = new AnimationData(frameCount, frametime * 50, frames);
                    }
                }
            }
        } catch (Exception ignored) {
            // 没有 mcmeta 或解析失败 = 静态贴图
        }

        if (data == null) {
            data = new AnimationData(1, 1, null);
        }
        ANIMATION_CACHE.put(tex, data);
        return data;
    }

    private static class MenuParticle {
        final ResourceLocation tex;
        final int tintColor;
        final AnimationData animData;
        final float xRot, yRot, zRot;

        float x, y;
        float spawnY;
        // 速度改为 像素/秒（构造时从 config 转换，*60 归一化到 60fps）
        final float sx_sec, sy_sec;
        float size;
        float angle, am;
        final long bornTime;

        MenuParticle() {
            if (!PARTICLE_TEXTURES.isEmpty()) {
                this.tex = PARTICLE_TEXTURES.get(RANDOM.nextInt(PARTICLE_TEXTURES.size()));
            } else {
                this.tex = ITEM_TEXTURES.get(RANDOM.nextInt(ITEM_TEXTURES.size()));
            }

            this.tintColor = RANDOM.nextInt(0xFFFFFF);
            this.animData = loadAnimation(tex);

            Minecraft mc = Minecraft.getInstance();
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();

            this.x = RANDOM.nextFloat() * sw;
            this.y = sh;
            this.spawnY = sh;
            float speedH = PotionEnchantConfig.CLIENT.particleSpeedH.get() / 100f * 60f;
            this.sx_sec = (RANDOM.nextFloat() - 0.5f) * 2 * speedH;
            float sv = PotionEnchantConfig.CLIENT.particleSpeedV.get() / 100f * 60f;
            float svs = PotionEnchantConfig.CLIENT.particleSpeedVSpread.get() / 100f * 60f;
            float dir = PotionEnchantConfig.CLIENT.particleGoUp.get() ? -1f : 1f;
            this.sy_sec = dir * (RANDOM.nextFloat() * svs + sv);
            int bs = PotionEnchantConfig.CLIENT.particleBaseSize.get();
            int ss = PotionEnchantConfig.CLIENT.particleSizeSpread.get();
            this.size = ss > 0 ? RANDOM.nextFloat() * ss + bs : bs;
            this.angle = RANDOM.nextFloat() * 360f;
            this.am = (RANDOM.nextFloat() - 0.5f) * 1.2f;
            this.xRot = RANDOM.nextFloat() * 360 - 180;
            this.yRot = RANDOM.nextFloat() * 360 - 180;
            this.zRot = RANDOM.nextFloat() * 360 - 180;
            this.bornTime = System.currentTimeMillis();
        }

        void tick(float deltaMs) {
            float dt = deltaMs / 1000f;
            x += sx_sec * dt;
            y += sy_sec * dt;
            angle += am * (dt * 60f);
        }

        boolean isDead() {
            int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            return y < -30 || y > sh + 30 || x < -30 || x > sw + 30;
        }

        float getAlpha(float yPos, int screenHeight) {
            // 出生淡入（前 particleFadeIn 帧 → 转换为毫秒）
            float fadeInFrames = PotionEnchantConfig.CLIENT.particleFadeIn.get();
            float elapsedMs = System.currentTimeMillis() - bornTime;
            float birthFade = Mth.clamp(elapsedMs / Math.max(fadeInFrames * 16.667f, 1f), 0, 1);
            // fadeYStart: 从出生点开始计算的淡出位置
            float pct = PotionEnchantConfig.CLIENT.particleFadeYStart.get() / 100f;
            boolean goUp = PotionEnchantConfig.CLIENT.particleGoUp.get();
            float totalDist = goUp ? spawnY : screenHeight - spawnY;
            float traveled = goUp ? spawnY - yPos : yPos - spawnY;
            float progress = totalDist > 0 ? Math.max(0, traveled / totalDist) : 1;
            float posAlpha = progress < pct ? 1 : 1.0f - (progress - pct) / Math.max(1 - pct, 0.01f);
            return Mth.clamp(posAlpha * birthFade, 0, 1);
        }

        void draw(PoseStack pose, MultiBufferSource bufferSource, float ga) {
            pose.pushPose();
            pose.translate(x, y, 500);

            float time = (System.currentTimeMillis() % 10000) / 1000f;
            pose.mulPose(Axis.XP.rotationDegrees(xRot + time * 30));
            pose.mulPose(Axis.YP.rotationDegrees(yRot + time * 20));
            pose.mulPose(Axis.ZP.rotationDegrees(angle + time * 40));

            float boost = 1.8f;
            int r = Math.min(255, (int)(FastColor.ARGB32.red(tintColor) * boost));
            int g = Math.min(255, (int)(FastColor.ARGB32.green(tintColor) * boost));
            int b = Math.min(255, (int)(FastColor.ARGB32.blue(tintColor) * boost));

            float yAlpha = getAlpha(y, Minecraft.getInstance().getWindow().getGuiScaledHeight()) * ga;
            int alpha = Mth.clamp((int)(yAlpha * 255), 0, 255);

            pose.scale(size * 0.5f, size * 0.5f, 1);

            if (tex != null) {
                var type = PolygonRenderer.RenderTypes.additiveEntityTranslucent(tex);
                var consumer = bufferSource.getBuffer(type);
                var matrix = pose.last().pose();
                var normal = pose.last().normal();

                float vEnd = 1.0f / PARTICLE_FRAME_COUNTS.getOrDefault(tex, 1);
                consumer.vertex(matrix, -0.5f, -0.5f, 0).color(r, g, b, alpha).uv(0, 0).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                consumer.vertex(matrix,  0.5f, -0.5f, 0).color(r, g, b, alpha).uv(1, 0).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                consumer.vertex(matrix,  0.5f,  0.5f, 0).color(r, g, b, alpha).uv(1, vEnd).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
                consumer.vertex(matrix, -0.5f,  0.5f, 0).color(r, g, b, alpha).uv(0, vEnd).overlayCoords(0).uv2(15728880).normal(normal, 0, 0, 1).endVertex();
            }

            pose.popPose();
        }
    }
}









