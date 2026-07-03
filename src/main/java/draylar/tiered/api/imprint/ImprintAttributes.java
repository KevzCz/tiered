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

import draylar.tiered.Tiered;
import draylar.tiered.compat.AccessoriesCompat;
import draylar.tiered.registry.ModComponents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ImprintAttributes {

    private ImprintAttributes() {
    }

    public static final ThreadLocal<PlayerEntity> CONTEXT_PLAYER = new ThreadLocal<>();

    public static final ThreadLocal<Map<String, Map<Integer, Float>>> OVERFLOW_ALLOCATION = new ThreadLocal<>();

    public static void buildOverflowAllocation(PlayerEntity player) {
        Map<String, Map<Integer, Float>> result = new HashMap<>();

        Set<String> seenIds = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = player.getEquippedStack(slot);
            if (s == null || s.isEmpty()) continue;
            ImprintComponent c = s.get(ModComponents.IMPRINTS);
            if (c != null) seenIds.addAll(c.ids());
        }
        if (FabricLoader.getInstance().isModLoaded("accessories")) {
            for (ItemStack s : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
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
                ItemStack stack = player.getEquippedStack(slot);
                ImprintComponent comp = stack == null ? null : stack.get(ModComponents.IMPRINTS);
                if (comp == null || !comp.has(imprintId)) { slotIdx++; continue; }
                if (!imprint.isEligible(stack, player)) { slotIdx++; continue; }
                float val = comp.valueOf(imprintId);
                if (val <= 0f) { slotIdx++; continue; }
                contributions.add(new int[]{slot.ordinal(), Float.floatToRawIntBits(val)});
                slotIdx++;
            }
            if (FabricLoader.getInstance().isModLoaded("accessories")) {
                int accIdx = -1;
                for (ItemStack stack : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
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
        PlayerEntity player = CONTEXT_PLAYER.get();
        if (player == null) return fullValue;
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (player.getEquippedStack(s) == stack) return getAllocatedValueBySlot(s.ordinal(), imprintId, fullValue);
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

    public static void apply(ItemStack stack, EquipmentSlot equipmentSlot, AttributeModifierSlot attributeModifierSlot,
            BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> out) {
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
            AttributeModifierSlot attributeModifierSlot,
            BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> out) {
        Optional<RegistryEntry.Reference<EntityAttribute>> attr = Registries.ATTRIBUTE.getEntry(Identifier.of(data.attributeTypeId()));
        if (attr.isEmpty()) return;

        if (data.anyWornSlot()) return;

        if (data.requiredSlots() != null && data.requiredSlots().length > 0) {
            if (equipmentSlot != null && Arrays.asList(data.requiredSlots()).contains(equipmentSlot)) {
                out.accept(attr.get(), data.modifierForSlot(equipmentSlot));
                return;
            } else if (attributeModifierSlot != null) {
                Optional<EquipmentSlot> match = Arrays.stream(data.requiredSlots()).filter(attributeModifierSlot::matches).findFirst();
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
            } else if (attributeModifierSlot != null && attributeModifierSlot != AttributeModifierSlot.ANY
                    && attributeModifierSlot != AttributeModifierSlot.HAND) {
                Optional<RegistryEntry.Reference<EntityAttribute>> match2 = Arrays.stream(data.optionalSlots())
                        .filter(attributeModifierSlot::matches)
                        .findFirst()
                        .flatMap(s -> Tiered.isPreferredEquipmentSlot(stack, s)
                                ? Registries.ATTRIBUTE.getEntry(Identifier.of(data.attributeTypeId()))
                                : Optional.empty());
                if (match2.isPresent()) {
                    Optional<EquipmentSlot> matchSlot = Arrays.stream(data.optionalSlots()).filter(attributeModifierSlot::matches).findFirst();
                    matchSlot.ifPresent(s -> out.accept(match2.get(), data.modifierForSlot(s)));
                }
            }
        }
    }

    private static final String CONSOLIDATED_PREFIX = "tiered:imprint_";

    private static Identifier consolidatedModId(String imprintId) {
        Identifier parsed = Identifier.tryParse(imprintId);
        String safePath = parsed == null ? imprintId.replaceAll("[^a-z0-9_]", "_") : parsed.getPath();
        return Identifier.of("tiered", "imprint_" + safePath);
    }

    public static final WeakHashMap<PlayerEntity, Map<String, Float>>
            PLAYER_IMPRINT_VALUES = new WeakHashMap<>();

    public static void refreshPlayerModifiers(PlayerEntity player) {
        var attrs = player.getAttributes();

        for (var entry : Registries.ATTRIBUTE.getIndexedEntries()) {
            var inst = attrs.getCustomInstance(entry);
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
            ItemStack s = player.getEquippedStack(slot);
            if (s == null || s.isEmpty()) continue;
            ImprintComponent comp = s.get(ModComponents.IMPRINTS);
            if (comp != null) imprintIds.addAll(comp.ids());
        }
        if (FabricLoader.getInstance().isModLoaded("accessories")) {
            for (ItemStack s : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
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

            Optional<RegistryEntry.Reference<EntityAttribute>> attrEntry =
                    Registries.ATTRIBUTE.getEntry(Identifier.of(di.definition().getAttribute()));
            if (attrEntry.isEmpty()) continue;

            Identifier modId = consolidatedModId(imprintId);
            EntityAttributeModifier.Operation op = di.attributeData(
                    new ImprintComponent.Entry(imprintId, resolved), null).operation();
            var inst = attrs.getCustomInstance(attrEntry.get());
            if (inst != null) {
                inst.addTemporaryModifier(new EntityAttributeModifier(modId, resolved, op));
                playerValues.put(di.definition().getAttribute(), resolved);
            }
        }
    }

    public static boolean isImprintAttribute(PlayerEntity player, String attributeId) {
        var map = PLAYER_IMPRINT_VALUES.get(player);
        return map != null && map.containsKey(attributeId);
    }

    public static boolean hasConditionalAnyWornAttribute(PlayerEntity player) {
        Set<String> ids = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = player.getEquippedStack(slot);
            ImprintComponent comp = s == null || s.isEmpty() ? null : s.get(ModComponents.IMPRINTS);
            if (comp != null) ids.addAll(comp.ids());
        }
        if (FabricLoader.getInstance().isModLoaded("accessories")) {
            for (ItemStack s : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
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
