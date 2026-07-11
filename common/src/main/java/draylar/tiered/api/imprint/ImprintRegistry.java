package draylar.tiered.api.imprint;

import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import draylar.tiered.Tiered;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class ImprintRegistry {

    public static final int MAX_IMPRINTS = 3;

    public static final Registrar<Imprint> REGISTRY =
            RegistrarManager.get("tiered_more")
                    .<Imprint>builder(ResourceLocation.fromNamespaceAndPath("tiered", "imprint"))
                    .build();

    private ImprintRegistry() {
    }

    public static void touch() {
    }

    public static void init() {
        if (REGISTRY == null) {
            throw new IllegalStateException("ImprintRegistry failed to build");
        }
    }

    public static Imprint register(String id, Imprint imprint) {
        return REGISTRY.register(ResourceLocation.parse(id), () -> imprint).get();
    }

    @Nullable
    public static Imprint get(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed != null) {
            Imprint builtin = REGISTRY.get(parsed);
            if (builtin != null) return builtin;
        }
        return Tiered.IMPRINT_DEFINITION_LOADER.get(id);
    }
}
