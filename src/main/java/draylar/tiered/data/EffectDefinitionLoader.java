package draylar.tiered.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import draylar.tiered.api.effect.DataEffect;
import draylar.tiered.api.effect.EffectDefinition;
import draylar.tiered.api.effect.ReforgeEffect;
import draylar.tiered.api.effect.ReforgeEffectRegistry;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EffectDefinitionLoader extends JsonDataLoader implements SimpleSynchronousResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, DataEffect> effects = new HashMap<>();
    private Map<String, EffectDefinition> definitions = new HashMap<>();

    public EffectDefinitionLoader() {
        super(GSON, "effect");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> loader, ResourceManager manager, Profiler profiler) {
        Map<String, DataEffect> readEffects = Maps.newHashMap();
        Map<String, EffectDefinition> readDefs = Maps.newHashMap();

        for (Map.Entry<Identifier, JsonElement> entry : loader.entrySet()) {
            Identifier fileId = entry.getKey();
            try {
                EffectDefinition def = GSON.fromJson(entry.getValue(), EffectDefinition.class);
                if (def == null) continue;
                String id = def.getId() != null ? def.getId() : fileId.toString();
                readEffects.put(id, new DataEffect(id, def));
                readDefs.put(id, def);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading effect definition {}", fileId, exception);
            }
        }

        effects = readEffects;
        definitions = readDefs;
        LOGGER.info("Loaded {} data-defined effects", effects.size());
    }

    @Nullable
    public ReforgeEffect get(String id) {
        return effects.get(id);
    }

    public Map<String, EffectDefinition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, EffectDefinition> incoming) {
        Map<String, DataEffect> rebuilt = new HashMap<>();
        for (Map.Entry<String, EffectDefinition> e : incoming.entrySet()) {
            rebuilt.put(e.getKey(), new DataEffect(e.getKey(), e.getValue()));
        }
        effects = rebuilt;
        definitions = new HashMap<>(incoming);
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "effect");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
    }
}
