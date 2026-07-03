package draylar.tiered.mixin.compat;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintAttribute;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.registry.ModComponents;
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
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Pseudo
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
                    if (template.getOptionalAccessoriesSlots() != null) {
                        for (String slotName : template.getOptionalAccessoriesSlots()) {
                            if (slotName.equalsIgnoreCase(ref.slotName())) {
                                Optional<RegistryEntry.Reference<EntityAttribute>> optional = Registries.ATTRIBUTE.getEntry(Identifier.of(template.getAttributeTypeID()));
                                if (optional.isPresent()) {
                                    String safeSlot = slotName.toLowerCase().replace(':', '_').replaceAll("[^a-z0-9/._-]", "_");
                                    EntityAttributeModifier modifier = new EntityAttributeModifier(
                                            Identifier.of("tiered", template.getEntityAttributeModifier().id().getPath() + "_" + safeSlot),
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

        ImprintComponent imprints = stack.get(ModComponents.IMPRINTS);
        if (imprints != null) {
            for (ImprintComponent.Entry entry : imprints.entries()) {
                Imprint imprint = ImprintRegistry.get(entry.id());
                if (imprint == null) continue;
                List<ImprintAttribute> attrList =
                        imprint instanceof DataImprint di
                                ? di.allAttributeData(entry, stack)
                                : List.of(imprint.attributeData(entry));
                for (ImprintAttribute data : attrList) {
                    if (data == null || data.accessoriesSlots() == null) continue;

                    if (data.anyWornSlot()) continue;
                    for (String slotName : data.accessoriesSlots()) {
                        if (slotName.equals("*") || slotName.equalsIgnoreCase(ref.slotName())) {
                            Optional<RegistryEntry.Reference<EntityAttribute>> optional = Registries.ATTRIBUTE.getEntry(Identifier.of(data.attributeTypeId()));
                            if (optional.isPresent()) {
                                cir.getReturnValue().addStackable(optional.get(), data.modifierForAccessory(ref.slotName()));
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
