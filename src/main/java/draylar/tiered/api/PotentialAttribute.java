package draylar.tiered.api;

import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.util.List;

public class PotentialAttribute {

    private final String id;
    private final List<ItemVerifier> verifiers;
    private final int weight;
    private final Style style;
    private final List<AttributeTemplate> attributes;
    private final List<ItemVerifier> excludes;
    public PotentialAttribute(String id, List<ItemVerifier> verifiers, List<ItemVerifier> excludes, int weight, Style style, List<AttributeTemplate> attributes) {
        this.id = id;
        this.verifiers = verifiers;
        this.excludes = excludes != null ? excludes : List.of();
        this.weight = weight;
        this.style = style;
        this.attributes = attributes;
    }

    public List<ItemVerifier> getExcludes() {
        return excludes;
    }
    public String getID() {
        return id;
    }

    public List<ItemVerifier> getVerifiers() {
        return verifiers;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isValid(Identifier id) {
        if (excludes != null) {
            for (ItemVerifier exclude : excludes) {
                if (exclude.isValid(id)) return false;
            }
        }
        for (ItemVerifier verifier : verifiers) {
            if (verifier.isValid(id)) return true;
        }
        return false;
    }


    public Style getStyle() {
        return style;
    }

    public List<AttributeTemplate> getAttributes() {
        return attributes;
    }

}
