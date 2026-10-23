package neth6.soulspoon.correo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CorreoMenu extends AbstractContainerMenu {
   public static final int ANCHO = 190;
   public static final int ALTO = 262;
   public static final int GRILLA_X = 14;
   public static final int GRILLA_Y = 102;
   public static final int CELDA_W = 18;
   public static final int CELDA_H = 18;
   public static final int COLUMNAS = 9;
   public static final int FILAS = 3;
   public static final int INVENTARIO_X = 15;
   public static final int INVENTARIO_Y = 180;
   final int carta;
   private final SimpleContainer grilla = new SimpleContainer(27);

   public CorreoMenu(int id, Inventory inventario) {
      this(id, inventario, Correo.ABIERTA_CLIENTE, List.of());
   }

   public CorreoMenu(int id, Inventory inventario, int carta, List<ItemStack> objetos) {
      super(Correo.MENU, id);
      this.carta = carta;

      for (int i = 0; i < objetos.size() && i < 27; i++) {
         this.grilla.setItem(i, objetos.get(i).copy());
      }

      final boolean leyendo = carta >= 0;

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(this.grilla, fila * 9 + col, 14 + col * 18 + 1, 102 + fila * 18 + 1) {
               public boolean mayPlace(ItemStack pila) {
                  return !leyendo;
               }
            });
         }
      }

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventario, col + fila * 9 + 9, 15 + col * 18, 180 + fila * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventario, col, 15 + col * 18, 238));
      }
   }

   public boolean leyendo() {
      return this.carta >= 0;
   }

   public int carta() {
      return this.carta;
   }

   List<ItemStack> objetos() {
      List<ItemStack> l = new ArrayList<>();

      for (int i = 0; i < this.grilla.getContainerSize(); i++) {
         if (!this.grilla.getItem(i).isEmpty()) {
            l.add(this.grilla.getItem(i).copy());
         }
      }

      return l;
   }

   void vaciar() {
      this.grilla.clearContent();
   }

   public ItemStack quickMoveStack(Player jugador, int indice) {
      Slot slot = (Slot)this.slots.get(indice);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      }

      ItemStack pila = slot.getItem();
      ItemStack antes = pila.copy();
      if (indice < 27) {
         if (!this.moveItemStackTo(pila, 27, this.slots.size(), true)) {
            return ItemStack.EMPTY;
         }
      } else if (this.leyendo() || !this.moveItemStackTo(pila, 0, 27, false)) {
         return ItemStack.EMPTY;
      }

      if (pila.isEmpty()) {
         slot.setByPlayer(ItemStack.EMPTY);
      } else {
         slot.setChanged();
      }

      return antes;
   }

   public boolean stillValid(Player jugador) {
      return true;
   }

   public void removed(Player jugador) {
      super.removed(jugador);
      if (jugador instanceof ServerPlayer servidor) {
         if (this.leyendo()) {
            Correo.guardarRestantes(servidor, this.carta, this.objetos());
         } else {
            this.clearContainer(jugador, this.grilla);
         }
      }
   }
}
