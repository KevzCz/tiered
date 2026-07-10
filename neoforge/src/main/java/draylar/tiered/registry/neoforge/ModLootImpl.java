package draylar.tiered.registry.neoforge;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootImpl {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "tiered");

    static {
        LOOT_MODIFIER_SERIALIZERS.register("slot_scaling", () -> SlotScalingLootModifier.CODEC);
    }

    private ModLootImpl() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
    }

    public static void registerSlotScaling() {
        // The actual hook-up happens via the data/tiered/loot_modifiers/slot_scaling.json
        // global loot modifier entry (referencing the "tiered:slot_scaling" serializer
        // registered above) plus register(modEventBus) called from the NeoForge entrypoint.
        // Nothing to do here at mod-init time; kept as a no-op to satisfy the common
        // ModLoot.registerSlotScaling() @ExpectPlatform contract.
    }
}
