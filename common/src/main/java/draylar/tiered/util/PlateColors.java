package draylar.tiered.util;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

/** Shared color math + diagonal shine sweep for the plate-style tooltip components. */
@Environment(EnvType.CLIENT)
public final class PlateColors {

    private PlateColors() {
    }

    public static int lighten(int argb, float amt) {
        int a = (argb >> 24) & 0xFF;
        int r = clamp((int) (((argb >> 16) & 0xFF) + 255 * amt));
        int g = clamp((int) (((argb >> 8) & 0xFF) + 255 * amt));
        int b = clamp((int) ((argb & 0xFF) + 255 * amt));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darken(int argb, float amt) {
        int a = (argb >> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * (1 - amt));
        int g = (int) (((argb >> 8) & 0xFF) * (1 - amt));
        int b = (int) ((argb & 0xFF) * (1 - amt));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    public static void drawShine(GuiGraphics context, int x, int y, int w, int h) {
        float shinePos = (float) ((System.currentTimeMillis() % 3000L) / 3000.0);
        float bandHalf = 0.15f;
        RenderSystem.enableBlend();
        for (int dx = 0; dx < w; dx += 2) {
            for (int dy = 0; dy < h; dy += 2) {
                float diagPos = ((float) dx / w + (float) dy / h) * 0.5f;
                float dist = Math.abs(diagPos - shinePos);
                if (dist < bandHalf) {
                    float intensity = (1.0f - dist / bandHalf) * 0.28f;
                    int alpha = (int) (intensity * 255) & 0xFF;
                    int color = (alpha << 24) | 0xFFFFFF;
                    context.fillGradient(x + dx, y + dy, x + dx + 2, y + dy + 2, color, color);
                }
            }
        }
    }
}
