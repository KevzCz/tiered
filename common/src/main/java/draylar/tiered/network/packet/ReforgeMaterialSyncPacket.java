package draylar.tiered.network.packet;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReforgeMaterialSyncPacket(List<String> itemIds, List<String> materialJsons) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReforgeMaterialSyncPacket> PACKET_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "reforge_material_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeMaterialSyncPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.itemIds, FriendlyByteBuf::writeUtf);
        buf.writeCollection(value.materialJsons, FriendlyByteBuf::writeUtf);
    }, buf -> new ReforgeMaterialSyncPacket(buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
