package net.diexv.potionenchant.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端侧"复活维度伪装"状态。
 *
 * <p>服务端在真重生开始前沿途发 {@code active=true}（{@code ReviveSpoofMessage}），
 * 客户端在接下来的**一个** respawn 包里把"中转维度"改写为自己当前的维度：
 * <pre>
 *   ClientPacketListener#handleRespawn:
 *     if (packet.dimension != level.dimension()) { 新建 ClientLevel + setScreen(ReceivingLevelScreen) }
 * </pre>
 * 改写后条件不成立 → **不切关卡、不弹维度加载界面、不重载区块**；玩家始终待在死亡维度的关卡里，
 * 我们随后发的血量/背包/坐标同步就能直接生效（真重生流程本身完全不变）。
 *
 * <p>安全约束：一次性（用掉即解除）+ 3 秒 TTL（服务端若中途异常没发解除包也不会残留），
 * 因此不会影响后续正常的维度切换（传送门、指令等）。
 */
@OnlyIn(Dist.CLIENT)
public final class RevivalDimensionSpoof {

    private static final long TTL_MS = 3000L;

    private static volatile boolean armed = false;
    private static volatile long expiresAt = 0L;

    /** 服务端消息入口（true = 本次真重生期间允许伪装一次） */
    public static void set(boolean active) {
        armed = active;
        expiresAt = active ? System.currentTimeMillis() + TTL_MS : 0L;
    }

    public static boolean isArmed() {
        if (!armed) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) {
            armed = false;   // TTL 兜底：过期自动解除
            return false;
        }
        return true;
    }

    /**
     * 决定这次 respawn 包用哪个维度：
     * <ul>
     *   <li>未武装 / 已过期 → 原样返回（正常维度切换不受影响）</li>
     *   <li>包里的维度 == 客户端当前维度 → 原样返回（本次复活的中转包已经处理完）</li>
     *   <li>不同 → 返回客户端当前维度（= 死亡维度），把中转维度"折叠"掉</li>
     * </ul>
     * 无论哪种情况都只用一次（用掉即解除武装）。
     */
    public static ResourceKey<Level> resolve(ResourceKey<Level> packetDimension, ResourceKey<Level> clientDimension) {
        if (!isArmed()) {
            return packetDimension;
        }
        armed = false;
        expiresAt = 0L;
        if (packetDimension == null || clientDimension == null || packetDimension.equals(clientDimension)) {
            return packetDimension;
        }
        return clientDimension;
    }

    private RevivalDimensionSpoof() {}
}
