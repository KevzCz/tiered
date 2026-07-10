package draylar.tiered.neoforge.mixin.compat;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import draylar.tiered.lib.Tab;
import fuzs.easyanvils.client.gui.screens.inventory.ModAnvilScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

/**
 * No NeoForge-safe equivalent of Fabric's mixin-plugin-time isModLoaded check exists here
 * (NeoForge's early mod-list APIs aren't part of the mod-facing dependency surface), so this
 * relies on {@code @Pseudo} to skip gracefully if EasyAnvils isn't installed, instead of the
 * plugin-based gating {@code TieredFabricMixinPlugin} uses on Fabric. Registered only in the
 * "client" list of tiered.neoforge.mixins.json, which already gates it to client-side.
 */
@Pseudo
@Mixin(ModAnvilScreen.class)
public class ModAnvilScreenMixin implements Tab {

    @Override
    public @Nullable Class<?> getParentScreenClass() {
        return AnvilScreen.class;
    }
}
