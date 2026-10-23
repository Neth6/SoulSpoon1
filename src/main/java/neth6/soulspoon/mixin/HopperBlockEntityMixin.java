package neth6.soulspoon.mixin;

import neth6.soulspoon.candados.Candados;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "getBlockContainer", at = @At("HEAD"), cancellable = true)
    private static void soulspoon$sinTolvaEnCandado(Level level, BlockPos pos, BlockState estado, CallbackInfoReturnable<Container> cir) {
        if (!level.isClientSide && Candados.tieneCandado(level, pos)) {
            cir.setReturnValue(null);
        }
    }
}
