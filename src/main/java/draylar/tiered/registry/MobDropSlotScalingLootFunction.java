package draylar.tiered.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import draylar.tiered.api.SpawnStructureHolder;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.SlotScalingConfig;
import draylar.tiered.util.ImprintSlots;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class MobDropSlotScalingLootFunction extends ConditionalLootFunction {

    public static final MapCodec<MobDropSlotScalingLootFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    addConditionsField(instance)
                            .apply(instance, MobDropSlotScalingLootFunction::new));

    public static final LootFunctionType<MobDropSlotScalingLootFunction> TYPE =
            new LootFunctionType<>(CODEC);

    public MobDropSlotScalingLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootFunctionType<MobDropSlotScalingLootFunction> getType() {
        return TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        SlotScalingConfig cfg = ConfigInit.SLOT_SCALING;
        if (cfg == null || !cfg.enabled) return stack;
        if (!ImprintSlots.canEverHaveSlots(stack)) return stack;

        Entity thisEntity = context.get(LootContextParameters.THIS_ENTITY);
        if (!(thisEntity instanceof LivingEntity living)) return stack;

        ServerWorld world = context.getWorld();
        if (world == null) return stack;
        String dimensionId = world.getRegistryKey().getValue().toString();
        String entityId = Registries.ENTITY_TYPE.getId(living.getType()).toString();

        Vec3d origin = context.get(LootContextParameters.ORIGIN);
        BlockPos pos = origin == null ? living.getBlockPos() : BlockPos.ofFloored(origin);

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

    public static Builder<?> builder() {
        return builder(MobDropSlotScalingLootFunction::new);
    }
}
