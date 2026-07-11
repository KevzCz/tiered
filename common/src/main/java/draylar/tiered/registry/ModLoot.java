package draylar.tiered.registry;

import dev.architectury.event.events.common.LootEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.SpecialIngotConfig;
import draylar.tiered.config.TuningIngotConfig;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModLoot {

    public static void init() {
        registerSlotScaling();

        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            final String tableId = key.location().toString();
            final int SCALE = 1000;

            if (ConfigInit.CUSTOM_TUNING_INGOT_LOOT_TABLES.contains(tableId)) {
                float sumP = 0f;
                for (TuningIngotConfig t : ConfigInit.CUSTOM_TUNING_INGOTS) {
                    float p = Math.max(0f, Math.min(1f, t.lootChance));
                    sumP += p;
                }
                float noneChance = Math.max(0f, 1f - sumP);

                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1));

                int noneWeight = Math.max(0, Math.round(noneChance * SCALE));
                if (noneWeight > 0) {
                    poolBuilder.add(EmptyLootItem.emptyItem().setWeight(noneWeight));
                }

                for (TuningIngotConfig tuning : ConfigInit.CUSTOM_TUNING_INGOTS) {
                    float p = Math.max(0f, Math.min(1f, tuning.lootChance));
                    if (p <= 0f) continue;

                    int weight = Math.max(0, Math.round(p * SCALE));
                    if (weight <= 0) continue;

                    poolBuilder.add(
                            LootItem.lootTableItem(ModItems.TUNING_INGOTS.get(tuning.group))
                                    .setWeight(weight)
                                    .apply(GeometricExtraCountLootFunction.builder(
                                            tuning.lootChance,
                                            Math.max(1, tuning.minCount),
                                            Math.max(Math.max(1, tuning.minCount), tuning.maxCount)
                                    ))
                    );
                }

                context.addPool(poolBuilder);
            }

            if (ConfigInit.SPECIAL_INGOT_ENABLED && ConfigInit.SPECIAL_INGOT != null) {
                var s = ConfigInit.SPECIAL_INGOT;

                if (s.lootTables != null && s.lootTables.contains(tableId)) {
                    float p = Math.max(0f, Math.min(1f, s.dropChance));
                    if (p > 0f) {
                        int itemWeight = Math.max(1, Math.round(p * SCALE));
                        int noneWeight = Math.max(0, SCALE - itemWeight);

                        LootPool.Builder specialPool = LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1));

                        if (noneWeight > 0) {
                            specialPool.add(EmptyLootItem.emptyItem().setWeight(noneWeight));
                        }
                        specialPool.add(LootItem.lootTableItem(ModItems.SPECIAL_TUNING_INGOT).setWeight(itemWeight));

                        context.addPool(specialPool);
                    }
                }
            }
        });
    }

    @ExpectPlatform
    private static void registerSlotScaling() {
        throw new AssertionError();
    }

    public static boolean isEntityLootTable(String tableId) {
        int slash = tableId.indexOf(':');
        if (slash < 0) return false;
        return tableId.substring(slash + 1).startsWith("entities/");
    }
}
