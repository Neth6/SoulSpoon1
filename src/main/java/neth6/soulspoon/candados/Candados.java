package neth6.soulspoon.candados;

import neth6.soulspoon.economy.Ids;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class Candados {
   public static final int LARGO_MAXIMO_CODIGO = 16;
   public static volatile Set<BlockPos> cerradosCliente = Set.of();
   private static final List<Runnable> PENDIENTES = new ArrayList<>();

   private Candados() {
   }

   public static CandadosData data(ServerLevel level) {
      return (CandadosData)level.getDataStorage().computeIfAbsent(CandadosData.FACTORY, "soulspoon_candados");
   }

   public static boolean sePuedeCerrar(BlockState estado) {
      return estado.getBlock() instanceof ChestBlock || estado.getBlock() instanceof BarrelBlock || estado.getBlock() instanceof ShulkerBoxBlock;
   }

   public static BlockPos principal(Level level, BlockPos pos) {
      BlockState estado = level.getBlockState(pos);
      if (estado.getBlock() instanceof ChestBlock && estado.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
         BlockPos vecino = pos.relative(ChestBlock.getConnectedDirection(estado));
         return pos.compareTo(vecino) <= 0 ? pos.immutable() : vecino.immutable();
      } else {
         return pos.immutable();
      }
   }

   public static boolean tieneCandado(Level level, BlockPos pos) {
      if (!sePuedeCerrar(level.getBlockState(pos))) {
         return false;
      }

      BlockPos clave = principal(level, pos);
      return level instanceof ServerLevel mundo ? data(mundo).cerradura(clave) != null : level.isClientSide && cerradosCliente.contains(clave);
   }

   public static void enviarLista(ServerPlayer jugador) {
      ServerPlayNetworking.send(jugador, new CandadosListaPayload(data(jugador.serverLevel()).posiciones()));
   }

   public static void avisarCambio(ServerLevel mundo) {
      for (ServerPlayer jugador : mundo.players()) {
         enviarLista(jugador);
      }
   }

   public static CandadosData.Cerradura cerradura(ServerLevel level, BlockPos pos) {
      return data(level).cerradura(principal(level, pos));
   }

   public static boolean puedeAbrir(ServerPlayer jugador, CandadosData.Cerradura cerradura) {
      UUID uuid = jugador.getUUID();
      return cerradura.duenio().equals(uuid) || cerradura.autorizados().contains(uuid);
   }

   private static void reenviarBloque(ServerPlayer jugador, ServerLevel mundo, BlockPos pos) {
      jugador.connection.send(new ClientboundBlockUpdatePacket(pos, mundo.getBlockState(pos)));
      BlockEntity conDatos = mundo.getBlockEntity(pos);
      if (conDatos != null) {
         Packet<ClientGamePacketListener> paquete = conDatos.getUpdatePacket();
         if (paquete != null) {
            jugador.connection.send(paquete);
         }
      }
   }

   public static void registrar() {
      Block[] cerrables = BuiltInRegistries.BLOCK
         .stream()
         .filter(b -> b instanceof ChestBlock || b instanceof BarrelBlock || b instanceof ShulkerBoxBlock)
         .toArray(Block[]::new);
      ItemStorage.SIDED
         .registerForBlocks((mundo, pos, estado, be, lado) -> mundo instanceof ServerLevel && tieneCandado(mundo, pos) ? Storage.empty() : null, cerrables);
      ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> enviarLista(handler.getPlayer()));
      ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((jugador, antes, despues) -> enviarLista(jugador));
      ServerPlayerEvents.AFTER_RESPAWN.register((viejo, nuevo, vivo) -> enviarLista(nuevo));
      ServerTickEvents.END_SERVER_TICK.register(server -> {
         if (server.getTickCount() % 100 == 0) {
            for (ServerLevel mundo : server.getAllLevels()) {
               limpiar(mundo);
            }
         }

         if (!PENDIENTES.isEmpty()) {
            List<Runnable> copia = new ArrayList<>(PENDIENTES);
            PENDIENTES.clear();
            copia.forEach(Runnable::run);
         }
      });
      UseBlockCallback.EVENT.register((jugador, level, mano, hit) -> {
         if (jugador instanceof ServerPlayer servidor && level instanceof ServerLevel mundo) {
            BlockPos pos = hit.getBlockPos();
            if (!sePuedeCerrar(mundo.getBlockState(pos))) {
               return InteractionResult.PASS;
            }

            BlockPos clave = principal(mundo, pos);
            CandadosData.Cerradura cerradura = data(mundo).cerradura(clave);
            if (jugador.getItemInHand(mano).is(Ids.item("llave_candado"))) {
               desproteger(servidor, mundo, clave, cerradura);
               return InteractionResult.SUCCESS;
            }

            if (cerradura == null) {
               if (!jugador.getItemInHand(mano).is(Ids.item("candado"))) {
                  return InteractionResult.PASS;
               }

               ServerPlayNetworking.send(servidor, new CandadoPantallaPayload(clave, true, ""));
               return InteractionResult.SUCCESS;
            } else {
               if (puedeAbrir(servidor, cerradura)) {
                  return InteractionResult.PASS;
               }

               ServerPlayNetworking.send(servidor, new CandadoPantallaPayload(clave, false, ""));
               mundo.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
               return InteractionResult.SUCCESS;
            }
         } else {
            return InteractionResult.PASS;
         }
      });
      PlayerBlockBreakEvents.BEFORE.register((level, jugador, pos, estado, entidad) -> {
         if (!(level instanceof ServerLevel mundo && jugador instanceof ServerPlayer servidor)) {
            return true;
         } else if (!sePuedeCerrar(estado)) {
            return true;
         } else {
            CandadosData.Cerradura cerradura = data(mundo).cerradura(principal(mundo, pos));
            if (cerradura != null && !puedeAbrir(servidor, cerradura)) {
               servidor.displayClientMessage(Component.literal("Este cofre está cerrado con un candado.").withStyle(ChatFormatting.RED), true);
               mundo.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
               BlockPos donde = pos.immutable();
               PENDIENTES.add(() -> reenviarBloque(servidor, mundo, donde));
               return false;
            } else {
               return true;
            }
         }
      });
      PlayerBlockBreakEvents.AFTER.register((level, jugador, pos, estado, entidad) -> {
         if (level instanceof ServerLevel mundo && sePuedeCerrar(estado)) {
            BlockPos donde = pos.immutable();
            CandadosData datos = data(mundo);
            CandadosData.Cerradura cerradura = datos.cerradura(donde);
            BlockPos otra = null;
            if (estado.getBlock() instanceof ChestBlock && estado.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
               otra = donde.relative(ChestBlock.getConnectedDirection(estado));
            }

            boolean habia = cerradura != null || otra != null && datos.cerradura(otra) != null;
            datos.sacar(donde);
            if (cerradura != null && otra != null && sePuedeCerrar(mundo.getBlockState(otra))) {
               datos.mover(otra, cerradura);
            }

            if (habia) {
               avisarCambio(mundo);
            }
         }
      });
   }

   private static void limpiar(ServerLevel mundo) {
      CandadosData datos = data(mundo);
      boolean cambio = false;

      for (BlockPos pos : datos.posiciones()) {
         if (mundo.isLoaded(pos)) {
            CandadosData.Cerradura cerradura = datos.cerradura(pos);
            if (!sePuedeCerrar(mundo.getBlockState(pos))) {
               datos.sacar(pos);
               cambio = true;
            } else {
               BlockPos clave = principal(mundo, pos);
               if (!clave.equals(pos) && datos.cerradura(clave) == null) {
                  datos.sacar(pos);
                  datos.mover(clave, cerradura);
                  cambio = true;
               }
            }
         }
      }

      if (cambio) {
         avisarCambio(mundo);
      }
   }

   private static void desproteger(ServerPlayer jugador, ServerLevel mundo, BlockPos clave, CandadosData.Cerradura cerradura) {
      if (cerradura == null) {
         jugador.displayClientMessage(Component.literal("Este cofre no tiene candado.").withStyle(ChatFormatting.GRAY), true);
      } else if (!cerradura.duenio().equals(jugador.getUUID())) {
         jugador.displayClientMessage(Component.literal("Este cofre no es de tu propiedad.").withStyle(ChatFormatting.RED), true);
         mundo.playSound(null, clave, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
      } else {
         data(mundo).sacar(clave);
         avisarCambio(mundo);
         ItemStack candado = new ItemStack(Ids.item("candado"));
         if (!jugador.getInventory().add(candado)) {
            jugador.drop(candado, false);
         }

         mundo.playSound(null, clave, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.2F);
         jugador.displayClientMessage(Component.literal("Cofre desprotegido.").withStyle(ChatFormatting.GREEN), true);
      }
   }

   public static void codigoEscrito(ServerPlayer jugador, CandadoCodigoPayload payload) {
      ServerLevel mundo = jugador.serverLevel();
      BlockPos pos = payload.pos();
      if (jugador.blockPosition().closerThan(pos, 8.0)) {
         BlockState estado = mundo.getBlockState(pos);
         if (sePuedeCerrar(estado)) {
            String codigo = payload.codigo().trim();
            CandadosData datos = data(mundo);
            CandadosData.Cerradura cerradura = datos.cerradura(pos);
            if (payload.cerrar()) {
               if (cerradura == null && !codigo.isEmpty()) {
                  ItemStack candado = jugador.getMainHandItem().is(Ids.item("candado")) ? jugador.getMainHandItem() : jugador.getOffhandItem();
                  if (candado.is(Ids.item("candado"))) {
                     candado.shrink(1);
                     datos.poner(pos, codigo, jugador.getUUID());
                     avisarCambio(mundo);
                     mundo.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.4F);
                     jugador.displayClientMessage(
                        Component.literal("Cofre cerrado con el código ")
                           .withStyle(ChatFormatting.GREEN)
                           .append(Component.literal(codigo).withStyle(ChatFormatting.YELLOW)),
                        false
                     );
                  }
               }
            } else if (cerradura != null) {
               if (!cerradura.codigo().equals(codigo)) {
                  ServerPlayNetworking.send(jugador, new CandadoPantallaPayload(pos, false, "Código incorrecto"));
                  mundo.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.8F);
               } else {
                  datos.autorizar(pos, jugador.getUUID());
                  mundo.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.4F);
                  abrir(jugador, mundo, pos);
               }
            }
         }
      }
   }

   private static void abrir(ServerPlayer jugador, ServerLevel mundo, BlockPos pos) {
      BlockState estado = mundo.getBlockState(pos);
      MenuProvider proveedor = estado.getMenuProvider(mundo, pos);
      if (proveedor != null) {
         jugador.openMenu(proveedor);
      }
   }

   public static Component nombreDe(Player jugador) {
      return jugador.getDisplayName();
   }
}
