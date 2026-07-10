package draylar.tiered.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StopAutoReforgePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StopAutoReforgePacket> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "stop_auto_reforge_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StopAutoReforgePacket> PACKET_CODEC = StreamCodec.ofMember(
            (value, buf) -> {},
            buf -> new StopAutoReforgePacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
