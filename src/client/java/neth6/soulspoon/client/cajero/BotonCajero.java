package neth6.soulspoon.client.cajero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class BotonCajero extends AbstractButton {
   private final Runnable accion;

   public BotonCajero(int x, int y, int ancho, int alto, String texto, Runnable accion) {
      super(x, y, ancho, alto, Component.literal(texto));
      this.accion = accion;
   }

   public void onPress() {
      this.accion.run();
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
      int x = this.getX();
      int y = this.getY();
      int ancho = this.getWidth();
      int alto = this.getHeight();
      g.fill(x - 1, y - 1, x + ancho + 1, y + alto + 1, -16447990);
      float t = EstiloCajero.tiempo();
      EstiloCajero.bordeArcoiris(g, x, y, ancho, alto, 0.95F + t, 1.6F + t);
      g.fill(x + 1, y + 1, x + ancho - 1, y + alto - 1, this.isHoveredOrFocused() ? -14011834 : -15394266);
      Font font = Minecraft.getInstance().font;
      float escala = Math.min(1.0F, (float)(ancho - 8) / font.width(this.getMessage()));
      g.pose().pushPose();
      g.pose().translate(x + ancho / 2.0F, y + alto / 2.0F, 0.0F);
      g.pose().scale(escala, escala, 1.0F);
      g.drawString(font, this.getMessage(), -font.width(this.getMessage()) / 2, -4, this.active ? -1 : -7696487, true);
      g.pose().popPose();
   }

   protected void updateWidgetNarration(NarrationElementOutput salida) {
      this.defaultButtonNarrationText(salida);
   }
}
