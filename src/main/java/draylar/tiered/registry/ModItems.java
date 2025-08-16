package draylar.tiered.registry;

import draylar.tiered.Tiered;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TuningIngotConfig;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ModItems {
    public static Item BLESSED_SCROLL;
    public static Item SPECIAL_TUNING_INGOT;

    public static final Map<String, Item> TUNING_INGOTS = new HashMap<>();
    public static ItemGroup TIERED_TAB;

    public static void init() {
        for (TuningIngotConfig entry : ConfigInit.CUSTOM_TUNING_INGOTS) {
            String group = entry.group;

            Item item = Registry.register(
                    Registries.ITEM,
                    Tiered.id("tuning_ingot_" + group.toLowerCase(Locale.ROOT)),
                    new TuningIngotItem(
                            new Item.Settings().rarity(Rarity.EPIC),
                            group, entry.color
                    ) {
                        @Override
                        public String getTranslationKey() {
                            return "item.tiered.tuning_ingot";
                        }
                    }
            );

            TUNING_INGOTS.put(group, item);
        }

        BLESSED_SCROLL = Registry.register(
                Registries.ITEM,
                Tiered.id("blessed_scroll"),
                new BlessedScrollItem(new Item.Settings().rarity(Rarity.EPIC))
        );

        SPECIAL_TUNING_INGOT = Registry.register(
                Registries.ITEM,
                Tiered.id("special_tuning_ingot"),
                new SpecialTuningIngotItem(new Item.Settings().rarity(Rarity.EPIC)) {
                    @Override public String getTranslationKey() { return "item.tiered.special_tuning_ingot"; }
                }
        );

        TIERED_TAB = FabricItemGroup.builder()
                .icon(() -> new ItemStack(TUNING_INGOTS.values().iterator().next()))
                .displayName(Text.translatable("itemGroup.tiered.tab"))
                .entries((context, entries) -> {
                    TUNING_INGOTS.values().forEach(entries::add);
                    entries.add(BLESSED_SCROLL);
                    if (SPECIAL_TUNING_INGOT != null) entries.add(SPECIAL_TUNING_INGOT);
                })
                .build();

        Registry.register(Registries.ITEM_GROUP, Tiered.id("tab"), TIERED_TAB);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> {
                    TUNING_INGOTS.values().forEach(entries::add);
                    if (SPECIAL_TUNING_INGOT != null) entries.add(SPECIAL_TUNING_INGOT);
                });
    }
}
