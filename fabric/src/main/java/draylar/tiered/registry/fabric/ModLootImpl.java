package draylar.tiered.registry.fabric;

import draylar.tiered.registry.LootTableSlotScalingLootFunction;
import draylar.tiered.registry.ModLoot;
import draylar.tiered.registry.MobDropSlotScalingLootFunction;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceLocation;

public final class ModLootImpl {

    private static final ResourceLocation LATE_PHASE = ResourceLocation.fromNamespaceAndPath("tiered", "slot_scaling_late");

    private ModLootImpl() {
    }

    public static void registerSlotScaling() {
        LootTableEvents.MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);

        LootTableEvents.MODIFY.register(LATE_PHASE, (key, tableBuilder, source, registries) -> {
            final String tableId = key.location().toString();
            if (ModLoot.isEntityLootTable(tableId)) {
                tableBuilder.modifyPools(pool -> pool.apply(MobDropSlotScalingLootFunction.builder()));
            } else {
                tableBuilder.modifyPools(pool -> pool.apply(LootTableSlotScalingLootFunction.builder()));
            }
        });
    }
}
