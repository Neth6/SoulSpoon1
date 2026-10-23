package neth6.soulspoon.client.cajero;

import neth6.soulspoon.banco.ModCajero;
import neth6.soulspoon.client.EconomyHud;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CajeroSacarScreen extends Screen {
   private static final ResourceLocation[] ICONOS = new ResourceLocation[]{EstiloCajero.ICONO_ROJA, EstiloCajero.ICONO_VERDE, EstiloCajero.ICONO_DEDITA};
   private static final long[] VALORES = new long[]{1000L, 100L, 1L};
   private final BlockPos pos;
   private final int[] cantidades = new int[3];
   private int x;
   private int y;
   private int lado;
   private String mensaje = "";

   public CajeroSacarScreen(BlockPos pos) {
      super(Component.literal("Sacar Dinero"));
      this.pos = pos;
   }

   private static long saldo() {
      return Math.max(0L, EconomyHud.total());
   }

   private long total() {
      long total = 0L;

      for (int i = 0; i < 3; i++) {
         total += this.cantidades[i] * VALORES[i];
      }

      return total;
   }

   private int columnaX(int i) {
      return this.x + this.lado * (i + 1) / 4 + (i - 1) * this.lado / 20;
   }

   protected void init() {
      this.lado = Math.min(200, this.height - 16);
      this.x = (this.width - this.lado) / 2;
      this.y = (this.height - this.lado) / 2 + 4;

      for (int i = 0; i < 3; i++) {
         int moneda = i;
         int cx = this.columnaX(i) - 6;
         this.addRenderableWidget(new FlechaCajero(cx, this.y + this.lado * 30 / 100, true, () -> this.cambiar(moneda, 1)));
         this.addRenderableWidget(new FlechaCajero(cx, this.y + this.lado * 48 / 100, false, () -> this.cambiar(moneda, -1)));
      }

      this.addRenderableWidget(new BotonCajero(this.x + (this.lado - 56) / 2, this.y + this.lado * 78 / 100, 56, 20, "Sacar", this::sacar));
   }

   private void cambiar(int moneda, int direccion) {
      int paso = hasShiftDown() ? 10 : 1;

      for (int i = 0; i < paso; i++) {
         if (direccion > 0) {
            if (this.total() + VALORES[moneda] > saldo() || this.cantidades[moneda] >= 3564) {
               break;
            }

            this.cantidades[moneda]++;
         } else if (this.cantidades[moneda] > 0) {
            this.cantidades[moneda]--;
         }
      }

      this.mensaje = "";
   }

   private void sacar() {
      long total = this.total();
      if (total <= 0L) {
         this.mensaje = "Elige cuántas monedas sacar.";
      } else if (total > saldo()) {
         this.mensaje = "No tienes suficientes deditas.";
      } else {
         ClientPlayNetworking.send(new ModCajero.SacarPayload(this.pos, this.cantidades[0], this.cantidades[1], this.cantidades[2]));
         this.mensaje = "Sacaste " + total + (total == 1L ? " dedita." : " deditas.");
         Arrays.fill(this.cantidades, 0);
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.renderBackground(g, mouseX, mouseY, delta);
      EstiloCajero.marco(g, this.font, this.x, this.y, this.lado, this.lado, "Sacar Dinero");
      float escala = 0.7F;
      String etiqueta = "Saldo: ";
      int anchoMonedas = Math.round(EstiloCajero.anchoMonedas(this.font, saldo()) * escala);
      int sx = this.x + (this.lado - this.font.width(etiqueta) - anchoMonedas) / 2;
      int sy = this.y + this.lado * 19 / 100;
      g.drawString(this.font, etiqueta, sx, sy, -11443, true);
      g.pose().pushPose();
      g.pose().translate(sx + this.font.width(etiqueta), sy - 2, 0.0F);
      g.pose().scale(escala, escala, 1.0F);
      EstiloCajero.dibujarMonedas(g, this.font, 0, 0, saldo());
      g.pose().popPose();

      for (int i = 0; i < 3; i++) {
         String cantidad = Integer.toString(this.cantidades[i]);
         int ancho = 14 + this.font.width(cantidad);
         int cx = this.columnaX(i) - ancho / 2;
         int cy = this.y + this.lado * 39 / 100;
         g.pose().pushPose();
         g.pose().translate(cx, cy - 1, 0.0F);
         g.pose().scale(0.6875F, 0.6875F, 1.0F);
         g.blit(ICONOS[i], 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
         g.pose().popPose();
         g.drawString(this.font, cantidad, cx + 14, cy + 1, -1, true);
      }

      int rx = this.x + this.lado / 4;
      int ry = this.y + this.lado * 60 / 100;
      int rAncho = this.lado / 2;
      int rAlto = this.lado * 12 / 100;
      g.fill(rx, ry, rx + rAncho, ry + rAlto, -15394785);
      String texto = !this.mensaje.isEmpty() ? this.mensaje : (this.total() > 0L ? "Total: " + this.total() : "");
      int color = this.mensaje.startsWith("Sacaste") ? -8589174 : (!this.mensaje.isEmpty() ? -38037 : -3747882);
      if (!texto.isEmpty()) {
         float escalaTexto = Math.min(1.0F, (float)(rAncho - 6) / this.font.width(texto));
         g.pose().pushPose();
         g.pose().translate(rx + rAncho / 2.0F, ry + rAlto / 2.0F, 0.0F);
         g.pose().scale(escalaTexto, escalaTexto, 1.0F);
         g.drawString(this.font, texto, -this.font.width(texto) / 2, -4, color, true);
         g.pose().popPose();
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
