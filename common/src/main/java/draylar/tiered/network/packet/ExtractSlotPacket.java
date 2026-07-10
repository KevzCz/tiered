package draylar.tiered.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ExtractSlotPacket(int slotIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ExtractSlotPacket> PACKET_ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "extract_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractSlotPacket> PACKET_CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeVarInt(value.slotIndex),
            buf -> new ExtractSlotPacket(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
