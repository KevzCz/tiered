package draylar.tiered.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.SlotScalingConfig;
import draylar.tiered.util.ImprintSlots;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class LootTableSlotScalingLootFunction extends ConditionalLootFunction {

    public static final MapCodec<LootTableSlotScalingLootFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    addConditionsField(instance)
                            .apply(instance, LootTableSlotScalingLootFunction::new));

    public static final LootFunctionType<LootTableSlotScalingLootFunction> TYPE =
            new LootFunctionType<>(CODEC);

    public LootTableSlotScalingLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootFunctionType<LootTableSlotScalingLootFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        SlotScalingConfig cfg = ConfigInit.SLOT_SCALING;
        if (cfg == null || !cfg.enabled) return stack;
        if (!ImprintSlots.canEverHaveSlots(stack)) return stack;

        ServerWorld world = context.getWorld();
        if (world == null) return stack;
        String dimensionId = world.getRegistryKey().getValue().toString();

        Vec3d origin = context.get(LootContextParameters.ORIGIN);
        BlockPos pos = origin == null ? null : BlockPos.ofFloored(origin);

        StructureContext.Result here = StructureContext.resolve(world, pos);
        int bonus = cfg.resolveLootTableBonus(stack, dimensionId, here.ids(), here.tags());
        bonus = cfg.clampGlobalBonus(stack, bonus);
        if (bonus > 0) ImprintSlots.addBonusSlots(stack, bonus);
        return stack;
    }

    public static Builder<?> builder() {
        return builder(LootTableSlotScalingLootFunction::new);
    }
}
