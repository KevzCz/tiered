package draylar.tiered.neoforge;

import draylar.tiered.api.ImprintPlatesData;
import draylar.tiered.api.ReforgeMaterialBadgeData;
import draylar.tiered.util.ImprintPlatesComponent;
import draylar.tiered.util.ReforgeMaterialBadgeComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

public final class TieredTooltipComponentsNeoForge {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(TieredTooltipComponentsNeoForge::onRegister);
    }

    private static void onRegister(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ReforgeMaterialBadgeData.class, ReforgeMaterialBadgeComponent::new);
        event.register(ImprintPlatesData.class, ImprintPlatesComponent::new);
    }
}
