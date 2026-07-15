package draylar.tiered;

import dev.architectury.utils.GameInstance;
import draylar.tiered.api.*;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.command.CommandInit;
import draylar.tiered.compat.LevelZCompat;
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
import draylar.tiered.registry.ModComponents;
import draylar.tiered.registry.BehaviorInit;
import draylar.tiered.registry.ModItems;
import draylar.tiered.registry.ModLoot;
import draylar.tiered.registry.ModMisc;
import draylar.tiered.registry.RollRuneContentLootFunction;
import dev.architectury.event.events.common.LootEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

@SuppressWarnings("unused")
public class Tiered {

    public static final boolean isLevelZLoaded = LevelZCompat.isLoaded();
    public static final boolean isSpellEngineLoaded = Platform.isModLoaded("spell_engine");

    public static final AttributeDataLoader ATTRIBUTE_DATA_LOADER = new AttributeDataLoader();

    public static final ReforgeDataLoader REFORGE_DATA_LOADER = new ReforgeDataLoader();

    public static final ReforgeMaterialLoader REFORGE_MATERIAL_LOADER = new ReforgeMaterialLoader();

    public static final RuneInjectionLoader RUNE_INJECTION_LOADER = new RuneInjectionLoader();

    public static final ImprintSlotLoader IMPRINT_SLOT_LOADER = new ImprintSlotLoader();

    public static final ImprintDefinitionLoader IMPRINT_DEFINITION_LOADER = new ImprintDefinitionLoader();

    public static final EffectDefinitionLoader EFFECT_DEFINITION_LOADER = new EffectDefinitionLoader();

    public static MenuType<ReforgeScreenHandler> REFORGE_SCREEN_HANDLER_TYPE;

    public static DataComponentType<TierComponent> TIER;
    public static TieredConfig CONFIG;
    public static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        ConfigInit.init();
        CONFIG = ConfigInit.CONFIG;

        invokeAddons();

        ModItems.register();

        ModComponents.init();
        ImprintRegistry.init();
        BehaviorInit.init();
        TieredItemTags.init();
        ModItems.init();
        ModLoot.init();
        CustomEntityAttributes.init();
        CommandInit.init();

        ReloadListenerRegistry.register(PackType.SERVER_DATA, Tiered.ATTRIBUTE_DATA_LOADER);
        ReloadListenerRegistry.register(PackType.SERVER_DATA, Tiered.REFORGE_DATA_LOADER);
        ReloadListenerRegistry.register(PackType.SERVER_DATA, Tiered.REFORGE_MATERIAL_LOADER);
        ReloadListenerRegistry.register(PackType.SERVER_DATA, Tiered.IMPRINT_SLOT_LOADER);
        ReloadListenerRegistry.register(PackType.SERVER_DATA, Tiered.IMPRINT_DEFINITION_LOADER);
        ReloadListenerRegistry.register(PackType.SERVER_DATA, Tiered.EFFECT_DEFINITION_LOADER);
        registerRuneLootInjection();
        ModMisc.register();
        REFORGE_SCREEN_HANDLER_TYPE = (MenuType<ReforgeScreenHandler>) ModMisc.REFORGE_SCREEN_HANDLER.get();

        TieredServerPacket.init();

        PlayerEvent.PLAYER_JOIN.register(player -> {
            TieredServerPacket.writeS2CReforgeItemSyncPacket(player);
            TieredServerPacket.writeS2CAttributePacket(player);
            TieredServerPacket.writeS2CReforgeMaterialSyncPacket(player);
            TieredServerPacket.writeS2CImprintDataSyncPacket(player);
            TieredServerPacket.writeS2CHealthPacket(player);
            ModifierUtils.updateItemStackComponent(player.getInventory());
        });

        ReloadListenerRegistry.register(PackType.SERVER_DATA, (ResourceManagerReloadListener) resourceManager -> {
            DataValidator.validate();
            DataValidator.validateEffects();

            var server = GameInstance.getServer();
            if (server != null) {
                for (var player : server.getPlayerList().getPlayers()) {
                    ModifierUtils.updateItemStackComponent(player.getInventory());
                    TieredServerPacket.writeS2CImprintDataSyncPacket(player);
                }
            }

            LOGGER.info("Finished reload on {}", Thread.currentThread());
        });
    }

    private static void registerRuneLootInjection() {
        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            if (!ConfigInit.runeItemsEnabled()) return;
            String tableId = key.location().toString();
            List<RuneInjection> matching = RUNE_INJECTION_LOADER.getMatching(tableId);
            if (matching.isEmpty()) return;

            var server = GameInstance.getServer();
            if (server == null) return;
            HolderLookup.Provider registries = server.registryAccess();

            for (RuneInjection injection : matching) {
                List<LootItemCondition.Builder> conditions =
                        buildContextConditions(injection, registries);

                for (RuneInjection.Pool injPool : injection.getPools()) {
                    List<RuneInjection.ItemCandidate> candidates = injPool.getItems();
                    if (candidates.isEmpty()) continue;

                    int cMin = injPool.getCountMin();
                    int cMax = injPool.getCountMax();
                    NumberProvider rollsProvider = cMin == cMax
                            ? ConstantValue.exactly(cMin)
                            : UniformGenerator.between(cMin, cMax);
                    LootPool.Builder pool = LootPool.lootPool()
                            .setRolls(rollsProvider);

                    for (LootItemCondition.Builder cond : conditions) {
                        pool.when(cond);
                    }

                    float chance = injPool.getChance();
                    int totalWeight = candidates.stream().mapToInt(RuneInjection.ItemCandidate::getWeight).sum();
                    if (chance < 1f && chance > 0f) {
                        int emptyWeight = Math.max(1, Math.round(totalWeight * (1f - chance) / chance));
                        pool.add(EmptyLootItem.emptyItem().setWeight(emptyWeight));
                    }

                    for (RuneInjection.ItemCandidate candidate : candidates) {
                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(candidate.getItem()));
                        if (item == Items.AIR) continue;
                        pool.add(LootItem.lootTableItem(item)
                                .setWeight(candidate.getWeight())
                                .apply(RollRuneContentLootFunction.builder()));
                    }

                    context.addPool(pool);
                }
            }
        });
    }

    private static List<LootItemCondition.Builder> buildContextConditions(RuneInjection injection, HolderLookup.Provider registries) {
        List<LootItemCondition.Builder> out = new ArrayList<>();
        if (!injection.hasContextFilters()) return out;

        List<LootItemCondition.Builder> dimConds = new ArrayList<>();
        if (injection.getDimensions() != null) {
            for (String dim : injection.getDimensions()) {
                dimConds.add(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location()
                                .setDimension(ResourceKey.create(
                                        Registries.DIMENSION, ResourceLocation.parse(dim)))));
            }
        }
        if (injection.getDimensionTags() != null) {
            for (String tag : injection.getDimensionTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                dimConds.add(LocationCheck.checkLocation(
                        LocationPredicate.Builder.location()
                                .setDimension(ResourceKey.create(
                                        Registries.DIMENSION, ResourceLocation.parse(id)))));
            }
        }
        if (!dimConds.isEmpty()) out.add(anyOf(dimConds));

        List<LootItemCondition.Builder> biomeConds = new ArrayList<>();
        var biomeReg = registries.lookupOrThrow(Registries.BIOME);
        if (injection.getBiomes() != null) {
            for (String biome : injection.getBiomes()) {
                biomeReg.get(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biome)))
                        .ifPresent(entry -> biomeConds.add(LocationCheck.checkLocation(
                                LocationPredicate.Builder.location()
                                        .setBiomes(HolderSet.direct(entry)))));
            }
        }
        if (injection.getBiomeTags() != null) {
            for (String tag : injection.getBiomeTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                var tagKey = TagKey.create(Registries.BIOME, ResourceLocation.parse(id));
                biomeReg.get(tagKey).ifPresent(list -> biomeConds.add(
                        LocationCheck.checkLocation(
                                LocationPredicate.Builder.location().setBiomes(list))));
            }
        }
        if (!biomeConds.isEmpty()) out.add(anyOf(biomeConds));

        List<LootItemCondition.Builder> structConds = new ArrayList<>();
        var structReg = registries.lookupOrThrow(Registries.STRUCTURE);
        if (injection.getStructures() != null) {
            for (String struct : injection.getStructures()) {
                structReg.get(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(struct)))
                        .ifPresent(entry -> structConds.add(LocationCheck.checkLocation(
                                LocationPredicate.Builder.location()
                                        .setStructures(HolderSet.direct(entry)))));
            }
        }
        if (injection.getStructureTags() != null) {
            for (String tag : injection.getStructureTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                var tagKey = TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(id));
                structReg.get(tagKey).ifPresent(list -> structConds.add(
                        LocationCheck.checkLocation(
                                LocationPredicate.Builder.location().setStructures(list))));
            }
        }
        if (!structConds.isEmpty()) out.add(anyOf(structConds));

        List<LootItemCondition.Builder> entConds = new ArrayList<>();
        if (injection.getEntities() != null) {
            for (String entId : injection.getEntities()) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entId));
                if (type == null) continue;
                entConds.add(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity()
                                .entityType(EntityTypePredicate.of(type))));
            }
        }
        if (injection.getEntityTags() != null) {
            for (String tag : injection.getEntityTags()) {
                String id = tag.startsWith("#") ? tag.substring(1) : tag;
                var tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(id));
                entConds.add(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity()
                                .entityType(EntityTypePredicate.of(tagKey))));
            }
        }
        if (!entConds.isEmpty()) out.add(anyOf(entConds));

        return out;
    }

    private static LootItemCondition.Builder anyOf(
            List<LootItemCondition.Builder> builders) {
        if (builders.size() == 1) return builders.get(0);
        AnyOfCondition.Builder any = null;
        for (LootItemCondition.Builder b : builders) {
            if (any == null) any = AnyOfCondition.anyOf(b);
            else any = any.or(b);
        }
        return any;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("tiered", path);
    }

    private static void invokeAddons() {
        for (TieredAddon addon : AddonLoader.getAddons()) {
            try {
                addon.onTieredInit();
            } catch (Throwable t) {
                LOGGER.error("Tiered addon entrypoint failed", t);
            }
        }
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.getItem() instanceof Equipable equipment) {
            return equipment.getEquipmentSlot().equals(slot);
        }
        if (stack.getItem() instanceof ShieldItem || stack.getItem() instanceof ProjectileWeaponItem || stack.is(TieredItemTags.MAIN_OFFHAND_ITEM)) {
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        }
        return slot == EquipmentSlot.MAINHAND;
    }

}
