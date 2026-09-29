package net.diexv.potionenchant.network;

import java.util.function.Supplier;
import net.diexv.potionenchant.PotionEnchantMod;
import net.diexv.potionenchant.client.renderer.orbital.CreeperTankStrikeRenderer;
import net.diexv.potionenchant.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 轨道轰击消息：服务端在药水罐满 + 下界之星右键时广播给所有客户端，
 * 客户端收到后触发轨道轰击渲染特效并播放 waring 音效。
 */
public class CreeperTankStrikeMessage {

    private final double x;
    private final double y;
    private final double z;
    private final int color;

    public CreeperTankStrikeMessage(double x, double y, double z, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public static void encode(CreeperTankStrikeMessage msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeInt(msg.color);
    }

    public static CreeperTankStrikeMessage decode(FriendlyByteBuf buf) {
        return new CreeperTankStrikeMessage(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    public static void handle(CreeperTankStrikeMessage msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> clientHandle(msg)));
        c.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void clientHandle(CreeperTankStrikeMessage msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        CreeperTankStrikeRenderer.trigger(new Vec3(msg.x, msg.y, msg.z), msg.color);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DIEXV_CREEPER_WARNING.get(), 1.0F, 1.0F));
    }
}
