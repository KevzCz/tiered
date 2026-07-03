package draylar.tiered.api;

import draylar.tiered.Tiered;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ItemVerifier {

    private final String id;
    private final String tag;

    public ItemVerifier(String id, String tag) {
        this.id = id;
        this.tag = tag;
    }

    public boolean isValid(Identifier itemID) {
        return isValid(itemID.toString());
    }

    @SuppressWarnings("deprecation")
    public boolean isValid(String itemID) {
        if (id != null) {
            return itemID.equals(id);
        } else if (tag != null) {
            TagKey<Item> itemTag = TagKey.of(RegistryKeys.ITEM, Identifier.of(tag));
            if (itemTag != null) {
                return Registries.ITEM.get(Identifier.of(itemID)).getRegistryEntry().isIn(itemTag);
            } else {
                Tiered.LOGGER.error(tag + " was specified as an item verifier tag, but it does not exist!");
            }
        }

        return false;
    }

    public String getId() {
        return id;
    }

    public TagKey<Item> getTagKey() {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(tag));
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode() * 17 + (tag == null ? 0 : tag.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ItemVerifier other)) {
            return false;
        }
        if (this != other) {
            return false;
        }
        String thisId = this.id == null ? "" : this.id;
        String thisTag = this.tag == null ? "" : this.tag;
        String otherId = other.id == null ? "" : other.id;
        String otherTag = other.tag == null ? "" : other.tag;
        return thisId.equals(otherId) && thisTag.equals(otherTag);
    }
}
