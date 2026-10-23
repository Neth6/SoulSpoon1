package neth6.soulspoon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Boton con el estilo de la ruleta: fondo oscuro, franja de color a la izquierda
 * y brillo al pasar el mouse. "marked" = la opcion elegida (pestana o fila activa).
 */
public class WheelButton extends Button {
    private final int accent;
    public boolean marked = false;

    public WheelButton(int x, int y, int w, int h, Component message, OnPress onPress, int accent) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    private static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hover = active && isHoveredOrFocused();
        boolean disabled = !active && !marked;

        int bg;
        int border;
        int text;
        int stripe = disabled ? 0xFF3B3552 : accent;
        if (marked) {
            bg = shade(accent, 0.55f);
            border = accent;
            text = 0xFFFFFFFF;
        } else if (disabled) {
            bg = 0xFF16121F;
            border = 0xFF251F35;
            text = 0xFF6C6677;
        } else if (hover) {
            bg = shade(accent, 0.30f);
            border = accent;
            text = 0xFFFFFFFF;
        } else {
            bg = 0xFF241E38;
            border = shade(accent, 0.45f);
            text = 0xFFE8E2D8;
        }

        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        g.fill(x + 1, y + 1, x + 4, y + h - 1, stripe);
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                x + w / 2 + 2, y + (h - 8) / 2, text);
    }
}
