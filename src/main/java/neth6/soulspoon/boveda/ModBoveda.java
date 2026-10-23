package neth6.soulspoon.boveda;

import neth6.soulspoon.banco.Banco;
import neth6.soulspoon.banco.CajeroMenu;
import neth6.soulspoon.hermandad.Hermandades;
import neth6.soulspoon.hermandad.HermandadesData;
import neth6.soulspoon.economy.Coins;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBoveda {
   public static final Block BOVEDA = (Block)Registry.register(
      BuiltInRegistries.BLOCK,
      id("boveda"),
      new BovedaBlock(
         Properties.of()
            .mapColor(MapColor.METAL)
            .strength(8.0F, 1200.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)
            .isSuffocating((e, m, p) -> false)
            .isViewBlocking((e, m, p) -> false)
      )
   );
   public static final Item BOVEDA_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, id("boveda"), new BlockItem(BOVEDA, new net.minecraft.world.item.Item.Properties()) {
         public void appendHoverText(ItemStack pila, TooltipContext contexto, List<Component> texto, TooltipFlag bandera) {
            texto.add(Component.literal("Puerta de 2x3. Guarda las deditas de tu Hermandad.").withColor(13029334));
         }
      }
   );
   public static final BlockEntityType<BovedaBlockEntity> ENTIDAD = (BlockEntityType<BovedaBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("boveda"), BlockEntityType.Builder.of(BovedaBlockEntity::new, BOVEDA).build(null)
   );
   public static final MenuType<BovedaMenu> MENU = (MenuType<BovedaMenu>)Registry.register(
      BuiltInRegistries.MENU, id("boveda"), new MenuType<>(BovedaMenu::new, FeatureFlags.VANILLA_SET)
   );
   public static final int MAXIMO_VIRTUAL = 1000000;

   private ModBoveda() {
   }

   private static ResourceLocation id(String nombre) {
      return ResourceLocation.fromNamespaceAndPath("soulspoon", nombre);
   }

   public static void initialize() {
      net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> e.accept(BOVEDA_ITEM));
      registrar();
   }

   public static void registrar() {
      PayloadTypeRegistry.playC2S().register(ModBoveda.AccionPayload.TYPE, ModBoveda.AccionPayload.CODEC);
      PayloadTypeRegistry.playS2C().register(ModBoveda.EstadoPayload.TYPE, ModBoveda.EstadoPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(
         ModBoveda.AccionPayload.TYPE, (payload, context) -> context.server().execute(() -> alAccion(context.player(), payload))
      );
   }

   public static String formatear(long cantidad) {
      return String.format(Locale.ROOT, "%,d", cantidad).replace(',', '.');
   }

   static void abrirMenu(ServerPlayer jugador, BlockPos pos) {
      Optional<HermandadesData.Hermandad> h = Hermandades.data(jugador).deJugador(jugador.getUUID());
      if (!h.isEmpty() && !(jugador.distanceToSqr(pos.above().getCenter()) > 64.0)) {
         jugador.openMenu(new SimpleMenuProvider((id, inventario, j) -> new BovedaMenu(id, inventario, pos), Component.literal("Bóveda")));
         enviarEstado(jugador, h.get(), "", false);
      }
   }

   private static void enviarEstado(ServerPlayer jugador, HermandadesData.Hermandad h, String mensaje, boolean error) {
      ServerPlayNetworking.send(jugador, new ModBoveda.EstadoPayload(h.nombre(), h.balance(), mensaje, error));
   }

   private static void refrescarAbiertas(ServerPlayer quien, HermandadesData.Hermandad h) {
      for (ServerPlayer otro : quien.server.getPlayerList().getPlayers()) {
         if (otro != quien && otro.containerMenu instanceof BovedaMenu && h.esMiembro(otro.getUUID())) {
            enviarEstado(otro, h, "", false);
         }
      }
   }

   private static void alAccion(ServerPlayer jugador, ModBoveda.AccionPayload accion) {
      if (jugador.containerMenu instanceof BovedaMenu menu && menu.stillValid(jugador)) {
         Optional<HermandadesData.Hermandad> hermandad = Hermandades.data(jugador).deJugador(jugador.getUUID());
         if (hermandad.isEmpty()) {
            jugador.closeContainer();
         } else {
            HermandadesData.Hermandad h = hermandad.get();
            int[] cantidades = new int[]{accion.rojas(), accion.verdes(), accion.deditas()};

            for (int c : cantidades) {
               if (c < 0 || c > 1000000) {
                  return;
               }
            }

            long escrito = cantidades[0] * 10000L + cantidades[1] * 100L + cantidades[2];
            if (accion.accion() == 0) {
               depositar(jugador, menu, h, escrito);
            } else if (accion.accion() == 1) {
               retirar(jugador, h, cantidades, escrito);
            }
         }
      }
   }

   private static void depositar(ServerPlayer jugador, BovedaMenu menu, HermandadesData.Hermandad h, long virtual) {
      long fisico = menu.valorCasillas();
      if (fisico + virtual <= 0L) {
         error(jugador, h, "Pon deditas en las casillas o escribe cuántas depositar.");
      } else if (virtual > 0L && !Banco.cobrar(jugador, virtual)) {
         error(jugador, h, "No tienes " + formatear(virtual) + " deditas en tu cuenta.");
      } else {
         menu.vaciarCasillas();
         long total = fisico + virtual;
         Hermandades.data(jugador).setBalance(h, h.balance() + total);
         jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
         Hermandades.avisarMiembros(
            jugador.server, h, jugador.getGameProfile().getName() + " colocó " + formatear(total) + " deditas en la Bóveda.", ChatFormatting.GOLD
         );
         enviarEstado(jugador, h, "Depositaste " + formatear(total) + " deditas.", false);
         refrescarAbiertas(jugador, h);
      }
   }

   private static void retirar(ServerPlayer jugador, HermandadesData.Hermandad h, int[] cantidades, long costo) {
      if (!h.puedePublicar(jugador.getUUID())) {
         error(jugador, h, "Solo el Maestro y los Líderes pueden retirar.");
      } else if (costo <= 0L) {
         error(jugador, h, "Escribe cuántas monedas retirar.");
      } else {
         for (int c : cantidades) {
            if (c > 3564) {
               error(jugador, h, "Máximo 3564 de cada moneda por vez.");
               return;
            }
         }

         if (costo > h.balance()) {
            error(jugador, h, "La Bóveda no tiene tantas deditas.");
         } else {
            Hermandades.data(jugador).setBalance(h, h.balance() - costo);
            Item[] monedas = new Item[]{Coins.roja(), Coins.verde(), Coins.azul()};

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
            Hermandades.avisarMiembros(
               jugador.server, h, jugador.getGameProfile().getName() + " sacó " + formatear(costo) + " deditas de la Bóveda.", ChatFormatting.YELLOW
            );
            enviarEstado(jugador, h, "Retiraste " + formatear(costo) + " deditas.", false);
            refrescarAbiertas(jugador, h);
         }
      }
   }

   private static void error(ServerPlayer jugador, HermandadesData.Hermandad h, String mensaje) {
      jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5F, 1.2F);
      enviarEstado(jugador, h, mensaje, true);
   }

   static long valor(ItemStack pila) {
      return CajeroMenu.valor(pila);
   }

   public record AccionPayload(int accion, int rojas, int verdes, int deditas) implements CustomPacketPayload {
      public static final int DEPOSITAR = 0;
      public static final int RETIRAR = 1;
      public static final Type<ModBoveda.AccionPayload> TYPE = new Type(ModBoveda.id("boveda_accion"));
      public static final StreamCodec<ByteBuf, ModBoveda.AccionPayload> CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         ModBoveda.AccionPayload::accion,
         ByteBufCodecs.VAR_INT,
         ModBoveda.AccionPayload::rojas,
         ByteBufCodecs.VAR_INT,
         ModBoveda.AccionPayload::verdes,
         ByteBufCodecs.VAR_INT,
         ModBoveda.AccionPayload::deditas,
         ModBoveda.AccionPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record EstadoPayload(String hermandad, long balance, String mensaje, boolean error) implements CustomPacketPayload {
      public static final Type<ModBoveda.EstadoPayload> TYPE = new Type(ModBoveda.id("boveda_estado"));
      public static final StreamCodec<ByteBuf, ModBoveda.EstadoPayload> CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         ModBoveda.EstadoPayload::hermandad,
         ByteBufCodecs.VAR_LONG,
         ModBoveda.EstadoPayload::balance,
         ByteBufCodecs.STRING_UTF8,
         ModBoveda.EstadoPayload::mensaje,
         ByteBufCodecs.BOOL,
         ModBoveda.EstadoPayload::error,
         ModBoveda.EstadoPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
