package draylar.tiered.registry;

import java.util.List;
import java.util.Random;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import draylar.tiered.api.RuneRollContext;
import draylar.tiered.api.imprint.RuneContent;

public class RollRuneContentLootFunction extends LootItemConditionalFunction {

    public static final MapCodec<RollRuneContentLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).apply(instance, RollRuneContentLootFunction::new)
    );

    public static final LootItemFunctionType<RollRuneContentLootFunction> TYPE = new LootItemFunctionType<>(CODEC);

    public RollRuneContentLootFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {

        Random rng = new Random(context.getRandom().nextLong());
        RuneContent.rollOnto(stack, rng, buildRollContext(context));
        return stack;
    }

    private static RuneRollContext buildRollContext(LootContext context) {
        float maxHealth = 0f;
        EntityType<?> type = null;
        var entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity instanceof LivingEntity living) {
            maxHealth = living.getMaxHealth();
            type = living.getType();
        } else if (entity != null) {
            type = entity.getType();
        }
        ResourceKey<Level> dimension =
                context.getLevel() == null ? null : context.getLevel().dimension();
        return new RuneRollContext(maxHealth, type, dimension);
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return simpleBuilder(RollRuneContentLootFunction::new);
    }
}
