package neth6.soulspoon.subastas;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VitrinaBlockEntity extends BlockEntity {
   private static final int REVISAR = 40;
   private static final int CAMBIAR = 600;
   private int numero = -1;
   private ItemStack item = ItemStack.EMPTY;
   private long precio;
   private String vendedor = "";

   public VitrinaBlockEntity(BlockPos pos, BlockState estado) {
      super(ModVitrina.ENTIDAD, pos, estado);
   }

   public ItemStack item() {
      return this.item;
   }

   public long precio() {
      return this.precio;
   }

   public String vendedor() {
      return this.vendedor;
   }

   static void tick(Level level, BlockPos pos, BlockState estado, VitrinaBlockEntity vitrina) {
      if (level instanceof ServerLevel mundo && mundo.getGameTime() % 40L == 0L) {
         SubastasData data = Subastas.data(mundo.getServer());
         if (vitrina.numero < 0) {
            vitrina.numero = data.nuevoNumeroDeVitrina();
            vitrina.setChanged();
         }

         List<SubastasData.Venta> ventas = data.ventas();
         ItemStack nuevo = ItemStack.EMPTY;
         long nuevoPrecio = 0L;
         String nuevoVendedor = "";
         if (!ventas.isEmpty()) {
            long turno = mundo.getGameTime() / 600L;
            SubastasData.Venta venta = ventas.get((int)((vitrina.numero + turno) % ventas.size()));
            nuevo = venta.item();
            nuevoPrecio = venta.precio();
            nuevoVendedor = venta.nombreVendedor();
         }

         if (!ItemStack.matches(nuevo, vitrina.item) || nuevoPrecio != vitrina.precio || !nuevoVendedor.equals(vitrina.vendedor)) {
            vitrina.item = nuevo.copy();
            vitrina.precio = nuevoPrecio;
            vitrina.vendedor = nuevoVendedor;
            vitrina.setChanged();
            mundo.sendBlockUpdated(pos, estado, estado, 3);
         }
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registros) {
      super.saveAdditional(tag, registros);
      tag.putInt("numero", this.numero);
      if (!this.item.isEmpty()) {
         tag.put("item", this.item.save(registros));
      }

      tag.putLong("precio", this.precio);
      tag.putString("vendedor", this.vendedor);
   }

   protected void loadAdditional(CompoundTag tag, Provider registros) {
      super.loadAdditional(tag, registros);
      this.numero = tag.contains("numero") ? tag.getInt("numero") : -1;
      this.item = tag.contains("item") ? ItemStack.parse(registros, tag.getCompound("item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
      this.precio = tag.getLong("precio");
      this.vendedor = tag.getString("vendedor");
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registros) {
      return this.saveWithoutMetadata(registros);
   }
}
