package neth6.soulspoon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import neth6.soulspoon.gui.ReviveMenu;
import neth6.soulspoon.item.ModItems;
import neth6.soulspoon.soul.AltarData;
import neth6.soulspoon.soul.ForgeManager;

public class SoulAltarBlock extends Block {
    // Distancia maxima (en bloques) al spawn del mundo para poder colocarlo.
    public static final int SPAWN_RADIUS = 32;
    // Altura de la columna de luz.
    public static final int BEAM_HEIGHT = 30;

    public SoulAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static void warn(Level level, Player player, String text) {
        if (player != null && !level.isClientSide) {
            player.displayClientMessage(Component.literal(text), true);
        }
    }

    // Si devuelve null, el bloque no se coloca.
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        // Solo en el Overworld, que es donde esta el spawn.
        if (level.dimension() != Level.OVERWORLD) {
            warn(level, player, "El altar solo puede colocarse en el Overworld");
            return null;
        }

        // Solo cerca del spawn.
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pos = context.getClickedPos();
        double dx = pos.getX() - spawn.getX();
        double dz = pos.getZ() - spawn.getZ();
        if (dx * dx + dz * dz > SPAWN_RADIUS * SPAWN_RADIUS) {
            warn(level, player, "El altar solo puede colocarse cerca del spawn");
            return null;
        }

        // Solo un altar por servidor.
        if (level instanceof ServerLevel serverLevel
                && AltarData.get(serverLevel.getServer()).hasAltar()) {
            warn(level, player, "Ya existe un Altar de Almas en este servidor");
            return null;
        }

        return super.getStateForPlacement(context);
    }

    // Se llama cuando el bloque aparece en el mundo: lo anotamos.
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel
                && !oldState.is(state.getBlock())) {
            AltarData.get(serverLevel.getServer()).setAltar(pos);
        }
    }

    // Se llama cuando el bloque desaparece: borramos el registro.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel
                && !state.is(newState.getBlock())) {
            AltarData.get(serverLevel.getServer()).clearAltar(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Columna de luz: destellos a lo largo de la altura sobre el altar.
    // Solo se ejecuta en el cliente, cerca del jugador.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos,
                            RandomSource random) {
        for (int i = 0; i < 120; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 1.0 + random.nextDouble() * BEAM_HEIGHT;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    // Clic derecho con la mano vacia (o con un item que no es la cuchara):
    // normal = ver la receta, Shift = forjar la cuchara.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
                                               BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                ForgeManager.tryForge(serverPlayer);
            } else {
                ForgeManager.showRecipe(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // Clic derecho con un item en la mano: con la cuchara, abre el menu.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
                                              Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(ModItems.SOUL_SPOON)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ReviveMenu.open(serverPlayer);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}