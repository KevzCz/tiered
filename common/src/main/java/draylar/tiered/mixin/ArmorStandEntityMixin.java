package draylar.tiered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

@Mixin(ArmorStand.class)
public abstract class ArmorStandEntityMixin {

    @Unique
    private boolean isGenerated = true;
    @Unique
    private boolean isClient = true;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCustomDataToNbtMixin(CompoundTag nbt, CallbackInfo info) {
        nbt.putBoolean("IsGenerated", this.isGenerated);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCustomDataFromNbtMixin(CompoundTag nbt, CallbackInfo info) {
        this.isGenerated = nbt.getBoolean("IsGenerated");
    }

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void interactAt(Player player, Vec3 hitPos, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        this.isGenerated = false;
        this.isClient = player.level().isClientSide();
    }

    @Inject(method = "setItemSlot", at = @At("HEAD"))
    private void equipStackMixin(EquipmentSlot slot, ItemStack stack, CallbackInfo info) {
        if (!this.isClient && this.isGenerated && ConfigInit.CONFIG.lootContainerModifier) {
            ModifierUtils.setItemStackAttribute(null, stack, false);
        }
    }

}
