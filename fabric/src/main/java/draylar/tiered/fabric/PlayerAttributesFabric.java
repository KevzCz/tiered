package draylar.tiered.fabric;

import draylar.tiered.api.CustomEntityAttributes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;

public final class PlayerAttributesFabric {

    public static void register() {
        AttributeSupplier.Builder builder = Player.createAttributes()
                .add(CustomEntityAttributes.CRIT_CHANCE)
                .add(CustomEntityAttributes.DIG_SPEED)
                .add(CustomEntityAttributes.DURABLE)
                .add(CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
        FabricDefaultAttributeRegistry.register(EntityType.PLAYER, builder);
    }
}
