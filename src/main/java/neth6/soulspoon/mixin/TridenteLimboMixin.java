package neth6.soulspoon.mixin;

import neth6.soulspoon.world.Limbo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class TridenteLimboMixin {
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void soulspoon$tridenteSoloEnElLimbo(Entity objetivo, CallbackInfo ci) {
      Player jugador = (Player)(Object)this;
      if (jugador.getMainHandItem().is(Items.TRIDENT) && !jugador.level().dimension().equals(Limbo.DIMENSION)) {
         ci.cancel();
      }
   }
}