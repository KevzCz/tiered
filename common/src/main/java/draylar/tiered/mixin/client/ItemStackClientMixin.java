package draylar.tiered.mixin.client;

import draylar.tiered.Tiered;
import draylar.tiered.api.PotentialAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void getNameMixin(CallbackInfoReturnable<Component> info) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.get(Tiered.TIER) != null) {

            if (Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().containsKey(ResourceLocation.parse(stack.get(Tiered.TIER).tier()))) {
                PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(ResourceLocation.parse(stack.get(Tiered.TIER).tier()));

                if (potentialAttribute != null) {
                    MutableComponent label = Component.translatable(potentialAttribute.getID() + ".label");
                    if (!info.getReturnValue().getString().startsWith(label.getString())) {
                        info.setReturnValue(label.append(" ").append(info.getReturnValue()).setStyle(potentialAttribute.getStyle()));
                    }
                }
            }
        }
    }
}
