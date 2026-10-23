package neth6.soulspoon.boveda;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BovedaMenu extends AbstractContainerMenu {
   public static final int ANCHO = 210;
   public static final int SLOTS_X = 25;
   public static final int CASILLAS_Y = 9;
   public static final int INVENTARIO_Y = 98;
   private final Container casillas = new SimpleContainer(9);
   private final BlockPos pos;

   public BovedaMenu(int id, Inventory inventario) {
      this(id, inventario, BlockPos.ZERO);
   }

   public BovedaMenu(int id, Inventory inventario, BlockPos pos) {
      super(ModBoveda.MENU, id);
      this.pos = pos;

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(this.casillas, col, 25 + col * 18, 9) {
            public boolean mayPlace(ItemStack pila) {
               return ModBoveda.valor(pila) > 0L;
            }
         });
      }

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventario, col + fila * 9 + 9, 25 + col * 18, 98 + fila * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventario, col, 25 + col * 18, 156));
      }
   }

   public BlockPos pos() {
      return this.pos;
   }

   long valorCasillas() {
      long total = 0L;

      for (int i = 0; i < this.casillas.getContainerSize(); i++) {
         ItemStack pila = this.casillas.getItem(i);
         total += ModBoveda.valor(pila) * pila.getCount();
      }

      return total;
   }

   void vaciarCasillas() {
      for (int i = 0; i < this.casillas.getContainerSize(); i++) {
         if (ModBoveda.valor(this.casillas.getItem(i)) > 0L) {
            this.casillas.setItem(i, ItemStack.EMPTY);
         }
      }

      this.broadcastChanges();
   }

   public ItemStack quickMoveStack(Player jugador, int indice) {
      Slot slot = (Slot)this.slots.get(indice);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      }

      ItemStack pila = slot.getItem();
      ItemStack copia = pila.copy();
      if (indice < 9) {
         if (!this.moveItemStackTo(pila, 9, this.slots.size(), true)) {
            return ItemStack.EMPTY;
         }
      } else if (ModBoveda.valor(pila) <= 0L || !this.moveItemStackTo(pila, 0, 9, false)) {
         return ItemStack.EMPTY;
      }

      if (pila.isEmpty()) {
         slot.setByPlayer(ItemStack.EMPTY);
      } else {
         slot.setChanged();
      }

      return copia;
   }

   public boolean stillValid(Player jugador) {
      return jugador.level().getBlockState(this.pos).is(ModBoveda.BOVEDA) && jugador.distanceToSqr(this.pos.above().getCenter()) <= 64.0;
   }

   public void removed(Player jugador) {
      super.removed(jugador);
      this.clearContainer(jugador, this.casillas);
   }
}
