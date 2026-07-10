package draylar.tiered.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import draylar.tiered.api.ImprintSlotRule;
import draylar.tiered.api.ItemVerifier;
import draylar.tiered.util.ImprintSlots;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

public class ImprintSlotLoader extends SimpleJsonResourceReloadListener {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Logger LOGGER = LogManager.getLogger();

    private List<ImprintSlotRule> rules = new ArrayList<>();

    public ImprintSlotLoader() {
        super(GSON, "imprint_slots");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ImprintSlotRule> read = Maps.newHashMap();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                ImprintSlotRule rule = GSON.fromJson(entry.getValue(), ImprintSlotRule.class);
                if (rule == null || (rule.getTarget() == null && rule.getTargets() == null)) {
                    LOGGER.error("Imprint slot rule {} is missing a 'target' or 'targets' field", fileId);
                    continue;
                }
                read.put(fileId, rule);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error("Parsing error loading imprint slot rule {}", fileId, exception);
            }
        }

        rules = new ArrayList<>(read.values());
        LOGGER.info("Loaded {} imprint slot rules", rules.size());
    }

    @Nullable
    public ImprintSlotRule bestRule(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        int bestSpecificity = -1;
        ImprintSlotRule best = null;
        for (ImprintSlotRule rule : rules) {
            if (!matches(rule, item, itemId)) continue;
            int spec = rule.specificity();
            if (spec > bestSpecificity) {
                bestSpecificity = spec;
                best = rule;
            }
        }
        return best;
    }

    public int datapackSlots(Item item) {
        ImprintSlotRule best = bestRule(item);
        return best == null ? 0 : best.getSlots();
    }

    public int datapackMaxSlots(Item item) {
        ImprintSlotRule best = bestRule(item);
        return best == null ? -1 : best.getMax();
    }

    private boolean matches(ImprintSlotRule rule, Item item, ResourceLocation itemId) {
        if (rule.getTargets() != null) {
            for (ItemVerifier verifier : rule.getTargets()) {
                if (verifier.isValid(itemId)) return true;
            }
            return false;
        }

        String target = rule.getTarget();
        if (target == null) return false;
        if (target.equals("*")) return true;
        if (target.endsWith(":*")) {
            String namespace = target.substring(0, target.length() - 2);
            return itemId.getNamespace().equals(namespace);
        }
        if (target.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(target.substring(1)));
            return item.builtInRegistryHolder().is(tagKey);
        }
        return itemId.equals(ResourceLocation.tryParse(target));
    }

    public List<ImprintSlotRule> getRules() {
        return rules;
    }

    public void setRules(List<ImprintSlotRule> incoming) {
        rules = new ArrayList<>(incoming);
    }

}
