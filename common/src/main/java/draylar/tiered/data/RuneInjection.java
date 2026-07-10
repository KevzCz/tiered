package draylar.tiered.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class RuneInjection {

    public static class ItemCandidate {
        private final String item;
        @Nullable private final Integer weight;

        public ItemCandidate(String item, Integer weight) { this.item = item; this.weight = weight; }
        public String getItem() { return item; }
        public int getWeight() { return weight == null ? 1 : Math.max(1, weight); }
    }

    public static class Pool {
        @Nullable private final String item;
        @Nullable private final List<ItemCandidate> items;
        @Nullable private final Float chance;
        @Nullable @SerializedName("count_min") private final Integer countMin;
        @Nullable @SerializedName("count_max") private final Integer countMax;

        public Pool(String item, List<ItemCandidate> items, Float chance, Integer countMin, Integer countMax) {
            this.item = item;
            this.items = items;
            this.chance = chance;
            this.countMin = countMin;
            this.countMax = countMax;
        }

        public List<ItemCandidate> getItems() {
            List<ItemCandidate> out = new ArrayList<>();
            if (items != null) out.addAll(items);
            if (item != null) out.add(new ItemCandidate(item, null));
            return out;
        }

        public float getChance() { return chance == null ? 1f : Math.max(0f, Math.min(1f, chance)); }
        public int getCountMin() { return countMin == null ? 1 : Math.max(0, countMin); }
        public int getCountMax() { return countMax == null ? Math.max(1, getCountMin()) : Math.max(getCountMin(), countMax); }
    }

    @Nullable private final List<String> tables;
    @Nullable @SerializedName("table_patterns") private final List<String> tablePatterns;

    @Nullable private final List<String> dimensions;
    @Nullable @SerializedName("dimension_tags") private final List<String> dimensionTags;
    @Nullable private final List<String> biomes;
    @Nullable @SerializedName("biome_tags") private final List<String> biomeTags;
    @Nullable private final List<String> structures;
    @Nullable @SerializedName("structure_tags") private final List<String> structureTags;
    @Nullable private final List<String> entities;
    @Nullable @SerializedName("entity_tags") private final List<String> entityTags;

    @Nullable private final List<Pool> pools;

    @Nullable private final String item;
    @Nullable private final List<ItemCandidate> items;
    @Nullable private final Float chance;
    @Nullable @SerializedName("count_min") private final Integer countMin;
    @Nullable @SerializedName("count_max") private final Integer countMax;

    public RuneInjection(List<String> tables, List<String> tablePatterns,
            List<String> dimensions, List<String> dimensionTags,
            List<String> biomes, List<String> biomeTags,
            List<String> structures, List<String> structureTags,
            List<String> entities, List<String> entityTags,
            List<Pool> pools,
            String item, List<ItemCandidate> items, Float chance, Integer countMin, Integer countMax) {
        this.tables = tables;
        this.tablePatterns = tablePatterns;
        this.dimensions = dimensions;
        this.dimensionTags = dimensionTags;
        this.biomes = biomes;
        this.biomeTags = biomeTags;
        this.structures = structures;
        this.structureTags = structureTags;
        this.entities = entities;
        this.entityTags = entityTags;
        this.pools = pools;
        this.item = item;
        this.items = items;
        this.chance = chance;
        this.countMin = countMin;
        this.countMax = countMax;
    }

    public List<Pool> getPools() {
        List<Pool> out = new ArrayList<>();
        if (pools != null) out.addAll(pools);

        if (item != null || items != null) {
            out.add(new Pool(item, items, chance, countMin, countMax));
        }
        return out;
    }

    public boolean matchesTable(String tableId) {
        if (tables != null) {
            for (String t : tables) if (t.equalsIgnoreCase(tableId)) return true;
        }
        if (tablePatterns != null) {
            for (String p : tablePatterns) if (tableId.contains(p)) return true;
        }
        return false;
    }

    @Nullable public List<String> getDimensions() { return dimensions; }
    @Nullable public List<String> getDimensionTags() { return dimensionTags; }
    @Nullable public List<String> getBiomes() { return biomes; }
    @Nullable public List<String> getBiomeTags() { return biomeTags; }
    @Nullable public List<String> getStructures() { return structures; }
    @Nullable public List<String> getStructureTags() { return structureTags; }
    @Nullable public List<String> getEntities() { return entities; }
    @Nullable public List<String> getEntityTags() { return entityTags; }

    public boolean hasContextFilters() {
        return (dimensions != null && !dimensions.isEmpty())
                || (dimensionTags != null && !dimensionTags.isEmpty())
                || (biomes != null && !biomes.isEmpty())
                || (biomeTags != null && !biomeTags.isEmpty())
                || (structures != null && !structures.isEmpty())
                || (structureTags != null && !structureTags.isEmpty())
                || (entities != null && !entities.isEmpty())
                || (entityTags != null && !entityTags.isEmpty());
    }
}
