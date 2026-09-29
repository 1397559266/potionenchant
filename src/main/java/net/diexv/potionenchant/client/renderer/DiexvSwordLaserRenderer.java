package net.diexv.potionenchant.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.diexv.potionenchant.client.renderer.gl.PolygonRenderer;
import net.diexv.potionenchant.entity.DiexvSwordLaserSkill;
import net.diexv.potionenchant.entity.LaserBeamData;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * DiexvSword 激光光柱渲染（移植自 DiexvCreeperLaserRenderer，蓝白色系）。
 * - 服务端光束列表经网络包同步（setServerBeams），这里逐帧平滑推进渲染进度。
 * - 新光束发射瞬间在枪口生成扩散蓝色冲击环（冲击波）。
 * - 本地玩家蓄力时额外绘制朝准星方向的预瞄细光柱。
 */
@OnlyIn(Dist.CLIENT)
public final class DiexvSwordLaserRenderer {

    private static final List<LaserEntry> activeLasers = new ArrayList<>();
    private static final float BEAM_WIDTH = 0.12f;
    private static final float GLOW_WIDTH = 0.35f;
    private static final int CYLINDER_SEGMENTS = 8;

    // 激光路径余辉：光柱飞过的地方原地留下浅蓝残迹（比激光浅；先全亮 1 秒，再 1 秒淡出）
    private static final int TRAIL_HOLD_TICKS = 20;      // 停留全亮 1 秒
    private static final int TRAIL_FADE_TICKS = 20;      // 淡出 1 秒
    private static final float TRAIL_WIDTH = 0.08F;
    private static final float TRAIL_ALPHA = 0.4F;       // 全亮时透明度（比光柱浅）

    // 激光发射瞬间的蓝色冲击环
    private static final float RING_LIFETIME = 9.0F;
    private static final float RING_START_RADIUS = 0.5F;
    private static final float RING_END_RADIUS = 6.5F;
    private static final float RING_BAND_THICKNESS = 0.5F;
    private static final int RING_SEGMENTS = 36;
    private static final int RING_TUBE_SEGMENTS = 6;
    private static final float NEW_BEAM_PROGRESS = 60.0F;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final List<ShockRing> activeRings = new ArrayList<>();
    private static final Set<Integer> spawnedRingKeys = new HashSet<>();

    private DiexvSwordLaserRenderer() {}

    /** 服务端同步的光束列表直接驱动渲染（每 tick 一次） */
    public static void setServerBeams(List<LaserBeamData> list) {
        if (list == null || list.isEmpty()) {
            activeLasers.clear();
            activeRings.clear();
            spawnedRingKeys.clear();
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            seen.add(i);
            LaserBeamData b = list.get(i);
            Vec3 origin = new Vec3(b.originX, b.originY, b.originZ);
            Vec3 dir = new Vec3(b.dirX, b.dirY, b.dirZ).normalize();
            addOrUpdate(i, origin, dir, b.progress);
        }
        activeLasers.removeIf(e -> !seen.contains(e.beamIndex));
        activeRings.removeIf(r -> !seen.contains(r.beamIndex));
        spawnedRingKeys.removeIf(k -> !seen.contains(k));
    }

    private static void addOrUpdate(int beamIndex, Vec3 origin, Vec3 dir, float progress) {
        for (LaserEntry entry : activeLasers) {
            if (entry.beamIndex == beamIndex) {
                boolean identityChanged = origin.distanceToSqr(entry.origin) > 1.0E-4
                        || entry.dir.dot(dir) < 0.9999;
                entry.origin = origin;
                entry.dir = dir;
                entry.serverProgress = progress;
                if (identityChanged) {
                    spawnedRingKeys.remove(beamIndex);
                    spawnShockRing(beamIndex, origin, dir, progress);
                }
                return;
            }
        }
        activeLasers.add(new LaserEntry(beamIndex, origin, dir, progress));
        spawnShockRing(beamIndex, origin, dir, progress);
    }

    private static void spawnShockRing(int beamIndex, Vec3 origin, Vec3 dir, float progress) {
        if (progress > NEW_BEAM_PROGRESS) return;
        if (!spawnedRingKeys.add(beamIndex)) return;
        activeRings.removeIf(r -> r.beamIndex == beamIndex);
        activeRings.add(new ShockRing(beamIndex, origin, dir.normalize()));
    }

    public static boolean hasActive() {
        return !activeLasers.isEmpty() || !activeRings.isEmpty();
    }

    /** 渲染所有光柱/冲击环/余辉。view 为世界坐标视图矩阵（相机已变换）。 */
    public static void renderAll(PoseStack view, MultiBufferSource.BufferSource bufferSource, float partialTick) {
        VertexConsumer consumer = bufferSource.getBuffer(PolygonRenderer.RenderTypes.UNLIT_ADDITIVE);
        Matrix4f matrix = view.last().pose();
        float nowTicks = net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders.cosmicTimeTicks();

        if (!activeLasers.isEmpty() || !activeRings.isEmpty()) {
            // 冲击环：逐帧推进 age，渲染扩散的蓝色冲击环
            Iterator<ShockRing> ringIt = activeRings.iterator();
            while (ringIt.hasNext()) {
                ShockRing ring = ringIt.next();
                float rFrame = partialTick - ring.lastPartialTick;
                if (rFrame < 0.0f) rFrame += 1.0f;
                ring.lastPartialTick = partialTick;
                ring.age += rFrame;
                if (ring.age >= RING_LIFETIME) {
                    ringIt.remove();
                    continue;
                }
                renderShockRing(matrix, consumer, ring);
            }

            // 光柱 + 路径余辉（整条连续直线，无分段）
            for (LaserEntry entry : activeLasers) {
                float frameAdvance = partialTick - entry.lastPartialTick;
                if (frameAdvance < 0.0f) frameAdvance += 1.0f;
                entry.lastPartialTick = partialTick;
                float advance = (float) DiexvSwordLaserSkill.BEAM_SPEED * frameAdvance;
                entry.renderProgress = Math.min(entry.serverProgress, entry.renderProgress + advance);
                float progress = entry.renderProgress;
                double tail = Math.max(0.0, progress - DiexvSwordLaserSkill.BEAM_LENGTH);
                Vec3 start = entry.origin.add(entry.dir.scale(tail));
                Vec3 end = entry.origin.add(entry.dir.scale(progress));

                // 余辉：激光尾部之前扫过的路径原地残留（一条连续直线，从淡出终点到光柱尾）
                renderLaserTrail(matrix, consumer, entry, (float) tail, nowTicks);

                renderSingleBeam(matrix, consumer, start, end, BEAM_WIDTH, 1.0f, 0.75f, 0.88f, 1.0f);
                renderSingleBeam(matrix, consumer, start, end, GLOW_WIDTH, 0.5f, 0.35f, 0.55f, 1.0f);
            }
        }
    }

    /** 渲染单束激光的路径余辉：连续直线 [origin+dir*fadeEndP, origin+dir*tail]。
     *  位置 P 的残留年龄 = nowTicks - fireTime - P/BEAM_SPEED：
     *  - < 1 秒（P > fullStartP）：全亮 TRAIL_ALPHA
     *  - 1~2 秒（fadeEndP < P < fullStartP）：线性淡出到 0 */
    private static void renderLaserTrail(Matrix4f matrix, VertexConsumer consumer, LaserEntry entry,
                                         float tail, float nowTicks) {
        if (tail <= 0.0F) return;
        float speed = (float) DiexvSwordLaserSkill.BEAM_SPEED;
        float fullStartP = (nowTicks - entry.fireTime - TRAIL_HOLD_TICKS) * speed;              // 全亮起点（age=1s）
        float fadeEndP = (nowTicks - entry.fireTime - TRAIL_HOLD_TICKS - TRAIL_FADE_TICKS) * speed; // 淡出终点（age=2s）
        float visStart = Math.max(0.0F, fadeEndP);
        if (tail <= visStart) return;

        // 全亮段 [fullStartP, tail]
        float fullStart = Math.max(visStart, fullStartP);
        if (tail > fullStart) {
            Vec3 a = entry.origin.add(entry.dir.scale(fullStart));
            Vec3 b = entry.origin.add(entry.dir.scale(tail));
            renderSingleBeam(matrix, consumer, a, b, TRAIL_WIDTH, TRAIL_ALPHA, 0.45f, 0.65f, 0.95f);
        }
        // 淡出段 [visStart, fullStart]：分 4 小段 alpha 线性递减到 0（段间精确相接，无中断）
        if (fullStart > visStart) {
            Vec3 a = entry.origin.add(entry.dir.scale(visStart));
            Vec3 b = entry.origin.add(entry.dir.scale(fullStart));
            for (int i = 0; i < 4; i++) {
                float t0 = i / 4.0F;
                float t1 = (i + 1) / 4.0F;
                Vec3 p0 = a.add(b.subtract(a).scale(t0));
                Vec3 p1 = a.add(b.subtract(a).scale(t1));
                float alpha = TRAIL_ALPHA * (1.0F - t0);
                renderSingleBeam(matrix, consumer, p0, p1, TRAIL_WIDTH, alpha, 0.45f, 0.65f, 0.95f);
            }
        }
    }

    private static void renderSingleBeam(Matrix4f matrix, VertexConsumer consumer,
                                         Vec3 from, Vec3 to, float width, float alpha,
                                         float r, float g, float b) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 0.001) return;
        Vec3 dirNorm = dir.scale(1.0 / length);

        Vec3 perp1, perp2;
        if (Math.abs(dirNorm.y) < 0.95) {
            perp1 = new Vec3(-dirNorm.z, 0, dirNorm.x).normalize();
        } else {
            perp1 = new Vec3(1, 0, 0);
        }
        perp2 = dirNorm.cross(perp1).normalize();

        for (int i = 0; i < CYLINDER_SEGMENTS; i++) {
            double angle1 = (double) i * Math.PI * 2.0 / CYLINDER_SEGMENTS;
            double angle2 = (double) (i + 1) * Math.PI * 2.0 / CYLINDER_SEGMENTS;

            double cos1 = Math.cos(angle1), sin1 = Math.sin(angle1);
            double cos2 = Math.cos(angle2), sin2 = Math.sin(angle2);

            Vec3 o1 = perp1.scale(cos1 * width).add(perp2.scale(sin1 * width));
            Vec3 o2 = perp1.scale(cos2 * width).add(perp2.scale(sin2 * width));

            float endAlpha = alpha * 0.4f;

            consumer.vertex(matrix, (float)(from.x + o1.x), (float)(from.y + o1.y), (float)(from.z + o1.z))
                    .color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, (float)(from.x + o2.x), (float)(from.y + o2.y), (float)(from.z + o2.z))
                    .color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, (float)(to.x + o2.x), (float)(to.y + o2.y), (float)(to.z + o2.z))
                    .color(r, g, b, endAlpha).endVertex();
            consumer.vertex(matrix, (float)(to.x + o1.x), (float)(to.y + o1.y), (float)(to.z + o1.z))
                    .color(r, g, b, endAlpha).endVertex();
        }
    }

    /** 渲染扩散的蓝色冲击环：管状主环 + 外层辉光环 + 光线扭曲幻影环。 */
    private static void renderShockRing(Matrix4f matrix, VertexConsumer consumer, ShockRing ring) {
        float p = ring.age / RING_LIFETIME;
        if (p <= 0.0F || p >= 1.0F) return;
        float ease = 1.0F - (1.0F - p) * (1.0F - p);
        float radius = RING_START_RADIUS + (RING_END_RADIUS - RING_START_RADIUS) * ease;
        float aIn = Mth.clamp(p / 0.25F, 0.0F, 1.0F);
        float aOut = 1.0F - Mth.clamp((p - 0.4F) / 0.6F, 0.0F, 1.0F);
        aOut = aOut * aOut * (3.0F - 2.0F * aOut);
        float alpha = aIn * aOut;
        if (alpha <= 0.01F) return;
        // 颜色：深蓝 -> 浅蓝白
        float r = Mth.lerp(p, 0.45F, 0.8F);
        float g = Mth.lerp(p, 0.65F, 0.92F);
        float b = 1.0F;

        Vec3 dir = ring.dir;
        Vec3 u, v;
        if (Math.abs(dir.y) < 0.95) {
            u = new Vec3(-dir.z, 0.0, dir.x).normalize();
        } else {
            u = new Vec3(1.0, 0.0, 0.0);
        }
        v = dir.cross(u).normalize();
        Vec3 center = ring.origin;

        float tube = RING_BAND_THICKNESS * 0.5F;
        renderRingTube(matrix, consumer, center, u, v, dir, radius, tube, r, g, b, alpha * 0.9F);
        renderRingTube(matrix, consumer, center, u, v, dir, radius + 0.08F, tube * 1.9F,
                r * 0.8F, g * 0.9F, b, alpha * 0.35F);

        // 光线扭曲幻影环
        float waveBase = radius * 1.15F;
        float waveA = alpha * 0.2F;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a1 = (float) i * TWO_PI / RING_SEGMENTS;
            float a2 = (float) (i + 1) * TWO_PI / RING_SEGMENTS;
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
            float w1 = waveBase * (1.0F + 0.07F * (float) Math.sin(a1 * 3.0F + ring.age * 0.5F));
            float w2 = waveBase * (1.0F + 0.07F * (float) Math.sin(a2 * 3.0F + ring.age * 0.5F));
            float z1 = 0.12F * (float) Math.sin(a1 * 2.0F + ring.age * 0.7F);
            float z2 = 0.12F * (float) Math.sin(a2 * 2.0F + ring.age * 0.7F);
            float rad1x = (float) (u.x * c1 + v.x * s1), rad1y = (float) (u.y * c1 + v.y * s1), rad1z = (float) (u.z * c1 + v.z * s1);
            float rad2x = (float) (u.x * c2 + v.x * s2), rad2y = (float) (u.y * c2 + v.y * s2), rad2z = (float) (u.z * c2 + v.z * s2);
            float t = 0.06F;
            consumer.vertex(matrix,
                    (float) (center.x + rad1x * (w1 - t) + dir.x * z1),
                    (float) (center.y + rad1y * (w1 - t) + dir.y * z1),
                    (float) (center.z + rad1z * (w1 - t) + dir.z * z1))
                    .color(0.6F, 0.8F, 1.0F, waveA).endVertex();
            consumer.vertex(matrix,
                    (float) (center.x + rad2x * (w2 - t) + dir.x * z2),
                    (float) (center.y + rad2y * (w2 - t) + dir.y * z2),
                    (float) (center.z + rad2z * (w2 - t) + dir.z * z2))
                    .color(0.6F, 0.8F, 1.0F, waveA).endVertex();
            consumer.vertex(matrix,
                    (float) (center.x + rad2x * (w2 + t) + dir.x * z2),
                    (float) (center.y + rad2y * (w2 + t) + dir.y * z2),
                    (float) (center.z + rad2z * (w2 + t) + dir.z * z2))
                    .color(0.6F, 0.8F, 1.0F, waveA).endVertex();
            consumer.vertex(matrix,
                    (float) (center.x + rad1x * (w1 + t) + dir.x * z1),
                    (float) (center.y + rad1y * (w1 + t) + dir.y * z1),
                    (float) (center.z + rad1z * (w1 + t) + dir.z * z1))
                    .color(0.6F, 0.8F, 1.0F, waveA).endVertex();
        }
    }

    /** 渲染一段管状圆环（torus）。 */
    private static void renderRingTube(Matrix4f matrix, VertexConsumer consumer, Vec3 center,
                                       Vec3 u, Vec3 v, Vec3 dir,
                                       float ringRadius, float tubeRadius,
                                       float r, float g, float b, float alpha) {
        if (alpha <= 0.01F) return;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a1 = (double) i * Math.PI * 2.0 / RING_SEGMENTS;
            double a2 = (double) (i + 1) * Math.PI * 2.0 / RING_SEGMENTS;
            for (int j = 0; j < RING_TUBE_SEGMENTS; j++) {
                double t1 = (double) j * Math.PI * 2.0 / RING_TUBE_SEGMENTS;
                double t2 = (double) (j + 1) * Math.PI * 2.0 / RING_TUBE_SEGMENTS;
                Vec3 p11 = torusVertex(center, u, v, dir, ringRadius, tubeRadius, a1, t1);
                Vec3 p12 = torusVertex(center, u, v, dir, ringRadius, tubeRadius, a1, t2);
                Vec3 p22 = torusVertex(center, u, v, dir, ringRadius, tubeRadius, a2, t2);
                Vec3 p21 = torusVertex(center, u, v, dir, ringRadius, tubeRadius, a2, t1);
                consumer.vertex(matrix, (float) p11.x, (float) p11.y, (float) p11.z).color(r, g, b, alpha).endVertex();
                consumer.vertex(matrix, (float) p12.x, (float) p12.y, (float) p12.z).color(r, g, b, alpha).endVertex();
                consumer.vertex(matrix, (float) p22.x, (float) p22.y, (float) p22.z).color(r, g, b, alpha).endVertex();
                consumer.vertex(matrix, (float) p21.x, (float) p21.y, (float) p21.z).color(r, g, b, alpha).endVertex();
            }
        }
    }

    private static Vec3 torusVertex(Vec3 center, Vec3 u, Vec3 v, Vec3 dir,
                                    double ringRadius, double tubeRadius,
                                    double ringAngle, double tubeAngle) {
        double rc = Math.cos(ringAngle), rs = Math.sin(ringAngle);
        double tc = Math.cos(tubeAngle), ts = Math.sin(tubeAngle);
        double rx = u.x * rc + v.x * rs;
        double ry = u.y * rc + v.y * rs;
        double rz = u.z * rc + v.z * rs;
        double ox = rx * tc + dir.x * ts;
        double oy = ry * tc + dir.y * ts;
        double oz = rz * tc + dir.z * ts;
        return new Vec3(center.x + rx * ringRadius + ox * tubeRadius,
                center.y + ry * ringRadius + oy * tubeRadius,
                center.z + rz * ringRadius + oz * tubeRadius);
    }

    private static class ShockRing {
        final int beamIndex;
        final Vec3 origin;
        final Vec3 dir;
        float age;
        float lastPartialTick;

        ShockRing(int beamIndex, Vec3 origin, Vec3 dir) {
            this.beamIndex = beamIndex;
            this.origin = origin;
            this.dir = dir;
            this.age = 0.0F;
            this.lastPartialTick = 0.0F;
        }
    }

    private static class LaserEntry {
        final int beamIndex;
        Vec3 origin;
        Vec3 dir;
        float serverProgress;
        float renderProgress;
        float fireTime;      // 发射时刻（统一墙钟 tick 等价单位，余辉年龄基准）
        float lastPartialTick;

        LaserEntry(int beamIndex, Vec3 origin, Vec3 dir, float progress) {
            this.beamIndex = beamIndex;
            this.origin = origin;
            this.dir = dir;
            this.serverProgress = progress;
            this.renderProgress = progress;
            this.fireTime = net.diexv.potionenchant.SkyRender.client.shader.AvaritiaShaders.cosmicTimeTicks();
            this.lastPartialTick = 0.0f;
        }
    }
}
