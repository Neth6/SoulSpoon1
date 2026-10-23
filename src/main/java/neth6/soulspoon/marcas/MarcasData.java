package neth6.soulspoon.marcas;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class MarcasData extends SavedData {
   public static final Factory<MarcasData> FACTORY = new Factory<>(MarcasData::new, MarcasData::cargar, net.minecraft.util.datafix.DataFixTypes.LEVEL);
   private final List<MarcasPayload.Marca> marcas = new ArrayList<>();

   public List<MarcasPayload.Marca> todas() {
      return this.marcas;
   }

   public void poner(MarcasPayload.Marca marca) {
      this.marcas.removeIf(m -> m.nombre().equalsIgnoreCase(marca.nombre()));
      this.marcas.add(marca);
      this.setDirty();
   }

   public boolean borrar(String nombre) {
      boolean borrada = this.marcas.removeIf(m -> m.nombre().equalsIgnoreCase(nombre));
      if (borrada) {
         this.setDirty();
      }

      return borrada;
   }

   private static MarcasData cargar(CompoundTag tag, Provider registries) {
      MarcasData data = new MarcasData();

      for (Tag t : tag.getList("marcas", 10)) {
         CompoundTag c = (CompoundTag)t;
         data.marcas
            .add(new MarcasPayload.Marca(c.getString("nombre"), c.getString("dimension"), c.getInt("x"), c.getInt("y"), c.getInt("z"), c.getInt("color")));
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag lista = new ListTag();

      for (MarcasPayload.Marca m : this.marcas) {
         CompoundTag c = new CompoundTag();
         c.putString("nombre", m.nombre());
         c.putString("dimension", m.dimension());
         c.putInt("x", m.x());
         c.putInt("y", m.y());
         c.putInt("z", m.z());
         c.putInt("color", m.color());
         lista.add(c);
      }

      tag.put("marcas", lista);
      return tag;
   }
}
