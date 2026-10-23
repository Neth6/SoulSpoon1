package neth6.soulspoon.banco;

import neth6.soulspoon.economy.Coins;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CajeroMenu extends AbstractContainerMenu {
   public static final int BOTON_INGRESAR = 0;
   public static final int GRILLA_X = 22;
   public static final int GRILLA_Y = 20;
   public static final int CELDA = 22;
   public static final int INVENTARIO_Y = 102;
   private final Container grilla = new SimpleContainer(9);
   private final ContainerLevelAccess acceso;

   public CajeroMenu(int id, Inventory inventario) {
      this(id, inventario, ContainerLevelAccess.NULL);
   }

   public CajeroMenu(int id, Inventory inventario, ContainerLevelAccess acceso) {
      super(ModCajero.MENU, id);
      this.acceso = acceso;

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 3; col++) {
            this.addSlot(new Slot(this.grilla, fila * 3 + col, 22 + col * 22 + 2, 20 + fila * 22 + 2) {
               public boolean mayPlace(ItemStack pila) {
                  return CajeroMenu.valor(pila) > 0L;
               }
            });
         }
      }

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventario, col + fila * 9 + 9, 8 + col * 18, 102 + fila * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventario, col, 8 + col * 18, 160));
      }
   }

   public static long valor(ItemStack pila) {
      if (pila.isEmpty()) {
         return 0L;
      }

      if (pila.is(Coins.azul())) {
         return 1L;
      } else if (pila.is(Coins.verde())) {
         return 100L;
      } else {
         return pila.is(Coins.roja()) ? 1000L : (Coins.morada() != null && pila.is(Coins.morada()) ? 10000L : 0L);
      }
   }

   public boolean clickMenuButton(Player jugador, int boton) {
      if (boton != 0) {
         return false;
      } else if (!(jugador instanceof ServerPlayer servidor)) {
         return true;
      } else {
         long total = 0L;

         for (int i = 0; i < this.grilla.getContainerSize(); i++) {
            ItemStack pila = this.grilla.getItem(i);
            total += valor(pila) * pila.getCount();
         }

         if (total <= 0L) {
            servidor.level().playSound(null, servidor.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5F, 1.2F);
            return true;
         }

         for (int i = 0; i < this.grilla.getContainerSize(); i++) {
            if (valor(this.grilla.getItem(i)) > 0L) {
               this.grilla.setItem(i, ItemStack.EMPTY);
            }
         }

         Banco.sumar(servidor, total);
         servidor.level().playSound(null, servidor.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
         this.broadcastChanges();
         return true;
      }
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
      } else if (valor(pila) <= 0L || !this.moveItemStackTo(pila, 0, 9, false)) {
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
      return stillValid(this.acceso, jugador, ModCajero.CAJERO);
   }

   public void removed(Player jugador) {
      super.removed(jugador);
      this.clearContainer(jugador, this.grilla);
   }
}
