package neth6.soulspoon.correo;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class Correo {
   public static final int MAX_TEXTO = 500;
   public static final int CASILLAS = 27;
   public static final String TODOS = "todos";
   public static final MenuType<CorreoMenu> MENU = (MenuType<CorreoMenu>)Registry.register(
      BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath("soulspoon", "correo"), new MenuType<>(CorreoMenu::new, FeatureFlags.VANILLA_SET)
   );
   public static volatile int ABIERTA_CLIENTE = -1;
   // El sonido "notify_message" ya esta en sounds.json; aqui solo se arma el evento (no se registra de nuevo).
   public static final SoundEvent SONIDO_MENSAJE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("soulspoon", "notify_message"));

   private Correo() {
   }

   private static Correo.Datos datos(MinecraftServer server) {
      return (Correo.Datos)server.overworld().getDataStorage().computeIfAbsent(Correo.Datos.FACTORY, "soulspoon_correo");
   }

   private static boolean esDe(Correo.Datos d, Correo.Carta c, UUID jugador) {
      return (c.para() == null || c.para().equals(jugador)) && !Correo.Datos.tiene(d.borradas, jugador, c.id());
   }

   private static Correo.Carta buscar(Correo.Datos d, ServerPlayer p, int id) {
      for (Correo.Carta c : d.cartas) {
         if (c.id() == id && esDe(d, c, p.getUUID())) {
            return c;
         }
      }

      return null;
   }

   public static void enviarBuzon(ServerPlayer p) {
      Correo.Datos d = datos(p.server);
      long ahora = System.currentTimeMillis();
      List<Correo.CartaVista> l = new ArrayList<>();

      for (int i = d.cartas.size() - 1; i >= 0; i--) {
         Correo.Carta c = d.cartas.get(i);
         if (esDe(d, c, p.getUUID())) {
            boolean conObjetos = d.restantes(p.getUUID(), c).stream().anyMatch(o -> !o.isEmpty());
            l.add(
               new Correo.CartaVista(
                  c.id(),
                  c.autorNombre(),
                  c.asunto(),
                  c.texto(),
                  Math.max(0L, (ahora - c.fecha()) / 1000L),
                  Correo.Datos.tiene(d.leidas, p.getUUID(), c.id()),
                  conObjetos
               )
            );
         }
      }

      ServerPlayNetworking.send(p, new Correo.BuzonPayload(l));
   }

   static Correo.Carta carta(ServerPlayer p, int id) {
      return buscar(datos(p.server), p, id);
   }

   static void guardarRestantes(ServerPlayer p, int id, List<ItemStack> objetos) {
      Correo.Datos d = datos(p.server);
      Correo.Carta c = buscar(d, p, id);
      if (c != null) {
         if (objetos.isEmpty()) {
            d.marcar(d.borradas, p.getUUID(), c.id());
            Map<Integer, List<ItemStack>> r = d.restantes.get(p.getUUID());
            if (r != null) {
               r.remove(c.id());
            }

            limpiar(d, c);
         } else {
            d.restantes.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(id, objetos);
         }

         d.setDirty();
         enviarBuzon(p);
      }
   }

   private static void accion(ServerPlayer p, Correo.AccionPayload a) {
      Correo.Datos d = datos(p.server);
      if (a.accion().equals("abrir") && a.id() < 0) {
         ServerPlayNetworking.send(p, new Correo.AbiertaPayload(-1));
         p.openMenu(new SimpleMenuProvider((id, inv, jx) -> new CorreoMenu(id, inv, -1, List.of()), Component.literal("Mensaje")));
      } else {
         Correo.Carta c = buscar(d, p, a.id());
         if (c != null) {
            UUID j = p.getUUID();
            switch (a.accion()) {
               case "abrir":
                  d.marcar(d.leidas, j, c.id());
                  enviarBuzon(p);
                  ServerPlayNetworking.send(p, new Correo.AbiertaPayload(c.id()));
                  List<ItemStack> objetos = d.restantes(j, c);
                  p.openMenu(new SimpleMenuProvider((id, inv, jj) -> new CorreoMenu(id, inv, c.id(), objetos), Component.literal("Mensaje")));
                  break;
               case "borrar":
                  if (p.containerMenu instanceof CorreoMenu m && m.carta == c.id()) {
                     p.closeContainer();
                  }

                  for (ItemStack o : d.restantes(j, c)) {
                     ItemStack copia = o.copy();
                     if (!copia.isEmpty() && !p.getInventory().add(copia)) {
                        p.drop(copia, false);
                     }
                  }

                  d.marcar(d.borradas, j, c.id());
                  Map<Integer, List<ItemStack>> r = d.restantes.get(j);
                  if (r != null) {
                     r.remove(c.id());
                  }

                  limpiar(d, c);
                  d.setDirty();
                  enviarBuzon(p);
            }
         }
      }
   }

   private static void limpiar(Correo.Datos d, Correo.Carta c) {
      if (c.para() != null && Correo.Datos.tiene(d.borradas, c.para(), c.id())) {
         d.cartas.remove(c);
         Set<Integer> s = d.leidas.get(c.para());
         if (s != null) {
            s.remove(c.id());
         }

         s = d.borradas.get(c.para());
         if (s != null) {
            s.remove(c.id());
         }
      }
   }

   private static String enviar(ServerPlayer p, Correo.EnviarPayload e) {
      if (p.containerMenu instanceof CorreoMenu menu && menu.carta == -1) {
         String texto = e.texto().strip();
         String para = e.para().strip();
         List<ItemStack> objetos = menu.objetos();
         if (para.isEmpty()) {
            return "Escribe a quién se lo mandas.";
         }

         if (texto.isEmpty() && objetos.isEmpty()) {
            return "El mensaje está vacío.";
         }

         if (texto.length() > 500) {
            texto = texto.substring(0, 500);
         }

         boolean aTodos = para.equalsIgnoreCase("todos");
         if (aTodos && !p.hasPermissions(2)) {
            return "Solo los admins pueden mandar mensajes a todos.";
         }

         UUID destino = null;
         String nombre = para;
         if (!aTodos) {
            ServerPlayer conectado = p.server.getPlayerList().getPlayerByName(para);
            if (conectado != null) {
               destino = conectado.getUUID();
               nombre = conectado.getGameProfile().getName();
            } else {
               Optional<GameProfile> perfil = p.server.getProfileCache() == null ? Optional.empty() : p.server.getProfileCache().get(para);
               if (perfil.isEmpty()) {
                  return "No existe el jugador " + para + ".";
               }

               destino = perfil.get().getId();
               nombre = perfil.get().getName();
            }

            if (destino.equals(p.getUUID())) {
               return "No te puedes mandar un mensaje a ti mismo.";
            }
         }

         menu.vaciar();
         Correo.Datos d = datos(p.server);
         Correo.Carta c = new Correo.Carta(d.siguiente++, p.getUUID(), p.getGameProfile().getName(), destino, "", texto, objetos, System.currentTimeMillis());
         d.cartas.add(c);
         d.setDirty();

         for (ServerPlayer otro : p.server.getPlayerList().getPlayers()) {
            if (otro != p && (destino == null || otro.getUUID().equals(destino))) {
               enviarBuzon(otro);
               otro.sendSystemMessage(
                  Component.literal("✉ Tienes un mensaje nuevo de ")
                     .withStyle(ChatFormatting.AQUA)
                     .append(Component.literal(c.autorNombre()).withStyle(ChatFormatting.WHITE))
                     .append(Component.literal(". Ábrelo en un Buzón.").withStyle(ChatFormatting.AQUA))
               );
               otro.playNotifySound(SONIDO_MENSAJE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
         }

         enviarBuzon(p);
         p.sendSystemMessage(Component.literal("✉ Mensaje enviado a " + (aTodos ? "todos" : nombre) + ".").withStyle(ChatFormatting.AQUA));
         p.closeContainer();
         return "";
      } else {
         return "Abre el buzón para mandar un mensaje.";
      }
   }

   public static void mandar(MinecraftServer server, UUID paraId, String autor, String asunto, String texto, List<ItemStack> objetos) {
      Correo.Datos d = datos(server);
      List<ItemStack> copia = new ArrayList<>();

      for (ItemStack o : objetos) {
         if (!o.isEmpty() && copia.size() < 27) {
            copia.add(o.copy());
         }
      }

      Correo.Carta c = new Correo.Carta(d.siguiente++, new UUID(0L, 0L), autor, paraId, asunto, texto, copia, System.currentTimeMillis());
      d.cartas.add(c);
      d.setDirty();
      ServerPlayer para = server.getPlayerList().getPlayer(paraId);
      if (para != null) {
         enviarBuzon(para);
         para.sendSystemMessage(
            Component.literal("✉ Tienes un mensaje nuevo de ")
               .withStyle(ChatFormatting.AQUA)
               .append(Component.literal(autor).withStyle(ChatFormatting.WHITE))
               .append(Component.literal(". Ábrelo en un Buzón.").withStyle(ChatFormatting.AQUA))
         );
         para.playNotifySound(SONIDO_MENSAJE, SoundSource.PLAYERS, 1.0F, 1.0F);
      }
   }

   public static void registrar() {
      PayloadTypeRegistry.playS2C().register(Correo.BuzonPayload.TYPE, Correo.BuzonPayload.CODEC);
      PayloadTypeRegistry.playS2C().register(Correo.AbiertaPayload.TYPE, Correo.AbiertaPayload.CODEC);
      PayloadTypeRegistry.playS2C().register(Correo.ResultadoPayload.TYPE, Correo.ResultadoPayload.CODEC);
      PayloadTypeRegistry.playC2S().register(Correo.AccionPayload.TYPE, Correo.AccionPayload.CODEC);
      PayloadTypeRegistry.playC2S().register(Correo.EnviarPayload.TYPE, Correo.EnviarPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(
         Correo.AccionPayload.TYPE, (payload, context) -> context.server().execute(() -> accion(context.player(), payload))
      );
      ServerPlayNetworking.registerGlobalReceiver(
         Correo.EnviarPayload.TYPE,
         (payload, context) -> context.server()
            .execute(() -> ServerPlayNetworking.send(context.player(), new Correo.ResultadoPayload(enviar(context.player(), payload))))
      );
      ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> enviarBuzon(handler.getPlayer()));
   }

   public record AbiertaPayload(int id) implements CustomPacketPayload {
      public static final Type<Correo.AbiertaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "correo_abierta"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Correo.AbiertaPayload> CODEC = StreamCodec.composite(ByteBufCodecs.INT, Correo.AbiertaPayload::id, Correo.AbiertaPayload::new);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record AccionPayload(String accion, int id) implements CustomPacketPayload {
      public static final Type<Correo.AccionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "correo_accion"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Correo.AccionPayload> CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8, Correo.AccionPayload::accion, ByteBufCodecs.VAR_INT, Correo.AccionPayload::id, Correo.AccionPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record BuzonPayload(List<Correo.CartaVista> cartas) implements CustomPacketPayload {
      public static final Type<Correo.BuzonPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "correo_buzon"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Correo.BuzonPayload> CODEC = Correo.CartaVista.CODEC
         .apply(ByteBufCodecs.list())
         .map(Correo.BuzonPayload::new, Correo.BuzonPayload::cartas);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   record Carta(int id, UUID autor, String autorNombre, UUID para, String asunto, String texto, List<ItemStack> objetos, long fecha) {
   }

   public record CartaVista(int id, String autor, String asunto, String texto, long segundos, boolean leida, boolean conObjetos) {
      public static final StreamCodec<RegistryFriendlyByteBuf, Correo.CartaVista> CODEC = StreamCodec.of(
         (buf, c) -> {
            buf.writeVarInt(c.id);
            buf.writeUtf(c.autor);
            buf.writeUtf(c.asunto);
            buf.writeUtf(c.texto, 2000);
            buf.writeVarLong(c.segundos);
            buf.writeBoolean(c.leida);
            buf.writeBoolean(c.conObjetos);
         },
         buf -> new Correo.CartaVista(
            buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readUtf(2000), buf.readVarLong(), buf.readBoolean(), buf.readBoolean()
         )
      );
   }

   static final class Datos extends SavedData {
      static final Factory<Correo.Datos> FACTORY = new Factory<>(Correo.Datos::new, Correo.Datos::cargar, net.minecraft.util.datafix.DataFixTypes.LEVEL);
      int siguiente = 1;
      final List<Correo.Carta> cartas = new ArrayList<>();
      final Map<UUID, Set<Integer>> leidas = new HashMap<>();
      final Map<UUID, Set<Integer>> borradas = new HashMap<>();
      final Map<UUID, Map<Integer, List<ItemStack>>> restantes = new HashMap<>();

      static boolean tiene(Map<UUID, Set<Integer>> m, UUID j, int id) {
         Set<Integer> s = m.get(j);
         return s != null && s.contains(id);
      }

      void marcar(Map<UUID, Set<Integer>> m, UUID j, int id) {
         if (m.computeIfAbsent(j, k -> new HashSet<>()).add(id)) {
            this.setDirty();
         }
      }

      List<ItemStack> restantes(UUID j, Correo.Carta c) {
         List<ItemStack> l = this.restantes.getOrDefault(j, Map.of()).get(c.id());
         if (l != null) {
            return l;
         }

         List<ItemStack> copia = new ArrayList<>();

         for (ItemStack o : c.objetos()) {
            copia.add(o.copy());
         }

         return copia;
      }

      private static List<ItemStack> leerObjetos(ListTag l, Provider registros) {
         List<ItemStack> objetos = new ArrayList<>();

         for (Tag o : l) {
            ItemStack.parse(registros, o).ifPresent(objetos::add);
         }

         return objetos;
      }

      private static ListTag guardarObjetos(List<ItemStack> objetos, Provider registros) {
         ListTag l = new ListTag();

         for (ItemStack o : objetos) {
            if (!o.isEmpty()) {
               l.add(o.save(registros));
            }
         }

         return l;
      }

      private static Correo.Datos cargar(CompoundTag tag, Provider registros) {
         Correo.Datos d = new Correo.Datos();
         d.siguiente = Math.max(1, tag.getInt("siguiente"));

         for (Tag t : tag.getList("cartas", 10)) {
            CompoundTag c = (CompoundTag)t;
            d.cartas
               .add(
                  new Correo.Carta(
                     c.getInt("id"),
                     c.getUUID("autor"),
                     c.getString("autor_nombre"),
                     c.hasUUID("para") ? c.getUUID("para") : null,
                     c.getString("asunto"),
                     c.getString("texto"),
                     leerObjetos(c.getList("objetos", 10), registros),
                     c.getLong("fecha")
                  )
               );
         }

         leerMapa(tag.getCompound("leidas"), d.leidas);
         leerMapa(tag.getCompound("borradas"), d.borradas);
         CompoundTag r = tag.getCompound("restantes");

         for (String j : r.getAllKeys()) {
            Map<Integer, List<ItemStack>> m = new HashMap<>();
            CompoundTag rj = r.getCompound(j);

            for (String id : rj.getAllKeys()) {
               m.put(Integer.parseInt(id), leerObjetos(rj.getList(id, 10), registros));
            }

            d.restantes.put(UUID.fromString(j), m);
         }

         return d;
      }

      private static void leerMapa(CompoundTag tag, Map<UUID, Set<Integer>> m) {
         for (String k : tag.getAllKeys()) {
            Set<Integer> s = new HashSet<>();

            for (int i : tag.getIntArray(k)) {
               s.add(i);
            }

            m.put(UUID.fromString(k), s);
         }
      }

      private static CompoundTag guardarMapa(Map<UUID, Set<Integer>> m) {
         CompoundTag tag = new CompoundTag();
         m.forEach((j, s) -> tag.put(j.toString(), new IntArrayTag(s.stream().mapToInt(Integer::intValue).toArray())));
         return tag;
      }

      public CompoundTag save(CompoundTag tag, Provider registros) {
         tag.putInt("siguiente", this.siguiente);
         ListTag l = new ListTag();

         for (Correo.Carta c : this.cartas) {
            CompoundTag t = new CompoundTag();
            t.putInt("id", c.id());
            t.putUUID("autor", c.autor());
            t.putString("autor_nombre", c.autorNombre());
            if (c.para() != null) {
               t.putUUID("para", c.para());
            }

            t.putString("asunto", c.asunto());
            t.putString("texto", c.texto());
            t.put("objetos", guardarObjetos(c.objetos(), registros));
            t.putLong("fecha", c.fecha());
            l.add(t);
         }

         tag.put("cartas", l);
         tag.put("leidas", guardarMapa(this.leidas));
         tag.put("borradas", guardarMapa(this.borradas));
         CompoundTag r = new CompoundTag();
         this.restantes.forEach((j, m) -> {
            CompoundTag rj = new CompoundTag();
            m.forEach((id, objetos) -> rj.put(String.valueOf(id), guardarObjetos((List<ItemStack>)objetos, registros)));
            r.put(j.toString(), rj);
         });
         tag.put("restantes", r);
         return tag;
      }
   }

   public record EnviarPayload(String para, String texto) implements CustomPacketPayload {
      public static final Type<Correo.EnviarPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "correo_enviar"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Correo.EnviarPayload> CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8, Correo.EnviarPayload::para, ByteBufCodecs.stringUtf8(2000), Correo.EnviarPayload::texto, Correo.EnviarPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record ResultadoPayload(String error) implements CustomPacketPayload {
      public static final Type<Correo.ResultadoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "correo_resultado"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Correo.ResultadoPayload> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Correo.ResultadoPayload::error, Correo.ResultadoPayload::new);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
