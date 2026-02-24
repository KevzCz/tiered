package draylar.tiered.reforge.widget;

import org.jetbrains.annotations.Nullable;

import draylar.tiered.lib.InventoryTab;
import draylar.tiered.network.TieredClientPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AnvilTab extends InventoryTab {

    public AnvilTab(Text title, @Nullable Identifier texture, int preferedPos, Class<?>... screenClasses) {
        super(title, texture, preferedPos, screenClasses);
    }

    @Override
    public void onClick(MinecraftClient client) {
        TieredClientPacket.writeC2SScreenPacket((int) client.mouse.getX(), (int) client.mouse.getY(), false);
    }

}
