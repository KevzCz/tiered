package draylar.tiered.api.imprint;

import draylar.tiered.Tiered;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class ImprintRegistry {

    public static final int MAX_IMPRINTS = 3;

    public static final RegistryKey<Registry<Imprint>> KEY =
            RegistryKey.ofRegistry(Identifier.of("tiered", "imprint"));

    public static final Registry<Imprint> REGISTRY =
            FabricRegistryBuilder.createSimple(KEY).buildAndRegister();

    private ImprintRegistry() {
    }

    public static void init() {
        if (REGISTRY == null) {
            throw new IllegalStateException("ImprintRegistry failed to build");
        }
    }

    public static Imprint register(String id, Imprint imprint) {
        return Registry.register(REGISTRY, Identifier.of(id), imprint);
    }

    @Nullable
    public static Imprint get(String id) {
        Identifier parsed = Identifier.tryParse(id);
        if (parsed != null) {
            Imprint builtin = REGISTRY.get(parsed);
            if (builtin != null) return builtin;
        }
        return Tiered.IMPRINT_DEFINITION_LOADER.get(id);
    }
}
