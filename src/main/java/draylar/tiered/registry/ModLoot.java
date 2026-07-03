package draylar.tiered.registry;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TuningIngotConfig;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.EmptyEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

public class ModLoot {

    private static final Identifier LATE_PHASE = Identifier.of("tiered", "slot_scaling_late");

    public static void init() {

        LootTableEvents.MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);

        LootTableEvents.MODIFY.register(LATE_PHASE, (key, tableBuilder, source, registries) -> {
            final String tableId = key.getValue().toString();
            if (isEntityLootTable(tableId)) {
                tableBuilder.modifyPools(pool -> pool.apply(MobDropSlotScalingLootFunction.builder()));
            } else {
                tableBuilder.modifyPools(pool -> pool.apply(LootTableSlotScalingLootFunction.builder()));
            }
        });

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            final String tableId = key.getValue().toString();
            final int SCALE = 1000;

            if (ConfigInit.CUSTOM_TUNING_INGOT_LOOT_TABLES.contains(tableId)) {
                float sumP = 0f;
                for (TuningIngotConfig t : ConfigInit.CUSTOM_TUNING_INGOTS) {
                    float p = Math.max(0f, Math.min(1f, t.lootChance));
                    sumP += p;
                }
                float noneChance = Math.max(0f, 1f - sumP);

                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1));

                int noneWeight = Math.max(0, Math.round(noneChance * SCALE));
                if (noneWeight > 0) {
                    poolBuilder.with(EmptyEntry.builder().weight(noneWeight));
                }

                for (TuningIngotConfig tuning : ConfigInit.CUSTOM_TUNING_INGOTS) {
                    float p = Math.max(0f, Math.min(1f, tuning.lootChance));
                    if (p <= 0f) continue;

                    int weight = Math.max(0, Math.round(p * SCALE));
                    if (weight <= 0) continue;

                    poolBuilder.with(
                            ItemEntry.builder(ModItems.TUNING_INGOTS.get(tuning.group))
                                    .weight(weight)
                                    .apply(GeometricExtraCountLootFunction.builder(
                                            tuning.lootChance,
                                            Math.max(1, tuning.minCount),
                                            Math.max(Math.max(1, tuning.minCount), tuning.maxCount)
                                    ))
                    );
                }

                tableBuilder.pool(poolBuilder.build());
            }

            if (ConfigInit.SPECIAL_INGOT_ENABLED && ConfigInit.SPECIAL_INGOT != null) {
                var s = ConfigInit.SPECIAL_INGOT;

                if (s.lootTables != null && s.lootTables.contains(tableId)) {
                    float p = Math.max(0f, Math.min(1f, s.dropChance));
                    if (p > 0f) {
                        int itemWeight = Math.max(1, Math.round(p * SCALE));
                        int noneWeight = Math.max(0, SCALE - itemWeight);

                        LootPool.Builder specialPool = LootPool.builder()
                                .rolls(ConstantLootNumberProvider.create(1));

                        if (noneWeight > 0) {
                            specialPool.with(EmptyEntry.builder().weight(noneWeight));
                        }
                        specialPool.with(ItemEntry.builder(ModItems.SPECIAL_TUNING_INGOT).weight(itemWeight));

                        tableBuilder.pool(specialPool.build());
                    }
                }
            }
        });
    }

    private static boolean isEntityLootTable(String tableId) {
        int slash = tableId.indexOf(':');
        if (slash < 0) return false;
        return tableId.substring(slash + 1).startsWith("entities/");
    }
}
