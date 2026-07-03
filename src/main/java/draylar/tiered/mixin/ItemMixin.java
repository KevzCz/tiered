package draylar.tiered.mixin;

import draylar.tiered.api.imprint.RuneContent;
import draylar.tiered.registry.ModComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.config.ConfigInit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void tieredImprintInventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo info) {

        if (!world.isClient()) {
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

    @Inject(method = "onCraftByPlayer", at = @At("TAIL"))
    private void onCraftByPlayerMixin(ItemStack stack, World world, PlayerEntity player, CallbackInfo info) {
        if (!world.isClient() && !stack.isEmpty() && ConfigInit.CONFIG.craftingModifier) {
            ModifierUtils.setItemStackAttribute(player, stack, false, null, true);
        }
    }

    @Inject(method = "onCraft", at = @At("TAIL"))
    private void onCraftMixin(ItemStack stack, World world, CallbackInfo info) {
        if (!world.isClient() && !stack.isEmpty() && ConfigInit.CONFIG.craftingModifier) {
            ModifierUtils.setItemStackAttribute(null, stack, false, null, true);
        }
    }

    @Inject(method = "getItemBarStep", at = @At("HEAD"), cancellable = true)
    private void getItemBarStepMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        if (stack.get(Tiered.TIER) != null && stack.get(Tiered.TIER).durable() > 0f) {
            info.setReturnValue(Math.round(13.0f - (float) stack.getDamage() * 13.0f / (float) stack.getMaxDamage()));
        }
    }

    @Inject(method = "getItemBarColor", at = @At("HEAD"), cancellable = true)
    private void getItemBarColorMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        if (stack.get(Tiered.TIER) != null && stack.get(Tiered.TIER).durable() > 0f) {
            float f = Math.max(0.0f, ((float) stack.getMaxDamage() - (float) stack.getDamage()) / (float) stack.getMaxDamage());
            info.setReturnValue(MathHelper.hsvToRgb(f / 3.0f, 1.0f, 1.0f));
        }
    }

}
