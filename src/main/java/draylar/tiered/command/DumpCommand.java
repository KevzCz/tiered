package draylar.tiered.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import draylar.tiered.Tiered;
import draylar.tiered.api.effect.ReforgeEffectRegistry;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.behavior.ImprintBehaviorRegistry;
import draylar.tiered.api.imprint.condition.ScaleConditionRegistry;
import draylar.tiered.registry.ModItems;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class DumpCommand {

    private DumpCommand() {
    }

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("dump")
                .then(CommandManager.literal("behaviors").executes(ctx -> dump(ctx, "behaviors",
                        ImprintBehaviorRegistry.ids())))
                .then(CommandManager.literal("conditions").executes(ctx -> dump(ctx, "conditions",
                        ScaleConditionRegistry.ids())))
                .then(CommandManager.literal("effects").executes(ctx -> {
                    List<String> ids = new ArrayList<>(Tiered.EFFECT_DEFINITION_LOADER.getDefinitions().keySet());
                    ids.addAll(ReforgeEffectRegistry.builtinIds());
                    return dump(ctx, "effects", ids);
                }))
                .then(CommandManager.literal("imprints").executes(ctx -> {
                    List<String> ids = new ArrayList<>(Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().keySet());
                    for (var id : ImprintRegistry.REGISTRY.getIds()) ids.add(id.toString());
                    return dump(ctx, "imprints", ids);
                }))
                .then(CommandManager.literal("runes").executes(ctx -> dump(ctx, "runes", ModItems.RUNES.keySet())));
    }

    private static int dump(CommandContext<ServerCommandSource> ctx, String label, Collection<String> ids) {
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(String::compareTo);
        ctx.getSource().sendFeedback(() -> Text.literal(label + " (" + sorted.size() + "):")
                .formatted(Formatting.GOLD), false);
        for (String id : sorted) {
            ctx.getSource().sendFeedback(() -> Text.literal("  " + id).formatted(Formatting.GRAY), false);
        }
        return sorted.size();
    }
}
