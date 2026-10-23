package neth6.soulspoon.casino;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import java.util.ArrayList;
import java.util.List;
import neth6.soulspoon.economy.Dedita;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Premios del casino.
 *   shift + clic izquierdo al casino -> tabla de premios (los admins la editan ahi mismo)
 *   /casino 0|1               -> 0: gira pero no da nada; 1: premios encendidos (por defecto)
 *   /casino precio [azules]   -> ver o cambiar lo que cuesta cada giro (100 = 1 verde)
 *   /casino reset             -> vuelve a la tabla de fabrica
 */
public final class CasinoPremios {
   private CasinoPremios() {
   }

   public static Modo modo(MinecraftServer server) {
      return server.overworld().getDataStorage().computeIfAbsent(Modo.FACTORY, "soulspoon_casino");
   }

   public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(Commands.literal("casino")
         .requires(s -> s.hasPermission(2))
         .then(Commands.literal("precio")
            .executes(c -> {
               long p = modo(c.getSource().getServer()).precio;
               c.getSource().sendSuccess(() -> Component.literal("Cada giro cuesta " + p + " (" + Dedita.describe(p) + ") o 1 dedita naranja."), false);
               return 1;
            })
            .then(Commands.argument("azules", LongArgumentType.longArg(0))
               .executes(c -> {
                  long p = LongArgumentType.getLong(c, "azules");
                  Modo m = modo(c.getSource().getServer());
                  m.precio = p;
                  m.setDirty();
                  c.getSource().sendSuccess(() -> Component.literal("Casino: cada giro cuesta " + p + " (" + Dedita.describe(p) + ").")
                     .withStyle(ChatFormatting.GOLD), true);
                  return 1;
               })))
         .then(Commands.literal("reset").executes(c -> {
            Modo m = modo(c.getSource().getServer());
            m.pool = null;
            m.setDirty();
            c.getSource().sendSuccess(() -> Component.literal("Casino: tabla de premios restablecida.").withStyle(ChatFormatting.GOLD), true);
            return 1;
         }))
         .then(Commands.argument("tabla", IntegerArgumentType.integer(0, 1))
            .executes(c -> {
               int tabla = IntegerArgumentType.getInteger(c, "tabla");
               Modo m = modo(c.getSource().getServer());
               m.modo = tabla;
               m.setDirty();
               c.getSource().sendSuccess(() -> Component.literal(tabla == 0
                  ? "Casino: sin premios (las ruedas giran pero no dan nada)."
                  : "Casino: premios prendidos.").withStyle(ChatFormatting.GOLD), true);
               return 1;
            })));
   }

   /** Entrega el premio: sale por delante del casino hacia el jugador. */
   public static void entregar(ServerPlayer jugador, ItemStack premio, Entity casino) {
      if (premio.isEmpty() || modo(jugador.server).modo != 1) {
         return;
      }
      ItemStack resto = premio.copy();
      while (!resto.isEmpty()) {
         ItemStack parte = resto.split(resto.getMaxStackSize());
         if (casino != null) {
            tirar(casino, jugador, parte);
         } else if (!jugador.getInventory().add(parte)) {
            jugador.drop(parte, false);
         }
      }
      jugador.sendSystemMessage(Component.literal("¡Casino! Ganaste " + premio.getCount() + " × " + premio.getHoverName().getString() + ".")
         .withStyle(ChatFormatting.GOLD));
   }

   private static void tirar(Entity casino, ServerPlayer jugador, ItemStack item) {
      float giro = casino.getYRot() * (float) (Math.PI / 180.0);
      double frenteX = -Mth.sin(giro);
      double frenteZ = Mth.cos(giro);
      double x = casino.getX() + frenteX * 0.9;
      double y = casino.getY() + 0.55;
      double z = casino.getZ() + frenteZ * 0.9;
      ItemEntity tirado = new ItemEntity(casino.level(), x, y, z, item);
      double dx = jugador.getX() - x;
      double dz = jugador.getZ() - z;
      double largo = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
      double fuerza = Math.min(0.3, 0.08 + largo * 0.05);
      tirado.setDeltaMovement(dx / largo * fuerza, 0.25, dz / largo * fuerza);
      tirado.setPickUpDelay(10);
      casino.level().addFreshEntity(tirado);
   }

   public static final class Modo extends SavedData {
      public static final Factory<Modo> FACTORY = new Factory<>(Modo::new, Modo::leer, DataFixTypes.LEVEL);
      int modo = 1;
      long precio = ModCasino.PRECIO_INICIAL;
      /** null = tabla de fabrica */
      List<CasinoPool.Entry> pool = null;

      public List<CasinoPool.Entry> tabla() {
         return this.pool != null ? this.pool : CasinoPool.porDefecto();
      }

      private static Modo leer(CompoundTag tag, Provider registros) {
         Modo m = new Modo();
         if (tag.contains("modo")) m.modo = tag.getInt("modo");
         if (tag.contains("precio")) m.precio = tag.getLong("precio");
         if (tag.contains("tabla")) {
            List<CasinoPool.Entry> lista = new ArrayList<>();
            for (Tag t : tag.getList("tabla", 10)) {
               CompoundTag e = (CompoundTag) t;
               int peso = e.getInt("peso");
               if (e.getBoolean("nada")) {
                  lista.add(new CasinoPool.Entry(ItemStack.EMPTY, peso));
               } else {
                  ItemStack.parse(registros, e.getCompound("item")).ifPresent(s -> lista.add(new CasinoPool.Entry(s, peso)));
               }
            }
            if (!lista.isEmpty()) m.pool = lista;
         }
         return m;
      }

      @Override
      public CompoundTag save(CompoundTag tag, Provider registros) {
         tag.putInt("modo", this.modo);
         tag.putLong("precio", this.precio);
         if (this.pool != null) {
            ListTag l = new ListTag();
            for (CasinoPool.Entry e : this.pool) {
               CompoundTag c = new CompoundTag();
               c.putInt("peso", e.weight());
               if (e.stack().isEmpty()) {
                  c.putBoolean("nada", true);
               } else {
                  c.put("item", e.stack().save(registros));
               }
               l.add(c);
            }
            tag.put("tabla", l);
         }
         return tag;
      }
   }
}
