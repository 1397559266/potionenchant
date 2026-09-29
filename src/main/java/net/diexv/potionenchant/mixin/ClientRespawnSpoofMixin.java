package net.diexv.potionenchant.mixin;

import net.diexv.potionenchant.client.RevivalDimensionSpoof;
import net.diexv.potionenchant.mixin.accessor.ClientboundRespawnPacketAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 真重生期间的"维度加载界面"消除（只作用于本模组复活药水触发的重生）。
 *
 * <p>原版 {@code ClientPacketListener#handleRespawn} 里唯一会切关卡 + 弹加载界面的判断是：
 * <pre>
 *   ResourceKey&lt;Level&gt; resourcekey = packet.getDimension();
 *   if (resourcekey != this.level.dimension()) {
 *       this.level = new ClientLevel(...);
 *       this.minecraft.setLevel(this.level);
 *       this.minecraft.setScreen(new ReceivingLevelScreen());   // ← 玩家看到的维度加载界面
 *   }
 * </pre>
 * 外维度死亡时，服务端 {@code PlayerList#respawn} 会先按"重生点/主世界"发一个 respawn 包，
 * 我们随后才把玩家拉回死亡维度 → 客户端就白切一次关卡、重载一次区块、闪一次加载界面。
 *
 * <p>这里在**方法入口替换包参数**：复活流程已武装（服务端 {@code ReviveSpoofMessage}）时，
 * 把这个"中转维度"改写成客户端当前维度（= 死亡维度）→ 上面的判断不成立 → 不切关卡、
 * 不弹加载界面、区块也不用重载；保留标志位（{@code dataToKeep}）等字段原样带上，
 * 因此客户端仍会重建 LocalPlayer、仍会触发 Forge 的 respawn 回调，真重生流程与语义完全不变。
 *
 * <p>只影响"武装期间的下一个 respawn 包"（一次性 + 3s TTL，见 {@link RevivalDimensionSpoof}），
 * 传送门/指令等正常维度切换不受影响。
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientRespawnSpoofMixin {

    @ModifyVariable(method = "handleRespawn", at = @At("HEAD"), argsOnly = true)
    private ClientboundRespawnPacket potionenchant$collapseTransientRespawnDimension(ClientboundRespawnPacket packet) {
        if (packet == null || !RevivalDimensionSpoof.isArmed()) {
            return packet;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return packet;
        }
        ResourceKey<Level> target = RevivalDimensionSpoof.resolve(packet.getDimension(), level.dimension());
        if (target == null || target.equals(packet.getDimension())) {
            return packet;   // 维度本来就一致：原样交给原版（正常处理）
        }
        byte keep = ((ClientboundRespawnPacketAccessor) (Object) packet).potionenchant$getDataToKeep();
        return new ClientboundRespawnPacket(
                packet.getDimensionType(),
                target,
                packet.getSeed(),
                packet.getPlayerGameType(),
                packet.getPreviousPlayerGameType(),
                packet.isDebug(),
                packet.isFlat(),
                keep,
                packet.getLastDeathLocation(),
                packet.getPortalCooldown());
    }
}
