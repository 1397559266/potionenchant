package net.diexv.potionenchant.network;

import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.renderer.DiexvSwordLaserRenderer;
import net.diexv.potionenchant.entity.LaserBeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * DiexvSword 激光光束同步网络（服务端 -> 客户端）。
 */
public final class DiexvSwordLaserNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PotionEnchantMod.MODID, "diexv_sword_laser"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, SyncLaserBeamsPacket.class,
                SyncLaserBeamsPacket::encode,
                SyncLaserBeamsPacket::decode,
                SyncLaserBeamsPacket::handle);
        CHANNEL.registerMessage(1, SyncLaserChargePacket.class,
                SyncLaserChargePacket::encode,
                SyncLaserChargePacket::decode,
                SyncLaserChargePacket::handle);
    }

    /** 广播当前所有光束到所有客户端 */
    public static void broadcastBeams(List<LaserBeamData> beams) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncLaserBeamsPacket(beams));
    }

    /** 广播剑蓄力状态变化（0=开始蓄力 1=激光发射爆裂 2=冷却结束恢复），仅对指定玩家生效 */
    public static void broadcastChargeState(int type, java.util.UUID playerId) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncLaserChargePacket(type, playerId));
    }

    /** 剑蓄力动画状态同步（服务端 → 客户端）：驱动体素动画与激光蓄力/发射/冷却精确同步 */
    public static class SyncLaserChargePacket {
        public static final int CHARGE_START = 0;
        public static final int FIRE = 1;
        public static final int RESTORE = 2;

        public final int type;
        public final java.util.UUID playerId;

        public SyncLaserChargePacket(int type, java.util.UUID playerId) {
            this.type = type;
            this.playerId = playerId;
        }

        public static void encode(SyncLaserChargePacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.type);
            buf.writeUUID(msg.playerId);
        }

        public static SyncLaserChargePacket decode(FriendlyByteBuf buf) {
            return new SyncLaserChargePacket(buf.readInt(), buf.readUUID());
        }

        public static void handle(SyncLaserChargePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player == null || !msg.playerId.equals(mc.player.getUUID())) return;
                switch (msg.type) {
                    case CHARGE_START -> net.diexv.potionenchant.SkyRender.client.model.DiexvSwordVoxelMesh.forceCharge();
                    case FIRE -> net.diexv.potionenchant.SkyRender.client.model.DiexvSwordVoxelMesh.forceFire();
                    case RESTORE -> net.diexv.potionenchant.SkyRender.client.model.DiexvSwordVoxelMesh.forceRestore();
                }
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SyncLaserBeamsPacket {
        public final List<LaserBeamData> beams;

        public SyncLaserBeamsPacket(List<LaserBeamData> beams) {
            this.beams = beams;
        }

        public static void encode(SyncLaserBeamsPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.beams.size());
            for (LaserBeamData b : msg.beams) {
                buf.writeFloat(b.progress);
                buf.writeFloat(b.originX);
                buf.writeFloat(b.originY);
                buf.writeFloat(b.originZ);
                buf.writeFloat(b.dirX);
                buf.writeFloat(b.dirY);
                buf.writeFloat(b.dirZ);
            }
        }

        public static SyncLaserBeamsPacket decode(FriendlyByteBuf buf) {
            int n = buf.readInt();
            List<LaserBeamData> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new LaserBeamData(
                        buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readFloat(), buf.readFloat(), buf.readFloat()));
            }
            return new SyncLaserBeamsPacket(list);
        }

        public static void handle(SyncLaserBeamsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DiexvSwordLaserRenderer.setServerBeams(msg.beams)));
            ctx.get().setPacketHandled(true);
        }
    }

    private DiexvSwordLaserNetwork() {}
}
