package draylar.tiered.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import draylar.tiered.api.SpawnStructureHolder;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.SlotScalingConfig;
import draylar.tiered.util.ImprintSlots;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public class MobDropSlotScalingLootFunction extends LootItemConditionalFunction {

    public static final MapCodec<MobDropSlotScalingLootFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    commonFields(instance)
                            .apply(instance, MobDropSlotScalingLootFunction::new));

    public static final LootItemFunctionType<MobDropSlotScalingLootFunction> TYPE =
            new LootItemFunctionType<>(CODEC);

    public MobDropSlotScalingLootFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<MobDropSlotScalingLootFunction> getType() {
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

        Entity thisEntity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(thisEntity instanceof LivingEntity living)) return stack;

        ServerLevel world = context.getLevel();
        if (world == null) return stack;
        String dimensionId = world.dimension().location().toString();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();

        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        BlockPos pos = origin == null ? living.blockPosition() : BlockPos.containing(origin);

        StructureContext.Result here = StructureContext.resolve(world, pos);
        List<String> structureIds = new ArrayList<>(here.ids());
        List<String> structureTags = new ArrayList<>(here.tags());
        if (living instanceof SpawnStructureHolder holder) {
            for (String id : holder.tiered$getSpawnStructureIds()) {
                if (!structureIds.contains(id)) structureIds.add(id);
            }
            for (String tag : holder.tiered$getSpawnStructureTags()) {
                if (!structureTags.contains(tag)) structureTags.add(tag);
            }
        }

        int bonus = cfg.resolveMobBonus(stack, entityId, dimensionId, living.getMaxHealth(),
                structureIds, structureTags);
        bonus = cfg.clampGlobalBonus(stack, bonus);
        if (bonus > 0) ImprintSlots.addBonusSlots(stack, bonus);
        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return simpleBuilder(MobDropSlotScalingLootFunction::new);
    }
}
