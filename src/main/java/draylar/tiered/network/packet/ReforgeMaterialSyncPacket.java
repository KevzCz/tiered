package draylar.tiered.network.packet;

import java.util.List;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ReforgeMaterialSyncPacket(List<String> itemIds, List<String> materialJsons) implements CustomPayload {

    public static final CustomPayload.Id<ReforgeMaterialSyncPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("tiered", "reforge_material_sync"));

    public static final PacketCodec<RegistryByteBuf, ReforgeMaterialSyncPacket> PACKET_CODEC = PacketCodec.of((value, buf) -> {
        buf.writeCollection(value.itemIds, PacketByteBuf::writeString);
        buf.writeCollection(value.materialJsons, PacketByteBuf::writeString);
    }, buf -> new ReforgeMaterialSyncPacket(buf.readList(PacketByteBuf::readString), buf.readList(PacketByteBuf::readString)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
