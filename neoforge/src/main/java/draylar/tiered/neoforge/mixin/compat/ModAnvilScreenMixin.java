package draylar.tiered.neoforge.mixin.compat;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import draylar.tiered.lib.Tab;
import fuzs.easyanvils.client.gui.screens.inventory.ModAnvilScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

@Pseudo
@Mixin(ModAnvilScreen.class)
public class ModAnvilScreenMixin implements Tab {

    @Override
    public @Nullable Class<?> getParentScreenClass() {
        return AnvilScreen.class;
    }
}
