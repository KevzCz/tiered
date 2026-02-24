package draylar.tiered.network.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MousePositionPacket(int mouseX, int mouseY) implements CustomPayload {
    
    public static final CustomPayload.Id<MousePositionPacket> PACKET_ID = 
        new CustomPayload.Id<>(Identifier.of("tiered", "mouse_position"));
    
    public static final PacketCodec<RegistryByteBuf, MousePositionPacket> PACKET_CODEC = 
        PacketCodec.tuple(
            PacketCodecs.INTEGER, MousePositionPacket::mouseX,
            PacketCodecs.INTEGER, MousePositionPacket::mouseY,
            MousePositionPacket::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
