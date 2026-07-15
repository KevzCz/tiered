package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import draylar.tiered.Tiered;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.api.RuneRollContext;
import draylar.tiered.api.effect.DataEffect;
import draylar.tiered.api.effect.EffectDefinition;
import draylar.tiered.api.effect.ReforgeEffect;
import draylar.tiered.api.effect.ReforgeEffectRegistry;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.registry.ModComponents;
import draylar.tiered.util.ReforgeMaterials;

public final class RuneContent {

    private RuneContent() {
    }

    private static boolean imprintPoolActive(ReforgeMaterial material) {
        return ConfigInit.imprintsEffectsAndBehaviorsEnabled() && material.hasImprintPool();
    }

    private static boolean effectPoolActive(ReforgeMaterial material) {
        return ConfigInit.imprintsEffectsAndBehaviorsEnabled() && material.hasEffectPool();
    }

    private static boolean biasPoolActive(ReforgeMaterial material) {
        return ConfigInit.biasPoolEnabled() && material.hasBehaviorPool();
    }

    public static void writeContent(ItemStack stack, List<ImprintComponent.Entry> group) {
        List<RuneContentComponent.Entry> entries = new ArrayList<>();
        for (ImprintComponent.Entry e : group) {
            entries.add(new RuneContentComponent.Entry(e.id(), e.value(), e.extraValues()));
        }
        stack.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(true, entries, List.of(), Map.of(), null));
    }

    public static boolean isRune(Item item) {
        ReforgeMaterial material = Tiered.REFORGE_MATERIAL_LOADER.getMaterial(item);
        return material != null && (imprintPoolActive(material) || effectPoolActive(material) || biasPoolActive(material));
    }

    public static boolean rollOnto(ItemStack stack, Random random) {
        return rollOnto(stack, random, null);
    }

    public static boolean rollOnto(ItemStack stack, Random random, RuneRollContext ctx) {
        if (stack == null || stack.isEmpty()) return false;

        RuneContentComponent existing = stack.get(ModComponents.RUNE_CONTENT);
        if (existing != null && existing.rolled()) return false;

        ReforgeMaterial material = ReforgeMaterials.resolve(stack);
        if (material == null || (!imprintPoolActive(material) && !effectPoolActive(material) && !biasPoolActive(material))) return false;

        Random rng = random == null ? new Random() : random;
        List<RuneContentComponent.Entry> rolled = new ArrayList<>();
        if (imprintPoolActive(material)) {
            for (ReforgeMaterial.ImprintPool pool : material.getImprintPools()) rolled.addAll(rollPool(pool, rng, ctx));

            ReforgeMaterial.ImprintPoolChoice choice = material.getImprintPoolChoice();
            if (choice != null && !choice.isEmpty()) {
                ReforgeMaterial.ImprintPool picked = choice.pick(rng);
                if (picked != null) rolled.addAll(rollPool(picked, rng, ctx));
            }
        }
        List<String> rolledEffects = effectPoolActive(material) ? material.rollEffects(rng) : List.of();
        Map<String, Map<String, Float>> rolledEffectParams = rollEffectParams(rolledEffects, rng);
        RuneContentComponent.RolledBias rolledBias = biasPoolActive(material) ? toBiasComponent(material.rollBehavior(rng)) : null;
        stack.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(true, rolled, rolledEffects, rolledEffectParams, rolledBias));
        return true;
    }

    private static Map<String, Map<String, Float>> rollEffectParams(List<String> effectIds, Random rng) {
        Map<String, Map<String, Float>> out = new LinkedHashMap<>();
        for (String effectId : effectIds) {
            ReforgeEffect eff = ReforgeEffectRegistry.get(effectId);
            if (!(eff instanceof DataEffect de)) continue;
            EffectDefinition ed = de.definition();
            if ("overcharge".equals(ed.getType())) {
                int costMin = Math.max(1, (int) ed.getParam("extra_cost_min", ed.getParam("extra_cost", 1f)));
                int costMax = Math.max(costMin, (int) ed.getParam("extra_cost_max", ed.getParam("extra_cost", (float) costMin)));
                int rolledCost = costMin >= costMax ? costMin : costMin + rng.nextInt(costMax - costMin + 1);
                Map<String, Float> params = new LinkedHashMap<>();
                params.put("extra_cost", (float) rolledCost);
                out.put(effectId, params);
            }
        }
        return out;
    }

    public static RuneContentComponent.RolledBias toBiasComponent(ReforgeMaterial.RolledBias b) {
        if (b == null) return null;
        return new RuneContentComponent.RolledBias(b.groups(), b.groupWeightMultipliers(), b.rarityBoost(),
                b.guaranteedMinRarity(), b.maxRarity());
    }

    public static List<ReforgeMaterial.Candidate> resolveCandidates(ReforgeMaterial.ImprintPool pool) {
        List<ReforgeMaterial.Candidate> remaining = new ArrayList<>(pool.getCandidates());
        List<String> groups = pool.getImprintGroups();
        if (!groups.isEmpty()) {
            Set<String> already = new HashSet<>();
            for (ReforgeMaterial.Candidate c : remaining) already.add(c.getImprint());
            boolean matchAll = groups.stream().anyMatch(g -> "all".equalsIgnoreCase(g));
            for (var e : Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().entrySet()) {
                boolean inAny = matchAll || groups.stream().anyMatch(g -> e.getValue().inGroup(g));
                if (inAny && already.add(e.getKey())) {
                    remaining.add(new ReforgeMaterial.Candidate(e.getKey(), null, null, 1));
                }
            }
        }
        return remaining;
    }

    private static List<RuneContentComponent.Entry> rollPool(ReforgeMaterial.ImprintPool pool, Random rng,
            RuneRollContext ctx) {
        List<RuneContentComponent.Entry> out = new ArrayList<>();

        int minBonus = pool.rollMinBonus(ctx);
        int maxBonus = pool.rollMaxBonus(ctx);
        float valueMinBonus = pool.valueMinBonus(ctx);
        float valueMaxBonus = pool.valueMaxBonus(ctx);

        for (String id : pool.getDefaultImprints()) {
            Imprint imprint = ImprintRegistry.get(id);
            if (imprint == null) continue;
            if (imprint instanceof DataImprint data) {
                out.add(new RuneContentComponent.Entry(id, data.rollValue(rng), data.rollExtras(rng)));
            } else {
                out.add(new RuneContentComponent.Entry(id, 0f));
            }
        }

        List<ReforgeMaterial.Candidate> remaining = new ArrayList<>(resolveCandidates(pool));
        boolean dupes = pool.allowsDuplicates();
        if (!remaining.isEmpty()) {
            int min = pool.getRollMin() + minBonus;
            int rawMax = pool.getRollMax() + maxBonus;
            int max = dupes ? rawMax : Math.min(rawMax, remaining.size());
            if (max < min) max = min;

            if (min == 0 && max > 0) {
                int candidateTotal = 0;
                for (ReforgeMaterial.Candidate c : remaining) candidateTotal += c.getWeight();
                Integer nw = pool.getNothingWeight();
                int nothingW = nw != null ? Math.max(0, nw) : candidateTotal;
                int total = candidateTotal + nothingW;
                if (total > 0 && rng.nextInt(total) < nothingW) return out;
                min = 1;
            }

            int count = min >= max ? min : min + rng.nextInt(max - min + 1);
            if (!dupes) count = Math.min(count, remaining.size());

            for (int n = 0; n < count && !remaining.isEmpty(); n++) {
                ReforgeMaterial.Candidate picked = weightedPick(remaining, rng);
                if (picked == null) break;
                if (!dupes) remaining.remove(picked);
                Imprint pickedImprint = ImprintRegistry.get(picked.getImprint());
                if (pickedImprint == null) continue;
                float value = rollValue(picked, rng, valueMinBonus, valueMaxBonus);
                Map<String, Float> extras = pickedImprint instanceof DataImprint data ? data.rollExtras(rng) : Map.of();
                out.add(new RuneContentComponent.Entry(picked.getImprint(), value, extras));
            }
        }
        return out;
    }

    private static ReforgeMaterial.Candidate weightedPick(List<ReforgeMaterial.Candidate> candidates, Random rng) {
        int total = 0;
        for (ReforgeMaterial.Candidate c : candidates) total += c.getWeight();
        if (total <= 0) return candidates.get(rng.nextInt(candidates.size()));
        int roll = rng.nextInt(total);
        for (ReforgeMaterial.Candidate c : candidates) {
            roll -= c.getWeight();
            if (roll < 0) return c;
        }
        return candidates.get(candidates.size() - 1);
    }

    private static float rollValue(ReforgeMaterial.Candidate c, Random rng, float valueMinBonus, float valueMaxBonus) {
        Imprint imprint = ImprintRegistry.get(c.getImprint());
        DataImprint data = imprint instanceof DataImprint di ? di : null;

        float lo, hi;
        if (!c.hasValueRange() && data != null) {
            lo = data.definition().getValueMin();
            hi = data.definition().getValueMax();
        } else {
            lo = c.getValueMin();
            hi = c.getValueMax();
        }

        lo += valueMinBonus;
        hi += valueMaxBonus;
        if (data != null) {
            float defMax = data.definition().getValueMax();
            if (defMax > 0f) { lo = Math.min(lo, defMax); hi = Math.min(hi, defMax); }
        }
        if (hi <= lo) return lo;
        return lo + rng.nextFloat() * (hi - lo);
    }
}
