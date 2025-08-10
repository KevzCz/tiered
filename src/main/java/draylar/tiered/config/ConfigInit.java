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
import java.util.List;

public class ConfigInit {

    public static TieredConfig CONFIG;

    public static List<TuningIngotConfig> CUSTOM_TUNING_INGOTS;
    public static List<String> CUSTOM_TUNING_INGOT_LOOT_TABLES;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/tiered_more.json");
    public static List<String> ALLOWED_REROLL_GROUPS;

    public static void init() {
        AutoConfig.register(TieredConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(TieredConfig.class).getConfig();

        loadExtraTuningIngotConfig();
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

                // 🛠 Patch missing config fields
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
                if (data.tuningIngotConfigs != null) {
                    List<TuningIngotConfig> defs = createDefaultTuningConfigs();
                    java.util.Map<String, TuningIngotConfig> defByGroup = new java.util.HashMap<>();
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


                if (needsRewrite) {
                    writeTuningConfig(data);
                    Tiered.LOGGER.info("Patched missing fields in tiered_more.json.");
                }

            } catch (IOException | JsonSyntaxException e) {
                Tiered.LOGGER.error("Failed to load tiered_more.json", e);
                data = createDefaultTuningConfig();
                writeTuningConfig(data);
            }
        }

        // ✅ Now that it's guaranteed to be loaded and valid:
        CUSTOM_TUNING_INGOTS = data.tuningIngotConfigs;
        CUSTOM_TUNING_INGOT_LOOT_TABLES = data.tuningIngotLootTables;
        ALLOWED_REROLL_GROUPS = data.allowedRerollGroupsScroll;
    }
    private static TuningIngotConfigList createDefaultTuningConfig() {
        TuningIngotConfigList defaultData = new TuningIngotConfigList();
        defaultData.tuningIngotConfigs = createDefaultTuningConfigs();
        defaultData.tuningIngotLootTables = createDefaultLootTables();
        defaultData.allowedRerollGroupsScroll = createDefaultAllowedRerollGroups();
        return defaultData;
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

    private static void writeTuningConfig(TuningIngotConfigList data) {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Tiered.LOGGER.error("Failed to write tiered_more.json", e);
        }
    }

}
