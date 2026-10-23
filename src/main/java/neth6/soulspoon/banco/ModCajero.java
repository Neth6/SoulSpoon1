package neth6.soulspoon.banco;

import neth6.soulspoon.economy.Coins;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class ModCajero {
   public static final Block CAJERO = (Block)Registry.register(
      BuiltInRegistries.BLOCK,
      id("cajero"),
      new CajeroBlock(
         Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0F, 1200.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(estado -> 6)
      )
   );
   public static final Item CAJERO_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, id("cajero"), new BlockItem(CAJERO, new net.minecraft.world.item.Item.Properties())
   );
   public static final MenuType<CajeroMenu> MENU = (MenuType<CajeroMenu>)Registry.register(
      BuiltInRegistries.MENU, id("cajero"), new MenuType<>(CajeroMenu::new, FeatureFlags.VANILLA_SET)
   );
   public static final int MAXIMO_POR_MONEDA = 3564;

   private ModCajero() {
   }

   private static ResourceLocation id(String nombre) {
      return ResourceLocation.fromNamespaceAndPath("soulspoon", nombre);
   }

   public static void initialize() {
      net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> e.accept(CAJERO_ITEM));
      registrar();
   }

   public static void registrar() {
      PayloadTypeRegistry.playC2S().register(ModCajero.IngresarPayload.TYPE, ModCajero.IngresarPayload.CODEC);
      PayloadTypeRegistry.playC2S().register(ModCajero.SacarPayload.TYPE, ModCajero.SacarPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(
         ModCajero.IngresarPayload.TYPE, (payload, context) -> context.server().execute(() -> abrirIngresar(context.player(), payload.pos()))
      );
      ServerPlayNetworking.registerGlobalReceiver(
         ModCajero.SacarPayload.TYPE, (payload, context) -> context.server().execute(() -> sacar(context.player(), payload))
      );
   }

   private static boolean cercaDelCajero(ServerPlayer jugador, BlockPos pos) {
      return jugador.level().getBlockState(pos).is(CAJERO) && jugador.distanceToSqr(pos.getCenter()) <= 64.0;
   }

   private static void sacar(ServerPlayer jugador, ModCajero.SacarPayload pedido) {
      if (cercaDelCajero(jugador, pedido.pos())) {
         int[] cantidades = new int[]{pedido.rojas(), pedido.verdes(), pedido.deditas()};
         Item[] monedas = new Item[]{Coins.roja(), Coins.verde(), Coins.azul()};
         long costo = 0L;

         for (int i = 0; i < 3; i++) {
            if (cantidades[i] < 0 || cantidades[i] > 3564) {
               return;
            }

            costo += cantidades[i] * CajeroMenu.valor(new ItemStack(monedas[i]));
         }

         if (costo > 0L) {
            if (!Banco.cobrar(jugador, costo)) {
               jugador.displayClientMessage(Component.literal("No tienes suficientes deditas.").withColor(16733525), true);
            } else {
               for (int i = 0; i < 3; i++) {
                  int quedan = cantidades[i];

                  while (quedan > 0) {
                     int tanda = Math.min(monedas[i].getDefaultMaxStackSize(), quedan);
                     quedan -= tanda;
                     ItemStack pila = new ItemStack(monedas[i], tanda);
                     if (!jugador.getInventory().add(pila)) {
                        jugador.drop(pila, false);
                     }
                  }
               }

               jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 0.9F);
            }
         }
      }
   }

   private static void abrirIngresar(ServerPlayer jugador, BlockPos pos) {
      if (cercaDelCajero(jugador, pos)) {
         ContainerLevelAccess acceso = ContainerLevelAccess.create(jugador.level(), pos);
         jugador.openMenu(new SimpleMenuProvider((id, inventario, j) -> new CajeroMenu(id, inventario, acceso), Component.literal("Cajero")));
      }
   }

   public record IngresarPayload(BlockPos pos) implements CustomPacketPayload {
      public static final Type<ModCajero.IngresarPayload> TYPE = new Type<>(ModCajero.id("cajero_ingresar"));
      public static final StreamCodec<ByteBuf, ModCajero.IngresarPayload> CODEC = BlockPos.STREAM_CODEC
         .map(ModCajero.IngresarPayload::new, ModCajero.IngresarPayload::pos);

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record SacarPayload(BlockPos pos, int rojas, int verdes, int deditas) implements CustomPacketPayload {
      public static final Type<ModCajero.SacarPayload> TYPE = new Type<>(ModCajero.id("cajero_sacar"));
      public static final StreamCodec<ByteBuf, ModCajero.SacarPayload> CODEC = StreamCodec.composite(
         BlockPos.STREAM_CODEC,
         ModCajero.SacarPayload::pos,
         ByteBufCodecs.VAR_INT,
         ModCajero.SacarPayload::rojas,
         ByteBufCodecs.VAR_INT,
         ModCajero.SacarPayload::verdes,
         ByteBufCodecs.VAR_INT,
         ModCajero.SacarPayload::deditas,
         ModCajero.SacarPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
