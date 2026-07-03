package draylar.tiered.util;

import java.util.List;

import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.EligibilityPredicate;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintRegistry;
import net.minecraft.item.ItemStack;

public final class ImprintRequirements {

    private ImprintRequirements() {}

    public static List<ItemStack> icons(String imprintId) {
        EligibilityPredicate p = activePredicate(imprintId);
        return p == null ? List.of() : p.requirementIcons();
    }

    public static List<String> tags(String imprintId) {
        EligibilityPredicate p = activePredicate(imprintId);
        return p == null ? List.of() : p.requirementTags();
    }

    // Resonance plates: works-with = the weapon of the ability's mastered_when imprint.
    public static List<String> abilityTags(String imprintId, String abilityId) {
        if (abilityId == null || abilityId.isEmpty()) return List.of();
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (!(imprint instanceof DataImprint data)) return List.of();
        var binding = data.bindingByAbility(abilityId);
        if (binding == null || binding.getMasteredWhen() == null) return List.of();
        return tags(binding.getMasteredWhen());
    }

    private static EligibilityPredicate activePredicate(String imprintId) {
        Imprint imprint = ImprintRegistry.get(imprintId);
        if (!(imprint instanceof DataImprint data)) return null;
        return data.definition().getActiveWhen();
    }
}
