package draylar.tiered.neoforge;

import draylar.tiered.api.CustomEntityAttributes;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

public final class PlayerAttributesNeoForge {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(PlayerAttributesNeoForge::onEntityAttributeModification);
    }

    private static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        CustomEntityAttributes.init();
        event.add(EntityType.PLAYER, CustomEntityAttributes.CRIT_CHANCE);
        event.add(EntityType.PLAYER, CustomEntityAttributes.DIG_SPEED);
        event.add(EntityType.PLAYER, CustomEntityAttributes.DURABLE);
        event.add(EntityType.PLAYER, CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
    }
}
