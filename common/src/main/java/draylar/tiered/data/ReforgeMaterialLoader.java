package draylar.tiered.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import draylar.tiered.api.ReforgeMaterial;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

public class ReforgeMaterialLoader extends SimpleJsonResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LogManager.getLogger();

    private Map<ResourceLocation, ReforgeMaterial> materials = new HashMap<>();

    private Map<String, ReforgeMaterial> tagMaterials = new HashMap<>();
    private boolean tagsExpanded = false;

    public ReforgeMaterialLoader() {
        super(GSON, "reforge_material");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ReforgeMaterial> read = Maps.newHashMap();
        Map<String, ReforgeMaterial> readTags = Maps.newHashMap();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                ReforgeMaterial material = GSON.fromJson(entry.getValue(), ReforgeMaterial.class);
                if (material == null || material.getItem() == null) {
                    LOGGER.error("Reforge material {} is missing an 'item' field", fileId);
                    continue;
                }
                String target = material.getItem();
                if (target.startsWith("#")) {
                    readTags.put(target.substring(1), material);
                } else {
                    read.put(ResourceLocation.parse(target), material);
                }
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading reforge material {}", fileId, exception);
            }
        }

        materials = read;
        tagMaterials = readTags;
        tagsExpanded = false;
        LOGGER.info("Loaded {} reforge materials ({} tag-based)", read.size(), readTags.size());
    }

    private void expandTagsIfNeeded() {
        if (tagsExpanded || tagMaterials.isEmpty()) {
            tagsExpanded = true;
            return;
        }
        for (Map.Entry<String, ReforgeMaterial> entry : tagMaterials.entrySet()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(entry.getKey()));
            BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(list -> {
                for (Holder<Item> itemEntry : list) {
                    materials.putIfAbsent(BuiltInRegistries.ITEM.getKey(itemEntry.value()), entry.getValue());
                }
            });
        }
        tagsExpanded = true;
    }

    public ReforgeMaterial getMaterial(Item item) {
        expandTagsIfNeeded();
        return materials.get(BuiltInRegistries.ITEM.getKey(item));
    }

    public Map<ResourceLocation, ReforgeMaterial> getMaterials() {
        expandTagsIfNeeded();
        return materials;
    }

    public void setMaterials(Map<ResourceLocation, ReforgeMaterial> incoming) {
        materials = new HashMap<>(incoming);
        tagMaterials = new HashMap<>();
        tagsExpanded = true;
    }

}
