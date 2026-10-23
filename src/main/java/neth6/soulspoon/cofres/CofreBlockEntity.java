package neth6.soulspoon.cofres;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CofreBlockEntity extends RandomizableContainerBlockEntity implements LidBlockEntity {
   private NonNullList<ItemStack> cosas = NonNullList.withSize(54, ItemStack.EMPTY);
   private final ChestLidController tapa = new ChestLidController();
   private final ContainerOpenersCounter mirando = new ContainerOpenersCounter() {
      protected void onOpen(Level level, BlockPos pos, BlockState estado) {
         if (estado.getBlock() instanceof CofreBlock c) {
            level.playSound(null, pos, c.sonidoAbrir(), SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }

      protected void onClose(Level level, BlockPos pos, BlockState estado) {
         if (estado.getBlock() instanceof CofreBlock c) {
            level.playSound(null, pos, c.sonidoCerrar(), SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }

      protected void openerCountChanged(Level level, BlockPos pos, BlockState estado, int antes, int ahora) {
         level.blockEvent(pos, estado.getBlock(), 1, ahora);
      }

      protected boolean isOwnContainer(Player jugador) {
         return jugador.containerMenu instanceof ChestMenu menu && menu.getContainer() == CofreBlockEntity.this;
      }
   };

   public CofreBlockEntity(BlockPos pos, BlockState estado) {
      this(ModCofres.COFRE_ENTIDAD, pos, estado);
   }

   public CofreBlockEntity(BlockEntityType<?> tipo, BlockPos pos, BlockState estado) {
      super(tipo, pos, estado);
   }

   public static void tickCliente(Level level, BlockPos pos, BlockState estado, CofreBlockEntity cofre) {
      cofre.tapa.tickLid();
   }

   public boolean triggerEvent(int id, int dato) {
      if (id == 1) {
         this.tapa.shouldBeOpen(dato > 0);
         return true;
      } else {
         return super.triggerEvent(id, dato);
      }
   }

   public float getOpenNess(float parcial) {
      return this.tapa.getOpenness(parcial);
   }

   public void startOpen(Player jugador) {
      if (!this.remove && !jugador.isSpectator()) {
         this.mirando.incrementOpeners(jugador, this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   public void stopOpen(Player jugador) {
      if (!this.remove && !jugador.isSpectator()) {
         this.mirando.decrementOpeners(jugador, this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   public void revisarQuienMira() {
      if (!this.remove) {
         this.mirando.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
      }
   }

   protected Component getDefaultName() {
      return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
   }

   protected AbstractContainerMenu createMenu(int id, Inventory inventario) {
      return ChestMenu.sixRows(id, inventario, this);
   }

   public int getContainerSize() {
      return 54;
   }

   protected NonNullList<ItemStack> getItems() {
      return this.cosas;
   }

   protected void setItems(NonNullList<ItemStack> nuevas) {
      this.cosas = nuevas;
   }

   protected void loadAdditional(CompoundTag tag, Provider registros) {
      super.loadAdditional(tag, registros);
      this.cosas = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(tag)) {
         ContainerHelper.loadAllItems(tag, this.cosas, registros);
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registros) {
      super.saveAdditional(tag, registros);
      if (!this.trySaveLootTable(tag)) {
         ContainerHelper.saveAllItems(tag, this.cosas, registros);
      }
   }
}
