package neth6.soulspoon.subastas;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VitrinaBlock extends Block implements EntityBlock {
   private static final VoxelShape FORMA = Block.box(3.0, 3.0, 3.0, 13.0, 13.0, 13.0);

   public VitrinaBlock(Properties propiedades) {
      super(propiedades);
   }

   protected RenderShape getRenderShape(BlockState estado) {
      return RenderShape.INVISIBLE;
   }

   protected VoxelShape getShape(BlockState estado, BlockGetter level, BlockPos pos, CollisionContext contexto) {
      return FORMA;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
      return new VitrinaBlockEntity(pos, estado);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState estado, BlockEntityType<T> tipo) {
      if (level.isClientSide || tipo != ModVitrina.ENTIDAD) {
         return null;
      }

      BlockEntityTicker<VitrinaBlockEntity> ticker = VitrinaBlockEntity::tick;
      @SuppressWarnings("unchecked")
      BlockEntityTicker<T> resultado = (BlockEntityTicker<T>)(BlockEntityTicker<?>)ticker;
      return resultado;
   }

   protected InteractionResult useWithoutItem(BlockState estado, Level level, BlockPos pos, Player jugador, BlockHitResult golpe) {
      if (jugador instanceof ServerPlayer servidor) {
         Subastas.abrir(servidor, 0);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}