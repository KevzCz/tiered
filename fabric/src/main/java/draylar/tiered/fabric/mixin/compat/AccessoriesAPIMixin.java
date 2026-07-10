package draylar.tiered.fabric.mixin.compat;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintAttribute;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.compat.ATCCompat;
import draylar.tiered.registry.ModComponents;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

@Pseudo
@Mixin(AccessoriesAPI.class)
public abstract class AccessoriesAPIMixin {

    @Inject(method = "getAttributeModifiers(Lnet/minecraft/world/item/ItemStack;Lio/wispforest/accessories/api/slot/SlotReference;Z)Lio/wispforest/accessories/api/attributes/AccessoryAttributeBuilder;",
            at = @At("TAIL"))
    private static void injectTieredAttributes(ItemStack stack, SlotReference ref, boolean useTooltipCheck, CallbackInfoReturnable<AccessoryAttributeBuilder> cir) {
        if (stack.get(Tiered.TIER) != null) {
            ResourceLocation tier = ModifierUtils.getAttributeId(stack);
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            if (potentialAttribute != null) {
                for (AttributeTemplate template : potentialAttribute.getAttributes()) {
                    if (template.getOptionalAccessoriesSlots() == null) continue;
                    boolean matches = false;
                    for (String slotName : template.getOptionalAccessoriesSlots()) {
                        if (ATCCompat.matchesSlotName(slotName, ref.slotName())) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) continue;

                    Optional<Holder.Reference<Attribute>> optional = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(template.getAttributeTypeID()));
                    if (optional.isPresent()) {
                        String safeSlot = ref.slotName().toLowerCase().replace(':', '_').replaceAll("[^a-z0-9/._-]", "_");
                        AttributeModifier modifier = new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath("tiered", template.getEntityAttributeModifier().id().getPath() + "_" + safeSlot),
                                template.getEntityAttributeModifier().amount(),
                                template.getEntityAttributeModifier().operation()
                        );
                        cir.getReturnValue().addStackable(optional.get(), modifier);
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
                            Optional<Holder.Reference<Attribute>> optional = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(data.attributeTypeId()));
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
