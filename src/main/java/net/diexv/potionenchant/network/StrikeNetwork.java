package net.diexv.potionenchant.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 轨道轰击网络通道（server → client）。
 */
public final class StrikeNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("potionenchant", "strike"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    static {
        CHANNEL.registerMessage(0, CreeperTankStrikeMessage.class,
                CreeperTankStrikeMessage::encode, CreeperTankStrikeMessage::decode,
                CreeperTankStrikeMessage::handle);
    }

    private StrikeNetwork() {
    }

    /** 在 mod 构造期调用以触发静态注册（Forge 通道注册必须在 registry 冻结前完成） */
    public static void ensureInit() {
        // 仅触发 <clinit>（CHANNEL 静态字段 + registerMessage）
        if (CHANNEL == null) throw new IllegalStateException("strike channel unavailable");
    }

    /** 服务端广播（触发所有客户端特效） */
    public static void broadcastStrike(double x, double y, double z, int color) {
        CHANNEL.send(PacketDistributor.ALL.noArg(),
                new CreeperTankStrikeMessage(x, y, z, color));
    }
}
