package draylar.tiered.fabric;

import draylar.tiered.api.TieredAddon;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public final class AddonLoaderImpl {

    private AddonLoaderImpl() {
    }

    public static List<TieredAddon> getAddons() {
        return FabricLoader.getInstance().getEntrypoints("tiered_more:addon", TieredAddon.class);
    }
}
