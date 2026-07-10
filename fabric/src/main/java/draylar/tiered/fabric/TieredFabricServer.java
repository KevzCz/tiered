package draylar.tiered.fabric;

import draylar.tiered.TieredServer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.SERVER)
public final class TieredFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        TieredServer.init();
    }
}
