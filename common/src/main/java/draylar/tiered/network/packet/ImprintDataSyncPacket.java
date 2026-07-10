package draylar.tiered.network.packet;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ImprintDataSyncPacket(
        List<String> imprintIds, List<String> imprintJsons,
        List<String> effectIds, List<String> effectJsons,
        List<String> slotRuleJsons) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ImprintDataSyncPacket> PACKET_ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "imprint_data_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImprintDataSyncPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.imprintIds, FriendlyByteBuf::writeUtf);
        buf.writeCollection(value.imprintJsons, FriendlyByteBuf::writeUtf);
        buf.writeCollection(value.effectIds, FriendlyByteBuf::writeUtf);
        buf.writeCollection(value.effectJsons, FriendlyByteBuf::writeUtf);
        buf.writeCollection(value.slotRuleJsons, FriendlyByteBuf::writeUtf);
    }, buf -> new ImprintDataSyncPacket(
            buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(FriendlyByteBuf::readUtf), buf.readList(FriendlyByteBuf::readUtf),
            buf.readList(FriendlyByteBuf::readUtf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
