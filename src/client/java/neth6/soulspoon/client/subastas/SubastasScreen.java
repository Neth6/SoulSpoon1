package neth6.soulspoon.client.subastas;

import neth6.soulspoon.client.cajero.EstiloCajero;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import neth6.soulspoon.subastas.MenuSubastas;
import neth6.soulspoon.subastas.Subastas;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SubastasScreen extends AbstractContainerScreen<MenuSubastas> {
   private static final String[] PESTANAS = new String[]{"Comprar", "Vender", "Mis ventas", "Recuperar"};
   private final Button[] pestanas = new Button[4];
   private EditBox precio;
   private Button ponerALaVenta;

   public SubastasScreen(MenuSubastas menu, Inventory inventario, Component titulo) {
      super(menu, inventario, titulo);
      this.imageWidth = 190;
      this.imageHeight = 236;
      this.inventoryLabelY = 10000;
      this.titleLabelY = 10000;
   }

   protected void init() {
      super.init();
      int l = this.leftPos;
      int t = this.topPos;

      for (int i = 0; i < 4; i++) {
         int boton = i;
         this.pestanas[i] = (Button)this.addRenderableWidget(
            Button.builder(
                  Component.literal(PESTANAS[i]), b -> this.minecraft.gameMode.handleInventoryButtonClick(((MenuSubastas)this.menu).containerId, boton)
               )
               .bounds(l + this.imageWidth + 4, t + 16 + i * 24, 66, 20)
               .build()
         );
      }

      this.precio = new EditBox(this.font, l + 14 + 36, t + 43 + 26, 90, 14, Component.literal("Precio"));
      this.precio.setMaxLength(10);
      this.precio.setFilter(s -> s.chars().allMatch(Character::isDigit));
      this.precio.setHint(Component.literal("precio en deditas").withColor(-10854288));
      this.addRenderableWidget(this.precio);
      this.ponerALaVenta = (Button)this.addRenderableWidget(Button.builder(Component.literal("Poner a la venta"), b -> {
         if (!this.precio.getValue().isEmpty()) {
            ClientPlayNetworking.send(new Subastas.PrecioPayload(Long.parseLong(this.precio.getValue())));
            this.precio.setValue("");
         }
      }).bounds(l + 14 + 36 - 4, t + 43 + 44, 98, 18).build());
   }

   protected void containerTick() {
      super.containerTick();
      boolean vender = ((MenuSubastas)this.menu).pestana() == 1;
      this.precio.visible = vender;
      this.ponerALaVenta.visible = vender;

      for (int i = 0; i < 4; i++) {
         this.pestanas[i].active = ((MenuSubastas)this.menu).pestana() != i;
      }
   }

   public boolean mouseClicked(double mx, double my, int boton) {
      int y = this.topPos + 118;
      if (((MenuSubastas)this.menu).pestana() != 1 && my >= y && my < y + 16) {
         int izq = this.leftPos + 14;
         int der = izq + 162;
         if (mx >= izq && mx < izq + 40 && ((MenuSubastas)this.menu).pagina() > 0) {
            this.minecraft.gameMode.handleInventoryButtonClick(((MenuSubastas)this.menu).containerId, 10);
            return true;
         }

         if (mx >= der - 40 && mx < der && ((MenuSubastas)this.menu).pagina() < ((MenuSubastas)this.menu).paginas() - 1) {
            this.minecraft.gameMode.handleInventoryButtonClick(((MenuSubastas)this.menu).containerId, 11);
            return true;
         }
      }

      return super.mouseClicked(mx, my, boton);
   }

   public boolean keyPressed(int tecla, int codigo, int modificadores) {
      if (this.precio.isFocused() && this.precio.visible && tecla != 256) {
         if (!this.precio.keyPressed(tecla, codigo, modificadores)) {
         }

         return true;
      } else {
         return super.keyPressed(tecla, codigo, modificadores);
      }
   }

   protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
      int l = this.leftPos;
      int t = this.topPos;
      EstiloCajero.marco(g, this.font, l, t, this.imageWidth, this.imageHeight, "Subastas");
      float tiempo = EstiloCajero.tiempo();
      int gx = l + 14;
      int gy = t + 24;
      int anchoGrilla = 162;
      int altoGrilla = 90;
      boolean vender = ((MenuSubastas)this.menu).pestana() == 1;
      EstiloCajero.bordeArcoiris(g, gx - 2, gy - 2, anchoGrilla + 4, altoGrilla + 4, tiempo, tiempo + 0.9F);
      g.fill(gx - 1, gy - 1, gx + anchoGrilla + 1, gy + altoGrilla + 1, -16052973);
      if (vender) {
         int vx = l + 87 - 1;
         int vy = t + 43 - 1;
         g.drawCenteredString(this.font, "Pon acá lo que quieres vender", gx + anchoGrilla / 2, gy + 5, -4669236);
         EstiloCajero.bordeArcoiris(g, vx - 2, vy - 2, 22, 22, tiempo, tiempo + 0.5F);
         g.fill(vx - 1, vy - 1, vx + 19, vy + 19, -15065560);
         g.fill(vx, vy, vx + 18, vy + 18, -14012613);
      } else {
         for (int fila = 0; fila < 5; fila++) {
            for (int col = 0; col < 9; col++) {
               int x = gx + col * 18;
               int y = gy + fila * 18;
               g.fill(x + 1, y + 1, x + 17, y + 17, -14933712);
               g.fill(x + 1, y + 1, x + 17, y + 2, -14275523);
            }
         }

         int py = t + 118;
         this.boton(g, gx, py, 40, "<<<", ((MenuSubastas)this.menu).pagina() > 0, mouseX, mouseY);
         this.boton(g, gx + anchoGrilla - 40, py, 40, ">>>", ((MenuSubastas)this.menu).pagina() < ((MenuSubastas)this.menu).paginas() - 1, mouseX, mouseY);
         g.drawCenteredString(this.font, ((MenuSubastas)this.menu).pagina() + 1 + " / " + ((MenuSubastas)this.menu).paginas(), gx + anchoGrilla / 2, py + 4, -1);
      }

      int ix = l + 15 - 1;
      int iy = t + 152 - 1;

      for (int fila = 0; fila < 4; fila++) {
         int y = iy + fila * 18 + (fila == 3 ? 4 : 0);

         for (int col = 0; col < 9; col++) {
            int x = ix + col * 18;
            g.fill(x, y, x + 18, y + 18, -16052973);
            g.fill(x + 1, y + 1, x + 17, y + 17, fila == 3 ? -14012613 : -14933712);
         }
      }

      EstiloCajero.bordeArcoiris(g, ix - 1, iy + 54 + 3, 164, 20, tiempo + 0.3F, tiempo + 1.2F);
   }

   private void boton(GuiGraphics g, int x, int y, int ancho, String texto, boolean activo, int mx, int my) {
      boolean encima = activo && mx >= x && mx < x + ancho && my >= y && my < y + 16;
      float tiempo = EstiloCajero.tiempo();
      if (activo) {
         EstiloCajero.bordeArcoiris(g, x, y, ancho, 16, tiempo, tiempo + 0.4F);
      } else {
         g.fill(x, y, x + ancho, y + 16, -12959926);
      }

      g.fill(x + 1, y + 1, x + ancho - 1, y + 15, encima ? -13880250 : -15394525);
      g.drawCenteredString(this.font, texto, x + ancho / 2, y + 4, activo ? -1 : -10854288);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.render(g, mouseX, mouseY, delta);
      this.renderTooltip(g, mouseX, mouseY);
   }
}
