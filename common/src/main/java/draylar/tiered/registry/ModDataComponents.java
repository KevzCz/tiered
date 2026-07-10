package draylar.tiered.registry;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import draylar.tiered.api.SpecialStatsComponent;
import draylar.tiered.api.TierComponent;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.RuneContentComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class ModDataComponents {

    // "tiered_more" is the FML mod id, used only to locate the mod event bus for the
    // NeoForge RegisterEvent hookup; resource ids below stay under the "tiered"
    // namespace to match the original Fabric mod and downstream addon expectations.
    @SuppressWarnings("unchecked")
    private static final ResourceKey<Registry<DataComponentType<?>>> DATA_COMPONENT_TYPE_KEY =
            Registries.DATA_COMPONENT_TYPE;

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create("tiered_more", DATA_COMPONENT_TYPE_KEY);

    public static final RegistrySupplier<DataComponentType<TierComponent>> TIER =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "tier"), () -> DataComponentType.<TierComponent>builder()
                    .persistent(TierComponent.CODEC)
                    .networkSynchronized(TierComponent.PACKET_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<String>> MODIFIER_GROUP =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "modifier_group"), () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .build());

    public static final RegistrySupplier<DataComponentType<SpecialStatsComponent>> SPECIAL_STATS =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "special_stats"), () -> DataComponentType.<SpecialStatsComponent>builder()
                    .persistent(SpecialStatsComponent.CODEC)
                    .networkSynchronized(SpecialStatsComponent.PACKET_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<ImprintComponent>> IMPRINTS =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "imprints"), () -> DataComponentType.<ImprintComponent>builder()
                    .persistent(ImprintComponent.CODEC)
                    .networkSynchronized(ImprintComponent.PACKET_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<RuneContentComponent>> RUNE_CONTENT =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "rune_content"), () -> DataComponentType.<RuneContentComponent>builder()
                    .persistent(RuneContentComponent.CODEC)
                    .networkSynchronized(RuneContentComponent.PACKET_CODEC)
                    .build());

    public static final RegistrySupplier<DataComponentType<Integer>> IMPRINT_SLOTS =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "imprint_slots"), () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final RegistrySupplier<DataComponentType<Integer>> BONUS_IMPRINT_SLOTS =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "bonus_imprint_slots"), () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final RegistrySupplier<DataComponentType<Integer>> RUNE_SLOT_GRANTS =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "rune_slot_grants"), () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final RegistrySupplier<DataComponentType<String>> REFORGE_MATERIAL =
            COMPONENTS.register(ResourceLocation.fromNamespaceAndPath("tiered", "reforge_material"), () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ModDataComponents() {
    }

    public static void register() {
        COMPONENTS.register();
    }
}
