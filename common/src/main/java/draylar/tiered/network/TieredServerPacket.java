package draylar.tiered.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import draylar.tiered.Tiered;
import draylar.tiered.access.AnvilScreenHandlerAccess;
import draylar.tiered.data.AttributeDataLoader;
import draylar.tiered.data.EffectDefinitionLoader;
import draylar.tiered.data.ImprintDefinitionLoader;
import draylar.tiered.data.ImprintSlotLoader;
import draylar.tiered.network.packet.AttributePacket;
import draylar.tiered.network.packet.AutoRefillPacket;
import draylar.tiered.network.packet.ExtractSlotPacket;
import draylar.tiered.network.packet.HealthPacket;
import draylar.tiered.network.packet.ImprintDataSyncPacket;
import draylar.tiered.network.packet.MousePositionPacket;
import draylar.tiered.data.ReforgeMaterialLoader;
import draylar.tiered.network.packet.ReforgeItemSyncPacket;
import draylar.tiered.network.packet.ReforgeMaterialSyncPacket;
import draylar.tiered.network.packet.ReforgePacket;
import draylar.tiered.network.packet.ReforgeReadyPacket;
import draylar.tiered.network.packet.ReforgeScreenPacket;
import draylar.tiered.network.packet.StopAutoReforgePacket;
import draylar.tiered.reforge.ReforgeScreenHandler;

public class TieredServerPacket {

    public static void init() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(AttributePacket.PACKET_ID, AttributePacket.PACKET_CODEC);
            NetworkManager.registerS2CPayloadType(HealthPacket.PACKET_ID, HealthPacket.PACKET_CODEC);
            NetworkManager.registerS2CPayloadType(ReforgeReadyPacket.PACKET_ID, ReforgeReadyPacket.PACKET_CODEC);
            NetworkManager.registerS2CPayloadType(ReforgeItemSyncPacket.PACKET_ID, ReforgeItemSyncPacket.PACKET_CODEC);
            NetworkManager.registerS2CPayloadType(StopAutoReforgePacket.PACKET_ID, StopAutoReforgePacket.PACKET_CODEC);
            NetworkManager.registerS2CPayloadType(MousePositionPacket.PACKET_ID, MousePositionPacket.PACKET_CODEC);
            NetworkManager.registerS2CPayloadType(ReforgeMaterialSyncPacket.PACKET_ID, ReforgeMaterialSyncPacket.PACKET_CODEC);
            NetworkManager.registerS2CPayloadType(ImprintDataSyncPacket.PACKET_ID, ImprintDataSyncPacket.PACKET_CODEC);
        }

        NetworkManager.registerReceiver(NetworkManager.c2s(), ReforgeScreenPacket.PACKET_ID, ReforgeScreenPacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            int mouseX = payload.mouseX();
            int mouseY = payload.mouseY();
            Boolean reforgingScreen = payload.reforgingScreen();

            BlockPos pos = reforgingScreen ? (player.containerMenu instanceof AnvilMenu ? ((AnvilScreenHandlerAccess) player.containerMenu).getPos() : null)
                    : (player.containerMenu instanceof ReforgeScreenHandler ? ((ReforgeScreenHandler) player.containerMenu).getPos() : null);
            if (pos != null) {
                context.queue(() -> {
                    if (reforgingScreen) {
                        player.openMenu(new SimpleMenuProvider((syncId, playerInventory, playerx) -> {
                            return new ReforgeScreenHandler(syncId, playerInventory, ContainerLevelAccess.create(playerx.level(), pos));
                        }, Component.translatable("container.reforge")));
                    } else {
                        player.openMenu(new SimpleMenuProvider((syncId, playerInventory, playerx) -> {
                            return new AnvilMenu(syncId, playerInventory, ContainerLevelAccess.create(playerx.level(), pos));
                        }, Component.translatable("container.repair")));
                    }
                    NetworkManager.sendToPlayer(player, new MousePositionPacket(mouseX, mouseY));
                });
            }
        });
        NetworkManager.registerReceiver(NetworkManager.c2s(), ReforgePacket.PACKET_ID, ReforgePacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            context.queue(() -> {
                if (player.containerMenu instanceof ReforgeScreenHandler reforgeScreenHandler) {
                    reforgeScreenHandler.reforge();
                }
            });
        });
        NetworkManager.registerReceiver(NetworkManager.c2s(), ExtractSlotPacket.PACKET_ID, ExtractSlotPacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            context.queue(() -> {
                if (player.containerMenu instanceof ReforgeScreenHandler reforgeScreenHandler) {
                    reforgeScreenHandler.performExtract(payload.slotIndex());
                }
            });
        });
        NetworkManager.registerReceiver(NetworkManager.c2s(), AutoRefillPacket.PACKET_ID, AutoRefillPacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            context.queue(() -> {
                if (player.containerMenu instanceof ReforgeScreenHandler reforgeScreenHandler) {
                    reforgeScreenHandler.setAutoRefill(payload.enabled());
                }
            });
        });
    }

    public static void writeS2CHealthPacket(ServerPlayer serverPlayerEntity) {
        NetworkManager.sendToPlayer(serverPlayerEntity, new HealthPacket(serverPlayerEntity.getHealth()));
    }

    public static void writeS2CReforgeReadyPacket(ServerPlayer serverPlayerEntity, boolean disableButton) {
        NetworkManager.sendToPlayer(serverPlayerEntity, new ReforgeReadyPacket(disableButton));
    }

    public static void writeS2CReforgeItemSyncPacket(ServerPlayer serverPlayerEntity) {
        List<ResourceLocation> ids = new ArrayList<ResourceLocation>();
        List<Integer> listSize = new ArrayList<Integer>();
        List<Integer> itemIds = new ArrayList<Integer>();

        Tiered.REFORGE_DATA_LOADER.getReforgeIdentifiers().forEach(id -> {
            ids.add(id);

            List<Integer> list = new ArrayList<Integer>();
            Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(BuiltInRegistries.ITEM.get(id)).forEach(item -> {
                list.add(BuiltInRegistries.ITEM.getId(item));
            });
            listSize.add(list.size());

            list.forEach(rawId -> {
                itemIds.add(rawId);
            });
        });

        NetworkManager.sendToPlayer(serverPlayerEntity, new ReforgeItemSyncPacket(ids, listSize, itemIds));
    }

    public static void writeS2CReforgeMaterialSyncPacket(ServerPlayer serverPlayerEntity) {
        List<String> itemIds = new ArrayList<String>();
        List<String> materialJsons = new ArrayList<String>();

        Tiered.REFORGE_MATERIAL_LOADER.getMaterials().forEach((id, material) -> {
            itemIds.add(id.toString());
            materialJsons.add(ReforgeMaterialLoader.GSON.toJson(material));
        });

        NetworkManager.sendToPlayer(serverPlayerEntity, new ReforgeMaterialSyncPacket(itemIds, materialJsons));
    }

    public static void writeS2CImprintDataSyncPacket(ServerPlayer serverPlayerEntity) {
        List<String> imprintIds = new ArrayList<>();
        List<String> imprintJsons = new ArrayList<>();
        Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().forEach((id, def) -> {
            imprintIds.add(id);
            imprintJsons.add(ImprintDefinitionLoader.GSON.toJson(def));
        });

        List<String> effectIds = new ArrayList<>();
        List<String> effectJsons = new ArrayList<>();
        Tiered.EFFECT_DEFINITION_LOADER.getDefinitions().forEach((id, def) -> {
            effectIds.add(id);
            effectJsons.add(EffectDefinitionLoader.GSON.toJson(def));
        });

        List<String> slotRuleJsons = new ArrayList<>();
        Tiered.IMPRINT_SLOT_LOADER.getRules().forEach(rule ->
                slotRuleJsons.add(ImprintSlotLoader.GSON.toJson(rule)));

        NetworkManager.sendToPlayer(serverPlayerEntity, new ImprintDataSyncPacket(
                imprintIds, imprintJsons, effectIds, effectJsons, slotRuleJsons));
    }

    public static void writeS2CAttributePacket(ServerPlayer serverPlayerEntity) {
        List<String> attributeIds = new ArrayList<String>();
        List<String> attributeJsons = new ArrayList<String>();

        Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            attributeIds.add(id.toString());
            attributeJsons.add(AttributeDataLoader.GSON.toJson(attribute));
        });

        NetworkManager.sendToPlayer(serverPlayerEntity, new AttributePacket(attributeIds, attributeJsons));
    }

    public static void writeS2CStopAutoReforgePacket(ServerPlayer serverPlayerEntity) {
        NetworkManager.sendToPlayer(serverPlayerEntity, new StopAutoReforgePacket());
    }

}
