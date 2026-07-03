package draylar.tiered.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ExtractSlotPacket(int slotIndex) implements CustomPayload {

    public static final CustomPayload.Id<ExtractSlotPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("tiered", "extract_slot"));

    public static final PacketCodec<RegistryByteBuf, ExtractSlotPacket> PACKET_CODEC = PacketCodec.of(
            (value, buf) -> buf.writeVarInt(value.slotIndex),
            buf -> new ExtractSlotPacket(buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
