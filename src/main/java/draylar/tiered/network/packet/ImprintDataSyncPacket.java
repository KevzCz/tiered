package draylar.tiered.network.packet;

import java.util.List;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ImprintDataSyncPacket(
        List<String> imprintIds, List<String> imprintJsons,
        List<String> effectIds, List<String> effectJsons,
        List<String> slotRuleJsons) implements CustomPayload {

    public static final CustomPayload.Id<ImprintDataSyncPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("tiered", "imprint_data_sync"));

    public static final PacketCodec<RegistryByteBuf, ImprintDataSyncPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeCollection(value.imprintIds, PacketByteBuf::writeString);
        buf.writeCollection(value.imprintJsons, PacketByteBuf::writeString);
        buf.writeCollection(value.effectIds, PacketByteBuf::writeString);
        buf.writeCollection(value.effectJsons, PacketByteBuf::writeString);
        buf.writeCollection(value.slotRuleJsons, PacketByteBuf::writeString);
    }, buf -> new ImprintDataSyncPacket(
            buf.readList(PacketByteBuf::readString), buf.readList(PacketByteBuf::readString),
            buf.readList(PacketByteBuf::readString), buf.readList(PacketByteBuf::readString),
            buf.readList(PacketByteBuf::readString)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
