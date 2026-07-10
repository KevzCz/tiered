package draylar.tiered.fabric.client;

import draylar.tiered.Tiered;
import draylar.tiered.TieredClient;
import draylar.tiered.fabric.TieredTooltipComponentsFabric;
import draylar.tiered.reforge.ReforgeScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class TieredFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Tiered.REFORGE_SCREEN_HANDLER_TYPE, ReforgeScreen::new);
        TieredTooltipComponentsFabric.register();
        TieredClient.init();
    }
}
