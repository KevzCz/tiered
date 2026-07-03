package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import draylar.tiered.api.imprint.ability.AbilityBinding;
import org.jetbrains.annotations.Nullable;

public class ImprintDefinition {

    @Nullable private String id;

    @Nullable private String color;
    @Nullable @SerializedName("name_key") private String nameKey;
    @Nullable @SerializedName("line_key") private String lineKey;

    @Nullable private String combine;
    @Nullable private String multiplicity;
    @Nullable private String reapply;

    @Nullable private String group;
    @Nullable private List<String> groups;

    @Nullable @SerializedName("active_when") private EligibilityPredicate activeWhen;
    @Nullable @SerializedName("inactive_when") private EligibilityPredicate inactiveWhen;

    @Nullable private List<AbilityBinding> abilities;

    @Nullable private List<TypeComponent> types;

    @Nullable private String type;

    private float value;
    @Nullable @SerializedName("value_min") private Float valueMin;
    @Nullable @SerializedName("value_max") private Float valueMax;
    @Nullable @SerializedName("max_stacks") private Integer maxStacks;
    @Nullable @SerializedName("max_bonus") private Float maxBonus;
    @Nullable @SerializedName("value_display") private String valueDisplay;

    @Nullable private String attribute;
    @Nullable private String operation;
    @SerializedName("any_worn") private boolean anyWorn;
    @Nullable @SerializedName("required_slots") private List<String> requiredSlots;
    @Nullable @SerializedName("optional_slots") private List<String> optionalSlots;
    @Nullable @SerializedName("accessories_slots") private List<String> accessoriesSlots;

    @Nullable private String behavior;
    @Nullable private Map<String, Float> params;

    public ImprintDefinition() {
    }

    public List<TypeComponent> resolvedComponents() {
        if (types != null && !types.isEmpty()) return types;
        TypeComponent c = new TypeComponent();
        c.type = type == null ? "attribute" : type;
        c.value = value;
        c.valueMin = valueMin;
        c.valueMax = valueMax;
        c.maxStacks = maxStacks;
        c.maxBonus = maxBonus;
        c.valueDisplay = valueDisplay;
        c.attribute = attribute;
        c.operation = operation;
        c.anyWorn = anyWorn;
        c.requiredSlots = requiredSlots;
        c.optionalSlots = optionalSlots;
        c.accessoriesSlots = accessoriesSlots;
        c.behavior = behavior;
        c.params = params;
        return List.of(c);
    }

    @Nullable public String getId() { return id; }
    @Nullable public String getColor() { return color; }
    @Nullable public String getNameKey() { return nameKey; }
    @Nullable public String getLineKey() { return lineKey; }
    @Nullable public String getCombine() { return combine; }
    @Nullable public String getMultiplicity() { return multiplicity; }
    @Nullable public String getReapply() { return reapply; }
    @Nullable public String getGroup() { return group; }
    @Nullable public EligibilityPredicate getActiveWhen() { return activeWhen; }
    @Nullable public EligibilityPredicate getInactiveWhen() { return inactiveWhen; }
    public List<AbilityBinding> getAbilities() { return abilities == null ? List.of() : abilities; }

    public List<String> getGroups() {
        if (group == null && groups == null) return List.of();
        List<String> out = new ArrayList<>();
        if (group != null) out.add(group);
        if (groups != null) {
            for (String g : groups) {
                if (g != null && !out.contains(g)) out.add(g);
            }
        }
        return out;
    }

    public boolean inGroup(String groupId) {
        return getGroups().contains(groupId);
    }

    public String getType() {
        List<TypeComponent> comps = resolvedComponents();
        return comps.isEmpty() ? "attribute" : comps.get(0).getType();
    }

    public boolean isAttribute() {
        return resolvedComponents().stream().anyMatch(c -> "attribute".equals(c.getType()));
    }

    public boolean isBehavioral() {
        return resolvedComponents().stream().anyMatch(c -> "behavioral".equals(c.getType()));
    }

    @Nullable public TypeComponent firstAttributeComponent() {
        for (TypeComponent c : resolvedComponents()) if ("attribute".equals(c.getType())) return c;
        return null;
    }

    @Nullable public TypeComponent firstBehavioralComponent() {
        for (TypeComponent c : resolvedComponents()) if ("behavioral".equals(c.getType())) return c;
        return null;
    }

    public List<TypeComponent> behavioralComponents() {
        List<TypeComponent> out = new ArrayList<>();
        for (TypeComponent c : resolvedComponents()) if ("behavioral".equals(c.getType())) out.add(c);
        return out;
    }

    @Nullable public String getAttribute() { TypeComponent c = firstAttributeComponent(); return c == null ? null : c.getAttribute(); }
    @Nullable public String getOperation() { TypeComponent c = firstAttributeComponent(); return c == null ? null : c.getOperation(); }
    public boolean isAnyWorn() { TypeComponent c = firstAttributeComponent(); return c != null && c.isAnyWorn(); }
    @Nullable public List<String> getRequiredSlots() { TypeComponent c = firstAttributeComponent(); return c == null ? null : c.getRequiredSlots(); }
    @Nullable public List<String> getOptionalSlots() { TypeComponent c = firstAttributeComponent(); return c == null ? null : c.getOptionalSlots(); }
    @Nullable public List<String> getAccessoriesSlots() { TypeComponent c = firstAttributeComponent(); return c == null ? null : c.getAccessoriesSlots(); }
    @Nullable public String getBehavior() { TypeComponent c = firstBehavioralComponent(); return c == null ? null : c.getBehavior(); }
    @Nullable public Map<String, Float> getParams() { TypeComponent c = firstBehavioralComponent(); return c == null ? null : c.getParams(); }

    public float getValue() { List<TypeComponent> cs = resolvedComponents(); return cs.isEmpty() ? 0f : cs.get(0).getValue(); }
    public float getValueMin() { List<TypeComponent> cs = resolvedComponents(); return cs.isEmpty() ? 0f : cs.get(0).getValueMin(); }
    public float getValueMax() { List<TypeComponent> cs = resolvedComponents(); return cs.isEmpty() ? 0f : cs.get(0).getValueMax(); }
    public boolean hasValueRange() { List<TypeComponent> cs = resolvedComponents(); return !cs.isEmpty() && cs.get(0).hasValueRange(); }
    public int getMaxStacks() { List<TypeComponent> cs = resolvedComponents(); return cs.isEmpty() ? 0 : cs.get(0).getMaxStacks(); }
    public float getMaxBonus() { List<TypeComponent> cs = resolvedComponents(); return cs.isEmpty() ? 0f : cs.get(0).getMaxBonus(); }
    public String getValueDisplay() { List<TypeComponent> cs = resolvedComponents(); return cs.isEmpty() ? "raw" : cs.get(0).getValueDisplay(); }

    public ImprintDefinition id(String v) { this.id = v; return this; }
    public ImprintDefinition type(String v) { this.type = v; return this; }
    public ImprintDefinition color(String v) { this.color = v; return this; }
    public ImprintDefinition nameKey(String v) { this.nameKey = v; return this; }
    public ImprintDefinition lineKey(String v) { this.lineKey = v; return this; }
    public ImprintDefinition valueDisplay(String v) { this.valueDisplay = v; return this; }
    public ImprintDefinition value(float v) { this.value = v; return this; }
    public ImprintDefinition valueRange(float min, float max) { this.valueMin = min; this.valueMax = max; return this; }
    public ImprintDefinition maxStacks(int v) { this.maxStacks = v; return this; }
    public ImprintDefinition maxBonus(float v) { this.maxBonus = v; return this; }
    public ImprintDefinition combine(String v) { this.combine = v; return this; }
    public ImprintDefinition multiplicity(String v) { this.multiplicity = v; return this; }
    public ImprintDefinition reapply(String v) { this.reapply = v; return this; }
    public ImprintDefinition group(String v) { this.group = v; return this; }
    public ImprintDefinition groups(List<String> v) { this.groups = v; return this; }
    public ImprintDefinition attribute(String v) { this.attribute = v; return this; }
    public ImprintDefinition operation(String v) { this.operation = v; return this; }
    public ImprintDefinition anyWorn(boolean v) { this.anyWorn = v; return this; }
    public ImprintDefinition requiredSlots(List<String> v) { this.requiredSlots = v; return this; }
    public ImprintDefinition optionalSlots(List<String> v) { this.optionalSlots = v; return this; }
    public ImprintDefinition accessoriesSlots(List<String> v) { this.accessoriesSlots = v; return this; }
    public ImprintDefinition behavior(String v) { this.behavior = v; return this; }
    public ImprintDefinition params(Map<String, Float> v) { this.params = v; return this; }
    public ImprintDefinition activeWhen(EligibilityPredicate v) { this.activeWhen = v; return this; }
    public ImprintDefinition inactiveWhen(EligibilityPredicate v) { this.inactiveWhen = v; return this; }
    public ImprintDefinition types(List<TypeComponent> v) { this.types = v; return this; }
    public ImprintDefinition abilities(List<AbilityBinding> v) { this.abilities = v; return this; }

    public static class TypeComponent {

        @Nullable private String type;

        private float value;
        @Nullable @SerializedName("value_min") private Float valueMin;
        @Nullable @SerializedName("value_max") private Float valueMax;
        @Nullable @SerializedName("max_stacks") private Integer maxStacks;
        @Nullable @SerializedName("max_bonus") private Float maxBonus;
        @Nullable @SerializedName("value_display") private String valueDisplay;

        @Nullable private String attribute;
        @Nullable private String operation;
        @SerializedName("any_worn") private boolean anyWorn;
        @Nullable @SerializedName("required_slots") private List<String> requiredSlots;
        @Nullable @SerializedName("optional_slots") private List<String> optionalSlots;
        @Nullable @SerializedName("accessories_slots") private List<String> accessoriesSlots;

        @Nullable private String behavior;
        @Nullable private Map<String, Float> params;

        @Nullable @SerializedName("scale_when") private List<ScaleEntry> scaleWhen;

        @Nullable @SerializedName("extra_ranges") private List<ExtraRange> extraRanges;

        public TypeComponent() {}

        public String getType() { return type == null ? "attribute" : type; }
        public boolean isAttribute() { return "attribute".equals(getType()); }
        public boolean isBehavioral() { return "behavioral".equals(getType()); }

        public float getValue() { return value; }
        public float getValueMin() { return valueMin == null ? value : valueMin; }
        public float getValueMax() { return valueMax == null ? getValueMin() : Math.max(getValueMin(), valueMax); }
        public boolean hasValueRange() { return valueMin != null || valueMax != null; }
        public int getMaxStacks() { return maxStacks == null ? 0 : Math.max(0, maxStacks); }
        public float getMaxBonus() { return maxBonus == null ? 0f : Math.max(0f, maxBonus); }
        public String getValueDisplay() { return valueDisplay == null ? "raw" : valueDisplay; }
        public List<ExtraRange> getExtraRanges() { return extraRanges == null ? List.of() : extraRanges; }

        @Nullable public String getAttribute() { return attribute; }
        @Nullable public String getOperation() { return operation; }
        public boolean isAnyWorn() { return anyWorn; }
        @Nullable public List<String> getRequiredSlots() { return requiredSlots; }
        @Nullable public List<String> getOptionalSlots() { return optionalSlots; }
        @Nullable public List<String> getAccessoriesSlots() { return accessoriesSlots; }
        @Nullable public String getBehavior() { return behavior; }
        @Nullable public Map<String, Float> getParams() { return params; }
        public List<ScaleEntry> getScaleWhen() { return scaleWhen == null ? List.of() : scaleWhen; }

        public TypeComponent type(String v) { this.type = v; return this; }
        public TypeComponent value(float v) { this.value = v; return this; }
        public TypeComponent valueRange(float min, float max) { this.valueMin = min; this.valueMax = max; return this; }
        public TypeComponent maxStacks(int v) { this.maxStacks = v; return this; }
        public TypeComponent maxBonus(float v) { this.maxBonus = v; return this; }
        public TypeComponent valueDisplay(String v) { this.valueDisplay = v; return this; }
        public TypeComponent attribute(String v) { this.attribute = v; return this; }
        public TypeComponent operation(String v) { this.operation = v; return this; }
        public TypeComponent anyWorn(boolean v) { this.anyWorn = v; return this; }
        public TypeComponent requiredSlots(List<String> v) { this.requiredSlots = v; return this; }
        public TypeComponent optionalSlots(List<String> v) { this.optionalSlots = v; return this; }
        public TypeComponent accessoriesSlots(List<String> v) { this.accessoriesSlots = v; return this; }
        public TypeComponent behavior(String v) { this.behavior = v; return this; }
        public TypeComponent params(Map<String, Float> v) { this.params = v; return this; }
        public TypeComponent scaleWhen(List<ScaleEntry> v) { this.scaleWhen = v; return this; }
        public TypeComponent extraRanges(List<ExtraRange> v) { this.extraRanges = v; return this; }
    }

    public static class ScaleEntry {

        @Nullable private String condition;
        @Nullable private Map<String, Float> params;

        public ScaleEntry() {}

        @Nullable public String getCondition() { return condition; }
        public Map<String, Float> getParams() { return params == null ? Map.of() : params; }

        public ScaleEntry condition(String v) { this.condition = v; return this; }
        public ScaleEntry params(Map<String, Float> v) { this.params = v; return this; }
    }

    public static class ExtraRange {

        @Nullable private String key;
        @Nullable @SerializedName("value_min") private Float valueMin;
        @Nullable @SerializedName("value_max") private Float valueMax;
        @Nullable @SerializedName("max_bonus") private Float maxBonus;
        @Nullable @SerializedName("value_display") private String valueDisplay;

        public ExtraRange() {}

        @Nullable public String getKey() { return key; }
        public float getValueMin() { return valueMin == null ? 0f : valueMin; }
        public float getValueMax() { return valueMax == null ? getValueMin() : valueMax; }
        public float getMaxBonus() { return maxBonus == null ? 0f : maxBonus; }
        public String getValueDisplay() { return valueDisplay == null ? "raw" : valueDisplay; }

        public ExtraRange key(String v) { this.key = v; return this; }
        public ExtraRange valueRange(float min, float max) { this.valueMin = min; this.valueMax = max; return this; }
        public ExtraRange maxBonus(float v) { this.maxBonus = v; return this; }
        public ExtraRange valueDisplay(String v) { this.valueDisplay = v; return this; }
    }

}
