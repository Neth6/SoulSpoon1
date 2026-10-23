package neth6.soulspoon.client;

import neth6.soulspoon.client.cajero.EstiloCajero;
import neth6.soulspoon.hermandad.HermandadAccionPayload;
import neth6.soulspoon.hermandad.HermandadChatPayload;
import neth6.soulspoon.hermandad.HermandadInfoPayload;
import neth6.soulspoon.hermandad.HermandadTablonPayload;
import neth6.soulspoon.marcas.MarcasPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public class HermandadScreen extends Screen {
   private static final ResourceLocation ICONO_DEDITA = ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/gui/dedita.png");
   private static final Component PROXIMAMENTE = Component.literal("Próximamente");
   private static final int ANCHO = 388;
   private static final int ALTO = 232;
   private static final int TAB = 22;
   private static final int FILA = 15;
   private static final int INICIO = 0;
   private static final int TABLON = 1;
   private static final int BRUJULA = 2;
   private static final int ESTANDARTES = 3;
   private static final int GESTION = 4;
   private static final int LISTA = 5;
   private static final String[] NOMBRES_TABS = new String[]{"Inicio", "Tablones", "Ubicaciones", "Estandartes", "Gestión"};
   private static final int MAXIMO_UBICACIONES = 16;
   private static final int METAL = -6645082;
   private static final int METAL_CLARO = -3618606;
   private static final int METAL_OSCURO = -10592664;
   private static final int BORDE = -15461352;
   private static final int FONDO = -14803421;
   private static final int PANEL = -14145490;
   private static final int PANEL_BORDE = -11908524;
   private static final int TEXTO = -1644826;
   private static final int TEXTO_GRIS = -5723984;
   private static final int CELESTE = -8408321;
   private static final int ORO = -22016;
   private static final int AZUL = -11162881;
   private static int pestana = 0;
   private HermandadInfoPayload info;
   private ItemStack[] iconosTabs;
   private EditBox invitarNombre;
   private EditBox chatEntrada;
   private int chatScroll;
   private int listaScroll;
   private int tablonScroll;
   private EditBox anuncioEntrada;
   private int estandarteElegido = -1;
   private static int escalaMapa = 1;
   private boolean hayElegido;
   private int elegidoX;
   private int elegidoY;
   private int elegidoZ;
   private int colorUbicacion = 16777045;
   private EditBox ubicacionNombre;
   private long confirmarIrse;
   private static final int MAPA_W = 358;
   private static final int MAPA_H = 148;
   private static final int CELDA_ESTANDARTE = 24;
   private static final int[] PALETA = new int[]{
      0, 170, 43520, 43690, 11141120, 11141290, 16755200, 11184810, 5592405, 5592575, 5635925, 5636095, 16733525, 16733695, 16777045, 16777215
   };
   private static final int CELDA_PALETA = 11;
   private boolean paletaAbierta;
   private static final int ALTO_LINEA = 10;
   private static final int HORA = -8408321;
   private static final int AUTOR = -11162881;
   private static final int AVISO = -8408321;
   private static final int BARRA = -11567152;
   private static final int FILA_LISTA = 20;
   private static final int ARRIBA_LISTA = 66;
   private static final String[] CORONA = new String[]{"X..X..X", "XX.X.XX", "XXXXXXX", "XXXXXXX", ".XXXXX."};
   private static final String[] PERSONA = new String[]{".XXX.", ".XXX.", "..X..", "XXXXX", "XXXXX", "XXXXX"};
   private static final String[] ESTRELLA = new String[]{"..X..", "..X..", "XXXXX", ".XXX.", ".X.X.", "X...X"};
   private static final int LIDER = -3703312;
   private static final int ARRIBA_TABLON = 50;
   private static final int ABAJO_TABLON = 196;

   public HermandadScreen(HermandadInfoPayload info) {
      super(Component.literal("Hermandad"));
      this.info = info;
   }

   public void actualizar(HermandadInfoPayload nueva) {
      String escrito = this.invitarNombre != null ? this.invitarNombre.getValue() : "";
      String chateando = this.chatEntrada != null ? this.chatEntrada.getValue() : "";
      boolean conFoco = this.chatEntrada != null && this.getFocused() == this.chatEntrada;
      this.info = nueva;
      this.rebuildWidgets();
      if (this.invitarNombre != null) {
         this.invitarNombre.setValue(escrito);
      }

      if (this.chatEntrada != null) {
         this.chatEntrada.setValue(chateando);
         if (conFoco) {
            this.setFocused(this.chatEntrada);
         }
      }
   }

   private int x() {
      return (this.width - 388) / 2;
   }

   private int y() {
      return (this.height - 232) / 2;
   }

   private boolean soyMaestro() {
      return this.info.maestro().equals(this.minecraft.player.getUUID());
   }

   private boolean puedoPublicar() {
      return this.soyMaestro() || this.info.miembros().stream().anyMatch(m -> m.uuid().equals(this.minecraft.player.getUUID()) && m.lider());
   }

   protected void init() {
      ItemStack cabeza = new ItemStack(Items.PLAYER_HEAD);
      cabeza.set(DataComponents.PROFILE, new ResolvableProfile(this.minecraft.player.getGameProfile()));
      this.iconosTabs = new ItemStack[]{
         new ItemStack(Items.BOOK), new ItemStack(Items.OAK_SIGN), new ItemStack(Items.COMPASS), new ItemStack(Items.WHITE_BANNER), cabeza
      };
      this.invitarNombre = null;
      this.chatEntrada = null;
      this.anuncioEntrada = null;
      int x = this.x();
      int y = this.y();
      switch (pestana) {
         case 0:
            this.initInicio(x, y);
            break;
         case 1:
            this.initTablon(x, y);
            break;
         case 2:
            this.initUbicaciones(x, y);
            break;
         case 3:
            this.initEstandartes(x, y);
            break;
         case 4:
            this.initGestion(x, y);
      }
   }

   private void initInicio(int x, int y) {
      this.chatEntrada = (EditBox)this.addRenderableWidget(new EditBox(this.font, x + 18, y + 211, 216, 10, Component.literal("Chat de la Hermandad")) {
         public boolean keyPressed(int tecla, int scanCode, int modificadores) {
            if (tecla != 257 && tecla != 335) {
               return super.keyPressed(tecla, scanCode, modificadores);
            }

            String mensaje = this.getValue().strip();
            if (!mensaje.isEmpty()) {
               ClientPlayNetworking.send(new HermandadAccionPayload(4, mensaje));
               this.setValue("");
               HermandadScreen.this.chatScroll = 0;
            }

            return true;
         }
      });
      this.chatEntrada.setBordered(false);
      this.chatEntrada.setMaxLength(256);
      this.chatEntrada.setTextColor(-1644826);
      this.chatEntrada.setHint(Component.literal("Escribe a tu Hermandad...").withColor(-9408392));
   }

   private void initUbicaciones(int x, int y) {
      int centro = x + 194;
      String escrito = this.ubicacionNombre != null ? this.ubicacionNombre.getValue() : "";
      Button aqui = (Button)this.addRenderableWidget(Button.builder(Component.literal("⌖"), b -> {
         this.hayElegido = true;
         this.elegidoX = this.minecraft.player.getBlockX();
         this.elegidoY = this.minecraft.player.getBlockY();
         this.elegidoZ = this.minecraft.player.getBlockZ();
         this.rebuildWidgets();
      }).bounds(centro - 88, y + 202, 16, 16).build());
      aqui.setTooltip(Tooltip.create(Component.literal("Elegir donde estás parado")));
      this.ubicacionNombre = (EditBox)this.addRenderableWidget(new EditBox(this.font, centro - 68, y + 202, 108, 16, Component.literal("Ubicación")));
      this.ubicacionNombre.setMaxLength(24);
      this.ubicacionNombre.setHint(Component.literal("Nombre ubicación...").withColor(-9408392));
      this.ubicacionNombre.setValue(escrito);
      Button guardar = (Button)this.addRenderableWidget(
         Button.builder(
               Component.literal("Guardar"),
               b -> {
                  String nombre = this.ubicacionNombre.getValue().replace(";", " ").strip();
                  ClientPlayNetworking.send(
                     new HermandadAccionPayload(17, String.format("%06x;%d;%d;%d;%s", this.colorUbicacion, this.elegidoX, this.elegidoY, this.elegidoZ, nombre))
                  );
                  this.hayElegido = false;
                  this.ubicacionNombre.setValue("");
                  this.rebuildWidgets();
               }
            )
            .bounds(centro + 44, y + 201, 70, 18)
            .build()
      );
      guardar.active = this.hayElegido && HermandadesCliente.ubicaciones().size() < 16;
      if (!this.hayElegido) {
         guardar.setTooltip(Tooltip.create(Component.literal("Haz click en el mapa (o en ⌖) para elegir el lugar")));
      }

      MapaHermandad.olvidar();
   }

   private int mapaX() {
      return this.x() + 15;
   }

   private int mapaY() {
      return this.y() + 47;
   }

   private int[] aMapa(int bx, int bz) {
      return new int[]{
         this.mapaX() + 179 + Math.floorDiv(bx - this.minecraft.player.getBlockX(), escalaMapa),
         this.mapaY() + 74 + Math.floorDiv(bz - this.minecraft.player.getBlockZ(), escalaMapa)
      };
   }

   private boolean enMapa(double mx, double my) {
      return dentro(mx, my, this.mapaX(), this.mapaY(), 358, 148);
   }

   private int ubicacionEn(double mx, double my) {
      if (!this.enMapa(mx, my)) {
         return -1;
      }

      String dim = this.minecraft.level.dimension().location().toString();
      List<MarcasPayload.Marca> lista = HermandadesCliente.ubicaciones();

      for (int i = lista.size() - 1; i >= 0; i--) {
         MarcasPayload.Marca u = lista.get(i);
         if (u.dimension().equals(dim)) {
            int[] p = this.aMapa(u.x(), u.z());
            if (Math.abs(mx - p[0]) <= 4.0 && Math.abs(my - p[1]) <= 4.0) {
               return i;
            }
         }
      }

      return -1;
   }

   private static String nombreDimension(String id) {
      return switch (id) {
         case "minecraft:overworld" -> "Mundo normal";
         case "minecraft:the_nether" -> "Nether";
         case "minecraft:the_end" -> "End";
         case "soulspoon:dimension_nueva", "soulspoon:centro_quiu" -> "Quiu";
         case "soulspoon:organos", "soulspoon:gleba" -> "Gleba";
         case "soulspoon:limbo" -> "El Limbo";
         case "soulspoon:espacio" -> "El Espacio";
         default -> id.substring(id.indexOf(58) + 1);
      };
   }

   private void initGestion(int x, int y) {
      this.invitarNombre = (EditBox)this.addRenderableWidget(new EditBox(this.font, x + 16, y + 111, 122, 16, Component.literal("Invitar jugador")));
      this.invitarNombre.setMaxLength(16);
      this.invitarNombre.setHint(Component.literal("Nombre del jugador...").withColor(-9408392));
      Button invitar = (Button)this.addRenderableWidget(Button.builder(Component.literal("Invitar"), b -> {
         String nombre = this.invitarNombre.getValue().strip();
         if (!nombre.isEmpty()) {
            ClientPlayNetworking.send(new HermandadAccionPayload(1, nombre));
            this.invitarNombre.setValue("");
         }
      }).bounds(x + 142, y + 110, 60, 18).build());
      invitar.active = this.soyMaestro();
      this.invitarNombre.setEditable(this.soyMaestro());
      EditBox motd = (EditBox)this.addRenderableWidget(new EditBox(this.font, x + 16, y + 147, 92, 16, Component.literal("MOTD")));
      motd.setHint(Component.literal("Mensaje...").withColor(-9408392));
      motd.setEditable(false);
      this.inactivo(Button.builder(Component.literal("Fijar"), b -> {}).bounds(x + 112, y + 146, 44, 18).build());
      this.inactivo(Button.builder(Component.literal("Limpiar"), b -> {}).bounds(x + 158, y + 146, 44, 18).build());
      this.inactivo(Button.builder(Component.literal("Solicitudes de unión: Cerradas"), b -> {}).bounds(x + 20, y + 170, 180, 18).build());
      boolean maestro = this.soyMaestro();
      String texto = maestro ? "Disolver Hermandad" : "Salir de la Hermandad";
      Button irse = Button.builder(Component.literal(texto), b -> {
         if (this.confirmarIrse != 0L && Util.getMillis() - this.confirmarIrse <= 3000L) {
            this.confirmarIrse = 0L;
            ClientPlayNetworking.send(new HermandadAccionPayload(maestro ? 15 : 16, ""));
            this.onClose();
         } else {
            this.confirmarIrse = Util.getMillis();
            b.setMessage(Component.literal("¿Seguro? Aprieta otra vez").withStyle(ChatFormatting.RED));
         }
      }).bounds(x + 44, y + 200, 132, 18).build();
      irse.setTooltip(
         Tooltip.create(Component.literal(maestro ? "Borra la Hermandad para todos (lo que hay en la Bóveda se pierde)." : "Te vas de la Hermandad."))
      );
      this.addRenderableWidget(irse);
      if (this.soyMaestro()) {
         int derecha = x + 388 - 14;
         List<HermandadInfoPayload.Miembro> miembros = this.info.miembros();

         for (int i = 0; i < miembros.size(); i++) {
            HermandadInfoPayload.Miembro m = miembros.get(i);
            if (!m.uuid().equals(this.info.maestro())) {
               int yFila = y + 48 + i * 15 + 2;
               Button rango = (Button)this.addRenderableWidget(
                  Button.builder(
                        Component.literal(m.lider() ? "v" : "^"),
                        b -> ClientPlayNetworking.send(new HermandadAccionPayload(m.lider() ? 14 : 13, m.uuid().toString()))
                     )
                     .bounds(derecha - 30, yFila, 13, 11)
                     .build()
               );
               rango.setTooltip(
                  Tooltip.create(Component.literal(m.lider() ? "Bajar a Miembro a " + m.nombre() : "Subir de rango a " + m.nombre() + " (Líder)"))
               );
               Button expulsar = (Button)this.addRenderableWidget(
                  Button.builder(Component.literal("x"), b -> ClientPlayNetworking.send(new HermandadAccionPayload(2, m.uuid().toString())))
                     .bounds(derecha - 15, yFila, 13, 11)
                     .build()
               );
               expulsar.setTooltip(Tooltip.create(Component.literal("Expulsar a " + m.nombre())));
            }
         }
      }
   }

   private void initEstandartes(int x, int y) {
      int centro = x + 194;
      boolean maestro = this.soyMaestro();
      if (this.estandarteElegido >= HermandadesCliente.coleccion().size()) {
         this.estandarteElegido = -1;
      }

      boolean hayElegido = this.estandarteElegido >= 0;
      Tooltip soloMaestro = Tooltip.create(Component.literal("Solo el Maestro"));
      Button activar = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Activar"), b -> this.accionEstandarte(7)).bounds(centro - 95, y + 176, 58, 16).build()
      );
      Button recuperar = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Recuperar"), b -> this.accionEstandarte(8)).bounds(centro - 29, y + 176, 58, 16).build()
      );
      Button borrar = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Borrar"), b -> this.accionEstandarte(9)).bounds(centro + 37, y + 176, 58, 16).build()
      );
      Button quitar = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Quitar Activo"), b -> ClientPlayNetworking.send(new HermandadAccionPayload(10, "")))
            .bounds(x + 20, y + 202, 108, 18)
            .build()
      );
      Button guardar = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Guardar Mano"), b -> ClientPlayNetworking.send(new HermandadAccionPayload(6, "")))
            .bounds(x + 388 - 128, y + 202, 108, 18)
            .build()
      );
      activar.active = maestro && hayElegido;
      recuperar.active = maestro && hayElegido;
      borrar.active = maestro && hayElegido;
      quitar.active = maestro && HermandadesCliente.activo() >= 0;
      guardar.active = this.minecraft.player.getMainHandItem().getItem() instanceof BannerItem;
      if (!maestro) {
         activar.setTooltip(soloMaestro);
         recuperar.setTooltip(soloMaestro);
         borrar.setTooltip(soloMaestro);
         quitar.setTooltip(soloMaestro);
      }

      if (!guardar.active) {
         guardar.setTooltip(Tooltip.create(Component.literal("Ten un estandarte en la mano")));
      }
   }

   private void accionEstandarte(int accion) {
      if (this.estandarteElegido >= 0) {
         ClientPlayNetworking.send(new HermandadAccionPayload(accion, Integer.toString(this.estandarteElegido)));
         if (accion != 7) {
            this.estandarteElegido = -1;
         }
      }
   }

   public void refrescarEstandartes() {
      if (pestana == 3) {
         this.rebuildWidgets();
      }
   }

   private int x0Estandartes() {
      return this.x() + 86;
   }

   private int y0Estandartes() {
      return this.y() + 68;
   }

   private int lugarEstandarte(double mx, double my) {
      int x0 = this.x0Estandartes();
      int y0 = this.y0Estandartes();
      return !dentro(mx, my, x0, y0, 216, 72) ? -1 : (int)(my - y0) / 24 * 9 + (int)(mx - x0) / 24;
   }

   private void inactivo(Button boton) {
      boton.active = false;
      boton.setTooltip(Tooltip.create(PROXIMAMENTE));
      this.addRenderableWidget(boton);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      if (this.paletaAbierta) {
         this.dibujarPaleta(g, mouseX, mouseY);
      } else {
         this.tooltipTabs(g, this.x(), this.y(), mouseX, mouseY);
      }

      if (pestana == 2 && !this.paletaAbierta) {
         this.tooltipUbicaciones(g, mouseX, mouseY);
      }

      if (pestana == 3 && !this.paletaAbierta) {
         int lugar = this.lugarEstandarte(mouseX, mouseY);
         if (lugar >= 0 && lugar < HermandadesCliente.coleccion().size()) {
            g.renderTooltip(this.font, HermandadesCliente.coleccion().get(lugar), mouseX, mouseY);
         }
      }
   }

   private int xCuadro() {
      return this.x() + 150 + this.font.width(this.info.nombre()) + 4;
   }

   private int xPaleta() {
      return this.xCuadro() + 4 - 44;
   }

   private int yPaleta() {
      return this.y() + 26;
   }

   private void dibujarPaleta(GuiGraphics g, int mouseX, int mouseY) {
      int px = this.xPaleta();
      int py = this.yPaleta();
      g.pose().pushPose();
      g.pose().translate(0.0F, 0.0F, 400.0F);
      caja(g, px - 3, py - 3, px + 88 + 3, py + 22 + 3, -14013904, -15461352);

      for (int i = 0; i < PALETA.length; i++) {
         int cx = px + i % 8 * 11;
         int cy = py + i / 8 * 11;
         boolean elegido = PALETA[i] == this.info.color();
         boolean encima = dentro(mouseX, mouseY, cx, cy, 11, 11);
         caja(g, cx, cy, cx + 11 - 1, cy + 11 - 1, 0xFF000000 | PALETA[i], !elegido && !encima ? -15461352 : -1);
      }

      g.pose().popPose();
   }

   private boolean clicColor(double mouseX, double mouseY) {
      if (this.paletaAbierta) {
         int px = this.xPaleta();
         int py = this.yPaleta();

         for (int i = 0; i < PALETA.length; i++) {
            if (dentro(mouseX, mouseY, px + i % 8 * 11, py + i / 8 * 11, 11, 11)) {
               ClientPlayNetworking.send(new HermandadAccionPayload(3, Integer.toHexString(PALETA[i])));
            }
         }

         this.paletaAbierta = false;
         return true;
      } else if (this.soyMaestro() && dentro(mouseX, mouseY, this.xCuadro(), this.y() + 14, 8, 8)) {
         this.paletaAbierta = true;
         return true;
      } else {
         return false;
      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      int x = this.x();
      int y = this.y();
      this.dibujarMarco(g, x, y);
      this.dibujarTabs(g, x, y, mouseX, mouseY);
      switch (pestana) {
         case 0:
            this.dibujarInicio(g, x, y);
            break;
         case 1:
            this.dibujarTablon(g, x, y);
            break;
         case 2:
            this.dibujarUbicaciones(g, x, y);
            break;
         case 3:
            this.dibujarEstandartes(g, x, y);
            break;
         case 4:
            this.dibujarGestion(g, x, y);
            break;
         case 5:
            this.dibujarLista(g, x, y);
            break;
         default:
            g.drawCenteredString(this.font, "Próximamente", x + 194, y + 128, -5723984);
      }
   }

   private void dibujarMarco(GuiGraphics g, int x, int y) {
      caja(g, x, y, x + 388, y + 232, -6645082, -15461352);
      g.fill(x + 1, y + 1, x + 388 - 1, y + 2, -3618606);
      g.fill(x + 1, y + 1, x + 2, y + 232 - 1, -3618606);
      g.fill(x + 1, y + 232 - 2, x + 388 - 1, y + 232 - 1, -10592664);
      caja(g, x + 8, y + 40, x + 388 - 8, y + 232 - 6, -14803421, -15461352);
   }

   private void dibujarTabs(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
      for (int i = 0; i < this.iconosTabs.length; i++) {
         int tx = x + 8 + i * 26;
         int ty = y + 8;
         boolean encima = dentro(mouseX, mouseY, tx, ty, 22, 22);
         caja(g, tx, ty, tx + 22, ty + 22, i == pestana ? -2694414 : (encima ? -8750458 : -11184802), -15461352);
         g.renderItem(this.iconosTabs[i], tx + 3, ty + 3);
      }

      int cx = x + 388 - 8 - 22;
      boolean encimaCatalejo = dentro(mouseX, mouseY, cx, y + 8, 22, 22);
      caja(g, cx, y + 8, cx + 22, y + 8 + 22, pestana == 5 ? -2694414 : (encimaCatalejo ? -8750458 : -11184802), -15461352);
      g.renderItem(new ItemStack(Items.SPYGLASS), cx + 3, y + 11);
      int xNombre = x + 150;
      caja(g, xNombre - 4, y + 12, xNombre + this.font.width(this.info.nombre()) + 16, y + 25, -14013904, -15461352);
      g.drawString(this.font, this.info.nombre(), xNombre, y + 15, 0xFF000000 | this.info.color(), false);
      int xCuadro = this.xCuadro();
      boolean encimaCuadro = this.soyMaestro() && dentro(mouseX, mouseY, xCuadro, y + 14, 8, 8);
      caja(g, xCuadro, y + 14, xCuadro + 8, y + 22, 0xFF000000 | this.info.color(), encimaCuadro ? -1 : -15461352);
      if (pestana == 2) {
         String contador = HermandadesCliente.ubicaciones().size() + "/16";
         g.drawString(this.font, contador, x + 388 - 12 - this.font.width(contador), y + 31, -5723984, false);
      } else {
         long enLinea = this.info.miembros().stream().filter(m -> this.conectado(m) != null).count();
         g.drawString(this.font, "En línea: " + enLinea + "/" + this.info.miembros().size(), x + 246, y + 30, -5723984, false);
      }
   }

   private void dibujarUbicaciones(GuiGraphics g, int x, int y) {
      caja(g, x + 14, y + 46, x + 388 - 14, y + 196, -14145490, -11908524);
      int centro = x + 194;
      caja(g, centro - 108, y + 202, centro - 92, y + 218, 0xFF000000 | this.colorUbicacion, -11908524);
      int mx0 = this.mapaX();
      int my0 = this.mapaY();
      MapaHermandad.actualizar(358, 148, this.minecraft.player.getBlockX(), this.minecraft.player.getBlockZ(), escalaMapa);
      g.blit(MapaHermandad.TEXTURA, mx0, my0, 0.0F, 0.0F, 358, 148, 358, 148);
      g.enableScissor(mx0, my0, mx0 + 358, my0 + 148);
      String dim = this.minecraft.level.dimension().location().toString();
      int otras = 0;

      for (MarcasPayload.Marca u : HermandadesCliente.ubicaciones()) {
         if (!u.dimension().equals(dim)) {
            otras++;
         } else {
            int[] p = this.aMapa(u.x(), u.z());
            g.fill(p[0] - 3, p[1] - 3, p[0] + 4, p[1] + 4, -16777216);
            g.fill(p[0] - 2, p[1] - 2, p[0] + 3, p[1] + 3, 0xFF000000 | u.color());
            g.pose().pushPose();
            g.pose().translate(p[0], p[1] - 9, 0.0F);
            g.pose().scale(0.6F, 0.6F, 1.0F);
            g.drawCenteredString(this.font, u.nombre(), 0, 0, 0xFF000000 | u.color());
            g.pose().popPose();
         }
      }

      if (this.hayElegido) {
         int[] p = this.aMapa(this.elegidoX, this.elegidoZ);
         g.fill(p[0] - 5, p[1], p[0] + 6, p[1] + 1, -1);
         g.fill(p[0], p[1] - 5, p[0] + 1, p[1] + 6, -1);
      }

      int jx = mx0 + 179;
      int jz = my0 + 74;
      g.fill(jx - 2, jz - 2, jx + 3, jz + 3, -16777216);
      g.fill(jx - 1, jz - 1, jx + 2, jz + 2, -1);
      double giro = Math.toRadians(this.minecraft.player.getYRot());
      int fx = jx + (int)Math.round(-Math.sin(giro) * 5.0);
      int fz = jz + (int)Math.round(Math.cos(giro) * 5.0);
      g.fill(fx - 1, fz - 1, fx + 1, fz + 1, -1);
      g.disableScissor();
      String arriba = nombreDimension(dim)
         + " · 1 píxel = "
         + escalaMapa
         + (escalaMapa == 1 ? " bloque" : " bloques")
         + (otras > 0 ? " · +" + otras + " en otras dimensiones" : "");
      g.fill(mx0, my0, mx0 + this.font.width(arriba) / 2 + 6, my0 + 8, -1610612736);
      g.pose().pushPose();
      g.pose().translate(mx0 + 3, my0 + 2, 0.0F);
      g.pose().scale(0.5F, 0.5F, 1.0F);
      g.drawString(this.font, arriba, 0, 0, -1, false);
      g.pose().popPose();
   }

   private void tooltipUbicaciones(GuiGraphics g, int mouseX, int mouseY) {
      int i = this.ubicacionEn(mouseX, mouseY);
      if (i >= 0) {
         MarcasPayload.Marca u = HermandadesCliente.ubicaciones().get(i);
         g.renderComponentTooltip(
            this.font,
            List.of(
               Component.literal(u.nombre()).withColor(u.color()),
               Component.literal(u.x() + " " + u.y() + " " + u.z()).withColor(11053232),
               Component.literal("Click derecho: borrar").withColor(7368824)
            ),
            mouseX,
            mouseY
         );
      } else if (dentro(mouseX, mouseY, this.mapaX(), this.mapaY(), 179, 8)) {
         String dim = this.minecraft.level.dimension().location().toString();
         List<Component> lineas = new ArrayList<>();

         for (MarcasPayload.Marca u : HermandadesCliente.ubicaciones()) {
            if (!u.dimension().equals(dim)) {
               lineas.add(
                  Component.literal(u.nombre())
                     .withColor(u.color())
                     .append(Component.literal(" · " + nombreDimension(u.dimension()) + " (" + u.x() + " " + u.y() + " " + u.z() + ")").withColor(11053232))
               );
            }
         }

         if (!lineas.isEmpty()) {
            g.renderComponentTooltip(this.font, lineas, mouseX, mouseY);
         }
      } else {
         if (this.enMapa(mouseX, mouseY)) {
            g.renderTooltip(this.font, Component.literal("Click: elegir este lugar · Ruedita: zoom"), mouseX, mouseY);
         }
      }
   }

   private void tooltipTabs(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
      for (int i = 0; i < this.iconosTabs.length; i++) {
         if (dentro(mouseX, mouseY, x + 8 + i * 26, y + 8, 22, 22)) {
            g.renderTooltip(this.font, (Component)(NOMBRES_TABS[i] != null ? Component.literal(NOMBRES_TABS[i]) : PROXIMAMENTE), mouseX, mouseY);
         }
      }

      if (dentro(mouseX, mouseY, x + 388 - 8 - 22, y + 8, 22, 22)) {
         g.renderTooltip(this.font, Component.literal("Hermandades"), mouseX, mouseY);
      }
   }

   private void dibujarInicio(GuiGraphics g, int x, int y) {
      caja(g, x + 14, y + 46, x + 238, y + 76, -14145490, -11908524);
      String motd = this.info.motd().isEmpty() ? "Sin mensaje fijado." : this.info.motd();
      g.drawString(this.font, "MOTD: ", x + 20, y + 57, -8408321, false);
      g.drawString(this.font, motd, x + 20 + this.font.width("MOTD: "), y + 57, -1644826, false);
      caja(g, x + 14, y + 80, x + 238, y + 204, -14145490, -11908524);
      this.dibujarChat(g, x + 18, y + 83, 212, y + 201);
      caja(g, x + 14, y + 207, x + 238, y + 222, -15198180, -11908524);
      this.dibujarMiembros(g, x + 242, y + 46, x + 388 - 14, y + 222);
   }

   private List<FormattedCharSequence> renglonesChat(int ancho) {
      List<FormattedCharSequence> renglones = new ArrayList<>();

      for (HermandadChatPayload.Linea linea : HermandadesCliente.chat()) {
         MutableComponent texto = Component.literal("[" + linea.hora() + "] ").withColor(-8408321);
         if (linea.autor().isEmpty()) {
            texto.append(Component.literal(linea.texto()).withColor(-8408321));
         } else {
            texto.append(Component.literal(linea.autor()).withColor(-11162881)).append(Component.literal(": " + linea.texto()).withColor(-1644826));
         }

         renglones.addAll(this.font.split(texto, ancho));
      }

      return renglones;
   }

   private int renglonesVisibles(int y1, int y2) {
      return (y2 - y1) / 10;
   }

   private void dibujarChat(GuiGraphics g, int x1, int y1, int ancho, int y2) {
      List<FormattedCharSequence> renglones = this.renglonesChat(ancho - 6);
      int visibles = this.renglonesVisibles(y1, y2);
      int maximo = Math.max(0, renglones.size() - visibles);
      this.chatScroll = Math.max(0, Math.min(this.chatScroll, maximo));
      int desde = Math.max(0, renglones.size() - visibles - this.chatScroll);
      int hasta = Math.min(renglones.size(), desde + visibles);

      for (int i = desde; i < hasta; i++) {
         g.drawString(this.font, renglones.get(i), x1, y1 + (i - desde) * 10, -1644826, false);
      }

      if (renglones.isEmpty()) {
         g.drawString(this.font, "Todavía no hay mensajes.", x1, y1, -5723984, false);
      }

      if (maximo > 0) {
         int alto = y2 - y1;
         int barra = Math.max(8, alto * visibles / renglones.size());
         int yBarra = y1 + (alto - barra) * (maximo - this.chatScroll) / maximo;
         g.fill(x1 + ancho - 2, yBarra, x1 + ancho, yBarra + barra, -11567152);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      int x = this.x();
      int y = this.y();
      if (pestana == 0 && dentro(mouseX, mouseY, x + 14, y + 80, 224, 124)) {
         this.chatScroll += scrollY > 0.0 ? 1 : -1;
         return true;
      } else if (pestana == 5) {
         this.listaScroll += scrollY > 0.0 ? -1 : 1;
         return true;
      } else if (pestana == 2 && this.enMapa(mouseX, mouseY)) {
         escalaMapa = Math.max(1, Math.min(8, scrollY > 0.0 ? escalaMapa / 2 : escalaMapa * 2));
         MapaHermandad.olvidar();
         return true;
      } else if (pestana == 1) {
         this.tablonScroll += scrollY > 0.0 ? -1 : 1;
         this.refrescarTablon();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   private int filasVisiblesLista() {
      return 7;
   }

   private void dibujarLista(GuiGraphics g, int x, int y) {
      caja(g, x + 14, y + 46, x + 388 - 14, y + 232 - 10, -14145490, -11908524);
      g.drawCenteredString(this.font, "Hermandades", x + 194, y + 53, -8408321);
      List<Entry<String, Integer>> todas = new ArrayList<>(HermandadesCliente.todas().entrySet());
      int visibles = this.filasVisiblesLista();
      this.listaScroll = Math.max(0, Math.min(this.listaScroll, todas.size() - visibles));
      ItemStack blanco = new ItemStack(Items.WHITE_BANNER);

      for (int i = 0; i < visibles && this.listaScroll + i < todas.size(); i++) {
         Entry<String, Integer> hermandad = todas.get(this.listaScroll + i);
         ItemStack estandarte = HermandadesCliente.estandarteDeHermandad(hermandad.getKey());
         if (estandarte.isEmpty()) {
            estandarte = blanco;
         }

         int fy = y + 66 + i * 20;
         g.fill(x + 15, fy, x + 388 - 15, fy + 20, i % 2 == 0 ? -16777216 : -13948111);
         g.renderItem(estandarte, x + 20, fy + 2);
         g.drawString(this.font, hermandad.getKey(), x + 42, fy + 6, 0xFF000000 | hermandad.getValue(), false);
      }

      if (todas.isEmpty()) {
         g.drawCenteredString(this.font, "Todavía no hay Hermandades.", x + 194, y + 80, -5723984);
      }

      if (todas.size() > visibles) {
         int alto = visibles * 20;
         int barra = Math.max(8, alto * visibles / todas.size());
         int yBarra = y + 66 + (alto - barra) * this.listaScroll / (todas.size() - visibles);
         g.fill(x + 388 - 19, yBarra, x + 388 - 17, yBarra + barra, -11567152);
      }
   }

   private void dibujarGestion(GuiGraphics g, int x, int y) {
      caja(g, x + 14, y + 46, x + 204, y + 94, -14145490, -11908524);
      g.drawString(this.font, "Maestro: " + this.nombreMaestro(), x + 20, y + 52, -1644826, false);
      String balance = "Balance: ";
      g.drawString(this.font, balance, x + 20, y + 65, -1644826, false);
      long total = Math.max(0L, this.info.balance());
      long[] monedas = new long[]{total / 10000L, total / 100L % 100L, total % 100L};
      ResourceLocation[] iconos = new ResourceLocation[]{EstiloCajero.ICONO_ROJA, EstiloCajero.ICONO_VERDE, ICONO_DEDITA};
      int bx = x + 20 + this.font.width(balance);

      for (int i = 0; i < 3; i++) {
         String cantidad = Long.toString(monedas[i]);
         g.drawString(this.font, cantidad, bx, y + 65, -1644826, false);
         bx += this.font.width(cantidad) + 1;
         g.blit(iconos[i], bx, y + 64, 9, 9, 0.0F, 0.0F, 16, 16, 16, 16);
         bx += 13;
      }

      g.drawString(this.font, "Miembros: " + this.info.miembros().size(), x + 20, y + 78, -1644826, false);
      String banco = "Banco: Nvl " + this.info.nivelBanco();
      g.drawString(this.font, banco, x + 198 - this.font.width(banco), y + 78, -1644826, false);
      g.drawString(this.font, "Invitar jugador:", x + 16, y + 100, -1644826, false);
      g.drawString(this.font, "Establecer MOTD:", x + 16, y + 136, -1644826, false);
      this.dibujarMiembros(g, x + 208, y + 46, x + 388 - 14, y + 222);
   }

   private void dibujarEstandartes(GuiGraphics g, int x, int y) {
      caja(g, x + 14, y + 46, x + 388 - 14, y + 198, -14145490, -11908524);
      g.drawCenteredString(this.font, "Colección de Estandartes", x + 194, y + 54, -5723984);
      int celda = 24;
      int x0 = this.x0Estandartes();
      int y0 = this.y0Estandartes();
      List<ItemStack> coleccion = HermandadesCliente.coleccion();
      g.fill(x0 - 1, y0 - 1, x0 + celda * 9 + 1, y0 + celda * 3 + 1, -15724525);

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 9; col++) {
            int i = fila * 9 + col;
            int cx = x0 + col * celda;
            int cy = y0 + fila * celda;
            int borde = i == HermandadesCliente.activo() ? -22016 : (i == this.estandarteElegido ? -1 : 0);
            if (borde != 0) {
               g.fill(cx, cy, cx + celda, cy + celda, borde);
            }

            g.fill(cx + 1, cy + 1, cx + celda - 1, cy + celda - 1, -14474456);
            if (i < coleccion.size()) {
               g.renderItem(coleccion.get(i), cx + 4, cy + 4);
            }
         }
      }

      if (HermandadesCliente.activo() >= 0) {
         g.drawCenteredString(this.font, "El estandarte con borde dorado es la capa de todos los miembros.", x + 194, y0 + celda * 3 + 6, -5723984);
      }
   }

   private void dibujarMiembros(GuiGraphics g, int x1, int y1, int x2, int y2) {
      caja(g, x1, y1, x2, y2, -15198180, -11908524);
      List<HermandadInfoPayload.Miembro> miembros = this.info.miembros();

      for (int i = 0; i < miembros.size(); i++) {
         HermandadInfoPayload.Miembro m = miembros.get(i);
         int fy = y1 + 2 + i * 15;
         if (fy + 15 > y2) {
            break;
         }

         boolean maestro = m.uuid().equals(this.info.maestro());
         int color = maestro ? -22016 : -11162881;
         g.fill(x1 + 1, fy, x2 - 1, fy + 15, i % 2 == 0 ? -16777216 : -13948111);
         g.fill(x1 + 3, fy + 3, x1 + 4, fy + 15 - 3, color);
         PlayerInfo enLinea = this.conectado(m);
         caja(g, x1 + 6, fy + 1, x1 + 19, fy + 14, -16777216, enLinea != null ? -12799933 : -2606534);
         PlayerFaceRenderer.draw(g, enLinea != null ? enLinea.getSkin() : DefaultPlayerSkin.get(m.uuid()), x1 + 8, fy + 3, 9);
         pixelArt(g, maestro ? CORONA : (m.lider() ? ESTRELLA : PERSONA), x1 + 22, fy + 4, maestro ? -15841 : (m.lider() ? -3703312 : -11688193));
         g.drawString(this.font, m.nombre(), x1 + 31, fy + 4, color, false);
      }
   }

   private List<HermandadScreen.Renglones> armarTablon(int x, int y) {
      List<HermandadScreen.Renglones> visibles = new ArrayList<>();
      List<HermandadTablonPayload.Anuncio> anuncios = HermandadesCliente.anuncios();
      this.tablonScroll = Math.max(0, Math.min(this.tablonScroll, anuncios.size() - 1));
      int fy = y + 50;

      for (int i = this.tablonScroll; i < anuncios.size(); i++) {
         List<FormattedCharSequence> lineas = this.font.split(Component.literal(anuncios.get(i).texto()), 340);
         int alto = 12 + lineas.size() * 10 + 6;
         if (fy + alto > y + 196 && !visibles.isEmpty()) {
            break;
         }

         visibles.add(new HermandadScreen.Renglones(i, fy, lineas));
         fy += alto;
      }

      return visibles;
   }

   private void initTablon(int x, int y) {
      if (this.puedoPublicar()) {
         for (HermandadScreen.Renglones r : this.armarTablon(x, y)) {
            int lugar = r.anuncio();
            Button borrar = (Button)this.addRenderableWidget(
               Button.builder(Component.literal("x"), b -> ClientPlayNetworking.send(new HermandadAccionPayload(12, Integer.toString(lugar))))
                  .bounds(x + 388 - 32, r.y() - 1, 13, 11)
                  .build()
            );
            borrar.setTooltip(Tooltip.create(Component.literal("Borrar anuncio")));
         }

         this.anuncioEntrada = (EditBox)this.addRenderableWidget(new EditBox(this.font, x + 16, y + 203, 264, 16, Component.literal("Anuncio")));
         this.anuncioEntrada.setMaxLength(256);
         this.anuncioEntrada.setHint(Component.literal("Escribe un anuncio...").withColor(-9408392));
         this.addRenderableWidget(Button.builder(Component.literal("Publicar"), b -> this.publicarAnuncio()).bounds(x + 388 - 104, y + 202, 88, 18).build());
      }
   }

   private void publicarAnuncio() {
      if (this.anuncioEntrada != null) {
         String texto = this.anuncioEntrada.getValue().strip();
         if (!texto.isEmpty()) {
            ClientPlayNetworking.send(new HermandadAccionPayload(11, texto));
            this.anuncioEntrada.setValue("");
            this.tablonScroll = 0;
         }
      }
   }

   public void refrescarTablon() {
      if (pestana == 1) {
         String escrito = this.anuncioEntrada != null ? this.anuncioEntrada.getValue() : "";
         this.rebuildWidgets();
         if (this.anuncioEntrada != null) {
            this.anuncioEntrada.setValue(escrito);
         }
      }
   }

   private void dibujarTablon(GuiGraphics g, int x, int y) {
      caja(g, x + 14, y + 46, x + 388 - 14, y + 198, -14145490, -11908524);
      List<HermandadTablonPayload.Anuncio> anuncios = HermandadesCliente.anuncios();
      if (anuncios.isEmpty()) {
         g.drawCenteredString(this.font, "Todavía no hay anuncios en el Tablón.", x + 194, y + 116, -5723984);
      }

      for (HermandadScreen.Renglones r : this.armarTablon(x, y)) {
         HermandadTablonPayload.Anuncio anuncio = anuncios.get(r.anuncio());
         g.drawString(this.font, anuncio.fecha(), x + 20, r.y(), -8408321, false);
         g.drawString(this.font, anuncio.autor(), x + 24 + this.font.width(anuncio.fecha()), r.y(), -11162881, false);

         for (int i = 0; i < r.lineas().size(); i++) {
            g.drawString(this.font, r.lineas().get(i), x + 24, r.y() + 12 + i * 10, -1644826, false);
         }

         int abajo = r.y() + 12 + r.lineas().size() * 10 + 2;
         g.fill(x + 18, abajo, x + 388 - 18, abajo + 1, -11908524);
      }

      if (!this.puedoPublicar()) {
         g.drawCenteredString(this.font, "Solo el Maestro y los Líderes pueden publicar anuncios.", x + 194, y + 208, -5723984);
      }
   }

   private static void pixelArt(GuiGraphics g, String[] patron, int x, int y, int color) {
      for (int fila = 0; fila < patron.length; fila++) {
         for (int col = 0; col < patron[fila].length(); col++) {
            if (patron[fila].charAt(col) == 'X') {
               g.fill(x + col, y + fila, x + col + 1, y + fila + 1, color);
            }
         }
      }
   }

   private static void caja(GuiGraphics g, int x1, int y1, int x2, int y2, int fondo, int borde) {
      g.fill(x1, y1, x2, y2, borde);
      g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fondo);
   }

   private PlayerInfo conectado(HermandadInfoPayload.Miembro m) {
      return this.minecraft.getConnection() != null ? this.minecraft.getConnection().getPlayerInfo(m.uuid()) : null;
   }

   private String nombreMaestro() {
      return this.info.miembros().stream().filter(m -> m.uuid().equals(this.info.maestro())).map(HermandadInfoPayload.Miembro::nombre).findFirst().orElse("?");
   }

   private static boolean dentro(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int boton) {
      if (this.clicColor(mouseX, mouseY)) {
         return true;
      }

      int x = this.x();
      int y = this.y();

      for (int i = 0; i < this.iconosTabs.length; i++) {
         if (dentro(mouseX, mouseY, x + 8 + i * 26, y + 8, 22, 22) && i != pestana) {
            pestana = i;
            this.rebuildWidgets();
            return true;
         }
      }

      if (pestana == 3) {
         int lugar = this.lugarEstandarte(mouseX, mouseY);
         if (lugar >= 0) {
            this.estandarteElegido = lugar < HermandadesCliente.coleccion().size() && lugar != this.estandarteElegido ? lugar : -1;
            this.rebuildWidgets();
            return true;
         }
      }

      if (pestana == 2) {
         int centro = x + 194;
         if (dentro(mouseX, mouseY, centro - 108, y + 202, 16, 16)) {
            int i = 0;

            for (int k = 0; k < PALETA.length; k++) {
               if (PALETA[k] == this.colorUbicacion) {
                  i = k;
               }
            }

            this.colorUbicacion = PALETA[(i + (boton == 1 ? PALETA.length - 1 : 1)) % PALETA.length];
            return true;
         }

         int marca = this.ubicacionEn(mouseX, mouseY);
         if (marca >= 0 && boton == 1) {
            ClientPlayNetworking.send(new HermandadAccionPayload(18, Integer.toString(marca)));
            return true;
         }

         if (this.enMapa(mouseX, mouseY) && boton == 0) {
            this.hayElegido = true;
            this.elegidoX = this.minecraft.player.getBlockX() + (int)Math.floor((mouseX - this.mapaX() - 179.0) * escalaMapa);
            this.elegidoZ = this.minecraft.player.getBlockZ() + (int)Math.floor((mouseY - this.mapaY() - 74.0) * escalaMapa);
            this.elegidoY = MapaHermandad.altura(this.minecraft.level, this.elegidoX, this.elegidoZ, this.minecraft.player.getBlockY()) + 1;
            this.rebuildWidgets();
            return true;
         }
      }

      if (dentro(mouseX, mouseY, x + 388 - 8 - 22, y + 8, 22, 22) && pestana != 5) {
         pestana = 5;
         this.listaScroll = 0;
         this.rebuildWidgets();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, boton);
      }
   }

   public boolean keyPressed(int tecla, int scanCode, int modificadores) {
      if (HermandadClient.TECLA_HERMANDAD.matches(tecla, scanCode) && !(this.getFocused() instanceof EditBox)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(tecla, scanCode, modificadores);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private record Renglones(int anuncio, int y, List<FormattedCharSequence> lineas) {
   }
}
