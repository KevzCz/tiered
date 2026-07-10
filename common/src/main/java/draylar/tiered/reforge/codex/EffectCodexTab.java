package draylar.tiered.reforge.codex;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import draylar.tiered.Tiered;
import draylar.tiered.api.effect.EffectDefinition;

public final class EffectCodexTab implements CodexTab {

    private static final int EFFECT_PLATE = 0xFF4A4A6A;

    @Override
    public String id() {
        return "tiered:effects";
    }

    @Override
    public Component title() {
        return Component.translatable("screen.tiered.codex.tab.effects");
    }

    @Override
    public List<CodexEntry> buildEntries() {
        List<EffectDefinition> defs = new ArrayList<>(Tiered.EFFECT_DEFINITION_LOADER.getDefinitions().values());
        defs.sort((a, b) -> CodexUtil.idTail(a.getId()).compareToIgnoreCase(CodexUtil.idTail(b.getId())));

        List<CodexEntry> out = new ArrayList<>();
        for (EffectDefinition def : defs) {
            CodexEntry.Builder b = CodexEntry.builder(CodexUtil.idTail(def.getId()), EFFECT_PLATE);

            String desc = def.getDescription() != null
                    ? CodexUtil.translateOr(def.getDescription(), def.getDescription())
                    : CodexUtil.translateOr("screen.tiered.reforge.material.effect." + def.getType() + ".desc", null);
            b.description(desc);

            boolean percent = isPercentType(def.getType());
            if (def.getValueMin() != def.getValueMax()) {
                b.field("Value", fmt(def.getValueMin(), percent) + " – " + fmt(def.getValueMax(), percent));
            } else if (def.getValue() != 0f) {
                b.field("Value", fmt(def.getValue(), percent));
            }
            if (def.getImprint() != null) b.field("Grants", CodexUtil.idTail(def.getImprint()));

            var materials = CodexUtil.materialsForEffect(def.getId());
            if (!materials.isEmpty()) {
                b.relatedHeader(Component.translatable("screen.tiered.codex.from"));
                for (var stack : materials) b.related(stack);
            }
            out.add(b.build());
        }
        return out;
    }

    private static boolean isPercentType(String type) {
        return switch (type) {
            case "repair", "extract", "overcharge" -> true;
            default -> false;
        };
    }

    private static String fmt(float v, boolean percent) {
        return percent ? Math.round(v * 100) + "%" : CodexUtil.num(v);
    }
}
