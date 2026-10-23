package neth6.soulspoon.client.cajero;

import neth6.soulspoon.banco.ModCajero;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CajeroScreen extends Screen {
   private final BlockPos pos;
   private int x;
   private int y;
   private int lado;
   private int yBotones;

   public CajeroScreen(BlockPos pos) {
      super(Component.literal("Cajero"));
      this.pos = pos;
   }

   protected void init() {
      this.lado = Math.min(200, this.height - 16);
      this.x = (this.width - this.lado) / 2;
      this.y = (this.height - this.lado) / 2 + 4;
      int anchoBoton = (this.lado - 42) / 2;
      int altoBoton = 20;
      int yBoton = this.y + this.lado - 52;
      this.yBotones = yBoton;
      this.addRenderableWidget(
         new BotonCajero(
            this.x + 14, yBoton, anchoBoton, altoBoton, "Ingresar Dinero", () -> ClientPlayNetworking.send(new ModCajero.IngresarPayload(this.pos))
         )
      );
      this.addRenderableWidget(
         new BotonCajero(
            this.x + this.lado - 14 - anchoBoton,
            yBoton,
            anchoBoton,
            altoBoton,
            "Sacar Dinero",
            () -> this.minecraft.setScreen(new CajeroSacarScreen(this.pos))
         )
      );
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.renderBackground(g, mouseX, mouseY, delta);
      EstiloCajero.marco(g, this.font, this.x, this.y, this.lado, this.lado, "Cajero");
      float escala = this.lado / 200.0F * 3.2F;
      float u = escala / 3.2F;
      float centroX = this.x + this.lado / 2.0F;
      float centroY = (this.y + 18 + this.yBotones) / 2.0F;
      moneda(g, EstiloCajero.ICONO_VERDE, centroX - 30.0F * u, centroY + 8.0F * u, escala);
      moneda(g, EstiloCajero.ICONO_DEDITA, centroX, centroY - 10.0F * u, escala);
      moneda(g, EstiloCajero.ICONO_ROJA, centroX + 30.0F * u, centroY + 10.0F * u, escala);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.render(g, mouseX, mouseY, delta);
   }

   private static void moneda(GuiGraphics g, ResourceLocation icono, float cx, float cy, float escala) {
      g.pose().pushPose();
      g.pose().translate(cx - 8.0F * escala, cy - 8.0F * escala, 0.0F);
      g.pose().scale(escala, escala, 1.0F);
      g.blit(icono, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
      g.pose().popPose();
   }

   public boolean isPauseScreen() {
      return false;
   }
}
