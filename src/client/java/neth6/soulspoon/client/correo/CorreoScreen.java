package neth6.soulspoon.client.correo;

import neth6.soulspoon.client.cajero.BotonCajero;
import neth6.soulspoon.client.cajero.EstiloCajero;
import neth6.soulspoon.correo.Correo;
import neth6.soulspoon.correo.CorreoMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

public class CorreoScreen extends AbstractContainerScreen<CorreoMenu> {
   private EditBox para;
   private MultiLineEditBox texto;
   private String error = "";

   public CorreoScreen(CorreoMenu menu, Inventory inventario, Component titulo) {
      super(menu, inventario, titulo);
      this.imageWidth = 190;
      this.imageHeight = 262;
   }

   public void error(String error) {
      this.error = error;
   }

   protected void init() {
      super.init();
      int l = this.leftPos;
      int t = this.topPos;
      this.addRenderableWidget(new BotonCajero(l + 8, t + 8, 46, 14, "<< ATRÁS", () -> {
         this.minecraft.player.closeContainer();
         this.minecraft.setScreen(new BuzonScreen());
      }));
      int yBoton = t + 102 + 54 + 3;
      int xBoton = l + 14 + 162 - 50;
      if (!((CorreoMenu)this.menu).leyendo()) {
         this.para = new EditBox(this.font, l + 44, t + 27, 132, 12, Component.literal("Para"));
         this.para.setMaxLength(16);
         this.para.setHint(Component.literal("nombre del jugador").withColor(-10854288));
         this.addRenderableWidget(this.para);
         this.texto = new MultiLineEditBox(
            this.font, l + 14, t + 54, 162, 42, Component.literal("Escribe tu mensaje...").withColor(-10854288), Component.literal("Mensaje")
         );
         this.texto.setValueListener(v -> {
            if (v.length() > 500) {
               this.texto.setValue(v.substring(0, 500));
            }
         });
         this.addRenderableWidget(this.texto);
         this.addRenderableWidget(new BotonCajero(xBoton, yBoton, 50, 13, "ENVIAR", () -> {
            this.error = "";
            ClientPlayNetworking.send(new Correo.EnviarPayload(this.para.getValue(), this.texto.getValue()));
         }));
         this.setInitialFocus(this.para);
      }
   }

   protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
      int l = this.leftPos;
      int t = this.topPos;
      EstiloCajero.marco(g, this.font, l, t, this.imageWidth, this.imageHeight, null);

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 9; col++) {
            int x = l + 14 + col * 18;
            int y = t + 102 + fila * 18;
            int color = EstiloCajero.arcoiris(0.16F + col / 9.0F * 0.85F);
            g.fill(x, y, x + 18 + 1, y + 18 + 1, color);
            g.fill(x + 1, y + 1, x + 18, y + 18, -16052974);
         }
      }

      for (int fila = 0; fila < 4; fila++) {
         for (int col = 0; col < 9; col++) {
            int x = l + 15 - 1 + col * 18;
            int y = t + 180 - 1 + (fila < 3 ? fila * 18 : 58);
            g.fill(x, y, x + 18, y + 18, -14012872);
            g.fill(x + 1, y + 1, x + 17, y + 17, -15394783);
         }
      }
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2 + 12, 11, -1, false);
      if (((CorreoMenu)this.menu).leyendo()) {
         Correo.CartaVista c = CorreoCliente.carta(((CorreoMenu)this.menu).carta());
         if (c == null) {
            return;
         }

         g.drawString(this.font, "De: " + c.autor(), 14, 28, -1, false);
         int y = 40;
         if (!c.asunto().isEmpty()) {
            g.drawString(this.font, c.asunto(), 14, 38, -11141121, false);
            y = 50;
         }

         for (FormattedCharSequence linea : this.font.split(Component.literal(c.texto()), 162)) {
            if (y > 92) {
               break;
            }

            g.drawString(this.font, linea, 14, y, -2564892, false);
            y += 9;
         }
      } else {
         g.drawString(this.font, "Para:", 14, 29, -4143408, false);
         g.drawString(this.font, "Mensaje:", 14, 44, -4143408, false);
         if (!this.error.isEmpty()) {
            int y = 161;
            g.drawString(this.font, this.font.plainSubstrByWidth(this.error, 105), 14, y, -40864, false);
         }
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.render(g, mouseX, mouseY, delta);
      this.renderTooltip(g, mouseX, mouseY);
   }

   public boolean keyPressed(int tecla, int scan, int mods) {
      if (tecla != 256) {
         if (this.para != null && this.para.isFocused()) {
            return this.para.keyPressed(tecla, scan, mods) || this.para.canConsumeInput();
         }

         if (this.texto != null && this.texto.isFocused()) {
            if (!this.texto.keyPressed(tecla, scan, mods)) {
            }

            return true;
         }
      }

      return super.keyPressed(tecla, scan, mods);
   }
}
