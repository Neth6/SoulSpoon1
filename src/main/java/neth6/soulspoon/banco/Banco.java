package neth6.soulspoon.banco;

import neth6.soulspoon.economy.Economy;
import net.minecraft.server.level.ServerPlayer;

/** Fachada del banco: usa la Economia del mod (un solo saldo por jugador). */
public final class Banco {
    private Banco() {}

    public static long saldo(ServerPlayer jugador) { return Economy.balance(jugador); }
    public static void setSaldo(ServerPlayer jugador, long cantidad) { Economy.set(jugador, cantidad); }
    public static void sumar(ServerPlayer jugador, long cantidad) { Economy.add(jugador, cantidad); }
    public static boolean cobrar(ServerPlayer jugador, long cantidad) { return Economy.spend(jugador, cantidad); }
}
