package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import draylar.tiered.Tiered;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class ImprintApi {

    private ImprintApi() {
    }

    public static List<String> allImprintIds() {
        List<String> ids = new ArrayList<>(Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().keySet());
        for (var id : ImprintRegistry.REGISTRY.getIds()) {
            String s = id.toString();
            if (!ids.contains(s)) ids.add(s);
        }
        return ids;
    }

    public static List<ImprintView> allImprints() {
        List<ImprintView> out = new ArrayList<>();
        for (String id : allImprintIds()) {
            ImprintView v = describe(id);
            if (v != null) out.add(v);
        }
        return out;
    }

    @Nullable
    public static ImprintView describe(String id) {
        Imprint imprint = ImprintRegistry.get(id);
        if (!(imprint instanceof DataImprint data)) return null;
        ImprintDefinition def = data.definition();

        List<ValueRangeView> ranges = new ArrayList<>();

        ranges.add(new ValueRangeView(null, def.getValueMin(), def.getValueMax(),
                def.getMaxBonus(), def.getValueDisplay(), true));

        List<ImprintDefinition.ExtraRange> extras = data.extraRanges();
        for (ImprintDefinition.ExtraRange r : extras) {
            ranges.add(new ValueRangeView(r.getKey(), r.getValueMin(), r.getValueMax(),
                    r.getMaxBonus(), r.getValueDisplay(), false));
        }

        Map<String, Float> params = def.getParams() == null ? Map.of() : new LinkedHashMap<>(def.getParams());

        return new ImprintView(id, data.nameKey(), data.translationKey(), data.colorRgb(),
                def.getGroups(), def.getCombine(), def.getMultiplicity(), def.getReapply(),
                def.getBehavior(), def.isAttribute(), def.isBehavioral(), ranges, params);
    }

    public static float resolvedPrimary(PlayerEntity player, String imprintId) {
        return ImprintResolver.resolveValue(player, imprintId);
    }

    public static float resolvedExtra(PlayerEntity player, String imprintId, String key) {
        return ImprintResolver.resolveExtraValue(player, imprintId, key);
    }

    public static boolean isActive(PlayerEntity player, String imprintId) {
        return ImprintResolver.isActive(player, imprintId);
    }

    public static float maxPrimary(String imprintId) {
        return ImprintResolver.maxCombinedValue(imprintId);
    }

    public static float maxExtra(String imprintId, String key) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        return imprint instanceof DataImprint data ? data.maxExtraValue(key) : 0f;
    }

    public record ImprintView(
            String id,
            String nameKey,
            String lineKey,
            int colorRgb,
            List<String> groups,
            @Nullable String combine,
            @Nullable String multiplicity,
            @Nullable String reapply,
            @Nullable String behavior,
            boolean attribute,
            boolean behavioral,
            List<ValueRangeView> valueRanges,
            Map<String, Float> params) {
    }

    public record ValueRangeView(
            @Nullable String key,
            float valueMin,
            float valueMax,
            float maxBonus,
            String display,
            boolean primary) {
    }
}
