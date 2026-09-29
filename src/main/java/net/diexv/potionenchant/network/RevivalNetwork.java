package net.diexv.potionenchant.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 复活网络通道（server → client）。目前只有一个"维度伪装开关"消息，
 * 用于让客户端不要把真重生过程中的"中转维度"当成真正的维度切换（见 {@link ReviveSpoofMessage}）。
 */
public final class RevivalNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("potionenchant", "revival"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    static {
        CHANNEL.registerMessage(0, ReviveSpoofMessage.class,
                ReviveSpoofMessage::encode, ReviveSpoofMessage::decode,
                ReviveSpoofMessage::handle);
    }

    private RevivalNetwork() {
    }

    /** 在 mod 构造期调用以触发静态注册（Forge 通道注册必须在 registry 冻结前完成） */
    public static void ensureInit() {
        if (CHANNEL == null) throw new IllegalStateException("revival channel unavailable");
    }

    /** 向单个玩家发送伪装开关；失败只记日志，绝不影响复活流程 */
    public static void sendSpoof(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ReviveSpoofMessage(active));
    }
}
