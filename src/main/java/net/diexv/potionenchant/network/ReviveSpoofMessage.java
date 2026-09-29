package net.diexv.potionenchant.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 复活维度伪装开关（server → client，只在"真重生"流程里用）。
 *
 * 背景：外维度（下界/末地/模组维度）死亡时，原版 {@code PlayerList#respawn} 会先把新玩家建在
 * "重生点/主世界"，客户端 {@code ClientPacketListener#handleRespawn} 一看维度不同就
 * **切换 ClientLevel + 弹 ReceivingLevelScreen（维度加载界面）**，我们随后再按快照把他拉回死亡维度
 * → 客户端又切一次、区块重载一次。玩家看到的就是"维度加载界面闪一下 + 世界重载"。
 *
 * 做法：真重生开始前沿途发 {@code active=true}，客户端在**下一个** respawn 包里把"中转维度"
 * 改写成自己当前的维度（= 死亡维度）→ 不切关卡、不弹加载界面、不重载区块；
 * 流程结束发 {@code active=false}（客户端另有 TTL 兜底）。载荷只有 1 个 boolean。
 */
public class ReviveSpoofMessage {

    private final boolean active;

    public ReviveSpoofMessage(boolean active) {
        this.active = active;
    }

    public static void encode(ReviveSpoofMessage msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static ReviveSpoofMessage decode(FriendlyByteBuf buf) {
        return new ReviveSpoofMessage(buf.readBoolean());
    }

    public static void handle(ReviveSpoofMessage msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> clientHandle(msg)));
        c.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void clientHandle(ReviveSpoofMessage msg) {
        net.diexv.potionenchant.client.RevivalDimensionSpoof.set(msg.active);
    }
}
