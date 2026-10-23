package neth6.soulspoon.client;

import neth6.soulspoon.marcas.MarcasPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MarcasCliente {
   private static final double DISTANCIA_DIBUJO = 32.0;
   private static final float TAMANIO = 0.0065F;
   private static List<MarcasPayload.Marca> marcas = List.of();

   private MarcasCliente() {
   }

   public static void actualizar(MarcasPayload payload) {
      marcas = List.copyOf(payload.marcas());
   }

   /** Marcas del servidor + ubicaciones de hermandad (para el mapa). */
   public static List<MarcasPayload.Marca> todas() {
      List<MarcasPayload.Marca> l = new ArrayList<>(marcas);
      l.addAll(HermandadesCliente.ubicaciones());
      return l;
   }

   public static void limpiar() {
      marcas = List.of();
   }

   public static void dibujar(WorldRenderContext contexto) {
      Minecraft mc = Minecraft.getInstance();
      List<MarcasPayload.Marca> todas = new ArrayList<>(marcas);
      todas.addAll(HermandadesCliente.ubicaciones());
      if (!todas.isEmpty() && mc.level != null && !mc.options.hideGui) {
         String dimension = mc.level.dimension().location().toString();
         Vec3 camara = contexto.camera().getPosition();
         BufferSource buffers = mc.renderBuffers().bufferSource();
         Font font = mc.font;

         for (MarcasPayload.Marca marca : todas) {
            if (marca.dimension().equals(dimension)) {
               Vec3 relativa = new Vec3(marca.x() + 0.5, marca.y() + 1.2, marca.z() + 0.5).subtract(camara);
               double distancia = relativa.length();
               if (!(distancia < 1.5)) {
                  double dibujo = Math.min(distancia, 32.0);
                  Vec3 punto = relativa.scale(dibujo / distancia);
                  float escala = (float)(dibujo * 0.0065F);
                  PoseStack pose = new PoseStack();
                  pose.translate(punto.x, punto.y, punto.z);
                  pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                  pose.scale(escala, -escala, escala);
                  Matrix4f m = pose.last().pose();
                  int color = 0xFF000000 | marca.color();
                  linea(font, buffers, m, Component.literal("◆"), -22.0F, color);
                  linea(font, buffers, m, Component.literal(marca.nombre()), -11.0F, color);
                  linea(font, buffers, m, Component.literal((int)distancia + " m"), 0.0F, -3355444);
               }
            }
         }

         buffers.endBatch();
      }
   }

   private static void linea(Font font, MultiBufferSource buffers, Matrix4f m, Component texto, float y, int color) {
      float x = -font.width(texto) / 2.0F;
      font.drawInBatch(texto, x, y, color, false, m, buffers, DisplayMode.SEE_THROUGH, -1879048192, 15728880);
      font.drawInBatch(texto, x, y, color, true, m, buffers, DisplayMode.NORMAL, 0, 15728880);
   }
}
