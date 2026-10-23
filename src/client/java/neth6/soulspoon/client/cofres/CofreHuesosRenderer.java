package neth6.soulspoon.client.cofres;

import neth6.soulspoon.cofres.CofreBlock;
import neth6.soulspoon.cofres.CofreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class CofreHuesosRenderer implements BlockEntityRenderer<CofreBlockEntity> {
   private static final int HUESO = 0;
   private static final int PIEDRA = 1;
   private static final int INTERIOR = 2;
   private static final int ORO = 3;
   private static final int MORADO = 4;
   private static final String[] NOMBRES = new String[]{"hueso", "piedra", "interior", "oro", "morado"};
   private static final RenderType[] TIPOS = new RenderType[NOMBRES.length];
   private static final List<CofreHuesosRenderer.Pieza> PIEZAS;
   private static final int[][] CUERNOS;

   private static void pieza(float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz, int grupo) {
      PIEZAS.add(new CofreHuesosRenderer.Pieza(w, h, d, mat, x, y, z, rx, ry, rz, grupo));
   }

   public CofreHuesosRenderer(Context contexto) {
   }

   public void render(CofreBlockEntity cofre, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
      BlockState estado = cofre.getBlockState();
      if (estado.getBlock() instanceof CofreBlock) {
         dibujar(pose, buffers, luz, overlay, (Direction)estado.getValue(CofreBlock.FACING), cofre.getOpenNess(parcial), true);
      }
   }

   public static void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay, Direction frente, float abierto, boolean enElMundo) {
      pose.pushPose();
      if (enElMundo) {
         Direction derecha = frente.getClockWise();
         pose.translate(0.5 + derecha.getStepX() * 0.5, 0.0, 0.5 + derecha.getStepZ() * 0.5);
         pose.mulPose(Axis.YP.rotationDegrees(-frente.toYRot()));
      }

      float tapa = -1.3F * (1.0F - (1.0F - abierto) * (1.0F - abierto) * (1.0F - abierto));

      for (int mat = 0; mat < NOMBRES.length; mat++) {
         VertexConsumer vc = buffers.getBuffer(TIPOS[mat]);
         int l = mat != 3 && mat != 4 ? luz : 15728880;

         for (CofreHuesosRenderer.Pieza p : PIEZAS) {
            if (p.mat() == mat) {
               pose.pushPose();
               if (p.grupo() == 1 && tapa != 0.0F) {
                  pose.translate(0.0F, 0.38F, -0.44F);
                  pose.mulPose(Axis.XP.rotation(tapa));
                  pose.translate(0.0F, -0.38F, 0.44F);
               } else if (p.grupo() >= 2) {
                  int[] c = CUERNOS[p.grupo() - 2];
                  pose.translate(c[0] * 0.93F, 0.82F, c[1] * 0.36F);
                  pose.mulPose(Axis.ZP.rotation(-c[0] * 0.55F));
               }

               pose.translate(p.x(), p.y(), p.z());
               if (p.rx() != 0.0F) {
                  pose.mulPose(Axis.XP.rotation(p.rx()));
               }

               if (p.ry() != 0.0F) {
                  pose.mulPose(Axis.YP.rotation(p.ry()));
               }

               if (p.rz() != 0.0F) {
                  pose.mulPose(Axis.ZP.rotation(p.rz()));
               }

               caja(vc, pose.last(), p.w(), p.h(), p.d(), l, overlay);
               pose.popPose();
            }
         }
      }

      pose.popPose();
   }

   private static void caja(VertexConsumer vc, Pose p, float w, float h, float d, int luz, int overlay) {
      float x0 = -w / 2.0F;
      float x1 = w / 2.0F;
      float y0 = -h / 2.0F;
      float y1 = h / 2.0F;
      float z0 = -d / 2.0F;
      float z1 = d / 2.0F;
      cara(vc, p, luz, overlay, 1.0F, 0.0F, 0.0F, d, h, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
      cara(vc, p, luz, overlay, -1.0F, 0.0F, 0.0F, d, h, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
      cara(vc, p, luz, overlay, 0.0F, 1.0F, 0.0F, w, d, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
      cara(vc, p, luz, overlay, 0.0F, -1.0F, 0.0F, w, d, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
      cara(vc, p, luz, overlay, 0.0F, 0.0F, 1.0F, w, h, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
      cara(vc, p, luz, overlay, 0.0F, 0.0F, -1.0F, w, h, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
   }

   private static void cara(VertexConsumer vc, Pose p, int luz, int overlay, float nx, float ny, float nz, float ancho, float alto, float... q) {
      float[] us = new float[]{0.0F, ancho, ancho, 0.0F};
      float[] vs = new float[]{alto, alto, 0.0F, 0.0F};

      for (int i = 0; i < 4; i++) {
         vc.addVertex(p, q[i * 3], q[i * 3 + 1], q[i * 3 + 2])
            .setColor(255, 255, 255, 255)
            .setUv(us[i], vs[i])
            .setOverlay(overlay)
            .setLight(luz)
            .setNormal(p, nx, ny, nz);
      }
   }

   public boolean shouldRenderOffScreen(CofreBlockEntity cofre) {
      return true;
   }

   static {
      for (int i = 0; i < NOMBRES.length; i++) {
         TIPOS[i] = RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/entity/cofre_huesos/" + NOMBRES[i] + ".png"));
      }

      PIEZAS = new ArrayList<>();
      CUERNOS = new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
      pieza(1.9F, 0.36F, 0.92F, 1, 0.0F, 0.18F, 0.0F, 0.0F, 0.0F, 0.0F, 0);
      pieza(1.96F, 0.06F, 0.98F, 0, 0.0F, 0.03F, 0.0F, 0.0F, 0.0F, 0.0F, 0);
      pieza(1.72F, 0.5F, 0.78F, 2, 0.0F, 0.55F, 0.0F, 0.0F, 0.0F, 0.0F, 0);
      int[] s = new int[]{5};
      DoubleSupplier rnd = () -> (s[0] = (s[0] * 9301 + 49297) % 233280) / 233280.0;
      pieza(1.7F, 0.1F, 0.03F, 4, 0.0F, 0.14F, 0.47F, 0.0F, 0.0F, 0.0F, 0);

      for (int i = 0; i < 26; i++) {
         float sz = (float)(0.05 + rnd.getAsDouble() * 0.07);
         boolean gema = i % 3 != 0;
         float x = (float)((rnd.getAsDouble() - 0.5) * 1.8);
         float y = (float)(0.08 + rnd.getAsDouble() * 0.16);
         float z = (float)(0.47 + rnd.getAsDouble() * 0.06);
         float rx = (float)rnd.getAsDouble();
         float ry = (float)(rnd.getAsDouble() * 3.0);
         float rz = (float)rnd.getAsDouble();
         pieza(sz, gema ? sz : sz * 0.4F, sz, gema ? 4 : 3, x, y, z, rx, ry, rz, 0);
      }

      float[] xs = new float[]{-0.75F, -0.45F, -0.15F, 0.15F, 0.45F, 0.75F};

      for (float x : xs) {
         for (int i = 0; i < 6; i++) {
            float a = (i + 0.5F) * (float) Math.PI / 6.0F;
            float z = Mth.cos(a) * 0.44F;
            float y = 0.38F + Mth.sin(a) * 0.46F;
            pieza(0.15F, 0.1F, 0.26F, i % 2 == 1 ? 0 : 1, x, y, z, a + (float) (Math.PI / 2), 0.0F, 0.0F, 1);
         }

         pieza(0.17F, 0.1F, 0.1F, 0, x, 0.38F, 0.45F, 0.0F, 0.0F, 0.0F, 0);
         pieza(0.17F, 0.1F, 0.1F, 0, x, 0.38F, -0.45F, 0.0F, 0.0F, 0.0F, 0);
      }

      pieza(1.84F, 0.1F, 0.12F, 0, 0.0F, 0.86F, 0.0F, 0.0F, 0.0F, 0.0F, 1);

      for (int sx : new int[]{-1, 1}) {
         pieza(0.1F, 0.5F, 0.92F, 0, sx * 0.93F, 0.6F, 0.0F, 0.0F, 0.0F, 0.0F, 0);
      }

      for (int k = 0; k < CUERNOS.length; k++) {
         int sx = CUERNOS[k][0];
         int szz = CUERNOS[k][1];
         pieza(0.12F, 0.28F, 0.12F, 0, 0.0F, 0.14F, 0.0F, 0.0F, 0.0F, 0.0F, 2 + k);
         pieza(0.1F, 0.1F, 0.22F, 1, sx * 0.02F, 0.3F, -szz * 0.06F, szz * 0.5F, 0.0F, 0.0F, 2 + k);
      }

      pieza(0.3F, 0.3F, 0.05F, 0, 0.0F, 0.4F, 0.5F, 0.0F, 0.0F, 0.0F, 0);
      pieza(0.22F, 0.22F, 0.04F, 3, 0.0F, 0.4F, 0.525F, 0.0F, 0.0F, 0.0F, 0);
      pieza(0.08F, 0.1F, 0.02F, 2, 0.0F, 0.4F, 0.55F, 0.0F, 0.0F, 0.0F, 0);
   }

   private record Pieza(float w, float h, float d, int mat, float x, float y, float z, float rx, float ry, float rz, int grupo) {
   }
}
