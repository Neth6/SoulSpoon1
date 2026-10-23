package neth6.soulspoon.marcas;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class BloqueoMinimapa {
   private static final List<ResourceLocation> EFECTOS = List.of(
      ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_minimap"), ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_waypoints")
   );
   private static final String SIN_MINIMAPA = codigo("nominimap");
   private static final String RESTABLECER = codigo("resetxaero");
   private static final int PRIMER_ENVIO = 40;
   private static final int SEGUNDO_ENVIO = 200;
   private static final Map<UUID, Integer> PENDIENTES = new HashMap<>();

   private BloqueoMinimapa() {
   }

   private static String codigo(String palabra) {
      StringBuilder sb = new StringBuilder();

      for (char c : palabra.toCharArray()) {
         sb.append('§').append(c);
      }

      return sb.toString();
   }

   public static void alEntrar(ServerPlayer jugador) {
      PENDIENTES.put(jugador.getUUID(), 0);
      ServerPlayNetworking.send(jugador, new MinimapaPayload(data(jugador.server).adminsLoUsan()));
   }

   private static MinimapaData data(MinecraftServer server) {
      return (MinimapaData)server.overworld().getDataStorage().computeIfAbsent(MinimapaData.FACTORY, "soulspoon_minimapa");
   }

   public static void setAdminsLoUsan(MinecraftServer server, boolean valor) {
      data(server).setAdminsLoUsan(valor);

      for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
         ServerPlayNetworking.send(jugador, new MinimapaPayload(valor));
         enviarCodigo(jugador);
         aplicarEfectos(jugador);
      }
   }

   public static void registrarComando(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("minimapa")
                     .requires(s -> s.hasPermission(2)))
                  .executes(
                     ctx -> {
                        boolean admins = data(((CommandSourceStack)ctx.getSource()).getServer()).adminsLoUsan();
                        ((CommandSourceStack)ctx.getSource())
                           .sendSuccess(
                              () -> Component.literal(
                                 admins
                                    ? "El minimapa está sacado para los jugadores; los admins lo pueden usar."
                                    : "El minimapa está sacado para todos (también los admins)."
                              ),
                              false
                           );
                        return 1;
                     }
                  ))
               .then(Commands.literal("todos").executes(ctx -> {
                  setAdminsLoUsan(((CommandSourceStack)ctx.getSource()).getServer(), false);
                  ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Minimapa sacado para todos (también los admins)."), true);
                  return 1;
               })))
            .then(Commands.literal("admins").executes(ctx -> {
               setAdminsLoUsan(((CommandSourceStack)ctx.getSource()).getServer(), true);
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Ahora los admins pueden usar el minimapa (los demás no)."), true);
               return 1;
            }))
      );
   }

   public static void tick(MinecraftServer server) {
      Iterator<Entry<UUID, Integer>> it = PENDIENTES.entrySet().iterator();

      while (it.hasNext()) {
         Entry<UUID, Integer> e = it.next();
         int ticks = e.getValue() + 1;
         e.setValue(ticks);
         ServerPlayer jugador = server.getPlayerList().getPlayer(e.getKey());
         if (jugador == null) {
            it.remove();
         } else {
            if (ticks == 40 || ticks == 200) {
               enviarCodigo(jugador);
            }

            if (ticks >= 200) {
               it.remove();
            }
         }
      }

      if (server.getTickCount() % 5 == 0) {
         for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
            aplicarEfectos(jugador);
         }
      }
   }

   private static boolean esAdmin(ServerPlayer jugador) {
      return jugador.hasPermissions(2) && data(jugador.server).adminsLoUsan();
   }

   private static void enviarCodigo(ServerPlayer jugador) {
      jugador.sendSystemMessage(Component.literal(RESTABLECER + (esAdmin(jugador) ? "" : SIN_MINIMAPA)));
   }

   public static void aplicarEfectos(ServerPlayer jugador) {
      boolean admin = esAdmin(jugador);

      for (ResourceLocation id : EFECTOS) {
         Optional<Reference<MobEffect>> efecto = BuiltInRegistries.MOB_EFFECT.getHolder(id);
         if (!efecto.isEmpty()) {
            if (admin) {
               jugador.removeEffect((Holder)efecto.get());
            } else if (!jugador.hasEffect((Holder)efecto.get())) {
               jugador.addEffect(new MobEffectInstance((Holder)efecto.get(), -1, 0, true, false, false));
            }
         }
      }
   }
}
