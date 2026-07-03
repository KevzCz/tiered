package draylar.tiered.registry;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import draylar.tiered.util.ImprintSlots;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;

public class GrantBonusImprintSlotsLootFunction extends ConditionalLootFunction {

    public static final MapCodec<GrantBonusImprintSlotsLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            addConditionsField(instance)
                    .and(Codec.INT.optionalFieldOf("count", 1).forGetter(f -> f.count))
                    .and(Codec.INT.optionalFieldOf("max_total", 0).forGetter(f -> f.maxTotal))
                    .apply(instance, GrantBonusImprintSlotsLootFunction::new)
    );

    public static final LootFunctionType<GrantBonusImprintSlotsLootFunction> TYPE = new LootFunctionType<>(CODEC);

    private final int count;
    private final int maxTotal;

    public GrantBonusImprintSlotsLootFunction(List<LootCondition> conditions, int count, int maxTotal) {
        super(conditions);
        this.count = count;
        this.maxTotal = maxTotal;
    }

    @Override
    public LootFunctionType<? extends ConditionalLootFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        int toAdd = count;
        if (maxTotal > 0) {
            toAdd = Math.min(toAdd, maxTotal - ImprintSlots.capacity(stack));
        }
        if (toAdd > 0) ImprintSlots.addBonusSlots(stack, toAdd);
        return stack;
    }

    public static Builder<?> builder(int count, int maxTotal) {
        return builder(conditions -> new GrantBonusImprintSlotsLootFunction(conditions, count, maxTotal));
    }
}
