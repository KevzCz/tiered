package draylar.tiered.mixin;

import net.minecraft.registry.ReloadableRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.Tiered;
import net.minecraft.loot.LootDataType;
import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.RegistryOps;
import net.minecraft.resource.ResourceManager;

import com.google.gson.JsonElement;

@Mixin(ReloadableRegistries.class)
public class ReloadableRegistriesMixin {

    @Inject(method = "method_58279", at = @At("HEAD"))
    private static <T> void tieredLoadRuneInjections(LootDataType<T> lootDataType, ResourceManager resourceManager,
            RegistryOps<JsonElement> registryOps, CallbackInfoReturnable<MutableRegistry<?>> cir) {
        if (lootDataType != LootDataType.LOOT_TABLES) return;
        Tiered.RUNE_INJECTION_LOADER.loadFrom(resourceManager);
    }
}
