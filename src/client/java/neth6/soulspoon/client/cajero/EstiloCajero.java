package neth6.soulspoon.client.cajero;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class EstiloCajero {
   public static final ResourceLocation ICONO_DEDITA = icono("dedita_azul");
   public static final ResourceLocation ICONO_VERDE = icono("dedita_verde");
   public static final ResourceLocation ICONO_ROJA = icono("dedita_roja");
   private static final int ICONO = 16;
   private static final int SEPARACION = 4;
   private static final int ENTRE_MONEDAS = 7;

   private EstiloCajero() {
   }

   private static ResourceLocation icono(String nombre) {
      return ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/gui/" + nombre + ".png");
   }

   public static int arcoiris(float matiz) {
      return 0xFF000000 | Mth.hsvToRgb((matiz % 1.0F + 1.0F) % 1.0F, 0.75F, 1.0F);
   }

   public static float tiempo() {
      return (float)(Util.getMillis() % 6000L) / 6000.0F;
   }

   public static void bordeArcoiris(GuiGraphics g, int x, int y, int ancho, int alto, float desde, float hasta) {
      for (int i = 0; i < ancho; i++) {
         int color = arcoiris(desde + (hasta - desde) * i / Math.max(1, ancho - 1));
         g.fill(x + i, y, x + i + 1, y + 1, color);
         g.fill(x + i, y + alto - 1, x + i + 1, y + alto, color);
      }

      g.fillGradient(x, y, x + 1, y + alto, arcoiris(desde), arcoiris(desde + 0.08F));
      g.fillGradient(x + ancho - 1, y, x + ancho, y + alto, arcoiris(hasta), arcoiris(hasta + 0.08F));
   }

   public static void marco(GuiGraphics g, Font font, int x, int y, int ancho, int alto, String titulo) {
      float t = tiempo();
      g.fill(x, y, x + ancho, y + alto, -16447990);
      g.fillGradient(x + 1, y + 1, x + ancho - 1, y + alto - 1, -2893601, -8485745);
      g.fill(x + 2, y + 2, x + ancho - 2, y + 3, -920328);
      g.fill(x + 6, y + 6, x + ancho - 6, y + alto - 6, -14012872);
      g.fill(x + 7, y + 7, x + ancho - 7, y + alto - 7, -16316405);
      int largo = Math.max(10, ancho / 8);

      for (int[] esquina : new int[][]{{x + 10, y + 2}, {x + ancho - 10 - largo, y + 2}, {x + 10, y + alto - 4}, {x + ancho - 10 - largo, y + alto - 4}}) {
         g.fill(esquina[0], esquina[1], esquina[0] + largo, esquina[1] + 2, -12589326);
      }

      int tiraAlto = alto / 3;
      int tiraY = y + (alto - tiraAlto) / 2;

      for (int i = 0; i * 3 < tiraAlto; i++) {
         int color = arcoiris(t + i * 0.06F);
         int yy = tiraY + i * 3;
         g.fill(x + 2, yy, x + 5, yy + 2, color);
         g.fill(x + ancho - 5, yy, x + ancho - 2, yy + 2, color);
      }

      for (int i = 0; i < 4; i++) {
         g.fill(x + 2, y + 16 + i * 3, x + 5, y + 17 + i * 3, -11906981);
         g.fill(x + ancho - 5, y + 16 + i * 3, x + ancho - 2, y + 17 + i * 3, -11906981);
      }

      int abajo = Math.min(60, ancho / 3);

      for (int i = 0; i < abajo; i += 3) {
         g.fill(x + (ancho - abajo) / 2 + i, y + alto - 5, x + (ancho - abajo) / 2 + i + 2, y + alto - 2, arcoiris(t * 2.0F + (float)i / abajo));
      }

      if (titulo != null) {
         int anchoCartel = font.width(titulo) + 28;
         int cx = x + (ancho - anchoCartel) / 2;
         g.fill(cx - 1, y - 1, cx + anchoCartel + 1, y + 17, -16447990);
         g.fillGradient(cx, y, cx + anchoCartel, y + 16, -3814702, -8354161);
         g.fill(cx + 3, y + 2, cx + anchoCartel - 3, y + 14, -15394525);
         g.drawString(font, titulo, cx + (anchoCartel - font.width(titulo)) / 2, y + 4, -1, true);
      }
   }

   public static List<EstiloCajero.Moneda> monedas(long saldo) {
      List<EstiloCajero.Moneda> lista = new ArrayList<>();
      if (saldo >= 10000L) {
         lista.add(new EstiloCajero.Moneda(Long.toString(saldo / 10000L), ICONO_ROJA));
      }

      if (saldo >= 100L) {
         lista.add(new EstiloCajero.Moneda(Long.toString(saldo / 100L % 100L), ICONO_VERDE));
      }

      lista.add(new EstiloCajero.Moneda(Long.toString(saldo % 100L), ICONO_DEDITA));
      return lista;
   }

   public static int anchoMonedas(Font font, long saldo) {
      return anchoMonedas(font, saldo, 16);
   }

   public static int anchoMonedas(Font font, long saldo, int icono) {
      List<EstiloCajero.Moneda> lista = monedas(saldo);
      int ancho = 7 * (lista.size() - 1);

      for (EstiloCajero.Moneda m : lista) {
         ancho += font.width(m.cantidad()) + 4 + icono;
      }

      return ancho;
   }

   public static void dibujarMonedas(GuiGraphics g, Font font, int x, int y, long saldo) {
      dibujarMonedas(g, font, x, y, saldo, 16);
   }

   public static void dibujarMonedas(GuiGraphics g, Font font, int x, int y, long saldo, int icono) {
      int alto = Math.max(icono, 9);
      int textoY = y + (alto - 9) / 2 + 1;
      int iconoY = y + (alto - icono) / 2;

      for (EstiloCajero.Moneda m : monedas(saldo)) {
         g.drawString(font, m.cantidad(), x, textoY, -1, true);
         x += font.width(m.cantidad()) + 4;
         g.blit(m.icono(), x, iconoY, 0.0F, 0.0F, icono, icono, icono, icono);
         x += icono + 7;
      }
   }

   public record Moneda(String cantidad, ResourceLocation icono) {
   }
}
