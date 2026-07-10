package draylar.tiered.network.packet;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AttributePacket(List<String> attributeIds, List<String> attributeJsons) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AttributePacket> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "attribute_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttributePacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.attributeIds, FriendlyByteBuf::writeUtf);
        buf.writeCollection(value.attributeJsons, FriendlyByteBuf::writeUtf);
    }, buf -> new AttributePacket(buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
