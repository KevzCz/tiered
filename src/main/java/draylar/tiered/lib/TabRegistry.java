package draylar.tiered.lib;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;

import java.util.*;

@Environment(EnvType.CLIENT)
public class TabRegistry {

    private static final Map<Class<?>, List<InventoryTab>> TABS = new HashMap<>();

    private static int lastMouseX = 0;
    private static int lastMouseY = 0;

    public static void setMousePosition(int x, int y) {
        lastMouseX = x;
        lastMouseY = y;
    }

    public static void registerOtherTab(InventoryTab tab, Class<?> parentScreenClass) {
        TABS.computeIfAbsent(parentScreenClass, k -> new ArrayList<>()).add(tab);
        TABS.get(parentScreenClass).sort(Comparator.comparingInt(InventoryTab::getPreferredPos));
    }

    public static List<InventoryTab> getTabs(Class<?> parentScreenClass) {
        return TABS.getOrDefault(parentScreenClass, Collections.emptyList());
    }

    public static List<InventoryTab> getTabsForScreen(Screen screen) {
        if (screen instanceof Tab tab) {
            Class<?> parentClass = tab.getParentScreenClass();
            if (parentClass != null) {
                return getTabs(parentClass);
            }
        }
        return getTabs(screen.getClass());
    }

    public static void clear() {
        TABS.clear();
    }
}
