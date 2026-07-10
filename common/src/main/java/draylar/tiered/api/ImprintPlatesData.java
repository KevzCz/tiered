package draylar.tiered.api;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record ImprintPlatesData(Component header, List<SlotView> slots, int capacity, List<Component> siblingLines)
        implements TooltipComponent {

    public record SlotView(ItemStack rune, List<Plate> plates) {
    }

    public record Plate(Component label, int fillColor, float cooldownFill, boolean active, String imprintId, String abilityId) {

        public Plate(Component label, int fillColor) {
            this(label, fillColor, 1.0f, false, "", "");
        }

        public Plate(Component label, int fillColor, float cooldownFill, boolean active) {
            this(label, fillColor, cooldownFill, active, "", "");
        }

        public Plate(Component label, int fillColor, float cooldownFill, boolean active, String imprintId) {
            this(label, fillColor, cooldownFill, active, imprintId, "");
        }
    }
}
