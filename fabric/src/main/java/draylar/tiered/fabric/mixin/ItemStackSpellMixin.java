package draylar.tiered.fabric.mixin;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.SpellTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = SpellContainerHelper.class, remap = false)
public class ItemStackSpellMixin {

    @Inject(method = "containerFromItemStack", at = @At("RETURN"), cancellable = true, remap = false)
    private static void containerFromItemStackMixin(ItemStack stack, CallbackInfoReturnable<SpellContainer> cir) {
        if (!Tiered.isSpellEngineLoaded) return;

        try {
            ResourceLocation tier = ModifierUtils.getAttributeId(stack);
            if (tier == null) return;

            PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            if (attribute == null) return;

            List<SpellTemplate> spellTemplates = attribute.getSpells();
            if (spellTemplates == null || spellTemplates.isEmpty()) return;

            List<String> modifierSpellIds = spellTemplates.stream()
                    .map(template -> template.getSpellId())
                    .collect(Collectors.toList());

            if (modifierSpellIds.isEmpty()) return;

            SpellContainer existingContainer = cir.getReturnValue();

            if (existingContainer != null && existingContainer.isValid()) {
                List<String> combinedSpells = new ArrayList<>(existingContainer.spell_ids());
                combinedSpells.addAll(modifierSpellIds);

                SpellContainer mergedContainer = new SpellContainer(
                        existingContainer.access(),
                        existingContainer.access_param(),
                        existingContainer.pool(),
                        existingContainer.slot(),
                        existingContainer.max_spell_count(),
                        combinedSpells,
                        existingContainer.extra_tier_binding()
                );
                cir.setReturnValue(mergedContainer);
            } else {
                SpellContainer newContainer = new SpellContainer(
                        SpellContainer.ContentType.MAGIC,
                        "",
                        "",
                        0,
                        modifierSpellIds
                );
                cir.setReturnValue(newContainer);
            }
        } catch (Throwable ignored) {}
    }
}
