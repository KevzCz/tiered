package draylar.tiered.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.List;
import java.util.Map;

@Config(name = "tiered")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class TieredConfig implements ConfigData {

    @Comment("Items in for example mineshaft chests get modifiers")
    public boolean lootContainerModifier = true;
    @Comment("Equipped items on entities get modifiers")
    public boolean entityItemModifier = true;
    @Comment("Crafted items get modifiers")
    public boolean craftingModifier = true;
    @Comment("Merchant items get modifiers")
    public boolean merchantModifier = true;
    @Comment("Decreases the biggest weights by this modifier")
    public float reforgeModifier = 0.9F;
    @Comment("Modify the biggest weights by this modifier per smithing level")
    public float levelzReforgeModifier = 0.01F;
    @Comment("Modify the biggest weights by this modifier per luck")
    public float luckReforgeModifier = 0.02F;
    @Comment("Tuning ingot definitions with group, tooltip color, and loot chance")
    @ConfigEntry.Gui.Excluded
    public List<TuningIngotConfig> tuningIngotConfigs = List.of(
            new TuningIngotConfig("common", "gray", 0.3f),
            new TuningIngotConfig("uncommon", "dark_green", 0.2f),
            new TuningIngotConfig("rare", "blue", 0.15f),
            new TuningIngotConfig("epic", "dark_purple", 0.075f),
            new TuningIngotConfig("legendary", "gold", 0.035f),
            new TuningIngotConfig("unique", "light_purple", 0.01f)
    );

    @Comment("Loot tables that tuning ingots can be injected into. Requires Relaunching.")
    public List<String> tuningIngotLootTables = List.of(
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
    public boolean uniqueReforge = true;

    @ConfigEntry.Category("client_settings")
    public boolean showReforgingTab = true;
    @ConfigEntry.Category("client_settings")
    public int xIconPosition = 0;
    @ConfigEntry.Category("client_settings")
    public int yIconPosition = 0;
    @ConfigEntry.Category("client_settings")
    public boolean tieredTooltip = true;
    @ConfigEntry.Category("client_settings")
    public boolean centerName = true;

    @ConfigEntry.Category("client_settings")
    @Comment("Move modifier list and icon to the left side of the screen")
    public boolean leftSideModifierList = true;
}
