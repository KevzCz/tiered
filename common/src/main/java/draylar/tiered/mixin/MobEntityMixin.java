package draylar.tiered.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.SpawnStructureHolder;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.registry.StructureContext;

@Mixin(Mob.class)
public abstract class MobEntityMixin implements SpawnStructureHolder {

    private static final String TIERED$PENDING_KEY = "TieredSpawnStructuresPending";
    private static final String TIERED$PENDING_POS_KEY = "TieredSpawnStructuresPos";

    private List<String> tiered$spawnStructureIds = List.of();
    private List<String> tiered$spawnStructureTags = List.of();
    private boolean tiered$spawnStructuresPending = false;
    private long tiered$pendingSpawnPos = 0L;

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void initializeMixin(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData, CallbackInfoReturnable<SpawnGroupData> info) {
        if (ConfigInit.CONFIG.entityItemModifier) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack itemStack = this.getItemBySlot(equipmentSlot);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ModifierUtils.setItemStackAttribute(null, itemStack, false);
            }
        }
        tiered$captureSpawnStructures(world);
    }

    private void tiered$captureSpawnStructures(ServerLevelAccessor world) {
        BlockPos pos = ((Mob) (Object) this).blockPosition();
        if (world instanceof WorldGenRegion) {
            this.tiered$spawnStructuresPending = true;
            this.tiered$pendingSpawnPos = pos.asLong();
            return;
        }
        if (world.getLevel() instanceof ServerLevel serverWorld) {
            tiered$applySpawnStructures(serverWorld, pos);
        }
    }

    private void tiered$applySpawnStructures(ServerLevel serverWorld, BlockPos pos) {
        StructureContext.Result r = StructureContext.resolve(serverWorld, pos);
        this.tiered$spawnStructureIds = r.ids();
        this.tiered$spawnStructureTags = r.tags();
        this.tiered$spawnStructuresPending = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tiered$resolvePendingSpawnStructures(CallbackInfo ci) {
        if (this.tiered$spawnStructuresPending && ((Mob) (Object) this).level() instanceof ServerLevel serverWorld) {
            tiered$applySpawnStructures(serverWorld, BlockPos.of(this.tiered$pendingSpawnPos));
        }
    }

    @Override
    public List<String> tiered$getSpawnStructureIds() {
        return tiered$spawnStructureIds == null ? List.of() : tiered$spawnStructureIds;
    }

    @Override
    public List<String> tiered$getSpawnStructureTags() {
        return tiered$spawnStructureTags == null ? List.of() : tiered$spawnStructureTags;
    }

    @Override
    public void tiered$setSpawnStructures(List<String> ids, List<String> tags) {
        this.tiered$spawnStructureIds = ids == null ? List.of() : ids;
        this.tiered$spawnStructureTags = tags == null ? List.of() : tags;
        this.tiered$spawnStructuresPending = false;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tiered$writeSpawnStructures(CompoundTag nbt, CallbackInfo ci) {
        if (!tiered$getSpawnStructureIds().isEmpty()) {
            nbt.put("TieredSpawnStructures", tiered$toNbtList(tiered$getSpawnStructureIds()));
        }
        if (!tiered$getSpawnStructureTags().isEmpty()) {
            nbt.put("TieredSpawnStructureTags", tiered$toNbtList(tiered$getSpawnStructureTags()));
        }
        if (this.tiered$spawnStructuresPending) {
            nbt.putBoolean(TIERED$PENDING_KEY, true);
            nbt.putLong(TIERED$PENDING_POS_KEY, this.tiered$pendingSpawnPos);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tiered$readSpawnStructures(CompoundTag nbt, CallbackInfo ci) {
        this.tiered$spawnStructureIds = tiered$fromNbtList(nbt, "TieredSpawnStructures");
        this.tiered$spawnStructureTags = tiered$fromNbtList(nbt, "TieredSpawnStructureTags");
        this.tiered$spawnStructuresPending = nbt.getBoolean(TIERED$PENDING_KEY);
        this.tiered$pendingSpawnPos = nbt.getLong(TIERED$PENDING_POS_KEY);
    }

    private static ListTag tiered$toNbtList(List<String> values) {
        ListTag list = new ListTag();
        for (String v : values) list.add(StringTag.valueOf(v));
        return list;
    }

    private static List<String> tiered$fromNbtList(CompoundTag nbt, String key) {
        if (!nbt.contains(key)) return List.of();
        ListTag list = nbt.getList(key, 8);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    @Shadow
    public abstract ItemStack getItemBySlot(EquipmentSlot slot);
}
