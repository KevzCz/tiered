package draylar.tiered.neoforge;

import draylar.tiered.api.CustomEntityAttributes;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

// Attributes are attached here instead of via a shared Player.createAttributes mixin: feeding
// Architectury's RegistrySupplier-backed Holder into AttributeSupplier.Builder throws
// IncompatibleClassChangeError on NeoForge (conflicting default getKey() methods between
// Architectury's DeferredSupplier and NeoForge's IHolderExtension). EntityAttributeModificationEvent
// builds its own separate AttributeSupplier.Builder and only needs a plain vanilla Holder, which
// CustomEntityAttributes resolves fresh from BuiltInRegistries.
public final class PlayerAttributesNeoForge {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(PlayerAttributesNeoForge::onEntityAttributeModification);
    }

    private static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // RegisterEvent for ATTRIBUTE only fires (and populates BuiltInRegistries.ATTRIBUTE)
        // after mod construction; EntityAttributeModificationEvent fires later still, so this
        // is the earliest safe point to resolve the Holder fields.
        CustomEntityAttributes.init();
        event.add(EntityType.PLAYER, CustomEntityAttributes.CRIT_CHANCE);
        event.add(EntityType.PLAYER, CustomEntityAttributes.DIG_SPEED);
        event.add(EntityType.PLAYER, CustomEntityAttributes.DURABLE);
        event.add(EntityType.PLAYER, CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
    }
}
