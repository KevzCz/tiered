package draylar.tiered.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import draylar.tiered.Tiered;
import draylar.tiered.api.effect.ReforgeEffectRegistry;
import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.RuneContent;
import draylar.tiered.api.imprint.RuneContentComponent;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.registry.ModComponents;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class RuneCommand {

    private RuneCommand() {
    }

    private static final SuggestionProvider<ServerCommandSource> IMPRINT_IDS = (ctx, builder) -> {
        for (String id : Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().keySet()) builder.suggest(id);
        for (Identifier id : ImprintRegistry.REGISTRY.getIds()) builder.suggest(id.toString());
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> EFFECT_IDS = (ctx, builder) -> {
        for (String id : Tiered.EFFECT_DEFINITION_LOADER.getDefinitions().keySet()) builder.suggest(id);
        for (String id : ReforgeEffectRegistry.builtinIds()) builder.suggest(id);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> ON_RUNE_IMPRINTS = (ctx, builder) -> {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            ItemStack stack = player.getMainHandStack();
            RuneContentComponent content = stack.get(ModComponents.RUNE_CONTENT);
            if (content != null) for (RuneContentComponent.Entry e : content.entries()) builder.suggest(e.imprintId());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> ON_RUNE_EFFECTS = (ctx, builder) -> {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            ItemStack stack = player.getMainHandStack();
            RuneContentComponent content = stack.get(ModComponents.RUNE_CONTENT);
            if (content != null) for (String id : content.rolledEffects()) builder.suggest(id);
        }
        return builder.buildFuture();
    };

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("rune")
                .then(CommandManager.literal("addimprint")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .suggests(IMPRINT_IDS)
                                .executes(ctx -> addImprint(ctx, Float.NaN))
                                .then(CommandManager.argument("value", FloatArgumentType.floatArg())
                                        .executes(ctx -> addImprint(ctx, FloatArgumentType.getFloat(ctx, "value"))))))
                .then(CommandManager.literal("addeffect")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .suggests(EFFECT_IDS)
                                .executes(RuneCommand::addEffect)))
                .then(CommandManager.literal("removeimprint")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .suggests(ON_RUNE_IMPRINTS)
                                .executes(RuneCommand::removeImprint)))
                .then(CommandManager.literal("removeeffect")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .suggests(ON_RUNE_EFFECTS)
                                .executes(RuneCommand::removeEffect)))
                .then(CommandManager.literal("setbias")
                        .then(CommandManager.literal("rarity_boost")
                                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                                        .executes(ctx -> setBias(ctx, BiasField.RARITY_BOOST))))
                        .then(CommandManager.literal("min_rarity")
                                .then(CommandManager.argument("rarity", StringArgumentType.word())
                                        .suggests(RARITY_NAMES)
                                        .executes(ctx -> setBias(ctx, BiasField.MIN_RARITY))))
                        .then(CommandManager.literal("max_rarity")
                                .then(CommandManager.argument("rarity", StringArgumentType.word())
                                        .suggests(RARITY_NAMES)
                                        .executes(ctx -> setBias(ctx, BiasField.MAX_RARITY))))
                        .then(CommandManager.literal("only_group")
                                .then(CommandManager.argument("group", StringArgumentType.word())
                                        .suggests(RARITY_NAMES)
                                        .executes(ctx -> setBias(ctx, BiasField.ADD_GROUP))))
                        .then(CommandManager.literal("favor")
                                .then(CommandManager.argument("group", StringArgumentType.word())
                                        .suggests(RARITY_NAMES)
                                        .then(CommandManager.argument("multiplier", FloatArgumentType.floatArg(0f))
                                                .executes(ctx -> setBias(ctx, BiasField.FAVOR)))))
                        .then(CommandManager.literal("clear").executes(ctx -> setBias(ctx, BiasField.CLEAR))))
                .then(CommandManager.literal("show").executes(RuneCommand::show))
                .then(CommandManager.literal("clear").executes(RuneCommand::clear));
    }

    private static final SuggestionProvider<ServerCommandSource> RARITY_NAMES = (ctx, builder) -> {
        List<String> order = ConfigInit.RARITY_ORDER;
        if (order != null) for (String r : order) builder.suggest(r);
        return builder.buildFuture();
    };

    private enum BiasField { RARITY_BOOST, MIN_RARITY, MAX_RARITY, ADD_GROUP, FAVOR, CLEAR }

    private static int setBias(CommandContext<ServerCommandSource> ctx, BiasField field) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ItemStack rune = heldRune(source, player);
        if (rune.isEmpty()) return 0;

        RuneContentComponent content = contentOf(rune);
        RuneContentComponent.RolledBias bias = content.rolledBias();

        List<String> groups = bias == null || bias.groups() == null ? null : new ArrayList<>(bias.groups());
        Map<String, Float> gwm = bias == null || bias.groupWeightMultipliers() == null
                ? null : new HashMap<>(bias.groupWeightMultipliers());
        float rarityBoost = bias == null ? 0f : bias.rarityBoost();
        String minRarity = bias == null ? null : bias.guaranteedMinRarity();
        String maxRarity = bias == null ? null : bias.maxRarity();

        String summary;
        boolean clear = false;
        switch (field) {
            case RARITY_BOOST -> {
                rarityBoost = FloatArgumentType.getFloat(ctx, "value");
                summary = "rarity_boost=" + rarityBoost;
            }
            case MIN_RARITY -> {
                minRarity = StringArgumentType.getString(ctx, "rarity");
                summary = "min_rarity=" + minRarity;
            }
            case MAX_RARITY -> {
                maxRarity = StringArgumentType.getString(ctx, "rarity");
                summary = "max_rarity=" + maxRarity;
            }
            case ADD_GROUP -> {
                String group = StringArgumentType.getString(ctx, "group");
                if (groups == null) groups = new ArrayList<>();
                if (!groups.contains(group)) groups.add(group);
                summary = "only_group+=" + group;
            }
            case FAVOR -> {
                String group = StringArgumentType.getString(ctx, "group");
                float mult = FloatArgumentType.getFloat(ctx, "multiplier");
                if (gwm == null) gwm = new HashMap<>();
                gwm.put(group, mult);
                summary = "favor " + group + " x" + mult;
            }
            default -> {
                clear = true;
                summary = "cleared";
            }
        }

        RuneContentComponent.RolledBias next = clear ? null
                : new RuneContentComponent.RolledBias(
                        groups == null || groups.isEmpty() ? null : groups,
                        gwm == null || gwm.isEmpty() ? null : gwm,
                        rarityBoost, minRarity, maxRarity);

        rune.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(
                true, content.entries(), content.rolledEffects(), content.rolledEffectParams(), next));
        final String shown = summary;
        source.sendFeedback(() -> Text.translatable("commands.tiered.rune.bias", shown), false);
        return 1;
    }

    private static ItemStack heldRune(ServerCommandSource source, ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || !RuneContent.isRune(stack.getItem())) {
            source.sendError(Text.translatable("commands.tiered.rune.not_a_rune"));
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private static RuneContentComponent contentOf(ItemStack stack) {
        RuneContentComponent existing = stack.get(ModComponents.RUNE_CONTENT);
        return existing == null ? RuneContentComponent.UNROLLED : existing;
    }

    private static int addImprint(CommandContext<ServerCommandSource> ctx, float value) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ItemStack rune = heldRune(source, player);
        if (rune.isEmpty()) return 0;

        String id = IdentifierArgumentType.getIdentifier(ctx, "id").toString();
        Imprint imprint = ImprintRegistry.get(id);
        if (imprint == null) {
            source.sendError(Text.translatable("commands.tiered.rune.unknown_imprint", id));
            return 0;
        }

        Random rng = new Random();
        float resolved = value;
        Map<String, Float> extras = Map.of();
        if (imprint instanceof DataImprint data) {
            if (Float.isNaN(resolved)) resolved = data.rollValue(rng);

            extras = data.rollExtras(rng);
        } else if (Float.isNaN(resolved)) {
            resolved = 0f;
        }

        RuneContentComponent content = contentOf(rune);
        List<RuneContentComponent.Entry> entries = new ArrayList<>(content.entries());
        entries.add(new RuneContentComponent.Entry(id, resolved, extras));
        rune.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(
                true, entries, content.rolledEffects(), content.rolledEffectParams(), content.rolledBias()));

        final float shown = resolved;
        source.sendFeedback(() -> Text.translatable("commands.tiered.rune.added_imprint", id, shown), false);
        return 1;
    }

    private static int removeImprint(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ItemStack rune = heldRune(source, player);
        if (rune.isEmpty()) return 0;

        String id = IdentifierArgumentType.getIdentifier(ctx, "id").toString();
        RuneContentComponent content = contentOf(rune);
        List<RuneContentComponent.Entry> entries = new ArrayList<>(content.entries());
        boolean removed = entries.removeIf(e -> e.imprintId().equals(id));
        if (!removed) {
            source.sendError(Text.translatable("commands.tiered.rune.not_present_imprint", id));
            return 0;
        }
        rune.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(
                true, entries, content.rolledEffects(), content.rolledEffectParams(), content.rolledBias()));
        source.sendFeedback(() -> Text.translatable("commands.tiered.rune.removed_imprint", id), false);
        return 1;
    }

    private static int removeEffect(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ItemStack rune = heldRune(source, player);
        if (rune.isEmpty()) return 0;

        String id = IdentifierArgumentType.getIdentifier(ctx, "id").toString();
        RuneContentComponent content = contentOf(rune);
        List<String> effects = new ArrayList<>(content.rolledEffects());
        if (!effects.remove(id)) {
            source.sendError(Text.translatable("commands.tiered.rune.not_present_effect", id));
            return 0;
        }
        rune.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(
                true, content.entries(), effects, content.rolledEffectParams(), content.rolledBias()));
        source.sendFeedback(() -> Text.translatable("commands.tiered.rune.removed_effect", id), false);
        return 1;
    }

    private static int addEffect(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ItemStack rune = heldRune(source, player);
        if (rune.isEmpty()) return 0;

        String id = IdentifierArgumentType.getIdentifier(ctx, "id").toString();
        if (ReforgeEffectRegistry.get(id) == null) {
            source.sendError(Text.translatable("commands.tiered.rune.unknown_effect", id));
            return 0;
        }

        RuneContentComponent content = contentOf(rune);
        if (content.rolledEffects().contains(id)) {
            source.sendError(Text.translatable("commands.tiered.rune.duplicate_effect", id));
            return 0;
        }

        List<String> effects = new ArrayList<>(content.rolledEffects());
        effects.add(id);
        rune.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(
                true, content.entries(), effects, content.rolledEffectParams(), content.rolledBias()));

        source.sendFeedback(() -> Text.translatable("commands.tiered.rune.added_effect", id), false);
        return 1;
    }

    private static int show(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ItemStack rune = heldRune(source, player);
        if (rune.isEmpty()) return 0;

        RuneContentComponent content = contentOf(rune);
        if (content.isEmpty() && content.rolledEffects().isEmpty()) {
            source.sendFeedback(() -> Text.translatable("commands.tiered.rune.empty"), false);
            return 1;
        }
        for (RuneContentComponent.Entry e : content.entries()) {
            source.sendFeedback(() -> Text.translatable("commands.tiered.rune.show_imprint", e.imprintId(), e.value()), false);
        }
        for (String effectId : content.rolledEffects()) {
            source.sendFeedback(() -> Text.translatable("commands.tiered.rune.show_effect", effectId), false);
        }
        RuneContentComponent.RolledBias bias = content.rolledBias();
        if (bias != null) {
            source.sendFeedback(() -> Text.translatable("commands.tiered.rune.show_bias",
                    bias.rarityBoost(),
                    bias.guaranteedMinRarity() == null ? "-" : bias.guaranteedMinRarity(),
                    bias.maxRarity() == null ? "-" : bias.maxRarity()), false);
            if (bias.groups() != null && !bias.groups().isEmpty()) {
                source.sendFeedback(() -> Text.translatable("commands.tiered.rune.show_bias_groups",
                        String.join(", ", bias.groups())), false);
            }
            if (bias.groupWeightMultipliers() != null) {
                bias.groupWeightMultipliers().forEach((g, m) ->
                        source.sendFeedback(() -> Text.translatable("commands.tiered.rune.show_bias_favor", g, m), false));
            }
        }
        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ItemStack rune = heldRune(source, player);
        if (rune.isEmpty()) return 0;

        rune.set(ModComponents.RUNE_CONTENT, new RuneContentComponent(
                true, List.of(), List.of(), Map.of(), null));
        source.sendFeedback(() -> Text.translatable("commands.tiered.rune.cleared"), false);
        return 1;
    }
}
