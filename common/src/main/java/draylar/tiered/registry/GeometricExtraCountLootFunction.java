package draylar.tiered.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class GeometricExtraCountLootFunction extends LootItemConditionalFunction {
    public static final MapCodec<GeometricExtraCountLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance)
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(f -> f.chance))
                    .and(Codec.INT.fieldOf("minCount").forGetter(f -> f.minCount))
                    .and(Codec.INT.fieldOf("maxCount").forGetter(f -> f.maxCount))
                    .apply(instance, GeometricExtraCountLootFunction::new)
    );

    public static final LootItemFunctionType<GeometricExtraCountLootFunction> TYPE = new LootItemFunctionType<>(CODEC);

    private final float chance;
    private final int minCount;
    private final int maxCount;

    public GeometricExtraCountLootFunction(List<LootItemCondition> conditions, float chance, int minCount, int maxCount) {
        super(conditions);
        this.chance = chance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        int count = Math.max(0, minCount);
        while (count < maxCount && context.getRandom().nextFloat() < chance) {
            count++;
        }
        stack.setCount(Math.max(0, count));
        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> builder(float chance, int minCount, int maxCount) {
        return simpleBuilder(conditions -> new GeometricExtraCountLootFunction(conditions, chance, minCount, maxCount));
    }
}
