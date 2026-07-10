package draylar.tiered.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.TierComponent;
import dev.architectury.event.events.common.CommandRegistrationEvent;

public class CommandInit {

    private static final List<String> TIER_LIST = List.of("common", "uncommon", "rare", "epic", "legendary", "unique");

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register((Commands.literal("tiered").requires((serverCommandSource) -> {
                return serverCommandSource.hasPermission(3);
            })).then(Commands.literal("tier").then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("common").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 0);
            })).then(Commands.literal("uncommon").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 1);
            })).then(Commands.literal("rare").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 2);
            })).then(Commands.literal("epic").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 3);
            })).then(Commands.literal("legendary").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 4);
            })).then(Commands.literal("unique").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 5);
            })))).then(Commands.literal("untier").then(Commands.argument("targets", EntityArgument.players()).executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), -1);
            }))).then(Commands.literal("reload").then(Commands.literal("slot_scaling").executes((commandContext) -> {
                ConfigInit.reloadSlotScalingConfig();
                commandContext.getSource().sendSuccess(() -> Component.translatable("commands.tiered.reload.slot_scaling"), true);
                return 1;
            }))).then(RuneCommand.build()).then(DumpCommand.build()));
        });
    }

    private static int executeCommand(CommandSourceStack source, Collection<ServerPlayer> targets, int tier) {

        for (ServerPlayer serverPlayerEntity : targets) {
            ItemStack itemStack = serverPlayerEntity.getMainHandItem();

            if (itemStack.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.tiered.failed", serverPlayerEntity.getDisplayName()), true);
                continue;
            }

            if (tier == -1) {
                if (itemStack.get(Tiered.TIER) != null) {
                    ModifierUtils.removeItemStackAttribute(itemStack);

                    source.sendSuccess(() -> Component.translatable("commands.tiered.untier", itemStack.getItem().getName(itemStack).getString(), serverPlayerEntity.getDisplayName()), true);
                } else {
                    source.sendSuccess(() -> Component.translatable("commands.tiered.untier_failed", itemStack.getItem().getName(itemStack).getString(), serverPlayerEntity.getDisplayName()), true);
                }
            } else {
                ArrayList<ResourceLocation> potentialAttributes = new ArrayList<ResourceLocation>();
                Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                    if (attribute.isValid(BuiltInRegistries.ITEM.getKey(itemStack.getItem()))) {
                        potentialAttributes.add(ResourceLocation.parse(attribute.getID()));
                    }
                });
                if (potentialAttributes.size() <= 0) {
                    source.sendSuccess(() -> Component.translatable("commands.tiered.tiering_failed", itemStack.getItem().getName(itemStack).getString(), serverPlayerEntity.getDisplayName()), true);
                    continue;
                } else {

                    List<ResourceLocation> potentialTier = new ArrayList<ResourceLocation>();
                    for (ResourceLocation potentialAttribute : potentialAttributes) {
                        if (potentialAttribute.getPath().contains(TIER_LIST.get(tier))) {
                            if (TIER_LIST.get(tier).equals("common") && potentialAttribute.getPath().contains("uncommon")) {
                                continue;
                            }
                            potentialTier.add(potentialAttribute);
                        }
                    }

                    if (potentialTier.size() <= 0) {
                        source.sendSuccess(() -> Component.translatable("commands.tiered.tiering_failed", itemStack.getItem().getName(itemStack).getString(), serverPlayerEntity.getDisplayName()), true);
                        continue;
                    } else {

                        ModifierUtils.removeItemStackAttribute(itemStack);

                        ResourceLocation attribute = potentialTier.get(serverPlayerEntity.level().getRandom().nextInt(potentialTier.size()));
                        if (attribute != null) {

                            float durableFactor = -1f;
                            int operation = 0;
                            List<AttributeTemplate> attributeList = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(ResourceLocation.parse(attribute.toString())).getAttributes();
                            for (AttributeTemplate attributeTemplate : attributeList) {
                                if (attributeTemplate.getAttributeTypeID().equals("tiered:generic.durable")) {
                                    durableFactor = (float) Math.round(attributeTemplate.getEntityAttributeModifier().amount() * 100.0f) / 100.0f;
                                    operation = attributeTemplate.getEntityAttributeModifier().operation().id();
                                    break;
                                }
                            }
                            itemStack.set(Tiered.TIER, new TierComponent(attribute.toString(), durableFactor, operation));

                            source.sendSuccess(() -> Component.translatable("commands.tiered.tier", itemStack.getItem().getName(itemStack).getString(), serverPlayerEntity.getDisplayName()), true);
                        }
                    }
                }
            }
        }
        return 1;
    }
}
