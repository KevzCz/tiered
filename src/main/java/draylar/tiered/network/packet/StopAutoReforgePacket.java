package draylar.tiered.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StopAutoReforgePacket() implements CustomPayload {

    public static final CustomPayload.Id<StopAutoReforgePacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("tiered", "stop_auto_reforge_packet"));

    public static final PacketCodec<RegistryByteBuf, StopAutoReforgePacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> {},
            buf -> new StopAutoReforgePacket()
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
