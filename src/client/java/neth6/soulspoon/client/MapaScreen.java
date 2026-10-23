package neth6.soulspoon.client;

import com.mojang.math.Axis;
import java.util.List;
import neth6.soulspoon.marcas.MarcasPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Mapa a pantalla completa (tecla M): terreno alrededor, jugador, marcas y ubicaciones de hermandad. */
public class MapaScreen extends Screen {
    private static final int[] ESCALAS = {1, 2, 3, 4, 6, 8, 12, 16, 24, 32};
    private static int indiceEscala = 3;
    private int cx, cz;
    private boolean centrado = false;
    private long ultimoArmado;
    private boolean sucio = true;

    public MapaScreen() {
        super(Component.literal("Mapa"));
    }

    private int escala() { return ESCALAS[indiceEscala]; }

    @Override
    protected void init() {
        if (!centrado && this.minecraft.player != null) {
            this.cx = this.minecraft.player.getBlockX();
            this.cz = this.minecraft.player.getBlockZ();
            centrado = true;
        }
        MapaHermandad.olvidar();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float parcial) {
        g.fill(0, 0, this.width, this.height, 0xFF0B0B10);
        Minecraft mc = this.minecraft;
        if (mc.level == null || mc.player == null) return;
        long ahora = Util.getMillis();
        if (sucio && ahora - ultimoArmado > 80) {
            MapaHermandad.olvidar();
            ultimoArmado = ahora;
            sucio = false;
        }
        int w = this.width, h = this.height, esc = escala();
        MapaHermandad.actualizar(w, h, cx, cz, esc);
        g.blit(MapaHermandad.TEXTURA, 0, 0, 0.0F, 0.0F, w, h, w, h);

        String dim = mc.level.dimension().location().toString();
        List<MarcasPayload.Marca> marcas = MarcasCliente.todas();
        for (MarcasPayload.Marca m : marcas) {
            if (!m.dimension().equals(dim)) continue;
            int px = w / 2 + Math.round((m.x() - cx) / (float) esc);
            int pz = h / 2 + Math.round((m.z() - cz) / (float) esc);
            if (px < 0 || pz < 0 || px >= w || pz >= h) continue;
            int color = 0xFF000000 | m.color();
            g.fill(px - 3, pz - 3, px + 4, pz + 4, 0xFF000000);
            g.fill(px - 2, pz - 2, px + 3, pz + 3, color);
            g.drawString(this.font, m.nombre(), px - this.font.width(m.nombre()) / 2, pz - 14, color, true);
        }

        // jugador
        int jx = w / 2 + Math.round((float) (mc.player.getX() - cx) / esc);
        int jz = h / 2 + Math.round((float) (mc.player.getZ() - cz) / esc);
        g.pose().pushPose();
        g.pose().translate(jx, jz, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(mc.player.getYRot() + 180.0F));
        g.drawString(this.font, "▲", -this.font.width("▲") / 2, -4, 0xFFFFFFFF, true);
        g.pose().popPose();

        // barra de info
        double wx = cx + (mx - w / 2) * (double) esc;
        double wz = cz + (my - h / 2) * (double) esc;
        String info = "X " + (int) Math.floor(wx) + "  Z " + (int) Math.floor(wz) + "   |   1 px = " + esc + " bloque" + (esc > 1 ? "s" : "")
            + "   |   Rueda: zoom · Arrastrar: mover · Espacio: centrar · M/Esc: cerrar";
        g.fill(0, h - 14, w, h, 0xB0000000);
        g.drawString(this.font, info, 6, h - 11, 0xFFE0E0E0, false);
    }

    @Override
    public boolean mouseDragged(double x, double y, int boton, double dx, double dy) {
        if (boton == 0 || boton == 2) {
            cx -= (int) Math.round(dx * escala());
            cz -= (int) Math.round(dy * escala());
            sucio = true;
            return true;
        }
        return super.mouseDragged(x, y, boton, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horiz, double vert) {
        int nuevo = Math.max(0, Math.min(ESCALAS.length - 1, indiceEscala + (vert > 0 ? -1 : 1)));
        if (nuevo != indiceEscala) {
            // mantener fijo el punto bajo el cursor
            double wx = cx + (x - this.width / 2.0) * escala();
            double wz = cz + (y - this.height / 2.0) * escala();
            indiceEscala = nuevo;
            cx = (int) Math.round(wx - (x - this.width / 2.0) * escala());
            cz = (int) Math.round(wz - (y - this.height / 2.0) * escala());
            sucio = true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int tecla, int scan, int mods) {
        if (MapaClient.TECLA_MAPA.matches(tecla, scan)) {
            this.onClose();
            return true;
        }
        if (tecla == 32 && this.minecraft.player != null) { // espacio: centrar en el jugador
            cx = this.minecraft.player.getBlockX();
            cz = this.minecraft.player.getBlockZ();
            sucio = true;
            return true;
        }
        return super.keyPressed(tecla, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
