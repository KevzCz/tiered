package draylar.tiered.data;

import draylar.tiered.config.ConfigInit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RuneInjectionLoader extends SimpleJsonResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogManager.getLogger();

    private List<RuneInjection> injections = new ArrayList<>();
    private ResourceManager lastManager = null;

    public RuneInjectionLoader() {
        super(GSON, "rune_injection");
    }

    public void loadFrom(ResourceManager manager) {
        if (manager == lastManager) return;
        lastManager = manager;
        Map<ResourceLocation, JsonElement> raw = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, "rune_injection", GSON, raw);
        apply(raw, manager, null);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        if (!ConfigInit.runeItemsEnabled()) {
            injections = new ArrayList<>();
            return;
        }
        Map<ResourceLocation, RuneInjection> read = Maps.newHashMap();
        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            try {
                RuneInjection injection = GSON.fromJson(entry.getValue(), RuneInjection.class);
                if (injection != null && !injection.getPools().isEmpty()) {
                    read.put(entry.getKey(), injection);
                }
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading rune injection {}", entry.getKey(), exception);
            }
        }
        injections = new ArrayList<>(read.values());
        LOGGER.info("Loaded {} rune loot injections", injections.size());
    }

    public List<RuneInjection> getMatching(String tableId) {
        List<RuneInjection> out = new ArrayList<>();
        for (RuneInjection injection : injections) {
            if (injection.matchesTable(tableId)) out.add(injection);
        }
        return out;
    }
}
