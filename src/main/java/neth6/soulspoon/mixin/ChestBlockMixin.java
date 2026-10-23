package neth6.soulspoon.mixin;

import neth6.soulspoon.candados.Candados;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.ChestBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public class ChestBlockMixin {
    @Inject(method = "candidatePartnerFacing", at = @At("RETURN"), cancellable = true)
    private void soulspoon$noJuntarConCandado(BlockPlaceContext contexto, Direction direccion, CallbackInfoReturnable<Direction> cir) {
        if (cir.getReturnValue() != null && Candados.tieneCandado(contexto.getLevel(), contexto.getClickedPos().relative(direccion))) {
            cir.setReturnValue(null);
        }
    }
}
