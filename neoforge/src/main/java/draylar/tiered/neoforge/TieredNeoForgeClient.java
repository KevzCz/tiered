package draylar.tiered.neoforge;

import draylar.tiered.Tiered;
import draylar.tiered.TieredClient;
import draylar.tiered.data.TooltipBorderLoader;
import draylar.tiered.neoforge.config.ConfigScreenFactory;
import draylar.tiered.neoforge.mixin.access.HandledScreensAccessor;
import draylar.tiered.reforge.ReforgeScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

final class TieredNeoForgeClient {

    private TieredNeoForgeClient() {
    }

    static void init(IEventBus modEventBus) {
        TieredTooltipComponentsNeoForge.register(modEventBus);
        TieredAccessoryTooltipNeoForge.register();

        modEventBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener(new TooltipBorderLoader()));

        ModList.get().getModContainerById("tiered_more").ifPresent(ConfigScreenFactory::register);
    }

    static void initAfterSetup() {
        HandledScreensAccessor.tiered$register(Tiered.REFORGE_SCREEN_HANDLER_TYPE, ReforgeScreen::new);
        TieredClient.init();
    }
}
