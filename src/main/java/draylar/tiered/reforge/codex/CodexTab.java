package draylar.tiered.reforge.codex;

import java.util.List;

import net.minecraft.text.Text;

public interface CodexTab {

    String id();

    Text title();

    List<CodexEntry> buildEntries();
}
