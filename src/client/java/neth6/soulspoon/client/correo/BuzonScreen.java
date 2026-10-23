package neth6.soulspoon.client.correo;

import neth6.soulspoon.client.cajero.BotonCajero;
import neth6.soulspoon.client.cajero.EstiloCajero;
import neth6.soulspoon.correo.Correo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public class BuzonScreen extends Screen {
   private static final int ALTO_FILA = 12;
   private int x;
   private int y;
   private int ancho;
   private int alto;
   private int pagina;
   private int yLista;
   private int filas;

   public BuzonScreen() {
      super(Component.literal("Buzón de correo"));
   }

   protected void init() {
      this.alto = Math.min(250, this.height - 16);
      this.ancho = this.alto * 4 / 5;
      this.x = (this.width - this.ancho) / 2;
      this.y = (this.height - this.alto) / 2;
      int anchoFlecha = this.ancho * 21 / 100;
      int altoFlecha = Math.max(16, this.alto * 7 / 100);
      int yFlecha = this.y + this.alto - 12 - altoFlecha;
      this.yLista = this.y + 36;
      this.filas = Math.max(1, (yFlecha - 6 - this.yLista) / 12);
      this.addRenderableWidget(new BotonCajero(this.x + 14, yFlecha, anchoFlecha, altoFlecha, "<<<", () -> this.pagina = Math.max(0, this.pagina - 1)));
      this.addRenderableWidget(
         new BotonCajero(
            this.x + this.ancho - 14 - anchoFlecha, yFlecha, anchoFlecha, altoFlecha, ">>>", () -> this.pagina = Math.min(this.paginas() - 1, this.pagina + 1)
         )
      );
      int lado = Math.max(18, this.ancho / 7);
      this.addRenderableWidget(new BuzonScreen.BotonMas(this.x - 1, this.y - 1, lado, () -> ClientPlayNetworking.send(new Correo.AccionPayload("abrir", -1))));
   }

   private int paginas() {
      return Math.max(1, (CorreoCliente.cartas().size() + this.filas - 1) / this.filas);
   }

   static String hace(long segundos) {
      if (segundos < 60L) {
         return segundos + "s";
      } else if (segundos < 3600L) {
         return segundos / 60L + "m";
      } else {
         return segundos < 345600L ? segundos / 3600L + "h" : segundos / 86400L + "d";
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.renderBackground(g, mouseX, mouseY, delta);
      EstiloCajero.marco(g, this.font, this.x, this.y, this.ancho, this.alto, null);
      float escala = 1.6F;
      g.pose().pushPose();
      g.pose().translate(this.x + this.ancho / 2.0F - this.font.width(this.title) * escala / 2.0F, this.y + 16, 0.0F);
      g.pose().scale(escala, escala, 1.0F);
      g.drawString(this.font, this.title, 0, 0, -1, false);
      g.pose().popPose();
      List<Correo.CartaVista> cartas = CorreoCliente.cartas();
      this.pagina = Math.max(0, Math.min(this.pagina, this.paginas() - 1));
      int x0 = this.x + 14;
      int x1 = this.x + this.ancho - 14;
      if (cartas.isEmpty()) {
         String t = "No tienes mensajes.";
         g.drawString(this.font, t, this.x + (this.ancho - this.font.width(t)) / 2, this.yLista + 4, -9801600, false);
      }

      for (int k = 0; k < this.filas; k++) {
         int i = this.pagina * this.filas + k;
         if (i >= cartas.size()) {
            break;
         }

         Correo.CartaVista c = cartas.get(i);
         int yf = this.yLista + k * 12;
         boolean encima = mouseX >= x0 && mouseX < x1 && mouseY >= yf && mouseY < yf + 12 - 1;
         g.fill(x0, yf, x1, yf + 12 - 1, encima ? -12957082 : (c.leida() ? -15328216 : -14536632));
         g.fill(x0, yf + 12 - 2, x1, yf + 12 - 1, -13747632);
         int color = c.leida() ? -5721920 : -1;
         String tiempo = hace(CorreoCliente.segundos(c));
         Component nombre = Component.literal("De: " + c.autor()).withColor(color);
         if (!c.asunto().isEmpty()) {
            nombre = nombre.copy().append(Component.literal(" · " + c.asunto()).withColor(-7696224));
         }

         if (c.conObjetos()) {
            nombre = nombre.copy().append(Component.literal(" ✦").withColor(-729014));
         }

         int anchoNombre = x1 - x0 - 10 - this.font.width(tiempo);
         g.drawString(
            this.font,
            this.font.width(nombre) <= anchoNombre
               ? nombre.getVisualOrderText()
               : Language.getInstance().getVisualOrder(this.font.substrByWidth(nombre, anchoNombre)),
            x0 + 3,
            yf + 2,
            color,
            false
         );
         g.drawString(this.font, tiempo, x1 - 3 - this.font.width(tiempo), yf + 2, color, false);
      }

      if (this.paginas() > 1) {
         String p = this.pagina + 1 + "/" + this.paginas();
         g.drawString(this.font, p, this.x + (this.ancho - this.font.width(p)) / 2, this.y + this.alto - 24, -7696224, false);
      }
   }

   public boolean mouseClicked(double mx, double my, int boton) {
      if (super.mouseClicked(mx, my, boton)) {
         return true;
      }

      int x0 = this.x + 14;
      int x1 = this.x + this.ancho - 14;
      List<Correo.CartaVista> cartas = CorreoCliente.cartas();

      for (int k = 0; k < this.filas; k++) {
         int i = this.pagina * this.filas + k;
         int yf = this.yLista + k * 12;
         if (i < cartas.size() && !(mx < x0) && !(mx >= x1) && !(my < yf) && !(my >= yf + 12 - 1)) {
            ClientPlayNetworking.send(new Correo.AccionPayload(boton == 1 ? "borrar" : "abrir", cartas.get(i).id()));
            return true;
         }
      }

      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class BotonMas extends AbstractButton {
      private final Runnable accion;

      BotonMas(int x, int y, int lado, Runnable accion) {
         super(x, y, lado, lado, Component.literal("+"));
         this.accion = accion;
      }

      public void onPress() {
         this.accion.run();
      }

      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
         int x = this.getX();
         int y = this.getY();
         int lado = this.getWidth();
         float t = EstiloCajero.tiempo();
         g.fill(x - 1, y - 1, x + lado + 1, y + lado + 1, -16447990);
         EstiloCajero.bordeArcoiris(g, x, y, lado, lado, 0.95F + t, 1.6F + t);
         g.fill(x + 1, y + 1, x + lado - 1, y + lado - 1, this.isHoveredOrFocused() ? -14011834 : -15394266);
         int c = lado / 2;
         int largo = lado * 3 / 10;
         int grosor = Math.max(2, lado / 9);
         g.fillGradient(x + c - grosor / 2, y + c - largo, x + c - grosor / 2 + grosor, y + c + largo, -729014, -11544336);

         for (int i = -largo; i < largo; i++) {
            if (i < -grosor / 2 || i >= grosor - grosor / 2) {
               int color = EstiloCajero.arcoiris(0.9F + 0.15F * (i + largo) / (2.0F * largo));
               g.fill(x + c + i, y + c - grosor / 2, x + c + i + 1, y + c - grosor / 2 + grosor, color);
            }
         }
      }

      protected void updateWidgetNarration(NarrationElementOutput salida) {
         this.defaultButtonNarrationText(salida);
      }
   }
}
