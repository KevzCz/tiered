package draylar.tiered.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MousePositionPacket(int mouseX, int mouseY) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MousePositionPacket> PACKET_ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "mouse_position"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MousePositionPacket> PACKET_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, MousePositionPacket::mouseX,
            ByteBufCodecs.INT, MousePositionPacket::mouseY,
            MousePositionPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
