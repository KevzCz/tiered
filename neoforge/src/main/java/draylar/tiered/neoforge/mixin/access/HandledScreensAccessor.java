package draylar.tiered.neoforge.mixin.access;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MenuScreens.class)
public interface HandledScreensAccessor {

    @Invoker(value = "register")
    static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void tiered$register(
            MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory) {
        throw new AssertionError();
    }
}
