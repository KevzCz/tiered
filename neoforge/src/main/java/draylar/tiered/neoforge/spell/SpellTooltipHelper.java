package draylar.tiered.neoforge.spell;

import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.SpellTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_engine.client.input.Keybindings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpellTooltipHelper {

    public static void addGrantedSpellsTooltip(ItemStack stack, PotentialAttribute attribute, Player player, List<Component> tooltip, TooltipFlag context) {
        if (attribute.getSpells().isEmpty()) return;

        List<Holder<Spell>> spellEntries = new ArrayList<>();

        for (SpellTemplate spellTemplate : attribute.getSpells()) {
            try {
                ResourceLocation spellId = spellTemplate.getSpellIdentifier();
                var spellEntry = SpellRegistry.from(player.level()).getHolder(spellId);
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
                Component lastLine = tooltip.get(tooltip.size() - 1);
                if (!lastLine.getString().isBlank()) {
                    tooltip.add(Component.literal(""));
                }
            }
            tooltip.add(Component.translatable("spell.tooltip.host.list.spell")
                    .withStyle(ChatFormatting.GRAY));
            insertionIndex = tooltip.size();
        }

        boolean showDetails = SpellEngineClient.config.alwaysShowFullTooltip
                || isDetailsKeyPressed();

        int indentLevel = 1;

        for (int i = 0; i < spellEntries.size(); i++) {
            Holder<Spell> spellEntry = spellEntries.get(i);
            try {
                if (showDetails && i > 0) {
                    tooltip.add(insertionIndex++, Component.literal(" "));
                }

                List<Component> entry = SpellTooltip.spellEntry(spellEntry, player, stack, showDetails, indentLevel);
                for (Component line : entry) {
                    tooltip.add(insertionIndex++, line);
                }
            } catch (Throwable ignored) {}
        }

        if (!itemHasSpells && !showDetails && !spellEntries.isEmpty()) {
            var keybinding = Keybindings.bypass_spell_hotbar;
            if (!keybinding.isUnbound()) {
                tooltip.add(insertionIndex, Component.translatable("spell.tooltip.hold_for_details",
                                keybinding.getTranslatedKeyMessage())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static int findSpellSectionEnd(List<Component> tooltip) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            String text = tooltip.get(i).getString().trim();

            if (text.contains("Hold") && text.contains("for details")) {
                return i;
            }
        }

        for (int i = tooltip.size() - 1; i >= 0; i--) {
            Component line = tooltip.get(i);
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
            return keybinding.isDown();
        } catch (Throwable t) {
            return false;
        }
    }
}
