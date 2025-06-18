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

    public static void init() {
        AutoConfig.register(TieredConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(TieredConfig.class).getConfig();

        loadExtraTuningIngotConfig();
    }

    private static void loadExtraTuningIngotConfig() {
        if (!FILE.exists()) {
            TuningIngotConfigList defaultData = new TuningIngotConfigList();
            defaultData.tuningIngotConfigs = List.of(
                    new TuningIngotConfig("common", "gray", 0.3f),
                    new TuningIngotConfig("uncommon", "dark_green", 0.2f),
                    new TuningIngotConfig("rare", "blue", 0.15f),
                    new TuningIngotConfig("epic", "dark_purple", 0.075f),
                    new TuningIngotConfig("legendary", "gold", 0.035f),
                    new TuningIngotConfig("unique", "light_purple", 0.01f)
            );
            defaultData.tuningIngotLootTables = List.of(
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

            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(defaultData, writer);
            } catch (IOException e) {
                Tiered.LOGGER.error("Failed to write default tiered_more.json", e);
            }
        }

        try (FileReader reader = new FileReader(FILE)) {
            TuningIngotConfigList data = GSON.fromJson(reader, TuningIngotConfigList.class);
            CUSTOM_TUNING_INGOTS = data.tuningIngotConfigs;
            CUSTOM_TUNING_INGOT_LOOT_TABLES = data.tuningIngotLootTables;
        } catch (IOException | JsonSyntaxException e) {
            Tiered.LOGGER.error("Failed to load tiered_more.json", e);
            CUSTOM_TUNING_INGOTS = List.of();
            CUSTOM_TUNING_INGOT_LOOT_TABLES = List.of();
        }
    }
}
