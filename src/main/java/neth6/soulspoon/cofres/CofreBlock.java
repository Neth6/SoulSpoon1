package neth6.soulspoon.cofres;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CofreBlock extends BaseEntityBlock {
   public static final MapCodec<CofreBlock> CODEC = simpleCodec(CofreBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final EnumProperty<CofreBlock.Parte> PARTE = EnumProperty.create("parte", CofreBlock.Parte.class);

   public CofreBlock(Properties propiedades) {
      super(propiedades);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(PARTE, CofreBlock.Parte.PRINCIPAL)
      );
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> constructor) {
      constructor.add(new Property[]{FACING, PARTE});
   }

   public static Direction haciaLaOtra(BlockState estado) {
      Direction derecha = ((Direction)estado.getValue(FACING)).getClockWise();
      return estado.getValue(PARTE) == CofreBlock.Parte.PRINCIPAL ? derecha : derecha.getOpposite();
   }

   public static BlockPos principal(BlockState estado, BlockPos pos) {
      return estado.getValue(PARTE) == CofreBlock.Parte.PRINCIPAL ? pos : pos.relative(haciaLaOtra(estado));
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext contexto) {
      Direction frente = contexto.getHorizontalDirection().getOpposite();
      BlockPos otra = contexto.getClickedPos().relative(frente.getClockWise());
      return !contexto.getLevel().getBlockState(otra).canBeReplaced(contexto)
         ? null
         : (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, frente)).setValue(PARTE, CofreBlock.Parte.PRINCIPAL);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState estado, @Nullable LivingEntity quien, ItemStack pila) {
      super.setPlacedBy(level, pos, estado, quien, pila);
      if (!level.isClientSide) {
         BlockPos otra = pos.relative(haciaLaOtra(estado));
         level.setBlock(otra, (BlockState)estado.setValue(PARTE, CofreBlock.Parte.SECUNDARIA), 3);
      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState estado, Player jugador) {
      BlockPos otra = pos.relative(haciaLaOtra(estado));
      BlockState estadoOtra = level.getBlockState(otra);
      if (estadoOtra.is(this) && estadoOtra.getValue(PARTE) != estado.getValue(PARTE)) {
         if (!level.isClientSide && estado.getValue(PARTE) == CofreBlock.Parte.SECUNDARIA && !jugador.isCreative() && jugador.hasCorrectToolForDrops(estado)) {
            Block.popResource(level, pos, new ItemStack(this.asItem()));
         }

         level.setBlock(otra, Blocks.AIR.defaultBlockState(), 35);
         level.levelEvent(jugador, 2001, otra, Block.getId(estadoOtra));
      }

      return super.playerWillDestroy(level, pos, estado, jugador);
   }

   protected void onRemove(BlockState estado, Level level, BlockPos pos, BlockState nuevo, boolean movido) {
      if (!estado.is(nuevo.getBlock()) && level.getBlockEntity(pos) instanceof CofreBlockEntity cofre) {
         Containers.dropContents(level, pos, cofre);
         level.updateNeighbourForOutputSignal(pos, this);
      }

      super.onRemove(estado, level, pos, nuevo, movido);
   }

   protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult hit) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      }

      if (level.getBlockEntity(principal(estado, pos)) instanceof CofreBlockEntity cofre) {
         jugador.openMenu(cofre);
      }

      return InteractionResult.CONSUME;
   }

   protected RenderShape getRenderShape(BlockState estado) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
      return estado.getValue(PARTE) == CofreBlock.Parte.PRINCIPAL ? new CofreBlockEntity(this.tipoEntidad(), pos, estado) : null;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState estado, BlockEntityType<T> tipo) {
      return level.isClientSide ? createTickerHelper(tipo, this.tipoEntidad(), CofreBlockEntity::tickCliente) : null;
   }

   protected BlockEntityType<CofreBlockEntity> tipoEntidad() {
      return ModCofres.COFRE_ENTIDAD;
   }

   public SoundEvent sonidoAbrir() {
      return ModCofres.SONIDO_ABRIR;
   }

   public SoundEvent sonidoCerrar() {
      return ModCofres.SONIDO_CERRAR;
   }

   protected void tick(BlockState estado, ServerLevel level, BlockPos pos, RandomSource azar) {
      if (level.getBlockEntity(pos) instanceof CofreBlockEntity cofre) {
         cofre.revisarQuienMira();
      }
   }

   protected boolean triggerEvent(BlockState estado, Level level, BlockPos pos, int id, int dato) {
      super.triggerEvent(estado, level, pos, id, dato);
      BlockEntity entidad = level.getBlockEntity(pos);
      return entidad != null && entidad.triggerEvent(id, dato);
   }

   protected boolean hasAnalogOutputSignal(BlockState estado) {
      return true;
   }

   protected int getAnalogOutputSignal(BlockState estado, Level level, BlockPos pos) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(principal(estado, pos)));
   }

   public static float apertura(BlockGetter level, BlockPos pos, float parcial) {
      return level.getBlockEntity(pos) instanceof CofreBlockEntity cofre ? cofre.getOpenNess(parcial) : 0.0F;
   }

   public enum Parte implements StringRepresentable {
      PRINCIPAL("principal"),
      SECUNDARIA("secundaria");

      private final String nombre;

      Parte(String nombre) {
         this.nombre = nombre;
      }

      public String getSerializedName() {
         return this.nombre;
      }
   }
}
