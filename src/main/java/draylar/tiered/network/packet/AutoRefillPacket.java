package draylar.tiered.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AutoRefillPacket(boolean enabled) implements CustomPayload {

    public static final CustomPayload.Id<AutoRefillPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("tiered", "auto_refill_packet"));

    public static final PacketCodec<RegistryByteBuf, AutoRefillPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBoolean(value.enabled()),
            buf -> new AutoRefillPacket(buf.readBoolean())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
