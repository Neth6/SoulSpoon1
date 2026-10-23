package neth6.soulspoon.cofres;

import com.mojang.serialization.MapCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class CofreHuesosBlock extends CofreBlock {
   public static final MapCodec<CofreHuesosBlock> CODEC = simpleCodec(CofreHuesosBlock::new);

   public CofreHuesosBlock(Properties propiedades) {
      super(propiedades);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected BlockEntityType<CofreBlockEntity> tipoEntidad() {
      return ModCofres.COFRE_HUESOS_ENTIDAD;
   }

   @Override
   public SoundEvent sonidoAbrir() {
      return SoundEvents.CHEST_OPEN;
   }

   @Override
   public SoundEvent sonidoCerrar() {
      return SoundEvents.CHEST_CLOSE;
   }
}
