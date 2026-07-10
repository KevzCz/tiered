package draylar.tiered.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import draylar.tiered.TieredKeybinds;
import draylar.tiered.api.ImprintPlatesData;
import draylar.tiered.api.effect.DataEffect;
import draylar.tiered.api.effect.EffectDefinition;
import draylar.tiered.api.effect.ReforgeEffect;
import draylar.tiered.api.effect.ReforgeEffectRegistry;
import draylar.tiered.api.imprint.ImprintAttributes;
import draylar.tiered.api.imprint.ImprintCooldownDisplay;
import draylar.tiered.api.imprint.ImprintParamDisplay;
import draylar.tiered.api.imprint.MaxDurabilityImprint;
import draylar.tiered.api.imprint.RuneContentComponent;
import draylar.tiered.api.imprint.RuneContentComponent.Entry;
import draylar.tiered.mixin.client.DrawContextMixin;
import draylar.tiered.registry.ModComponents;
import org.jetbrains.annotations.Nullable;

import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.api.ReforgeMaterialBadgeData;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.ability.AbilityBinding;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class ReforgeMaterialTooltip {

    private static boolean expanded = false;
    private static boolean altWasDown = false;

    private ReforgeMaterialTooltip() {
    }

    public static void appendAll(List<Component> tooltip, ItemStack stack, ReforgeMaterial material) {
        appendAll(tooltip, stack, material, null);
    }

    public static void appendAll(List<Component> tooltip, ItemStack stack, ReforgeMaterial material, @Nullable Player player) {
        poll();

        boolean hasMaterial = material != null;
        boolean hasRune = hasRuneContent(stack);
        boolean hasImprints = hasImprints(stack);
        boolean hasSlots = ImprintSlots.capacity(stack) > 0;

        if (!hasMaterial && !hasRune && !hasImprints && !hasSlots) return;

        if (!expanded) {

            tooltip.add(Component.translatable("screen.tiered.reforge.material.hold_alt", TieredKeybinds.detailsKeyName())
                    .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
            return;
        }

        if (hasMaterial) {
            tooltip.add(Component.translatable("screen.tiered.reforge.material.header").withStyle(s -> s.withColor(ChatFormatting.GRAY)));

            if (!shiftDown() && hasRevealableDescriptions(material)) {
                tooltip.add(Component.translatable("screen.tiered.reforge.material.hold_shift", TieredKeybinds.descriptionsKeyName())
                        .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
            }
        }
        appendRuneContentSection(tooltip, stack);
        appendImprintsSection(tooltip, stack, player);
    }

    private static final String IMPRINT_MARKER_TAG = "tiered:imprint_plate/";
    private static final String IMPRINT_MARKER_SEP = "|";

    private static Component imprintMarker(ItemStack stack, Imprint imprint, ImprintComponent.Entry entry) {
        List<Component> lines = new ArrayList<>();
        imprint.appendTooltip(stack, entry, lines);
        MutableComponent visible = (lines.isEmpty() ? Component.translatable(imprint.nameKey()) : lines.get(0)).copy();

        boolean eligible = imprint.isEligible(stack);
        String payload = IMPRINT_MARKER_TAG + entry.id() + IMPRINT_MARKER_SEP + entry.value()
                + IMPRINT_MARKER_SEP + (eligible ? "1" : "0")
                + IMPRINT_MARKER_SEP + encodeExtras(entry.extraValues());
        return visible.withStyle(s -> s.withInsertion(payload));
    }

    public static Component imprintMarker(String id, float value) {
        return imprintMarker(id, value, Map.of());
    }

    public static Component imprintMarker(String id, float value, Map<String, Float> extras) {
        Imprint imprint = ImprintRegistry.get(id);
        MutableComponent visible = Component.literal(imprint == null ? id : Component.translatable(imprint.nameKey()).getString());
        String payload = IMPRINT_MARKER_TAG + id + IMPRINT_MARKER_SEP + value + IMPRINT_MARKER_SEP + "1"
                + IMPRINT_MARKER_SEP + encodeExtras(extras);
        return visible.withStyle(s -> s.withInsertion(payload));
    }

    private static final String EXTRA_PAIR_SEP = ",";

    private static final String EXTRA_KV_SEP = ":";

    private static String encodeExtras(Map<String, Float> extras) {
        if (extras == null || extras.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Float> e : extras.entrySet()) {
            if (sb.length() > 0) sb.append(EXTRA_PAIR_SEP);
            sb.append(e.getKey()).append(EXTRA_KV_SEP).append(e.getValue());
        }
        return sb.toString();
    }

    private static Map<String, Float> decodeExtras(String s) {
        if (s == null || s.isEmpty()) return Map.of();
        Map<String, Float> out = new LinkedHashMap<>();
        for (String pair : s.split(EXTRA_PAIR_SEP)) {
            int eq = pair.indexOf(EXTRA_KV_SEP);
            if (eq <= 0) continue;
            try {
                out.put(pair.substring(0, eq), Float.parseFloat(pair.substring(eq + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public static boolean isImprintMarker(Component line) {
        String ins = line.getStyle().getInsertion();
        return ins != null && ins.startsWith(IMPRINT_MARKER_TAG);
    }

    public static ImprintPlatesData.Plate markerToPlate(Component line) {
        ParsedMarker m = parseMarker(line);
        if (m == null) return null;
        String label = Component.translatable(m.imprint.nameKey()).getString().toUpperCase(Locale.ROOT);

        int fill = m.eligible ? imprintToFill(m.imprint) : 0xFF444444;
        return new ImprintPlatesData.Plate(Component.literal(label), fill);
    }

    public static Component imprintDescriptionLine(Component line, ItemStack stack) {
        ParsedMarker m = parseMarker(line);
        if (m == null) return null;
        List<Component> lines = new ArrayList<>();
        m.imprint.appendTooltip(stack == null ? ItemStack.EMPTY : stack,
                new ImprintComponent.Entry(m.id, 1, m.value, m.extras), lines);
        if (lines.isEmpty()) return null;
        return Component.literal(" ").append(lines.get(0));
    }

    private static ParsedMarker parseMarker(Component line) {
        String ins = line.getStyle().getInsertion();
        if (ins == null || !ins.startsWith(IMPRINT_MARKER_TAG)) return null;
        String payload = ins.substring(IMPRINT_MARKER_TAG.length());

        String[] parts = payload.split("\\" + IMPRINT_MARKER_SEP);
        if (parts.length < 2) return null;
        String id = parts[0];
        float value;
        try {
            value = Float.parseFloat(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        boolean eligible = parts.length < 3 || !"0".equals(parts[2]);
        Map<String, Float> extras = parts.length < 4 ? Map.of() : decodeExtras(parts[3]);
        Imprint imprint = ImprintRegistry.get(id);
        if (imprint == null) return null;
        return new ParsedMarker(id, value, eligible, extras, imprint);
    }

    private record ParsedMarker(String id, float value, boolean eligible, Map<String, Float> extras, Imprint imprint) {
    }

    public static int imprintToFill(Imprint imprint) {
        if (imprint instanceof DataImprint di) {
            return 0xFF000000 | di.colorRgb();
        }
        return formattingToFill(imprint.color());
    }

    private static int formattingToFill(ChatFormatting formatting) {
        Integer rgb = formatting == null ? null : formatting.getColor();
        int base = rgb == null ? 0x808080 : rgb;
        return 0xFF000000 | base;
    }

    public static boolean isExpanded() {
        poll();
        return expanded;
    }

    private static boolean hasImprints(ItemStack stack) {
        ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
        return comp != null && !comp.entries().isEmpty();
    }

    private static boolean hasRuneContent(ItemStack stack) {
        var content = stack.get(ModComponents.RUNE_CONTENT);
        return content != null && !content.isEmpty();
    }

    private static void appendImprintsSection(List<Component> tooltip, ItemStack stack, @Nullable Player player) {
        ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
        int capacity = ImprintSlots.capacity(stack);
        boolean hasAny = comp != null && !comp.entries().isEmpty();

        if (!hasAny && capacity <= 0) return;

        int used = comp == null ? 0 : comp.slotsUsed();

        tooltip.add(slotsMarker(stack, comp, used, capacity, player));
    }

    private static final String SLOTS_MARKER_TAG = "tiered:imprint_slots/";

    private static final String SLOT_SEP = "|";
    private static final String ENTRY_SEP = ";";
    private static final String KV_SEP = "=";

    private static int findEquipmentSlot(ItemStack stack, @Nullable Player player) {
        if (player == null) return -1;
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (player.getItemBySlot(s) == stack) return s.ordinal();
        }
        return -1;
    }

    private static Component slotsMarker(ItemStack stack, ImprintComponent comp, int used, int capacity,
            @Nullable Player player) {

        int slotOrdinal = findEquipmentSlot(stack, player);
        StringBuilder sb = new StringBuilder(SLOTS_MARKER_TAG).append(used).append(',').append(capacity)
                .append(',').append(slotOrdinal);
        if (comp != null) {
            for (ImprintComponent.Slot slot : comp.slots()) {
                sb.append(SLOT_SEP).append(slot.sourceId());
                for (ImprintComponent.Entry e : slot.entries()) {
                    Imprint imprint = ImprintRegistry.get(e.id());
                    boolean eligible = imprint == null || imprint.isEligible(stack, player);

                    String abilityId = "";
                    if (imprint instanceof DataImprint di) {
                        AbilityBinding b = di.activeBinding(player, stack);
                        if (b != null && b.getAbility() != null) abilityId = b.getAbility();
                    }

                    sb.append(ENTRY_SEP).append(e.id()).append(KV_SEP).append(e.value())
                            .append(KV_SEP).append(eligible ? "1" : "0")
                            .append(KV_SEP).append(encodeExtras(e.extraValues()))
                            .append(KV_SEP).append(abilityId);
                }
            }
        }
        return Component.literal("").withStyle(s -> s.withInsertion(sb.toString()));
    }

    public static boolean isSlotsMarker(Component line) {
        String ins = line.getStyle().getInsertion();
        return ins != null && ins.startsWith(SLOTS_MARKER_TAG);
    }

    public static ImprintPlatesData slotsMarkerToData(Component line, List<Component> siblingLines) {
        String ins = line.getStyle().getInsertion();
        if (ins == null || !ins.startsWith(SLOTS_MARKER_TAG)) return null;
        String payload = ins.substring(SLOTS_MARKER_TAG.length());
        String[] parts = payload.split("\\" + SLOT_SEP, -1);
        int used, capacity;
        int stackHash = 0;
        try {
            String[] uc = parts[0].split(",", -1);
            used = Integer.parseInt(uc[0]);
            capacity = Integer.parseInt(uc[1]);
            if (uc.length >= 3) stackHash = Integer.parseInt(uc[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
        final int finalStackHash = stackHash;
        Component header = Component.translatable("screen.tiered.imprints.header.slots", used, capacity)
                .withStyle(s -> s.withColor(ChatFormatting.GRAY));
        List<ImprintPlatesData.SlotView> slots = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String[] sp = parts[i].split(ENTRY_SEP, -1);
            ItemStack rune = runeIcon(sp[0]);
            List<ImprintPlatesData.Plate> plates = new ArrayList<>();
            for (int j = 1; j < sp.length; j++) {

                String[] kv = sp[j].split(KV_SEP, -1);
                String id = kv[0];
                Imprint imprint = ImprintRegistry.get(id);
                if (imprint == null) continue;
                boolean eligible = kv.length < 3 || !"0".equals(kv[2]);

                if (eligible && imprint instanceof DataImprint) {
                    float allocated = ImprintAttributes.getAllocatedValueBySlot(finalStackHash, id, -1f);
                    if (allocated == 0f) eligible = false;
                }

                String abilityId = kv.length >= 5 ? kv[4] : "";
                String nameKey = imprint instanceof DataImprint di ? di.plateNameKeyFor(abilityId) : imprint.nameKey();
                int baseFill = imprint instanceof DataImprint di ? (0xFF000000 | di.plateColorRgbFor(abilityId)) : imprintToFill(imprint);
                String label = Component.translatable(nameKey).getString().toUpperCase(Locale.ROOT);
                int fill = eligible ? baseFill : 0xFF444444;

                String cdKey = !abilityId.isEmpty() ? abilityId : id;
                float cdFill = eligible ? ImprintCooldownDisplay.fillOf(cdKey) : 1.0f;
                boolean active = eligible && ImprintCooldownDisplay.isActive(cdKey);
                plates.add(new ImprintPlatesData.Plate(Component.literal(label), fill, cdFill, active, id, abilityId));
            }
            slots.add(new ImprintPlatesData.SlotView(rune, plates));
        }
        return new ImprintPlatesData(header, slots, capacity, siblingLines);
    }

    public static List<Component> slotsMarkerToDescriptions(Component line) {
        List<Component> out = new ArrayList<>();
        String ins = line.getStyle().getInsertion();
        if (ins == null || !ins.startsWith(SLOTS_MARKER_TAG)) return out;
        String payload = ins.substring(SLOTS_MARKER_TAG.length());
        String[] parts = payload.split("\\" + SLOT_SEP, -1);

        try {
            String[] uc = parts[0].split(",", -1);
            out.add(Component.translatable("screen.tiered.imprints.header.slots",
                    Integer.parseInt(uc[0]), Integer.parseInt(uc[1])).withStyle(s -> s.withColor(ChatFormatting.GRAY)));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
        }
        for (int i = 1; i < parts.length; i++) {
            String[] sp = parts[i].split(ENTRY_SEP, -1);
            for (int j = 1; j < sp.length; j++) {

                String[] kv = sp[j].split(KV_SEP, -1);
                if (kv.length < 2) continue;
                String id = kv[0];
                float value;
                try {
                    value = Float.parseFloat(kv[1]);
                } catch (NumberFormatException e) {
                    continue;
                }
                Imprint imprint = ImprintRegistry.get(id);
                if (imprint == null) continue;

                String abilityId = kv.length >= 5 ? kv[4] : "";
                if (imprint instanceof DataImprint di && di.bindingByAbility(abilityId) != null) {
                    int tier = Math.max(1, (int) value);
                    AbilityBinding binding = di.bindingByAbility(abilityId);

                    String lineKey = di.plateLineKeyFor(abilityId);
                    Map<String, Float> plateParams = binding != null ? binding.getParams(tier) : Map.of();

                    String rawLine = Language.getInstance().getOrDefault(lineKey);
                    String named = ImprintParamDisplay.resolveNamedTokens(rawLine, plateParams);
                    Component plateLine;
                    if (named != null) {
                        plateLine = Component.literal(named);
                    } else {
                        List<Object> pArgs = new ArrayList<>();
                        plateParams.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .forEach(e -> pArgs.add(DataImprint.formatParamPublic(
                                        e.getValue(), ImprintParamDisplay.displayOf(e.getKey()))));
                        plateLine = pArgs.isEmpty()
                                ? Component.translatable(lineKey)
                                : Component.translatable(lineKey, pArgs.toArray());
                    }
                    out.add(Component.literal(" ").append(plateLine.copy().withStyle(s -> s.withColor(ChatFormatting.GRAY))));
                    continue;
                }

                Map<String, Float> extras = kv.length >= 4 ? decodeExtras(kv[3]) : Map.of();
                List<Component> lines = new ArrayList<>();
                imprint.appendTooltip(ItemStack.EMPTY, new ImprintComponent.Entry(id, 1, value, extras), lines);
                if (!lines.isEmpty()) out.add(Component.literal(" ").append(lines.get(0)));
            }
        }
        return out;
    }

    private static ItemStack runeIcon(String runeId) {
        if (runeId == null || runeId.isBlank()) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(runeId);
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static void appendRuneContentSection(List<Component> tooltip, ItemStack stack) {
        var content = stack.get(ModComponents.RUNE_CONTENT);
        if (content == null || content.isEmpty()) return;

        tooltip.add(Component.translatable("screen.tiered.rune.grants.header").withStyle(s -> s.withColor(ChatFormatting.GRAY)));
        for (var entry : content.entries()) {
            Imprint imprint = ImprintRegistry.get(entry.imprintId());
            if (imprint == null) continue;
            List<Component> lines = new ArrayList<>();

            if (MaxDurabilityImprint.ID.equals(entry.imprintId())) {
                int pct = Math.round(entry.value() * 100);
                String name = Component.translatable(imprint.nameKey()).getString();
                lines.add(Component.literal(name + " (+" + pct + "% durability)")
                        .withStyle(s -> s.withColor(ChatFormatting.GOLD)));
            } else {
                imprint.appendTooltip(stack,
                        new ImprintComponent.Entry(entry.imprintId(), 1, entry.value(), entry.extraValues()), lines);
            }
            for (Component line : lines) {
                tooltip.add(Component.literal(" ").append(line));
            }
            List<String> reqTags = ImprintRequirements.tags(entry.imprintId());
            if (!reqTags.isEmpty()) {
                tooltip.add(Component.literal("  ").append(
                        Component.translatable("screen.tiered.rune.requires", String.join(", ", reqTags))
                                .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY))));
            }
        }
    }

    private static void poll() {
        boolean altDown = TieredKeybinds.detailsHeld();
        if (altDown && !altWasDown) {
            expanded = !expanded;
        }
        altWasDown = altDown;
    }

    public static ReforgeMaterialBadgeData buildBadges(ReforgeMaterial material) {
        List<ReforgeMaterialBadgeData.Badge> badges = new ArrayList<>();

        addStaticBiasBadges(badges, material);

        if (material.isSkipsReforge()) {
            badges.add(badge("⟳", Component.translatable("screen.tiered.reforge.material.preserves_tier"), 0xFF4A7FA5,
                    "screen.tiered.reforge.material.preserves_tier.desc"));
        }

        if (material.isSkipsBaseItem()) {
            if (material.isSkipsReforge()) {
                if (!badges.isEmpty()) badges.remove(badges.size() - 1);
                badges.add(badge("∅", Component.translatable("screen.tiered.reforge.material.standalone"), 0xFF555555,
                        "screen.tiered.reforge.material.standalone.desc"));
            } else {
                badges.add(badge("∅", Component.translatable("screen.tiered.reforge.material.no_base_item"), 0xFF7A9E7E,
                        "screen.tiered.reforge.material.no_base_item.desc"));
            }
        }

        appendEffectBadges(badges, material.getEffects(), material);
        if (!material.isBadgeHidden("effect_pool")) {
            for (ReforgeMaterial.EffectPool pool : material.getEffectPools()) {
                appendEffectPoolBadge(badges, pool, material);
            }

            appendEffectChoiceBadge(badges, material);
        }

        if (!material.isBadgeHidden("behavior_pool")) {
            for (ReforgeMaterial.BehaviorPool pool : material.getBehaviorPools()) {
                appendBehaviorPoolBadge(badges, pool);
            }

            appendBehaviorChoiceBadge(badges, material);
        }

        if (!material.isBadgeHidden("imprint_pool")) {
            for (ReforgeMaterial.ImprintPool pool : material.getImprintPools()) {
                appendImprintPoolBadge(badges, pool);
            }
            appendImprintChoiceBadge(badges, material);
        }

        List<Component> lore = new ArrayList<>();
        if (material.getDescription() != null) {
            for (String line : material.getDescription()) {
                lore.add(Component.translatable(line));
            }
        }

        if (badges.isEmpty() && lore.isEmpty()) return null;
        return new ReforgeMaterialBadgeData(badges, lore, shiftDown());
    }

    public static ReforgeMaterialBadgeData buildBadges(ReforgeMaterial material, @Nullable ItemStack stack) {
        RuneContentComponent comp = stack == null ? null
                : stack.get(ModComponents.RUNE_CONTENT);
        if (comp == null || !comp.rolled()) return buildBadges(material);

        List<ReforgeMaterialBadgeData.Badge> badges = new ArrayList<>();

        RuneContentComponent.RolledBias bias = comp.rolledBias();
        if (bias != null) {
            if (bias.hasGroupFilter()) {
                String groups = String.join(", ", bias.groups().stream().map(ReforgeMaterialTooltip::capitalize).toList());
                badges.add(badge("✦", Component.translatable("screen.tiered.reforge.material.groups", groups), 0xFF6B5B95,
                        "screen.tiered.reforge.material.groups.desc"));
            }
            if (bias.groupWeightMultipliers() != null && !bias.groupWeightMultipliers().isEmpty()) {
                bias.groupWeightMultipliers().forEach((group, mult) ->
                        badges.add(badge("⚖", Component.translatable("screen.tiered.reforge.material.favor",
                                String.format(Locale.ROOT, "%.1f", mult), capitalize(group)), 0xFFB8860B,
                                "screen.tiered.reforge.material.favor.desc")));
            }
            if (bias.rarityBoost() > 0f) {
                badges.add(badge("▲", Component.translatable("screen.tiered.reforge.material.rarity_boost",
                        (int) (bias.rarityBoost() * 100)), 0xFF8E44AD,
                        "screen.tiered.reforge.material.rarity_boost.desc"));
            }
            if (bias.guaranteedMinRarity() != null) {
                badges.add(badge("≥", Component.translatable("screen.tiered.reforge.material.min_rarity",
                        capitalize(bias.guaranteedMinRarity())), 0xFF2E8B57,
                        "screen.tiered.reforge.material.min_rarity.desc"));
            }
            if (bias.maxRarity() != null) {
                badges.add(badge("≤", Component.translatable("screen.tiered.reforge.material.max_rarity",
                        capitalize(bias.maxRarity())), 0xFFB23A48,
                        "screen.tiered.reforge.material.max_rarity.desc"));
            }
        } else {

            addStaticBiasBadges(badges, material);
        }

        if (material.isSkipsReforge()) {
            if (material.isSkipsBaseItem()) {
                badges.add(badge("∅", Component.translatable("screen.tiered.reforge.material.standalone"), 0xFF555555,
                        "screen.tiered.reforge.material.standalone.desc"));
            } else {
                badges.add(badge("⟳", Component.translatable("screen.tiered.reforge.material.preserves_tier"), 0xFF4A7FA5,
                        "screen.tiered.reforge.material.preserves_tier.desc"));
            }
        } else if (material.isSkipsBaseItem()) {
            badges.add(badge("∅", Component.translatable("screen.tiered.reforge.material.no_base_item"), 0xFF7A9E7E,
                    "screen.tiered.reforge.material.no_base_item.desc"));
        }

        appendEffectBadges(badges, material.getEffects(), material, comp);
        appendEffectBadges(badges, comp.rolledEffects(), material, comp);

        List<Component> lore = new ArrayList<>();
        if (material.getDescription() != null) {
            for (String line : material.getDescription()) lore.add(Component.translatable(line));
        }

        if (badges.isEmpty() && lore.isEmpty()) return null;
        return new ReforgeMaterialBadgeData(badges, lore, shiftDown());
    }

    private static void addStaticBiasBadges(List<ReforgeMaterialBadgeData.Badge> badges, ReforgeMaterial material) {
        if (material.hasGroupFilter() && !material.isBadgeHidden("groups")) {
            String groups = String.join(", ", material.getGroups().stream().map(ReforgeMaterialTooltip::capitalize).toList());
            badges.add(badge("✦", Component.translatable("screen.tiered.reforge.material.groups", groups), 0xFF6B5B95,
                    "screen.tiered.reforge.material.groups.desc"));
        }
        if (material.getGroupWeightMultipliers() != null && !material.getGroupWeightMultipliers().isEmpty()
                && !material.isBadgeHidden("group_weight_multipliers")) {
            material.getGroupWeightMultipliers().forEach((group, mult) ->
                    badges.add(badge("⚖", Component.translatable("screen.tiered.reforge.material.favor",
                            String.format(Locale.ROOT, "%.1f", mult), capitalize(group)), 0xFFB8860B,
                            "screen.tiered.reforge.material.favor.desc")));
        }
        if (material.getRarityBoost() > 0f && !material.isBadgeHidden("rarity_boost")) {
            badges.add(badge("▲", Component.translatable("screen.tiered.reforge.material.rarity_boost",
                    (int) (material.getRarityBoost() * 100)), 0xFF8E44AD,
                    "screen.tiered.reforge.material.rarity_boost.desc"));
        }
        if (material.getGuaranteedMinRarity() != null && !material.isBadgeHidden("min_rarity")) {
            badges.add(badge("≥", Component.translatable("screen.tiered.reforge.material.min_rarity",
                    capitalize(material.getGuaranteedMinRarity())), 0xFF2E8B57,
                    "screen.tiered.reforge.material.min_rarity.desc"));
        }
        if (material.getMaxRarity() != null && !material.isBadgeHidden("max_rarity")) {
            badges.add(badge("≤", Component.translatable("screen.tiered.reforge.material.max_rarity",
                    capitalize(material.getMaxRarity())), 0xFFB23A48,
                    "screen.tiered.reforge.material.max_rarity.desc"));
        }
    }

    private static void appendEffectPoolBadge(List<ReforgeMaterialBadgeData.Badge> badges,
            ReforgeMaterial.EffectPool pool, ReforgeMaterial material) {
        if (pool.getCandidates().isEmpty()) return;

        Component countLabel = pool.getRollMin() == pool.getRollMax()
                ? Component.translatable("screen.tiered.reforge.material.effect_pool.fixed", pool.getRollMin())
                : Component.translatable("screen.tiered.reforge.material.effect_pool", pool.getRollMin(), pool.getRollMax());

        if (shiftDown()) {
            List<ReforgeMaterialBadgeData.Badge> candidateBadges = new ArrayList<>();
            for (ReforgeMaterial.EffectCandidate ec : pool.getCandidates()) {
                if (ec.getEffect() == null) continue;
                ReforgeMaterialBadgeData.Badge b = effectBadge(ec.getEffect(), material, null);
                if (b != null) candidateBadges.add(b);
            }
            if (!candidateBadges.isEmpty()) {
                int idx = (int) ((System.currentTimeMillis() / 1000L) % candidateBadges.size());
                ReforgeMaterialBadgeData.Badge b = candidateBadges.get(idx);
                Component desc = Component.translatable("screen.tiered.reforge.material.effect_pool.desc")
                        .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(true));
                badges.add(new ReforgeMaterialBadgeData.Badge(b.glyph(), b.label(), b.fillColor(), desc));
                return;
            }
        }
        badges.add(badge("?", countLabel, 0xFF888888, "screen.tiered.reforge.material.effect_pool.desc"));
    }

    private static void appendEffectChoiceBadge(List<ReforgeMaterialBadgeData.Badge> badges, ReforgeMaterial material) {
        ReforgeMaterial.EffectPoolChoice choice = material.getEffectPoolChoice();
        if (choice == null || choice.isEmpty()) return;

        List<List<ReforgeMaterialBadgeData.Badge>> options = new ArrayList<>();
        for (ReforgeMaterial.EffectPoolChoice.Candidate c : choice.getCandidates()) {
            if (c.getPool() == null) continue;
            List<ReforgeMaterialBadgeData.Badge> bundle = new ArrayList<>();
            for (ReforgeMaterial.EffectCandidate ec : c.getPool().getCandidates()) {
                if (ec.getEffect() == null) continue;
                ReforgeMaterialBadgeData.Badge b = effectBadge(ec.getEffect(), material, null);
                if (b != null) bundle.add(b);
            }
            if (!bundle.isEmpty()) options.add(bundle);
        }
        appendChoiceOptions(badges, options, "screen.tiered.reforge.material.effect_pool_choice");
    }

    private static void appendChoiceOptions(List<ReforgeMaterialBadgeData.Badge> badges,
            List<List<ReforgeMaterialBadgeData.Badge>> options, String countKey) {
        if (options.isEmpty()) return;
        if (!shiftDown() || options.size() <= 1) {
            badges.add(badge("?", Component.translatable(countKey, options.size()),
                    0xFF888888, countKey + ".desc"));
            return;
        }

        int idx = (int) ((System.currentTimeMillis() / 1000L) % options.size());
        List<ReforgeMaterialBadgeData.Badge> shown = options.get(idx);
        Component choiceDesc = Component.translatable(countKey + ".desc")
                .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(true));
        for (int i = 0; i < shown.size(); i++) {
            ReforgeMaterialBadgeData.Badge b = shown.get(i);
            badges.add(i == shown.size() - 1
                    ? new ReforgeMaterialBadgeData.Badge(b.glyph(), b.label(), b.fillColor(), choiceDesc)
                    : new ReforgeMaterialBadgeData.Badge(b.glyph(), b.label(), b.fillColor(), null));
        }
    }

    private static void appendBehaviorPoolBadge(List<ReforgeMaterialBadgeData.Badge> badges,
            ReforgeMaterial.BehaviorPool pool) {
        if (pool.getCandidates().isEmpty()) return;

        Component countLabel = pool.getRollMin() == pool.getRollMax()
                ? Component.translatable("screen.tiered.reforge.material.behavior_pool.fixed", pool.getRollMin())
                : Component.translatable("screen.tiered.reforge.material.behavior_pool", pool.getRollMin(), pool.getRollMax());

        if (shiftDown()) {

            List<List<ReforgeMaterialBadgeData.Badge>> fragments = new ArrayList<>();
            for (ReforgeMaterial.BehaviorPool.Candidate frag : pool.getCandidates()) {
                List<ReforgeMaterialBadgeData.Badge> b = biasBadges(frag);
                if (!b.isEmpty()) fragments.add(b);
            }
            if (!fragments.isEmpty()) {
                int idx = (int) ((System.currentTimeMillis() / 1000L) % fragments.size());
                List<ReforgeMaterialBadgeData.Badge> shown = fragments.get(idx);
                Component desc = Component.translatable("screen.tiered.reforge.material.behavior_pool.desc")
                        .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(true));
                for (int i = 0; i < shown.size(); i++) {
                    ReforgeMaterialBadgeData.Badge b = shown.get(i);
                    badges.add(i == 0
                            ? new ReforgeMaterialBadgeData.Badge(b.glyph(), b.label(), b.fillColor(), desc)
                            : b);
                }
                return;
            }
        }
        badges.add(badge("~", countLabel, 0xFF607060, "screen.tiered.reforge.material.behavior_pool.desc"));
    }

    @Nullable
    private static ReforgeMaterialBadgeData.Badge imprintBadge(String imprintId) {
        Imprint imp = ImprintRegistry.get(imprintId);
        if (imp == null) return null;
        String name = Component.translatable(imp.nameKey()).getString().toUpperCase(Locale.ROOT);
        return new ReforgeMaterialBadgeData.Badge("✦", Component.literal(name), imprintToFill(imp), null);
    }

    private static List<ReforgeMaterialBadgeData.Badge> imprintPoolPlates(ReforgeMaterial.ImprintPool pool) {
        List<ReforgeMaterialBadgeData.Badge> out = new ArrayList<>();
        for (String id : pool.getDefaultImprints()) {
            ReforgeMaterialBadgeData.Badge b = imprintBadge(id);
            if (b != null) out.add(b);
        }
        for (ReforgeMaterial.Candidate c : pool.getCandidates()) {
            ReforgeMaterialBadgeData.Badge b = imprintBadge(c.getImprint());
            if (b != null) out.add(b);
        }

        for (String group : pool.getImprintGroups()) {
            out.add(badge("✦", Component.translatable("screen.tiered.reforge.material.imprint_group", capitalize(group)),
                    0xFF6B5B95, null));
        }
        return out;
    }

    private static void appendImprintPoolBadge(List<ReforgeMaterialBadgeData.Badge> badges,
            ReforgeMaterial.ImprintPool pool) {
        List<ReforgeMaterialBadgeData.Badge> plates = imprintPoolPlates(pool);
        if (plates.isEmpty()) return;

        Component countLabel = pool.getRollMin() == pool.getRollMax()
                ? Component.translatable("screen.tiered.reforge.material.imprint_pool.fixed", pool.getRollMin())
                : Component.translatable("screen.tiered.reforge.material.imprint_pool", pool.getRollMin(), pool.getRollMax());

        if (shiftDown()) {
            int idx = (int) ((System.currentTimeMillis() / 1000L) % plates.size());
            ReforgeMaterialBadgeData.Badge b = plates.get(idx);
            Component desc = Component.translatable("screen.tiered.reforge.material.imprint_pool.desc")
                    .withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(true));
            badges.add(new ReforgeMaterialBadgeData.Badge(b.glyph(), b.label(), b.fillColor(), desc));
            return;
        }
        badges.add(badge("?", countLabel, 0xFF888888, "screen.tiered.reforge.material.imprint_pool.desc"));
    }

    private static void appendImprintChoiceBadge(List<ReforgeMaterialBadgeData.Badge> badges, ReforgeMaterial material) {
        ReforgeMaterial.ImprintPoolChoice choice = material.getImprintPoolChoice();
        if (choice == null || choice.isEmpty()) return;
        List<List<ReforgeMaterialBadgeData.Badge>> options = new ArrayList<>();
        for (ReforgeMaterial.ImprintPoolChoice.Candidate c : choice.getCandidates()) {
            if (c.getPool() == null) continue;
            List<ReforgeMaterialBadgeData.Badge> bundle = imprintPoolPlates(c.getPool());
            if (!bundle.isEmpty()) options.add(bundle);
        }
        appendChoiceOptions(badges, options, "screen.tiered.reforge.material.imprint_pool_choice");
    }

    private static void appendBehaviorChoiceBadge(List<ReforgeMaterialBadgeData.Badge> badges, ReforgeMaterial material) {
        ReforgeMaterial.BehaviorPoolChoice choice = material.getBehaviorPoolChoice();
        if (choice == null || choice.isEmpty()) return;

        List<List<ReforgeMaterialBadgeData.Badge>> options = new ArrayList<>();
        for (ReforgeMaterial.BehaviorPoolChoice.Candidate c : choice.getCandidates()) {
            if (c.getPool() == null) continue;
            List<ReforgeMaterialBadgeData.Badge> bundle = new ArrayList<>();
            for (ReforgeMaterial.BehaviorPool.Candidate frag : c.getPool().getCandidates()) {
                bundle.addAll(biasBadges(frag));
            }
            if (!bundle.isEmpty()) options.add(bundle);
        }
        appendChoiceOptions(badges, options, "screen.tiered.reforge.material.behavior_pool_choice");
    }

    private static List<ReforgeMaterialBadgeData.Badge> biasBadges(ReforgeMaterial.BehaviorPool.Candidate frag) {
        List<ReforgeMaterialBadgeData.Badge> out = new ArrayList<>();
        if (frag.getRarityBoost() != null && frag.getRarityBoost() > 0f) {
            out.add(badge("▲", Component.translatable("screen.tiered.reforge.material.rarity_boost",
                    (int) (frag.getRarityBoost() * 100)), 0xFF8E44AD, "screen.tiered.reforge.material.rarity_boost.desc"));
        }
        if (frag.getGuaranteedMinRarity() != null) {
            out.add(badge("≥", Component.translatable("screen.tiered.reforge.material.min_rarity",
                    capitalize(frag.getGuaranteedMinRarity())), 0xFF2E8B57, "screen.tiered.reforge.material.min_rarity.desc"));
        }
        if (frag.getMaxRarity() != null) {
            out.add(badge("≤", Component.translatable("screen.tiered.reforge.material.max_rarity",
                    capitalize(frag.getMaxRarity())), 0xFFB23A48, "screen.tiered.reforge.material.max_rarity.desc"));
        }
        if (frag.getGroupWeightMultipliers() != null) {
            frag.getGroupWeightMultipliers().forEach((group, mult) ->
                    out.add(badge("⚖", Component.translatable("screen.tiered.reforge.material.favor",
                            String.format(Locale.ROOT, "%.1f", mult), capitalize(group)), 0xFFB8860B,
                            "screen.tiered.reforge.material.favor.desc")));
        }
        return out;
    }

    private static void appendEffectBadges(List<ReforgeMaterialBadgeData.Badge> badges,
            @Nullable List<String> effectIds, ReforgeMaterial material) {
        appendEffectBadges(badges, effectIds, material, null);
    }

    private static void appendEffectBadges(List<ReforgeMaterialBadgeData.Badge> badges,
            @Nullable List<String> effectIds, ReforgeMaterial material,
            @Nullable RuneContentComponent comp) {
        if (effectIds == null) return;
        for (String effectId : effectIds) {
            ReforgeMaterialBadgeData.Badge b = effectBadge(effectId, material, comp);
            if (b != null) badges.add(b);
        }
    }

    @Nullable
    private static ReforgeMaterialBadgeData.Badge effectBadge(String effectId, ReforgeMaterial material,
            @Nullable RuneContentComponent comp) {
        ReforgeEffect eff = ReforgeEffectRegistry.get(effectId);
        if (!(eff instanceof DataEffect de)) return null;
        EffectDefinition ed = de.definition();
        if (material.isBadgeHidden(ed.getType())) return null;
        switch (ed.getType()) {
            case "repair" -> {
                Component repairLabel = ed.hasValueRange()
                        ? Component.translatable("screen.tiered.reforge.material.effect.repair.range",
                                Math.round(ed.getValueMin() * 100), Math.round(ed.getValueMax() * 100))
                        : Component.translatable("screen.tiered.reforge.material.effect.repair",
                                Math.round(ed.getValue() * 100));
                return badge("♥", repairLabel, 0xFF5BA85B,
                        effectDesc(ed, "screen.tiered.reforge.material.effect.repair.desc"));
            }
            case "grant_imprint" -> {
                Imprint imp = ImprintRegistry.get(ed.getImprint());
                if (imp == null) return null;
                String name = Component.translatable(imp.nameKey()).getString().toUpperCase(Locale.ROOT);
                return badge("✦", Component.translatable(
                        "screen.tiered.reforge.material.effect.grant_imprint", name),
                        imprintToFill(imp), effectDesc(ed, "screen.tiered.reforge.material.effect.grant_imprint.desc"));
            }
            case "extract" -> {
                return badge("⊗", Component.translatable(
                        "screen.tiered.reforge.material.effect.extract", Math.round(ed.getValue() * 100)),
                        0xFF8080C0, effectDesc(ed, "screen.tiered.reforge.material.effect.extract.desc"));
            }
            case "stabilize" -> {
                return badge("⬛", Component.translatable("screen.tiered.reforge.material.effect.stabilize"),
                        0xFF4A90D9, effectDesc(ed, "screen.tiered.reforge.material.effect.stabilize.desc"));
            }
            case "forget" -> {
                return badge("✗", Component.translatable("screen.tiered.reforge.material.effect.forget"),
                        0xFFB23A48, effectDesc(ed, "screen.tiered.reforge.material.effect.forget.desc"));
            }
            case "grant_rune_slot" -> {
                return badge("➕", Component.translatable("screen.tiered.reforge.material.effect.grant_rune_slot"),
                        0xFF5BA85B, effectDesc(ed, "screen.tiered.reforge.material.effect.grant_rune_slot.desc"));
            }
            case "overcharge" -> {
                Component label;
                if (comp != null && comp.rolledEffectParams() != null
                        && comp.rolledEffectParams().containsKey(effectId)) {
                    int cost = Math.max(1, (int) comp.getRolledParam(effectId, "extra_cost", 1f));
                    label = Component.translatable("screen.tiered.reforge.material.effect.overcharge_fixed", cost);
                } else {
                    int costMin = Math.max(1, (int) ed.getParam("extra_cost_min", ed.getParam("extra_cost", 1f)));
                    int costMax = Math.max(costMin, (int) ed.getParam("extra_cost_max",
                            ed.getParam("extra_cost", (float) costMin)));
                    label = costMin == costMax
                            ? Component.translatable("screen.tiered.reforge.material.effect.overcharge_fixed", costMin)
                            : Component.translatable("screen.tiered.reforge.material.effect.overcharge_range", costMin, costMax);
                }
                return badge("⚡", label, 0xFFFFAA00,
                        effectDesc(ed, "screen.tiered.reforge.material.effect.overcharge.desc"));
            }
            default -> {
                return null;
            }
        }
    }

    private static boolean hasRevealableDescriptions(ReforgeMaterial material) {
        ReforgeMaterialBadgeData data = buildBadges(material);
        if (data == null || data.badges() == null) return false;
        for (ReforgeMaterialBadgeData.Badge b : data.badges()) {
            if (b.description() != null) return true;
        }
        return false;
    }

    private static ReforgeMaterialBadgeData.Badge badge(String glyph, Component label, int fillColor, String descKey) {
        Component desc = descKey == null ? null
                : Component.translatable(descKey).withStyle(s -> s.withColor(ChatFormatting.DARK_GRAY).withItalic(true));
        return new ReforgeMaterialBadgeData.Badge(glyph, label, fillColor, desc);
    }

    private static String effectDesc(EffectDefinition ed, String fallbackKey) {
        return ed.getDescription() != null ? ed.getDescription() : fallbackKey;
    }

    private static boolean shiftDown() {
        return TieredKeybinds.descriptionsHeld();
    }

    public static boolean shiftHeld() {
        return shiftDown();
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String trimFloat(float value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
