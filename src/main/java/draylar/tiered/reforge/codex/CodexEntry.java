package draylar.tiered.reforge.codex;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public final class CodexEntry {

    private final String name;
    private final int plateFill;
    @Nullable
    private final String description;
    private final List<String[]> fields;
    private final List<ItemStack> relatedItems;
    @Nullable
    private final Text relatedHeader;
    private final List<WorksWithTag> worksWith;
    private final List<AbilityRecipe> recipes;

    public record WorksWithTag(String label, ItemStack icon, List<ItemStack> items) {
    }

    // "<gate> + <self> = <ability>" recipe row + the ability's description, for ability imprints (Resonance).
    public record AbilityRecipe(String gateName, int gateFill, String selfName, int selfFill,
                                String abilityName, int abilityFill, @Nullable String description) {
    }

    private CodexEntry(String name, int plateFill, @Nullable String description,
            List<String[]> fields, List<ItemStack> relatedItems,
            @Nullable Text relatedHeader, List<WorksWithTag> worksWith, List<AbilityRecipe> recipes) {
        this.name = name;
        this.plateFill = plateFill;
        this.description = description;
        this.fields = fields;
        this.relatedItems = relatedItems;
        this.relatedHeader = relatedHeader;
        this.worksWith = worksWith;
        this.recipes = recipes;
    }

    public String name() { return name; }
    public int plateFill() { return plateFill; }
    @Nullable public String description() { return description; }
    public List<String[]> fields() { return fields; }
    public List<ItemStack> relatedItems() { return relatedItems; }
    @Nullable public Text relatedHeader() { return relatedHeader; }
    public List<WorksWithTag> worksWith() { return worksWith; }
    public List<AbilityRecipe> recipes() { return recipes; }

    public static Builder builder(String name, int plateFill) {
        return new Builder(name, plateFill);
    }

    public static final class Builder {
        private final String name;
        private final int plateFill;
        private String description;
        private final List<String[]> fields = new ArrayList<>();
        private final List<ItemStack> relatedItems = new ArrayList<>();
        private Text relatedHeader;
        private final List<WorksWithTag> worksWith = new ArrayList<>();
        private final List<AbilityRecipe> recipes = new ArrayList<>();

        private Builder(String name, int plateFill) {
            this.name = name;
            this.plateFill = plateFill;
        }

        public Builder description(@Nullable String desc) {
            if (desc != null && !desc.isBlank()) this.description = desc;
            return this;
        }

        public Builder field(String label, @Nullable String value) {
            if (value != null && !value.isBlank()) fields.add(new String[]{label, value});
            return this;
        }

        public Builder related(ItemStack stack) {
            if (stack != null && !stack.isEmpty()) relatedItems.add(stack);
            return this;
        }

        public Builder relatedHeader(Text header) {
            this.relatedHeader = header;
            return this;
        }

        public Builder worksWith(String label, ItemStack icon, List<ItemStack> items) {
            if (icon != null && !icon.isEmpty() && items != null && !items.isEmpty()) {
                worksWith.add(new WorksWithTag(label, icon, items));
            }
            return this;
        }

        public Builder recipe(AbilityRecipe r) {
            if (r != null) recipes.add(r);
            return this;
        }

        public CodexEntry build() {
            return new CodexEntry(name, plateFill, description, fields, relatedItems, relatedHeader, worksWith, recipes);
        }
    }
}
