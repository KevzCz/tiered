package draylar.tiered.fabric;

import dev.architectury.registry.client.gui.ClientTooltipComponentRegistry;
import draylar.tiered.api.ImprintPlatesData;
import draylar.tiered.api.ReforgeMaterialBadgeData;
import draylar.tiered.util.ImprintPlatesComponent;
import draylar.tiered.util.ReforgeMaterialBadgeComponent;

public final class TieredTooltipComponentsFabric {

    private TieredTooltipComponentsFabric() {
    }

    public static void register() {
        ClientTooltipComponentRegistry.register(ReforgeMaterialBadgeData.class, ReforgeMaterialBadgeComponent::new);
        ClientTooltipComponentRegistry.register(ImprintPlatesData.class, ImprintPlatesComponent::new);
    }
}
