package draylar.tiered.registry;

import draylar.tiered.config.ConfigInit;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

import draylar.tiered.Tiered;
import draylar.tiered.config.TuningIngotConfig;
import draylar.tiered.config.TieredConfig;
import draylar.tiered.registry.ModItems;

public class ModLoot {
    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (ConfigInit.CUSTOM_TUNING_INGOT_LOOT_TABLES.contains(key.getValue().toString())) {
                for (TuningIngotConfig tuning : ConfigInit.CUSTOM_TUNING_INGOTS) {
                    LootPool pool = LootPool.builder()
                            .with(ItemEntry.builder(ModItems.TUNING_INGOTS.get(tuning.group))
                                    .conditionally(RandomChanceLootCondition.builder(tuning.lootChance)))
                            .rolls(ConstantLootNumberProvider.create(1))
                            .build();

                    tableBuilder.pool(pool);
                }
            }
        });
    }
}
