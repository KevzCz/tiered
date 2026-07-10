package draylar.tiered.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.SlotScalingConfig;
import draylar.tiered.util.ImprintSlots;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public class LootTableSlotScalingLootFunction extends LootItemConditionalFunction {

    public static final MapCodec<LootTableSlotScalingLootFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    commonFields(instance)
                            .apply(instance, LootTableSlotScalingLootFunction::new));

    public static final LootItemFunctionType<LootTableSlotScalingLootFunction> TYPE =
            new LootItemFunctionType<>(CODEC);

    public LootTableSlotScalingLootFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<LootTableSlotScalingLootFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        return applyScaling(stack, context);
    }

    public static ItemStack applyScaling(ItemStack stack, LootContext context) {
        SlotScalingConfig cfg = ConfigInit.SLOT_SCALING;
        if (cfg == null || !cfg.enabled) return stack;
        if (!ImprintSlots.canEverHaveSlots(stack)) return stack;

        ServerLevel world = context.getLevel();
        if (world == null) return stack;
        String dimensionId = world.dimension().location().toString();

        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        BlockPos pos = origin == null ? null : BlockPos.containing(origin);

        StructureContext.Result here = StructureContext.resolve(world, pos);
        int bonus = cfg.resolveLootTableBonus(stack, dimensionId, here.ids(), here.tags());
        bonus = cfg.clampGlobalBonus(stack, bonus);
        if (bonus > 0) ImprintSlots.addBonusSlots(stack, bonus);
        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return simpleBuilder(LootTableSlotScalingLootFunction::new);
    }
}
