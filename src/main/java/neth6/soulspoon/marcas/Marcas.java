package neth6.soulspoon.marcas;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class Marcas {
   public static final Map<String, Integer> COLORES = new LinkedHashMap<>();

   private Marcas() {
   }

   private static MarcasData data(MinecraftServer server) {
      return (MarcasData)server.overworld().getDataStorage().computeIfAbsent(MarcasData.FACTORY, "soulspoon_marcas");
   }

   public static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("marca")
                     .requires(s -> s.hasPermission(2)))
                  .then(
                     Commands.literal("crear")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("nombre", StringArgumentType.string()).executes(ctx -> crear(ctx, "naranja")))
                              .then(
                                 Commands.argument("color", StringArgumentType.word())
                                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(COLORES.keySet(), b))
                                    .executes(ctx -> crear(ctx, StringArgumentType.getString(ctx, "color")))
                              )
                        )
                  ))
               .then(
                  Commands.literal("borrar")
                     .then(
                        Commands.argument("nombre", StringArgumentType.string())
                           .suggests(
                              (ctx, b) -> SharedSuggestionProvider.suggest(
                                 data(((CommandSourceStack)ctx.getSource()).getServer()).todas().stream().map(m -> "\"" + m.nombre() + "\""), b
                              )
                           )
                           .executes(ctx -> {
                              String nombre = StringArgumentType.getString(ctx, "nombre");
                              MinecraftServer server = ((CommandSourceStack)ctx.getSource()).getServer();
                              if (!data(server).borrar(nombre)) {
                                 ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("No hay ninguna marca llamada " + nombre + "."));
                                 return 0;
                              } else {
                                 enviarATodos(server);
                                 ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Marca " + nombre + " borrada."), true);
                                 return 1;
                              }
                           })
                     )
               ))
            .then(
               Commands.literal("lista")
                  .executes(
                     ctx -> {
                        List<MarcasPayload.Marca> marcas = data(((CommandSourceStack)ctx.getSource()).getServer()).todas();
                        String texto = marcas.isEmpty()
                           ? "No hay marcas."
                           : marcas.stream()
                              .map(m -> m.nombre() + " (" + m.x() + ", " + m.y() + ", " + m.z() + ")")
                              .collect(Collectors.joining("\n", "Marcas:\n", ""));
                        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal(texto), false);
                        return marcas.size();
                     }
                  )
            )
      );
   }

   private static int crear(CommandContext<CommandSourceStack> ctx, String nombreColor) {
      CommandSourceStack fuente = (CommandSourceStack)ctx.getSource();
      Integer color = COLORES.get(nombreColor.toLowerCase());
      if (color == null) {
         fuente.sendFailure(Component.literal("Colores: " + String.join(", ", COLORES.keySet())));
         return 0;
      } else {
         BlockPos pos = BlockPos.containing(fuente.getPosition());
         String nombre = StringArgumentType.getString(ctx, "nombre");
         MarcasPayload.Marca marca = new MarcasPayload.Marca(
            nombre, fuente.getLevel().dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ(), color
         );
         data(fuente.getServer()).poner(marca);
         enviarATodos(fuente.getServer());
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.literal("Marca " + nombre + " creada en " + marca.x() + ", " + marca.y() + ", " + marca.z() + "."), true);
         return 1;
      }
   }

   private static void enviarATodos(MinecraftServer server) {
      for (ServerPlayer jugador : server.getPlayerList().getPlayers()) {
         sincronizar(jugador);
      }
   }

   public static void sincronizar(ServerPlayer jugador) {
      ServerPlayNetworking.send(jugador, new MarcasPayload(data(jugador.server).todas()));
   }

   static {
      COLORES.put("naranja", 16755200);
      COLORES.put("rojo", 16733525);
      COLORES.put("amarillo", 16777045);
      COLORES.put("verde", 5635925);
      COLORES.put("celeste", 5636095);
      COLORES.put("azul", 5592575);
      COLORES.put("violeta", 16733695);
      COLORES.put("blanco", 16777215);
   }
}
