package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import draylar.tiered.api.imprint.ability.AbilityBinding;
import draylar.tiered.api.imprint.behavior.ImprintBehavior;
import draylar.tiered.api.imprint.behavior.ImprintBehaviorRegistry;
import draylar.tiered.api.imprint.condition.ScaleCondition;
import draylar.tiered.api.imprint.condition.ScaleConditionRegistry;
import draylar.tiered.util.ReforgeMaterialTooltip;
import org.jetbrains.annotations.Nullable;

public class DataImprint extends Imprint {

    private final String id;
    private final ImprintDefinition def;
    private final ChatFormatting color;
    private final int colorRgb;
    private final Multiplicity multiplicity;
    private final ReapplyMode reapplyMode;
    private final CombineMode combineMode;
    private final ImprintScope scope;
    private final ResourceLocation baseModifierId;

    private final List<ImprintBehavior> behaviors;
    private final List<Map<String, Float>> behaviorParams;

    private final List<float[]> behaviorRanges;

    private final List<List<ImprintDefinition.ScaleEntry>> behaviorScales;

    public DataImprint(String id, ImprintDefinition def) {
        this.id = id;
        this.def = def;
        this.colorRgb = parseColorRgb(def.getColor());
        this.color = colorRgb >= 0 ? ChatFormatting.WHITE : parseNamedColor(def.getColor());
        this.multiplicity = parseEnum(Multiplicity.class, def.getMultiplicity(), Multiplicity.MERGE);
        this.reapplyMode = parseEnum(ReapplyMode.class, def.getReapply(), ReapplyMode.REPLACE);
        this.combineMode = parseEnum(CombineMode.class, def.getCombine(), CombineMode.ADDITIVE);

        List<String> allRequired = new ArrayList<>();
        List<String> allOptional = new ArrayList<>();
        List<String> allAccessories = null;
        for (ImprintDefinition.TypeComponent c : def.resolvedComponents()) {
            if (c.getRequiredSlots() != null) allRequired.addAll(c.getRequiredSlots());
            if (c.getOptionalSlots() != null) allOptional.addAll(c.getOptionalSlots());
            if (c.getAccessoriesSlots() != null) {
                if (allAccessories == null) allAccessories = new ArrayList<>();
                allAccessories.addAll(c.getAccessoriesSlots());
            }
        }
        this.scope = ImprintScope.fromSlots(
                allRequired.isEmpty() ? null : allRequired,
                allOptional.isEmpty() ? null : allOptional,
                allAccessories);

        ResourceLocation parsed = ResourceLocation.tryParse(id);
        String safePath = parsed == null ? id.replaceAll("[^a-z0-9_]", "_") : parsed.getPath();
        this.baseModifierId = ResourceLocation.fromNamespaceAndPath("tiered", "imprint_" + safePath);

        this.behaviors = new ArrayList<>();
        this.behaviorParams = new ArrayList<>();
        this.behaviorRanges = new ArrayList<>();
        this.behaviorScales = new ArrayList<>();
        for (ImprintDefinition.TypeComponent c : def.resolvedComponents()) {
            if (c.isBehavioral()) {
                ImprintBehavior b = ImprintBehaviorRegistry.get(c.getBehavior());
                behaviors.add(b);
                behaviorParams.add(c.getParams() == null ? Map.of() : c.getParams());
                behaviorRanges.add(new float[]{c.getValueMin(), c.getValueMax()});
                behaviorScales.add(c.getScaleWhen());
            }
        }
    }

    public ImprintDefinition definition() {
        return def;
    }

    @Override
    public ImprintAttribute attributeData(ImprintComponent.Entry entry) {
        return attributeData(entry, null);
    }

    public ImprintAttribute attributeData(ImprintComponent.Entry entry, @Nullable ItemStack stack) {
        ImprintDefinition.TypeComponent c = def.firstAttributeComponent();
        if (c == null || c.getAttribute() == null) return null;
        return buildAttributeData(c, entry, stack, baseModifierId);
    }

    public List<ImprintAttribute> allAttributeData(ImprintComponent.Entry entry, @Nullable ItemStack stack) {
        List<ImprintAttribute> result = new ArrayList<>();
        int attrIdx = 0;
        for (ImprintDefinition.TypeComponent c : def.resolvedComponents()) {
            if (!c.isAttribute() || c.getAttribute() == null) continue;
            ResourceLocation modId = attrIdx == 0 ? baseModifierId
                    : ResourceLocation.fromNamespaceAndPath(baseModifierId.getNamespace(), baseModifierId.getPath() + "_attr" + attrIdx);
            ImprintAttribute attr = buildAttributeData(c, entry, stack, modId);
            if (attr != null) result.add(attr);
            attrIdx++;
        }
        return result;
    }

    private ImprintAttribute buildAttributeData(ImprintDefinition.TypeComponent c, ImprintComponent.Entry entry,
            @Nullable ItemStack stack, ResourceLocation modId) {
        double value = entry.value() != 0f ? entry.value() : c.getValue();
        AttributeModifier.Operation op = parseOperation(c.getOperation());

        if (c.isAnyWorn()) {
            ResourceLocation itemModId = modId;
            if (stack != null) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String suffix = itemId.getPath().replace('/', '_');
                itemModId = ResourceLocation.fromNamespaceAndPath(modId.getNamespace(), modId.getPath() + "_" + suffix);
            }
            return ImprintAttribute.anyWorn(c.getAttribute(), itemModId, value, op);
        }
        EquipmentSlot[] required = parseSlots(c.getRequiredSlots());
        EquipmentSlot[] optional = parseSlots(c.getOptionalSlots());
        String[] accessories = c.getAccessoriesSlots() == null ? null
                : c.getAccessoriesSlots().toArray(new String[0]);
        return new ImprintAttribute(c.getAttribute(), modId, value, op, required, optional, accessories, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        for (int i = 0; i < behaviors.size(); i++) {
            ImprintBehavior b = behaviors.get(i);
            if (b != null) b.inventoryTick(stack, world, entity, slot, selected, behaviorParams.get(i));
        }
    }

    @Override
    public void onRemove(ItemStack stack) {
        for (int i = 0; i < behaviors.size(); i++) {
            ImprintBehavior b = behaviors.get(i);
            if (b != null) b.onRemove(stack, behaviorParams.get(i));
        }
    }

    @Nullable
    public ImprintBehavior behavior() {
        return behaviors.isEmpty() ? null : behaviors.get(0);
    }

    public List<ImprintBehavior> behaviors() {
        return behaviors;
    }

    public Map<String, Float> params() {
        return behaviorParams.isEmpty() ? Map.of() : behaviorParams.get(0);
    }

    public Map<String, Float> paramsForBehavior(int i) {
        if (i < 0 || i >= behaviorParams.size()) return Map.of();
        return behaviorParams.get(i);
    }


    public float totalMeleeDamageFraction(Player player, float resolvedValue) {
        return totalMeleeDamageFraction(player, null, resolvedValue);
    }

    public float totalMeleeDamageFraction(Player player, LivingEntity target, float resolvedValue) {
        float total = 0f;
        for (int i = 0; i < behaviors.size(); i++) {
            ImprintBehavior b = behaviors.get(i);
            if (b == null) continue;
            float scaled = scaleValue(i, player, target, null, resolvedValue);
            total += b.meleeDamageFraction(player, target, scaled, behaviorParams.get(i));
        }
        return total;
    }

    public float totalRangedDamageFraction(Player player, float resolvedValue) {
        return totalRangedDamageFraction(player, null, resolvedValue);
    }

    public float totalRangedDamageFraction(Player player, LivingEntity target, float resolvedValue) {
        float total = 0f;
        for (int i = 0; i < behaviors.size(); i++) {
            ImprintBehavior b = behaviors.get(i);
            if (b == null) continue;
            float scaled = scaleValue(i, player, target, null, resolvedValue);
            total += b.rangedDamageFraction(player, target, scaled, behaviorParams.get(i));
        }
        return total;
    }

    public float totalMagicDamageFraction(Player player,
                                          DamageSource source, float resolvedValue) {
        return totalMagicDamageFraction(player, null, source, resolvedValue);
    }

    public float totalMagicDamageFraction(Player player, LivingEntity target,
                                          DamageSource source, float resolvedValue) {
        float total = 0f;
        for (int i = 0; i < behaviors.size(); i++) {
            ImprintBehavior b = behaviors.get(i);
            if (b == null) continue;
            float scaled = scaleValue(i, player, target, source, resolvedValue);
            total += b.magicDamageFraction(player, target, source, scaled, behaviorParams.get(i));
        }
        return total;
    }

    private float scaleValue(int i, Player player, LivingEntity target, DamageSource source, float value) {
        List<ImprintDefinition.ScaleEntry> scales = behaviorScales.get(i);
        if (scales.isEmpty()) return value;
        float mult = 1f;
        for (ImprintDefinition.ScaleEntry s : scales) {
            ScaleCondition cond = ScaleConditionRegistry.get(s.getCondition());
            if (cond != null) mult *= cond.multiplier(player, target, source, s.getParams());
        }
        return value * mult;
    }

    public float totalBonusMeleeDamage(Player player,
                                       LivingEntity target, float amount, float resolvedValue) {
        float total = 0f;
        for (int i = 0; i < behaviors.size(); i++) {
            ImprintBehavior b = behaviors.get(i);
            if (b != null) total += b.bonusMeleeDamage(player, target, amount, resolvedValue, behaviorParams.get(i));
        }
        return total;
    }

    public float maxCombinedValue() {
        return def.getMaxBonus();
    }

    public float rollValue(Random rng) {
        float lo = def.getValueMin();
        float hi = def.getValueMax();
        if (hi <= lo) return lo;
        return lo + rng.nextFloat() * (hi - lo);
    }

    public List<ImprintDefinition.ExtraRange> extraRanges() {
        for (ImprintDefinition.TypeComponent c : def.resolvedComponents()) {
            if (!c.getExtraRanges().isEmpty()) return c.getExtraRanges();
        }
        return List.of();
    }

    public Map<String, Float> rollExtras(Random rng) {
        List<ImprintDefinition.ExtraRange> ranges = extraRanges();
        if (ranges.isEmpty()) return Map.of();
        Map<String, Float> out = new LinkedHashMap<>();
        for (int i = 0; i < ranges.size(); i++) {
            ImprintDefinition.ExtraRange r = ranges.get(i);
            String key = r.getKey() == null ? ("extra" + i) : r.getKey();

            float a = r.getValueMin();
            float b = r.getValueMax();
            float lo = Math.min(a, b);
            float hi = Math.max(a, b);
            out.put(key, hi <= lo ? lo : lo + rng.nextFloat() * (hi - lo));
        }
        return out;
    }

    public float maxExtraValue(String key) {
        for (ImprintDefinition.ExtraRange r : extraRanges()) {
            String k = r.getKey() == null ? null : r.getKey();
            if (key.equals(k)) return r.getMaxBonus();
        }
        return 0f;
    }

    @Nullable
    public AbilityBinding activeBinding(@Nullable Player player) {
        return activeBinding(player, null);
    }

    @Nullable
    public AbilityBinding activeBinding(@Nullable Player player, @Nullable ItemStack stack) {
        if (player == null) return null;
        List<String> onStack = stack != null ? Imprints.ids(stack) : null;
        for (AbilityBinding b : def.getAbilities()) {
            String gate = b.getMasteredWhen();
            if (gate == null) continue;
            if (onStack != null && !onStack.contains(gate)) continue;
            if (ImprintMastery.isMastered(player, gate, 0.99f)) return b;
        }
        return null;
    }

    @Nullable
    public AbilityBinding bindingByAbility(@Nullable String abilityId) {
        if (abilityId == null || abilityId.isEmpty()) return null;
        for (AbilityBinding b : def.getAbilities()) {
            if (abilityId.equals(b.getAbility())) return b;
        }
        return null;
    }

    public String plateNameKeyFor(@Nullable String abilityId) {
        AbilityBinding b = bindingByAbility(abilityId);
        AbilityBinding.Plate p = b == null ? null : b.getPlate();
        return p != null && p.getNameKey() != null ? p.getNameKey() : nameKey();
    }

    public int plateColorRgbFor(@Nullable String abilityId) {
        AbilityBinding b = bindingByAbility(abilityId);
        AbilityBinding.Plate p = b == null ? null : b.getPlate();
        if (p != null && p.getColor() != null) {
            int rgb = parseColorRgb(p.getColor());
            if (rgb >= 0) return rgb;
        }
        return colorRgb();
    }

    public String plateLineKeyFor(@Nullable String abilityId) {
        AbilityBinding b = bindingByAbility(abilityId);
        AbilityBinding.Plate p = b == null ? null : b.getPlate();
        return p != null && p.getLineKey() != null ? p.getLineKey() : translationKey();
    }

    @Override
    public boolean isEligible(ItemStack stack) {
        return isEligible(stack, null);
    }

    @Override
    public boolean isEligible(ItemStack stack, @Nullable Player player) {
        EligibilityPredicate active = def.getActiveWhen();
        if (active != null && !active.isEligible(stack, player)) return false;
        EligibilityPredicate inactive = def.getInactiveWhen();
        return inactive == null || !inactive.isEligible(stack, player);
    }

    public int colorRgb() { return colorRgb >= 0 ? colorRgb : (color.getColor() != null ? color.getColor() : 0x808080); }

    @Override public Multiplicity multiplicity() { return multiplicity; }
    @Override public ReapplyMode reapplyMode() { return reapplyMode; }
    @Override public CombineMode combineMode() { return combineMode; }
    @Override public ImprintScope scope() { return scope; }
    @Override public ChatFormatting color() { return color; }

    @Override
    public String translationKey() {
        return def.getLineKey() != null ? def.getLineKey() : "imprint." + safeKey();
    }

    @Override
    public String nameKey() {
        return def.getNameKey() != null ? def.getNameKey() : translationKey() + ".name";
    }

    @Nullable
    public String describeForCodex() {
        String key = translationKey();
        String display = def.getValueDisplay();
        List<Object> args = new ArrayList<>();
        if (!"none".equals(display)) {
            args.add(rangeArg(def.getValueMin(), def.getValueMax(), display));
        }
        for (ImprintDefinition.ExtraRange r : extraRanges()) {
            args.add(rangeArg(r.getValueMin(), r.getValueMax(), r.getValueDisplay()));
        }
        ImprintDefinition.TypeComponent c = primaryBehavioralComponent();

        String rawLine = Language.getInstance().getOrDefault(key);
        Map<String, String> tokens = new HashMap<>();
        if (!"none".equals(display)) tokens.put("value", codexRange(def.getValueMin(), def.getValueMax(), display));
        for (ImprintDefinition.ExtraRange r : extraRanges()) {
            tokens.put(r.getKey() == null ? "extra" : r.getKey(),
                    codexRange(r.getValueMin(), r.getValueMax(), r.getValueDisplay()));
        }
        if (c != null && c.getParams() != null) {
            for (Map.Entry<String, Float> p : c.getParams().entrySet()) {
                tokens.put(p.getKey(), String.valueOf(formatParam(p.getValue(), ImprintParamDisplay.displayOf(p.getKey()))));
            }
        }
        String named = ImprintParamDisplay.resolveNamedTokensString(rawLine, tokens);
        if (named != null) {
            return named.isEmpty() ? null : named;
        }

        if (c != null && c.getParams() != null) {
            for (Map.Entry<String, Float> p : c.getParams().entrySet()) {
                args.add(formatParam(p.getValue(), ImprintParamDisplay.displayOf(p.getKey())));
            }
        }
        String s = args.isEmpty() ? Component.translatable(key).getString()
                : Component.translatable(key, args.toArray()).getString();
        return s.isEmpty() || s.equals(key) ? null : s;
    }

    private static String rangeArg(float min, float max, String display) {
        if (min == max) return String.valueOf(formatValue(min, display));
        return formatValue(min, display) + " - " + formatValue(max, display);
    }

    private static String codexRange(float min, float max, String display) {
        String a = String.valueOf(formatParam(min, display));
        if (min == max) return a;
        return a + "–" + formatParam(max, display);
    }

    @Override
    public void appendTooltip(ItemStack stack, ImprintComponent.Entry entry, List<Component> tooltip) {
        String display = def.getValueDisplay();

        boolean reinforcePoints = "tiered:reinforce".equals(def.getBehavior()) && entry.value() >= 1f;
        MutableComponent line;
        if (reinforcePoints) {
            line = Component.translatable(translationKey() + ".points", Math.round(entry.value()));
        } else {

            List<Object> args = new ArrayList<>();
            if (!display.equals("none")) args.add(formatValue(entry.value(), display));
            for (ImprintDefinition.ExtraRange r : extraRanges()) {
                String key = r.getKey() == null ? "extra" : r.getKey();
                args.add(formatValue(entry.extra(key), r.getValueDisplay()));
            }

            ImprintDefinition.TypeComponent c = primaryBehavioralComponent();

            Map<String, String> tokens = new HashMap<>();
            if (!display.equals("none")) tokens.put("value", String.valueOf(formatValue(entry.value(), display)));
            for (ImprintDefinition.ExtraRange r : extraRanges()) {
                String key = r.getKey() == null ? "extra" : r.getKey();
                tokens.put(key, String.valueOf(formatValue(entry.extra(key), r.getValueDisplay())));
            }
            if (c != null && c.getParams() != null) {
                for (Map.Entry<String, Float> p : c.getParams().entrySet()) {
                    tokens.put(p.getKey(), String.valueOf(formatParam(p.getValue(), ImprintParamDisplay.displayOf(p.getKey()))));
                }
            }
            String named = ImprintParamDisplay.resolveNamedTokensString(
                    Language.getInstance().getOrDefault(translationKey()), tokens);
            if (named != null) {
                line = Component.literal(named);
            } else {
                if (c != null && c.getParams() != null) {
                    for (Map.Entry<String, Float> p : c.getParams().entrySet()) {
                        args.add(formatParam(p.getValue(), ImprintParamDisplay.displayOf(p.getKey())));
                    }
                }
                line = args.isEmpty() ? Component.translatable(translationKey())
                        : Component.translatable(translationKey(), args.toArray());
            }
        }
        if (colorRgb >= 0) {
            tooltip.add(line.withStyle(s -> s.withColor(TextColor.fromRgb(colorRgb))));
        } else {
            tooltip.add(line.withStyle(s -> s.withColor(color)));
        }
    }

    private ImprintDefinition.TypeComponent primaryBehavioralComponent() {
        for (ImprintDefinition.TypeComponent c : def.resolvedComponents()) {
            if (c.isBehavioral()) return c;
        }
        return null;
    }

    public static Object formatParamPublic(float value, String display) {
        return formatParam(value, display);
    }

    private static Object formatParam(float value, String display) {
        if ("percent".equals(display)) return Math.round(value * 100);
        if ("seconds".equals(display)) return ReforgeMaterialTooltip.trimFloat(value / 20f) + "s";

        if ("amp_level".equals(display)) return toRoman(Math.round(value) + 1);
        return ReforgeMaterialTooltip.trimFloat(value);
    }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static String toRoman(int n) {
        if (n >= 0 && n < ROMAN.length) return ROMAN[n];
        return Integer.toString(n);
    }

    private static Object formatValue(float value, String display) {
        if ("percent".equals(display)) {
            int pct = Math.round(value * 100);
            return (pct >= 0 ? "+" : "") + pct;
        }
        if ("seconds".equals(display)) {
            String secs = ReforgeMaterialTooltip.trimFloat(Math.abs(value) / 20f);
            return (value < 0 ? "-" : "+") + secs + "s";
        }
        String trimmed = ReforgeMaterialTooltip.trimFloat(Math.abs(value));
        return (value < 0 ? "-" : "+") + trimmed;
    }

    private String safeKey() {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? id.replace(':', '.') : (parsed.getNamespace() + "." + parsed.getPath());
    }

    private static int parseColorRgb(@Nullable String name) {
        if (name != null && name.startsWith("#")) {
            try { return Integer.parseInt(name.substring(1), 16) & 0xFFFFFF; }
            catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private static ChatFormatting parseNamedColor(@Nullable String name) {
        if (name == null) return ChatFormatting.GRAY;
        ChatFormatting f = ChatFormatting.getByName(name.toLowerCase());
        return f == null ? ChatFormatting.GRAY : f;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, @Nullable String name, E fallback) {
        if (name == null) return fallback;
        try { return Enum.valueOf(type, name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    private static AttributeModifier.Operation parseOperation(@Nullable String name) {
        if (name == null) return AttributeModifier.Operation.ADD_VALUE;
        return switch (name.toUpperCase()) {
            case "MULTIPLY_BASE", "MULTIPLY_BASE_VALUE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private static EquipmentSlot[] parseSlots(@Nullable List<String> names) {
        if (names == null || names.isEmpty()) return new EquipmentSlot[0];
        List<EquipmentSlot> out = new ArrayList<>();
        for (String n : names) {
            EquipmentSlot s = switch (n.toLowerCase()) {
                case "mainhand", "main_hand" -> EquipmentSlot.MAINHAND;
                case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
                case "head", "helmet" -> EquipmentSlot.HEAD;
                case "chest", "chestplate" -> EquipmentSlot.CHEST;
                case "legs", "leggings" -> EquipmentSlot.LEGS;
                case "feet", "boots" -> EquipmentSlot.FEET;
                default -> null;
            };
            if (s != null) out.add(s);
        }
        return out.toArray(new EquipmentSlot[0]);
    }
}
