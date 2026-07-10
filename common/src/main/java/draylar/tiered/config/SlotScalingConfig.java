package draylar.tiered.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SlotScalingConfig {

    public boolean enabled = true;
    public List<DifficultyTier> difficultyTiers = defaultTiers();
    public List<DimensionEntry> dimensions = defaultDimensions();
    public List<BonusCap> bonusCaps = defaultBonusCaps();
    public int bossBonusCap = -1;
    public List<String> bossTiers = List.of("bosses");

    private transient Map<String, DifficultyTier> tierIndex;
    private transient List<DifficultyTier> tierIndexSource;

    private Map<String, DifficultyTier> tiers() {
        if (tierIndex == null || tierIndexSource != difficultyTiers) {
            Map<String, DifficultyTier> m = new HashMap<>();
            if (difficultyTiers != null) {
                for (DifficultyTier t : difficultyTiers) {
                    if (t != null && t.name != null) m.put(t.name, t);
                }
            }
            tierIndex = m;
            tierIndexSource = difficultyTiers;
        }
        return tierIndex;
    }

    public int tierSlotBonus(String tierName) {
        Map<String, DifficultyTier> idx = tiers();
        int total = 0;
        int guard = 0;
        String cur = tierName;
        Set<String> seen = new HashSet<>();
        while (cur != null && idx.containsKey(cur) && seen.add(cur) && guard++ < 64) {
            DifficultyTier t = idx.get(cur);
            if (!t.allowSlotScaling) return 0;
            total += Math.max(0, t.slotBonus);
            cur = t.parent;
        }
        return total;
    }

    public int resolveBonus(String entityId, String dimensionId, List<String> structureIdsAtPos,
            List<String> structureTagsAtPos) {
        if (!enabled || dimensions == null) return 0;
        int best = 0;
        for (DimensionEntry dim : dimensions) {
            if (dim == null || dim.dimension == null || !dim.dimension.equals(dimensionId)) continue;

            int sum = tierSlotBonus(dim.tier);

            if (dim.zones != null && structureIdsAtPos != null) {
                for (ZoneEntry z : dim.zones) {
                    if (z == null || z.structure == null) continue;
                    if (StructureMatch.matchesAny(z.structure, structureIdsAtPos, structureTagsAtPos)) {
                        sum += tierSlotBonus(z.tier);
                        break;
                    }
                }
            }
            if (entityId != null && dim.entities != null) {
                for (EntityTierEntry e : dim.entities) {
                    if (e != null && e.entity != null && e.entity.equals(entityId)) {
                        sum += tierSlotBonus(e.tier);
                        break;
                    }
                }
            }
            best = Math.max(best, sum);
        }
        return best;
    }

    public int dimensionCap(String dimensionId) {
        if (dimensions == null) return -1;
        int cap = -1;
        for (DimensionEntry dim : dimensions) {
            if (dim != null && dim.dimension != null && dim.dimension.equals(dimensionId) && dim.maxBonus >= 0) {
                cap = Math.max(cap, dim.maxBonus);
            }
        }
        return cap;
    }

    private int tierCap(String dimensionId) {
        if (dimensions == null) return -1;
        int cap = -1;
        for (DimensionEntry dim : dimensions) {
            if (dim != null && dim.dimension != null && dim.dimension.equals(dimensionId) && dim.tierMaxBonus >= 0) {
                cap = Math.max(cap, dim.tierMaxBonus);
            }
        }
        return cap;
    }

    private int applyTierCap(String dimensionId, int tierBonus) {
        int cap = tierCap(dimensionId);
        return cap >= 0 ? Math.max(0, Math.min(tierBonus, cap)) : tierBonus;
    }

    private boolean isBossEntity(String entityId, String dimensionId) {
        if (entityId == null || bossTiers == null || bossTiers.isEmpty() || dimensions == null) return false;
        for (DimensionEntry dim : dimensions) {
            if (dim == null || !dimensionId.equals(dim.dimension) || dim.entities == null) continue;
            for (EntityTierEntry e : dim.entities) {
                if (e != null && entityId.equals(e.entity) && bossTiers.contains(e.tier)) return true;
            }
        }
        return false;
    }

    private DimensionEntry dimensionOf(String dimensionId) {
        if (dimensions == null) return null;
        for (DimensionEntry dim : dimensions) {
            if (dim != null && dim.dimension != null && dim.dimension.equals(dimensionId)) return dim;
        }
        return null;
    }

    private List<ScalingRule> pickMobRule(String entityId, DimensionEntry dim) {
        if (dim != null && entityId != null && dim.entities != null) {
            for (EntityTierEntry e : dim.entities) {
                if (e != null && entityId.equals(e.entity) && e.rule != null && !e.rule.isEmpty()) return e.rule;
            }
        }
        return dim == null ? List.of() : dim.mobRule;
    }

    private List<ScalingRule> pickLootRule(DimensionEntry dim, List<String> structureIds, List<String> structureTags) {
        if (dim != null && dim.zones != null && structureIds != null) {
            for (ZoneEntry z : dim.zones) {
                if (z == null || z.structure == null) continue;
                if (StructureMatch.matchesAny(z.structure, structureIds, structureTags)
                        && z.rule != null && !z.rule.isEmpty()) {
                    return z.rule;
                }
            }
        }
        return dim == null ? List.of() : dim.lootRule;
    }

    private static int firstMatchingBonus(List<ScalingRule> rules, ItemStack stack, float maxHealth, String dimensionId) {
        if (rules == null) return 0;
        for (ScalingRule rule : rules) {
            if (rule == null || !rule.enabled) continue;
            if (rule.appliesTo != null && !rule.appliesTo.matches(stack)) continue;
            return rule.computeFor(stack, maxHealth, dimensionId);
        }
        return 0;
    }

    public int resolveMobBonus(ItemStack stack, String entityId, String dimensionId, float entityMaxHealth,
            List<String> structureIdsAtPos, List<String> structureTagsAtPos) {
        if (!enabled) return 0;
        int tier = applyTierCap(dimensionId, resolveBonus(entityId, dimensionId, structureIdsAtPos, structureTagsAtPos));

        int extra = firstMatchingBonus(pickMobRule(entityId, dimensionOf(dimensionId)), stack, entityMaxHealth, dimensionId);

        int total = Math.max(0, tier + extra);
        int dimCap = dimensionCap(dimensionId);
        int cap = dimCap;
        if (bossBonusCap >= 0 && isBossEntity(entityId, dimensionId)) {
            cap = (dimCap < 0) ? bossBonusCap : Math.max(dimCap, bossBonusCap);
        }
        return cap >= 0 ? Math.max(0, Math.min(total, cap)) : total;
    }

    public int resolveLootTableBonus(ItemStack stack, String dimensionId,
            List<String> structureIdsAtPos, List<String> structureTagsAtPos) {
        if (!enabled) return 0;
        int tier = applyTierCap(dimensionId, resolveBonus(null, dimensionId, structureIdsAtPos, structureTagsAtPos));

        DimensionEntry dim = dimensionOf(dimensionId);
        int extra = firstMatchingBonus(pickLootRule(dim, structureIdsAtPos, structureTagsAtPos), stack, 0f, dimensionId);

        int total = Math.max(0, tier + extra);
        int cap = dimensionCap(dimensionId);
        return cap >= 0 ? Math.max(0, Math.min(total, cap)) : total;
    }

    public int clampGlobalBonus(ItemStack stack, int totalBonus) {
        if (bonusCaps == null || totalBonus <= 0) return Math.max(0, totalBonus);
        for (BonusCap cap : bonusCaps) {
            if (cap != null && cap.matches(stack)) {
                return Math.max(0, Math.min(totalBonus, Math.max(0, cap.maxBonus)));
            }
        }
        return totalBonus;
    }

    public static class DifficultyTier {
        public String name = "";
        public String parent = null;
        public boolean allowSlotScaling = true;
        public int slotBonus = 0;

        public DifficultyTier() {}

        public DifficultyTier(String name, String parent, boolean allowSlotScaling, int slotBonus) {
            this.name = name;
            this.parent = parent;
            this.allowSlotScaling = allowSlotScaling;
            this.slotBonus = slotBonus;
        }
    }

    public static class DimensionEntry {
        public String dimension = "";
        public String tier = "";
        public int maxBonus = -1;
        public int tierMaxBonus = -1;

        public List<ScalingRule> mobRule = List.of();
        public List<ScalingRule> lootRule = List.of();
        public List<ZoneEntry> zones = List.of();
        public List<EntityTierEntry> entities = List.of();

        public DimensionEntry() {}

        public DimensionEntry(String dimension, String tier, int maxBonus,
                List<ZoneEntry> zones, List<EntityTierEntry> entities) {
            this.dimension = dimension;
            this.tier = tier;
            this.maxBonus = maxBonus;
            this.zones = zones;
            this.entities = entities;
        }
    }

    public static class ZoneEntry {
        public String structure = "";
        public String tier = "";
        public List<ScalingRule> rule = List.of();

        public ZoneEntry() {}

        public ZoneEntry(String structure, String tier) {
            this.structure = structure;
            this.tier = tier;
        }
    }

    public static class EntityTierEntry {
        public String entity = "";
        public String tier = "";
        public List<ScalingRule> rule = List.of();

        public EntityTierEntry() {}

        public EntityTierEntry(String entity, String tier) {
            this.entity = entity;
            this.tier = tier;
        }
    }

    public static final class StructureMatch {
        private StructureMatch() {}

        private static final String ANY = "*";

        public static boolean matchesAny(String pattern, List<String> structureIds, List<String> structureTags) {
            if (pattern == null || pattern.isEmpty() || pattern.equals(ANY)) {
                return (structureIds != null && !structureIds.isEmpty())
                        || (structureTags != null && !structureTags.isEmpty());
            }
            if (pattern.startsWith("!")) {
                return !matchesInner(pattern.substring(1), structureIds, structureTags);
            }
            return matchesInner(pattern, structureIds, structureTags);
        }

        private static boolean matchesInner(String pattern, List<String> structureIds, List<String> structureTags) {
            if (pattern.startsWith("#")) {
                if (structureTags == null) return false;
                String want = pattern.substring(1);
                for (String t : structureTags) {
                    if (t != null && t.equals(want)) return true;
                }
                return false;
            }
            if (pattern.startsWith("~")) {
                if (structureIds == null) return false;
                Pattern p;
                try {
                    p = Pattern.compile(pattern.substring(1), Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    return false;
                }
                for (String id : structureIds) {
                    if (id != null && p.matcher(id).find()) return true;
                }
                return false;
            }
            if (structureIds == null) return false;
            for (String id : structureIds) {
                if (id != null && id.equals(pattern)) return true;
            }
            return false;
        }
    }

    public static class BonusCap {
        public List<String> items = List.of();
        public List<String> tags = List.of();
        public int maxBonus = 1;

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            boolean itemsEmpty = items == null || items.isEmpty();
            boolean tagsEmpty = tags == null || tags.isEmpty();
            if (itemsEmpty && tagsEmpty) return false;
            return ItemMatch.matchesAny(stack, items, tags);
        }

        static BonusCap of(String tag, int maxBonus) {
            BonusCap c = new BonusCap();
            c.tags = List.of(tag);
            c.maxBonus = maxBonus;
            return c;
        }
    }

    public static final class ItemMatch {
        private ItemMatch() {}

        public static boolean matchesAny(ItemStack stack, List<String> ids, List<String> tagList) {
            if (ids != null && !ids.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                for (String wanted : ids) {
                    if (wanted != null && wanted.equals(id)) return true;
                }
            }
            if (tagList != null && !tagList.isEmpty()) {
                for (String t : tagList) {
                    if (t == null) continue;
                    String tagId = t.startsWith("#") ? t.substring(1) : t;
                    ResourceLocation parsed = ResourceLocation.tryParse(tagId);
                    if (parsed == null) continue;
                    TagKey<Item> key = TagKey.create(Registries.ITEM, parsed);
                    if (stack.is(key)) return true;
                }
            }
            return false;
        }
    }

    public static class ScalingRule {
        public boolean enabled = true;
        public float healthPerStep = 100f;
        public int slotsPerStep = 1;
        public int maxBonusSlots = 2;
        public int flatBonus = 0;
        public float minHealthThreshold = 0f;
        public List<DimensionBonus> dimensionBonuses = List.of();

        public AppliesTo appliesTo = null;

        static ScalingRule defaultHealth() {
            ScalingRule r = new ScalingRule();
            r.healthPerStep = 200f;
            r.slotsPerStep = 1;
            r.maxBonusSlots = 2;
            r.flatBonus = 0;
            r.dimensionBonuses = List.of();
            return r;
        }

        static ScalingRule healthFor(float perStep, String itemTag) {
            ScalingRule r = defaultHealth();
            r.healthPerStep = perStep;
            AppliesTo a = new AppliesTo();
            a.tags = List.of(itemTag);
            r.appliesTo = a;
            return r;
        }

        static ScalingRule flat(int flatBonus) {
            ScalingRule r = new ScalingRule();
            r.healthPerStep = 0f;
            r.slotsPerStep = 0;
            r.maxBonusSlots = 0;
            r.flatBonus = flatBonus;
            r.dimensionBonuses = List.of();
            return r;
        }

        public int compute(float entityMaxHealth, String dimensionId) {
            if (!enabled) return 0;
            int scaled = 0;
            if (healthPerStep > 0f && entityMaxHealth >= (minHealthThreshold > 0f ? minHealthThreshold : 0f)) {
                int steps = (int) Math.floor(entityMaxHealth / healthPerStep);
                scaled = Math.max(0, Math.min(maxBonusSlots, steps * slotsPerStep));
            }
            int dim = 0;
            if (dimensionId != null && dimensionBonuses != null) {
                for (DimensionBonus db : dimensionBonuses) if (db.matches(dimensionId)) dim += db.bonus;
            }
            return Math.max(0, scaled + flatBonus + dim);
        }

        public int computeFor(ItemStack stack, float entityMaxHealth, String dimensionId) {
            if (appliesTo != null && !appliesTo.matches(stack)) return 0;
            int v = compute(entityMaxHealth, dimensionId);
            return appliesTo == null ? v : appliesTo.clampBonus(v);
        }
    }

    public static class DimensionBonus {
        public List<String> dimensions = List.of();
        public int bonus = 1;

        public boolean matches(String dimensionId) {
            if (dimensions == null || dimensionId == null) return false;
            for (String d : dimensions) {
                if (d != null && !d.startsWith("#") && d.equals(dimensionId)) return true;
            }
            return false;
        }
    }

    public static class AppliesTo {
        public List<String> items = List.of();
        public List<String> tags = List.of();
        public int minBonus = -1;
        public int maxBonus = -1;
        public List<String> excludeItems = List.of();
        public List<String> excludeTags = List.of();

        public int clampBonus(int bonus) {
            int r = bonus;
            if (maxBonus >= 0) r = Math.min(r, maxBonus);
            if (minBonus > 0 && r > 0) r = Math.max(r, minBonus);
            return Math.max(0, r);
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            if (ItemMatch.matchesAny(stack, excludeItems, excludeTags)) return false;
            boolean itemsEmpty = items == null || items.isEmpty();
            boolean tagsEmpty = tags == null || tags.isEmpty();
            if (itemsEmpty && tagsEmpty) return true;
            return ItemMatch.matchesAny(stack, items, tags);
        }
    }

    private static List<BonusCap> defaultBonusCaps() {

        List<BonusCap> caps = new ArrayList<>();
        caps.add(BonusCap.of("#accessories_compat_layer:all_trinkets_items", 0));
        caps.add(BonusCap.of("#tiered:imprint_slot_weapons", 10));
        caps.add(BonusCap.of("#tiered:imprint_slot_armor", 6));
        return caps;
    }

    private static List<DifficultyTier> defaultTiers() {
        List<DifficultyTier> t = new ArrayList<>();
        t.add(new DifficultyTier("overworld", null, true, 0));
        t.add(new DifficultyTier("nether", null, true, 1));
        t.add(new DifficultyTier("end", null, true, 1));
        t.add(new DifficultyTier("dungeon", null, true, 1));
        t.add(new DifficultyTier("bosses", "dungeon", true, 1));
        return t;
    }

    private static List<DimensionEntry> defaultDimensions() {
        List<DimensionEntry> dims = new ArrayList<>();

        List<ZoneEntry> commonDungeonZones = List.of(
                new ZoneEntry("#dungeon_difficulty:level_1", "dungeon"),
                new ZoneEntry("#dungeon_difficulty:level_2", "dungeon"),
                new ZoneEntry("#dungeon_difficulty:level_3", "dungeon"),
                new ZoneEntry("~mostructures", "dungeon"),
                new ZoneEntry("~nova_structures", "dungeon"),
                new ZoneEntry("~philipsruins", "dungeon")
        );

        List<EntityTierEntry> overworldBosses = List.of(
                new EntityTierEntry("minecraft:warden", "bosses"),
                new EntityTierEntry("minecraft:wither", "bosses"),
                new EntityTierEntry("mutantmonsters:mutant_zombie", "bosses"),
                new EntityTierEntry("formidulus:deer_god", "bosses"),
                new EntityTierEntry("hexblade:magus", "bosses")
        );

        DimensionEntry overworld = new DimensionEntry("minecraft:overworld", "overworld", -1,
                zones(commonDungeonZones,
                        "minecraft:ancient_city",
                        "minecraft:trial_chambers",
                        "minecraft:woodland_mansion",
                        "minecraft:stronghold",
                        "minecraft:monument",
                        "minecraft:pillager_outpost"),
                overworldBosses);
        overworld.mobRule = defaultMobRules();
        dims.add(overworld);

        DimensionEntry nether = new DimensionEntry("minecraft:the_nether", "nether", -1,
                zones(commonDungeonZones,
                        "minecraft:fortress",
                        "minecraft:bastion_remnant"),
                List.of(
                        new EntityTierEntry("minecraft:warden", "bosses"),
                        new EntityTierEntry("minecraft:wither", "bosses"),
                        new EntityTierEntry("bosses_of_mass_destruction:gauntlet", "bosses")));
        nether.mobRule = defaultMobRules();
        nether.lootRule = List.of(ScalingRule.flat(1));
        dims.add(nether);

        DimensionEntry end = new DimensionEntry("minecraft:the_end", "end", -1,
                zones(commonDungeonZones, "minecraft:end_city"),
                List.of(
                        new EntityTierEntry("minecraft:ender_dragon", "bosses"),
                        new EntityTierEntry("minecraft:warden", "bosses"),
                        new EntityTierEntry("minecraft:wither", "bosses"),
                        new EntityTierEntry("bosses_of_mass_destruction:obsidilith", "bosses")));
        end.mobRule = defaultMobRules();
        dims.add(end);

        return dims;
    }

    private static List<ScalingRule> defaultMobRules() {
        return List.of(
                ScalingRule.healthFor(150f, "#tiered:imprint_slot_weapons"),
                ScalingRule.healthFor(200f, "#tiered:imprint_slot_armor"));
    }

    private static List<ZoneEntry> zones(List<ZoneEntry> common, String... extraStructureIds) {
        List<ZoneEntry> list = new ArrayList<>(common);
        for (String id : extraStructureIds) list.add(new ZoneEntry(id, "dungeon"));
        return list;
    }
}
