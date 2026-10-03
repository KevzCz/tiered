package draylar.tiered.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeDataLoader extends SimpleJsonResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(AttributeModifier.class, new EntityAttributeModifierDeserializer())
            .registerTypeAdapter(AttributeModifier.class, new EntityAttributeModifierSerializer()).registerTypeAdapter(EquipmentSlot.class, new EquipmentSlotSerializer())
            .registerTypeAdapter(EquipmentSlot.class, new EquipmentSlotDeserializer()).registerTypeAdapter(Style.class, new StyleDeserializer()).registerTypeAdapter(Style.class, new StyleSerializer()).create();

    private static final String PARSING_ERROR_MESSAGE = "Parsing error loading tier {}";
    private static final String LOADED_TIERS_MESSAGE = "Loaded {} tiers";
    private static final String SKIPPED_NON_TIER_MESSAGE = "Skipped {} non-tier files in item_attributes (missing \"id\"), likely from another mod using the same folder";
    private static final Logger LOGGER = LogManager.getLogger();

    private volatile Map<ResourceLocation, PotentialAttribute> itemAttributes = Map.of();

    public AttributeDataLoader() {
        super(GSON, "item_attributes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, PotentialAttribute> readItemAttributes = Maps.newHashMap();
        int skippedNonTier = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            ResourceLocation identifier = entry.getKey();
            try {
                PotentialAttribute itemAttribute = GSON.fromJson(entry.getValue(), PotentialAttribute.class);
                if (itemAttribute == null || itemAttribute.getID() == null) {
                    skippedNonTier++;
                    continue;
                }
                readItemAttributes.put(ResourceLocation.parse(itemAttribute.getID()), itemAttribute);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error(PARSING_ERROR_MESSAGE, identifier, exception);
            }
        }

        itemAttributes = Collections.unmodifiableMap(readItemAttributes);
        LOGGER.info(LOADED_TIERS_MESSAGE, readItemAttributes.size());
        if (skippedNonTier > 0) {
            LOGGER.warn(SKIPPED_NON_TIER_MESSAGE, skippedNonTier);
        }
    }

    public Map<ResourceLocation, PotentialAttribute> getItemAttributes() {
        return itemAttributes;
    }

    public void setItemAttributes(Map<ResourceLocation, PotentialAttribute> attributes) {
        itemAttributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

}
