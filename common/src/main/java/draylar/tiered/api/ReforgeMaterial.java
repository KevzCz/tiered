package draylar.tiered.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.google.gson.annotations.SerializedName;
import draylar.tiered.api.effect.ReforgeEffects;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.ReforgeMaterialLoader;
import org.jetbrains.annotations.Nullable;

public class ReforgeMaterial {

    private final String item;
    @Nullable
    private final List<String> groups;
    @Nullable
    @SerializedName("group_weight_multipliers")
    private final Map<String, Float> groupWeightMultipliers;
    @SerializedName("rarity_boost")
    private final float rarityBoost;
    @Nullable
    @SerializedName("guaranteed_min_rarity")
    private final String guaranteedMinRarity;
    @Nullable
    @SerializedName("max_rarity")
    private final String maxRarity;
    @Nullable
    private final List<String> compatible;
    @Nullable
    private final List<String> incompatible;
    @Nullable
    private final List<String> effects;
    @Nullable
    @SerializedName("effect_params")
    private final Map<String, Map<String, Float>> effectParams;
    @Nullable
    private final String color;
    @Nullable
    private final List<String> description;
    @Nullable
    @SerializedName("imprint_pool")
    private final ImprintPool imprintPool;
    @Nullable
    @SerializedName("effect_pool")
    private final EffectPool effectPool;
    @Nullable
    @SerializedName("behavior_pool")
    private final BehaviorPool behaviorPool;
    @Nullable
    @SerializedName("imprint_pools")
    private final List<ImprintPool> imprintPools;

    @Nullable
    @SerializedName("imprint_pool_choices")
    private final ImprintPoolChoice imprintPoolChoice;
    @Nullable
    @SerializedName("effect_pools")
    private final List<EffectPool> effectPools;

    @Nullable
    @SerializedName("effect_pool_choices")
    private final EffectPoolChoice effectPoolChoice;
    @Nullable
    @SerializedName("behavior_pools")
    private final List<BehaviorPool> behaviorPools;

    @Nullable
    @SerializedName("behavior_pool_choices")
    private final BehaviorPoolChoice behaviorPoolChoice;
    @SerializedName("skip_reforge")
    private final boolean skipReforge;
    @SerializedName("skip_base_item")
    private final boolean skipBaseItem;
    @Nullable
    @SerializedName("hidden_badges")
    private final List<String> hiddenBadges;

    public ReforgeMaterial(String item, @Nullable List<String> groups, @Nullable Map<String, Float> groupWeightMultipliers,
            @Nullable Float rarityBoost, @Nullable String guaranteedMinRarity, @Nullable String maxRarity,
            @Nullable List<String> compatible, @Nullable List<String> incompatible, @Nullable List<String> effects,
            @Nullable Map<String, Map<String, Float>> effectParams, @Nullable String color, @Nullable List<String> description,
            @Nullable ImprintPool imprintPool) {
        this(item, groups, groupWeightMultipliers, rarityBoost, guaranteedMinRarity, maxRarity,
                compatible, incompatible, effects, effectParams, color, description, imprintPool, false, false);
    }

    public ReforgeMaterial(String item, @Nullable List<String> groups, @Nullable Map<String, Float> groupWeightMultipliers,
            @Nullable Float rarityBoost, @Nullable String guaranteedMinRarity, @Nullable String maxRarity,
            @Nullable List<String> compatible, @Nullable List<String> incompatible, @Nullable List<String> effects,
            @Nullable Map<String, Map<String, Float>> effectParams, @Nullable String color, @Nullable List<String> description,
            @Nullable ImprintPool imprintPool, boolean skipReforge, boolean skipBaseItem) {
        this(item, groups, groupWeightMultipliers, rarityBoost, guaranteedMinRarity, maxRarity,
                compatible, incompatible, effects, effectParams, color, description, imprintPool, skipReforge, skipBaseItem, null, null);
    }

    public ReforgeMaterial(String item, @Nullable List<String> groups, @Nullable Map<String, Float> groupWeightMultipliers,
            @Nullable Float rarityBoost, @Nullable String guaranteedMinRarity, @Nullable String maxRarity,
            @Nullable List<String> compatible, @Nullable List<String> incompatible, @Nullable List<String> effects,
            @Nullable Map<String, Map<String, Float>> effectParams, @Nullable String color, @Nullable List<String> description,
            @Nullable ImprintPool imprintPool, boolean skipReforge, boolean skipBaseItem,
            @Nullable EffectPool effectPool, @Nullable BehaviorPool behaviorPool) {
        this.item = item;
        this.groups = groups;
        this.groupWeightMultipliers = groupWeightMultipliers;
        this.rarityBoost = rarityBoost == null ? 0f : Math.max(0f, Math.min(1f, rarityBoost));
        this.guaranteedMinRarity = guaranteedMinRarity;
        this.maxRarity = maxRarity;
        this.compatible = compatible;
        this.incompatible = incompatible;
        this.effects = effects;
        this.effectParams = effectParams;
        this.color = color;
        this.description = description;
        this.imprintPool = imprintPool;
        this.effectPool = effectPool;
        this.behaviorPool = behaviorPool;
        this.imprintPools = null;
        this.imprintPoolChoice = null;
        this.effectPools = null;
        this.effectPoolChoice = null;
        this.behaviorPools = null;
        this.behaviorPoolChoice = null;
        this.hiddenBadges = null;
        this.skipReforge = skipReforge;
        this.skipBaseItem = skipBaseItem;
    }

    public static class ImprintPool {
        @SerializedName("roll_min")
        private final Integer rollMin;
        @SerializedName("roll_max")
        private final Integer rollMax;
        private final List<Candidate> candidates;
        @SerializedName("imprint_group")
        private final String imprintGroup;

        @SerializedName("imprint_groups")
        private final List<String> imprintGroups;
        @SerializedName("default_imprints")
        private final List<String> defaultImprints;
        @SerializedName("allow_duplicates")
        private final Boolean allowDuplicates;
        @SerializedName("nothing_weight")
        private final Integer nothingWeight;
        @SerializedName("roll_scaling")
        private final List<RollScaling> rollScaling;

        public ImprintPool(Integer rollMin, Integer rollMax, List<Candidate> candidates, String imprintGroup,
                List<String> defaultImprints, Boolean allowDuplicates, Integer nothingWeight) {
            this(rollMin, rollMax, candidates, imprintGroup, defaultImprints, allowDuplicates, nothingWeight, null);
        }

        public ImprintPool(Integer rollMin, Integer rollMax, List<Candidate> candidates, String imprintGroup,
                List<String> defaultImprints, Boolean allowDuplicates, Integer nothingWeight,
                List<RollScaling> rollScaling) {
            this.rollMin = rollMin;
            this.rollMax = rollMax;
            this.candidates = candidates;
            this.imprintGroup = imprintGroup;
            this.imprintGroups = null;
            this.defaultImprints = defaultImprints;
            this.allowDuplicates = allowDuplicates;
            this.nothingWeight = nothingWeight;
            this.rollScaling = rollScaling;
        }

        public int getRollMin() {
            return rollMin == null ? 1 : Math.max(0, rollMin);
        }

        public int getRollMax() {
            return rollMax == null ? getRollMin() : Math.max(getRollMin(), rollMax);
        }

        public List<Candidate> getCandidates() {
            return candidates == null ? List.of() : candidates;
        }

        @Nullable
        public String getImprintGroup() {
            return imprintGroup;
        }

        public List<String> getImprintGroups() {
            List<String> all = new ArrayList<>();
            if (imprintGroup != null && !imprintGroup.isBlank()) all.add(imprintGroup);
            if (imprintGroups != null) {
                for (String g : imprintGroups) {
                    if (g != null && !g.isBlank() && !all.contains(g)) all.add(g);
                }
            }
            return all;
        }

        public List<String> getDefaultImprints() {
            return defaultImprints == null ? List.of() : defaultImprints;
        }

        public boolean allowsDuplicates() {
            return allowDuplicates != null && allowDuplicates;
        }

        public Integer getNothingWeight() {
            return nothingWeight;
        }

        public List<RollScaling> getRollScaling() {
            return rollScaling == null ? List.of() : rollScaling;
        }

        public int rollMinBonus(RuneRollContext ctx) {
            if (ctx == null) return 0;
            int total = 0;
            for (RollScaling s : getRollScaling()) total += s.minBonus(ctx);
            return total;
        }

        public int rollMaxBonus(RuneRollContext ctx) {
            if (ctx == null) return 0;
            int total = 0;
            for (RollScaling s : getRollScaling()) total += s.maxBonus(ctx);
            return total;
        }

        public float valueMinBonus(RuneRollContext ctx) {
            if (ctx == null) return 0f;
            float total = 0f;
            for (RollScaling s : getRollScaling()) total += s.valueMinBonus(ctx);
            return total;
        }

        public float valueMaxBonus(RuneRollContext ctx) {
            if (ctx == null) return 0f;
            float total = 0f;
            for (RollScaling s : getRollScaling()) total += s.valueMaxBonus(ctx);
            return total;
        }
    }

    public static class ImprintPoolChoice {
        private final List<ReforgeMaterial.ImprintPoolChoice.Candidate> candidates;

        public ImprintPoolChoice(List<ReforgeMaterial.ImprintPoolChoice.Candidate> candidates) {
            this.candidates = candidates;
        }

        public List<ReforgeMaterial.ImprintPoolChoice.Candidate> getCandidates() {
            return candidates == null ? List.of() : candidates;
        }

        public boolean isEmpty() {
            return getCandidates().isEmpty();
        }

        @Nullable
        public ImprintPool pick(Random rng) {
            List<ReforgeMaterial.ImprintPoolChoice.Candidate> pool = getCandidates();
            if (pool.isEmpty()) return null;
            int total = 0;
            for (ReforgeMaterial.ImprintPoolChoice.Candidate c : pool) total += c.getWeight();
            if (total <= 0) return pool.get(rng.nextInt(pool.size())).getPool();
            int roll = rng.nextInt(total);
            for (ReforgeMaterial.ImprintPoolChoice.Candidate c : pool) {
                roll -= c.getWeight();
                if (roll < 0) return c.getPool();
            }
            return pool.get(pool.size() - 1).getPool();
        }

        public static class Candidate {
            private final Integer weight;
            private final ImprintPool pool;

            public Candidate(Integer weight, ImprintPool pool) {
                this.weight = weight;
                this.pool = pool;
            }

            public int getWeight() {
                return weight == null ? 1 : Math.max(1, weight);
            }

            @Nullable
            public ImprintPool getPool() {
                return pool;
            }
        }
    }

    public static class EffectPoolChoice {
        private final List<ReforgeMaterial.EffectPoolChoice.Candidate> candidates;

        public EffectPoolChoice(List<ReforgeMaterial.EffectPoolChoice.Candidate> candidates) {
            this.candidates = candidates;
        }

        public List<ReforgeMaterial.EffectPoolChoice.Candidate> getCandidates() {
            return candidates == null ? List.of() : candidates;
        }

        public boolean isEmpty() {
            return getCandidates().isEmpty();
        }

        @Nullable
        public EffectPool pick(Random rng) {
            return weightedPickChoice(getCandidates(), rng, ReforgeMaterial.EffectPoolChoice.Candidate::getWeight, ReforgeMaterial.EffectPoolChoice.Candidate::getPool);
        }

        public static class Candidate {
            private final Integer weight;
            private final EffectPool pool;

            public Candidate(Integer weight, EffectPool pool) {
                this.weight = weight;
                this.pool = pool;
            }

            public int getWeight() { return weight == null ? 1 : Math.max(1, weight); }

            @Nullable
            public EffectPool getPool() { return pool; }
        }
    }

    public static class BehaviorPoolChoice {
        private final List<ReforgeMaterial.BehaviorPoolChoice.Candidate> candidates;

        public BehaviorPoolChoice(List<ReforgeMaterial.BehaviorPoolChoice.Candidate> candidates) {
            this.candidates = candidates;
        }

        public List<ReforgeMaterial.BehaviorPoolChoice.Candidate> getCandidates() {
            return candidates == null ? List.of() : candidates;
        }

        public boolean isEmpty() {
            return getCandidates().isEmpty();
        }

        @Nullable
        public BehaviorPool pick(Random rng) {
            return weightedPickChoice(getCandidates(), rng, ReforgeMaterial.BehaviorPoolChoice.Candidate::getWeight, ReforgeMaterial.BehaviorPoolChoice.Candidate::getPool);
        }

        public static class Candidate {
            private final Integer weight;
            private final BehaviorPool pool;

            public Candidate(Integer weight, BehaviorPool pool) {
                this.weight = weight;
                this.pool = pool;
            }

            public int getWeight() { return weight == null ? 1 : Math.max(1, weight); }

            @Nullable
            public BehaviorPool getPool() { return pool; }
        }
    }

    private static <C, P> P weightedPickChoice(List<C> candidates, Random rng,
            ToIntFunction<C> weightOf, Function<C, P> poolOf) {
        if (candidates.isEmpty()) return null;
        int total = 0;
        for (C c : candidates) total += weightOf.applyAsInt(c);
        if (total <= 0) return poolOf.apply(candidates.get(rng.nextInt(candidates.size())));
        int roll = rng.nextInt(total);
        for (C c : candidates) {
            roll -= weightOf.applyAsInt(c);
            if (roll < 0) return poolOf.apply(c);
        }
        return poolOf.apply(candidates.get(candidates.size() - 1));
    }

    public static class RollScaling {
        private final String source;
        private final String tag;
        private final String dimension;
        private final Float per;
        @SerializedName("min_bonus_per") private final Integer minBonusPer;
        @SerializedName("max_bonus_per") private final Integer maxBonusPer;
        @SerializedName("min_bonus_cap") private final Integer minBonusCap;
        @SerializedName("max_bonus_cap") private final Integer maxBonusCap;
        @SerializedName("value_min_bonus_per") private final Float valueMinBonusPer;
        @SerializedName("value_max_bonus_per") private final Float valueMaxBonusPer;
        @SerializedName("value_min_bonus_cap") private final Float valueMinBonusCap;
        @SerializedName("value_max_bonus_cap") private final Float valueMaxBonusCap;

        public RollScaling(String source, String tag, String dimension, Float per,
                Integer minBonusPer, Integer maxBonusPer, Integer minBonusCap, Integer maxBonusCap,
                Float valueMinBonusPer, Float valueMaxBonusPer, Float valueMinBonusCap, Float valueMaxBonusCap) {
            this.source = source;
            this.tag = tag;
            this.dimension = dimension;
            this.per = per;
            this.minBonusPer = minBonusPer;
            this.maxBonusPer = maxBonusPer;
            this.minBonusCap = minBonusCap;
            this.maxBonusCap = maxBonusCap;
            this.valueMinBonusPer = valueMinBonusPer;
            this.valueMaxBonusPer = valueMaxBonusPer;
            this.valueMinBonusCap = valueMinBonusCap;
            this.valueMaxBonusCap = valueMaxBonusCap;
        }

        private float steps(RuneRollContext ctx) {
            float value = switch (source == null ? "" : source) {
                case "entity_max_health" -> ctx.entityMaxHealth();
                case "entity_tag" -> ctx.entityInTag(tag) ? 1f : 0f;
                case "dimension" -> ctx.isDimension(dimension) ? 1f : 0f;
                default -> 0f;
            };
            float step = per == null || per <= 0f ? 1f : per;
            return value <= 0f ? 0f : (float) Math.floor(value / step);
        }

        int minBonus(RuneRollContext ctx) {
            return intBonus(ctx, minBonusPer, minBonusCap);
        }

        int maxBonus(RuneRollContext ctx) {
            return intBonus(ctx, maxBonusPer, maxBonusCap);
        }

        float valueMinBonus(RuneRollContext ctx) {
            return floatBonus(ctx, valueMinBonusPer, valueMinBonusCap);
        }

        float valueMaxBonus(RuneRollContext ctx) {
            return floatBonus(ctx, valueMaxBonusPer, valueMaxBonusCap);
        }

        private int intBonus(RuneRollContext ctx, Integer per, Integer cap) {
            if (per == null || per == 0) return 0;
            int bonus = (int) (steps(ctx) * per);
            if (cap != null) bonus = Math.max(0, Math.min(cap, bonus));
            return Math.max(0, bonus);
        }

        private float floatBonus(RuneRollContext ctx, Float per, Float cap) {
            if (per == null || per == 0f) return 0f;
            float bonus = steps(ctx) * per;
            if (cap != null) bonus = Math.max(0f, Math.min(cap, bonus));
            return Math.max(0f, bonus);
        }
    }

    public static class Candidate {
        @SerializedName("imprint")
        private final String imprint;
        @SerializedName("value_min")
        private final Float valueMin;
        @SerializedName("value_max")
        private final Float valueMax;
        private final Integer weight;

        public Candidate(String imprint, Float valueMin, Float valueMax, Integer weight) {
            this.imprint = imprint;
            this.valueMin = valueMin;
            this.valueMax = valueMax;
            this.weight = weight;
        }

        public String getImprint() {
            return imprint;
        }

        public float getValueMin() {
            return valueMin == null ? 0f : valueMin;
        }

        public float getValueMax() {
            return valueMax == null ? getValueMin() : Math.max(getValueMin(), valueMax);
        }

        public boolean hasValueRange() {
            return valueMin != null || valueMax != null;
        }

        public int getWeight() {
            return weight == null ? 1 : Math.max(1, weight);
        }
    }

    public String getItem() {
        return item;
    }

    @Nullable
    public List<String> getGroups() {
        return groups;
    }

    public boolean hasGroupFilter() {
        return groups != null && !groups.isEmpty();
    }

    @Nullable
    public Map<String, Float> getGroupWeightMultipliers() {
        return ConfigInit.biasPoolEnabled() ? groupWeightMultipliers : null;
    }

    public float getRarityBoost() {
        return ConfigInit.biasPoolEnabled() ? rarityBoost : 0f;
    }

    @Nullable
    public String getGuaranteedMinRarity() {
        return ConfigInit.biasPoolEnabled() ? guaranteedMinRarity : null;
    }

    @Nullable
    public String getMaxRarity() {
        return ConfigInit.biasPoolEnabled() ? maxRarity : null;
    }

    @Nullable
    public List<String> getCompatible() {
        return compatible;
    }

    @Nullable
    public List<String> getIncompatible() {
        return incompatible;
    }

    public boolean hasCompatibilityRules() {
        return (compatible != null && !compatible.isEmpty()) || (incompatible != null && !incompatible.isEmpty());
    }

    @Nullable
    public List<String> getEffects() {
        return effects;
    }

    @Nullable
    public Map<String, Map<String, Float>> getEffectParams() {
        return effectParams;
    }

    public boolean hasEffects() {
        return effects != null && !effects.isEmpty();
    }

    @Nullable
    public String getColor() {
        return color;
    }

    @Nullable
    public List<String> getDescription() {
        return description;
    }

    public boolean isSkipsReforge() {
        return skipReforge || ReforgeEffects.anySkipsReforge(effects);
    }

    public boolean isSkipsBaseItem() {
        return skipBaseItem || ReforgeEffects.anySkipsReforge(effects);
    }

    public boolean isBadgeHidden(String key) {
        return hiddenBadges != null && hiddenBadges.contains(key);
    }

    public List<ImprintPool> getImprintPools() {
        List<ImprintPool> all = new ArrayList<>();
        if (imprintPool != null) all.add(imprintPool);
        if (imprintPools != null) all.addAll(imprintPools);
        return all;
    }

    @Nullable
    public ImprintPoolChoice getImprintPoolChoice() {
        return imprintPoolChoice;
    }

    private static boolean poolRolls(ImprintPool p) {
        return p != null && (!p.getCandidates().isEmpty()
                || !p.getImprintGroups().isEmpty()
                || !p.getDefaultImprints().isEmpty());
    }

    public boolean hasImprintPool() {
        for (ImprintPool p : getImprintPools()) {
            if (poolRolls(p)) return true;
        }
        if (imprintPoolChoice != null) {
            for (ImprintPoolChoice.Candidate c : imprintPoolChoice.getCandidates()) {
                if (poolRolls(c.getPool())) return true;
            }
        }
        return false;
    }

    @Nullable
    public ImprintPool getImprintPool() {
        List<ImprintPool> all = getImprintPools();
        return all.isEmpty() ? null : all.get(0);
    }

    public List<EffectPool> getEffectPools() {
        List<EffectPool> all = new ArrayList<>();
        if (effectPool != null) all.add(effectPool);
        if (effectPools != null) all.addAll(effectPools);
        return all;
    }

    @Nullable
    public EffectPoolChoice getEffectPoolChoice() {
        return effectPoolChoice;
    }

    public boolean hasEffectPool() {
        if (getEffectPools().stream().anyMatch(p -> !p.getCandidates().isEmpty())) return true;
        if (effectPoolChoice != null) {
            for (EffectPoolChoice.Candidate c : effectPoolChoice.getCandidates()) {
                if (c.getPool() != null && !c.getPool().getCandidates().isEmpty()) return true;
            }
        }
        return false;
    }

    @Nullable
    public EffectPool getEffectPool() {
        List<EffectPool> all = getEffectPools();
        return all.isEmpty() ? null : all.get(0);
    }

    public List<BehaviorPool> getBehaviorPools() {
        List<BehaviorPool> all = new ArrayList<>();
        if (behaviorPool != null) all.add(behaviorPool);
        if (behaviorPools != null) all.addAll(behaviorPools);
        return all;
    }

    @Nullable
    public BehaviorPoolChoice getBehaviorPoolChoice() {
        return behaviorPoolChoice;
    }

    public boolean hasBehaviorPool() {
        if (getBehaviorPools().stream().anyMatch(p -> !p.getCandidates().isEmpty())) return true;
        if (behaviorPoolChoice != null) {
            for (BehaviorPoolChoice.Candidate c : behaviorPoolChoice.getCandidates()) {
                if (c.getPool() != null && !c.getPool().getCandidates().isEmpty()) return true;
            }
        }
        return false;
    }

    @Nullable
    public BehaviorPool getBehaviorPool() {
        List<BehaviorPool> all = getBehaviorPools();
        return all.isEmpty() ? null : all.get(0);
    }

    public List<String> rollEffects(Random rng) {
        List<EffectPool> pools = getEffectPools();
        List<String> combined = new ArrayList<>();
        for (EffectPool pool : pools) combined.addAll(pool.roll(rng));

        if (effectPoolChoice != null && !effectPoolChoice.isEmpty()) {
            EffectPool picked = effectPoolChoice.pick(rng);
            if (picked != null) combined.addAll(picked.roll(rng));
        }
        return combined;
    }

    public RolledBias rollBehavior(Random rng) {

        Object[] bias = { groups, groupWeightMultipliers, rarityBoost, guaranteedMinRarity, maxRarity };
        for (BehaviorPool pool : getBehaviorPools()) overlayBehaviorPool(bias, pool, rng);

        if (behaviorPoolChoice != null && !behaviorPoolChoice.isEmpty()) {
            BehaviorPool picked = behaviorPoolChoice.pick(rng);
            if (picked != null) overlayBehaviorPool(bias, picked, rng);
        }
        return new RolledBias((List<String>) bias[0], (Map<String, Float>) bias[1],
                (float) bias[2], (String) bias[3], (String) bias[4]);
    }

    @SuppressWarnings("unchecked")
    private static void overlayBehaviorPool(Object[] bias, BehaviorPool pool, Random rng) {
        BehaviorPool.Candidate c = pool.getCandidates().isEmpty() ? null : pool.roll(rng);
        if (c == null) return;
        if (c.groups != null) bias[0] = c.groups;
        if (c.groupWeightMultipliers != null) bias[1] = c.groupWeightMultipliers;
        if (c.rarityBoost != null) bias[2] = Math.max(0f, Math.min(1f, c.rarityBoost));
        if (c.guaranteedMinRarity != null) bias[3] = c.guaranteedMinRarity;
        if (c.maxRarity != null) bias[4] = c.maxRarity;
    }

    public record RolledBias(
            @Nullable List<String> groups,
            @Nullable Map<String, Float> groupWeightMultipliers,
            float rarityBoost,
            @Nullable String guaranteedMinRarity,
            @Nullable String maxRarity) {

        public boolean hasGroupFilter() { return groups != null && !groups.isEmpty(); }
    }

    public static class EffectPool {
        @SerializedName("roll_min")
        private final Integer rollMin;
        @SerializedName("roll_max")
        private final Integer rollMax;
        @SerializedName("nothing_weight")
        private final Integer nothingWeight;
        private final List<EffectCandidate> candidates;

        public EffectPool(Integer rollMin, Integer rollMax, Integer nothingWeight, List<EffectCandidate> candidates) {
            this.rollMin = rollMin;
            this.rollMax = rollMax;
            this.nothingWeight = nothingWeight;
            this.candidates = candidates;
        }

        public int getRollMin() { return rollMin == null ? 1 : Math.max(0, rollMin); }
        public int getRollMax() { return rollMax == null ? getRollMin() : Math.max(getRollMin(), rollMax); }
        public List<EffectCandidate> getCandidates() { return candidates == null ? List.of() : candidates; }

        List<String> roll(Random rng) {
            List<EffectCandidate> pool = new ArrayList<>(getCandidates());
            if (pool.isEmpty()) return List.of();
            int min = getRollMin();
            int max = Math.min(getRollMax(), pool.size());
            if (max < min) max = min;

            if (min == 0 && max > 0) {
                int candidateTotal = 0;
                for (EffectCandidate c : pool) candidateTotal += c.getWeight();
                int nw = nothingWeight != null ? Math.max(0, nothingWeight) : candidateTotal;
                int total = candidateTotal + nw;
                if (total > 0 && rng.nextInt(total) < nw) return List.of();
                min = 1;
            }

            int count = (min >= max) ? min : min + rng.nextInt(max - min + 1);
            count = Math.min(count, pool.size());
            List<String> out = new ArrayList<>();
            for (int i = 0; i < count && !pool.isEmpty(); i++) {
                EffectCandidate picked = weightedPick(pool, rng);
                if (picked == null) break;
                pool.remove(picked);
                if (picked.effect != null && !picked.effect.isBlank()) out.add(picked.effect);
            }
            return out;
        }

        private static EffectCandidate weightedPick(List<EffectCandidate> candidates, Random rng) {
            int total = 0;
            for (EffectCandidate c : candidates) total += c.getWeight();
            if (total <= 0) return candidates.get(rng.nextInt(candidates.size()));
            int roll = rng.nextInt(total);
            for (EffectCandidate c : candidates) {
                roll -= c.getWeight();
                if (roll < 0) return c;
            }
            return candidates.get(candidates.size() - 1);
        }
    }

    public static class EffectCandidate {
        @Nullable
        private final String effect;
        private final Integer weight;

        public EffectCandidate(@Nullable String effect, Integer weight) {
            this.effect = effect;
            this.weight = weight;
        }

        @Nullable public String getEffect() { return effect; }
        public int getWeight() { return weight == null ? 1 : Math.max(1, weight); }
    }

    public static class BehaviorPool {
        @SerializedName("roll_min")
        private final Integer rollMin;
        @SerializedName("roll_max")
        private final Integer rollMax;
        @SerializedName("nothing_weight")
        private final Integer nothingWeight;
        private final List<ReforgeMaterial.BehaviorPool.Candidate> candidates;

        public BehaviorPool(Integer rollMin, Integer rollMax, Integer nothingWeight, List<ReforgeMaterial.BehaviorPool.Candidate> candidates) {
            this.rollMin = rollMin;
            this.rollMax = rollMax;
            this.nothingWeight = nothingWeight;
            this.candidates = candidates;
        }

        public int getRollMin() { return rollMin == null ? 1 : Math.max(0, rollMin); }
        public int getRollMax() { return rollMax == null ? getRollMin() : Math.max(getRollMin(), rollMax); }
        public List<ReforgeMaterial.BehaviorPool.Candidate> getCandidates() { return candidates == null ? List.of() : candidates; }

        @Nullable
        ReforgeMaterial.BehaviorPool.Candidate roll(Random rng) {
            List<ReforgeMaterial.BehaviorPool.Candidate> pool = getCandidates();
            if (pool.isEmpty()) return null;
            int min = getRollMin();
            if (min == 0) {
                int candidateTotal = 0;
                for (ReforgeMaterial.BehaviorPool.Candidate c : pool) candidateTotal += c.getWeight();
                int nw = nothingWeight != null ? Math.max(0, nothingWeight) : candidateTotal;
                int total = candidateTotal + nw;
                if (total > 0 && rng.nextInt(total) < nw) return null;
            }
            return weightedPick(pool, rng);
        }

        private static ReforgeMaterial.BehaviorPool.Candidate weightedPick(List<ReforgeMaterial.BehaviorPool.Candidate> candidates, Random rng) {
            int total = 0;
            for (ReforgeMaterial.BehaviorPool.Candidate c : candidates) total += c.getWeight();
            if (total <= 0) return candidates.get(rng.nextInt(candidates.size()));
            int roll = rng.nextInt(total);
            for (ReforgeMaterial.BehaviorPool.Candidate c : candidates) {
                roll -= c.getWeight();
                if (roll < 0) return c;
            }
            return candidates.get(candidates.size() - 1);
        }

        public static class Candidate {
            @Nullable
            private final List<String> groups;
            @Nullable
            @SerializedName("group_weight_multipliers")
            private final Map<String, Float> groupWeightMultipliers;
            @Nullable
            @SerializedName("rarity_boost")
            private final Float rarityBoost;
            @Nullable
            @SerializedName("guaranteed_min_rarity")
            private final String guaranteedMinRarity;
            @Nullable
            @SerializedName("max_rarity")
            private final String maxRarity;
            private final Integer weight;

            public Candidate(@Nullable List<String> groups, @Nullable Map<String, Float> groupWeightMultipliers,
                    @Nullable Float rarityBoost, @Nullable String guaranteedMinRarity,
                    @Nullable String maxRarity, Integer weight) {
                this.groups = groups;
                this.groupWeightMultipliers = groupWeightMultipliers;
                this.rarityBoost = rarityBoost;
                this.guaranteedMinRarity = guaranteedMinRarity;
                this.maxRarity = maxRarity;
                this.weight = weight;
            }

            @Nullable public List<String> getGroups() { return groups; }
            @Nullable public Map<String, Float> getGroupWeightMultipliers() { return groupWeightMultipliers; }
            @Nullable public Float getRarityBoost() { return rarityBoost; }
            @Nullable public String getGuaranteedMinRarity() { return guaranteedMinRarity; }
            @Nullable public String getMaxRarity() { return maxRarity; }
            public int getWeight() { return weight == null ? 1 : Math.max(1, weight); }
        }
    }
}
