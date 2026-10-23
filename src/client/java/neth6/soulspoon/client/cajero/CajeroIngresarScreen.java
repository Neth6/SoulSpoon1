package neth6.soulspoon.client.cajero;

import neth6.soulspoon.banco.CajeroMenu;
import neth6.soulspoon.client.EconomyHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CajeroIngresarScreen extends AbstractContainerScreen<CajeroMenu> {
   private static final int BORDE = 6;

   public CajeroIngresarScreen(CajeroMenu menu, Inventory inventario, Component titulo) {
      super(menu, inventario, titulo);
      this.imageWidth = 176;
      this.imageHeight = 186;
   }

   protected void init() {
      super.init();
      this.addRenderableWidget(
         new BotonCajero(
            this.leftPos + 108,
            this.topPos + 38,
            56,
            20,
            "Ingresar",
            () -> this.minecraft.gameMode.handleInventoryButtonClick(((CajeroMenu)this.menu).containerId, 0)
         )
      );
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.render(g, mouseX, mouseY, delta);
      this.renderTooltip(g, mouseX, mouseY);
   }

   protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
      int x = this.leftPos - 6;
      int y = this.topPos - 6;
      EstiloCajero.marco(g, this.font, x, y, this.imageWidth + 12, this.imageHeight + 12, null);
      float t = EstiloCajero.tiempo();

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 3; col++) {
            int cx = this.leftPos + 22 + col * 22;
            int cy = this.topPos + 20 + fila * 22;
            float matiz = t + (fila * 3 + col) / 9.0F;
            EstiloCajero.bordeArcoiris(g, cx, cy, 20, 20, matiz, matiz + 0.12F);
            g.fill(cx + 1, cy + 1, cx + 19, cy + 19, -15855077);
         }
      }

      int invY = this.topPos + 102 - 6;
      g.fillGradient(this.leftPos - 1, invY, this.leftPos + this.imageWidth + 1, this.topPos + this.imageHeight + 1, -4604218, -8354161);

      for (int fila = 0; fila < 4; fila++) {
         int sy = this.topPos + 102 + fila * 18 + (fila == 3 ? 4 : 0);

         for (int col = 0; col < 9; col++) {
            int sx = this.leftPos + 8 + col * 18;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, -12959926);
            g.fill(sx, sy, sx + 16, sy + 16, -15065820);
         }
      }

      long saldo = Math.max(0L, EconomyHud.total());
      float escala = 0.75F;
      int ancho = Math.round(EstiloCajero.anchoMonedas(this.font, saldo) * escala);
      g.pose().pushPose();
      g.pose().translate(this.leftPos + 136 - ancho / 2.0F, this.topPos + 66, 0.0F);
      g.pose().scale(escala, escala, 1.0F);
      EstiloCajero.dibujarMonedas(g, this.font, 0, 0, saldo);
      g.pose().popPose();
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
   }
}
