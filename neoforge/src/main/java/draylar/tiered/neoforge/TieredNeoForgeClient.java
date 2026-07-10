package draylar.tiered.neoforge;

import draylar.tiered.Tiered;
import draylar.tiered.TieredClient;
import draylar.tiered.neoforge.compat.CuriosCompat;
import draylar.tiered.neoforge.config.ConfigScreenFactory;
import draylar.tiered.neoforge.mixin.access.HandledScreensAccessor;
import draylar.tiered.reforge.ReforgeScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

final class TieredNeoForgeClient {

    private TieredNeoForgeClient() {
    }

    static void init(IEventBus modEventBus) {
        CuriosCompat.init();
        TieredTooltipComponentsNeoForge.register(modEventBus);

        ModList.get().getModContainerById("tiered_more").ifPresent(ConfigScreenFactory::register);
    }

    // Called from LifecycleEvent.SETUP, after Tiered.init() has already run and populated
    // Tiered.REFORGE_SCREEN_HANDLER_TYPE - unlike Fabric's onInitializeClient (which is
    // guaranteed to run after onInitialize), this mod's constructor calls init() directly,
    // before LifecycleEvent.SETUP, so anything depending on Tiered's init-time state must
    // be deferred here instead.
    static void initAfterSetup() {
        HandledScreensAccessor.tiered$register(Tiered.REFORGE_SCREEN_HANDLER_TYPE, ReforgeScreen::new);
        TieredClient.init();
    }
}
