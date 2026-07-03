package draylar.tiered.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import draylar.tiered.Tiered;
import draylar.tiered.TieredClient;
import draylar.tiered.api.ImprintSlotRule;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.effect.EffectDefinition;
import draylar.tiered.api.imprint.ImprintDefinition;
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
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.data.ReforgeMaterialLoader;
import draylar.tiered.network.packet.ReforgeItemSyncPacket;
import draylar.tiered.network.packet.ReforgeMaterialSyncPacket;
import draylar.tiered.network.packet.ReforgePacket;
import draylar.tiered.network.packet.ReforgeReadyPacket;
import draylar.tiered.network.packet.ReforgeScreenPacket;
import draylar.tiered.network.packet.StopAutoReforgePacket;
import draylar.tiered.lib.TabRegistry;
import draylar.tiered.reforge.ReforgeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class TieredClientPacket {

    @SuppressWarnings("resource")
    public static void init() {

        ClientPlayNetworking.registerGlobalReceiver(ReforgeReadyPacket.PACKET_ID, (payload, context) -> {
            boolean disableButton = payload.disableButton();
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof ReforgeScreen reforgeScreen) {
                    reforgeScreen.reforgeButton.setDisabled(disableButton);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(HealthPacket.PACKET_ID, (payload, context) -> {
            float health = payload.health();
            context.client().execute(() -> {
                context.player().setHealth(health);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ReforgeItemSyncPacket.PACKET_ID, (payload, context) -> {
            List<Identifier> identifiers = payload.ids();
            List<Integer> listSize = payload.listSize();
            List<Integer> itemIds = payload.itemIds();

            context.client().execute(() -> {
                Tiered.REFORGE_DATA_LOADER.clearReforgeBaseItems();

                int count = 0;
                for (int i = 0; i < identifiers.size(); i++) {
                    List<Item> items = new ArrayList<Item>();

                    for (int u = count; u < (count + listSize.get(i)); u++) {
                        items.add(Registries.ITEM.get(itemIds.get(u)));
                    }
                    count += listSize.get(i);
                    Tiered.REFORGE_DATA_LOADER.putReforgeBaseItems(identifiers.get(i), items);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(AttributePacket.PACKET_ID, (payload, context) -> {
            TieredClient.CACHED_ATTRIBUTES.putAll(Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes());
            Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().clear();

            for (int i = 0; i < payload.attributeIds().size(); i++) {
                Identifier id = Identifier.of(payload.attributeIds().get(i));
                PotentialAttribute pa = AttributeDataLoader.GSON.fromJson(payload.attributeJsons().get(i), PotentialAttribute.class);
                Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().put(id, pa);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(ReforgeMaterialSyncPacket.PACKET_ID, (payload, context) -> {
            Map<Identifier, ReforgeMaterial> incoming = new HashMap<>();
            for (int i = 0; i < payload.itemIds().size(); i++) {
                Identifier id = Identifier.of(payload.itemIds().get(i));
                ReforgeMaterial material = ReforgeMaterialLoader.GSON.fromJson(payload.materialJsons().get(i), ReforgeMaterial.class);
                incoming.put(id, material);
            }
            context.client().execute(() -> Tiered.REFORGE_MATERIAL_LOADER.setMaterials(incoming));
        });
        ClientPlayNetworking.registerGlobalReceiver(ImprintDataSyncPacket.PACKET_ID, (payload, context) -> {
            Map<String, ImprintDefinition> imprints = new HashMap<>();
            for (int i = 0; i < payload.imprintIds().size(); i++) {
                imprints.put(payload.imprintIds().get(i),
                        ImprintDefinitionLoader.GSON.fromJson(payload.imprintJsons().get(i),
                                ImprintDefinition.class));
            }
            Map<String, EffectDefinition> effects = new HashMap<>();
            for (int i = 0; i < payload.effectIds().size(); i++) {
                effects.put(payload.effectIds().get(i),
                        EffectDefinitionLoader.GSON.fromJson(payload.effectJsons().get(i),
                                EffectDefinition.class));
            }
            List<ImprintSlotRule> slotRules = new ArrayList<>();
            for (String json : payload.slotRuleJsons()) {
                slotRules.add(ImprintSlotLoader.GSON.fromJson(json, ImprintSlotRule.class));
            }
            context.client().execute(() -> {
                Tiered.IMPRINT_DEFINITION_LOADER.setDefinitions(imprints);
                Tiered.EFFECT_DEFINITION_LOADER.setDefinitions(effects);
                Tiered.IMPRINT_SLOT_LOADER.setRules(slotRules);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(StopAutoReforgePacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof ReforgeScreen reforgeScreen) {
                    reforgeScreen.stopAutoReforgeFromServer();
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(MousePositionPacket.PACKET_ID, (payload, context) -> {
            context.client().execute(() -> {
                TabRegistry.setMousePosition(payload.mouseX(), payload.mouseY());
            });
        });
    }

    public static void writeC2SScreenPacket(int mouseX, int mouseY, boolean reforgingScreen) {
        ClientPlayNetworking.send(new ReforgeScreenPacket(mouseX, mouseY, reforgingScreen));
    }

    public static void writeC2SReforgePacket() {
        ClientPlayNetworking.send(new ReforgePacket());
    }

    public static void writeC2SExtractSlotPacket(int index) {
        ClientPlayNetworking.send(new ExtractSlotPacket(index));
    }

    public static void writeC2SAutoRefillPacket(boolean enabled) {
        ClientPlayNetworking.send(new AutoRefillPacket(enabled));
    }

}
