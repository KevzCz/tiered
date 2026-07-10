package draylar.tiered.reforge.codex;

import java.util.ArrayList;
import java.util.List;

public final class CodexTabRegistry {

    private static final List<CodexTab> TABS = new ArrayList<>();
    private static boolean bootstrapped = false;

    private CodexTabRegistry() {
    }

    public static void register(CodexTab tab) {
        for (int i = 0; i < TABS.size(); i++) {
            if (TABS.get(i).id().equals(tab.id())) {
                TABS.set(i, tab);
                return;
            }
        }
        TABS.add(tab);
    }

    public static List<CodexTab> tabs() {
        if (!bootstrapped) {
            bootstrapped = true;
            register(new ImprintCodexTab());
            register(new EffectCodexTab());
        }
        return List.copyOf(TABS);
    }
}
