package neth6.soulspoon.client;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.economy.Dedita;
import neth6.soulspoon.economy.Economy;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * HUD de deditas arriba a la derecha, sobre la tabla (hud_panel.png).
 * Solo aparecen las monedas que el jugador tiene; sin deditas no se dibuja nada.
 *   [azul]5  [verde]3  [roja]1  [morada]2
 */
public final class EconomyHud {
    private EconomyHud() {}

    private static final ResourceLocation[] ICONS = new ResourceLocation[Dedita.values().length];
    private static final ResourceLocation PANEL = SoulSpoon.id("textures/gui/hud_panel.png");
    // hud_panel.png mide 54x15 y su borde 3 px.
    private static final int PANEL_TEX_W = 54, PANEL_TEX_H = 15, EDGE = 3;
    private static final int BAR_H = 20, PAD = 6, ICON = 13, GAP = 9;

    private static long total = 0;
    private static long lastDelta = 0;
    private static long changedAt = 0;

    static {
        for (Dedita d : Dedita.values()) {
            ICONS[d.ordinal()] = SoulSpoon.id("textures/gui/dedita_" + d.id + ".png");
        }
    }

    public static long total() {
        return total;
    }

    /** Registra el receptor del saldo y el dibujo del panel. Llamar desde SoulSpoonClient. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Economy.BalanceS2C.TYPE, (payload, ctx) -> {
            if (payload.total() != total) {
                lastDelta = payload.total() - total;
                changedAt = System.currentTimeMillis();
            }
            total = payload.total();
        });
        HudRenderCallback.EVENT.register(EconomyHud::render);
    }

    /** Dibuja la tabla estirando el centro y dejando los bordes de 3 px intactos. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        int cw = PANEL_TEX_W - 2 * EDGE, ch = PANEL_TEX_H - 2 * EDGE;
        int mw = w - 2 * EDGE, mh = h - 2 * EDGE;
        // esquinas
        g.blit(PANEL, x, y, EDGE, EDGE, 0, 0, EDGE, EDGE, PANEL_TEX_W, PANEL_TEX_H);
        g.blit(PANEL, x + w - EDGE, y, EDGE, EDGE, PANEL_TEX_W - EDGE, 0, EDGE, EDGE, PANEL_TEX_W, PANEL_TEX_H);
        g.blit(PANEL, x, y + h - EDGE, EDGE, EDGE, 0, PANEL_TEX_H - EDGE, EDGE, EDGE, PANEL_TEX_W, PANEL_TEX_H);
        g.blit(PANEL, x + w - EDGE, y + h - EDGE, EDGE, EDGE, PANEL_TEX_W - EDGE, PANEL_TEX_H - EDGE, EDGE, EDGE, PANEL_TEX_W, PANEL_TEX_H);
        // lados
        g.blit(PANEL, x + EDGE, y, mw, EDGE, EDGE, 0, cw, EDGE, PANEL_TEX_W, PANEL_TEX_H);
        g.blit(PANEL, x + EDGE, y + h - EDGE, mw, EDGE, EDGE, PANEL_TEX_H - EDGE, cw, EDGE, PANEL_TEX_W, PANEL_TEX_H);
        g.blit(PANEL, x, y + EDGE, EDGE, mh, 0, EDGE, EDGE, ch, PANEL_TEX_W, PANEL_TEX_H);
        g.blit(PANEL, x + w - EDGE, y + EDGE, EDGE, mh, PANEL_TEX_W - EDGE, EDGE, EDGE, ch, PANEL_TEX_W, PANEL_TEX_H);
        // centro
        g.blit(PANEL, x + EDGE, y + EDGE, mw, mh, EDGE, EDGE, cw, ch, PANEL_TEX_W, PANEL_TEX_H);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        if (total <= 0) return; // sin deditas no aparece nada

        Font font = mc.font;
        long[] parts = Dedita.split(total);

        int width = PAD * 2 - GAP;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] > 0) width += ICON + 3 + font.width(String.valueOf(parts[i])) + GAP;
        }

        int x = g.guiWidth() - width - 6;
        // Con efectos de pocion (iconos arriba a la derecha) la tabla baja para no taparlos.
        int y = mc.player.getActiveEffects().isEmpty() ? 6 : 56;

        panel(g, x, y, width, BAR_H);

        int cx = x + PAD;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] == 0) continue;
            g.blit(ICONS[i], cx, y + (BAR_H - ICON) / 2, ICON, ICON, 0, 0, 16, 16, 16, 16);
            String n = String.valueOf(parts[i]);
            g.drawString(font, n, cx + ICON + 3, y + (BAR_H - 8) / 2 + 1, 0xFFFFFFFF, true);
            cx += ICON + 3 + font.width(n) + GAP;
        }

        // "+20" / "-5" que se desvanece cuando cambia el saldo.
        long age = System.currentTimeMillis() - changedAt;
        if (lastDelta != 0 && age < 2500) {
            int alpha = (int) (255 * (1.0 - age / 2500.0));
            if (alpha > 12) {
                String change = (lastDelta > 0 ? "+" : "") + format(lastDelta);
                int color = (alpha << 24) | (lastDelta > 0 ? 0x5BE27A : 0xFF6B6B);
                g.drawString(font, change, x + width - font.width(change), y + BAR_H + 3, color, true);
            }
        }
    }

    // ------------------------------------------------------------------ helpers para otras pantallas

    public static String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }

    /** Ancho de una cantidad dibujada con drawAmount. */
    public static int amountWidth(Font font, long amount) {
        long[] parts = Dedita.split(amount);
        int width = 0;
        boolean any = false;
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i] == 0) continue;
            width += 13 + font.width(String.valueOf(parts[i])) + 6;
            any = true;
        }
        return any ? width - 6 : 13 + font.width("0");
    }

    /** Dibuja una cantidad como iconos de colores: [morada]1 [verde]3 [azul]5. */
    public static void drawAmount(GuiGraphics g, Font font, int x, int y, long amount, int textColor) {
        long[] parts = Dedita.split(amount);
        boolean any = false;
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i] == 0) continue;
            any = true;
            g.blit(ICONS[i], x, y - 2, 11, 11, 0, 0, 16, 16, 16, 16);
            String n = String.valueOf(parts[i]);
            g.drawString(font, n, x + 13, y, textColor, false);
            x += 13 + font.width(n) + 6;
        }
        if (!any) {
            g.blit(ICONS[0], x, y - 2, 11, 11, 0, 0, 16, 16, 16, 16);
            g.drawString(font, "0", x + 13, y, textColor, false);
        }
    }
}
