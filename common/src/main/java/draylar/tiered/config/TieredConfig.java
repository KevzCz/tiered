package draylar.tiered.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Config(name = "tiered_more/tiered")
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
    public boolean uniqueReforge = true;

    @Comment("Rune items and their loot-table injection")
    public boolean enableRuneItems = true;
    @Comment("Imprints, effects and behaviors: their data, imprint slots, application and reforge material imprint/effect pools")
    public boolean enableImprintsEffectsAndBehaviors = true;
    @Comment("Reforge material bias pools that shift rarity/group weights (rare-only, rarity boost, max rarity)")
    public boolean enableBiasPool = true;
    @Comment("The imprint/effect codex button and screen in the reforge UI")
    public boolean enableCodex = true;

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
