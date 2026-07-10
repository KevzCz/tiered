package draylar.tiered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.Tiered;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import com.google.gson.JsonElement;

@Mixin(ReloadableServerRegistries.class)
public class ReloadableRegistriesMixin {

    @Inject(method = "method_58279", at = @At("HEAD"))
    private static <T> void tieredLoadRuneInjections(LootDataType<T> lootDataType, ResourceManager resourceManager,
            RegistryOps<JsonElement> registryOps, CallbackInfoReturnable<WritableRegistry<?>> cir) {
        if (lootDataType != LootDataType.TABLE) return;
        Tiered.RUNE_INJECTION_LOADER.loadFrom(resourceManager);
    }
}
