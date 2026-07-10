package draylar.tiered.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class RuneRollContext {

    private final float entityMaxHealth;
    @Nullable private final EntityType<?> entityType;
    @Nullable private final ResourceKey<Level> dimension;

    public RuneRollContext(float entityMaxHealth, @Nullable EntityType<?> entityType,
            @Nullable ResourceKey<Level> dimension) {
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
        ResourceLocation id = ResourceLocation.tryParse(path);
        if (id == null) return false;
        return entityType.is(TagKey.create(Registries.ENTITY_TYPE, id));
    }

    public boolean isDimension(String dimensionId) {
        if (dimension == null || dimensionId == null) return false;
        return dimension.location().toString().equals(dimensionId);
    }
}
