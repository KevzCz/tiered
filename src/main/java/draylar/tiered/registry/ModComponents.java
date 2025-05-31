package draylar.tiered.registry;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModComponents {
    public static ComponentType<String> MODIFIER_GROUP;

    public static void init() {
        MODIFIER_GROUP = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("tiered", "modifier_group"),
                ComponentType.<String>builder()
                        .codec(Codec.STRING)
                        .build()
        );
    }
}
