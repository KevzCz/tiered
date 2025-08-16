package draylar.tiered.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.List;

public record SpecialStatsComponent(List<Entry> specials, Entry basic) {
    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("attribute").forGetter(Entry::attribute),
            Codec.DOUBLE.fieldOf("value").forGetter(Entry::value),
            Codec.INT.fieldOf("operation").forGetter(Entry::operation),
            Codec.STRING.listOf().fieldOf("slots").forGetter(Entry::slots)
    ).apply(i, Entry::new));

    public static final Codec<SpecialStatsComponent> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().fieldOf("specials").forGetter(SpecialStatsComponent::specials),
            ENTRY_CODEC.fieldOf("basic").forGetter(SpecialStatsComponent::basic)
    ).apply(i, SpecialStatsComponent::new));

    public static final PacketCodec<RegistryByteBuf, Entry> ENTRY_PACKET = PacketCodec.tuple(
            PacketCodecs.STRING, Entry::attribute,
            PacketCodecs.DOUBLE, Entry::value,
            PacketCodecs.INTEGER, Entry::operation,
            PacketCodecs.STRING.collect(PacketCodecs.toList()), Entry::slots,
            Entry::new
    );

    public static final PacketCodec<RegistryByteBuf, SpecialStatsComponent> PACKET_CODEC = PacketCodec.tuple(
            ENTRY_PACKET.collect(PacketCodecs.toList()), SpecialStatsComponent::specials,
            ENTRY_PACKET, SpecialStatsComponent::basic,
            SpecialStatsComponent::new
    );

    public record Entry(String attribute, double value, int operation, List<String> slots) {}
}
