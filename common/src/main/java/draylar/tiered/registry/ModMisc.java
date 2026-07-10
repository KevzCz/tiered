package draylar.tiered.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import draylar.tiered.reforge.ReforgeScreenHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

// Same unsafe-static-init class of bug as ModDataComponents/CustomEntityAttributes/ModItems:
// these were previously registered via direct Registry.register(...) calls inside Tiered.init()
// (LifecycleEvent.SETUP), which is after NeoForge's RegisterEvent has fired and frozen the
// LOOT_FUNCTION_TYPE/MENU registries. Moved to a DeferredRegister committed during mod construction.
public final class ModMisc {

    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create("tiered_more", Registries.LOOT_FUNCTION_TYPE);

    public static final RegistrySupplier<LootItemFunctionType<?>> GEOMETRIC_EXTRA_COUNT =
            LOOT_FUNCTIONS.register(ResourceLocation.fromNamespaceAndPath("tiered", "geometric_extra_count"),
                    () -> GeometricExtraCountLootFunction.TYPE);

    public static final RegistrySupplier<LootItemFunctionType<?>> ROLL_RUNE_CONTENT =
            LOOT_FUNCTIONS.register(ResourceLocation.fromNamespaceAndPath("tiered", "roll_rune_content"),
                    () -> RollRuneContentLootFunction.TYPE);

    public static final RegistrySupplier<LootItemFunctionType<?>> GRANT_BONUS_IMPRINT_SLOTS =
            LOOT_FUNCTIONS.register(ResourceLocation.fromNamespaceAndPath("tiered", "grant_bonus_imprint_slots"),
                    () -> GrantBonusImprintSlotsLootFunction.TYPE);

    public static final RegistrySupplier<LootItemFunctionType<?>> MOB_DROP_SLOT_SCALING =
            LOOT_FUNCTIONS.register(ResourceLocation.fromNamespaceAndPath("tiered", "mob_drop_slot_scaling"),
                    () -> MobDropSlotScalingLootFunction.TYPE);

    public static final RegistrySupplier<LootItemFunctionType<?>> LOOT_TABLE_SLOT_SCALING =
            LOOT_FUNCTIONS.register(ResourceLocation.fromNamespaceAndPath("tiered", "loot_table_slot_scaling"),
                    () -> LootTableSlotScalingLootFunction.TYPE);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create("tiered_more", Registries.MENU);

    public static final RegistrySupplier<MenuType<?>> REFORGE_SCREEN_HANDLER =
            MENUS.register(ResourceLocation.fromNamespaceAndPath("tiered", "reforge"),
                    () -> new MenuType<>((syncId, inventory) -> new ReforgeScreenHandler(syncId, inventory, ContainerLevelAccess.NULL), FeatureFlags.VANILLA_SET));

    private ModMisc() {
    }

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        LOOT_FUNCTIONS.register();
        MENUS.register();
    }
}
