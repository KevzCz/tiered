package draylar.tiered.util;

import draylar.tiered.Tiered;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.config.ConfigInit;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReforgeUtil {

    private static final List<String> DEFAULT_ORDER = List.of("common", "uncommon", "rare", "epic", "legendary", "unique");

    private static List<String> order() {
        List<String> configured = ConfigInit.RARITY_ORDER;
        return (configured == null || configured.isEmpty()) ? DEFAULT_ORDER : configured;
    }
    public static String getDynamicGroupName(Identifier id, Map<String, Integer> frequencyMap) {
        String path = id.getPath().toLowerCase();
        String[] parts = path.split("_");

        if (parts.length == 0) return "unknown";
        String prefix = parts[0];

        return frequencyMap.getOrDefault(prefix, 0) > 1 ? prefix : path;
    }

    public static String formatModifierName(Identifier id) {
        String path = id.getPath();
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1 && part.matches("\\d+")) {
                builder.append(" ").append(part);
            } else {
                builder.append(i == 0 ? capitalize(part) : " " + capitalize(part));
            }
        }

        return builder.toString();
    }

    public static int getColorForModifier(Identifier id) {
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (path.equals("special") || path.startsWith("special_")) {
            return 0xFF55FF;
        }

        PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(id);
        if (attribute != null && attribute.getStyle() != null && attribute.getStyle().getColor() != null) {
            String color = attribute.getStyle().getColor().getName();
            if (color!= null) {
                color = color.toLowerCase(Locale.ROOT);
            }

            return switch (color) {
                case "red" -> 0xFF5555;
                case "gold" -> 0xFFAA00;
                case "yellow" -> 0xFFFF55;
                case "green" -> 0x55FF55;
                case "dark_green" -> 0x005F00;
                case "aqua" -> 0x55FFFF;
                case "blue" -> 0x5555FF;
                case "purple", "light_purple" -> 0xFF55FF;
                case "dark_purple" -> 0xAA00AA;
                case "gray", "grey" -> 0xAAAAAA;
                case "dark_gray", "dark_grey" -> 0x555555;
                case "black" -> 0x000000;
                case "white" -> 0xFFFFFF;
                default -> 0xCCCCCC;
            };

        }
        return 0xCCCCCC;
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static List<Identifier> getAvailableModifiers(ItemStack stack) {
        List<Identifier> modifiers = new ArrayList<>();
        Map<Identifier, PotentialAttribute> allAttributes = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes();

        Identifier itemId = Registries.ITEM.getId(stack.getItem());

        for (Map.Entry<Identifier, PotentialAttribute> entry : allAttributes.entrySet()) {
            PotentialAttribute attribute = entry.getValue();
            if (attribute.isCursed()) continue;
            if (attribute.isValid(itemId)) {
                modifiers.add(entry.getKey());
            }
        }

        return modifiers;
    }

    public static int getRarityOrder(Identifier id) {
        List<String> order = order();
        for (int i = 0; i < order.size(); i++) {
            if (id.getPath().toLowerCase().contains(order.get(i))) {
                return i;
            }
        }
        return order.size();
    }

    public static int getNumericSuffixOrZero(Identifier id) {
        String path = id.getPath();
        int underscoreIndex = path.lastIndexOf('_');
        if (underscoreIndex != -1 && underscoreIndex < path.length() - 1) {
            try {
                return Integer.parseInt(path.substring(underscoreIndex + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public static int getRarityOrderExact(String group) {
        return order().indexOf(group.toLowerCase());
    }

    public static boolean isMaterialCompatible(ReforgeMaterial material, ItemStack target) {
        if (material == null || target.isEmpty()) return true;

        List<String> incompatible = material.getIncompatible();
        if (incompatible != null) {
            for (String entry : incompatible) {
                if (matchesEntry(entry, target)) return false;
            }
        }

        List<String> compatible = material.getCompatible();
        if (compatible == null || compatible.isEmpty()) return true;
        for (String entry : compatible) {
            if (matchesEntry(entry, target)) return true;
        }
        return false;
    }

    public static boolean matchesEntry(String entry, ItemStack target) {
        if (entry == null || entry.isEmpty()) return false;
        if (entry.startsWith("#")) {
            Identifier tagId = Identifier.of(entry.substring(1));
            return target.isIn(TagKey.of(Registries.ITEM.getKey(), tagId));
        }
        return Registries.ITEM.getId(target.getItem()).toString().equalsIgnoreCase(entry);
    }

    public static boolean matchesAny(List<String> entries, ItemStack target) {
        if (entries == null || entries.isEmpty()) return false;
        for (String entry : entries) {
            if (matchesEntry(entry, target)) return true;
        }
        return false;
    }
}
