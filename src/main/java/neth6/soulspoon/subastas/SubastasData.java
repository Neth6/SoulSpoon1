package neth6.soulspoon.subastas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class SubastasData extends SavedData {
   public static final Factory<SubastasData> FACTORY = new Factory<>(SubastasData::new, SubastasData::cargar, net.minecraft.util.datafix.DataFixTypes.LEVEL);
   private final List<SubastasData.Venta> ventas = new ArrayList<>();
   private final Map<UUID, List<ItemStack>> recuperar = new HashMap<>();
   private final Map<UUID, Long> cobros = new HashMap<>();
   private int siguienteVitrina;

   /** Deditas ganadas por un vendedor que estaba desconectado; se pagan al entrar. */
   public void agregarCobro(UUID jugador, long cantidad) {
      this.cobros.merge(jugador, cantidad, Long::sum);
      this.setDirty();
   }

   public long sacarCobro(UUID jugador) {
      Long v = this.cobros.remove(jugador);
      if (v != null) {
         this.setDirty();
      }

      return v == null ? 0L : v;
   }

   public int nuevoNumeroDeVitrina() {
      this.setDirty();
      return this.siguienteVitrina++;
   }

   public List<SubastasData.Venta> ventas() {
      return this.ventas;
   }

   public void agregar(SubastasData.Venta venta) {
      this.ventas.add(0, venta);
      this.setDirty();
   }

   public SubastasData.Venta buscar(UUID id) {
      for (SubastasData.Venta v : this.ventas) {
         if (v.id().equals(id)) {
            return v;
         }
      }

      return null;
   }

   public boolean quitar(SubastasData.Venta venta) {
      boolean estaba = this.ventas.remove(venta);
      if (estaba) {
         this.setDirty();
      }

      return estaba;
   }

   public List<ItemStack> recuperar(UUID jugador) {
      return this.recuperar.getOrDefault(jugador, List.of());
   }

   public void guardarParaRecuperar(UUID jugador, ItemStack item) {
      this.recuperar.computeIfAbsent(jugador, k -> new ArrayList<>()).add(item);
      this.setDirty();
   }

   public ItemStack sacarDeRecuperar(UUID jugador, int i) {
      List<ItemStack> l = this.recuperar.get(jugador);
      if (l != null && i >= 0 && i < l.size()) {
         ItemStack item = l.remove(i);
         if (l.isEmpty()) {
            this.recuperar.remove(jugador);
         }

         this.setDirty();
         return item;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public long cuantasDe(UUID vendedor) {
      return this.ventas.stream().filter(v -> v.vendedor().equals(vendedor)).count();
   }

   private static SubastasData cargar(CompoundTag tag, Provider registros) {
      SubastasData data = new SubastasData();
      data.siguienteVitrina = tag.getInt("siguienteVitrina");

      for (Tag t : tag.getList("ventas", 10)) {
         CompoundTag v = (CompoundTag)t;
         ItemStack item = ItemStack.parse(registros, v.getCompound("item")).orElse(ItemStack.EMPTY);
         if (!item.isEmpty()) {
            data.ventas
               .add(new SubastasData.Venta(v.getUUID("id"), v.getUUID("vendedor"), v.getString("nombre"), item, v.getLong("precio"), v.getLong("cuando")));
         }
      }

      CompoundTag rec = tag.getCompound("recuperar");

      for (String jugador : rec.getAllKeys()) {
         List<ItemStack> l = new ArrayList<>();

         for (Tag t : rec.getList(jugador, 10)) {
            ItemStack.parse(registros, t).ifPresent(l::add);
         }

         if (!l.isEmpty()) {
            data.recuperar.put(UUID.fromString(jugador), l);
         }
      }

      CompoundTag cob = tag.getCompound("cobros");

      for (String k : cob.getAllKeys()) {
         data.cobros.put(UUID.fromString(k), cob.getLong(k));
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag, Provider registros) {
      ListTag lista = new ListTag();

      for (SubastasData.Venta v : this.ventas) {
         CompoundTag t = new CompoundTag();
         t.putUUID("id", v.id());
         t.putUUID("vendedor", v.vendedor());
         t.putString("nombre", v.nombreVendedor());
         t.put("item", v.item().save(registros));
         t.putLong("precio", v.precio());
         t.putLong("cuando", v.cuando());
         lista.add(t);
      }

      tag.put("ventas", lista);
      CompoundTag rec = new CompoundTag();
      this.recuperar.forEach((jugador, l) -> {
         ListTag objetos = new ListTag();

         for (ItemStack o : l) {
            if (!o.isEmpty()) {
               objetos.add(o.save(registros));
            }
         }

         rec.put(jugador.toString(), objetos);
      });
      tag.put("recuperar", rec);
      CompoundTag cob = new CompoundTag();
      this.cobros.forEach((u, c) -> cob.putLong(u.toString(), c));
      tag.put("cobros", cob);
      tag.putInt("siguienteVitrina", this.siguienteVitrina);
      return tag;
   }

   public record Venta(UUID id, UUID vendedor, String nombreVendedor, ItemStack item, long precio, long cuando) {
   }
}
