package neth6.soulspoon.economy;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * Economia del evento: deditas azules (1), verdes (100), rojas (1.000) y moradas (10.000).
 * Las misiones pagan en deditas y el casino (la ruleta, mas adelante) las gasta.
 *
 * /deditas                                   -> ver tu saldo
 * /dedsafio deditas dar|quitar <jugador> <cantidad> [azul|verde|roja|morada]
 */
public final class Economy {
    private Economy() {}

    public record BalanceS2C(long total) implements CustomPacketPayload {
        public static final Type<BalanceS2C> TYPE = new Type<>(SoulSpoon.id("balance"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BalanceS2C> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_LONG, BalanceS2C::total, BalanceS2C::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(BalanceS2C.TYPE, BalanceS2C.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(Commands.literal("deditas").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                long total = balance(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "Tienes " + total + " (" + Dedita.describe(total) + ")"), false);
                return 1;
            }));

            dispatcher.register(Commands.literal("dedsafio")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("deditas")
                            .then(Commands.literal("dar").then(amountNode(true)))
                            .then(Commands.literal("quitar").then(amountNode(false)))));
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> amountNode(boolean give) {
        return Commands.argument("jugador", EntityArgument.player())
                .then(Commands.argument("cantidad", LongArgumentType.longArg(1))
                        .executes(ctx -> change(ctx, give, Dedita.AZUL))
                        .then(Commands.argument("tipo", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Dedita.values()).map(d -> d.id), b))
                                .executes(ctx -> {
                                    Dedita type = Dedita.byId(StringArgumentType.getString(ctx, "tipo"));
                                    if (type == null) {
                                        ctx.getSource().sendFailure(Component.literal("Tipo: azul, verde, roja o morada"));
                                        return 0;
                                    }
                                    return change(ctx, give, type);
                                })));
    }

    private static int change(CommandContext<CommandSourceStack> ctx, boolean give, Dedita type)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
        long amount = LongArgumentType.getLong(ctx, "cantidad") * type.value;
        if (give) {
            add(target, amount);
        } else {
            set(target, balance(target) - amount);
        }
        long total = balance(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getName().getString() + " ahora tiene " + total + " (" + Dedita.describe(total) + ")"), true);
        return 1;
    }

    // ------------------------------------------------------------------ API

    public static long balance(ServerPlayer player) {
        return EconomyData.get(player.server).balance(player.getUUID());
    }

    public static void add(ServerPlayer player, long amount) {
        if (amount <= 0) return;
        set(player, balance(player) + amount);
    }

    /** Gasta si alcanza; devuelve false si no hay saldo suficiente. */
    public static boolean spend(ServerPlayer player, long amount) {
        long current = balance(player);
        if (amount < 0 || current < amount) return false;
        set(player, current - amount);
        return true;
    }

    public static void set(ServerPlayer player, long value) {
        EconomyData.get(player.server).set(player.getUUID(), value);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ServerPlayNetworking.send(player, new BalanceS2C(balance(player)));
    }
}
