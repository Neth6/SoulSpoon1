package neth6.soulspoon.mision;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import neth6.soulspoon.correo.Correo;
import neth6.soulspoon.economy.Coins;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bloque de entrega: con la Dedita de la Mision en la mano, se entrega la mision principal y,
 * unos segundos despues, llega por correo (de "Adeabel") el premio aparte configurado con /mision premio.
 */
public class EntregaMisionBlock extends Block {
    public static final String REMITENTE = "Adeabel";
    /** Ticks entre entregar la dedita y recibir la carta (suspenso). */
    private static final int ESPERA = 60;

    private record Pendiente(UUID jugador, String nombre, String texto, List<ItemStack> premio, long cuando) {}

    private static final List<Pendiente> PENDIENTES = new ArrayList<>();

    public static void registrar() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Pendiente> it = PENDIENTES.iterator();
            while (it.hasNext()) {
                Pendiente p = it.next();
                if (server.getTickCount() >= p.cuando()) {
                    it.remove();
                    Correo.mandar(server, p.jugador(), REMITENTE, "Misión principal", p.texto(), p.premio());
                    server.getPlayerList().broadcastSystemMessage(
                        Component.literal("★ " + p.nombre() + " ha completado una misión principal.").withColor(0x7FDFF2), false);
                }
            }
        });
    }

    public EntregaMisionBlock(Properties propiedades) {
        super(propiedades);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack pila, BlockState estado, Level level, BlockPos pos, Player jugador,
                                              InteractionHand mano, BlockHitResult golpe) {
        if (pila.isEmpty() || !pila.is(Coins.mision())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(jugador instanceof ServerPlayer servidor)) {
            return ItemInteractionResult.SUCCESS;
        }
        String id = MisionPrincipal.idDe(pila);
        int dia = MisionPrincipal.diaDe(pila);
        MisionPrincipal.Premio premio = MisionPrincipal.premioDe(id);
        List<ItemStack> objetos = premio.aItems();
        String texto = !premio.texto.isEmpty() ? premio.texto
            : (dia >= 0 ? "¡Completaste la Misión Principal del Día " + dia + "!" : "¡Completaste una Misión Principal!")
              + (objetos.isEmpty() ? " Gracias por entregar tu Dedita de la Misión." : " Aquí tienes tu premio.");
        pila.consume(1, jugador);
        PENDIENTES.add(new Pendiente(servidor.getUUID(), servidor.getGameProfile().getName(), texto, objetos,
            servidor.server.getTickCount() + ESPERA));
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        jugador.displayClientMessage(Component.literal("Entregada. Pronto te llegará una carta de " + REMITENTE + ".").withColor(0x55FFFF), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
        if (!level.isClientSide) {
            jugador.displayClientMessage(Component.literal("Trae tu Dedita de la Misión para recibir el premio.").withColor(0x55FFFF), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
