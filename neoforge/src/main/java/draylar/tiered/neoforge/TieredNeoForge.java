package draylar.tiered.neoforge;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import draylar.tiered.Tiered;
import draylar.tiered.TieredServer;
import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.neoforge.CooldownsImpl;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.registry.ModDataComponents;
import draylar.tiered.registry.ModItems;
import draylar.tiered.registry.ModMisc;
import draylar.tiered.registry.neoforge.ModLootImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("tiered_more")
public final class TieredNeoForge {
    public TieredNeoForge(IEventBus modEventBus) {
        ModDataComponents.register();
        CustomEntityAttributes.register();
        ModMisc.register();
        ImprintRegistry.touch();
        // ConfigInit is pure file I/O + Cloth Config registration, safe this early; ModItems.register()
        // needs CUSTOM_TUNING_INGOTS from it to build its dynamic item list before the ITEM registry freezes.
        ConfigInit.init();
        ModItems.register();
        PlayerAttributesNeoForge.register(modEventBus);
        PlayerDigSpeedNeoForge.register();
        CooldownsImpl.register(modEventBus);
        ModLootImpl.register(modEventBus);

        boolean isClient = Platform.getEnvironment() == Env.CLIENT;

        LifecycleEvent.SETUP.register(() -> {
            Tiered.init();
            TieredServer.init();

            if (isClient) {
                TieredNeoForgeClient.initAfterSetup();
            }
        });

        if (isClient) {
            TieredNeoForgeClient.init(modEventBus);
        }
    }
}
