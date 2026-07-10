package draylar.tiered.registry;

import draylar.tiered.Tiered;
import draylar.tiered.api.SpecialStatsComponent;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.RuneContentComponent;
import net.minecraft.core.component.DataComponentType;

public class ModComponents {
    public static DataComponentType<String> MODIFIER_GROUP;
    public static DataComponentType<SpecialStatsComponent> SPECIAL_STATS;
    public static DataComponentType<ImprintComponent> IMPRINTS;
    public static DataComponentType<RuneContentComponent> RUNE_CONTENT;

    public static DataComponentType<Integer> IMPRINT_SLOTS;

    public static DataComponentType<Integer> BONUS_IMPRINT_SLOTS;

    public static DataComponentType<Integer> RUNE_SLOT_GRANTS;

    public static DataComponentType<String> REFORGE_MATERIAL;

    public static void init() {
        Tiered.TIER = ModDataComponents.TIER.get();
        MODIFIER_GROUP = ModDataComponents.MODIFIER_GROUP.get();
        SPECIAL_STATS = ModDataComponents.SPECIAL_STATS.get();
        IMPRINTS = ModDataComponents.IMPRINTS.get();
        RUNE_CONTENT = ModDataComponents.RUNE_CONTENT.get();
        IMPRINT_SLOTS = ModDataComponents.IMPRINT_SLOTS.get();
        BONUS_IMPRINT_SLOTS = ModDataComponents.BONUS_IMPRINT_SLOTS.get();
        RUNE_SLOT_GRANTS = ModDataComponents.RUNE_SLOT_GRANTS.get();
        REFORGE_MATERIAL = ModDataComponents.REFORGE_MATERIAL.get();
    }
}
