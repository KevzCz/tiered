package draylar.tiered.registry;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import draylar.tiered.util.ImprintSlots;

public class GrantBonusImprintSlotsLootFunction extends LootItemConditionalFunction {

    public static final MapCodec<GrantBonusImprintSlotsLootFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance)
                    .and(Codec.INT.optionalFieldOf("count", 1).forGetter(f -> f.count))
                    .and(Codec.INT.optionalFieldOf("max_total", 0).forGetter(f -> f.maxTotal))
                    .apply(instance, GrantBonusImprintSlotsLootFunction::new)
    );

    public static final LootItemFunctionType<GrantBonusImprintSlotsLootFunction> TYPE = new LootItemFunctionType<>(CODEC);

    private final int count;
    private final int maxTotal;

    public GrantBonusImprintSlotsLootFunction(List<LootItemCondition> conditions, int count, int maxTotal) {
        super(conditions);
        this.count = count;
        this.maxTotal = maxTotal;
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        int toAdd = count;
        if (maxTotal > 0) {
            toAdd = Math.min(toAdd, maxTotal - ImprintSlots.capacity(stack));
        }
        if (toAdd > 0) ImprintSlots.addBonusSlots(stack, toAdd);
        return stack;
    }

    public static net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction.Builder<?> builder(int count, int maxTotal) {
        return simpleBuilder(conditions -> new GrantBonusImprintSlotsLootFunction(conditions, count, maxTotal));
    }
}
