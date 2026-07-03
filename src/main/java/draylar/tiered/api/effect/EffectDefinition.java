package draylar.tiered.api.effect;

import com.google.gson.annotations.SerializedName;
import draylar.tiered.util.ReforgeUtil;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class EffectDefinition {

    @Nullable private String id;
    @Nullable private String type;

    @SerializedName("skips_reforge") private boolean skipsReforge;

    private float value;
    @SerializedName("value_min") @Nullable private Float valueMin;
    @SerializedName("value_max") @Nullable private Float valueMax;

    @Nullable private String imprint;

    @Nullable private String effect;

    @Nullable private String description;

    @Nullable @SerializedName("applies_to") private List<String> appliesTo;
    @Nullable @SerializedName("not_applies_to") private List<String> notAppliesTo;

    @Nullable private Map<String, Float> params;

    public EffectDefinition() {
    }

    @Nullable public String getId() { return id; }
    public String getType() { return type == null ? "repair" : type; }
    public boolean isSkipsReforge() { return skipsReforge; }
    public boolean isExtract() { return "extract".equals(getType()); }

    public float getValue() { return value; }
    public float getValueMin() { return valueMin == null ? value : valueMin; }
    public float getValueMax() { return valueMax == null ? getValueMin() : Math.max(getValueMin(), valueMax); }
    public boolean hasValueRange() { return valueMin != null || valueMax != null; }
    public float rollValue(Random rng) {
        float lo = getValueMin(), hi = getValueMax();
        return hi > lo ? lo + rng.nextFloat() * (hi - lo) : lo;
    }

    @Nullable public String getImprint() { return imprint; }
    @Nullable public String getEffect() { return effect; }
    @Nullable public String getDescription() { return description; }
    @Nullable public List<String> getAppliesTo() { return appliesTo; }
    @Nullable public List<String> getNotAppliesTo() { return notAppliesTo; }

    public boolean appliesTo(ItemStack target) {
        if (ReforgeUtil.matchesAny(notAppliesTo, target)) return false;
        if (appliesTo == null || appliesTo.isEmpty()) return true;
        return ReforgeUtil.matchesAny(appliesTo, target);
    }
    @Nullable public Map<String, Float> getParams() { return params; }
    public float getParam(String key, float defaultValue) {
        return params != null && params.containsKey(key) ? params.get(key) : defaultValue;
    }

    public EffectDefinition id(String v) { this.id = v; return this; }
    public EffectDefinition type(String v) { this.type = v; return this; }
    public EffectDefinition skipsReforge(boolean v) { this.skipsReforge = v; return this; }
    public EffectDefinition value(float v) { this.value = v; return this; }
    public EffectDefinition valueRange(float min, float max) { this.valueMin = min; this.valueMax = max; return this; }
    public EffectDefinition imprint(String v) { this.imprint = v; return this; }
    public EffectDefinition effect(String v) { this.effect = v; return this; }
    public EffectDefinition description(String v) { this.description = v; return this; }
    public EffectDefinition appliesTo(List<String> v) { this.appliesTo = v; return this; }
    public EffectDefinition notAppliesTo(List<String> v) { this.notAppliesTo = v; return this; }
    public EffectDefinition params(Map<String, Float> v) { this.params = v; return this; }
}
