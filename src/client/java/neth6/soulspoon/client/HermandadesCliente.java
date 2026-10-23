package neth6.soulspoon.client;

import neth6.soulspoon.hermandad.HermandadChatPayload;
import neth6.soulspoon.hermandad.HermandadEstandartesPayload;
import neth6.soulspoon.hermandad.HermandadTablonPayload;
import neth6.soulspoon.hermandad.HermandadUbicacionesPayload;
import neth6.soulspoon.hermandad.HermandadesJugadoresPayload;
import neth6.soulspoon.marcas.MarcasPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class HermandadesCliente {
   private static final Map<UUID, HermandadesJugadoresPayload.Entrada> POR_JUGADOR = new HashMap<>();
   private static final Map<String, ItemStack> ACTIVOS = new HashMap<>();
   private static List<MarcasPayload.Marca> ubicaciones = List.of();
   private static List<HermandadTablonPayload.Anuncio> anuncios = List.of();
   private static List<ItemStack> coleccion = List.of();
   private static int activo = -1;
   private static List<HermandadChatPayload.Linea> chat = List.of();

   private HermandadesCliente() {
   }

   public static void actualizar(HermandadesJugadoresPayload payload) {
      POR_JUGADOR.clear();

      for (HermandadesJugadoresPayload.Entrada e : payload.entradas()) {
         POR_JUGADOR.put(e.jugador(), e);
      }

      ACTIVOS.clear();

      for (HermandadesJugadoresPayload.Activo a : payload.activos()) {
         ACTIVOS.put(a.hermandad(), a.estandarte());
      }
   }

   public static ItemStack estandarteDe(UUID jugador) {
      HermandadesJugadoresPayload.Entrada e = POR_JUGADOR.get(jugador);
      return e == null ? ItemStack.EMPTY : estandarteDeHermandad(e.hermandad());
   }

   public static ItemStack estandarteDeHermandad(String hermandad) {
      return ACTIVOS.getOrDefault(hermandad, ItemStack.EMPTY);
   }

   public static void actualizarUbicaciones(HermandadUbicacionesPayload payload) {
      ubicaciones = List.copyOf(payload.ubicaciones());
   }

   public static List<MarcasPayload.Marca> ubicaciones() {
      return ubicaciones;
   }

   public static void actualizarTablon(HermandadTablonPayload payload) {
      anuncios = List.copyOf(payload.anuncios());
   }

   public static List<HermandadTablonPayload.Anuncio> anuncios() {
      return anuncios;
   }

   public static void actualizarEstandartes(HermandadEstandartesPayload payload) {
      coleccion = List.copyOf(payload.coleccion());
      activo = payload.activo();
   }

   public static List<ItemStack> coleccion() {
      return coleccion;
   }

   public static int activo() {
      return activo;
   }

   public static Map<String, Integer> todas() {
      Map<String, Integer> lista = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

      for (HermandadesJugadoresPayload.Entrada e : POR_JUGADOR.values()) {
         lista.put(e.hermandad(), e.color());
      }

      return lista;
   }

   public static void actualizarChat(HermandadChatPayload payload) {
      chat = List.copyOf(payload.lineas());
   }

   public static List<HermandadChatPayload.Linea> chat() {
      return chat;
   }

   public static void limpiar() {
      POR_JUGADOR.clear();
      ACTIVOS.clear();
      chat = List.of();
      coleccion = List.of();
      activo = -1;
      anuncios = List.of();
      ubicaciones = List.of();
   }

   public static HermandadesJugadoresPayload.Entrada de(UUID jugador) {
      return POR_JUGADOR.get(jugador);
   }
}
