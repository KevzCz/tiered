package draylar.tiered.datagen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import draylar.tiered.api.effect.EffectDefinition;
import draylar.tiered.api.imprint.EligibilityPredicate;
import draylar.tiered.api.imprint.ImprintDefinition;

public final class ImprintEffectDefGenerator {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ImprintDefinition.class, new DefaultSkippingSerializer<>(ImprintDefinition.class))
            .registerTypeAdapter(ImprintDefinition.TypeComponent.class, new DefaultSkippingSerializer<>(ImprintDefinition.TypeComponent.class))
            .registerTypeAdapter(EffectDefinition.class, new DefaultSkippingSerializer<>(EffectDefinition.class))
            .create();

    private ImprintEffectDefGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of("generated", "data", "tiered");
        Path imprintDir = root.resolve("imprint");
        Path effectDir = root.resolve("effect");
        Files.createDirectories(imprintDir);
        Files.createDirectories(effectDir);

        write(imprintDir, "vigorous", new ImprintDefinition()
                .id("tiered:vigorous").color("RED")
                .nameKey("imprint.tiered.vigorous.name").lineKey("imprint.tiered.vigorous")
                .combine("ADDITIVE").multiplicity("SLOT").group("vitality")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("attribute").attribute("minecraft:generic.max_health")
                        .operation("ADD_VALUE").anyWorn(true)
                        .valueRange(1.0f, 3.0f).maxBonus(30.0f))));

        write(imprintDir, "velocity", new ImprintDefinition()
                .id("tiered:velocity").color("YELLOW")
                .nameKey("imprint.tiered.velocity.name").lineKey("imprint.tiered.velocity")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("movement")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:sprint_melee")
                        .valueDisplay("percent").valueRange(0.01f, 0.05f).maxBonus(0.50f))));

        write(imprintDir, "airborne", new ImprintDefinition()
                .id("tiered:airborne").color("AQUA")
                .nameKey("imprint.tiered.airborne.name").lineKey("imprint.tiered.airborne")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("movement")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:airborne_melee")
                        .valueDisplay("percent").valueRange(0.01f, 0.05f).maxBonus(0.50f))));

        write(imprintDir, "reinforced", new ImprintDefinition()
                .id("tiered:reinforced").color("GOLD")
                .nameKey("imprint.tiered.reinforced.name").lineKey("imprint.tiered.reinforced")
                .multiplicity("SLOT")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:reinforce")
                        .valueDisplay("percent").valueRange(0.10f, 0.25f))));

        write(imprintDir, "second_wind", new ImprintDefinition()
                .id("tiered:second_wind").color("LIGHT_PURPLE")
                .nameKey("imprint.tiered.second_wind.name").lineKey("imprint.tiered.second_wind")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("survival")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:second_wind").valueDisplay("percent")
                        .valueRange(0.05f, 0.10f).maxBonus(0.65f)
                        .params(Map.of("health_threshold", 0.35f, "cooldown_ticks", 300f,
                                "heal_ticks", 40f)))));

        write(imprintDir, "honed", new ImprintDefinition()
                .id("tiered:honed").color("RED")
                .nameKey("imprint.tiered.honed.name").lineKey("imprint.tiered.honed")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("weapon")
                .activeWhen(EligibilityPredicate.withEquippedItem(
                        EligibilityPredicate.EquippedItemClause.ofTags("mainhand", List.of("#minecraft:swords"))))
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:universal_damage")
                        .valueDisplay("percent").valueRange(0.01f, 0.05f).maxBonus(0.50f))));

        write(imprintDir, "rending", new ImprintDefinition()
                .id("tiered:rending").color("GOLD")
                .nameKey("imprint.tiered.rending.name").lineKey("imprint.tiered.rending")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("weapon")
                .activeWhen(EligibilityPredicate.withEquippedItem(
                        EligibilityPredicate.EquippedItemClause.ofTags("mainhand", List.of("#minecraft:axes"))))
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:universal_damage")
                        .valueDisplay("percent").valueRange(0.01f, 0.05f).maxBonus(0.50f))));

        write(imprintDir, "crushing", new ImprintDefinition()
                .id("tiered:crushing").color("#A07040")
                .nameKey("imprint.tiered.crushing.name").lineKey("imprint.tiered.crushing")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("weapon")
                .activeWhen(EligibilityPredicate.withEquippedItem(
                        EligibilityPredicate.EquippedItemClause.ofItems("mainhand", List.of("minecraft:mace"))))
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:universal_damage")
                        .valueDisplay("percent").valueRange(0.01f, 0.05f).maxBonus(0.50f))));

        write(imprintDir, "piercing", new ImprintDefinition()
                .id("tiered:piercing").color("#5B9BD5")
                .nameKey("imprint.tiered.piercing.name").lineKey("imprint.tiered.piercing")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("weapon")
                .activeWhen(EligibilityPredicate.withEquippedItem(
                        EligibilityPredicate.EquippedItemClause.ofItems("mainhand", List.of("minecraft:trident"))))
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:universal_damage")
                        .valueDisplay("percent").valueRange(0.01f, 0.05f).maxBonus(0.50f))));

        write(imprintDir, "retaliation", new ImprintDefinition()
                .id("tiered:retaliation").color("#C0392B")
                .nameKey("imprint.tiered.retaliation.name").lineKey("imprint.tiered.retaliation")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:retaliation")
                        .valueDisplay("percent").valueRange(0.05f, 0.15f).maxBonus(0.35f)
                        .params(Map.of("duration_ticks", 100f)))));

        write(imprintDir, "thorns_aura", new ImprintDefinition()
                .id("tiered:thorns_aura").color("#27AE60")
                .nameKey("imprint.tiered.thorns_aura.name").lineKey("imprint.tiered.thorns_aura")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("defense")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:thorns_aura")
                        .valueDisplay("percent").valueRange(0.05f, 0.15f).maxBonus(0.45f)
                        .params(Map.of("min_damage", 0.5f)))));

        write(imprintDir, "bloodthirst", new ImprintDefinition()
                .id("tiered:bloodthirst").color("#922B21")
                .nameKey("imprint.tiered.bloodthirst.name").lineKey("imprint.tiered.bloodthirst")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:bloodthirst")
                        .valueDisplay("percent").valueRange(0.01f, 0.1f).maxBonus(0.35f)
                        .params(Map.of("heal_fraction", 0.1f)))));

        write(imprintDir, "wither_strike", new ImprintDefinition()
                .id("tiered:wither_strike").color("#1C1C1C")
                .nameKey("imprint.tiered.wither_strike.name").lineKey("imprint.tiered.wither_strike")
                .combine("UNIQUE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:wither_strike").valueDisplay("none")
                        .params(Map.of("duration_ticks", 60f, "amplifier", 1f, "chance", 1.0f)))));

        write(imprintDir, "soul_harvest", new ImprintDefinition()
                .id("tiered:soul_harvest").color("#8E44AD")
                .nameKey("imprint.tiered.soul_harvest.name").lineKey("imprint.tiered.soul_harvest")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:soul_harvest").valueDisplay("none")
                        .params(Map.of("duration_ticks", 100f, "amplifier", 0f, "max_duration_ticks", 400f)))));

        write(imprintDir, "executioner", new ImprintDefinition()
                .id("tiered:executioner").color("#E74C3C")
                .nameKey("imprint.tiered.executioner.name").lineKey("imprint.tiered.executioner")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:executioner")
                        .valueDisplay("raw").valueRange(0.5f, 1.0f).maxBonus(3.0f)
                        .params(Map.of("heal_amount", 1.0f)))));

        write(imprintDir, "fortified", new ImprintDefinition()
                .id("tiered:fortified").color("#5D6D7E")
                .nameKey("imprint.tiered.fortified.name").lineKey("imprint.tiered.fortified")
                .combine("ADDITIVE").multiplicity("SLOT").group("defense")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("attribute").attribute("minecraft:generic.armor")
                        .operation("ADD_VALUE").anyWorn(true)
                        .valueRange(1.0f, 3.0f).maxBonus(10.0f))));

        write(imprintDir, "swiftness", new ImprintDefinition()
                .id("tiered:swiftness").color("#1ABC9C")
                .nameKey("imprint.tiered.swiftness.name").lineKey("imprint.tiered.swiftness")
                .combine("ADDITIVE").multiplicity("SLOT").group("movement")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("attribute").attribute("minecraft:generic.movement_speed")
                        .operation("MULTIPLY_BASE").anyWorn(true).valueDisplay("percent")
                        .valueRange(0.03f, 0.06f).maxBonus(0.15f))));

        write(imprintDir, "last_stand", new ImprintDefinition()
                .id("tiered:last_stand").color("#E67E22")
                .nameKey("imprint.tiered.last_stand.name").lineKey("imprint.tiered.last_stand")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:last_stand")
                        .valueDisplay("percent").valueRange(0.075f, 0.15f).maxBonus(0.65f)
                        .params(Map.of("health_threshold", 0.35f)))));

        write(imprintDir, "momentum", new ImprintDefinition()
                .id("tiered:momentum").color("#F39C12")
                .nameKey("imprint.tiered.momentum.name").lineKey("imprint.tiered.momentum")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:momentum")
                        .valueDisplay("percent").valueRange(0.05f, 0.1f).maxBonus(0.25f)
                        .params(Map.of("max_stacks", 5f, "reset_ticks", 60f)))));

        write(imprintDir, "flanking", new ImprintDefinition()
                .id("tiered:flanking").color("#9B59B6")
                .nameKey("imprint.tiered.flanking.name").lineKey("imprint.tiered.flanking")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("weapon")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:flanking")
                        .valueDisplay("percent").valueRange(0.03f, 0.08f).maxBonus(0.30f)
                        .params(Map.of("bonus_angle_threshold", 120f, "penalty_angle_threshold", 60f))
                        .extraRanges(List.of(new ImprintDefinition.ExtraRange()
                                .key("penalty").valueRange(-0.02f, -0.05f).maxBonus(-0.15f).valueDisplay("percent"))))));

        write(imprintDir, "condemned", new ImprintDefinition()
                .id("tiered:condemned").color("#7D3C98")
                .nameKey("imprint.tiered.condemned.name").lineKey("imprint.tiered.condemned")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:condemned")
                        .valueDisplay("percent").valueRange(0.03f, 0.05f).maxBonus(0.20f)
                        .params(Map.of("threshold_count", 4f, "threshold_multiplier", 1.2f)))));

        write(imprintDir, "tormented", new ImprintDefinition()
                .id("tiered:tormented").color("#5B2C6F")
                .nameKey("imprint.tiered.tormented.name").lineKey("imprint.tiered.tormented")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:tormented")
                        .valueDisplay("percent").valueRange(0.005f, 0.015f).maxBonus(0.075f))));

        write(imprintDir, "affliction", new ImprintDefinition()
                .id("tiered:affliction").color("#48C9B0")
                .nameKey("imprint.tiered.affliction.name").lineKey("imprint.tiered.affliction")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("combat")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:affliction")
                        .valueDisplay("percent").valueRange(0.075f, 0.15f).maxBonus(1.00f)
                        .params(Map.of("effect_duration_ticks", 100f))
                        .extraRanges(List.of(new ImprintDefinition.ExtraRange()
                                .key("duration").valueRange(10f, 20f).maxBonus(40f).valueDisplay("seconds"))))));

        write(imprintDir, "ravenous", new ImprintDefinition()
                .id("tiered:ravenous").color("#CA6F1E")
                .nameKey("imprint.tiered.ravenous.name").lineKey("imprint.tiered.ravenous")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("vitality")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:ravenous")
                        .valueDisplay("percent").valueRange(0.025f, 0.07f).maxBonus(0.35f)
                        .params(Map.of("saturation_max_multiplier", 1.5f)))));

        write(imprintDir, "tempered", new ImprintDefinition()
                .id("tiered:tempered").color("#85929E")
                .nameKey("imprint.tiered.tempered.name").lineKey("imprint.tiered.tempered")
                .combine("ADDITIVE").multiplicity("SLOT").reapply("HIGHEST").group("defense")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:tempered")
                        .valueDisplay("percent").valueRange(0.02f, 0.05f).maxBonus(0.50f))));

        write(imprintDir, "stalwart", new ImprintDefinition()
                .id("tiered:stalwart").color("#5D6D7E")
                .nameKey("imprint.tiered.stalwart.name").lineKey("imprint.tiered.stalwart")
                .combine("ADDITIVE").multiplicity("SLOT").group("defense")
                .types(List.of(new ImprintDefinition.TypeComponent()
                        .type("behavioral").behavior("tiered:stalwart")
                        .valueDisplay("percent").valueRange(0.01f, 0.02f).maxBonus(0.10f))));

        write(effectDir, "mend", new EffectDefinition().id("tiered:mend").type("repair").valueRange(0.1f, 0.25f)
                .description("effect.tiered.mend.desc"));
        write(effectDir, "extract", new EffectDefinition().id("tiered:extract").type("extract").value(0.75f)
                .description("effect.tiered.extract.desc"));
        write(effectDir, "forget", new EffectDefinition().id("tiered:forget").type("forget")
                .description("effect.tiered.forget.desc"));
        write(effectDir, "stabilize", new EffectDefinition().id("tiered:stabilize").type("stabilize")
                .description("effect.tiered.stabilize.desc"));
        write(effectDir, "overcharge", new EffectDefinition().id("tiered:overcharge").type("overcharge")
                .valueRange(0.1f, 0.25f)
                .params(Map.of("extra_cost_min", 1f, "extra_cost_max", 2f,
                        "boost_per_cost", 0.35f, "min_rarity_order", 2f))
                .description("effect.tiered.overcharge.desc"));
        write(effectDir, "grant_rune_slot", new EffectDefinition().id("tiered:grant_rune_slot").type("grant_rune_slot")
                .value(1f)
                .params(Map.of("max_total", 1f))
                .appliesTo(List.of("#minecraft:enchantable/weapon", "#minecraft:enchantable/armor"))
                .description("effect.tiered.grant_rune_slot.desc"));

        System.out.println("[Tiered] Generated imprint/effect definition templates under " + root.toAbsolutePath());
    }

    private static void write(Path dir, String name, Object def) throws IOException {
        Files.writeString(dir.resolve(name + ".json"), GSON.toJson(def) + "\n", StandardCharsets.UTF_8);
    }
}
