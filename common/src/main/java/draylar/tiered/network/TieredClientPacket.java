package draylar.tiered.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.architectury.networking.NetworkManager;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

@Environment(EnvType.CLIENT)
public class TieredClientPacket {

    public static void init() {

        NetworkManager.registerReceiver(NetworkManager.s2c(), ReforgeReadyPacket.PACKET_ID, ReforgeReadyPacket.PACKET_CODEC, (payload, context) -> {
            boolean disableButton = payload.disableButton();
            context.queue(() -> {
                if (Minecraft.getInstance().screen instanceof ReforgeScreen reforgeScreen) {
                    reforgeScreen.reforgeButton.setDisabled(disableButton);
                }
            });
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), HealthPacket.PACKET_ID, HealthPacket.PACKET_CODEC, (payload, context) -> {
            float health = payload.health();
            context.queue(() -> {
                context.getPlayer().setHealth(health);
            });
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), ReforgeItemSyncPacket.PACKET_ID, ReforgeItemSyncPacket.PACKET_CODEC, (payload, context) -> {
            List<ResourceLocation> identifiers = payload.ids();
            List<Integer> listSize = payload.listSize();
            List<Integer> itemIds = payload.itemIds();

            context.queue(() -> {
                Tiered.REFORGE_DATA_LOADER.clearReforgeBaseItems();

                int count = 0;
                for (int i = 0; i < identifiers.size(); i++) {
                    List<Item> items = new ArrayList<Item>();

                    for (int u = count; u < (count + listSize.get(i)); u++) {
                        items.add(BuiltInRegistries.ITEM.byId(itemIds.get(u)));
                    }
                    count += listSize.get(i);
                    Tiered.REFORGE_DATA_LOADER.putReforgeBaseItems(identifiers.get(i), items);
                }
            });
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), AttributePacket.PACKET_ID, AttributePacket.PACKET_CODEC, (payload, context) -> {
            TieredClient.CACHED_ATTRIBUTES.putAll(Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes());
            Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().clear();

            for (int i = 0; i < payload.attributeIds().size(); i++) {
                ResourceLocation id = ResourceLocation.parse(payload.attributeIds().get(i));
                PotentialAttribute pa = AttributeDataLoader.GSON.fromJson(payload.attributeJsons().get(i), PotentialAttribute.class);
                Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().put(id, pa);
            }
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), ReforgeMaterialSyncPacket.PACKET_ID, ReforgeMaterialSyncPacket.PACKET_CODEC, (payload, context) -> {
            Map<ResourceLocation, ReforgeMaterial> incoming = new HashMap<>();
            for (int i = 0; i < payload.itemIds().size(); i++) {
                ResourceLocation id = ResourceLocation.parse(payload.itemIds().get(i));
                ReforgeMaterial material = ReforgeMaterialLoader.GSON.fromJson(payload.materialJsons().get(i), ReforgeMaterial.class);
                incoming.put(id, material);
            }
            context.queue(() -> Tiered.REFORGE_MATERIAL_LOADER.setMaterials(incoming));
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), ImprintDataSyncPacket.PACKET_ID, ImprintDataSyncPacket.PACKET_CODEC, (payload, context) -> {
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
            context.queue(() -> {
                Tiered.IMPRINT_DEFINITION_LOADER.setDefinitions(imprints);
                Tiered.EFFECT_DEFINITION_LOADER.setDefinitions(effects);
                Tiered.IMPRINT_SLOT_LOADER.setRules(slotRules);
            });
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), StopAutoReforgePacket.PACKET_ID, StopAutoReforgePacket.PACKET_CODEC, (payload, context) -> {
            context.queue(() -> {
                if (Minecraft.getInstance().screen instanceof ReforgeScreen reforgeScreen) {
                    reforgeScreen.stopAutoReforgeFromServer();
                }
            });
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), MousePositionPacket.PACKET_ID, MousePositionPacket.PACKET_CODEC, (payload, context) -> {
            context.queue(() -> {
                TabRegistry.setMousePosition(payload.mouseX(), payload.mouseY());
            });
        });
    }

    public static void writeC2SScreenPacket(int mouseX, int mouseY, boolean reforgingScreen) {
        NetworkManager.sendToServer(new ReforgeScreenPacket(mouseX, mouseY, reforgingScreen));
    }

    public static void writeC2SReforgePacket() {
        NetworkManager.sendToServer(new ReforgePacket());
    }

    public static void writeC2SExtractSlotPacket(int index) {
        NetworkManager.sendToServer(new ExtractSlotPacket(index));
    }

    public static void writeC2SAutoRefillPacket(boolean enabled) {
        NetworkManager.sendToServer(new AutoRefillPacket(enabled));
    }

}
