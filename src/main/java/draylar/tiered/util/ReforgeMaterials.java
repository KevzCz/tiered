package draylar.tiered.util;

import draylar.tiered.Tiered;
import draylar.tiered.api.ReforgeMaterial;
import draylar.tiered.data.ReforgeMaterialLoader;
import draylar.tiered.registry.ModComponents;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ReforgeMaterials {

    private ReforgeMaterials() {
    }

    @Nullable
    public static ReforgeMaterial resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String overrideJson = stack.get(ModComponents.REFORGE_MATERIAL);
        if (overrideJson != null && !overrideJson.isBlank()) {
            try {
                ReforgeMaterial parsed = ReforgeMaterialLoader.GSON.fromJson(overrideJson, ReforgeMaterial.class);
                if (parsed != null) return parsed;
            } catch (RuntimeException ignored) {

            }
        }
        return Tiered.REFORGE_MATERIAL_LOADER.getMaterial(stack.getItem());
    }

    public static void setOverride(ItemStack stack, @Nullable ReforgeMaterial material) {
        if (material == null) {
            stack.remove(ModComponents.REFORGE_MATERIAL);
        } else {
            stack.set(ModComponents.REFORGE_MATERIAL, ReforgeMaterialLoader.GSON.toJson(material));
        }
    }
}
