package draylar.tiered.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AutoRefillPacket(boolean enabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AutoRefillPacket> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "auto_refill_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutoRefillPacket> PACKET_CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeBoolean(value.enabled()),
            buf -> new AutoRefillPacket(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
