package draylar.tiered.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import draylar.tiered.Tiered;
import draylar.tiered.api.effect.ReforgeEffectRegistry;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.behavior.ImprintBehaviorRegistry;
import draylar.tiered.api.imprint.condition.ScaleConditionRegistry;
import draylar.tiered.registry.ModItems;

public final class DumpCommand {

    private DumpCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("dump")
                .then(Commands.literal("behaviors").executes(ctx -> dump(ctx, "behaviors",
                        ImprintBehaviorRegistry.ids())))
                .then(Commands.literal("conditions").executes(ctx -> dump(ctx, "conditions",
                        ScaleConditionRegistry.ids())))
                .then(Commands.literal("effects").executes(ctx -> {
                    List<String> ids = new ArrayList<>(Tiered.EFFECT_DEFINITION_LOADER.getDefinitions().keySet());
                    ids.addAll(ReforgeEffectRegistry.builtinIds());
                    return dump(ctx, "effects", ids);
                }))
                .then(Commands.literal("imprints").executes(ctx -> {
                    List<String> ids = new ArrayList<>(Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().keySet());
                    for (var id : ImprintRegistry.REGISTRY.getIds()) ids.add(id.toString());
                    return dump(ctx, "imprints", ids);
                }))
                .then(Commands.literal("runes").executes(ctx -> dump(ctx, "runes", ModItems.RUNES.keySet())));
    }

    private static int dump(CommandContext<CommandSourceStack> ctx, String label, Collection<String> ids) {
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(String::compareTo);
        ctx.getSource().sendSuccess(() -> Component.literal(label + " (" + sorted.size() + "):")
                .withStyle(ChatFormatting.GOLD), false);
        for (String id : sorted) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + id).withStyle(ChatFormatting.GRAY), false);
        }
        return sorted.size();
    }
}
