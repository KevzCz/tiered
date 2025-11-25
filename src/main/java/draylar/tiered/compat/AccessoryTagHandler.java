package draylar.tiered.compat;

import io.wispforest.accessories.api.AccessoriesAPI;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class AccessoryTagHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("Tiered/Accessories");
    private static MinecraftServer serverInstance;

    public static final TagKey<Item> ALL_TRINKET_ITEMS = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of("tclayer", "all_trinket_items")
    );

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
            onServerStarted(server);
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(AccessoryTagHandler::onDataPackReload);
    }

    private static void onServerStarted(MinecraftServer server) {
        var accessories = AccessoriesAPI.getAllAccessories();
        LOGGER.info("Registered {} accessory items for tclayer:all_trinket_items virtual tag:", accessories.size());
        accessories.keySet().forEach(item -> {
            LOGGER.info("  - {}", net.minecraft.registry.Registries.ITEM.getId(item));
        });
    }

    private static void onDataPackReload(MinecraftServer server, net.minecraft.resource.LifecycledResourceManager resourceManager, boolean success) {
        if (success) {
            serverInstance = server;
            onServerStarted(server);
        }
    }

    /**
     * Check if an item should be in the tclayer:all_trinket_items tag.
     * This is used by mixins and verifiers.
     */
    public static boolean shouldBeInTag(Item item) {
        return AccessoriesAPI.getAccessory(item) != null;
    }

    /**
     * Check if an ItemStack is in the tclayer:all_trinket_items tag.
     */
    public static boolean isInTag(ItemStack stack) {
        return shouldBeInTag(stack.getItem());
    }

    /**
     * Get all items that are considered accessories.
     */
    public static Set<Item> getAllTrinketItems() {
        return AccessoriesAPI.getAllAccessories().keySet();
    }
}