package neth6.soulspoon.subastas;

import neth6.soulspoon.banco.Banco;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class Subastas {
   public static final int MAXIMO_POR_JUGADOR = 14;
   public static final long PRECIO_MAXIMO = 1000000000L;
   public static final MenuType<MenuSubastas> MENU = (MenuType<MenuSubastas>)Registry.register(
      BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath("soulspoon", "subastas"), new MenuType<>(MenuSubastas::new, FeatureFlags.VANILLA_SET)
   );

   private Subastas() {
   }

   public static void registrarRed() {
      PayloadTypeRegistry.playC2S().register(Subastas.PrecioPayload.TYPE, Subastas.PrecioPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(Subastas.PrecioPayload.TYPE, (payload, contexto) -> contexto.server().execute(() -> {
         if (contexto.player().containerMenu instanceof MenuSubastas menu) {
            menu.venderDesdeCasilla(Math.max(1L, Math.min(1000000000L, payload.precio())));
         }
      }));
   }

   /** Paga al vendedor: al instante si esta conectado, o al entrar si no. */
   static void pagarVendedor(MinecraftServer server, java.util.UUID vendedor, long precio) {
      ServerPlayer online = server.getPlayerList().getPlayer(vendedor);
      if (online != null) {
         Banco.sumar(online, precio);
      } else {
         data(server).agregarCobro(vendedor, precio);
      }
   }

   /** Llamar al entrar un jugador: cobra las ventas que hizo estando desconectado. */
   public static void entregarCobros(ServerPlayer jugador) {
      long total = data(jugador.server).sacarCobro(jugador.getUUID());
      if (total > 0) {
         Banco.sumar(jugador, total);
         jugador.sendSystemMessage(Component.literal("Mientras no estabas vendiste en las subastas: +" + total + " deditas.").withStyle(ChatFormatting.GOLD));
      }
   }

   static SubastasData data(MinecraftServer server) {
      return (SubastasData)server.overworld().getDataStorage().computeIfAbsent(SubastasData.FACTORY, "soulspoon_subastas");
   }

   public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ah").executes(ctx -> {
               abrir(((CommandSourceStack)ctx.getSource()).getPlayerOrException(), 0);
               return 1;
            }))
            .then(
               Commands.literal("vender")
                  .then(
                     Commands.argument("precio", LongArgumentType.longArg(1L, 1000000000L))
                        .executes(ctx -> vender(((CommandSourceStack)ctx.getSource()).getPlayerOrException(), LongArgumentType.getLong(ctx, "precio")))
                  )
            )
      );
   }

   public static void abrir(ServerPlayer jugador, int pagina) {
      jugador.openMenu(new SimpleMenuProvider((id, inventario, p) -> MenuSubastas.abrir(id, inventario), Component.literal("Subastas")));
   }

   private static int vender(ServerPlayer jugador, long precio) {
      ItemStack enMano = jugador.getMainHandItem();
      if (enMano.isEmpty()) {
         jugador.sendSystemMessage(Component.literal("Tienes que tener en la mano lo que quieres vender.").withStyle(ChatFormatting.RED));
         return 0;
      }

      if (!vender(jugador, enMano.copy(), precio)) {
         return 0;
      }

      enMano.setCount(0);
      return 1;
   }

   static boolean vender(ServerPlayer jugador, ItemStack item, long precio) {
      SubastasData data = data(jugador.server);
      if (data.cuantasDe(jugador.getUUID()) >= 14L) {
         jugador.sendSystemMessage(Component.literal("Ya tienes 14 cosas a la venta. Saca alguna desde Mis ventas.").withStyle(ChatFormatting.RED));
         return false;
      } else {
         data.agregar(
            new SubastasData.Venta(UUID.randomUUID(), jugador.getUUID(), jugador.getGameProfile().getName(), item, precio, jugador.serverLevel().getGameTime())
         );
         jugador.sendSystemMessage(
            Component.literal("Pusiste a la venta ")
               .withStyle(ChatFormatting.GREEN)
               .append(item.getHoverName().copy().withStyle(ChatFormatting.WHITE))
               .append(Component.literal(" x" + item.getCount() + " por " + precio + " deditas.").withStyle(ChatFormatting.GREEN))
         );
         jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
         return true;
      }
   }

   static void recuperar(ServerPlayer jugador, int i) {
      SubastasData data = data(jugador.server);
      ItemStack item = data.sacarDeRecuperar(jugador.getUUID(), i);
      if (!item.isEmpty()) {
         ItemStack copia = item.copy();
         if (!jugador.getInventory().add(copia)) {
            if (!copia.isEmpty()) {
               data.guardarParaRecuperar(jugador.getUUID(), copia);
            }

            jugador.displayClientMessage(Component.literal("No tienes lugar en el inventario.").withStyle(ChatFormatting.RED), true);
         } else {
            jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.0F);
         }
      }
   }

   static boolean tocar(ServerPlayer jugador, UUID idVenta) {
      SubastasData data = data(jugador.server);
      SubastasData.Venta venta = data.buscar(idVenta);
      if (venta == null) {
         jugador.sendSystemMessage(Component.literal("Esa venta ya no está.").withStyle(ChatFormatting.RED));
         return true;
      }

      if (venta.vendedor().equals(jugador.getUUID())) {
         data.quitar(venta);
         data.guardarParaRecuperar(jugador.getUUID(), venta.item().copy());
         jugador.sendSystemMessage(
            Component.literal("Sacaste de la venta ")
               .withStyle(ChatFormatting.YELLOW)
               .append(venta.item().getHoverName())
               .append(Component.literal(". Está en Recuperar.").withStyle(ChatFormatting.YELLOW))
         );
         return true;
      }

      if (!Banco.cobrar(jugador, venta.precio())) {
         jugador.sendSystemMessage(
            Component.literal("No te alcanzan las deditas: cuesta " + venta.precio() + " y tienes " + Banco.saldo(jugador) + ".").withStyle(ChatFormatting.RED)
         );
         jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
         return false;
      }

      data.quitar(venta);
      darItem(jugador, venta.item().copy());
      pagarVendedor(jugador.server, venta.vendedor(), venta.precio());
      jugador.sendSystemMessage(
         Component.literal("Compraste ")
            .withStyle(ChatFormatting.GREEN)
            .append(venta.item().getHoverName().copy().withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" x" + venta.item().getCount() + " por " + venta.precio() + " deditas.").withStyle(ChatFormatting.GREEN))
      );
      jugador.serverLevel().playSound(null, jugador.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.5F);
      ServerPlayer vendedor = jugador.server.getPlayerList().getPlayer(venta.vendedor());
      if (vendedor != null) {
         vendedor.sendSystemMessage(
            Component.literal(jugador.getGameProfile().getName() + " te compró ")
               .withStyle(ChatFormatting.GOLD)
               .append(venta.item().getHoverName().copy().withStyle(ChatFormatting.WHITE))
               .append(Component.literal(" por " + venta.precio() + " deditas.").withStyle(ChatFormatting.GOLD))
         );
      }

      return true;
   }

   private static void darItem(ServerPlayer jugador, ItemStack item) {
      if (!jugador.getInventory().add(item) && !item.isEmpty()) {
         jugador.drop(item, false);
      }
   }

   public record PrecioPayload(long precio) implements CustomPacketPayload {
      public static final Type<Subastas.PrecioPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "subastas_precio"));
      public static final StreamCodec<ByteBuf, Subastas.PrecioPayload> CODEC = ByteBufCodecs.VAR_LONG
         .map(Subastas.PrecioPayload::new, Subastas.PrecioPayload::precio);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
