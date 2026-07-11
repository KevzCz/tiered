package draylar.tiered.config;

import draylar.tiered.Tiered;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.GsonBuilder;
import org.spongepowered.include.com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigInit {

    public static TieredConfig CONFIG;

    public static List<TuningIngotConfig> CUSTOM_TUNING_INGOTS;
    public static List<String> CUSTOM_TUNING_INGOT_LOOT_TABLES;

    public static SpecialIngotConfig SPECIAL_INGOT;
    public static boolean SPECIAL_INGOT_ENABLED;

    public static List<String> ALLOWED_REROLL_GROUPS;
    public static List<String> RARITY_ORDER;

    public static SlotScalingConfig SLOT_SCALING;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/tiered_more/tiered_more.json");
    private static final File SLOT_SCALING_FILE = new File("config/tiered_more/slot-scaling.json");

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        AutoConfig.register(TieredConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(TieredConfig.class).getConfig();
        loadExtraTuningIngotConfig();
        loadSlotScalingConfig();
    }

    private static void loadExtraTuningIngotConfig() {
        TuningIngotConfigList data;

        if (!FILE.exists()) {
            data = createDefaultTuningConfig();
            writeTuningConfig(data);
        } else {
            try (FileReader reader = new FileReader(FILE)) {
                data = GSON.fromJson(reader, TuningIngotConfigList.class);

                boolean needsRewrite = false;

                if (data.tuningIngotConfigs == null) {
                    data.tuningIngotConfigs = createDefaultTuningConfigs();
                    needsRewrite = true;
                }
                if (data.tuningIngotLootTables == null) {
                    data.tuningIngotLootTables = createDefaultLootTables();
                    needsRewrite = true;
                }
                if (data.allowedRerollGroupsScroll == null) {
                    data.allowedRerollGroupsScroll = createDefaultAllowedRerollGroups();
                    needsRewrite = true;
                }
                if (data.rarityOrder == null || data.rarityOrder.isEmpty()) {
                    data.rarityOrder = createDefaultRarityOrder();
                    needsRewrite = true;
                }

                if (data.tuningIngotConfigs != null) {
                    List<TuningIngotConfig> defs = createDefaultTuningConfigs();
                    Map<String, TuningIngotConfig> defByGroup = new HashMap<>();
                    for (TuningIngotConfig d : defs) defByGroup.put(d.group, d);

                    for (TuningIngotConfig t : data.tuningIngotConfigs) {
                        TuningIngotConfig def = defByGroup.get(t.group);

                        if (def != null) {
                            if (t.minCount <= 0) { t.minCount = def.minCount; needsRewrite = true; }
                            if (t.maxCount <= 0) { t.maxCount = def.maxCount; needsRewrite = true; }
                            if (t.maxCount < t.minCount) {
                                t.maxCount = Math.max(t.minCount, def.maxCount);
                                needsRewrite = true;
                            }
                        } else {
                            if (t.minCount <= 0) { t.minCount = 1; needsRewrite = true; }
                            if (t.maxCount < t.minCount) { t.maxCount = t.minCount; needsRewrite = true; }
                        }

                        if (t.lootChance < 0f) { t.lootChance = 0f; needsRewrite = true; }
                        if (t.lootChance > 1f) { t.lootChance = 1f; needsRewrite = true; }
                    }
                }

                if (data.enableSpecialIngot == null) {
                    data.enableSpecialIngot = false;
                    needsRewrite = true;
                }
                if (data.specialIngot == null) {
                    data.specialIngot = createDefaultSpecialIngot();
                    needsRewrite = true;
                }

                if (data.specialIngot != null) {
                    SpecialIngotConfig s = data.specialIngot;
                    boolean patch = false;

                    float clampedDrop = clamp01(s.dropChance);
                    if (s.dropChance != clampedDrop) { s.dropChance = clampedDrop; patch = true; }

                    float clampedTotal = clamp01(s.totalSpecialPercent);
                    if (s.totalSpecialPercent != clampedTotal) { s.totalSpecialPercent = clampedTotal; patch = true; }

                    if (s.lootTables == null) { s.lootTables = createDefaultSpecialLootTables(); patch = true; }
                    if (s.specialStats == null) { s.specialStats = createDefaultSpecialIngot().specialStats; patch = true; }
                    if (s.basicStats == null)   { s.basicStats   = createDefaultSpecialIngot().basicStats;   patch = true; }
                    if (s.blockedItemIds == null)  { s.blockedItemIds  = List.of(); patch = true; }
                    if (s.blockedItemTags == null) { s.blockedItemTags = List.of(); patch = true; }

                    if (patch) needsRewrite = true;
                }

                if (needsRewrite) {
                    writeTuningConfig(data);
                    Tiered.LOGGER.info("Patched missing/invalid fields in tiered_more.json.");
                }
            } catch (IOException | JsonSyntaxException e) {
                Tiered.LOGGER.error("Failed to load tiered_more.json", e);
                data = createDefaultTuningConfig();
                writeTuningConfig(data);
            }
        }

        CUSTOM_TUNING_INGOTS = data.tuningIngotConfigs;
        CUSTOM_TUNING_INGOT_LOOT_TABLES = data.tuningIngotLootTables;
        ALLOWED_REROLL_GROUPS = data.allowedRerollGroupsScroll;
        RARITY_ORDER = (data.rarityOrder == null || data.rarityOrder.isEmpty())
                ? createDefaultRarityOrder() : data.rarityOrder;

        SPECIAL_INGOT = data.specialIngot;
        SPECIAL_INGOT_ENABLED = Boolean.TRUE.equals(data.enableSpecialIngot);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (Math.min(v, 1f));
    }

    private static TuningIngotConfigList createDefaultTuningConfig() {
        TuningIngotConfigList defaultData = new TuningIngotConfigList();
        defaultData.tuningIngotConfigs = createDefaultTuningConfigs();
        defaultData.tuningIngotLootTables = createDefaultLootTables();
        defaultData.allowedRerollGroupsScroll = createDefaultAllowedRerollGroups();
        defaultData.rarityOrder = createDefaultRarityOrder();
        defaultData.enableSpecialIngot = false;
        defaultData.specialIngot = createDefaultSpecialIngot();
        return defaultData;
    }

    private static SpecialIngotConfig createDefaultSpecialIngot() {
        SpecialIngotConfig c = new SpecialIngotConfig();
        c.totalSpecialPercent = 0.5f;
        c.dropChance = 0.01f;
        c.lootTables = createDefaultSpecialLootTables();
        c.specialStats = List.of(
                stat("minecraft:generic.attack_damage",   2, 1f, List.of("any")),
                stat("minecraft:generic.attack_speed",    2, 1f, List.of("any")),
                stat("minecraft:generic.movement_speed",  2, 1f, List.of("any")),
                stat("minecraft:generic.armor",           2, 1f, List.of("any")),
                stat("minecraft:generic.max_health",      2, 1f, List.of("any"))
        );
        c.basicStats = List.of(
                basic("minecraft:generic.luck",            0, 1.0f, 1f, List.of("any")),
                basic("minecraft:generic.armor_toughness", 0, 1.0f, 1f, List.of("any"))
        );
        c.blockedItemIds  = List.of();
        c.blockedItemTags = List.of("#tclayer:all_trinket_items");
        return c;
    }

    private static List<String> createDefaultSpecialLootTables() {
        return List.of(
                "minecraft:chests/end_city_treasure",
                "minecraft:chests/ancient_city",
                "minecraft:chests/bastion_treasure"
        );
    }

    private static SpecialStatEntry stat(String id, int op, float weight, List<String> slots) {
        SpecialStatEntry e = new SpecialStatEntry();
        e.attributeId = id;
        e.operation = op;
        e.weight = weight;
        e.slots = slots;
        return e;
    }

    private static BasicStatEntry basic(String id, int op, float value, float weight, List<String> slots) {
        BasicStatEntry e = new BasicStatEntry();
        e.attributeId = id;
        e.operation = op;
        e.value = value;
        e.weight = weight;
        e.slots = slots;
        return e;
    }

    private static List<TuningIngotConfig> createDefaultTuningConfigs() {
        return List.of(
                new TuningIngotConfig("common",    "gray",         0.30f, 1, 5),
                new TuningIngotConfig("uncommon",  "dark_green",   0.20f, 1, 4),
                new TuningIngotConfig("rare",      "blue",         0.15f, 1, 3),
                new TuningIngotConfig("epic",      "dark_purple",  0.075f, 1, 2),
                new TuningIngotConfig("legendary", "gold",         0.035f, 1, 2),
                new TuningIngotConfig("unique",    "light_purple", 0.01f, 1, 1)
        );
    }

    private static List<String> createDefaultLootTables() {
        return List.of(
                "minecraft:chests/simple_dungeon",
                "minecraft:chests/abandoned_mineshaft",
                "minecraft:chests/bastion_bridge",
                "minecraft:chests/bastion_hoglin_stable",
                "minecraft:chests/bastion_other",
                "minecraft:chests/bastion_treasure",
                "minecraft:chests/buried_treasure",
                "minecraft:chests/shipwreck_treasure",
                "minecraft:chests/desert_pyramid",
                "minecraft:chests/jungle_temple",
                "minecraft:chests/woodland_mansion",
                "minecraft:chests/stronghold_corridor",
                "minecraft:chests/stronghold_library",
                "minecraft:chests/stronghold_crossing",
                "minecraft:chests/end_city_treasure",
                "minecraft:chests/nether_bridge"
        );
    }

    private static List<String> createDefaultAllowedRerollGroups() {
        return List.of("rare", "epic", "legendary", "unique");
    }

    private static List<String> createDefaultRarityOrder() {
        return List.of("common", "uncommon", "rare", "epic", "legendary", "unique");
    }

    private static void writeTuningConfig(TuningIngotConfigList data) {
        FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Tiered.LOGGER.error("Failed to write tiered_more.json", e);
        }
    }

    public static void reloadSlotScalingConfig() {
        loadSlotScalingConfig();
    }

    private static void loadSlotScalingConfig() {
        if (!SLOT_SCALING_FILE.exists()) {
            SLOT_SCALING = new SlotScalingConfig();
            writeSlotScalingConfig(SLOT_SCALING);
            return;
        }
        try (FileReader reader = new FileReader(SLOT_SCALING_FILE)) {
            SlotScalingConfig loaded = GSON.fromJson(reader, SlotScalingConfig.class);
            boolean needsRewrite = false;

            if (loaded == null) {
                loaded = new SlotScalingConfig();
                needsRewrite = true;
            }

            if (loaded.difficultyTiers == null) {
                loaded = new SlotScalingConfig();
                needsRewrite = true;
            }
            if (loaded.dimensions == null) {
                loaded.dimensions = List.of();
                needsRewrite = true;
            }
            if (loaded.bossTiers == null) {
                loaded.bossTiers = List.of("bosses");
                needsRewrite = true;
            }
            if (loaded.bonusCaps == null) {
                loaded.bonusCaps = List.of();
                needsRewrite = true;
            }

            SLOT_SCALING = loaded;
            if (needsRewrite) {
                writeSlotScalingConfig(SLOT_SCALING);
                Tiered.LOGGER.info("Patched missing fields in slot-scaling.json.");
            }
        } catch (IOException | JsonSyntaxException e) {
            Tiered.LOGGER.error("Failed to load slot-scaling.json, using defaults", e);
            SLOT_SCALING = new SlotScalingConfig();
            writeSlotScalingConfig(SLOT_SCALING);
        }
    }

    private static void writeSlotScalingConfig(SlotScalingConfig data) {
        SLOT_SCALING_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(SLOT_SCALING_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Tiered.LOGGER.error("Failed to write slot-scaling.json", e);
        }
    }
}
