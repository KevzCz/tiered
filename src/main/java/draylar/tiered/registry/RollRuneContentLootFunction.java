package draylar.tiered.registry;

import java.util.List;
import java.util.Random;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import draylar.tiered.api.RuneRollContext;
import draylar.tiered.api.imprint.RuneContent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class RollRuneContentLootFunction extends ConditionalLootFunction {

    public static final MapCodec<RollRuneContentLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            addConditionsField(instance).apply(instance, RollRuneContentLootFunction::new)
    );

    public static final LootFunctionType<RollRuneContentLootFunction> TYPE = new LootFunctionType<>(CODEC);

    public RollRuneContentLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootFunctionType<? extends ConditionalLootFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {

        Random rng = new Random(context.getRandom().nextLong());
        RuneContent.rollOnto(stack, rng, buildRollContext(context));
        return stack;
    }

    private static RuneRollContext buildRollContext(LootContext context) {
        float maxHealth = 0f;
        EntityType<?> type = null;
        var entity = context.get(LootContextParameters.THIS_ENTITY);
        if (entity instanceof LivingEntity living) {
            maxHealth = living.getMaxHealth();
            type = living.getType();
        } else if (entity != null) {
            type = entity.getType();
        }
        RegistryKey<World> dimension =
                context.getWorld() == null ? null : context.getWorld().getRegistryKey();
        return new RuneRollContext(maxHealth, type, dimension);
    }

    public static Builder<?> builder() {
        return builder(RollRuneContentLootFunction::new);
    }
}
