package draylar.tiered.lib;

import org.jetbrains.annotations.Nullable;

public interface Tab {

    @Nullable
    Class<?> getParentScreenClass();
}
