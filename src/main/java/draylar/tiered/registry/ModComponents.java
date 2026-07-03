package draylar.tiered.registry;

import com.mojang.serialization.Codec;
import draylar.tiered.api.SpecialStatsComponent;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.RuneContentComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponents {
    public static ComponentType<String> MODIFIER_GROUP;
    public static ComponentType<SpecialStatsComponent> SPECIAL_STATS;
    public static ComponentType<ImprintComponent> IMPRINTS;
    public static ComponentType<RuneContentComponent> RUNE_CONTENT;

    public static ComponentType<Integer> IMPRINT_SLOTS;

    public static ComponentType<Integer> BONUS_IMPRINT_SLOTS;

    public static ComponentType<Integer> RUNE_SLOT_GRANTS;

    public static ComponentType<String> REFORGE_MATERIAL;

    public static void init() {
        MODIFIER_GROUP = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "modifier_group"),
                ComponentType.<String>builder().codec(Codec.STRING).build()
        );

        SPECIAL_STATS = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "special_stats"),
                ComponentType.<SpecialStatsComponent>builder()
                        .codec(SpecialStatsComponent.CODEC)
                        .packetCodec(SpecialStatsComponent.PACKET_CODEC)
                        .build()
        );

        IMPRINTS = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "imprints"),
                ComponentType.<ImprintComponent>builder()
                        .codec(ImprintComponent.CODEC)
                        .packetCodec(ImprintComponent.PACKET_CODEC)
                        .build()
        );

        RUNE_CONTENT = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "rune_content"),
                ComponentType.<RuneContentComponent>builder()
                        .codec(RuneContentComponent.CODEC)
                        .packetCodec(RuneContentComponent.PACKET_CODEC)
                        .build()
        );

        IMPRINT_SLOTS = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "imprint_slots"),
                ComponentType.<Integer>builder()
                        .codec(Codec.INT)
                        .packetCodec(PacketCodecs.INTEGER)
                        .build()
        );

        REFORGE_MATERIAL = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "reforge_material"),
                ComponentType.<String>builder()
                        .codec(Codec.STRING)
                        .packetCodec(PacketCodecs.STRING)
                        .build()
        );

        BONUS_IMPRINT_SLOTS = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "bonus_imprint_slots"),
                ComponentType.<Integer>builder()
                        .codec(Codec.INT)
                        .packetCodec(PacketCodecs.INTEGER)
                        .build()
        );

        RUNE_SLOT_GRANTS = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "rune_slot_grants"),
                ComponentType.<Integer>builder()
                        .codec(Codec.INT)
                        .packetCodec(PacketCodecs.INTEGER)
                        .build()
        );

    }
}
