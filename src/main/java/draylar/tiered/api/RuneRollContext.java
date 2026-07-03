package draylar.tiered.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class RuneRollContext {

    private final float entityMaxHealth;
    @Nullable private final EntityType<?> entityType;
    @Nullable private final RegistryKey<World> dimension;

    public RuneRollContext(float entityMaxHealth, @Nullable EntityType<?> entityType,
            @Nullable RegistryKey<World> dimension) {
        this.entityMaxHealth = entityMaxHealth;
        this.entityType = entityType;
        this.dimension = dimension;
    }

    public float entityMaxHealth() {
        return entityMaxHealth;
    }

    public boolean entityInTag(String entityTag) {
        if (entityType == null || entityTag == null) return false;
        String path = entityTag.startsWith("#") ? entityTag.substring(1) : entityTag;
        Identifier id = Identifier.tryParse(path);
        if (id == null) return false;
        return entityType.isIn(TagKey.of(RegistryKeys.ENTITY_TYPE, id));
    }

    public boolean isDimension(String dimensionId) {
        if (dimension == null || dimensionId == null) return false;
        return dimension.getValue().toString().equals(dimensionId);
    }
}
