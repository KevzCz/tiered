package draylar.tiered.mixin;

import draylar.tiered.api.imprint.RuneContent;
import draylar.tiered.api.imprint.RuneContentComponent;
import draylar.tiered.registry.ModComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.config.ConfigInit;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void tieredImprintInventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected, CallbackInfo info) {

        if (!world.isClientSide()) {
            var runeContent = stack.get(ModComponents.RUNE_CONTENT);
            if ((runeContent == null || !runeContent.rolled()) && RuneContent.isRune(stack.getItem())) {
                RuneContent.rollOnto(stack, null);
            }
        }

        var component = stack.get(ModComponents.IMPRINTS);
        if (component == null) return;
        for (String id : component.ids()) {
            Imprint imprint = ImprintRegistry.get(id);
            if (imprint != null) imprint.inventoryTick(stack, world, entity, slot, selected);
        }
    }

    @Inject(method = "onCraftedBy", at = @At("TAIL"))
    private void onCraftByPlayerMixin(ItemStack stack, Level world, Player player, CallbackInfo info) {
        if (!world.isClientSide() && !stack.isEmpty() && ConfigInit.CONFIG.craftingModifier) {
            ModifierUtils.setItemStackAttribute(player, stack, false, null, true);
        }
    }

    @Inject(method = "onCraftedPostProcess", at = @At("TAIL"))
    private void onCraftMixin(ItemStack stack, Level world, CallbackInfo info) {
        if (!world.isClientSide() && !stack.isEmpty() && ConfigInit.CONFIG.craftingModifier) {
            ModifierUtils.setItemStackAttribute(null, stack, false, null, true);
        }
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void getItemBarStepMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        if (stack.get(Tiered.TIER) != null && stack.get(Tiered.TIER).durable() > 0f) {
            info.setReturnValue(Math.round(13.0f - (float) stack.getDamageValue() * 13.0f / (float) stack.getMaxDamage()));
        }
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void getItemBarColorMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        if (stack.get(Tiered.TIER) != null && stack.get(Tiered.TIER).durable() > 0f) {
            float f = Math.max(0.0f, ((float) stack.getMaxDamage() - (float) stack.getDamageValue()) / (float) stack.getMaxDamage());
            info.setReturnValue(Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f));
        }
    }

}
