package draylar.tiered.registry;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TuningIngotConfig;
import draylar.tiered.registry.GeometricExtraCountLootFunction;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.EmptyEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

public class ModLoot {
    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (ConfigInit.CUSTOM_TUNING_INGOT_LOOT_TABLES.contains(key.getValue().toString())) {
                final int SCALE = 1000;

                float sumP = 0f;
                for (TuningIngotConfig t : ConfigInit.CUSTOM_TUNING_INGOTS) {
                    float p = t.lootChance;
                    if (p < 0f) p = 0f;
                    if (p > 1f) p = 1f;
                    sumP += p;
                }

                float noneChance = Math.max(0f, 1f - sumP);

                LootPool.Builder poolBuilder = LootPool.builder().rolls(ConstantLootNumberProvider.create(1));

                int noneWeight = Math.max(0, Math.round(noneChance * SCALE));
                if (noneWeight > 0) {
                    poolBuilder.with(EmptyEntry.builder().weight(noneWeight));
                }

                for (TuningIngotConfig tuning : ConfigInit.CUSTOM_TUNING_INGOTS) {
                    float p = tuning.lootChance;
                    if (p <= 0f) continue;
                    if (p > 1f) p = 1f;

                    int weight = Math.max(0, Math.round(p * SCALE));
                    if (weight <= 0) continue;

                    ItemEntry.Builder<?> entry = ItemEntry.builder(ModItems.TUNING_INGOTS.get(tuning.group))
                            .weight(weight)
                            .apply(GeometricExtraCountLootFunction.builder(
                                    tuning.lootChance,
                                    Math.max(1, tuning.minCount),
                                    Math.max(Math.max(1, tuning.minCount), tuning.maxCount)
                            ));

                    poolBuilder.with(entry);
                }

                tableBuilder.pool(poolBuilder.build());
            }
        });
    }
}
