package neth6.soulspoon.hermandad;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.item.ManuscritoHermandadItem;
import neth6.soulspoon.marcas.MarcasPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

public final class Hermandades {
   private static final Map<UUID, String> INVITACIONES = new HashMap<>();
   private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
   public static final int MAXIMO_LIDERES = 2;
   private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

   private Hermandades() {
   }

   public static HermandadesData data(ServerPlayer jugador) {
      return (HermandadesData)jugador.server.overworld().getDataStorage().computeIfAbsent(HermandadesData.FACTORY, "soulspoon_hermandades");
   }

   public static void alAccion(ServerPlayer jugador, ManuscritoAccionPayload accion) {
      InteractionHand mano = accion.manoPrincipal() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
      ItemStack manuscrito = jugador.getItemInHand(mano);
      if (ManuscritoHermandadItem.es(manuscrito) && !ManuscritoDatos.sellado(manuscrito)) {
         String nombre = ManuscritoDatos.limpiarNombre(accion.nombre());
         if (!nombre.equals(ManuscritoDatos.nombre(manuscrito))) {
            ManuscritoDatos.setNombre(manuscrito, nombre);
         }

         switch (accion.accion()) {
            case 1:
               inscribirse(jugador, manuscrito);
               break;
            case 2:
               sellar(jugador, manuscrito, nombre);
         }
      }
   }

   private static void inscribirse(ServerPlayer jugador, ItemStack manuscrito) {
      if (ManuscritoDatos.estaInscrito(manuscrito, jugador.getUUID())) {
         error(jugador, "Ya estás inscrito en este Manuscrito.");
      } else if (ManuscritoDatos.inscritos(manuscrito).size() >= 10) {
         error(jugador, "El Manuscrito ya no tiene lugar para más firmas.");
      } else if (data(jugador).deJugador(jugador.getUUID()).isPresent()) {
         error(jugador, "Ya perteneces a una Hermandad.");
      } else {
         ManuscritoDatos.inscribir(manuscrito, jugador.getUUID(), jugador.getGameProfile().getName());
      }
   }

   private static void sellar(ServerPlayer jugador, ItemStack manuscrito, String nombre) {
      if (puedeFundarse(jugador, manuscrito, nombre)) {
         ManuscritoDatos.sellar(manuscrito);
         jugador.sendSystemMessage(
            Component.empty()
               .append(Component.literal("El manuscrito ha sido sellado. Sostenlo y usa /guild found para fundar ").withStyle(ChatFormatting.YELLOW))
               .append(Component.literal(nombre).withStyle(ChatFormatting.GOLD))
               .append(Component.literal(".").withStyle(ChatFormatting.YELLOW))
         );
      }
   }

   public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("guild").then(Commands.literal("found").executes(ctx -> {
         fundar(((CommandSourceStack)ctx.getSource()).getPlayerOrException());
         return 1;
      }))).then(Commands.literal("accept").executes(ctx -> {
         aceptar(((CommandSourceStack)ctx.getSource()).getPlayerOrException());
         return 1;
      })));
   }

   private static void fundar(ServerPlayer jugador) {
      ItemStack manuscrito = manuscritoSellado(jugador);
      if (manuscrito == null) {
         error(jugador, "Debes sostener un Manuscrito de Hermandad sellado.");
      } else {
         String nombre = ManuscritoDatos.nombre(manuscrito);
         if (puedeFundarse(jugador, manuscrito, nombre)) {
            List<ManuscritoDatos.Inscrito> inscritos = ManuscritoDatos.inscritos(manuscrito);
            ManuscritoDatos.Inscrito maestro = inscritos.get(0);
            if (!maestro.uuid().equals(jugador.getUUID())) {
               error(jugador, "Solo el Maestro de la Hermandad (" + maestro.nombre() + ") puede usar /guild found.");
            } else {
               data(jugador).agregar(new HermandadesData.Hermandad(nombre, maestro.uuid(), inscritos));
               manuscrito.shrink(1);
               sincronizarJugadores(jugador.server);
               jugador.server
                  .getPlayerList()
                  .broadcastSystemMessage(
                     Component.empty()
                        .append(Component.literal("¡La hermandad ").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(nombre).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" ha sido fundada por " + maestro.nombre() + "!").withStyle(ChatFormatting.GREEN)),
                     false
                  );
            }
         }
      }
   }

   public static void alAccionInterfaz(ServerPlayer jugador, HermandadAccionPayload accion) {
      Optional<HermandadesData.Hermandad> hermandad = data(jugador).deJugador(jugador.getUUID());
      if (hermandad.isEmpty()) {
         error(jugador, "No perteneces a ninguna Hermandad.");
      } else {
         switch (accion.accion()) {
            case 0:
               enviarInfo(jugador, hermandad.get());
               ServerPlayNetworking.send(jugador, new HermandadChatPayload(hermandad.get().chat()));
               enviarEstandartes(jugador, hermandad.get());
               ServerPlayNetworking.send(jugador, new HermandadTablonPayload(hermandad.get().anuncios()));
               ServerPlayNetworking.send(jugador, new HermandadUbicacionesPayload(hermandad.get().ubicaciones()));
               break;
            case 1:
               invitar(jugador, hermandad.get(), accion.texto().strip());
               break;
            case 2:
               expulsar(jugador, hermandad.get(), accion.texto());
               break;
            case 3:
               cambiarColor(jugador, hermandad.get(), accion.texto());
               break;
            case 4:
               chatear(jugador, hermandad.get(), accion.texto());
            case 5:
            default:
               break;
            case 6:
               guardarEstandarte(jugador, hermandad.get());
               break;
            case 7:
            case 8:
            case 9:
            case 10:
               manejarEstandarte(jugador, hermandad.get(), accion.accion(), accion.texto());
               break;
            case 11:
               publicarAnuncio(jugador, hermandad.get(), accion.texto());
               break;
            case 12:
               borrarAnuncio(jugador, hermandad.get(), accion.texto());
               break;
            case 13:
               cambiarRango(jugador, hermandad.get(), accion.texto(), true);
               break;
            case 14:
               cambiarRango(jugador, hermandad.get(), accion.texto(), false);
               break;
            case 15:
               disolver(jugador, hermandad.get());
               break;
            case 16:
               salir(jugador, hermandad.get());
               break;
            case 17:
               agregarUbicacion(jugador, hermandad.get(), accion.texto());
               break;
            case 18:
               borrarUbicacion(jugador, hermandad.get(), accion.texto());
         }
      }
   }

   private static void enviarInfo(ServerPlayer jugador, HermandadesData.Hermandad h) {
      List<HermandadInfoPayload.Miembro> miembros = h.miembros()
         .stream()
         .map(m -> new HermandadInfoPayload.Miembro(m.uuid(), m.nombre(), h.esLider(m.uuid())))
         .toList();
      ServerPlayNetworking.send(jugador, new HermandadInfoPayload(h.nombre(), h.color(), h.maestro(), miembros, h.balance(), h.nivelBanco(), h.motd()));
   }

   private static void cambiarColor(ServerPlayer maestro, HermandadesData.Hermandad h, String hex) {
      if (!h.maestro().equals(maestro.getUUID())) {
         error(maestro, "Solo el Maestro puede cambiar el color de la Hermandad.");
      } else {
         try {
            data(maestro).setColor(h, Integer.parseInt(hex, 16));
         } catch (NumberFormatException e) {
            return;
         }

         refrescarMiembros(maestro.server, h);
         sincronizarJugadores(maestro.server);
      }
   }

   private static HermandadesJugadoresPayload payloadJugadores(MinecraftServer server) {
      HermandadesData data = (HermandadesData)server.overworld().getDataStorage().computeIfAbsent(HermandadesData.FACTORY, "soulspoon_hermandades");
      List<HermandadesJugadoresPayload.Entrada> entradas = new ArrayList<>();

      for (HermandadesData.Hermandad h : data.todas()) {
         for (ManuscritoDatos.Inscrito m : h.miembros()) {
            String rango = h.maestro().equals(m.uuid()) ? "Maestro" : (h.esLider(m.uuid()) ? "Líder" : "Miembro");
            entradas.add(new HermandadesJugadoresPayload.Entrada(m.uuid(), h.nombre(), h.color(), rango));
         }
      }

      List<HermandadesJugadoresPayload.Activo> activos = new ArrayList<>();

      for (HermandadesData.Hermandad h : data.todas()) {
         ItemStack activo = h.estandarteActivo();
         if (!activo.isEmpty()) {
            activos.add(new HermandadesJugadoresPayload.Activo(h.nombre(), activo.copy()));
         }
      }

      return new HermandadesJugadoresPayload(entradas, activos);
   }

   public static void sincronizarJugadores(MinecraftServer server) {
      HermandadesJugadoresPayload payload = payloadJugadores(server);

      for (ServerPlayer conectado : server.getPlayerList().getPlayers()) {
         ServerPlayNetworking.send(conectado, payload);
      }
   }

   public static void sincronizarJugador(ServerPlayer jugador) {
      ServerPlayNetworking.send(jugador, payloadJugadores(jugador.server));
   }

   private static void refrescarMiembros(MinecraftServer server, HermandadesData.Hermandad h) {
      for (ManuscritoDatos.Inscrito m : h.miembros()) {
         ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
         if (conectado != null) {
            enviarInfo(conectado, h);
         }
      }
   }

   private static void invitar(ServerPlayer maestro, HermandadesData.Hermandad h, String nombreInvitado) {
      if (!h.maestro().equals(maestro.getUUID())) {
         error(maestro, "Solo el Maestro puede invitar jugadores.");
      } else {
         ServerPlayer invitado = maestro.server.getPlayerList().getPlayerByName(nombreInvitado);
         if (invitado == null) {
            error(maestro, "No hay ningún jugador conectado llamado " + nombreInvitado + ".");
         } else if (data(maestro).deJugador(invitado.getUUID()).isPresent()) {
            error(maestro, invitado.getGameProfile().getName() + " ya pertenece a una Hermandad.");
         } else {
            INVITACIONES.put(invitado.getUUID(), h.nombre());
            maestro.sendSystemMessage(
               Component.literal("Invitaste a " + invitado.getGameProfile().getName() + " a la Hermandad.").withStyle(ChatFormatting.GREEN)
            );
            invitado.sendSystemMessage(
               Component.empty()
                  .append(Component.literal(maestro.getGameProfile().getName()).withStyle(ChatFormatting.GOLD))
                  .append(Component.literal(" te invitó a la Hermandad ").withStyle(ChatFormatting.YELLOW))
                  .append(Component.literal(h.nombre()).withStyle(ChatFormatting.GOLD))
                  .append(Component.literal(". ").withStyle(ChatFormatting.YELLOW))
                  .append(
                     Component.literal("[Aceptar]")
                        .withStyle(
                           s -> s.withColor(ChatFormatting.GREEN)
                              .withBold(true)
                              .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/guild accept"))
                              .withHoverEvent(
                                 new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Unirse a " + h.nombre()))
                              )
                        )
                  )
            );
         }
      }
   }

   private static void aceptar(ServerPlayer jugador) {
      String nombre = INVITACIONES.remove(jugador.getUUID());
      HermandadesData data = data(jugador);
      Optional<HermandadesData.Hermandad> hermandad = nombre == null ? Optional.empty() : data.porNombre(nombre);
      if (hermandad.isEmpty()) {
         error(jugador, "No tienes ninguna invitación pendiente.");
      } else if (data.deJugador(jugador.getUUID()).isPresent()) {
         error(jugador, "Ya perteneces a una Hermandad.");
      } else {
         HermandadesData.Hermandad h = hermandad.get();
         data.agregarMiembro(h, jugador.getUUID(), jugador.getGameProfile().getName());
         avisarMiembros(jugador.server, h, jugador.getGameProfile().getName() + " se unió a la Hermandad.", ChatFormatting.GREEN);
         refrescarMiembros(jugador.server, h);
         sincronizarJugadores(jugador.server);
      }
   }

   private static void expulsar(ServerPlayer maestro, HermandadesData.Hermandad h, String uuidTexto) {
      if (!h.maestro().equals(maestro.getUUID())) {
         error(maestro, "Solo el Maestro puede expulsar miembros.");
      } else {
         UUID uuid;
         try {
            uuid = UUID.fromString(uuidTexto);
         } catch (IllegalArgumentException e) {
            return;
         }

         if (!uuid.equals(h.maestro()) && h.esMiembro(uuid)) {
            String nombre = h.miembros().stream().filter(m -> m.uuid().equals(uuid)).map(ManuscritoDatos.Inscrito::nombre).findFirst().orElse("?");
            data(maestro).quitarMiembro(h, uuid);
            avisarMiembros(maestro.server, h, nombre + " fue expulsado de la Hermandad.", ChatFormatting.RED);
            refrescarMiembros(maestro.server, h);
            sincronizarJugadores(maestro.server);
            ServerPlayer expulsado = maestro.server.getPlayerList().getPlayer(uuid);
            if (expulsado != null) {
               expulsado.sendSystemMessage(Component.literal("Fuiste expulsado de la Hermandad " + h.nombre() + ".").withStyle(ChatFormatting.RED));
            }
         }
      }
   }

   private static void salir(ServerPlayer jugador, HermandadesData.Hermandad h) {
      if (h.maestro().equals(jugador.getUUID())) {
         error(jugador, "El Maestro no puede salirse: puede disolver la Hermandad.");
      } else {
         String nombre = jugador.getGameProfile().getName();
         data(jugador).quitarMiembro(h, jugador.getUUID());
         jugador.sendSystemMessage(Component.literal("Saliste de la Hermandad " + h.nombre() + ".").withStyle(ChatFormatting.YELLOW));
         avisarMiembros(jugador.server, h, nombre + " se fue de la Hermandad.", ChatFormatting.GRAY);
         refrescarMiembros(jugador.server, h);
         sincronizarJugadores(jugador.server);
      }
   }

   private static void disolver(ServerPlayer maestro, HermandadesData.Hermandad h) {
      if (!h.maestro().equals(maestro.getUUID())) {
         error(maestro, "Solo el Maestro puede disolver la Hermandad.");
      } else {
         for (ManuscritoDatos.Inscrito m : h.miembros()) {
            ServerPlayer conectado = maestro.server.getPlayerList().getPlayer(m.uuid());
            if (conectado != null) {
               conectado.sendSystemMessage(Component.literal("La Hermandad " + h.nombre() + " fue disuelta por su Maestro.").withStyle(ChatFormatting.RED));
            }
         }

         data(maestro).quitar(h);
         sincronizarJugadores(maestro.server);
      }
   }

   public static void avisarMiembros(MinecraftServer server, HermandadesData.Hermandad h, String mensaje, ChatFormatting color) {
      for (ManuscritoDatos.Inscrito m : h.miembros()) {
         ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
         if (conectado != null) {
            conectado.sendSystemMessage(Component.literal(mensaje).withStyle(color));
         }
      }

      agregarAlChat(server, h, "", mensaje);
   }

   private static void refrescarTablon(MinecraftServer server, HermandadesData.Hermandad h) {
      HermandadTablonPayload payload = new HermandadTablonPayload(h.anuncios());

      for (ManuscritoDatos.Inscrito m : h.miembros()) {
         ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
         if (conectado != null) {
            ServerPlayNetworking.send(conectado, payload);
         }
      }
   }

   private static void publicarAnuncio(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
      if (!h.puedePublicar(jugador.getUUID())) {
         error(jugador, "Solo el Maestro y los Líderes pueden publicar en el Tablón.");
      } else {
         String limpio = texto.replace("§", "").strip();
         if (!limpio.isEmpty()) {
            if (limpio.length() > 256) {
               limpio = limpio.substring(0, 256);
            }

            data(jugador).publicarAnuncio(h, new HermandadTablonPayload.Anuncio(LocalDateTime.now().format(FECHA), jugador.getGameProfile().getName(), limpio));
            avisarMiembros(jugador.server, h, "Nuevo anuncio en el tablón: " + limpio, ChatFormatting.AQUA);
            refrescarTablon(jugador.server, h);
         }
      }
   }

   private static void borrarAnuncio(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
      if (!h.puedePublicar(jugador.getUUID())) {
         error(jugador, "Solo el Maestro y los Líderes pueden borrar anuncios.");
      } else {
         try {
            if (data(jugador).borrarAnuncio(h, Integer.parseInt(texto))) {
               refrescarTablon(jugador.server, h);
            }
         } catch (NumberFormatException var4) {
         }
      }
   }

   public static void enviarUbicaciones(ServerPlayer jugador) {
      List<MarcasPayload.Marca> lista = data(jugador).deJugador(jugador.getUUID()).map(HermandadesData.Hermandad::ubicaciones).orElse(List.of());
      ServerPlayNetworking.send(jugador, new HermandadUbicacionesPayload(lista));
   }

   private static void refrescarUbicaciones(MinecraftServer server, HermandadesData.Hermandad h) {
      HermandadUbicacionesPayload payload = new HermandadUbicacionesPayload(h.ubicaciones());

      for (ManuscritoDatos.Inscrito m : h.miembros()) {
         ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
         if (conectado != null) {
            ServerPlayNetworking.send(conectado, payload);
         }
      }
   }

   private static void agregarUbicacion(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
      String[] partes = texto.split(";", 5);
      if (partes.length >= 5) {
         try {
            int color = Integer.parseInt(partes[0], 16) & 16777215;
            int x = Integer.parseInt(partes[1]);
            int y = Integer.parseInt(partes[2]);
            int z = Integer.parseInt(partes[3]);
            String nombre = partes[4].replace("§", "").strip();
            if (nombre.isEmpty()) {
               nombre = "Ubicación";
            }

            if (nombre.length() > 24) {
               nombre = nombre.substring(0, 24);
            }

            String dimension = jugador.level().dimension().location().toString();
            if (!data(jugador).agregarUbicacion(h, new MarcasPayload.Marca(nombre, dimension, x, y, z, color), jugador.getUUID())) {
               error(jugador, "La Hermandad ya tiene 16 ubicaciones: borra alguna.");
               return;
            }

            avisarMiembros(
               jugador.server,
               h,
               jugador.getGameProfile().getName() + " marcó la ubicación " + nombre + " (" + x + " " + y + " " + z + ").",
               ChatFormatting.AQUA
            );
            refrescarUbicaciones(jugador.server, h);
         } catch (NumberFormatException var10) {
         }
      }
   }

   private static void borrarUbicacion(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
      try {
         int lugar = Integer.parseInt(texto);
         if (lugar < 0 || lugar >= h.ubicaciones().size()) {
            return;
         }

         if (!h.puedePublicar(jugador.getUUID()) && !h.autoresUbicaciones().get(lugar).equals(jugador.getUUID())) {
            error(jugador, "Solo el que la marcó, el Maestro o un Líder pueden borrarla.");
            return;
         }

         data(jugador).borrarUbicacion(h, lugar);
         refrescarUbicaciones(jugador.server, h);
      } catch (NumberFormatException var4) {
      }
   }

   private static void cambiarRango(ServerPlayer maestro, HermandadesData.Hermandad h, String uuidTexto, boolean subir) {
      if (!h.maestro().equals(maestro.getUUID())) {
         error(maestro, "Solo el Maestro puede cambiar los rangos.");
      } else {
         UUID uuid;
         try {
            uuid = UUID.fromString(uuidTexto);
         } catch (IllegalArgumentException e) {
            return;
         }

         if (!uuid.equals(h.maestro()) && h.esMiembro(uuid) && h.esLider(uuid) != subir) {
            if (subir && h.lideres().size() >= 2) {
               error(maestro, "La Hermandad ya tiene 2 Líderes: baja a uno antes.");
            } else {
               String nombre = h.miembros().stream().filter(m -> m.uuid().equals(uuid)).map(ManuscritoDatos.Inscrito::nombre).findFirst().orElse("?");
               data(maestro).setLider(h, uuid, subir);
               avisarMiembros(
                  maestro.server,
                  h,
                  subir ? nombre + " ahora es Líder de la Hermandad." : nombre + " ya no es Líder de la Hermandad.",
                  subir ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY
               );
               refrescarMiembros(maestro.server, h);
            }
         }
      }
   }

   private static void enviarEstandartes(ServerPlayer jugador, HermandadesData.Hermandad h) {
      ServerPlayNetworking.send(jugador, new HermandadEstandartesPayload(h.estandartes().stream().<ItemStack>map(ItemStack::copy).toList(), h.activo()));
   }

   private static void refrescarEstandartes(MinecraftServer server, HermandadesData.Hermandad h) {
      for (ManuscritoDatos.Inscrito m : h.miembros()) {
         ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
         if (conectado != null) {
            enviarEstandartes(conectado, h);
         }
      }
   }

   private static void guardarEstandarte(ServerPlayer jugador, HermandadesData.Hermandad h) {
      ItemStack enMano = jugador.getMainHandItem();
      if (!(enMano.getItem() instanceof BannerItem)) {
         error(jugador, "Tienes que tener un estandarte en la mano.");
      } else if (!data(jugador).guardarEstandarte(h, enMano)) {
         error(jugador, "La Colección de Estandartes está llena.");
      } else {
         if (!jugador.getAbilities().instabuild) {
            enMano.shrink(1);
         }

         jugador.displayClientMessage(Component.literal("Estandarte guardado en la colección.").withStyle(ChatFormatting.GREEN), true);
         refrescarEstandartes(jugador.server, h);
      }
   }

   private static void manejarEstandarte(ServerPlayer maestro, HermandadesData.Hermandad h, int accion, String texto) {
      if (!h.maestro().equals(maestro.getUUID())) {
         error(maestro, "Solo el Maestro puede administrar los estandartes.");
      } else {
         HermandadesData data = data(maestro);

         int lugar;
         try {
            lugar = texto.isEmpty() ? -1 : Integer.parseInt(texto);
         } catch (NumberFormatException e) {
            return;
         }

         boolean cambiaActivo;
         switch (accion) {
            case 7:
               if (lugar < 0 || lugar >= h.estandartes().size()) {
                  return;
               }

               data.activarEstandarte(h, lugar);
               avisarMiembros(maestro.server, h, "La Hermandad tiene un nuevo estandarte.", ChatFormatting.AQUA);
               cambiaActivo = true;
               break;
            case 8:
            case 9:
               boolean eraActivo = lugar == h.activo();
               ItemStack sacado = data.sacarEstandarte(h, lugar);
               if (sacado.isEmpty()) {
                  return;
               }

               if (accion == 8 && !maestro.getInventory().add(sacado)) {
                  maestro.drop(sacado, false);
               }

               cambiaActivo = eraActivo;
               break;
            case 10:
               if (h.activo() < 0) {
                  return;
               }

               data.activarEstandarte(h, -1);
               cambiaActivo = true;
               break;
            default:
               return;
         }

         refrescarEstandartes(maestro.server, h);
         if (cambiaActivo) {
            sincronizarJugadores(maestro.server);
         }
      }
   }

   private static void chatear(ServerPlayer jugador, HermandadesData.Hermandad h, String texto) {
      String limpio = texto.replace("§", "").strip();
      limpio = limpio.chars().filter(c -> c >= 32 && c != 127).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
      if (!limpio.isEmpty()) {
         if (limpio.length() > 256) {
            limpio = limpio.substring(0, 256);
         }

         String nombre = jugador.getGameProfile().getName();
         Component mensaje = Component.empty()
            .append(Component.literal("[" + h.nombre() + "] " + nombre).withColor(h.color()))
            .append(Component.literal(": " + limpio).withStyle(ChatFormatting.WHITE));

         for (ManuscritoDatos.Inscrito m : h.miembros()) {
            ServerPlayer conectado = jugador.server.getPlayerList().getPlayer(m.uuid());
            if (conectado != null) {
               conectado.sendSystemMessage(mensaje);
            }
         }

         SoulSpoon.LOGGER.info("[Hermandad {}] {}: {}", new Object[]{h.nombre(), nombre, limpio});
         agregarAlChat(jugador.server, h, nombre, limpio);
      }
   }

   private static void agregarAlChat(MinecraftServer server, HermandadesData.Hermandad h, String autor, String texto) {
      HermandadesData data = (HermandadesData)server.overworld().getDataStorage().computeIfAbsent(HermandadesData.FACTORY, "soulspoon_hermandades");
      data.agregarChat(h, new HermandadChatPayload.Linea(LocalTime.now().format(HORA), autor, texto));
      HermandadChatPayload payload = new HermandadChatPayload(h.chat());

      for (ManuscritoDatos.Inscrito m : h.miembros()) {
         ServerPlayer conectado = server.getPlayerList().getPlayer(m.uuid());
         if (conectado != null) {
            ServerPlayNetworking.send(conectado, payload);
         }
      }
   }

   private static ItemStack manuscritoSellado(ServerPlayer jugador) {
      for (InteractionHand mano : InteractionHand.values()) {
         ItemStack stack = jugador.getItemInHand(mano);
         if (ManuscritoHermandadItem.es(stack) && ManuscritoDatos.sellado(stack)) {
            return stack;
         }
      }

      return null;
   }

   private static boolean puedeFundarse(ServerPlayer jugador, ItemStack manuscrito, String nombre) {
      List<ManuscritoDatos.Inscrito> inscritos = ManuscritoDatos.inscritos(manuscrito);
      HermandadesData data = data(jugador);
      if (nombre.isEmpty()) {
         return error(jugador, "Escribe el nombre de la Hermandad arriba del Manuscrito.");
      }

      int minimo = ManuscritoHermandadItem.minimoInscritos(manuscrito);
      if (inscritos.size() < minimo) {
         return error(jugador, "Se necesitan al menos " + minimo + " inscritos.");
      }

      if (!ManuscritoDatos.estaInscrito(manuscrito, jugador.getUUID())) {
         return error(jugador, "Debes estar inscrito en el Manuscrito.");
      }

      if (data.nombreUsado(nombre)) {
         return error(jugador, "Ya existe una Hermandad llamada " + nombre + ".");
      }

      for (ManuscritoDatos.Inscrito inscrito : inscritos) {
         if (data.deJugador(inscrito.uuid()).isPresent()) {
            return error(jugador, inscrito.nombre() + " ya pertenece a otra Hermandad.");
         }
      }

      return true;
   }

   private static boolean error(ServerPlayer jugador, String mensaje) {
      jugador.sendSystemMessage(Component.literal(mensaje).withStyle(ChatFormatting.RED));
      return false;
   }
}
