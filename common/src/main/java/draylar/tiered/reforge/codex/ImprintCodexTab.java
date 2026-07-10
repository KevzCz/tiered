package draylar.tiered.reforge.codex;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import draylar.tiered.Tiered;
import draylar.tiered.api.imprint.ImprintDefinition;
import draylar.tiered.reforge.codex.CodexEntry.AbilityRecipe;
import draylar.tiered.reforge.codex.CodexEntry.WorksWithTag;

public final class ImprintCodexTab implements CodexTab {

    @Override
    public String id() {
        return "tiered:imprints";
    }

    @Override
    public Component title() {
        return Component.translatable("screen.tiered.codex.tab.imprints");
    }

    @Override
    public List<CodexEntry> buildEntries() {
        List<ImprintDefinition> defs = new ArrayList<>(Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().values());
        defs.sort((a, b) -> displayName(a).compareToIgnoreCase(displayName(b)));

        List<CodexEntry> out = new ArrayList<>();
        for (ImprintDefinition def : defs) {
            int fill = CodexUtil.fillFromColor(def.getColor());
            CodexEntry.Builder b = CodexEntry.builder(displayName(def), fill);

            b.description(CodexUtil.imprintDescription(def));

            String display = def.getValueDisplay();

            if (def.hasValueRange() && def.getValueMin() != def.getValueMax()) {
                b.field("Value", rangeWithCap(CodexUtil.fmtValue(def.getValueMin(), display)
                        + " – " + CodexUtil.fmtValue(def.getValueMax(), display), def.getMaxBonus(), display));
            } else if (def.hasValueRange() || def.getValue() != 0f) {
                b.field("Value", rangeWithCap(CodexUtil.fmtValue(def.hasValueRange() ? def.getValueMin() : def.getValue(), display),
                        def.getMaxBonus(), display));
            }
            if (def.getMaxStacks() > 0) b.field("Max stacks", String.valueOf(def.getMaxStacks()));

            for (ImprintDefinition.ExtraRange r : CodexUtil.extraRanges(def)) {
                String label = CodexUtil.capitalize(r.getKey() == null ? "Extra" : r.getKey());
                String rd = r.getValueDisplay();
                String range = r.getValueMin() == r.getValueMax()
                        ? CodexUtil.fmtValue(r.getValueMin(), rd)
                        : CodexUtil.fmtValue(r.getValueMin(), rd) + " – " + CodexUtil.fmtValue(r.getValueMax(), rd);
                b.field(label, rangeWithCap(range, r.getMaxBonus(), rd));
            }
            if (!def.getGroups().isEmpty()) b.field("Group", String.join(", ", def.getGroups()));

            for (var recipe : CodexUtil.abilityRecipes(def)) {
                b.recipe(recipe);
            }

            for (var ww : CodexUtil.worksWithTags(def)) {
                b.worksWith(ww.label(), ww.icon(), ww.items());
            }

            var materials = CodexUtil.materialsForImprint(def.getId(), def.getGroups());
            if (!materials.isEmpty()) {
                b.relatedHeader(Component.translatable("screen.tiered.codex.from"));
                for (var stack : materials) b.related(stack);
            }
            out.add(b.build());
        }
        return out;
    }

    private static String rangeWithCap(String range, float maxBonus, String display) {
        return maxBonus != 0f ? range + " (max " + CodexUtil.fmtValue(maxBonus, display) + ")" : range;
    }

    private static String displayName(ImprintDefinition def) {
        String key = def.getNameKey() != null ? def.getNameKey()
                : (def.getLineKey() != null ? def.getLineKey() : null);
        return CodexUtil.translateOr(key, CodexUtil.idTail(def.getId()));
    }
}
