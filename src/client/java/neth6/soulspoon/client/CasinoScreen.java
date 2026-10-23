package neth6.soulspoon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import neth6.soulspoon.casino.CasinoNet;
import neth6.soulspoon.casino.CasinoPool;
import neth6.soulspoon.economy.Dedita;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Mini panel del casino (shift + clic izquierdo): lista de premios con su probabilidad.
 * Los jugadores solo la ven; los admins editan pesos, agregan el item de su mano, quitan premios y fijan el precio.
 */
public class CasinoScreen extends Screen {
    private static final int W = 330;
    private static final int ROW_H = 22;
    private static final int VISIBLE = 7;
    private static final int HEADER = 30;
    private static final int FOOTER = 74;

    private static final int GOLD = 0xFFFFAA00;
    private static final int PURPLE = 0xFFC77DFF;
    private static final int BLUE = 0xFF55AAFF;
    private static final int GRAY = 0xFFBBBBBB;
    private static final int DARK = 0xFF888888;

    private static final class Row {
        ItemStack stack;
        int weight;
        Row(ItemStack s, int w) { stack = s; weight = w; }
        boolean nada() { return stack.isEmpty(); }
    }

    private final boolean editable;
    private final int fichas;
    private long precio;
    private final List<Row> rows = new ArrayList<>();
    private int scroll = 0;

    public CasinoScreen(CasinoNet.TablaS2C data) {
        super(Component.literal("Casino"));
        this.editable = data.editable();
        this.fichas = data.fichas();
        this.precio = data.precio();
        for (CasinoPool.Entry e : data.entradas()) rows.add(new Row(e.stack().copy(), e.weight()));
    }

    private int x0() { return (this.width - W) / 2; }
    private int y0() { return (this.height - (HEADER + VISIBLE * ROW_H + FOOTER)) / 2; }
    private int total() { int t = 0; for (Row r : rows) t += Math.max(0, r.weight); return t; }
    private double prob(Row r) { int t = total(); return t <= 0 ? 0 : (double) Math.max(0, r.weight) / t; }

    @Override
    protected void init() {
        int x0 = x0();
        int y0 = y0();
        int maxScroll = Math.max(0, rows.size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int footY = y0 + HEADER + VISIBLE * ROW_H + 24;
        if (editable) {
            for (int i = 0; i < VISIBLE; i++) {
                int idx = scroll + i;
                if (idx >= rows.size()) break;
                Row row = rows.get(idx);
                int y = y0 + HEADER + i * ROW_H + 3;
                addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    row.weight = Math.max(0, row.weight - (hasShiftDown() ? 1 : 10));
                }).bounds(x0 + W - 64, y, 16, 16).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    row.weight = Math.min(CasinoPool.MAX_PESO, row.weight + (hasShiftDown() ? 1 : 10));
                }).bounds(x0 + W - 46, y, 16, 16).build());
                if (!row.nada()) {
                    addRenderableWidget(Button.builder(Component.literal("x"), b -> {
                        rows.remove(row);
                        rebuildWidgets();
                    }).bounds(x0 + W - 26, y, 16, 16).build());
                }
            }
            addRenderableWidget(Button.builder(Component.literal("Agregar item de tu mano"), b -> {
                var player = Minecraft.getInstance().player;
                if (player != null && !player.getMainHandItem().isEmpty() && rows.size() < CasinoPool.MAX_ENTRADAS) {
                    rows.add(new Row(player.getMainHandItem().copy(), 10));
                    scroll = Math.max(0, rows.size() - VISIBLE);
                    rebuildWidgets();
                }
            }).bounds(x0 + 8, footY, 140, 20).build());
            addRenderableWidget(Button.builder(Component.literal("-"), b -> precio = Math.max(0, precio - (hasShiftDown() ? 10 : 100)))
                    .bounds(x0 + W - 148, footY, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> precio = Math.min(1_000_000, precio + (hasShiftDown() ? 10 : 100)))
                    .bounds(x0 + W - 24, footY, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> enviar(false))
                    .bounds(x0 + 8, footY + 24, 90, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Restablecer"), b -> enviar(true))
                    .bounds(x0 + 102, footY + 24, 90, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                    .bounds(x0 + W - 98, footY + 24, 90, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                    .bounds(x0 + W / 2 - 45, footY + 24, 90, 20).build());
        }
    }

    private void enviar(boolean restablecer) {
        List<CasinoPool.Entry> out = new ArrayList<>();
        if (!restablecer) for (Row r : rows) out.add(new CasinoPool.Entry(r.stack.copy(), r.weight));
        ClientPlayNetworking.send(new CasinoNet.GuardarC2S(precio, out));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int maxScroll = Math.max(0, rows.size() - VISIBLE);
        int nuevo = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(sy)));
        if (nuevo != scroll) {
            scroll = nuevo;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        int x0 = x0();
        int y0 = y0();
        int h = HEADER + VISIBLE * ROW_H + FOOTER;
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + h + 2, 0xFFB8860B);        // borde dorado
        g.fill(x0, y0, x0 + W, y0 + h, 0xF0140C16);                        // fondo
        g.fill(x0, y0, x0 + W, y0 + HEADER - 4, 0xFF3A0A1F);               // franja del titulo
        for (int i = 0; i < VISIBLE; i++) {
            if (i % 2 == 0) g.fill(x0 + 4, y0 + HEADER + i * ROW_H, x0 + W - 4, y0 + HEADER + (i + 1) * ROW_H, 0x30FFFFFF);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x0 = x0();
        int y0 = y0();
        g.drawCenteredString(this.font, "CASINO - Premios y probabilidades", x0 + W / 2, y0 + 8, GOLD);

        for (int i = 0; i < VISIBLE; i++) {
            int idx = scroll + i;
            if (idx >= rows.size()) break;
            Row r = rows.get(idx);
            int y = y0 + HEADER + i * ROW_H;
            double p = prob(r);
            int color = r.nada() ? DARK : p <= 0.01 ? GOLD : p <= 0.05 ? PURPLE : p <= 0.15 ? BLUE : GRAY;
            String rareza = r.nada() ? "" : p <= 0.01 ? "Legendario" : p <= 0.05 ? "Epico" : p <= 0.15 ? "Raro" : "Comun";

            if (r.nada()) {
                g.drawString(this.font, "X", x0 + 12, y + 7, DARK, false);
            } else {
                g.renderItem(r.stack, x0 + 8, y + 3);
                g.renderItemDecorations(this.font, r.stack, x0 + 8, y + 3);
            }
            Component nombre = r.nada() ? Component.literal("Perder (nada)") : r.stack.getHoverName();
            g.drawString(this.font, this.font.plainSubstrByWidth(nombre.getString(), 110), x0 + 30, y + 3, color, false);
            if (!rareza.isEmpty()) g.drawString(this.font, rareza, x0 + 30, y + 12, color, false);

            String pct = String.format(Locale.ROOT, p < 0.1 ? "%.2f%%" : "%.1f%%", p * 100);
            g.drawString(this.font, pct, x0 + W - 124 - this.font.width(pct) + (editable ? 0 : 50), y + 7, color, false);
            // barra de probabilidad
            int barMax = 70;
            int bx = x0 + 150;
            g.fill(bx, y + 17, bx + barMax, y + 19, 0x40FFFFFF);
            g.fill(bx, y + 17, bx + Math.max(1, (int) (barMax * Math.min(1.0, p * 2))), y + 19, color);
            if (editable) {
                String w = String.valueOf(r.weight);
                g.drawString(this.font, w, x0 + W - 88 + 12 - this.font.width(w) / 2, y + 7, 0xFFFFFFFF, false);
            }
        }
        int base = y0 + HEADER + VISIBLE * ROW_H;
        if (rows.size() > VISIBLE) {
            g.drawString(this.font, (scroll + 1) + "-" + Math.min(rows.size(), scroll + VISIBLE) + " / " + rows.size() + "  (rueda del raton)",
                    x0 + 8, base + 3, DARK, false);
        }
        if (editable) {
            String ayuda = "Shift = pasos de 1";
            g.drawString(this.font, ayuda, x0 + W - 8 - this.font.width(ayuda), base + 3, DARK, false);
        }
        String cuesta = "Giro: 1 dedita naranja (tienes " + fichas + ") o " + precio + " (" + Dedita.describe(precio) + ")";
        g.drawString(this.font, this.font.plainSubstrByWidth(cuesta, W - 16), x0 + 8, base + 13, GOLD, false);
        if (editable) {
            g.drawCenteredString(this.font, String.valueOf(precio), x0 + W - 76, base + 24 + 6, 0xFFFFFFFF);
        }
    }
}
