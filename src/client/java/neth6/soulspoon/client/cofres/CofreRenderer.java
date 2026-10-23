package neth6.soulspoon.client.cofres;

import neth6.soulspoon.cofres.CofreBlock;
import neth6.soulspoon.cofres.CofreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class CofreRenderer implements BlockEntityRenderer<CofreBlockEntity> {
   private static final MallaCofre MALLA = new MallaCofre("cofre.json", Set.of(3, 4, 5), 6);

   public CofreRenderer(Context contexto) {
   }

   public void render(CofreBlockEntity cofre, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
      BlockState estado = cofre.getBlockState();
      if (estado.getBlock() instanceof CofreBlock) {
         float abierto = cofre.getOpenNess(parcial);
         dibujar(pose, buffers, luz, overlay, (Direction)estado.getValue(CofreBlock.FACING), abierto, true);
      }
   }

   public static void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay, Direction frente, float abierto, boolean enElMundo) {
      pose.pushPose();
      if (enElMundo) {
         Direction derecha = frente.getClockWise();
         pose.translate(0.5 + derecha.getStepX() * 0.5, 0.0, 0.5 + derecha.getStepZ() * 0.5);
         pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));
      }

      MALLA.dibujar(pose, buffers, luz, overlay, abierto);
      pose.popPose();
   }

   public boolean shouldRenderOffScreen(CofreBlockEntity cofre) {
      return true;
   }
}
