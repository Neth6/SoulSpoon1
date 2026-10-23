package neth6.soulspoon.banco;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CajeroBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<CajeroBlock> CODEC = simpleCodec(CajeroBlock::new);
   public static final EnumProperty<DoubleBlockHalf> MITAD = BlockStateProperties.DOUBLE_BLOCK_HALF;
   private static final Map<Direction, VoxelShape> FORMA_ABAJO = formas(16.0);
   private static final Map<Direction, VoxelShape> FORMA_ARRIBA = formas(15.0);
   public static Consumer<BlockPos> abrirPantalla = pos -> {};

   private static Map<Direction, VoxelShape> formas(double alto) {
      return Map.of(
         Direction.SOUTH,
         Block.box(2.0, 0.0, 0.0, 14.0, alto, 10.5),
         Direction.NORTH,
         Block.box(2.0, 0.0, 5.5, 14.0, alto, 16.0),
         Direction.EAST,
         Block.box(0.0, 0.0, 2.0, 10.5, alto, 14.0),
         Direction.WEST,
         Block.box(5.5, 0.0, 2.0, 16.0, alto, 14.0)
      );
   }

   public CajeroBlock(Properties propiedades) {
      super(propiedades);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(MITAD, DoubleBlockHalf.LOWER)
      );
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, MITAD});
   }

   public BlockState getStateForPlacement(BlockPlaceContext contexto) {
      BlockPos pos = contexto.getClickedPos();
      Level level = contexto.getLevel();
      return pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(contexto)
         ? (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite()))
            .setValue(MITAD, DoubleBlockHalf.LOWER)
         : null;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
      level.setBlock(pos.above(), (BlockState)estado.setValue(MITAD, DoubleBlockHalf.UPPER), 3);
   }

   protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
      return (estado.getValue(MITAD) == DoubleBlockHalf.LOWER ? FORMA_ABAJO : FORMA_ARRIBA).get(estado.getValue(FACING));
   }

   protected BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos vecinoPos) {
      DoubleBlockHalf mitad = (DoubleBlockHalf)estado.getValue(MITAD);
      if (direccion.getAxis() == Axis.Y && mitad == DoubleBlockHalf.LOWER == (direccion == Direction.UP)) {
         return vecino.is(this) && vecino.getValue(MITAD) != mitad ? estado : Blocks.AIR.defaultBlockState();
      } else {
         return super.updateShape(estado, direccion, vecino, level, pos, vecinoPos);
      }
   }

   protected boolean canSurvive(BlockState estado, LevelReader level, BlockPos pos) {
      if (estado.getValue(MITAD) == DoubleBlockHalf.LOWER) {
         return true;
      }

      BlockState abajo = level.getBlockState(pos.below());
      return abajo.is(this) && abajo.getValue(MITAD) == DoubleBlockHalf.LOWER;
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState estado, Player jugador) {
      if (!level.isClientSide && estado.getValue(MITAD) == DoubleBlockHalf.UPPER && (jugador.isCreative() || !jugador.hasCorrectToolForDrops(estado))) {
         BlockPos abajo = pos.below();
         BlockState estadoAbajo = level.getBlockState(abajo);
         if (estadoAbajo.is(this) && estadoAbajo.getValue(MITAD) == DoubleBlockHalf.LOWER) {
            level.setBlock(abajo, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(jugador, 2001, abajo, Block.getId(estadoAbajo));
         }
      }

      return super.playerWillDestroy(level, pos, estado, jugador);
   }

   protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
      if (level.isClientSide) {
         abrirPantalla.accept(pos);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}
