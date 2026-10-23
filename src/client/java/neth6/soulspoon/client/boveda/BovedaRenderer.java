package neth6.soulspoon.client.boveda;

import neth6.soulspoon.boveda.BovedaBlock;
import neth6.soulspoon.boveda.BovedaBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class BovedaRenderer implements BlockEntityRenderer<BovedaBlockEntity> {
   private static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/entity/boveda.png");
   private static final float P = 0.0625F;
   private static final float GROSOR = 0.375F;
   private static final float TEX = 128.0F;
   private static final int BLANCO = 0;
   private static final int BORDE = 1;
   private static final int HIERRO = 2;
   private static final int ACERO = 3;
   private static final int MADERA = 4;
   private static final int MADERA_OSCURA = 5;
   private static final List<int[]> RUEDA = new ArrayList<>();

   public BovedaRenderer(Context contexto) {
   }

   private static float tramo(float t, float a, float b) {
      return Math.min(1.0F, Math.max(0.0F, (t - a) / (b - a)));
   }

   private static float suave(float t) {
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   public void render(BovedaBlockEntity boveda, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
      float t = boveda.progreso(parcial);
      float giroRueda = -180.0F * suave(tramo(t, 0.0F, 0.25F));
      float cerrojos = -0.3F * suave(tramo(t, 0.15F, 0.32F));
      float separacion = 0.06F * suave(tramo(t, 0.32F, 0.42F));
      float giroPuerta = -105.0F * suave(tramo(t, 0.42F, 1.0F));
      VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEXTURA));
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.mulPose(Axis.YP.rotationDegrees(-((Direction)boveda.getBlockState().getValue(BovedaBlock.FACING)).toYRot()));
      pose.translate(-0.5, 0.0, 0.5);
      pose.mulPose(Axis.YP.rotationDegrees(giroPuerta));
      pose.translate(0.0F, 0.0F, separacion);
      Pose p = pose.last();
      cara(
         vc,
         p,
         new float[]{0.0F, 0.0F, 0.0F},
         new float[]{2.0F, 0.0F, 0.0F},
         new float[]{2.0F, 3.0F, 0.0F},
         new float[]{0.0F, 3.0F, 0.0F},
         0.0F,
         0.0F,
         0.5F,
         0.75F,
         0.0F,
         0.0F,
         1.0F,
         luz,
         overlay
      );
      caja(vc, p, 0.0F, 0.0F, -0.375F, 2.0F, 3.0F, -0.001F, 1, false, luz, overlay);

      for (float y : new float[]{0.4F, 1.5F, 2.6F}) {
         caja(vc, p, -0.03125F, y - 0.15625F, 0.0F, 0.28125F, y + 0.15625F, 0.09375F, 2, true, luz, overlay);
         caja(vc, p, -0.09375F, y - 0.1875F, -0.09375F, 0.09375F, y + 0.1875F, 0.09375F, 2, true, luz, overlay);
      }

      caja(vc, p, 0.15F, 0.15F, -0.4375F, 1.85F, 2.85F, -0.375F, 3, true, luz, overlay);
      caja(vc, p, 1.64F, 0.3F, -0.53125F, 1.76F, 2.7F, -0.4375F, 2, true, luz, overlay);

      for (float y : new float[]{0.5F, 1.5F, 2.5F}) {
         caja(vc, p, 1.75F + cerrojos, y - 0.09375F, -0.28125F, 2.25F + cerrojos, y + 0.09375F, -0.09375F, 3, true, luz, overlay);
      }

      pose.pushPose();
      pose.translate(0.5625F, 2.25F, 0.1F);
      pose.mulPose(Axis.ZP.rotationDegrees(giroRueda));
      Pose pr = pose.last();

      for (int[] cubo : RUEDA) {
         float x = cubo[0] * 0.0625F;
         float y = cubo[1] * 0.0625F;
         boolean centro = cubo[2] == 1;
         caja(vc, pr, x - 0.03125F, y - 0.03125F, -0.03125F, x + 0.03125F, y + 0.03125F, centro ? 0.09375F : 0.03125F, centro ? 5 : 4, true, luz, overlay);
      }

      caja(vc, pr, -0.03125F, -0.03125F, -0.1F, 0.03125F, 0.03125F, -0.03125F, 2, true, luz, overlay);
      pose.popPose();
      float x0 = 1.3125F;
      float x1 = 1.8125F;
      float y0 = 0.25F;
      float y1 = 0.75F;
      float z = 0.002F;
      cara(
         vc,
         p,
         new float[]{x0, y0, z},
         new float[]{x1, y0, z},
         new float[]{x1, y1, z},
         new float[]{x0, y1, z},
         0.328125F,
         0.5625F,
         0.453125F,
         0.6875F,
         0.0F,
         0.0F,
         1.0F,
         15728880,
         overlay
      );
      pose.popPose();
   }

   private static void caja(
      VertexConsumer vc, Pose p, float x0, float y0, float z0, float x1, float y1, float z1, int color, boolean frente, int luz, int overlay
   ) {
      float u0 = (color * 8 + 2) / 128.0F;
      float u1 = (color * 8 + 6) / 128.0F;
      float v0 = 0.890625F;
      float v1 = 0.921875F;
      if (frente) {
         cara(
            vc,
            p,
            new float[]{x0, y0, z1},
            new float[]{x1, y0, z1},
            new float[]{x1, y1, z1},
            new float[]{x0, y1, z1},
            u0,
            v0,
            u1,
            v1,
            0.0F,
            0.0F,
            1.0F,
            luz,
            overlay
         );
      }

      float ua = frente ? u0 : 0.015625F;
      float ub = frente ? u1 : 0.046875F;
      cara(
         vc,
         p,
         new float[]{x1, y0, z0},
         new float[]{x0, y0, z0},
         new float[]{x0, y1, z0},
         new float[]{x1, y1, z0},
         ua,
         v0,
         ub,
         v1,
         0.0F,
         0.0F,
         -1.0F,
         luz,
         overlay
      );
      cara(
         vc,
         p,
         new float[]{x1, y0, z1},
         new float[]{x1, y0, z0},
         new float[]{x1, y1, z0},
         new float[]{x1, y1, z1},
         u0,
         v0,
         u1,
         v1,
         1.0F,
         0.0F,
         0.0F,
         luz,
         overlay
      );
      cara(
         vc,
         p,
         new float[]{x0, y0, z0},
         new float[]{x0, y0, z1},
         new float[]{x0, y1, z1},
         new float[]{x0, y1, z0},
         u0,
         v0,
         u1,
         v1,
         -1.0F,
         0.0F,
         0.0F,
         luz,
         overlay
      );
      cara(
         vc,
         p,
         new float[]{x0, y1, z1},
         new float[]{x1, y1, z1},
         new float[]{x1, y1, z0},
         new float[]{x0, y1, z0},
         u0,
         v0,
         u1,
         v1,
         0.0F,
         1.0F,
         0.0F,
         luz,
         overlay
      );
      cara(
         vc,
         p,
         new float[]{x0, y0, z0},
         new float[]{x1, y0, z0},
         new float[]{x1, y0, z1},
         new float[]{x0, y0, z1},
         u0,
         v0,
         u1,
         v1,
         0.0F,
         -1.0F,
         0.0F,
         luz,
         overlay
      );
   }

   private static void cara(
      VertexConsumer vc,
      Pose p,
      float[] a,
      float[] b,
      float[] c,
      float[] d,
      float u0,
      float v0,
      float u1,
      float v1,
      float nx,
      float ny,
      float nz,
      int luz,
      int overlay
   ) {
      vertice(vc, p, a, u0, v1, nx, ny, nz, luz, overlay);
      vertice(vc, p, b, u1, v1, nx, ny, nz, luz, overlay);
      vertice(vc, p, c, u1, v0, nx, ny, nz, luz, overlay);
      vertice(vc, p, d, u0, v0, nx, ny, nz, luz, overlay);
   }

   private static void vertice(VertexConsumer vc, Pose p, float[] xyz, float u, float v, float nx, float ny, float nz, int luz, int overlay) {
      vc.addVertex(p, xyz[0], xyz[1], xyz[2]).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(luz).setNormal(p, nx, ny, nz);
   }

   public boolean shouldRenderOffScreen(BovedaBlockEntity boveda) {
      return true;
   }

   public int getViewDistance() {
      return 96;
   }

   static {
      for (int gx = -6; gx <= 6; gx++) {
         for (int gy = -6; gy <= 6; gy++) {
            double r = Math.hypot(gx, gy);
            int ax = Math.abs(gx);
            int ay = Math.abs(gy);
            boolean aro = r >= 3.5 && r < 4.6;
            boolean rayo = r < 3.5 && (gx == 0 || gy == 0 || ax == ay);
            boolean manija = (gx == 0 || gy == 0) && r >= 4.6 || ax == 4 && ay == 4;
            if (aro || rayo || manija) {
               RUEDA.add(new int[]{gx, gy, r < 1.5 ? 1 : 0});
            }
         }
      }
   }
}
