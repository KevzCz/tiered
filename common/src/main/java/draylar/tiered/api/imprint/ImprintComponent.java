package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import draylar.tiered.util.ImprintSlots;

public record ImprintComponent(List<Slot> slots) {

    public static final ImprintComponent EMPTY = new ImprintComponent(List.of());

    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Entry::id),
            Codec.INT.optionalFieldOf("count", 1).forGetter(Entry::count),
            Codec.FLOAT.optionalFieldOf("value", 0f).forGetter(Entry::value),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("extra", Map.of()).forGetter(Entry::extraValues)
    ).apply(i, Entry::new));

    public static final Codec<Slot> SLOT_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("source", "").forGetter(Slot::sourceId),
            ENTRY_CODEC.listOf().fieldOf("imprints").forGetter(Slot::entries)
    ).apply(i, Slot::new));

    public static final Codec<ImprintComponent> CODEC = RecordCodecBuilder.create(i -> i.group(
            SLOT_CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(ImprintComponent::slots)
    ).apply(i, ImprintComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_PACKET = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Entry::id,
            ByteBufCodecs.INT, Entry::count,
            ByteBufCodecs.FLOAT, Entry::value,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT), Entry::extraValues,
            Entry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Slot> SLOT_PACKET = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Slot::sourceId,
            ENTRY_PACKET.apply(ByteBufCodecs.list()), Slot::entries,
            Slot::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ImprintComponent> PACKET_CODEC = StreamCodec.composite(
            SLOT_PACKET.apply(ByteBufCodecs.list()), ImprintComponent::slots,
            ImprintComponent::new
    );

    public List<Entry> entries() {
        List<Entry> flat = new ArrayList<>();
        for (Slot slot : slots) flat.addAll(slot.entries());
        return flat;
    }

    public boolean has(String id) {
        for (Slot slot : slots) {
            for (Entry e : slot.entries()) {
                if (e.id().equals(id)) return true;
            }
        }
        return false;
    }

    public int countOf(String id) {
        int total = 0;
        for (Slot slot : slots) {
            for (Entry e : slot.entries()) {
                if (e.id().equals(id)) total += e.count();
            }
        }
        return total;
    }

    public int stacksOf(String id) {
        int n = 0;
        for (Slot slot : slots) {
            for (Entry e : slot.entries()) {
                if (e.id().equals(id)) n++;
            }
        }
        return n;
    }

    public float valueOf(String id) {
        float total = 0f;
        for (Slot slot : slots) {
            for (Entry e : slot.entries()) {
                if (e.id().equals(id)) total += e.value();
            }
        }
        return total;
    }

    public float extraValueOf(String id, String key) {
        float total = 0f;
        for (Slot slot : slots) {
            for (Entry e : slot.entries()) {
                if (e.id().equals(id)) total += e.extra(key);
            }
        }
        return total;
    }

    public List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Slot slot : slots) {
            for (Entry e : slot.entries()) {
                if (!out.contains(e.id())) out.add(e.id());
            }
        }
        return out;
    }

    public int slotsUsed() {
        return slots.size();
    }

    public boolean isFull(int capacity) {
        return slots.size() >= capacity;
    }

    public boolean hasSource(String sourceId) {
        for (Slot slot : slots) {
            if (slot.sourceId().equals(sourceId)) return true;
        }
        return false;
    }

    public ImprintComponent withSlot(String sourceId, List<Entry> group, int capacity) {
        if (group == null || group.isEmpty()) return this;
        if (slots.size() >= capacity) return this;
        List<Slot> next = new ArrayList<>(slots);
        next.add(new Slot(sourceId == null ? "" : sourceId, List.copyOf(group)));
        return new ImprintComponent(next);
    }

    public ImprintComponent withoutSlot(int index) {
        if (index < 0 || index >= slots.size()) return this;
        List<Slot> next = new ArrayList<>(slots);
        next.remove(index);
        return new ImprintComponent(next);
    }

    public ImprintComponent withoutSource(String sourceId) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).sourceId().equals(sourceId)) return withoutSlot(i);
        }
        return this;
    }

    public record Slot(String sourceId, List<Entry> entries) {
    }

    public record Entry(String id, int count, float value, Map<String, Float> extraValues) {
        public Entry(String id, int count, float value) {
            this(id, count, value, Map.of());
        }

        public Entry(String id, float value) {
            this(id, 1, value, Map.of());
        }

        public float extra(String key) {
            Float v = extraValues.get(key);
            return v == null ? 0f : v;
        }
    }
}
