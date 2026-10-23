package neth6.soulspoon.client.boveda;

import neth6.soulspoon.boveda.BovedaMenu;
import neth6.soulspoon.boveda.ModBoveda;
import neth6.soulspoon.client.EconomyHud;
import neth6.soulspoon.client.cajero.EstiloCajero;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BovedaScreen extends AbstractContainerScreen<BovedaMenu> {
   private static final ResourceLocation[] ICONOS = new ResourceLocation[]{EstiloCajero.ICONO_ROJA, EstiloCajero.ICONO_VERDE, EstiloCajero.ICONO_DEDITA};
   private static final int PANEL_Y = 31;
   private static final int PANEL_ALTO = 60;
   private static final int NARANJA = -874181;
   private static final int PANEL = -15459533;
   private static final long DURACION_AVISO = 5000L;
   private static ModBoveda.EstadoPayload estado = new ModBoveda.EstadoPayload("", 0L, "", false);
   private static long avisoDesde;
   private final EditBox[] casilleros = new EditBox[3];

   public BovedaScreen(BovedaMenu menu, Inventory inventario, Component titulo) {
      super(menu, inventario, titulo);
      this.imageWidth = 210;
      this.imageHeight = 182;
   }

   public static void recibir(ModBoveda.EstadoPayload nuevo) {
      estado = nuevo;
      if (!nuevo.mensaje().isEmpty()) {
         avisoDesde = Util.getMillis();
         if (!nuevo.error() && Minecraft.getInstance().screen instanceof BovedaScreen pantalla) {
            for (EditBox casillero : pantalla.casilleros) {
               casillero.setValue("");
            }
         }
      }
   }

   protected void init() {
      super.init();
      avisoDesde = 0L;
      int y = this.topPos + 31 + 22;

      for (int i = 0; i < 3; i++) {
         String anterior = this.casilleros[i] != null ? this.casilleros[i].getValue() : "";
         EditBox casillero = new EditBox(this.font, this.leftPos + 9 + i * 38, y, 24, 14, Component.literal("Cantidad"));
         casillero.setMaxLength(7);
         casillero.setFilter(texto -> texto.chars().allMatch(Character::isDigit));
         casillero.setValue(anterior);
         this.casilleros[i] = (EditBox)this.addRenderableWidget(casillero);
      }

      BovedaScreen.BotonBoveda depositar = (BovedaScreen.BotonBoveda)this.addRenderableWidget(
         new BovedaScreen.BotonBoveda(this.leftPos + 124, y - 1, 42, 16, "Depositar", () -> this.enviar(0))
      );
      depositar.setTooltip(Tooltip.create(Component.literal("Deposita las monedas de las casillas de arriba y lo que escribas (sale de tu cuenta del banco).")));
      BovedaScreen.BotonBoveda retirar = (BovedaScreen.BotonBoveda)this.addRenderableWidget(
         new BovedaScreen.BotonBoveda(this.leftPos + 169, y - 1, 34, 16, "Retirar", () -> this.enviar(1))
      );
      retirar.setTooltip(Tooltip.create(Component.literal("Retira lo que escribas, en monedas. Solo el Maestro y los Líderes.")));
   }

   private int cantidad(int i) {
      try {
         return Math.min(1000000, Integer.parseInt(this.casilleros[i].getValue()));
      } catch (NumberFormatException e) {
         return 0;
      }
   }

   private void enviar(int accion) {
      ClientPlayNetworking.send(new ModBoveda.AccionPayload(accion, this.cantidad(0), this.cantidad(1), this.cantidad(2)));
   }

   public boolean keyPressed(int tecla, int codigo, int modificadores) {
      if (tecla == 256) {
         return super.keyPressed(tecla, codigo, modificadores);
      }

      for (EditBox casillero : this.casilleros) {
         if (casillero.isFocused()) {
            return casillero.keyPressed(tecla, codigo, modificadores) || casillero.canConsumeInput() || super.keyPressed(tecla, codigo, modificadores);
         }
      }

      return super.keyPressed(tecla, codigo, modificadores);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.render(g, mouseX, mouseY, delta);
      this.renderTooltip(g, mouseX, mouseY);
   }

   protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      int w = this.imageWidth;
      int h = this.imageHeight;
      g.fill(x, y, x + w, y + h, -15724524);
      g.fill(x + 1, y + 1, x + w - 1, y + h - 1, -3750202);
      g.fill(x + 1, y + 1, x + w - 2, y + 3, -723724);
      g.fill(x + 1, y + 1, x + 3, y + h - 2, -723724);
      g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, -10855846);
      g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, -10855846);

      for (int ax : new int[]{x + 14, x + w - 44}) {
         g.fill(ax, y - 1, ax + 30, y + 2, -874181);
         g.fill(ax, y + h - 2, ax + 30, y + h + 1, -874181);
      }

      for (int ay : new int[]{y + 22, y + h - 50}) {
         g.fill(x - 1, ay, x + 2, ay + 26, -874181);
         g.fill(x + w - 2, ay, x + w + 1, ay + 26, -874181);
      }

      for (int col = 0; col < 9; col++) {
         int sx = x + 25 + col * 18;
         int sy = y + 9;
         g.fill(sx - 1, sy - 1, sx + 17, sy + 17, -15066594);
         g.fill(sx, sy, sx + 16, sy + 16, -13355974);
      }

      int px = x + 5;
      int py = y + 31;
      int pw = w - 10;
      g.fill(px, py, px + pw, py + 60, -874181);
      g.fill(px + 1, py + 1, px + pw - 1, py + 60 - 1, -15459533);
      g.fill(px + 2, py + 2, px + pw - 2, py + 3, -14274214);
      this.dibujarBalance(g, x + w / 2, py + 7);

      for (int i = 0; i < 3; i++) {
         g.blit(ICONOS[i], x + 9 + i * 38 + 25, py + 24, 10, 10, 0.0F, 0.0F, 16, 16, 16, 16);
      }

      this.dibujarAviso(g, x + w / 2, py + 60 - 12, pw - 8);

      for (int fila = 0; fila < 4; fila++) {
         int sy = y + 98 + fila * 18 + (fila == 3 ? 4 : 0);

         for (int col = 0; col < 9; col++) {
            int sx = x + 25 + col * 18;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, -13158601);
            g.fill(sx, sy, sx + 17, sy + 17, -1);
            g.fill(sx, sy, sx + 16, sy + 16, -7631989);
         }
      }
   }

   private void dibujarBalance(GuiGraphics g, int centro, int y) {
      long balance = Math.max(0L, estado.balance());
      String[] partes = new String[]{Long.toString(balance / 10000L), Long.toString(balance / 100L % 100L), Long.toString(balance % 100L)};
      String etiqueta = "Balance: ";
      int ancho = this.font.width(etiqueta);

      for (String parte : partes) {
         ancho += this.font.width(parte) + 2 + 9 + 5;
      }

      int x = centro - (ancho - 5) / 2;
      g.drawString(this.font, etiqueta, x, y, -1, true);
      x += this.font.width(etiqueta);

      for (int i = 0; i < 3; i++) {
         g.drawString(this.font, partes[i], x, y, -1, true);
         x += this.font.width(partes[i]) + 2;
         g.blit(ICONOS[i], x, y - 1, 9, 9, 0.0F, 0.0F, 16, 16, 16, 16);
         x += 14;
      }
   }

   private void dibujarAviso(GuiGraphics g, int centro, int y, int anchoMaximo) {
      boolean hayAviso = !estado.mensaje().isEmpty() && Util.getMillis() - avisoDesde < 5000L;
      String texto;
      int color;
      if (hayAviso) {
         texto = estado.mensaje();
         color = estado.error() ? -38037 : -8589174;
      } else {
         texto = "Tu cuenta: "
            + ModBoveda.formatear(Math.max(0L, EconomyHud.total()))
            + " deditas"
            + (estado.hermandad().isEmpty() ? "" : "  ·  " + estado.hermandad());
         color = -6642763;
      }

      float escala = Math.min(0.8F, (float)anchoMaximo / this.font.width(texto));
      g.pose().pushPose();
      g.pose().translate(centro, y, 0.0F);
      g.pose().scale(escala, escala, 1.0F);
      g.drawString(this.font, texto, -this.font.width(texto) / 2, 0, color, false);
      g.pose().popPose();
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
   }

   private static class BotonBoveda extends AbstractButton {
      private final Runnable accion;

      BotonBoveda(int x, int y, int ancho, int alto, String texto, Runnable accion) {
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
         g.fill(x, y, x + ancho, y + alto, this.isHoveredOrFocused() ? -2045697 : -4944656);
         g.fill(x + 1, y + 1, x + ancho - 1, y + alto - 1, this.isHoveredOrFocused() ? -13747606 : -14932920);
         Font font = Minecraft.getInstance().font;
         float escala = Math.min(1.0F, (float)(ancho - 4) / font.width(this.getMessage()));
         g.pose().pushPose();
         g.pose().translate(x + ancho / 2.0F, y + alto / 2.0F, 0.0F);
         g.pose().scale(escala, escala, 1.0F);
         g.drawString(font, this.getMessage(), -font.width(this.getMessage()) / 2, -4, -1, true);
         g.pose().popPose();
      }

      protected void updateWidgetNarration(NarrationElementOutput salida) {
         this.defaultButtonNarrationText(salida);
      }
   }
}
