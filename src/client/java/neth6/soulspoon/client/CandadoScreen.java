package neth6.soulspoon.client;

import neth6.soulspoon.candados.CandadoCodigoPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class CandadoScreen extends Screen {
   private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/gui/candado_teclado.png");
   private static final int ANCHO = 228;
   private static final int ALTO = 268;
   public static final int LARGO_CODIGO = 8;
   private static final int TECLA = 30;
   private static final int[] COLUMNAS = new int[]{72, 115, 159};
   private static final int[] FILAS = new int[]{95, 138, 183, 226};
   private static final int CERRAR_X = 182;
   private static final int CERRAR_Y = 42;
   private static final int CERRAR_LADO = 19;
   private static final int NARANJA = -24004;
   private static final int ROJO = -43691;
   private static final int BLANCO = -1513240;
   private final BlockPos pos;
   private final boolean cerrar;
   private final String aviso;
   private final StringBuilder codigo = new StringBuilder();
   private int teclaApretada = -1;
   private int brillo;
   private float escala = 1.0F;
   private int izquierda;
   private int arriba;

   public CandadoScreen(BlockPos pos, boolean cerrar, String aviso) {
      super(Component.literal(cerrar ? "Elige tu código" : "Introduce tu código"));
      this.pos = pos;
      this.cerrar = cerrar;
      this.aviso = aviso;
   }

   protected void init() {
      this.escala = Math.min(1.0F, (this.height - 8) / 268.0F);
      this.izquierda = Math.round((this.width - 228.0F * this.escala) / 2.0F);
      this.arriba = Math.round((this.height - 268.0F * this.escala) / 2.0F);
   }

   private int teclaEn(double raton, double ratonY) {
      for (int i = 0; i < 12; i++) {
         int x = COLUMNAS[i % 3] - 15;
         int y = FILAS[i / 3] - 15;
         if (this.dentro(raton, ratonY, x, y, 30, 30)) {
            return i;
         }
      }

      return -1;
   }

   private boolean dentro(double raton, double ratonY, int x, int y, int ancho, int alto) {
      double rx = (raton - this.izquierda) / this.escala;
      double ry = (ratonY - this.arriba) / this.escala;
      return rx >= x && rx < x + ancho && ry >= y && ry < y + alto;
   }

   public boolean mouseClicked(double raton, double ratonY, int boton) {
      if (boton == 0) {
         if (this.dentro(raton, ratonY, 182, 42, 19, 19)) {
            this.sonido(0.8F);
            this.onClose();
            return true;
         }

         int tecla = this.teclaEn(raton, ratonY);
         if (tecla >= 0) {
            this.apretar(tecla);
            return true;
         }
      }

      return super.mouseClicked(raton, ratonY, boton);
   }

   private void apretar(int tecla) {
      this.teclaApretada = tecla;
      this.brillo = 3;
      if (tecla == 9) {
         this.borrar();
      } else if (tecla == 11) {
         this.enviar();
      } else {
         this.agregar(tecla == 10 ? '0' : (char)(49 + tecla));
      }
   }

   private void agregar(char numero) {
      if (this.codigo.length() < 8) {
         this.codigo.append(numero);
         this.sonido(1.2F + this.codigo.length() * 0.05F);
      }
   }

   private void borrar() {
      if (!this.codigo.isEmpty()) {
         this.codigo.setLength(this.codigo.length() - 1);
         this.sonido(0.7F);
      }
   }

   private void enviar() {
      if (this.codigo.isEmpty()) {
         this.sonido(0.5F);
      } else {
         this.sonido(1.6F);
         ClientPlayNetworking.send(new CandadoCodigoPayload(this.pos, this.cerrar, this.codigo.toString()));
         this.onClose();
      }
   }

   private void sonido(float tono) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), tono));
   }

   public boolean keyPressed(int tecla, int codigoEscaneo, int modificadores) {
      if (tecla >= 48 && tecla <= 57) {
         this.apretar(tecla == 48 ? 10 : tecla - 49);
         return true;
      }

      if (tecla >= 320 && tecla <= 329) {
         this.apretar(tecla == 320 ? 10 : tecla - 321);
         return true;
      }

      if (tecla == 259) {
         this.apretar(9);
         return true;
      }

      if (tecla != 257 && tecla != 335) {
         return super.keyPressed(tecla, codigoEscaneo, modificadores);
      }

      this.apretar(11);
      return true;
   }

   public void tick() {
      if (this.brillo > 0 && --this.brillo == 0) {
         this.teclaApretada = -1;
      }
   }

   public void render(GuiGraphics graphics, int raton, int ratonY, float parcial) {
      super.render(graphics, raton, ratonY, parcial);
      PoseStack pose = graphics.pose();
      pose.pushPose();
      pose.translate(this.izquierda, this.arriba, 0.0F);
      pose.scale(this.escala, this.escala, 1.0F);
      graphics.blit(TEXTURA, 0, 0, 0.0F, 0.0F, 228, 268, 228, 268);
      Component titulo = (Component)(this.aviso.isEmpty() ? this.title : Component.literal(this.aviso));
      graphics.drawCenteredString(this.font, titulo, 114, 14, this.aviso.isEmpty() ? -1513240 : -43691);
      if (!this.codigo.isEmpty()) {
         int mitad = this.font.width(this.codigo.toString()) / 2 + 3;
         graphics.fill(114 - mitad, 65, 114 + mitad, 77, -16052976);
         graphics.drawCenteredString(this.font, this.codigo.toString(), 114, 67, -24004);
      }

      if (this.teclaApretada >= 0) {
         this.marcar(graphics, this.teclaApretada, 1358954495);
      }

      int encima = this.teclaEn(raton, ratonY);
      if (encima >= 0 && encima != this.teclaApretada) {
         this.marcar(graphics, encima, 687865855);
      }

      pose.popPose();
   }

   private void marcar(GuiGraphics graphics, int tecla, int color) {
      int x = COLUMNAS[tecla % 3] - 15;
      int y = FILAS[tecla / 3] - 15;
      graphics.fill(x, y, x + 30, y + 30, color);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
