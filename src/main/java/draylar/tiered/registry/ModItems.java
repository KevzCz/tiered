package draylar.tiered.registry;

import draylar.tiered.Tiered;
import draylar.tiered.config.TieredConfig;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ModItems {

    public static final Map<String, Item> TUNING_INGOTS = new HashMap<>();
    public static ItemGroup TIERED_TAB;

    public static void init() {
        TieredConfig config = Tiered.CONFIG; // however you access your config

        for (TuningIngotConfig entry : config.tuningIngotConfigs) {
            String group = entry.group;

            Item item = Registry.register(
                    Registries.ITEM,
                    Tiered.id("tuning_ingot_" + group.toLowerCase()),
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



        TIERED_TAB = FabricItemGroup.builder()
                .icon(() -> new ItemStack(TUNING_INGOTS.values().iterator().next()))
                .displayName(Text.translatable("itemGroup.tiered.tab"))
                .entries((context, entries) -> TUNING_INGOTS.values().forEach(entries::add))
                .build();
        Registry.register(Registries.ITEM_GROUP, Tiered.id("tab"), TIERED_TAB);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> TUNING_INGOTS.values().forEach(entries::add));
    }
}

