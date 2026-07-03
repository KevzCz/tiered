package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import draylar.tiered.api.Cooldowns;
import draylar.tiered.api.imprint.behavior.ImprintBehavior;
import draylar.tiered.compat.AccessoriesCompat;
import draylar.tiered.registry.ModComponents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class ImprintResolver {

    private static final boolean ACCESSORIES_LOADED = FabricLoader.getInstance().isModLoaded("accessories");

    private ImprintResolver() {
    }

    public static boolean isActive(PlayerEntity player, String imprintId) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (imprint == null) return false;
        return !collectPerItemValues(player, imprint, imprintId).isEmpty();
    }

    public static float resolveValue(PlayerEntity player, String imprintId) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (imprint == null) return 0f;

        List<Float> perItem = collectPerItemValues(player, imprint, imprintId);
        if (perItem.isEmpty()) return 0f;

        float combined = switch (imprint.combineMode()) {
            case ADDITIVE -> {
                float sum = 0f;
                for (float v : perItem) sum += v;
                yield sum;
            }
            case HIGHEST, UNIQUE -> {
                float best = 0f;
                for (float v : perItem) best = Math.max(best, v);
                yield best;
            }
        };

        if (imprint instanceof DataImprint data) {
            int maxPieces = data.definition().getMaxStacks();
            if (maxPieces > 0 && perItem.size() > maxPieces) {
                List<Float> sorted = new ArrayList<>(perItem);
                sorted.sort((a, b) -> Float.compare(b, a));
                combined = 0f;
                for (int i = 0; i < maxPieces; i++) combined += sorted.get(i);
            }
            float bonus = data.definition().getMaxBonus();
            if (bonus > 0f && combined > bonus) combined = bonus;
        }
        return combined;
    }

    public static float resolveExtraValue(PlayerEntity player, String imprintId, String key) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (!(imprint instanceof DataImprint data)) return 0f;

        ImprintScope scope = imprint.scope();
        List<Float> perItem = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!scope.includesEquipment(slot)) continue;
            addExtraIfPresent(player.getEquippedStack(slot), imprint, imprintId, key, perItem, player);
        }
        if (scope.includesAccessories() && ACCESSORIES_LOADED) {
            for (ItemStack stack : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
                addExtraIfPresent(stack, imprint, imprintId, key, perItem, player);
            }
        }
        if (perItem.isEmpty()) return 0f;

        float combined = switch (imprint.combineMode()) {
            case ADDITIVE -> {
                float sum = 0f;
                for (float v : perItem) sum += v;
                yield sum;
            }
            case HIGHEST, UNIQUE -> {
                float best = 0f;
                for (float v : perItem) best = Math.max(best, v);
                yield best;
            }
        };

        float cap = data.maxExtraValue(key);
        if (cap > 0f && combined > cap) combined = cap;
        else if (cap < 0f && combined < cap) combined = cap;
        return combined;
    }

    private static void addExtraIfPresent(ItemStack stack, Imprint imprint, String imprintId, String key,
            List<Float> out, PlayerEntity player) {
        if (stack == null || stack.isEmpty()) return;
        ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
        if (comp == null || !comp.has(imprintId)) return;
        if (!imprint.isEligible(stack, player)) return;
        out.add(comp.extraValueOf(imprintId, key));
    }

    public static float resolveDataMeleeDamageFraction(PlayerEntity player) {
        return resolveDataMeleeDamageFraction(player, null);
    }

    public static float resolveDataMeleeDamageFraction(PlayerEntity player, LivingEntity target) {
        return accumulateFraction(player, (data, resolved) -> data.totalMeleeDamageFraction(player, target, resolved));
    }

    public static float resolveDataRangedDamageFraction(PlayerEntity player) {
        return resolveDataRangedDamageFraction(player, null);
    }

    public static float resolveDataRangedDamageFraction(PlayerEntity player, LivingEntity target) {
        return accumulateFraction(player, (data, resolved) -> data.totalRangedDamageFraction(player, target, resolved));
    }

    public static float resolveDataMagicDamageFraction(PlayerEntity player, DamageSource source) {
        return resolveDataMagicDamageFraction(player, null, source);
    }

    public static float resolveDataMagicDamageFraction(PlayerEntity player, LivingEntity target, DamageSource source) {
        return accumulateFraction(player, (data, resolved) -> data.totalMagicDamageFraction(player, target, source, resolved));
    }

    public static float resolveDataBonusMeleeDamage(PlayerEntity player, LivingEntity target, float amount) {
        return accumulateFraction(player, (data, resolved) -> data.totalBonusMeleeDamage(player, target, amount, resolved));
    }

    @FunctionalInterface
    private interface FractionSource {
        float of(DataImprint data, float resolved);
    }

    private static float accumulateFraction(PlayerEntity player, FractionSource source) {
        Set<String> seen = new HashSet<>();
        float total = 0f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            total += accumulate(player, player.getEquippedStack(slot), seen, source);
        }
        if (ACCESSORIES_LOADED) {
            for (ItemStack stack : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
                total += accumulate(player, stack, seen, source);
            }
        }
        return total;
    }

    private static float accumulate(PlayerEntity player, ItemStack stack, Set<String> seen, FractionSource source) {
        if (stack == null || stack.isEmpty()) return 0f;
        ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
        if (comp == null) return 0f;
        float total = 0f;
        for (String id : comp.ids()) {
            if (!seen.add(id)) continue;
            Imprint imprint = ImprintRegistry.get(id);
            if (!(imprint instanceof DataImprint data)) continue;
            if (data.behaviors().isEmpty()) continue;
            float resolved = resolveValue(player, id);
            if (resolved == 0f) continue;
            total += source.of(data, resolved);
        }
        return total;
    }

    public static int stackCount(PlayerEntity player, String imprintId) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (imprint == null) return 0;
        return collectPerItemValues(player, imprint, imprintId).size();
    }

    public static float rawSum(PlayerEntity player, String imprintId) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (imprint == null) return 0f;
        List<Float> perItem = collectPerItemValues(player, imprint, imprintId);
        float sum = 0f;
        for (float v : perItem) sum += v;
        return sum;
    }

    public static float maxCombinedValue(String imprintId) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (imprint instanceof DataImprint data) return data.maxCombinedValue();
        return 0f;
    }

    public static void dispatchDamageTaken(PlayerEntity player, float amount,
            DamageSource source) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onDamageTaken(player, amount, source, resolved, params));
    }

    public static void dispatchDamageDealt(PlayerEntity player, LivingEntity target,
            float amount) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onDamageDealt(player, target, amount, resolved, params));
    }

    public static void dispatchKill(PlayerEntity player, LivingEntity target) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onKill(player, target, resolved, params));
    }

    public static void dispatchProjectileDamageDealt(PlayerEntity player, LivingEntity target,
            float amount) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onProjectileDamageDealt(player, target, amount, resolved, params));
    }

    public static void dispatchProjectileKill(PlayerEntity player, LivingEntity target) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onProjectileKill(player, target, resolved, params));
    }

    public static void dispatchSpellCast(PlayerEntity player) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onSpellCast(player, resolved, params));
    }

    public static void dispatchSpellHeal(PlayerEntity player, LivingEntity target, float amount) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onSpellHeal(player, target, amount, resolved, params));
    }

    public static void dispatchCustom(PlayerEntity player, String eventId, float value) {
        dispatchEvent(player, (data, behavior, resolved, params) ->
                behavior.onCustomEvent(player, eventId, value, resolved, params));
    }

    private static final Map<PlayerEntity, Set<String>> EQUIPPED_SNAPSHOT =
            new WeakHashMap<>();

    public static void serverTick(PlayerEntity player) {
        Cooldowns.tick(player);
        Set<String> current = new HashSet<>();
        Map<String, Float> resolvedById = new HashMap<>();
        dispatchEvent(player, (data, behavior, resolved, params) -> {
            behavior.tick(player, resolved, params);
            String id = data.definition().getId();
            if (id != null) { current.add(id); resolvedById.putIfAbsent(id, resolved); }
        });

        Set<String> previous = EQUIPPED_SNAPSHOT.get(player);
        if (previous == null) previous = Set.of();

        for (String id : current) {
            if (!previous.contains(id)) fireEquipEvent(player, id, resolvedById.getOrDefault(id, 0f), true);
        }
        for (String id : previous) {
            if (!current.contains(id)) fireEquipEvent(player, id, 0f, false);
        }

        if (current.isEmpty()) EQUIPPED_SNAPSHOT.remove(player);
        else EQUIPPED_SNAPSHOT.put(player, current);
    }

    private static void fireEquipEvent(PlayerEntity player, String imprintId, float resolved, boolean equip) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (!(imprint instanceof DataImprint data) || data.behaviors().isEmpty()) return;
        var behaviors = data.behaviors();
        for (int i = 0; i < behaviors.size(); i++) {
            var behavior = behaviors.get(i);
            if (behavior == null) continue;
            Map<String, Float> params = data.paramsForBehavior(i);
            if (equip) behavior.onEquip(player, resolved, params);
            else behavior.onUnequip(player, resolved, params);
        }
    }

    @FunctionalInterface
    private interface EventDispatch {
        void fire(DataImprint data, ImprintBehavior behavior,
                float resolved, Map<String, Float> params);
    }

    private static void dispatchEvent(PlayerEntity player, EventDispatch dispatch) {
        Set<String> seen = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            collectAndDispatch(player, player.getEquippedStack(slot), seen, dispatch);
        }
        if (ACCESSORIES_LOADED) {
            for (ItemStack stack : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
                collectAndDispatch(player, stack, seen, dispatch);
            }
        }
    }

    private static void collectAndDispatch(PlayerEntity player, ItemStack stack,
            Set<String> seen, EventDispatch dispatch) {
        if (stack == null || stack.isEmpty()) return;
        ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
        if (comp == null) return;
        for (String id : comp.ids()) {
            if (!seen.add(id)) continue;
            Imprint imprint = ImprintRegistry.get(id);
            if (!(imprint instanceof DataImprint data)) continue;
            if (data.behaviors().isEmpty()) continue;
            if (!imprint.isEligible(stack, player)) continue;
            float resolved = resolveValue(player, id);
            if (resolved == 0f && data.definition().getValueMin() != 0f) continue;
            var behaviors = data.behaviors();
            for (int i = 0; i < behaviors.size(); i++) {
                var behavior = behaviors.get(i);
                if (behavior != null) {
                    Map<String, Float> params = data.paramsForBehavior(i);
                    dispatch.fire(data, behavior, resolved, params);
                }
            }
        }
    }

    private static List<Float> collectPerItemValues(PlayerEntity player, Imprint imprint, String imprintId) {
        ImprintScope scope = imprint.scope();
        List<Float> values = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!scope.includesEquipment(slot)) continue;
            ItemStack stack = player.getEquippedStack(slot);
            addIfPresent(stack, imprint, imprintId, values, player);
        }

        if (scope.includesAccessories() && ACCESSORIES_LOADED) {
            for (ItemStack stack : AccessoriesCompat.getEquippedAccessoryStacks(player)) {
                addIfPresent(stack, imprint, imprintId, values, player);
            }
        }

        return values;
    }

    private static void addIfPresent(ItemStack stack, Imprint imprint, String imprintId, List<Float> out, PlayerEntity player) {
        if (stack == null || stack.isEmpty()) return;
        ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
        if (comp == null || !comp.has(imprintId)) return;
        if (!imprint.isEligible(stack, player)) return;
        out.add(comp.valueOf(imprintId));
    }
}
