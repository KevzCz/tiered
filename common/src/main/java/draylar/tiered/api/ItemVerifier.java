package draylar.tiered.api;

import draylar.tiered.Tiered;
import draylar.tiered.compat.ATCCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemVerifier {

    private static final String ALL_TRINKETS_ID = "tiered:all_trinkets_items";
    private static final String ALL_CURIOS_ID = "tiered:all_curios_items";

    private final String id;
    private final String tag;
    private final String custom;

    public ItemVerifier(String id, String tag) {
        this(id, tag, null);
    }

    public ItemVerifier(String id, String tag, String custom) {
        this.id = id;
        this.tag = tag;
        this.custom = custom;
    }

    public boolean isValid(ResourceLocation itemID) {
        return isValid(itemID.toString());
    }

    @SuppressWarnings("deprecation")
    public boolean isValid(String itemID) {
        if (custom != null) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemID)));
            if (ALL_TRINKETS_ID.equals(custom)) {
                return ATCCompat.isAnyTrinketItem(stack);
            } else if (ALL_CURIOS_ID.equals(custom)) {
                return ATCCompat.isAnyCurioItem(stack);
            } else {
                Tiered.LOGGER.error(custom + " was specified as an item verifier custom id, but it is not recognized!");
                return false;
            }
        } else if (id != null) {
            return itemID.equals(id);
        } else if (tag != null) {
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tag));
            if (itemTag != null) {
                return BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemID)).builtInRegistryHolder().is(itemTag);
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
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(tag));
    }

    @Override
    public int hashCode() {
        int result = id == null ? 0 : id.hashCode();
        result = result * 17 + (tag == null ? 0 : tag.hashCode());
        result = result * 17 + (custom == null ? 0 : custom.hashCode());
        return result;
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
        String thisCustom = this.custom == null ? "" : this.custom;
        String otherId = other.id == null ? "" : other.id;
        String otherTag = other.tag == null ? "" : other.tag;
        String otherCustom = other.custom == null ? "" : other.custom;
        return thisId.equals(otherId) && thisTag.equals(otherTag) && thisCustom.equals(otherCustom);
    }
}
