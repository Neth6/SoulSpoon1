package neth6.soulspoon.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import neth6.soulspoon.candados.Candados;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Las explosiones no rompen cofres/barriles/shulkers con candado. */
@Mixin(Explosion.class)
public abstract class ExplosionCandadoMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;

    @Inject(method = "explode", at = @At("RETURN"))
    private void soulspoon$respetarCandados(CallbackInfo ci) {
        if (this.level instanceof ServerLevel mundo) {
            this.toBlow.removeIf(pos -> Candados.sePuedeCerrar(mundo.getBlockState(pos)) && Candados.cerradura(mundo, pos) != null);
        }
    }
}
