package draylar.tiered.fabric;

import draylar.tiered.Tiered;
import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.fabric.compat.TrinketsCompat;
import draylar.tiered.registry.ModDataComponents;
import net.fabricmc.api.ModInitializer;

public final class TieredFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModDataComponents.register();
        CustomEntityAttributes.register();
        TrinketsCompat.init();
        Tiered.init();
        PlayerAttributesFabric.register();
    }
}
