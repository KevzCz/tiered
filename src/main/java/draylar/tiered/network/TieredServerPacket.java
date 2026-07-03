package draylar.tiered.network;

import java.util.ArrayList;
import java.util.List;

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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class TieredServerPacket {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(AttributePacket.PACKET_ID, AttributePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(HealthPacket.PACKET_ID, HealthPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ReforgePacket.PACKET_ID, ReforgePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ReforgeReadyPacket.PACKET_ID, ReforgeReadyPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ReforgeItemSyncPacket.PACKET_ID, ReforgeItemSyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ReforgeScreenPacket.PACKET_ID, ReforgeScreenPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(AutoRefillPacket.PACKET_ID, AutoRefillPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(StopAutoReforgePacket.PACKET_ID, StopAutoReforgePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(MousePositionPacket.PACKET_ID, MousePositionPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ReforgeMaterialSyncPacket.PACKET_ID, ReforgeMaterialSyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ImprintDataSyncPacket.PACKET_ID, ImprintDataSyncPacket.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(ExtractSlotPacket.PACKET_ID, ExtractSlotPacket.PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ReforgeScreenPacket.PACKET_ID, (payload, context) -> {
            int mouseX = payload.mouseX();
            int mouseY = payload.mouseY();
            Boolean reforgingScreen = payload.reforgingScreen();

            BlockPos pos = reforgingScreen ? (context.player().currentScreenHandler instanceof AnvilScreenHandler ? ((AnvilScreenHandlerAccess) context.player().currentScreenHandler).getPos() : null)
                    : (context.player().currentScreenHandler instanceof ReforgeScreenHandler ? ((ReforgeScreenHandler) context.player().currentScreenHandler).getPos() : null);
            if (pos != null) {
                context.server().execute(() -> {
                    if (reforgingScreen) {
                        context.player().openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerx) -> {
                            return new ReforgeScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(playerx.getWorld(), pos));
                        }, Text.translatable("container.reforge")));
                    } else {
                        context.player().openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerx) -> {
                            return new AnvilScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(playerx.getWorld(), pos));
                        }, Text.translatable("container.repair")));
                    }
                    ServerPlayNetworking.send(context.player(), new MousePositionPacket(mouseX, mouseY));
                });
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ReforgePacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ReforgeScreenHandler reforgeScreenHandler) {
                    reforgeScreenHandler.reforge();
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(ExtractSlotPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ReforgeScreenHandler reforgeScreenHandler) {
                    reforgeScreenHandler.performExtract(payload.slotIndex());
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(AutoRefillPacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ReforgeScreenHandler reforgeScreenHandler) {
                    reforgeScreenHandler.setAutoRefill(payload.enabled());
                }
            });
        });
    }

    public static void writeS2CHealthPacket(ServerPlayerEntity serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new HealthPacket(serverPlayerEntity.getHealth()));
    }

    public static void writeS2CReforgeReadyPacket(ServerPlayerEntity serverPlayerEntity, boolean disableButton) {
        ServerPlayNetworking.send(serverPlayerEntity, new ReforgeReadyPacket(disableButton));
    }

    public static void writeS2CReforgeItemSyncPacket(ServerPlayerEntity serverPlayerEntity) {
        List<Identifier> ids = new ArrayList<Identifier>();
        List<Integer> listSize = new ArrayList<Integer>();
        List<Integer> itemIds = new ArrayList<Integer>();

        Tiered.REFORGE_DATA_LOADER.getReforgeIdentifiers().forEach(id -> {
            ids.add(id);

            List<Integer> list = new ArrayList<Integer>();
            Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(Registries.ITEM.get(id)).forEach(item -> {
                list.add(Registries.ITEM.getRawId(item));
            });
            listSize.add(list.size());

            list.forEach(rawId -> {
                itemIds.add(rawId);
            });
        });

        ServerPlayNetworking.send(serverPlayerEntity, new ReforgeItemSyncPacket(ids, listSize, itemIds));
    }

    public static void writeS2CReforgeMaterialSyncPacket(ServerPlayerEntity serverPlayerEntity) {
        List<String> itemIds = new ArrayList<String>();
        List<String> materialJsons = new ArrayList<String>();

        Tiered.REFORGE_MATERIAL_LOADER.getMaterials().forEach((id, material) -> {
            itemIds.add(id.toString());
            materialJsons.add(ReforgeMaterialLoader.GSON.toJson(material));
        });

        ServerPlayNetworking.send(serverPlayerEntity, new ReforgeMaterialSyncPacket(itemIds, materialJsons));
    }

    public static void writeS2CImprintDataSyncPacket(ServerPlayerEntity serverPlayerEntity) {
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

        ServerPlayNetworking.send(serverPlayerEntity, new ImprintDataSyncPacket(
                imprintIds, imprintJsons, effectIds, effectJsons, slotRuleJsons));
    }

    public static void writeS2CAttributePacket(ServerPlayerEntity serverPlayerEntity) {
        List<String> attributeIds = new ArrayList<String>();
        List<String> attributeJsons = new ArrayList<String>();

        Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            attributeIds.add(id.toString());
            attributeJsons.add(AttributeDataLoader.GSON.toJson(attribute));
        });

        ServerPlayNetworking.send(serverPlayerEntity, new AttributePacket(attributeIds, attributeJsons));
    }

    public static void writeS2CStopAutoReforgePacket(ServerPlayerEntity serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new StopAutoReforgePacket());
    }

}
