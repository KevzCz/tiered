package draylar.tiered.mixin.compat;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import draylar.tiered.lib.Tab;
import fuzs.easyanvils.client.gui.screens.inventory.ModAnvilScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;

@Environment(EnvType.CLIENT)
@Mixin(ModAnvilScreen.class)
public class ModAnvilScreenMixin implements Tab {

    @Override
    public @Nullable Class<?> getParentScreenClass() {
        return AnvilScreen.class;
    }
}
