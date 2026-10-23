package neth6.soulspoon.boveda;

import neth6.soulspoon.hermandad.Hermandades;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BovedaBlock extends BaseEntityBlock {
   public static final MapCodec<BovedaBlock> CODEC = simpleCodec(BovedaBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final IntegerProperty PARTE = IntegerProperty.create("parte", 0, 5);
   public static final BooleanProperty ABIERTA = BooleanProperty.create("abierta");
   public static final int PRINCIPAL = 0;

   public BovedaBlock(Properties propiedades) {
      super(propiedades);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(PARTE, 0))
            .setValue(ABIERTA, false)
      );
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, PARTE, ABIERTA});
   }

   static Direction derecha(Direction frente) {
      return frente.getCounterClockWise();
   }

   static BlockPos posParte(BlockPos principal, Direction frente, int parte) {
      return principal.relative(derecha(frente), parte % 2).above(parte / 2);
   }

   public static BlockPos principal(BlockPos pos, BlockState estado) {
      int parte = (Integer)estado.getValue(PARTE);
      return pos.relative(derecha((Direction)estado.getValue(FACING)), -(parte % 2)).below(parte / 2);
   }

   public BlockState getStateForPlacement(BlockPlaceContext contexto) {
      Direction frente = contexto.getHorizontalDirection().getOpposite();
      BlockPos pos = contexto.getClickedPos();
      Level level = contexto.getLevel();

      for (int parte = 0; parte < 6; parte++) {
         BlockPos p = posParte(pos, frente, parte);
         if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(contexto)) {
            return null;
         }
      }

      return (BlockState)this.defaultBlockState().setValue(FACING, frente);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState estado, LivingEntity quien, ItemStack pila) {
      for (int parte = 1; parte < 6; parte++) {
         level.setBlock(posParte(pos, (Direction)estado.getValue(FACING), parte), (BlockState)estado.setValue(PARTE, parte), 3);
      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState estado, Player jugador) {
      if (!level.isClientSide && !jugador.isCreative() && jugador.hasCorrectToolForDrops(estado)) {
         popResource(level, pos, new ItemStack(ModBoveda.BOVEDA_ITEM));
      }

      return super.playerWillDestroy(level, pos, estado, jugador);
   }

   protected void onRemove(BlockState estado, Level level, BlockPos pos, BlockState nuevo, boolean movido) {
      if (!nuevo.is(this)) {
         BlockPos principal = principal(pos, estado);
         Direction frente = (Direction)estado.getValue(FACING);

         for (int parte = 0; parte < 6; parte++) {
            BlockPos p = posParte(principal, frente, parte);
            if (!p.equals(pos)) {
               BlockState otro = level.getBlockState(p);
               if (otro.is(this) && (Integer)otro.getValue(PARTE) == parte && otro.getValue(FACING) == frente) {
                  level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
               }
            }
         }
      }

      super.onRemove(estado, level, pos, nuevo, movido);
   }

   protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      }

      if (jugador instanceof ServerPlayer servidor) {
         BlockPos principal = principal(pos, estado);
         if (Hermandades.data(servidor).deJugador(servidor.getUUID()).isEmpty()) {
            servidor.displayClientMessage(Component.literal("Solo los miembros de una Hermandad pueden usar la Bóveda.").withColor(16739179), true);
            level.playSound(null, principal.above(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 0.5F);
            return InteractionResult.CONSUME;
         }

         if (level.getBlockEntity(principal) instanceof BovedaBlockEntity boveda) {
            boveda.pedirApertura(servidor);
         }

         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   static void setAbierta(Level level, BlockPos principal, BlockState estado, boolean abierta) {
      Direction frente = (Direction)estado.getValue(FACING);
      Block bloque = estado.getBlock();

      for (int parte = 0; parte < 6; parte++) {
         BlockPos p = posParte(principal, frente, parte);
         BlockState otro = level.getBlockState(p);
         if (otro.is(bloque) && (Boolean)otro.getValue(ABIERTA) != abierta) {
            level.setBlock(p, (BlockState)otro.setValue(ABIERTA, abierta), 3);
         }
      }
   }

   private static VoxelShape caja(Direction frente, double x0, double y0, double z0, double x1, double y1, double z1) {
      Direction der = derecha(frente);
      boolean derPos = der.getAxisDirection() == AxisDirection.POSITIVE;
      boolean frentePos = frente.getAxisDirection() == AxisDirection.POSITIVE;
      double a = derPos ? x0 : 16.0 - x1;
      double b = derPos ? x1 : 16.0 - x0;
      double c = frentePos ? z0 : 16.0 - z1;
      double d = frentePos ? z1 : 16.0 - z0;
      return der.getAxis() == Axis.X ? Block.box(a, y0, c, b, y1, d) : Block.box(c, y0, a, d, y1, b);
   }

   protected VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
      return caja((Direction)estado.getValue(FACING), 0.0, 0.0, 10.0, 16.0, 16.0, 16.0);
   }

   protected VoxelShape getCollisionShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
      return estado.getValue(ABIERTA) ? Shapes.empty() : this.getShape(estado, nivel, pos, contexto);
   }

   protected RenderShape getRenderShape(BlockState estado) {
      return RenderShape.INVISIBLE;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
      return estado.getValue(PARTE) == 0 ? new BovedaBlockEntity(pos, estado) : null;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState estado, BlockEntityType<T> tipo) {
      return level.isClientSide ? null : createTickerHelper(tipo, ModBoveda.ENTIDAD, BovedaBlockEntity::tickServidor);
   }
}
