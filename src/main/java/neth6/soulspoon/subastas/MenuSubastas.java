package neth6.soulspoon.subastas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public class MenuSubastas extends AbstractContainerMenu {
   public static final int ANCHO = 190;
   public static final int ALTO = 236;
   public static final int GRILLA_X = 14;
   public static final int GRILLA_Y = 24;
   public static final int COLUMNAS = 9;
   public static final int FILAS = 5;
   public static final int POR_PAGINA = 45;
   public static final int PAGINAS_Y = 118;
   public static final int INVENTARIO_X = 15;
   public static final int INVENTARIO_Y = 152;
   public static final int VENTA_X = 87;
   public static final int VENTA_Y = 43;
   public static final int COMPRAR = 0;
   public static final int VENDER = 1;
   public static final int MIS_VENTAS = 2;
   public static final int RECUPERAR = 3;
   public static final int ANTERIOR = 10;
   public static final int SIGUIENTE = 11;
   private final Player jugador;
   private final SimpleContainer vidriera = new SimpleContainer(45);
   private final SimpleContainer venta = new SimpleContainer(1);
   private final ContainerData datos;
   private final UUID[] idPorCasillero = new UUID[45];
   private final int[] indicePorCasillero = new int[45];

   public MenuSubastas(int id, Inventory inventario) {
      this(id, inventario, new SimpleContainerData(3));
   }

   private MenuSubastas(int id, Inventory inventario, ContainerData datos) {
      super(Subastas.MENU, id);
      this.jugador = inventario.player;
      this.datos = datos;

      for (int fila = 0; fila < 5; fila++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(this.vidriera, fila * 9 + col, 14 + col * 18 + 1, 24 + fila * 18 + 1) {
               public boolean mayPlace(ItemStack pila) {
                  return false;
               }

               public boolean mayPickup(Player quien) {
                  return false;
               }

               public boolean isActive() {
                  return MenuSubastas.this.pestana() != 1;
               }
            });
         }
      }

      this.addSlot(new Slot(this.venta, 0, 87, 43) {
         public boolean isActive() {
            return MenuSubastas.this.pestana() == 1;
         }
      });

      for (int fila = 0; fila < 3; fila++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventario, col + fila * 9 + 9, 15 + col * 18, 152 + fila * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventario, col, 15 + col * 18, 210));
      }

      this.addDataSlots(datos);
   }

   public static MenuSubastas abrir(int id, Inventory inventario) {
      MenuSubastas menu = new MenuSubastas(id, inventario, new SimpleContainerData(3));
      menu.armar();
      return menu;
   }

   public int pestana() {
      return this.datos.get(0);
   }

   public int pagina() {
      return this.datos.get(1);
   }

   public int paginas() {
      return Math.max(1, this.datos.get(2));
   }

   void armar() {
      if (this.jugador instanceof ServerPlayer sp) {
         this.vidriera.clearContent();
         Arrays.fill(this.idPorCasillero, null);
         Arrays.fill(this.indicePorCasillero, -1);
         SubastasData var9 = Subastas.data(sp.server);
         ArrayList mostrar = new ArrayList();
         ArrayList ids = new ArrayList();
         switch (this.pestana()) {
            case 0:
            case 2:
               for (SubastasData.Venta v : var9.ventas()) {
                  if (this.pestana() != 2 || v.vendedor().equals(sp.getUUID())) {
                     mostrar.add(mostrar(v, sp));
                     ids.add(v.id());
                  }
               }
            case 1:
            default:
               break;
            case 3:
               for (ItemStack o : var9.recuperar(sp.getUUID())) {
                  ItemStack muestra = o.copy();
                  agregarLineas(
                     muestra, List.of(Component.literal("Clic para recuperarlo").withStyle(s -> s.withColor(ChatFormatting.GREEN).withItalic(false)))
                  );
                  mostrar.add(muestra);
                  ids.add(null);
               }
         }

         int paginas = Math.max(1, (mostrar.size() + 45 - 1) / 45);
         int pagina = Math.max(0, Math.min(this.datos.get(1), paginas - 1));

         for (int i = 0; i < 45; i++) {
            int indice = pagina * 45 + i;
            if (indice >= mostrar.size()) {
               break;
            }

            this.vidriera.setItem(i, (ItemStack)mostrar.get(indice));
            this.idPorCasillero[i] = (UUID)ids.get(indice);
            this.indicePorCasillero[i] = indice;
         }

         this.datos.set(1, pagina);
         this.datos.set(2, paginas);
         this.broadcastChanges();
      }
   }

   private static ItemStack mostrar(SubastasData.Venta v, ServerPlayer quien) {
      ItemStack muestra = v.item().copy();
      boolean mia = v.vendedor().equals(quien.getUUID());
      agregarLineas(
         muestra,
         List.of(
            Component.empty(),
            Component.literal("Precio: " + v.precio() + " deditas").withStyle(s -> s.withColor(ChatFormatting.GOLD).withItalic(false)),
            Component.literal("Vende: " + v.nombreVendedor()).withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(false)),
            mia
               ? Component.literal("Clic para sacarlo de la venta").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false))
               : Component.literal("Clic para comprar").withStyle(s -> s.withColor(ChatFormatting.GREEN).withItalic(false))
         )
      );
      return muestra;
   }

   private static void agregarLineas(ItemStack pila, List<Component> extra) {
      List<Component> lineas = new ArrayList<>();
      ItemLore antes = (ItemLore)pila.get(DataComponents.LORE);
      if (antes != null) {
         lineas.addAll(antes.lines());
      }

      lineas.addAll(extra);
      pila.set(DataComponents.LORE, new ItemLore(lineas));
   }

   public boolean clickMenuButton(Player quien, int boton) {
      if (boton >= 0 && boton <= 3) {
         if (this.pestana() == 1 && boton != 1) {
            this.devolverVenta();
         }

         this.datos.set(0, boton);
         this.datos.set(1, 0);
      } else if (boton == 10) {
         this.datos.set(1, Math.max(0, this.pagina() - 1));
      } else {
         if (boton != 11) {
            return false;
         }

         this.datos.set(1, this.pagina() + 1);
      }

      this.armar();
      return true;
   }

   public void clicked(int casillero, int boton, ClickType tipo, Player quien) {
      if (casillero >= 0 && casillero < 45) {
         if (quien instanceof ServerPlayer sp && tipo == ClickType.PICKUP && this.pestana() != 1) {
            if (this.pestana() == 3) {
               if (this.indicePorCasillero[casillero] >= 0) {
                  Subastas.recuperar(sp, this.indicePorCasillero[casillero]);
               }
            } else if (this.idPorCasillero[casillero] != null) {
               Subastas.tocar(sp, this.idPorCasillero[casillero]);
            }

            this.armar();
            this.sendAllDataToRemote();
         }
      } else {
         super.clicked(casillero, boton, tipo, quien);
      }
   }

   void venderDesdeCasilla(long precio) {
      if (this.jugador instanceof ServerPlayer sp && this.pestana() == 1) {
         ItemStack item = this.venta.getItem(0);
         if (item.isEmpty()) {
            sp.displayClientMessage(Component.literal("Pon en la casilla lo que quieres vender.").withStyle(ChatFormatting.RED), true);
         } else {
            if (Subastas.vender(sp, item.copy(), precio)) {
               this.venta.setItem(0, ItemStack.EMPTY);
               this.datos.set(0, 2);
               this.datos.set(1, 0);
               this.armar();
            }

            this.sendAllDataToRemote();
         }
      }
   }

   private void devolverVenta() {
      if (!this.venta.getItem(0).isEmpty()) {
         this.clearContainer(this.jugador, this.venta);
      }
   }

   public ItemStack quickMoveStack(Player quien, int indice) {
      Slot slot = (Slot)this.slots.get(indice);
      if (indice >= 45 && slot.hasItem()) {
         ItemStack pila = slot.getItem();
         ItemStack antes = pila.copy();
         int casillaVenta = 45;
         int inicioInventario = 46;
         if (indice == casillaVenta) {
            if (!this.moveItemStackTo(pila, inicioInventario, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.pestana() != 1 || !this.moveItemStackTo(pila, casillaVenta, casillaVenta + 1, false)) {
            return ItemStack.EMPTY;
         }

         if (pila.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         return antes;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean canDragTo(Slot casillero) {
      return casillero.container != this.vidriera;
   }

   public boolean stillValid(Player quien) {
      return true;
   }

   public void removed(Player quien) {
      super.removed(quien);
      this.vidriera.clearContent();
      if (quien instanceof ServerPlayer) {
         this.clearContainer(quien, this.venta);
      }
   }
}
