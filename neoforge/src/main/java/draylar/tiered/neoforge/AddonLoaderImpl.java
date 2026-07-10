package draylar.tiered.neoforge;

import draylar.tiered.api.TieredAddon;

import java.util.List;

/**
 * No NeoForge addon exists yet (KevsTieredZModifiers is Fabric-only for now), so there is no
 * entrypoint-discovery mechanism to hook up here. Revisit if/when a NeoForge addon ships.
 */
public final class AddonLoaderImpl {

    private AddonLoaderImpl() {
    }

    public static List<TieredAddon> getAddons() {
        return List.of();
    }
}
