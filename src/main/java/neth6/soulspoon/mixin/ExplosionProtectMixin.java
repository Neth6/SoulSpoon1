package neth6.soulspoon.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import neth6.soulspoon.soul.ProtectedZone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Las explosiones (TNT, creepers, bombas...) no rompen bloques dentro de la zona protegida del altar.
 * Los jugadores y mobs si reciben dano; solo se salva el terreno.
 */
@Mixin(Explosion.class)
public abstract class ExplosionProtectMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;

    @Inject(method = "explode", at = @At("RETURN"))
    private void soulspoon$protegerZona(CallbackInfo ci) {
        this.toBlow.removeIf(pos -> ProtectedZone.isProtected(this.level, pos));
    }
}
