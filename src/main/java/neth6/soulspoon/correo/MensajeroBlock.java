package neth6.soulspoon.correo;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MensajeroBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<MensajeroBlock> CODEC = simpleCodec(MensajeroBlock::new);
   private static final VoxelShape OESTE = Block.box(8.0, 0.0, 1.0, 16.0, 16.0, 15.0);
   private static final VoxelShape NORTE = Block.box(1.0, 0.0, 8.0, 15.0, 16.0, 16.0);
   private static final VoxelShape ESTE = Block.box(0.0, 0.0, 1.0, 8.0, 16.0, 15.0);
   private static final VoxelShape SUR = Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 8.0);
   public static Consumer<BlockPos> abrirPantalla = pos -> {};

   public MensajeroBlock(Properties propiedades) {
      super(propiedades);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
      return switch ((Direction)estado.getValue(FACING)) {
         case WEST -> OESTE;
         case EAST -> ESTE;
         case SOUTH -> SUR;
         default -> NORTE;
      };
   }

   protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
      if (level.isClientSide) {
         abrirPantalla.accept(pos);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public BlockState getStateForPlacement(BlockPlaceContext contexto) {
      for (Direction direccion : contexto.getNearestLookingDirections()) {
         if (direccion.getAxis().isHorizontal()) {
            BlockState estado = (BlockState)this.defaultBlockState().setValue(FACING, direccion.getOpposite());
            if (estado.canSurvive(contexto.getLevel(), contexto.getClickedPos())) {
               return estado;
            }
         }
      }

      return null;
   }

   protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
      Direction frente = (Direction)estado.getValue(FACING);
      BlockPos pared = pos.relative(frente.getOpposite());
      return level.getBlockState(pared).isFaceSturdy(level, pared, frente);
   }

   protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos vecinoPos) {
      return direccion == ((Direction)estado.getValue(FACING)).getOpposite() && !estado.canSurvive(level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(estado, direccion, vecino, level, pos, vecinoPos);
   }
}
