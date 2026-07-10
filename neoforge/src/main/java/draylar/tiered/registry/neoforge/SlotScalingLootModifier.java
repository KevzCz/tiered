package draylar.tiered.registry.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import draylar.tiered.registry.LootTableSlotScalingLootFunction;
import draylar.tiered.registry.ModLoot;
import draylar.tiered.registry.MobDropSlotScalingLootFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

public class SlotScalingLootModifier extends LootModifier {

    public static final MapCodec<SlotScalingLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    codecStart(instance).apply(instance, SlotScalingLootModifier::new));

    public SlotScalingLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        String tableId = resolveTableId(context);
        boolean entityTable = tableId != null && ModLoot.isEntityLootTable(tableId);

        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(generatedLoot.size());
        for (ItemStack stack : generatedLoot) {
            ItemStack scaled = entityTable
                    ? MobDropSlotScalingLootFunction.applyScaling(stack, context)
                    : LootTableSlotScalingLootFunction.applyScaling(stack, context);
            result.add(scaled);
        }
        return result;
    }

    private static String resolveTableId(LootContext context) {
        ResourceLocation id = context.getQueriedLootTableId();
        return id == null ? null : id.toString();
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
