package draylar.tiered.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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

    public static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_PACKET = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Entry::attribute,
            ByteBufCodecs.DOUBLE, Entry::value,
            ByteBufCodecs.INT, Entry::operation,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Entry::slots,
            Entry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SpecialStatsComponent> PACKET_CODEC = StreamCodec.composite(
            ENTRY_PACKET.apply(ByteBufCodecs.list()), SpecialStatsComponent::specials,
            ENTRY_PACKET, SpecialStatsComponent::basic,
            SpecialStatsComponent::new
    );

    public record Entry(String attribute, double value, int operation, List<String> slots) {}
}
