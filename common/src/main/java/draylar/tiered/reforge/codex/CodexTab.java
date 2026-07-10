package draylar.tiered.reforge.codex;

import java.util.List;
import net.minecraft.network.chat.Component;

public interface CodexTab {

    String id();

    Component title();

    List<CodexEntry> buildEntries();
}
