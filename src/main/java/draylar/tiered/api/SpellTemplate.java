package draylar.tiered.api;

import com.google.gson.annotations.SerializedName;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

public class SpellTemplate {
    @SerializedName("spell_id")
    private final String spellId;

    @SerializedName("required_equipment_slots")
    private final EquipmentSlot[] requiredEquipmentSlots;

    @SerializedName("optional_equipment_slots")
    private final EquipmentSlot[] optionalEquipmentSlots;

    @SerializedName("optional_accessories_slots")
    private final String[] optionalAccessoriesSlots;

    public SpellTemplate(String spellId, EquipmentSlot[] requiredEquipmentSlots, EquipmentSlot[] optionalEquipmentSlots, String[] optionalAccessoriesSlots) {
        this.spellId = spellId;
        this.requiredEquipmentSlots = requiredEquipmentSlots;
        this.optionalEquipmentSlots = optionalEquipmentSlots;
        this.optionalAccessoriesSlots = optionalAccessoriesSlots;
    }

    public String getSpellId() {
        return spellId;
    }

    public EquipmentSlot[] getRequiredEquipmentSlots() {
        return requiredEquipmentSlots;
    }

    public EquipmentSlot[] getOptionalEquipmentSlots() {
        return optionalEquipmentSlots;
    }

    public String[] getOptionalAccessoriesSlots() {
        return optionalAccessoriesSlots;
    }

    public boolean isValidForSlot(EquipmentSlot slot) {
        if (requiredEquipmentSlots != null) {
            for (EquipmentSlot requiredSlot : requiredEquipmentSlots) {
                if (requiredSlot == slot) {
                    return true;
                }
            }
        }
        if (optionalEquipmentSlots != null) {
            for (EquipmentSlot optionalSlot : optionalEquipmentSlots) {
                if (optionalSlot == slot) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isValidForAccessorySlot(String slotName) {
        if (optionalAccessoriesSlots != null) {
            for (String accessorySlot : optionalAccessoriesSlots) {
                if (accessorySlot.equalsIgnoreCase(slotName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOnlyForAccessories() {
        return (requiredEquipmentSlots == null || requiredEquipmentSlots.length == 0)
                && (optionalEquipmentSlots == null || optionalEquipmentSlots.length == 0)
                && optionalAccessoriesSlots != null && optionalAccessoriesSlots.length > 0;
    }

    public Identifier getSpellIdentifier() {
        return Identifier.of(spellId);
    }
}
