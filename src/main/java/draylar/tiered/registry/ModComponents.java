package draylar.tiered.registry;

import com.mojang.serialization.Codec;
import draylar.tiered.api.SpecialStatsComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponents {
    public static ComponentType<String> MODIFIER_GROUP;
    public static ComponentType<SpecialStatsComponent> SPECIAL_STATS;

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

    }
}