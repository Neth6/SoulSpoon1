package neth6.soulspoon.client.correo;

import neth6.soulspoon.correo.Correo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class CorreoCliente {
   private static final ResourceLocation SOBRE = ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/gui/sobre.png");
   private static List<Correo.CartaVista> cartas = List.of();
   private static long recibido;

   private CorreoCliente() {
   }

   public static List<Correo.CartaVista> cartas() {
      return cartas;
   }

   public static Correo.CartaVista carta(int id) {
      for (Correo.CartaVista c : cartas) {
         if (c.id() == id) {
            return c;
         }
      }

      return null;
   }

   public static long segundos(Correo.CartaVista c) {
      return c.segundos() + (Util.getMillis() - recibido) / 1000L;
   }

   public static void registrar() {
      ClientPlayNetworking.registerGlobalReceiver(Correo.BuzonPayload.TYPE, (payload, context) -> {
         cartas = List.copyOf(payload.cartas());
         recibido = Util.getMillis();
      });
      ClientPlayNetworking.registerGlobalReceiver(Correo.AbiertaPayload.TYPE, (payload, context) -> Correo.ABIERTA_CLIENTE = payload.id());
      ClientPlayNetworking.registerGlobalReceiver(Correo.ResultadoPayload.TYPE, (payload, context) -> context.client().execute(() -> {
         Minecraft mc = context.client();
         if (payload.error().isEmpty()) {
            mc.setScreen(new BuzonScreen());
         } else if (mc.screen instanceof CorreoScreen s) {
            s.error(payload.error());
         }
      }));
   }

   public static void dibujarSobre(GuiGraphics g, Minecraft mc, int yArriba) {
      long sinLeer = cartas.stream().filter(c -> !c.leida()).count();
      if (sinLeer != 0L) {
         String n = Long.toString(sinLeer);
         int relleno = 3;
         int icono = 10;
         int ancho = relleno * 2 + mc.font.width(n) + 4 + icono;
         int alto = relleno + Math.max(icono, 9) + relleno;
         g.pose().pushPose();
         g.pose().translate(g.guiWidth() - 3, yArriba, 0.0F);
         g.pose().scale(0.75F, 0.75F, 1.0F);
         int x = -ancho;
         g.fill(x, 0, 0, alto, -11184806);
         g.fill(x + 1, 1, -1, alto - 1, -14803422);
         g.drawString(mc.font, n, x + relleno, relleno + 2, -1, true);
         g.blit(SOBRE, x + relleno + mc.font.width(n) + 4, relleno, 0.0F, 0.0F, icono, icono, icono, icono);
         g.pose().popPose();
      }
   }
}
