package draylar.tiered.reforge.codex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import draylar.tiered.Tiered;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.api.effect.EffectDefinition;
import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.EligibilityPredicate;
import draylar.tiered.api.imprint.Imprint;
import draylar.tiered.api.imprint.ImprintDefinition;
import draylar.tiered.api.imprint.ImprintParamDisplay;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.ability.AbilityBinding;
import draylar.tiered.api.imprint.ability.AbilityBinding.Plate;
import org.jetbrains.annotations.Nullable;

public final class CodexUtil {

    private CodexUtil() {
    }

    public static String idTail(String id) {
        if (id == null) return "?";
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }

    public static String num(float v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    public static String capitalize(@Nullable String s) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static List<ImprintDefinition.ExtraRange> extraRanges(ImprintDefinition def) {
        for (ImprintDefinition.TypeComponent c : def.resolvedComponents()) {
            if (!c.getExtraRanges().isEmpty()) return c.getExtraRanges();
        }
        return List.of();
    }

    public static String fmtValue(float v, @Nullable String display) {
        if ("seconds".equals(display)) return num(v / 20f) + "s";
        return "percent".equals(display) ? Math.round(v * 100) + "%" : num(v);
    }

    public static int fillFromColor(String color) {
        if (color != null && color.startsWith("#")) {
            try { return 0xFF000000 | (Integer.parseInt(color.substring(1), 16) & 0xFFFFFF); }
            catch (NumberFormatException ignored) {}
        }
        if (color != null) {
            ChatFormatting f = ChatFormatting.getByName(color.toUpperCase(Locale.ROOT));
            if (f != null && f.getColor() != null) return 0xFF000000 | f.getColor();
        }
        return 0xFF808080;
    }

    public static String translateOr(String key, String fallback) {
        if (key == null) return fallback;
        String s = Component.translatable(key).getString();
        return s.isEmpty() || s.equals(key) ? fallback : s;
    }

    @Nullable
    public static String imprintDescription(ImprintDefinition def) {
        if (def.getLineKey() == null || def.getId() == null) return null;

        var imprint = ImprintRegistry.get(def.getId());
        if (imprint instanceof DataImprint data) {
            return data.describeForCodex();
        }
        return null;
    }

    public static List<CodexEntry.AbilityRecipe> abilityRecipes(ImprintDefinition def) {
        List<CodexEntry.AbilityRecipe> out = new ArrayList<>();
        int selfFill = fillFromColor(def.getColor());
        String selfName = imprintName(def.getId());
        for (var binding : def.getAbilities()) {
            String gate = binding.getMasteredWhen();
            if (gate == null) continue;
            var imp = ImprintRegistry.get(gate);
            int gateFill = imp instanceof DataImprint gd ? fillFromColor(gd.definition().getColor()) : 0xFF808080;
            String gateName = imprintName(gate);

            var plate = binding.getPlate();
            String abilityName = plate != null && plate.getNameKey() != null
                    ? translateOr(plate.getNameKey(), idTail(binding.getAbility()))
                    : idTail(binding.getAbility());
            int abilityFill = plate != null && plate.getColor() != null
                    ? fillFromColor(plate.getColor()) : selfFill;

            String desc = null;
            if (plate != null && plate.getLineKey() != null) {
                String raw = Language.getInstance().getOrDefault(plate.getLineKey());
                desc = ImprintParamDisplay.resolveNamedTokens(raw, binding.getParams(1));
                if (desc == null) desc = raw;
                if (desc.equals(plate.getLineKey())) desc = null;
            }
            out.add(new CodexEntry.AbilityRecipe(gateName, gateFill, selfName, selfFill,
                    abilityName, abilityFill, desc));
        }
        return out;
    }

    private static String imprintName(String imprintId) {
        var imp = ImprintRegistry.get(imprintId);
        if (imp instanceof DataImprint data) {
            return translateOr(data.nameKey(), capitalizeWords(idTail(imprintId).replace('_', ' ')));
        }
        return capitalizeWords(idTail(imprintId).replace('_', ' '));
    }


    private static String capitalizeWords(String s) {
        if (s == null || s.isBlank()) return s == null ? "" : s;
        StringBuilder sb = new StringBuilder();
        for (String p : s.trim().split("\\s+")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(capitalize(p));
        }
        return sb.toString();
    }

    public static List<CodexEntry.WorksWithTag> worksWithTags(ImprintDefinition def) {
        var pred = def.getActiveWhen();
        if (pred == null || !pred.hasItemRequirement()) return List.of();
        List<CodexEntry.WorksWithTag> out = new ArrayList<>();
        for (String t : pred.requirementTags()) {
            boolean isTag = t.startsWith("#");
            String path = isTag ? t.substring(1) : t;
            String label = capitalizeWords(idTail(path).replace('_', ' '));
            List<ItemStack> items = new ArrayList<>();
            if (isTag) {
                ResourceLocation tid = ResourceLocation.tryParse(path);
                if (tid != null) {
                    TagKey<Item> key = TagKey.create(Registries.ITEM, tid);
                    for (var entry : BuiltInRegistries.ITEM.getTagOrEmpty(key)) {
                        ItemStack s = new ItemStack(entry.value());
                        if (!s.isEmpty()) items.add(s);
                    }
                }
            } else {
                ResourceLocation iid = ResourceLocation.tryParse(path);
                if (iid != null) {
                    Item item = BuiltInRegistries.ITEM.get(iid);
                    if (item != Items.AIR) items.add(new ItemStack(item));
                }
            }
            if (!items.isEmpty()) out.add(new CodexEntry.WorksWithTag(label, items.get(0), items));
        }
        return out;
    }

    public static List<ItemStack> materialsForImprint(String imprintId, List<String> imprintGroups) {
        return collectMaterials(m -> materialReferencesImprint(m, imprintId, imprintGroups));
    }

    private static boolean materialReferencesImprint(ReforgeMaterial material, String imprintId, List<String> imprintGroups) {

        if (material.getEffects() != null) {
            for (String effectId : material.getEffects()) {
                EffectDefinition def = Tiered.EFFECT_DEFINITION_LOADER.getDefinitions().get(effectId);
                if (def != null && "grant_imprint".equals(def.getType()) && imprintId.equals(def.getImprint())) {
                    return true;
                }
            }
        }

        for (ReforgeMaterial.ImprintPool pool : material.getImprintPools()) {
            if (imprintPoolReferences(pool, imprintId, imprintGroups)) return true;
        }

        ReforgeMaterial.ImprintPoolChoice choice = material.getImprintPoolChoice();
        if (choice != null) {
            for (ReforgeMaterial.ImprintPoolChoice.Candidate cand : choice.getCandidates()) {
                if (imprintPoolReferences(cand.getPool(), imprintId, imprintGroups)) return true;
            }
        }
        return false;
    }

    private static boolean imprintPoolReferences(ReforgeMaterial.ImprintPool pool, String imprintId, List<String> imprintGroups) {
        if (pool == null) return false;
        if (pool.getDefaultImprints().contains(imprintId)) return true;
        for (ReforgeMaterial.Candidate c : pool.getCandidates()) {
            if (imprintId.equals(c.getImprint())) return true;
        }
        String group = pool.getImprintGroup();
        if (group != null && !group.isBlank()) {
            if ("all".equalsIgnoreCase(group)) return true;
            return imprintGroups.contains(group);
        }
        return false;
    }

    public static List<ItemStack> materialsForEffect(String effectId) {
        return collectMaterials(m -> materialReferencesEffect(m, effectId));
    }

    private static List<ItemStack> collectMaterials(Predicate<ReforgeMaterial> predicate) {
        List<ItemStack> out = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (ReforgeMaterial material : Tiered.REFORGE_MATERIAL_LOADER.getMaterials().values()) {
            if (!predicate.test(material)) continue;
            ResourceLocation itemId = ResourceLocation.tryParse(material.getItem());
            if (itemId == null || !seen.add(itemId)) continue;
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != null && item != Items.AIR) out.add(new ItemStack(item));
        }
        return out;
    }

    private static boolean materialReferencesEffect(ReforgeMaterial material, String effectId) {
        if (material.getEffects() != null && material.getEffects().contains(effectId)) return true;
        for (ReforgeMaterial.EffectPool pool : material.getEffectPools()) {
            if (effectPoolReferences(pool, effectId)) return true;
        }

        ReforgeMaterial.EffectPoolChoice choice = material.getEffectPoolChoice();
        if (choice != null) {
            for (ReforgeMaterial.EffectPoolChoice.Candidate cand : choice.getCandidates()) {
                if (effectPoolReferences(cand.getPool(), effectId)) return true;
            }
        }
        return false;
    }

    private static boolean effectPoolReferences(ReforgeMaterial.EffectPool pool, String effectId) {
        if (pool == null) return false;
        for (ReforgeMaterial.EffectCandidate c : pool.getCandidates()) {
            if (effectId.equals(c.getEffect())) return true;
        }
        return false;
    }
}
