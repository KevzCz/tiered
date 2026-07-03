package draylar.tiered.mixin;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements SpawnStructureHolder {

    private List<String> tiered$spawnStructureIds = List.of();
    private List<String> tiered$spawnStructureTags = List.of();

    @Inject(method = "initialize", at = @At("TAIL"))
    private void initializeMixin(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, CallbackInfoReturnable<EntityData> info) {
        if (ConfigInit.CONFIG.entityItemModifier) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack itemStack = this.getEquippedStack(equipmentSlot);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ModifierUtils.setItemStackAttribute(null, itemStack, false);
            }
        }
        tiered$captureSpawnStructures(world);
    }

    private void tiered$captureSpawnStructures(ServerWorldAccess world) {
        if (world.toServerWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = ((MobEntity) (Object) this).getBlockPos();
            StructureContext.Result r = StructureContext.resolve(serverWorld, pos);
            this.tiered$spawnStructureIds = r.ids();
            this.tiered$spawnStructureTags = r.tags();
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
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void tiered$writeSpawnStructures(NbtCompound nbt, CallbackInfo ci) {
        if (!tiered$getSpawnStructureIds().isEmpty()) {
            nbt.put("TieredSpawnStructures", tiered$toNbtList(tiered$getSpawnStructureIds()));
        }
        if (!tiered$getSpawnStructureTags().isEmpty()) {
            nbt.put("TieredSpawnStructureTags", tiered$toNbtList(tiered$getSpawnStructureTags()));
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void tiered$readSpawnStructures(NbtCompound nbt, CallbackInfo ci) {
        this.tiered$spawnStructureIds = tiered$fromNbtList(nbt, "TieredSpawnStructures");
        this.tiered$spawnStructureTags = tiered$fromNbtList(nbt, "TieredSpawnStructureTags");
    }

    private static NbtList tiered$toNbtList(List<String> values) {
        NbtList list = new NbtList();
        for (String v : values) list.add(NbtString.of(v));
        return list;
    }

    private static List<String> tiered$fromNbtList(NbtCompound nbt, String key) {
        if (!nbt.contains(key)) return List.of();
        NbtList list = nbt.getList(key, 8);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot slot);
}
