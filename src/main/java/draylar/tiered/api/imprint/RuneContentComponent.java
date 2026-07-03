package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import draylar.tiered.api.ReforgeMaterial;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import org.jetbrains.annotations.Nullable;

public record RuneContentComponent(boolean rolled, List<Entry> entries,
        List<String> rolledEffects, Map<String, Map<String, Float>> rolledEffectParams, RolledBias rolledBias) {

    public static final RuneContentComponent UNROLLED = new RuneContentComponent(false, List.of(), List.of(), Map.of(), null);

    public float getRolledParam(String effectId, String key, float defaultValue) {
        Map<String, Float> params = rolledEffectParams == null ? null : rolledEffectParams.get(effectId);
        if (params == null) return defaultValue;
        Float v = params.get(key);
        return v == null ? defaultValue : v;
    }

    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Entry::imprintId),
            Codec.FLOAT.fieldOf("value").forGetter(Entry::value),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("extra", Map.of()).forGetter(Entry::extraValues)
    ).apply(i, Entry::new));

    public static final PacketCodec<RegistryByteBuf, Entry> ENTRY_PACKET = PacketCodec.tuple(
            PacketCodecs.STRING, Entry::imprintId,
            PacketCodecs.FLOAT, Entry::value,
            PacketCodecs.map(HashMap::new, PacketCodecs.STRING, PacketCodecs.FLOAT), Entry::extraValues,
            Entry::new
    );

    public static final Codec<RolledBias> BIAS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("groups", List.of()).forGetter(b -> b.groups() == null ? List.of() : b.groups()),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("group_weight_multipliers", Map.of()).forGetter(b -> b.groupWeightMultipliers() == null ? Map.of() : b.groupWeightMultipliers()),
            Codec.FLOAT.optionalFieldOf("rarity_boost", 0f).forGetter(RolledBias::rarityBoost),
            Codec.STRING.optionalFieldOf("guaranteed_min_rarity", "").forGetter(b -> b.guaranteedMinRarity() == null ? "" : b.guaranteedMinRarity()),
            Codec.STRING.optionalFieldOf("max_rarity", "").forGetter(b -> b.maxRarity() == null ? "" : b.maxRarity())
    ).apply(i, (groups, gwm, rb, minR, maxR) -> new RolledBias(
            groups.isEmpty() ? null : groups,
            gwm.isEmpty() ? null : gwm,
            rb,
            minR.isEmpty() ? null : minR,
            maxR.isEmpty() ? null : maxR)));

    public static final PacketCodec<RegistryByteBuf, RolledBias> BIAS_PACKET = PacketCodec.of(
            (bias, buf) -> {
                List<String> g = bias.groups() == null ? List.of() : bias.groups();
                buf.writeInt(g.size());
                for (String s : g) buf.writeString(s);
                Map<String, Float> gwm = bias.groupWeightMultipliers() == null ? Map.of() : bias.groupWeightMultipliers();
                buf.writeInt(gwm.size());
                for (var e : gwm.entrySet()) { buf.writeString(e.getKey()); buf.writeFloat(e.getValue()); }
                buf.writeFloat(bias.rarityBoost());
                buf.writeString(bias.guaranteedMinRarity() == null ? "" : bias.guaranteedMinRarity());
                buf.writeString(bias.maxRarity() == null ? "" : bias.maxRarity());
            },
            buf -> {
                int gSize = buf.readInt();
                List<String> groups = new ArrayList<>(gSize);
                for (int i = 0; i < gSize; i++) groups.add(buf.readString());
                int gwmSize = buf.readInt();
                Map<String, Float> gwm = new LinkedHashMap<>(gwmSize);
                for (int i = 0; i < gwmSize; i++) gwm.put(buf.readString(), buf.readFloat());
                float rb = buf.readFloat();
                String minR = buf.readString();
                String maxR = buf.readString();
                return new RolledBias(
                        groups.isEmpty() ? null : groups,
                        gwm.isEmpty() ? null : gwm,
                        rb,
                        minR.isEmpty() ? null : minR,
                        maxR.isEmpty() ? null : maxR);
            });

    private static final Codec<Map<String, Float>> PARAM_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT);
    private static final Codec<Map<String, Map<String, Float>>> EFFECT_PARAMS_CODEC =
            Codec.unboundedMap(Codec.STRING, PARAM_MAP_CODEC);

    public static final Codec<RuneContentComponent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("rolled", false).forGetter(RuneContentComponent::rolled),
            ENTRY_CODEC.listOf().fieldOf("entries").forGetter(RuneContentComponent::entries),
            Codec.STRING.listOf().optionalFieldOf("rolled_effects", List.of()).forGetter(RuneContentComponent::rolledEffects),
            EFFECT_PARAMS_CODEC.optionalFieldOf("rolled_effect_params", Map.of()).forGetter(c -> c.rolledEffectParams() == null ? Map.of() : c.rolledEffectParams()),
            BIAS_CODEC.optionalFieldOf("rolled_bias").forGetter(c -> Optional.ofNullable(c.rolledBias()))
    ).apply(i, (rolled, entries, fx, params, bias) -> new RuneContentComponent(rolled, entries, fx, params, bias.orElse(null))));

    public static final PacketCodec<RegistryByteBuf, RuneContentComponent> PACKET_CODEC = PacketCodec.of(
            (comp, buf) -> {
                buf.writeBoolean(comp.rolled());
                buf.writeInt(comp.entries().size());
                for (Entry e : comp.entries()) ENTRY_PACKET.encode(buf, e);
                buf.writeInt(comp.rolledEffects().size());
                for (String s : comp.rolledEffects()) buf.writeString(s);
                Map<String, Map<String, Float>> ep = comp.rolledEffectParams() == null ? Map.of() : comp.rolledEffectParams();
                buf.writeInt(ep.size());
                for (var e : ep.entrySet()) {
                    buf.writeString(e.getKey());
                    buf.writeInt(e.getValue().size());
                    for (var p : e.getValue().entrySet()) { buf.writeString(p.getKey()); buf.writeFloat(p.getValue()); }
                }
                boolean hasBias = comp.rolledBias() != null;
                buf.writeBoolean(hasBias);
                if (hasBias) BIAS_PACKET.encode(buf, comp.rolledBias());
            },
            buf -> {
                boolean rolled = buf.readBoolean();
                int n = buf.readInt();
                List<Entry> entries = new ArrayList<>(n);
                for (int i = 0; i < n; i++) entries.add(ENTRY_PACKET.decode(buf));
                int fn = buf.readInt();
                List<String> fx = new ArrayList<>(fn);
                for (int i = 0; i < fn; i++) fx.add(buf.readString());
                int epn = buf.readInt();
                Map<String, Map<String, Float>> ep = new LinkedHashMap<>(epn);
                for (int i = 0; i < epn; i++) {
                    String effId = buf.readString();
                    int pn = buf.readInt();
                    Map<String, Float> pm = new LinkedHashMap<>(pn);
                    for (int j = 0; j < pn; j++) pm.put(buf.readString(), buf.readFloat());
                    ep.put(effId, pm);
                }
                boolean hasBias = buf.readBoolean();
                RolledBias bias = hasBias ? BIAS_PACKET.decode(buf) : null;
                return new RuneContentComponent(rolled, entries, fx, ep, bias);
            });

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public record Entry(String imprintId, float value, Map<String, Float> extraValues) {
        public Entry(String imprintId, float value) {
            this(imprintId, value, Map.of());
        }
    }

    public record RolledBias(
            @Nullable List<String> groups,
            @Nullable Map<String, Float> groupWeightMultipliers,
            float rarityBoost,
            @Nullable String guaranteedMinRarity,
            @Nullable String maxRarity) {

        public boolean hasGroupFilter() { return groups != null && !groups.isEmpty(); }
    }
}
