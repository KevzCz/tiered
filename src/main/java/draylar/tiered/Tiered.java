package draylar.tiered;

import draylar.tiered.api.*;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.command.CommandInit;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TieredConfig;
import draylar.tiered.data.AttributeDataLoader;
import draylar.tiered.data.DataValidator;
import draylar.tiered.data.EffectDefinitionLoader;
import draylar.tiered.data.ImprintDefinitionLoader;
import draylar.tiered.data.ImprintSlotLoader;
import draylar.tiered.data.ReforgeDataLoader;
import draylar.tiered.data.ReforgeMaterialLoader;
import draylar.tiered.data.RuneInjection;
import draylar.tiered.data.RuneInjectionLoader;
import draylar.tiered.network.TieredServerPacket;
import draylar.tiered.reforge.ReforgeScreenHandler;
import draylar.tiered.registry.GeometricExtraCountLootFunction;
import draylar.tiered.registry.GrantBonusImprintSlotsLootFunction;
import draylar.tiered.registry.LootTableSlotScalingLootFunction;
import draylar.tiered.registry.MobDropSlotScalingLootFunction;
import draylar.tiered.registry.ModComponents;
import draylar.tiered.registry.BehaviorInit;
import draylar.tiered.registry.ModLoot;
import draylar.tiered.registry.RollRuneContentLootFunction;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.AnyOfLootCondition;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.LocationCheckLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.EmptyEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.EntityTypePredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import draylar.tiered.registry.ModItems;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class Tiered implements ModInitializer {

    public static final boolean isLevelZLoaded = FabricLoader.getInstance().isModLoaded("levelz");
    public static final boolean isSpellEngineLoaded = FabricLoader.getInstance().isModLoaded("spell_engine");

    public static final AttributeDataLoader ATTRIBUTE_DATA_LOADER = new AttributeDataLoader();

    public static final ReforgeDataLoader REFORGE_DATA_LOADER = new ReforgeDataLoader();

    public static final ReforgeMaterialLoader REFORGE_MATERIAL_LOADER = new ReforgeMaterialLoader();

    public static final RuneInjectionLoader RUNE_INJECTION_LOADER = new RuneInjectionLoader();

    public static final ImprintSlotLoader IMPRINT_SLOT_LOADER = new ImprintSlotLoader();

    public static final ImprintDefinitionLoader IMPRINT_DEFINITION_LOADER = new ImprintDefinitionLoader();

    public static final EffectDefinitionLoader EFFECT_DEFINITION_LOADER = new EffectDefinitionLoader();

    public static ScreenHandlerType<ReforgeScreenHandler> REFORGE_SCREEN_HANDLER_TYPE;

    public static final ComponentType<TierComponent> TIER = registerComponent("tiered:tier", builder -> builder.codec(TierComponent.CODEC).packetCodec(TierComponent.PACKET_CODEC));
    public static TieredConfig CONFIG;
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        ConfigInit.init();
        CONFIG = ConfigInit.CONFIG;

        invokeAddons();

        ModComponents.init();
        ImprintRegistry.init();
        BehaviorInit.init();
        TieredItemTags.init();
        ModItems.init();
        ModLoot.init();
        CustomEntityAttributes.init();
        CommandInit.init();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.ATTRIBUTE_DATA_LOADER);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.REFORGE_DATA_LOADER);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.REFORGE_MATERIAL_LOADER);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.IMPRINT_SLOT_LOADER);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.IMPRINT_DEFINITION_LOADER);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.EFFECT_DEFINITION_LOADER);
        registerRuneLootInjection();
        Registry.register(Registries.LOOT_FUNCTION_TYPE, Identifier.of("tiered", "geometric_extra_count"), GeometricExtraCountLootFunction.TYPE);
        Registry.register(Registries.LOOT_FUNCTION_TYPE, Identifier.of("tiered", "roll_rune_content"), RollRuneContentLootFunction.TYPE);
        Registry.register(Registries.LOOT_FUNCTION_TYPE, Identifier.of("tiered", "grant_bonus_imprint_slots"), GrantBonusImprintSlotsLootFunction.TYPE);
        Registry.register(Registries.LOOT_FUNCTION_TYPE, Identifier.of("tiered", "mob_drop_slot_scaling"), MobDropSlotScalingLootFunction.TYPE);
        Registry.register(Registries.LOOT_FUNCTION_TYPE, Identifier.of("tiered", "loot_table_slot_scaling"), LootTableSlotScalingLootFunction.TYPE);
        REFORGE_SCREEN_HANDLER_TYPE = Registry.register(Registries.SCREEN_HANDLER, "tiered:reforge",
                new ScreenHandlerType<>((syncId, inventory) -> new ReforgeScreenHandler(syncId, inventory, ScreenHandlerContext.EMPTY), FeatureFlags.VANILLA_FEATURES));

        TieredServerPacket.init();

        ServerPlayConnectionEvents.JOIN.register((network, packetSender, minecraftServer) -> {
            TieredServerPacket.writeS2CReforgeItemSyncPacket(network.getPlayer());
            TieredServerPacket.writeS2CAttributePacket(network.getPlayer());
            TieredServerPacket.writeS2CReforgeMaterialSyncPacket(network.getPlayer());
            TieredServerPacket.writeS2CImprintDataSyncPacket(network.getPlayer());
            TieredServerPacket.writeS2CHealthPacket(network.getPlayer());
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (success) {
                DataValidator.validate();
                DataValidator.validateEffects();
                for (int i = 0; i < server.getPlayerManager().getPlayerList().size(); i++) {
                    ModifierUtils.updateItemStackComponent(server.getPlayerManager().getPlayerList().get(i).getInventory());

                    TieredServerPacket.writeS2CImprintDataSyncPacket(server.getPlayerManager().getPlayerList().get(i));
                }
                LOGGER.info("Finished reload on {}", Thread.currentThread());
            } else {
                LOGGER.error("Failed to reload on {}", Thread.currentThread());
            }
        });

        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ModifierUtils.updateItemStackComponent(handler.player.getInventory());
        });

    }

    private static void registerRuneLootInjection() {
        LootTableEvents.ALL_LOADED.register((resourceManager, lootRegistry) -> {
            RUNE_INJECTION_LOADER.loadFrom(resourceManager);
        });

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            String tableId = key.getValue().toString();
            List<RuneInjection> matching = RUNE_INJECTION_LOADER.getMatching(tableId);
            if (matching.isEmpty()) return;

            for (RuneInjection injection : matching) {
                List<LootCondition.Builder> conditions =
                        buildContextConditions(injection, registries);

                for (RuneInjection.Pool injPool : injection.getPools()) {
                    List<RuneInjection.ItemCandidate> candidates = injPool.getItems();
                    if (candidates.isEmpty()) continue;

                    int cMin = injPool.getCountMin();
                    int cMax = injPool.getCountMax();
                    LootNumberProvider rollsProvider = cMin == cMax
                            ? ConstantLootNumberProvider.create(cMin)
                            : UniformLootNumberProvider.create(cMin, cMax);
                    LootPool.Builder pool = LootPool.builder()
                            .rolls(rollsProvider);

                    for (LootCondition.Builder cond : conditions) {
                        pool.conditionally(cond);
                    }

                    float chance = injPool.getChance();
                    int totalWeight = candidates.stream().mapToInt(RuneInjection.ItemCandidate::getWeight).sum();
                    if (chance < 1f && chance > 0f) {
                        int emptyWeight = Math.max(1, Math.round(totalWeight * (1f - chance) / chance));
                        pool.with(EmptyEntry.builder().weight(emptyWeight));
                    }

                    for (RuneInjection.ItemCandidate candidate : candidates) {
                        Item item = Registries.ITEM.get(Identifier.of(candidate.getItem()));
                        if (item == Items.AIR) continue;
                        pool.with(ItemEntry.builder(item)
                                .weight(candidate.getWeight())
                                .apply(RollRuneContentLootFunction.builder()));
                    }

                    tableBuilder.pool(pool);
                }
            }
        });
    }

    private static List<LootCondition.Builder> buildContextConditions(
            RuneInjection injection,
            RegistryWrapper.WrapperLookup registries) {
        List<LootCondition.Builder> out = new ArrayList<>();
        if (!injection.hasContextFilters()) return out;

        List<LootCondition.Builder> dimConds = new ArrayList<>();
        if (injection.getDimensions() != null) {
            for (String dim : injection.getDimensions()) {
                dimConds.add(LocationCheckLootCondition.builder(
                        LocationPredicate.Builder.create()
                                .dimension(RegistryKey.of(
                                        RegistryKeys.WORLD, Identifier.of(dim)))));
            }
        }
        if (injection.getDimensionTags() != null) {
            for (String tag : injection.getDimensionTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                dimConds.add(LocationCheckLootCondition.builder(
                        LocationPredicate.Builder.create()
                                .dimension(RegistryKey.of(
                                        RegistryKeys.WORLD, Identifier.of(id)))));
            }
        }
        if (!dimConds.isEmpty()) out.add(anyOf(dimConds));

        List<LootCondition.Builder> biomeConds = new ArrayList<>();
        var biomeReg = registries.getWrapperOrThrow(RegistryKeys.BIOME);
        if (injection.getBiomes() != null) {
            for (String biome : injection.getBiomes()) {
                biomeReg.getOptional(RegistryKey.of(RegistryKeys.BIOME, Identifier.of(biome)))
                        .ifPresent(entry -> biomeConds.add(LocationCheckLootCondition.builder(
                                LocationPredicate.Builder.create()
                                        .biome(RegistryEntryList.of(entry)))));
            }
        }
        if (injection.getBiomeTags() != null) {
            for (String tag : injection.getBiomeTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                var tagKey = TagKey.of(RegistryKeys.BIOME, Identifier.of(id));
                biomeReg.getOptional(tagKey).ifPresent(list -> biomeConds.add(
                        LocationCheckLootCondition.builder(
                                LocationPredicate.Builder.create().biome(list))));
            }
        }
        if (!biomeConds.isEmpty()) out.add(anyOf(biomeConds));

        List<LootCondition.Builder> structConds = new ArrayList<>();
        var structReg = registries.getWrapperOrThrow(RegistryKeys.STRUCTURE);
        if (injection.getStructures() != null) {
            for (String struct : injection.getStructures()) {
                structReg.getOptional(RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(struct)))
                        .ifPresent(entry -> structConds.add(LocationCheckLootCondition.builder(
                                LocationPredicate.Builder.create()
                                        .structure(RegistryEntryList.of(entry)))));
            }
        }
        if (injection.getStructureTags() != null) {
            for (String tag : injection.getStructureTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                var tagKey = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(id));
                structReg.getOptional(tagKey).ifPresent(list -> structConds.add(
                        LocationCheckLootCondition.builder(
                                LocationPredicate.Builder.create().structure(list))));
            }
        }
        if (!structConds.isEmpty()) out.add(anyOf(structConds));

        List<LootCondition.Builder> entConds = new ArrayList<>();
        if (injection.getEntities() != null) {
            for (String entId : injection.getEntities()) {
                EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(entId));
                if (type == null) continue;
                entConds.add(EntityPropertiesLootCondition.builder(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.create()
                                .type(EntityTypePredicate.create(type))));
            }
        }
        if (injection.getEntityTags() != null) {
            for (String tag : injection.getEntityTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                var tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(id));
                entConds.add(EntityPropertiesLootCondition.builder(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.create()
                                .type(EntityTypePredicate.create(tagKey))));
            }
        }
        if (!entConds.isEmpty()) out.add(anyOf(entConds));

        return out;
    }

    private static LootCondition.Builder anyOf(
            List<LootCondition.Builder> builders) {
        if (builders.size() == 1) return builders.get(0);
        AnyOfLootCondition.Builder any = null;
        for (LootCondition.Builder b : builders) {
            if (any == null) any = AnyOfLootCondition.builder(b);
            else any = any.or(b);
        }
        return any;
    }

    private static <T> ComponentType<T> registerComponent(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, id, builderOperator.apply(ComponentType.builder()).build());
    }

    public static Identifier id(String path) {
        return Identifier.of("tiered", path);
    }

    private static void invokeAddons() {
        for (TieredAddon addon : FabricLoader.getInstance()
                .getEntrypoints("tiered_more:addon", TieredAddon.class)) {
            try {
                addon.onTieredInit();
            } catch (Throwable t) {
                LOGGER.error("Tiered addon entrypoint failed", t);
            }
        }
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.getItem() instanceof Equipment equipment) {
            return equipment.getSlotType().equals(slot);
        }
        if (stack.getItem() instanceof ShieldItem || stack.getItem() instanceof RangedWeaponItem || stack.isIn(TieredItemTags.MAIN_OFFHAND_ITEM)) {
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        }
        return slot == EquipmentSlot.MAINHAND;
    }

}
