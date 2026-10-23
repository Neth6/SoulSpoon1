package neth6.soulspoon.candados;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class CandadosData extends SavedData {
   public static final Factory<CandadosData> FACTORY = new Factory<>(CandadosData::new, CandadosData::cargar, net.minecraft.util.datafix.DataFixTypes.LEVEL);
   private final Map<BlockPos, CandadosData.Cerradura> cerraduras = new HashMap<>();

   public CandadosData.Cerradura cerradura(BlockPos pos) {
      return this.cerraduras.get(pos);
   }

   public void poner(BlockPos pos, String codigo, UUID duenio) {
      Set<UUID> autorizados = new HashSet<>();
      autorizados.add(duenio);
      this.cerraduras.put(pos.immutable(), new CandadosData.Cerradura(codigo, duenio, autorizados));
      this.setDirty();
   }

   public void sacar(BlockPos pos) {
      if (this.cerraduras.remove(pos) != null) {
         this.setDirty();
      }
   }

   public void mover(BlockPos pos, CandadosData.Cerradura cerradura) {
      this.cerraduras.put(pos.immutable(), cerradura);
      this.setDirty();
   }

   public void autorizar(BlockPos pos, UUID jugador) {
      CandadosData.Cerradura cerradura = this.cerraduras.get(pos);
      if (cerradura != null && cerradura.autorizados().add(jugador)) {
         this.setDirty();
      }
   }

   public CompoundTag save(CompoundTag tag, Provider registros) {
      ListTag lista = new ListTag();
      this.cerraduras.forEach((pos, cerradura) -> {
         CompoundTag entrada = new CompoundTag();
         entrada.putLong("pos", pos.asLong());
         entrada.putString("codigo", cerradura.codigo());
         entrada.putUUID("duenio", cerradura.duenio());
         ListTag autorizados = new ListTag();

         for (UUID uuid : cerradura.autorizados()) {
            CompoundTag u = new CompoundTag();
            u.putUUID("uuid", uuid);
            autorizados.add(u);
         }

         entrada.put("autorizados", autorizados);
         lista.add(entrada);
      });
      tag.put("cerraduras", lista);
      return tag;
   }

   private static CandadosData cargar(CompoundTag tag, Provider registros) {
      CandadosData data = new CandadosData();
      ListTag lista = tag.getList("cerraduras", 10);

      for (int i = 0; i < lista.size(); i++) {
         CompoundTag entrada = lista.getCompound(i);
         Set<UUID> autorizados = new HashSet<>();
         ListTag guardados = entrada.getList("autorizados", 10);

         for (int j = 0; j < guardados.size(); j++) {
            autorizados.add(guardados.getCompound(j).getUUID("uuid"));
         }

         data.cerraduras
            .put(BlockPos.of(entrada.getLong("pos")), new CandadosData.Cerradura(entrada.getString("codigo"), entrada.getUUID("duenio"), autorizados));
      }

      return data;
   }

   public List<BlockPos> posiciones() {
      return new ArrayList<>(this.cerraduras.keySet());
   }

   public record Cerradura(String codigo, UUID duenio, Set<UUID> autorizados) {
   }
}
