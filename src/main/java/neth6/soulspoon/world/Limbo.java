package neth6.soulspoon.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class Limbo {
   public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("soulspoon", "limbo"));
   public static final ResourceKey<DamageType> DANIO_AGUA = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("soulspoon", "agua_limbo")
   );
   private static final ResourceLocation MODIFICADOR = ResourceLocation.fromNamespaceAndPath("soulspoon", "limbo_corazones");

   private Limbo() {
   }

   private static Limbo.Datos datos(MinecraftServer server) {
      return (Limbo.Datos)server.overworld().getDataStorage().computeIfAbsent(Limbo.Datos.FACTORY, "soulspoon_limbo");
   }

   private static boolean bloqueado(Player jugador, Level mundo) {
      return mundo.dimension().equals(DIMENSION) && !jugador.isCreative();
   }

   private static boolean esBloqueOBalde(ItemStack pila) {
      return pila.getItem() instanceof BlockItem || pila.getItem() instanceof BucketItem || pila.getItem() instanceof SolidBucketItem;
   }

   public static void initialize() {
      net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> registrarComandos(dispatcher));

      PlayerBlockBreakEvents.BEFORE.register((mundo, jugador, pos, estado, entidad) -> {
         if (!bloqueado(jugador, mundo)) {
            return true;
         }

         jugador.displayClientMessage(Component.literal("En el Limbo no se pueden romper bloques.").withColor(9079464), true);
         return false;
      });
      UseBlockCallback.EVENT.register((jugador, mundo, mano, golpe) -> {
         if (bloqueado(jugador, mundo) && esBloqueOBalde(jugador.getItemInHand(mano))) {
            jugador.displayClientMessage(Component.literal("En el Limbo no se puede poner nada.").withColor(9079464), true);
            return InteractionResult.FAIL;
         } else {
            return InteractionResult.PASS;
         }
      });
      UseItemCallback.EVENT.register((jugador, mundo, mano) -> {
         ItemStack pila = jugador.getItemInHand(mano);
         if (pila.is(Items.TRIDENT) && !mundo.dimension().equals(DIMENSION)) {
            if (!mundo.isClientSide) {
               jugador.displayClientMessage(Component.literal("El tridente solo sirve en el Limbo.").withColor(9079464), true);
            }

            return InteractionResultHolder.fail(pila);
         } else {
            return InteractionResultHolder.pass(pila);
         }
      });
      UseItemCallback.EVENT.register((jugador, mundo, mano) -> {
         ItemStack pila = jugador.getItemInHand(mano);
         return bloqueado(jugador, mundo) && esBloqueOBalde(pila) ? InteractionResultHolder.fail(pila) : InteractionResultHolder.pass(pila);
      });
      ServerTickEvents.END_SERVER_TICK.register(server -> {
         if (server.getTickCount() % 20 == 0) {
            Limbo.Datos d = datos(server);

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
               if (p.isAlive() && !p.isCreative() && !p.isSpectator() && p.level().dimension().equals(DIMENSION) && p.isInWater()) {
                  p.hurt(new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DANIO_AGUA)), 2.0F);
                  quitarCorazon(p, "El agua del Limbo te quitó un corazón para siempre.");
               }

               aplicar(p, d.perdidos.getOrDefault(p.getUUID(), 0));
            }
         }
      });
   }

   public static void quitarCorazon(ServerPlayer p, String aviso) {
      if (!(p.getMaxHealth() <= 2.0F)) {
         Limbo.Datos d = datos(p.server);
         int antes = d.perdidos.getOrDefault(p.getUUID(), 0);
         d.perdidos.put(p.getUUID(), antes + 1);
         d.setDirty();
         aplicar(p, antes + 1);
         p.displayClientMessage(Component.literal(aviso).withColor(9079464), true);
         p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.6F);
      }
   }

   private static void aplicar(ServerPlayer p, int perdidos) {
      AttributeInstance vida = p.getAttribute(Attributes.MAX_HEALTH);
      if (vida != null) {
         AttributeModifier actual = vida.getModifier(MODIFICADOR);
         double quiere = -2.0 * perdidos;
         if (actual == null || actual.amount() != quiere) {
            vida.removeModifier(MODIFICADOR);
            if (perdidos > 0) {
               vida.addTransientModifier(new AttributeModifier(MODIFICADOR, quiere, Operation.ADD_VALUE));
            }

            if (p.getHealth() > p.getMaxHealth()) {
               p.setHealth(p.getMaxHealth());
            }
         }
      }
   }

   /** Primer lugar libre con suelo en esa columna (o y=100 si no hay). */
   public static BlockPos lugarSeguro(ServerLevel destino, BlockPos desde) {
      BlockPos columna = new BlockPos(desde.getX(), 0, desde.getZ());
      destino.getChunk(columna.getX() >> 4, columna.getZ() >> 4);
      BlockPos arriba = destino.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, columna);
      return arriba.getY() <= destino.getMinBuildHeight() + 1 ? columna.atY(100) : arriba;
   }

   public static void mandar(ServerPlayer jugador) {
      ServerLevel limbo = jugador.server.getLevel(DIMENSION);
      if (limbo != null) {
         BlockPos spawn = datos(jugador.server).spawn;
         BlockPos llegada = spawn != null ? spawn : lugarSeguro(limbo, jugador.blockPosition());
         jugador.teleportTo(limbo, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
         limbo.playSound(null, llegada, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.8F, 0.5F);
         jugador.sendSystemMessage(Component.literal("Fuiste arrastrado al Limbo.").withColor(9079464));
      }
   }

   public static int viajar(ServerPlayer jugador) {
      ServerLevel desde = jugador.serverLevel();
      boolean volver = desde.dimension().equals(DIMENSION);
      ServerLevel destino = volver ? desde.getServer().overworld() : desde.getServer().getLevel(DIMENSION);
      if (destino == null) {
         return 0;
      }

      BlockPos llegada = lugarSeguro(destino, jugador.blockPosition());
      jugador.teleportTo(destino, llegada.getX() + 0.5, llegada.getY(), llegada.getZ() + 0.5, jugador.getYRot(), jugador.getXRot());
      destino.playSound(null, llegada, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6F, 0.5F);
      jugador.sendSystemMessage(
         Component.literal(
               volver
                  ? "Saliste del Limbo."
                  : "Entraste al Limbo. Cuidado con el agua: cada segundo te quita un corazón para siempre (para volver: /admin limbo)."
            )
            .withColor(9079464)
      );
      return 1;
   }

   public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawn_limbo")
                     .requires(s -> s.hasPermission(2)))
                  .executes(
                     c -> {
                        BlockPos spawn = datos(((CommandSourceStack)c.getSource()).getServer()).spawn;
                        ((CommandSourceStack)c.getSource())
                           .sendSuccess(
                              () -> Component.literal(
                                    spawn == null
                                       ? "No hay spawn del Limbo: se llega a un lugar seguro cerca de donde estabas."
                                       : "Spawn del Limbo: " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ()
                                 )
                                 .withStyle(ChatFormatting.GRAY),
                              false
                           );
                        return 1;
                     }
                  ))
               .then(Commands.literal("quitar").executes(c -> {
                  Limbo.Datos d = datos(((CommandSourceStack)c.getSource()).getServer());
                  d.spawn = null;
                  d.setDirty();
                  ((CommandSourceStack)c.getSource()).sendSuccess(() -> Component.literal("Spawn del Limbo quitado.").withStyle(ChatFormatting.GRAY), true);
                  return 1;
               })))
            .then(
               Commands.argument("coordenadas", BlockPosArgument.blockPos())
                  .executes(
                     c -> {
                        BlockPos pos = BlockPosArgument.getBlockPos(c, "coordenadas");
                        Limbo.Datos d = datos(((CommandSourceStack)c.getSource()).getServer());
                        d.spawn = pos.immutable();
                        d.setDirty();
                        ((CommandSourceStack)c.getSource())
                           .sendSuccess(
                              () -> Component.literal(
                                    "Spawn del Limbo: " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ". Ahí llega el que es mandado al Limbo."
                                 )
                                 .withStyle(ChatFormatting.GRAY),
                              true
                           );
                        return 1;
                     }
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("limbo").requires(s -> s.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("devolver")
                        .executes(c -> devolver((CommandSourceStack)c.getSource(), List.of(((CommandSourceStack)c.getSource()).getPlayerOrException()))))
                     .then(
                        Commands.argument("jugadores", EntityArgument.players())
                           .executes(c -> devolver((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "jugadores")))
                     )
               ))
            .then(
               Commands.literal("ver")
                  .then(
                     Commands.argument("jugador", EntityArgument.player())
                        .executes(
                           c -> {
                              ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
                              int n = datos(((CommandSourceStack)c.getSource()).getServer()).perdidos.getOrDefault(p.getUUID(), 0);
                              ((CommandSourceStack)c.getSource())
                                 .sendSuccess(
                                    () -> Component.literal(
                                          p.getGameProfile().getName() + " perdió " + n + (n == 1 ? " corazón" : " corazones") + " en el Limbo."
                                       )
                                       .withStyle(ChatFormatting.GRAY),
                                    false
                                 );
                              return n;
                           }
                        )
                  )
            )
      );
   }

   public static void devolverCorazones(ServerPlayer p) {
      Limbo.Datos d = datos(p.server);
      if (d.perdidos.remove(p.getUUID()) != null) {
         d.setDirty();
      }

      aplicar(p, 0);
   }

   private static int devolver(CommandSourceStack fuente, Collection<ServerPlayer> jugadores) {
      Limbo.Datos d = datos(fuente.getServer());

      for (ServerPlayer p : jugadores) {
         d.perdidos.remove(p.getUUID());
         aplicar(p, 0);
         p.displayClientMessage(Component.literal("Recuperaste tus corazones.").withColor(16733525), true);
      }

      d.setDirty();
      fuente.sendSuccess(() -> Component.literal("Corazones devueltos a " + jugadores.size() + " jugador(es).").withStyle(ChatFormatting.GRAY), true);
      return jugadores.size();
   }

   static final class Datos extends SavedData {
      static final Factory<Limbo.Datos> FACTORY = new Factory<>(Limbo.Datos::new, Limbo.Datos::cargar, net.minecraft.util.datafix.DataFixTypes.LEVEL);
      final Map<UUID, Integer> perdidos = new HashMap<>();
      BlockPos spawn;

      private static Limbo.Datos cargar(CompoundTag tag, Provider registros) {
         Limbo.Datos d = new Limbo.Datos();

         for (String k : tag.getAllKeys()) {
            if (k.equals("spawn")) {
               d.spawn = BlockPos.of(tag.getLong(k));
            } else {
               d.perdidos.put(UUID.fromString(k), tag.getInt(k));
            }
         }

         return d;
      }

      public CompoundTag save(CompoundTag tag, Provider registros) {
         this.perdidos.forEach((j, n) -> tag.putInt(j.toString(), n));
         if (this.spawn != null) {
            tag.putLong("spawn", this.spawn.asLong());
         }

         return tag;
      }
   }
}
