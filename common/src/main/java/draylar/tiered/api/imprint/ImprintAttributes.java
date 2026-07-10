package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import draylar.tiered.Tiered;
import draylar.tiered.compat.ATCCompat;
import draylar.tiered.registry.ModComponents;
import dev.architectury.platform.Platform;

public final class ImprintAttributes {

    private ImprintAttributes() {
    }

    private static boolean hasAccessorySlots() {
        return Platform.isModLoaded("accessories")
                || Platform.isModLoaded("trinkets")
                || Platform.isModLoaded("curios");
    }

    public static final ThreadLocal<Player> CONTEXT_PLAYER = new ThreadLocal<>();

    public static final ThreadLocal<Map<String, Map<Integer, Float>>> OVERFLOW_ALLOCATION = new ThreadLocal<>();

    public static void buildOverflowAllocation(Player player) {
        Map<String, Map<Integer, Float>> result = new HashMap<>();

        Set<String> seenIds = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = player.getItemBySlot(slot);
            if (s == null || s.isEmpty()) continue;
            ImprintComponent c = s.get(ModComponents.IMPRINTS);
            if (c != null) seenIds.addAll(c.ids());
        }
        if (hasAccessorySlots()) {
            for (ItemStack s : ATCCompat.getEquippedAccessoryStacks(player)) {
                ImprintComponent c = s == null ? null : s.get(ModComponents.IMPRINTS);
                if (c != null) seenIds.addAll(c.ids());
            }
        }

        for (String imprintId : seenIds) {
            Imprint imprint = ImprintRegistry.get(imprintId);
            if (!(imprint instanceof DataImprint di)) continue;
            int maxPieces = di.definition().getMaxStacks();
            float valueCap = di.definition().getMaxBonus();
            if (maxPieces <= 0 && valueCap <= 0f) continue;

            List<int[]> contributions = new ArrayList<>();
            int slotIdx = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                ImprintComponent comp = stack == null ? null : stack.get(ModComponents.IMPRINTS);
                if (comp == null || !comp.has(imprintId)) { slotIdx++; continue; }
                if (!imprint.isEligible(stack, player)) { slotIdx++; continue; }
                float val = comp.valueOf(imprintId);
                if (val <= 0f) { slotIdx++; continue; }
                contributions.add(new int[]{slot.ordinal(), Float.floatToRawIntBits(val)});
                slotIdx++;
            }
            if (hasAccessorySlots()) {
                int accIdx = -1;
                for (ItemStack stack : ATCCompat.getEquippedAccessoryStacks(player)) {
                    ImprintComponent comp = stack == null ? null : stack.get(ModComponents.IMPRINTS);
                    if (comp != null && comp.has(imprintId) && imprint.isEligible(stack, player)) {
                        float val = comp.valueOf(imprintId);
                        if (val > 0f) contributions.add(new int[]{accIdx, Float.floatToRawIntBits(val)});
                    }
                    accIdx--;
                }
            }

            contributions.sort((a, b) -> Float.compare(Float.intBitsToFloat(b[1]), Float.intBitsToFloat(a[1])));

            Map<Integer, Float> allocation = new HashMap<>();
            int pieceCount = 0;
            float valueSpent = 0f;
            for (int[] entry : contributions) {
                int key = entry[0];
                float val = Float.intBitsToFloat(entry[1]);
                boolean countExceeded = maxPieces > 0 && pieceCount >= maxPieces;
                float valueHeadroom = valueCap > 0f ? valueCap - valueSpent : Float.MAX_VALUE;
                if (countExceeded || valueHeadroom <= 0f) {
                    allocation.put(key, 0f);
                } else {
                    float effective = Math.min(val, valueHeadroom);
                    allocation.put(key, effective);
                    valueSpent += effective;
                    pieceCount++;
                }
            }
            result.put(imprintId, allocation);
        }

        OVERFLOW_ALLOCATION.set(result);
    }

    public static float getAllocatedValue(ItemStack stack, String imprintId, float fullValue) {
        Player player = CONTEXT_PLAYER.get();
        if (player == null) return fullValue;
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (player.getItemBySlot(s) == stack) return getAllocatedValueBySlot(s.ordinal(), imprintId, fullValue);
        }
        return fullValue;
    }

    public static float getAllocatedValueBySlot(int slotOrdinal, String imprintId, float fullValue) {
        Map<String, Map<Integer, Float>> alloc = OVERFLOW_ALLOCATION.get();
        if (alloc == null) return fullValue;
        Map<Integer, Float> perItem = alloc.get(imprintId);
        if (perItem == null) return fullValue;
        if (slotOrdinal < 0) return fullValue;
        Float allocated = perItem.get(slotOrdinal);
        return allocated == null ? fullValue : allocated;
    }

    public static void apply(ItemStack stack, EquipmentSlot equipmentSlot, EquipmentSlotGroup attributeModifierSlot,
            BiConsumer<Holder<Attribute>, AttributeModifier> out) {
        ImprintComponent component = stack.get(ModComponents.IMPRINTS);
        if (component == null) return;

        Map<String, Integer> emittedCountPerId = new HashMap<>();
        Map<String, Float> emittedOnItemPerId = new HashMap<>();

        for (ImprintComponent.Entry entry : component.entries()) {
            Imprint imprint = ImprintRegistry.get(entry.id());
            if (imprint == null) continue;
            if (!imprint.isEligible(stack, CONTEXT_PLAYER.get())) continue;

            List<ImprintAttribute> attrList;
            if (imprint instanceof DataImprint di) {
                boolean allAnyWorn = di.definition().resolvedComponents().stream()
                        .filter(c -> c.isAttribute()).allMatch(c -> c.isAnyWorn());
                if (allAnyWorn && di.definition().isAttribute()) continue;

                int stackCap = di.definition().getMaxStacks();
                if (stackCap > 0) {
                    int used = emittedCountPerId.getOrDefault(entry.id(), 0);
                    if (used >= stackCap) continue;
                    emittedCountPerId.put(entry.id(), used + 1);
                }

                float allocated = getAllocatedValue(stack, entry.id(), entry.value());
                if (allocated <= 0f) continue;
                float alreadyOnItem = emittedOnItemPerId.getOrDefault(entry.id(), 0f);
                if (alreadyOnItem >= allocated) continue;
                emittedOnItemPerId.put(entry.id(), alreadyOnItem + entry.value());

                attrList = di.allAttributeData(entry, stack);
            } else {
                ImprintAttribute single = imprint.attributeData(entry);
                attrList = single == null ? List.of() : List.of(single);
            }

            for (ImprintAttribute attrData : attrList) {
                applyOne(stack, attrData, equipmentSlot, attributeModifierSlot, out);
            }
        }
    }


    private static void applyOne(ItemStack stack, ImprintAttribute data, EquipmentSlot equipmentSlot,
            EquipmentSlotGroup attributeModifierSlot,
            BiConsumer<Holder<Attribute>, AttributeModifier> out) {
        Optional<Holder.Reference<Attribute>> attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(data.attributeTypeId()));
        if (attr.isEmpty()) return;

        if (data.anyWornSlot()) return;

        if (data.requiredSlots() != null && data.requiredSlots().length > 0) {
            if (equipmentSlot != null && Arrays.asList(data.requiredSlots()).contains(equipmentSlot)) {
                out.accept(attr.get(), data.modifierForSlot(equipmentSlot));
                return;
            } else if (attributeModifierSlot != null) {
                Optional<EquipmentSlot> match = Arrays.stream(data.requiredSlots()).filter(attributeModifierSlot::test).findFirst();
                if (match.isPresent() && Tiered.isPreferredEquipmentSlot(stack, match.get())) {
                    out.accept(attr.get(), data.modifierForSlot(match.get()));
                    return;
                }
            }
        }

        if (data.isOnlyForAccessories()) return;

        if (data.optionalSlots() != null && data.optionalSlots().length > 0) {
            if (equipmentSlot != null && Arrays.asList(data.optionalSlots()).contains(equipmentSlot)
                    && Tiered.isPreferredEquipmentSlot(stack, equipmentSlot)) {
                out.accept(attr.get(), data.modifierForSlot(equipmentSlot));
            } else if (attributeModifierSlot != null && attributeModifierSlot != EquipmentSlotGroup.ANY
                    && attributeModifierSlot != EquipmentSlotGroup.HAND) {
                Optional<Holder.Reference<Attribute>> match2 = Arrays.stream(data.optionalSlots())
                        .filter(attributeModifierSlot::test)
                        .findFirst()
                        .flatMap(s -> Tiered.isPreferredEquipmentSlot(stack, s)
                                ? BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(data.attributeTypeId()))
                                : Optional.empty());
                if (match2.isPresent()) {
                    Optional<EquipmentSlot> matchSlot = Arrays.stream(data.optionalSlots()).filter(attributeModifierSlot::test).findFirst();
                    matchSlot.ifPresent(s -> out.accept(match2.get(), data.modifierForSlot(s)));
                }
            }
        }
    }

    private static final String CONSOLIDATED_PREFIX = "tiered:imprint_";

    private static ResourceLocation consolidatedModId(String imprintId) {
        ResourceLocation parsed = ResourceLocation.tryParse(imprintId);
        String safePath = parsed == null ? imprintId.replaceAll("[^a-z0-9_]", "_") : parsed.getPath();
        return ResourceLocation.fromNamespaceAndPath("tiered", "imprint_" + safePath);
    }

    public static final WeakHashMap<Player, Map<String, Float>>
            PLAYER_IMPRINT_VALUES = new WeakHashMap<>();

    public static void refreshPlayerModifiers(Player player) {
        var attrs = player.getAttributes();

        for (var entry : BuiltInRegistries.ATTRIBUTE.asHolderIdMap()) {
            var inst = attrs.getInstance(entry);
            if (inst == null) continue;
            inst.getModifiers().stream()
                    .filter(m -> m.id().toString().startsWith(CONSOLIDATED_PREFIX))
                    .toList()
                    .forEach(inst::removeModifier);
        }

        Map<String, Float> playerValues = new HashMap<>();
        PLAYER_IMPRINT_VALUES.put(player, playerValues);

        Set<String> imprintIds = new LinkedHashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = player.getItemBySlot(slot);
            if (s == null || s.isEmpty()) continue;
            ImprintComponent comp = s.get(ModComponents.IMPRINTS);
            if (comp != null) imprintIds.addAll(comp.ids());
        }
        if (hasAccessorySlots()) {
            for (ItemStack s : ATCCompat.getEquippedAccessoryStacks(player)) {
                ImprintComponent comp = s.get(ModComponents.IMPRINTS);
                if (comp != null) imprintIds.addAll(comp.ids());
            }
        }

        for (String imprintId : imprintIds) {
            Imprint imprint = ImprintRegistry.get(imprintId);
            if (!(imprint instanceof DataImprint di)) continue;
            if (!di.definition().isAttribute() || di.definition().getAttribute() == null) continue;
            if (!di.definition().isAnyWorn()) continue;

            float resolved = ImprintResolver.resolveValue(player, imprintId);
            if (resolved <= 0f) continue;

            Optional<Holder.Reference<Attribute>> attrEntry =
                    BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(di.definition().getAttribute()));
            if (attrEntry.isEmpty()) continue;

            ResourceLocation modId = consolidatedModId(imprintId);
            AttributeModifier.Operation op = di.attributeData(
                    new ImprintComponent.Entry(imprintId, resolved), null).operation();
            var inst = attrs.getInstance(attrEntry.get());
            if (inst != null) {
                inst.addTransientModifier(new AttributeModifier(modId, resolved, op));
                playerValues.put(di.definition().getAttribute(), resolved);
            }
        }
    }

    public static boolean isImprintAttribute(Player player, String attributeId) {
        var map = PLAYER_IMPRINT_VALUES.get(player);
        return map != null && map.containsKey(attributeId);
    }

    public static boolean hasConditionalAnyWornAttribute(Player player) {
        Set<String> ids = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = player.getItemBySlot(slot);
            ImprintComponent comp = s == null || s.isEmpty() ? null : s.get(ModComponents.IMPRINTS);
            if (comp != null) ids.addAll(comp.ids());
        }
        if (hasAccessorySlots()) {
            for (ItemStack s : ATCCompat.getEquippedAccessoryStacks(player)) {
                ImprintComponent comp = s == null ? null : s.get(ModComponents.IMPRINTS);
                if (comp != null) ids.addAll(comp.ids());
            }
        }
        for (String id : ids) {
            Imprint imprint = ImprintRegistry.get(id);
            if (!(imprint instanceof DataImprint di)) continue;
            if (!di.definition().isAnyWorn()) continue;
            if (di.definition().getActiveWhen() != null || di.definition().getInactiveWhen() != null) return true;
        }
        return false;
    }
}
