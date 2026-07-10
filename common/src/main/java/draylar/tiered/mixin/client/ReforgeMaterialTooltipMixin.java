package draylar.tiered.mixin.client;

import java.util.Optional;

import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.api.ReforgeMaterialBadgeData;
import draylar.tiered.util.ReforgeMaterialTooltip;
import draylar.tiered.util.ReforgeMaterials;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ReforgeMaterialTooltipMixin {

    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void tiered$reforgeMaterialBadges(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {

        if (cir.getReturnValue().isPresent()) return;
        if (!ReforgeMaterialTooltip.isExpanded()) return;

        ItemStack stack = (ItemStack) (Object) this;
        ReforgeMaterial material = ReforgeMaterials.resolve(stack);
        if (material == null) return;

        ReforgeMaterialBadgeData badges = ReforgeMaterialTooltip.buildBadges(material, stack);
        if (badges != null) {
            cir.setReturnValue(Optional.of(badges));
        }
    }
}
