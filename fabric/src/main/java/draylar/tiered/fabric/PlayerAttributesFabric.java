package draylar.tiered.fabric;

import draylar.tiered.api.CustomEntityAttributes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.List;

public final class PlayerAttributesFabric {

    private PlayerAttributesFabric() {
    }

    public static void register() {
        AttributeSupplier current = DefaultAttributes.getSupplier(EntityType.PLAYER);
        AttributeSupplier.Builder builder = AttributeSupplier.builder();
        BuiltInRegistries.ATTRIBUTE.holders().filter(current::hasAttribute).forEach(attribute -> builder.add(attribute, current.getBaseValue(attribute)));
        addTo(builder);
        FabricDefaultAttributeRegistry.register(EntityType.PLAYER, builder);
    }

    public static void addTo(AttributeSupplier.Builder builder) {
        if (CustomEntityAttributes.CRIT_CHANCE == null) {
            return;
        }
        for (Holder<Attribute> attribute : List.of(CustomEntityAttributes.CRIT_CHANCE, CustomEntityAttributes.DIG_SPEED, CustomEntityAttributes.DURABLE,
                CustomEntityAttributes.RANGE_ATTACK_DAMAGE)) {
            builder.add(attribute);
        }
    }
}
