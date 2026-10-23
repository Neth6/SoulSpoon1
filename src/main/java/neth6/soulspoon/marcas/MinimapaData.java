package neth6.soulspoon.marcas;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class MinimapaData extends SavedData {
   public static final Factory<MinimapaData> FACTORY = new Factory<>(MinimapaData::new, MinimapaData::cargar, net.minecraft.util.datafix.DataFixTypes.LEVEL);
   private boolean adminsLoUsan;

   public boolean adminsLoUsan() {
      return this.adminsLoUsan;
   }

   public void setAdminsLoUsan(boolean valor) {
      this.adminsLoUsan = valor;
      this.setDirty();
   }

   private static MinimapaData cargar(CompoundTag tag, Provider registros) {
      MinimapaData data = new MinimapaData();
      data.adminsLoUsan = tag.getBoolean("admins");
      return data;
   }

   public CompoundTag save(CompoundTag tag, Provider registros) {
      tag.putBoolean("admins", this.adminsLoUsan);
      return tag;
   }
}
