package draylar.tiered.mixin.compat;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AccessoriesAPI.class)
public abstract class AccessoriesAPIMixin {

    @Inject(method = "getAttributeModifiers(Lnet/minecraft/item/ItemStack;Lio/wispforest/accessories/api/slot/SlotReference;Z)Lio/wispforest/accessories/api/attributes/AccessoryAttributeBuilder;",
            at = @At("TAIL"))
    private static void injectTieredAttributes(ItemStack stack, SlotReference ref, boolean useTooltipCheck, CallbackInfoReturnable<AccessoryAttributeBuilder> cir) {
        if (stack.get(Tiered.TIER) != null) {
            Identifier tier = ModifierUtils.getAttributeId(stack);
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            if (potentialAttribute != null) {
                for (AttributeTemplate template : potentialAttribute.getAttributes()) {
                    if (template.appliesToAccessorySlot(ref.slotName())) {
                        Optional<RegistryEntry.Reference<EntityAttribute>> optional = Registries.ATTRIBUTE.getEntry(Identifier.of(template.getAttributeTypeID()));
                        if (optional.isPresent()) {
                            EntityAttributeModifier modifier = new EntityAttributeModifier(
                                    Identifier.of("tiered", template.getEntityAttributeModifier().id().getPath() + "_" + ref.slotName().toLowerCase()),
                                    template.getEntityAttributeModifier().value(),
                                    template.getEntityAttributeModifier().operation()
                            );
                            cir.getReturnValue().addStackable(optional.get(), modifier);
                        }
                    }
                }
            }
        }
    }
}