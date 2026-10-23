package neth6.soulspoon.client;

import neth6.soulspoon.hermandad.ManuscritoAccionPayload;
import neth6.soulspoon.hermandad.ManuscritoDatos;
import neth6.soulspoon.item.ManuscritoHermandadItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ManuscritoScreen extends Screen {
   private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/gui/manuscrito.png");
   private static final int ANCHO = 148;
   private static final int ALTO = 182;
   private static final int ANCHO_BOTON = 88;
   private static final int ALTO_BOTON = 20;
   private static final int SEPARACION = 6;
   private static final int NEGRO = 0;
   private final InteractionHand mano;
   private String nombre;
   private int ticks;
   private Button inscribirse;
   private Button crear;

   public ManuscritoScreen(InteractionHand mano, ItemStack manuscrito) {
      super(Component.literal("Manuscrito de Hermandad"));
      this.mano = mano;
      this.nombre = ManuscritoDatos.nombre(manuscrito);
   }

   private ItemStack manuscrito() {
      return this.minecraft.player.getItemInHand(this.mano);
   }

   private int x() {
      return (this.width - 148) / 2;
   }

   private int y() {
      return (this.height - 182 - 6 - 20) / 2;
   }

   protected void init() {
      int yBotones = this.y() + 182 + 6;
      int xBotones = (this.width - 176 - 6) / 2;
      this.inscribirse = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Inscribirse"), b -> this.enviar(1)).bounds(xBotones, yBotones, 88, 20).build()
      );
      this.crear = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Crear Hermandad"), b -> this.enviar(2)).bounds(xBotones + 88 + 6, yBotones, 88, 20).build()
      );
      this.actualizarBotones();
   }

   private void enviar(int accion) {
      this.setFocused(null);
      ClientPlayNetworking.send(new ManuscritoAccionPayload(accion, this.nombre, this.mano == InteractionHand.MAIN_HAND));
   }

   private boolean sellado() {
      return ManuscritoDatos.sellado(this.manuscrito());
   }

   private void actualizarBotones() {
      ItemStack stack = this.manuscrito();
      this.inscribirse.visible = this.crear.visible = !ManuscritoDatos.sellado(stack);
      List<ManuscritoDatos.Inscrito> inscritos = ManuscritoDatos.inscritos(stack);
      boolean yoInscrito = ManuscritoDatos.estaInscrito(stack, this.minecraft.player.getUUID());
      this.inscribirse.active = !yoInscrito && inscritos.size() < 10;
      this.crear.active = inscritos.size() >= ManuscritoHermandadItem.minimoInscritos(stack)
         && yoInscrito
         && !ManuscritoDatos.limpiarNombre(this.nombre).isEmpty();
   }

   public void tick() {
      this.ticks++;
      if (!ManuscritoHermandadItem.es(this.manuscrito())) {
         this.minecraft.setScreen(null);
      } else {
         this.actualizarBotones();
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      int x = this.x();
      int y = this.y();
      graphics.blit(TEXTURA, x, y, 0.0F, 0.0F, 148, 182, 256, 256);
      int xTexto = x + 20;
      String cursor = !this.sellado() && this.ticks / 6 % 2 == 0 ? "_" : "";
      graphics.drawString(this.font, this.nombre + cursor, xTexto, y + 22, 0, false);
      graphics.drawString(this.font, "Inscritos:", xTexto, y + 40, 0, false);
      int yInscrito = y + 54;

      for (ManuscritoDatos.Inscrito inscrito : ManuscritoDatos.inscritos(this.manuscrito())) {
         graphics.drawString(this.font, inscrito.nombre(), xTexto, yInscrito, 0, false);
         yInscrito += 11;
      }
   }

   public boolean charTyped(char caracter, int modificadores) {
      if (!this.sellado() && StringUtil.isAllowedChatCharacter(caracter) && this.nombre.length() < 16) {
         this.nombre = this.nombre + caracter;
         return true;
      } else {
         return super.charTyped(caracter, modificadores);
      }
   }

   public boolean keyPressed(int tecla, int scanCode, int modificadores) {
      if (tecla == 259 && !this.sellado() && !this.nombre.isEmpty()) {
         this.nombre = this.nombre.substring(0, this.nombre.length() - 1);
         return true;
      } else {
         return super.keyPressed(tecla, scanCode, modificadores);
      }
   }

   public void removed() {
      if (ManuscritoHermandadItem.es(this.manuscrito()) && !this.sellado()) {
         this.enviar(0);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
