package draylar.tiered.spell;

import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.SpellTemplate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.mixin.client.keybinding.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_engine.client.input.Keybindings;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class SpellTooltipHelper {

    public static void addGrantedSpellsTooltip(ItemStack stack, PotentialAttribute attribute, PlayerEntity player, List<Text> tooltip, TooltipType context) {
        if (attribute.getSpells().isEmpty()) return;

        List<RegistryEntry<Spell>> spellEntries = new ArrayList<>();

        for (SpellTemplate spellTemplate : attribute.getSpells()) {
            try {
                Identifier spellId = spellTemplate.getSpellIdentifier();
                var spellEntry = SpellRegistry.from(player.getWorld()).getEntry(spellId);
                if (spellEntry.isPresent()) {
                    spellEntries.add(spellEntry.get());
                }
            } catch (Throwable ignored) {}
        }

        if (spellEntries.isEmpty()) return;

        var existingContainer = SpellContainerHelper.containerFromItemStack(stack);
        boolean itemHasSpells = existingContainer != null && existingContainer.isValid() && !existingContainer.spell_ids().isEmpty();

        int insertionIndex = tooltip.size();

        if (itemHasSpells) {
            insertionIndex = findSpellSectionEnd(tooltip);
        } else {
            if (!tooltip.isEmpty()) {
                Text lastLine = tooltip.get(tooltip.size() - 1);
                if (!lastLine.getString().isBlank()) {
                    tooltip.add(Text.literal(""));
                }
            }
            tooltip.add(Text.translatable("spell.tooltip.host.list.spell")
                    .formatted(Formatting.GRAY));
            insertionIndex = tooltip.size();
        }

        boolean showDetails = SpellEngineClient.config.alwaysShowFullTooltip
                || isDetailsKeyPressed();

        int indentLevel = 1;

        for (int i = 0; i < spellEntries.size(); i++) {
            RegistryEntry<Spell> spellEntry = spellEntries.get(i);
            try {
                if (showDetails && i > 0) {
                    tooltip.add(insertionIndex++, Text.literal(" "));
                }

                List<Text> entry = SpellTooltip.spellEntry(spellEntry, player, stack, showDetails, indentLevel);
                for (Text line : entry) {
                    tooltip.add(insertionIndex++, line);
                }
            } catch (Throwable ignored) {}
        }

        if (!itemHasSpells && !showDetails && !spellEntries.isEmpty()) {
            var keybinding = Keybindings.bypass_spell_hotbar;
            if (!keybinding.isUnbound()) {
                tooltip.add(insertionIndex, Text.translatable("spell.tooltip.hold_for_details",
                                keybinding.getBoundKeyLocalizedText())
                        .formatted(Formatting.DARK_GRAY));
            }
        }
    }

    private static int findSpellSectionEnd(List<Text> tooltip) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            String text = tooltip.get(i).getString().trim();

            if (text.contains("Hold") && text.contains("for details")) {
                return i;
            }
        }

        for (int i = tooltip.size() - 1; i >= 0; i--) {
            Text line = tooltip.get(i);
            String text = line.getString();

            if (text.trim().startsWith(" ") && !text.isBlank()) {
                continue;
            }

            if (text.isBlank()) {
                for (int j = i - 1; j >= 0; j--) {
                    String prevText = tooltip.get(j).getString();
                    if (prevText.trim().startsWith(" ") && !prevText.isBlank()) {
                        return i;
                    }
                }
            }
        }

        return tooltip.size();
    }

    private static boolean isDetailsKeyPressed() {
        try {
            var keybinding = Keybindings.bypass_spell_hotbar;
            if (keybinding.isUnbound()) return false;

            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            var boundKey = ((KeyBindingAccessor) keybinding).fabric_getBoundKey();

            int keyCode = boundKey.getCode();
            int action = GLFW.glfwGetKey(handle, keyCode);
            return action == GLFW.GLFW_PRESS;
        } catch (Throwable t) {
            return false;
        }
    }
}
