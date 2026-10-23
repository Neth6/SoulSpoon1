package neth6.soulspoon.mision;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.economy.Coins;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Premios de las misiones principales (el "premio aparte" que llega por correo al entregar la Dedita de la Mision).
 * Se guardan en config/soulspoon/mision_principal.json y se editan con comandos (op 2):
 *   /mision dedita <id> [dia] [jugador]      -> te da una Dedita de la Mision de esa mision
 *   /mision premio <id> ver | borrar
 *   /mision premio <id> item                 -> agrega el item de tu mano (con su cantidad)
 *   /mision premio <id> deditas <azules>     -> deditas (se mandan como monedas en la carta)
 *   /mision premio <id> texto <mensaje>
 * El id "predeterminado" se usa cuando la Dedita no trae mision o esa mision no tiene premio propio.
 */
public final class MisionPrincipal {
    private MisionPrincipal() {}

    public static final String PREDETERMINADO = "predeterminado";

    public static class Pila {
        public String item = "";
        public int count = 1;
    }

    public static class Premio {
        public String texto = "";
        public long deditas = 0;
        public List<Pila> items = new ArrayList<>();

        public boolean vacio() { return deditas <= 0 && items.isEmpty(); }

        /** Los items de la carta: items + monedas por las deditas. */
        public List<ItemStack> aItems() {
            List<ItemStack> out = new ArrayList<>();
            for (Pila p : items) {
                ResourceLocation rl = ResourceLocation.tryParse(p.item);
                Item it = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
                if (it == null || p.count <= 0) continue;
                int n = p.count;
                while (n > 0) {
                    int tanda = Math.min(it.getDefaultMaxStackSize(), n);
                    out.add(new ItemStack(it, tanda));
                    n -= tanda;
                }
            }
            out.addAll(Coins.itemsPara(deditas));
            return out;
        }
    }

    public static class Config {
        public Map<String, Premio> misiones = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config config;

    private static Path archivo() {
        return FabricLoader.getInstance().getConfigDir().resolve("soulspoon").resolve("mision_principal.json");
    }

    private static Config cfg() {
        if (config == null) {
            config = new Config();
            Path p = archivo();
            if (Files.exists(p)) {
                try {
                    Config c = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Config.class);
                    if (c != null && c.misiones != null) config = c;
                } catch (Exception e) {
                    SoulSpoon.LOGGER.warn("mision_principal.json ilegible: {}", e.toString());
                }
            }
        }
        return config;
    }

    private static void guardar() {
        try {
            Path p = archivo();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(cfg()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SoulSpoon.LOGGER.warn("No pude guardar mision_principal.json: {}", e.toString());
        }
    }

    /** Premio de una mision (o el predeterminado, o vacio). */
    public static Premio premioDe(String id) {
        Premio p = id == null || id.isEmpty() ? null : cfg().misiones.get(id);
        if (p == null) p = cfg().misiones.get(PREDETERMINADO);
        return p != null ? p : new Premio();
    }

    // ---------------------------------------------------------------- Dedita de la Mision

    public static ItemStack deditaDeMision(String id, int dia) {
        ItemStack s = new ItemStack(Coins.mision());
        CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> {
            tag.putString("mision", id);
            if (dia >= 0) tag.putInt("dia", dia);
        });
        return s;
    }

    public static String idDe(ItemStack dedita) {
        CustomData d = dedita.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString("mision");
    }

    public static int diaDe(ItemStack dedita) {
        CustomData d = dedita.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().contains("dia") ? d.copyTag().getInt("dia") : -1;
    }

    // ---------------------------------------------------------------- comandos

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(
            Commands.literal("mision").requires(s -> s.hasPermission(2))
                .then(Commands.literal("dedita")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> dar(c, -1, null))
                        .then(Commands.argument("dia", IntegerArgumentType.integer(0))
                            .executes(c -> dar(c, IntegerArgumentType.getInteger(c, "dia"), null))
                            .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(c -> dar(c, IntegerArgumentType.getInteger(c, "dia"), EntityArgument.getPlayer(c, "jugador")))))))
                .then(Commands.literal("premio")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.literal("ver").executes(MisionPrincipal::ver))
                        .then(Commands.literal("borrar").executes(c -> {
                            cfg().misiones.remove(StringArgumentType.getString(c, "id"));
                            guardar();
                            c.getSource().sendSuccess(() -> Component.literal("Premio borrado."), true);
                            return 1;
                        }))
                        .then(Commands.literal("item").executes(MisionPrincipal::agregarItem))
                        .then(Commands.literal("deditas").then(Commands.argument("azules", LongArgumentType.longArg(0)).executes(c -> {
                            Premio p = cfg().misiones.computeIfAbsent(StringArgumentType.getString(c, "id"), k -> new Premio());
                            p.deditas = LongArgumentType.getLong(c, "azules");
                            guardar();
                            c.getSource().sendSuccess(() -> Component.literal("Premio en deditas: " + p.deditas), true);
                            return 1;
                        })))
                        .then(Commands.literal("texto").then(Commands.argument("mensaje", StringArgumentType.greedyString()).executes(c -> {
                            Premio p = cfg().misiones.computeIfAbsent(StringArgumentType.getString(c, "id"), k -> new Premio());
                            p.texto = StringArgumentType.getString(c, "mensaje");
                            guardar();
                            c.getSource().sendSuccess(() -> Component.literal("Texto de la carta guardado."), true);
                            return 1;
                        })))))));
    }

    private static int dar(CommandContext<CommandSourceStack> c, int dia, ServerPlayer para) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer j = para != null ? para : c.getSource().getPlayerOrException();
        ItemStack s = deditaDeMision(StringArgumentType.getString(c, "id"), dia);
        if (!j.getInventory().add(s)) j.drop(s, false);
        c.getSource().sendSuccess(() -> Component.literal("Dedita de la Misión \"" + StringArgumentType.getString(c, "id") + "\" entregada a " + j.getName().getString() + "."), true);
        return 1;
    }

    private static int ver(CommandContext<CommandSourceStack> c) {
        String id = StringArgumentType.getString(c, "id");
        Premio p = cfg().misiones.get(id);
        if (p == null) {
            c.getSource().sendSuccess(() -> Component.literal("\"" + id + "\" no tiene premio propio."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("Premio de \"" + id + "\": ");
        for (Pila x : p.items) sb.append(x.count).append(" × ").append(x.item).append(", ");
        sb.append(p.deditas).append(" deditas");
        if (!p.texto.isEmpty()) sb.append(" | texto: ").append(p.texto);
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int agregarItem(CommandContext<CommandSourceStack> c) {
        if (!(c.getSource().getEntity() instanceof ServerPlayer j) || j.getMainHandItem().isEmpty()) {
            c.getSource().sendFailure(Component.literal("Ten en la mano el item del premio (con la cantidad)."));
            return 0;
        }
        ItemStack mano = j.getMainHandItem();
        Premio p = cfg().misiones.computeIfAbsent(StringArgumentType.getString(c, "id"), k -> new Premio());
        Pila x = new Pila();
        x.item = BuiltInRegistries.ITEM.getKey(mano.getItem()).toString();
        x.count = mano.getCount();
        p.items.add(x);
        guardar();
        c.getSource().sendSuccess(() -> Component.literal("Agregado al premio: " + x.count + " × " + x.item), true);
        return 1;
    }
}
