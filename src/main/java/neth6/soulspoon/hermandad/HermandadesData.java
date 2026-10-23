package neth6.soulspoon.hermandad;

import neth6.soulspoon.marcas.MarcasPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class HermandadesData extends SavedData {
   public static final Factory<HermandadesData> FACTORY = new Factory<>(HermandadesData::new, HermandadesData::cargar, net.minecraft.util.datafix.DataFixTypes.LEVEL);
   private final List<HermandadesData.Hermandad> hermandades = new ArrayList<>();
   public static final int MAXIMO_ESTANDARTES = 27;
   public static final int LINEAS_CHAT = 100;
   public static final int MAXIMO_UBICACIONES = 16;
   public static final int MAXIMO_ANUNCIOS = 30;

   public List<HermandadesData.Hermandad> todas() {
      return this.hermandades;
   }

   public Optional<HermandadesData.Hermandad> deJugador(UUID jugador) {
      return this.hermandades.stream().filter(h -> h.esMiembro(jugador)).findFirst();
   }

   public Optional<HermandadesData.Hermandad> porNombre(String nombre) {
      return this.hermandades.stream().filter(h -> h.nombre().equalsIgnoreCase(nombre)).findFirst();
   }

   public boolean nombreUsado(String nombre) {
      return this.porNombre(nombre).isPresent();
   }

   public void quitar(HermandadesData.Hermandad hermandad) {
      this.hermandades.remove(hermandad);
      this.setDirty();
   }

   public void agregar(HermandadesData.Hermandad hermandad) {
      this.hermandades.add(hermandad);
      this.setDirty();
   }

   public void agregarMiembro(HermandadesData.Hermandad hermandad, UUID uuid, String nombre) {
      hermandad.miembros.add(new ManuscritoDatos.Inscrito(uuid, nombre));
      this.setDirty();
   }

   public void setBalance(HermandadesData.Hermandad hermandad, long balance) {
      hermandad.balance = Math.max(0L, balance);
      this.setDirty();
   }

   public void setColor(HermandadesData.Hermandad hermandad, int color) {
      hermandad.color = color & 16777215;
      this.setDirty();
   }

   public boolean guardarEstandarte(HermandadesData.Hermandad hermandad, ItemStack estandarte) {
      if (hermandad.estandartes.size() >= 27) {
         return false;
      }

      hermandad.estandartes.add(estandarte.copyWithCount(1));
      this.setDirty();
      return true;
   }

   public ItemStack sacarEstandarte(HermandadesData.Hermandad hermandad, int lugar) {
      if (lugar >= 0 && lugar < hermandad.estandartes.size()) {
         ItemStack sacado = hermandad.estandartes.remove(lugar);
         if (hermandad.activo == lugar) {
            hermandad.activo = -1;
         } else if (hermandad.activo > lugar) {
            hermandad.activo--;
         }

         this.setDirty();
         return sacado;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void activarEstandarte(HermandadesData.Hermandad hermandad, int lugar) {
      hermandad.activo = lugar >= 0 && lugar < hermandad.estandartes.size() ? lugar : -1;
      this.setDirty();
   }

   public void agregarChat(HermandadesData.Hermandad hermandad, HermandadChatPayload.Linea linea) {
      hermandad.chat.add(linea);

      while (hermandad.chat.size() > 100) {
         hermandad.chat.remove(0);
      }

      this.setDirty();
   }

   public void quitarMiembro(HermandadesData.Hermandad hermandad, UUID uuid) {
      hermandad.miembros.removeIf(m -> m.uuid().equals(uuid));
      hermandad.lideres.remove(uuid);
      this.setDirty();
   }

   public void setLider(HermandadesData.Hermandad hermandad, UUID uuid, boolean lider) {
      if (lider) {
         hermandad.lideres.add(uuid);
      } else {
         hermandad.lideres.remove(uuid);
      }

      this.setDirty();
   }

   public boolean agregarUbicacion(HermandadesData.Hermandad hermandad, MarcasPayload.Marca marca, UUID autor) {
      if (hermandad.ubicaciones.size() >= 16) {
         return false;
      }

      hermandad.ubicaciones.add(marca);
      hermandad.autoresUbicaciones.add(autor);
      this.setDirty();
      return true;
   }

   public void borrarUbicacion(HermandadesData.Hermandad hermandad, int lugar) {
      hermandad.ubicaciones.remove(lugar);
      hermandad.autoresUbicaciones.remove(lugar);
      this.setDirty();
   }

   public void publicarAnuncio(HermandadesData.Hermandad hermandad, HermandadTablonPayload.Anuncio anuncio) {
      hermandad.anuncios.add(0, anuncio);

      while (hermandad.anuncios.size() > 30) {
         hermandad.anuncios.remove(hermandad.anuncios.size() - 1);
      }

      this.setDirty();
   }

   public boolean borrarAnuncio(HermandadesData.Hermandad hermandad, int lugar) {
      if (lugar >= 0 && lugar < hermandad.anuncios.size()) {
         hermandad.anuncios.remove(lugar);
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   private static HermandadesData cargar(CompoundTag tag, Provider registries) {
      HermandadesData data = new HermandadesData();

      for (Tag t : tag.getList("hermandades", 10)) {
         CompoundTag h = (CompoundTag)t;
         List<ManuscritoDatos.Inscrito> miembros = new ArrayList<>();

         for (Tag m : h.getList("miembros", 10)) {
            CompoundTag c = (CompoundTag)m;
            miembros.add(new ManuscritoDatos.Inscrito(c.getUUID("uuid"), c.getString("nombre")));
         }

         HermandadesData.Hermandad hermandad = new HermandadesData.Hermandad(h.getString("nombre"), h.getUUID("maestro"), miembros);
         hermandad.balance = h.getLong("balance");
         hermandad.nivelBanco = Math.max(1, h.getInt("nivelBanco"));
         hermandad.motd = h.getString("motd");
         hermandad.color = h.contains("color") ? h.getInt("color") : 16777215;

         for (Tag l : h.getList("lideres", 11)) {
            hermandad.lideres.add(NbtUtils.loadUUID(l));
         }

         for (Tag a : h.getList("anuncios", 10)) {
            CompoundTag c = (CompoundTag)a;
            hermandad.anuncios.add(new HermandadTablonPayload.Anuncio(c.getString("fecha"), c.getString("autor"), c.getString("texto")));
         }

         for (Tag e : h.getList("estandartes", 10)) {
            ItemStack.parse(registries, e).ifPresent(hermandad.estandartes::add);
         }

         hermandad.activo = h.contains("activo") ? h.getInt("activo") : -1;

         for (Tag u : h.getList("ubicaciones", 10)) {
            CompoundTag c = (CompoundTag)u;
            hermandad.ubicaciones
               .add(new MarcasPayload.Marca(c.getString("nombre"), c.getString("dimension"), c.getInt("x"), c.getInt("y"), c.getInt("z"), c.getInt("color")));
            hermandad.autoresUbicaciones.add(c.hasUUID("autor") ? c.getUUID("autor") : hermandad.maestro);
         }

         if (hermandad.activo >= hermandad.estandartes.size()) {
            hermandad.activo = -1;
         }

         for (Tag l : h.getList("chat", 10)) {
            CompoundTag c = (CompoundTag)l;
            hermandad.chat.add(new HermandadChatPayload.Linea(c.getString("hora"), c.getString("autor"), c.getString("texto")));
         }

         data.hermandades.add(hermandad);
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag lista = new ListTag();

      for (HermandadesData.Hermandad hermandad : this.hermandades) {
         CompoundTag h = new CompoundTag();
         h.putString("nombre", hermandad.nombre);
         h.putUUID("maestro", hermandad.maestro);
         h.putLong("balance", hermandad.balance);
         h.putInt("nivelBanco", hermandad.nivelBanco);
         h.putString("motd", hermandad.motd);
         h.putInt("color", hermandad.color);
         ListTag miembros = new ListTag();

         for (ManuscritoDatos.Inscrito m : hermandad.miembros) {
            CompoundTag c = new CompoundTag();
            c.putUUID("uuid", m.uuid());
            c.putString("nombre", m.nombre());
            miembros.add(c);
         }

         h.put("miembros", miembros);
         ListTag chat = new ListTag();

         for (HermandadChatPayload.Linea linea : hermandad.chat) {
            CompoundTag c = new CompoundTag();
            c.putString("hora", linea.hora());
            c.putString("autor", linea.autor());
            c.putString("texto", linea.texto());
            chat.add(c);
         }

         h.put("chat", chat);
         ListTag lideres = new ListTag();

         for (UUID l : hermandad.lideres) {
            lideres.add(NbtUtils.createUUID(l));
         }

         h.put("lideres", lideres);
         ListTag anuncios = new ListTag();

         for (HermandadTablonPayload.Anuncio a : hermandad.anuncios) {
            CompoundTag c = new CompoundTag();
            c.putString("fecha", a.fecha());
            c.putString("autor", a.autor());
            c.putString("texto", a.texto());
            anuncios.add(c);
         }

         h.put("anuncios", anuncios);
         ListTag estandartes = new ListTag();

         for (ItemStack e : hermandad.estandartes) {
            estandartes.add(e.save(registries));
         }

         h.put("estandartes", estandartes);
         h.putInt("activo", hermandad.activo);
         ListTag ubicaciones = new ListTag();

         for (int i = 0; i < hermandad.ubicaciones.size(); i++) {
            MarcasPayload.Marca u = hermandad.ubicaciones.get(i);
            CompoundTag c = new CompoundTag();
            c.putString("nombre", u.nombre());
            c.putString("dimension", u.dimension());
            c.putInt("x", u.x());
            c.putInt("y", u.y());
            c.putInt("z", u.z());
            c.putInt("color", u.color());
            c.putUUID("autor", hermandad.autoresUbicaciones.get(i));
            ubicaciones.add(c);
         }

         h.put("ubicaciones", ubicaciones);
         lista.add(h);
      }

      tag.put("hermandades", lista);
      return tag;
   }

   public static final class Hermandad {
      private final String nombre;
      private final UUID maestro;
      private final List<ManuscritoDatos.Inscrito> miembros;
      private long balance;
      private int nivelBanco = 1;
      private String motd = "";
      private int color = 16777215;
      private final Set<UUID> lideres = new HashSet<>();
      private final List<HermandadTablonPayload.Anuncio> anuncios = new ArrayList<>();
      private final List<ItemStack> estandartes = new ArrayList<>();
      private int activo = -1;
      private final List<MarcasPayload.Marca> ubicaciones = new ArrayList<>();
      private final List<UUID> autoresUbicaciones = new ArrayList<>();
      private final List<HermandadChatPayload.Linea> chat = new ArrayList<>();

      public Hermandad(String nombre, UUID maestro, List<ManuscritoDatos.Inscrito> miembros) {
         this.nombre = nombre;
         this.maestro = maestro;
         this.miembros = new ArrayList<>(miembros);
      }

      public String nombre() {
         return this.nombre;
      }

      public UUID maestro() {
         return this.maestro;
      }

      public List<ManuscritoDatos.Inscrito> miembros() {
         return this.miembros;
      }

      public long balance() {
         return this.balance;
      }

      public int nivelBanco() {
         return this.nivelBanco;
      }

      public String motd() {
         return this.motd;
      }

      public int color() {
         return this.color;
      }

      public List<HermandadChatPayload.Linea> chat() {
         return this.chat;
      }

      public Set<UUID> lideres() {
         return this.lideres;
      }

      public List<HermandadTablonPayload.Anuncio> anuncios() {
         return this.anuncios;
      }

      public List<MarcasPayload.Marca> ubicaciones() {
         return this.ubicaciones;
      }

      public List<UUID> autoresUbicaciones() {
         return this.autoresUbicaciones;
      }

      public boolean esLider(UUID jugador) {
         return this.lideres.contains(jugador);
      }

      public boolean puedePublicar(UUID jugador) {
         return this.maestro.equals(jugador) || this.lideres.contains(jugador);
      }

      public List<ItemStack> estandartes() {
         return this.estandartes;
      }

      public int activo() {
         return this.activo;
      }

      public ItemStack estandarteActivo() {
         return this.activo >= 0 && this.activo < this.estandartes.size() ? this.estandartes.get(this.activo) : ItemStack.EMPTY;
      }

      public boolean esMiembro(UUID jugador) {
         return this.miembros.stream().anyMatch(m -> m.uuid().equals(jugador));
      }

      public String nombreMaestro() {
         return this.miembros.stream().filter(m -> m.uuid().equals(this.maestro)).map(ManuscritoDatos.Inscrito::nombre).findFirst().orElse("?");
      }
   }
}
