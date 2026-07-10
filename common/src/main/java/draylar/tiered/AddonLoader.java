package draylar.tiered;

import dev.architectury.injectables.annotations.ExpectPlatform;
import draylar.tiered.api.TieredAddon;

import java.util.List;

public final class AddonLoader {

    private AddonLoader() {
    }

    @ExpectPlatform
    public static List<TieredAddon> getAddons() {
        throw new AssertionError();
    }
}
