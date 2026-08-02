package net.diexv.potionenchant.client.renderer.gl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class SnowflakeRenderer {

    private static final List<Snowflake> FLAKES = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static final int MAX_FLAKES = 30;

    private static final List<ResourceLocation> TEXTURES = new ArrayList<>();
    private static boolean texturesLoaded = false;

    private static final float SPAWN_Y_TOP = 1.5f;
    private static final float SPAWN_Y_RANGE = 0.5f;
    private static final float MELT_START_Y = -0.2f;
    private static final float MELT_END_Y = -0.7f;
    private static final float BASE_FALL_SPEED = 0.006f;

    private static long lastFrameTime = 0;

    private SnowflakeRenderer() {}

    public static void renderSnowflakes(PoseStack poseStack, MultiBufferSource buffers,
                                         int packedLight, int packedOverlay) {
        ensureTexturesLoaded();
        if (TEXTURES.isEmpty()) return;

        long now = Util.getMillis();
        boolean paused = Minecraft.getInstance().isPaused();
        if (paused) {
            now = lastFrameTime;
        }
        if (lastFrameTime == 0) lastFrameTime = now;
        float delta = (now - lastFrameTime) / 16.667f;
        lastFrameTime = now;
        if (delta > 5f) delta = 1f;

        while (FLAKES.size() < MAX_FLAKES) {
            FLAKES.add(new Snowflake(now));
        }

        Iterator<Snowflake> it = FLAKES.iterator();
        while (it.hasNext()) {
            Snowflake f = it.next();
            f.update(now, delta);
            if (f.life <= 0f) {
                it.remove();
            } else {
                f.render(poseStack, buffers, packedLight, packedOverlay, now);
            }
        }
    }

    private static void ensureTexturesLoaded() {
        if (texturesLoaded) return;
        texturesLoaded = true;
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            var resources = resourceManager.listResources("textures/particle",
                s -> s.getPath().endsWith(".png"));
            for (var entry : resources.entrySet()) {
                var loc = entry.getKey();
                if ("potionenchant".equals(loc.getNamespace())) {
                    TEXTURES.add(loc);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static class Snowflake {
        float x, y, z;
        float velX, velY;
        float wobblePhase;
        float wobbleAmp;
        float wobbleFreq;
        float life;
        float size;
        long spawnTime;
        int texIndex;
        float rotSpeed;
        float rotAngle;
        float flipSpeed;

        Snowflake(long now) {
            this.spawnTime = now;
            this.x = RANDOM.nextFloat() * 1.0f - 0.5f;
            this.y = SPAWN_Y_TOP + RANDOM.nextFloat() * SPAWN_Y_RANGE;
            this.z = RANDOM.nextFloat() * 0.5f - 0.25f;
            this.velX = (RANDOM.nextFloat() - 0.5f) * 0.002f;
            this.velY = BASE_FALL_SPEED * (0.6f + RANDOM.nextFloat() * 0.8f);
            this.wobblePhase = RANDOM.nextFloat() * Mth.PI * 2f;
            this.wobbleAmp = 0.003f + RANDOM.nextFloat() * 0.008f;
            this.wobbleFreq = 0.3f + RANDOM.nextFloat() * 0.7f;
            this.life = 1.0f;
            this.size = 0.04f + RANDOM.nextFloat() * 0.06f;
            this.texIndex = RANDOM.nextInt(TEXTURES.size());
            this.rotSpeed = (RANDOM.nextFloat() - 0.5f) * 2.5f;
            this.rotAngle = RANDOM.nextFloat() * Mth.PI * 2f;
            this.flipSpeed = 0.5f + RANDOM.nextFloat() * 2.0f;
        }

        void update(long now, float delta) {
            float elapsed = (now - spawnTime) / 1000f;
            float wobble = Mth.sin(elapsed * wobbleFreq * 3f + wobblePhase) * wobbleAmp;

            this.x += (velX + wobble) * delta;
            this.y -= velY * delta;
            this.z += Mth.cos(elapsed * wobbleFreq * 2.5f + wobblePhase + 1f) * wobbleAmp * 0.5f * delta;
            this.rotAngle += rotSpeed * delta * 0.05f;

            if (this.y < MELT_START_Y) {
                float meltFrac = (MELT_START_Y - this.y) / (MELT_START_Y - MELT_END_Y);
                this.life = 1.0f - Mth.clamp(meltFrac, 0f, 1f);
            }
        }

        void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay, long now) {
            if (life <= 0f) return;

            ResourceLocation tex = TEXTURES.get(texIndex);
            VertexConsumer cons = buffers.getBuffer(PolygonRenderer.RenderTypes.additiveEntityTranslucent(tex));
            Matrix4f mat = poseStack.last().pose();

            float alpha = life * 0.65f;
            int a = (int)(alpha * 255);
            float hs = size * 0.5f;

            float cosR = Mth.cos(rotAngle);
            float sinR = Mth.sin(rotAngle);

            float rx0 = -hs * cosR - (-hs) * sinR;
            float ry0 = -hs * sinR + (-hs) * cosR;
            float rx1 =  hs * cosR - (-hs) * sinR;
            float ry1 =  hs * sinR + (-hs) * cosR;
            float rx2 =  hs * cosR -   hs  * sinR;
            float ry2 =  hs * sinR +   hs  * cosR;
            float rx3 = -hs * cosR -   hs  * sinR;
            float ry3 = -hs * sinR +   hs  * cosR;

            float cx = x;
            float cy = y;
            float cz = z;

            float elapsed = (now - spawnTime) / 1000f;
            boolean flipped = Mth.sin(elapsed * flipSpeed) > 0;

            float u0 = flipped ? 1 : 0;
            float u1 = flipped ? 0 : 1;
            float v0 = 0;
            float v1 = 1;

            cons.vertex(mat, cx + rx0, cy + ry0, cz).color(220, 230, 255, a).uv(u0, v0).overlayCoords(packedOverlay).uv2(15728880).normal(0, 0, 1).endVertex();
            cons.vertex(mat, cx + rx1, cy + ry1, cz).color(220, 230, 255, a).uv(u1, v0).overlayCoords(packedOverlay).uv2(15728880).normal(0, 0, 1).endVertex();
            cons.vertex(mat, cx + rx2, cy + ry2, cz).color(220, 230, 255, a).uv(u1, v1).overlayCoords(packedOverlay).uv2(15728880).normal(0, 0, 1).endVertex();
            cons.vertex(mat, cx + rx3, cy + ry3, cz).color(220, 230, 255, a).uv(u0, v1).overlayCoords(packedOverlay).uv2(15728880).normal(0, 0, 1).endVertex();
        }
    }
}