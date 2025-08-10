package draylar.tiered.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;

public class GeometricExtraCountLootFunction extends ConditionalLootFunction {
    public static final MapCodec<GeometricExtraCountLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            addConditionsField(instance)
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(f -> f.chance))
                    .and(Codec.INT.fieldOf("minCount").forGetter(f -> f.minCount))
                    .and(Codec.INT.fieldOf("maxCount").forGetter(f -> f.maxCount))
                    .apply(instance, GeometricExtraCountLootFunction::new)
    );

    public static final LootFunctionType<GeometricExtraCountLootFunction> TYPE = new LootFunctionType<>(CODEC);

    private final float chance;
    private final int minCount;
    private final int maxCount;

    public GeometricExtraCountLootFunction(List<LootCondition> conditions, float chance, int minCount, int maxCount) {
        super(conditions);
        this.chance = chance;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    @Override
    public LootFunctionType<? extends ConditionalLootFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        int count = Math.max(0, minCount);
        while (count < maxCount && context.getRandom().nextFloat() < chance) {
            count++;
        }
        stack.setCount(Math.max(0, count));
        return stack;
    }

    public static Builder<?> builder(float chance, int minCount, int maxCount) {
        return builder(conditions -> new GeometricExtraCountLootFunction(conditions, chance, minCount, maxCount));
    }
}
