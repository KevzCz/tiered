package draylar.tiered.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

import draylar.tiered.lib.Tab;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;

@Environment(EnvType.CLIENT)
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin implements Tab {

    @Override
    public Class<?> getParentScreenClass() {
        return this.getClass();
    }

}
