package draylar.tiered.data;

import java.util.HashSet;
import java.util.Set;

import draylar.tiered.Tiered;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.api.effect.EffectDefinition;
import draylar.tiered.api.imprint.ImprintDefinition;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.behavior.ImprintBehaviorRegistry;
import draylar.tiered.api.imprint.condition.ScaleConditionRegistry;
import draylar.tiered.api.effect.ReforgeEffectRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DataValidator {

    private static final Logger LOGGER = LogManager.getLogger("Tiered/Validation");

    private DataValidator() {
    }

    public static void validate() {
        int warnings = 0;
        warnings += validateImprints();
        warnings += validateMaterials();
        if (warnings > 0) {
            LOGGER.warn("Tiered data validation found {} issue(s); the entries above will be skipped at runtime.", warnings);
        }
    }

    private static int validateImprints() {
        int warnings = 0;
        for (var entry : Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().entrySet()) {
            ImprintDefinition def = entry.getValue();
            for (ImprintDefinition.TypeComponent c : def.behavioralComponents()) {
                String behavior = c.getBehavior();
                if (behavior != null && !ImprintBehaviorRegistry.contains(behavior)) {
                    LOGGER.warn("Imprint '{}' references unknown behavior '{}'.", entry.getKey(), behavior);
                    warnings++;
                }
                for (ImprintDefinition.ScaleEntry s : c.getScaleWhen()) {
                    if (s.getCondition() != null && !ScaleConditionRegistry.contains(s.getCondition())) {
                        LOGGER.warn("Imprint '{}' references unknown scale condition '{}'.", entry.getKey(), s.getCondition());
                        warnings++;
                    }
                }
            }
        }
        return warnings;
    }

    private static int validateMaterials() {
        int warnings = 0;
        Set<String> allGroups = collectImprintGroups();

        for (ReforgeMaterial material : Tiered.REFORGE_MATERIAL_LOADER.getMaterials().values()) {

            if (material.getEffects() != null) {
                for (String effectId : material.getEffects()) {
                    if (ReforgeEffectRegistry.get(effectId) == null) {
                        LOGGER.warn("Reforge material '{}' references unknown effect '{}'.", material.getItem(), effectId);
                        warnings++;
                    }
                }
            }

            for (ReforgeMaterial.ImprintPool pool : material.getImprintPools()) {
                warnings += validatePool(material.getItem(), pool, allGroups);
            }
            ReforgeMaterial.ImprintPoolChoice choice = material.getImprintPoolChoice();
            if (choice != null) {
                for (ReforgeMaterial.ImprintPoolChoice.Candidate cand : choice.getCandidates()) {
                    if (cand.getPool() != null) warnings += validatePool(material.getItem(), cand.getPool(), allGroups);
                }
            }
        }
        return warnings;
    }

    private static int validatePool(String materialId, ReforgeMaterial.ImprintPool pool, Set<String> allGroups) {
        int warnings = 0;
        for (String id : pool.getDefaultImprints()) {
            if (ImprintRegistry.get(id) == null) {
                LOGGER.warn("Reforge material '{}' default_imprint '{}' is unknown.", materialId, id);
                warnings++;
            }
        }
        for (ReforgeMaterial.Candidate c : pool.getCandidates()) {
            if (ImprintRegistry.get(c.getImprint()) == null) {
                LOGGER.warn("Reforge material '{}' candidate imprint '{}' is unknown.", materialId, c.getImprint());
                warnings++;
            }
        }
        for (String group : pool.getImprintGroups()) {
            if (!"all".equalsIgnoreCase(group) && !allGroups.contains(group)) {
                LOGGER.warn("Reforge material '{}' imprint_group '{}' matches no imprints.", materialId, group);
                warnings++;
            }
        }
        return warnings;
    }

    private static Set<String> collectImprintGroups() {
        Set<String> groups = new HashSet<>();
        for (ImprintDefinition def : Tiered.IMPRINT_DEFINITION_LOADER.getDefinitions().values()) {
            groups.addAll(def.getGroups());
        }
        return groups;
    }

    public static void validateEffects() {
        for (var entry : Tiered.EFFECT_DEFINITION_LOADER.getDefinitions().entrySet()) {
            EffectDefinition def = entry.getValue();
            if ("grant_imprint".equals(def.getType()) && def.getImprint() != null
                    && ImprintRegistry.get(def.getImprint()) == null) {
                LOGGER.warn("Effect '{}' grants unknown imprint '{}'.", entry.getKey(), def.getImprint());
            }
            if ("custom".equals(def.getType()) && def.getEffect() != null
                    && ReforgeEffectRegistry.getBuiltin(def.getEffect()) == null) {
                LOGGER.warn("Effect '{}' delegates to unknown custom effect '{}'.", entry.getKey(), def.getEffect());
            }
        }
    }
}
