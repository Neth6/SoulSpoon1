package neth6.soulspoon.client.cajero;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class FlechaCajero extends AbstractButton {
   public static final int ANCHO = 13;
   public static final int ALTO = 7;
   private final boolean arriba;
   private final Runnable accion;

   public FlechaCajero(int x, int y, boolean arriba, Runnable accion) {
      super(x, y, 13, 7, Component.literal(arriba ? "+" : "-"));
      this.arriba = arriba;
      this.accion = accion;
   }

   public void onPress() {
      this.accion.run();
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
      float t = EstiloCajero.tiempo();

      for (int fila = 0; fila < 7; fila++) {
         int ancho = 1 + 2 * (this.arriba ? fila : 6 - fila);
         boolean base = this.arriba ? fila == 6 : fila == 0;
         int x0 = this.getX() + (13 - ancho) / 2;
         int y = this.getY() + fila;

         for (int i = 0; i < ancho; i++) {
            int color = base ? EstiloCajero.arcoiris(t + i / 13.0F) : (this.isHoveredOrFocused() ? -3152 : (i == 0 ? -3551530 : -1));
            if (!this.active) {
               color = -11183777;
            }

            g.fill(x0 + i, y, x0 + i + 1, y + 1, color);
         }
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput salida) {
      this.defaultButtonNarrationText(salida);
   }
}
