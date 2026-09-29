package net.diexv.potionenchant.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 {@link ClientboundRespawnPacket} 的原始 {@code dataToKeep} 字节。
 * 原版只有 {@code shouldKeep(byte)} 而没有原始值读取器，我们重建这个包时需要原样带上它
 * （见 {@code ClientRespawnSpoofMixin}：把"中转维度"折叠成当前维度时不能丢掉保留标志）。
 */
@Mixin(ClientboundRespawnPacket.class)
public interface ClientboundRespawnPacketAccessor {

    @Accessor("dataToKeep")
    byte potionenchant$getDataToKeep();
}
