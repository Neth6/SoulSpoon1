package neth6.soulspoon.client.cofres;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class MallaCofre {
   private static final ResourceLocation BLANCO = ResourceLocation.fromNamespaceAndPath("soulspoon", "textures/entity/blanco.png");
   private static final int LUZ_MAXIMA = 15728880;
   private final String archivo;
   private final Set<Integer> brillan;
   private final int interior;
   private List<MallaCofre.Cara> base;
   private List<MallaCofre.Cara> tapa;

   public MallaCofre(String archivo, Set<Integer> brillan, int interior) {
      this.archivo = archivo;
      this.brillan = brillan;
      this.interior = interior;
   }

   private void cargar() {
      if (this.base == null) {
         ResourceLocation id = ResourceLocation.fromNamespaceAndPath("soulspoon", "modelos/" + this.archivo);

         try (Reader lector = Minecraft.getInstance().getResourceManager().openAsReader(id)) {
            JsonObject json = JsonParser.parseReader(lector).getAsJsonObject();
            this.base = leer(json.getAsJsonArray("base"));
            this.tapa = leer(json.getAsJsonArray("tapa"));
         } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el modelo " + this.archivo, e);
         }
      }
   }

   private static List<MallaCofre.Cara> leer(JsonArray caras) {
      List<MallaCofre.Cara> lista = new ArrayList<>(caras.size());

      for (JsonElement e : caras) {
         JsonObject c = e.getAsJsonObject();
         float[] v = new float[12];
         JsonArray esquinas = c.getAsJsonArray("v");

         for (int i = 0; i < 4; i++) {
            JsonArray p = esquinas.get(i).getAsJsonArray();

            for (int k = 0; k < 3; k++) {
               v[i * 3 + k] = p.get(k).getAsFloat() / 16.0F;
            }
         }

         JsonArray n = c.getAsJsonArray("n");
         JsonArray col = c.getAsJsonArray("c");
         int color = 0xFF000000 | col.get(0).getAsInt() << 16 | col.get(1).getAsInt() << 8 | col.get(2).getAsInt();
         lista.add(new MallaCofre.Cara(v, n.get(0).getAsFloat(), n.get(1).getAsFloat(), n.get(2).getAsFloat(), color, c.get("k").getAsInt()));
      }

      return lista;
   }

   public void dibujar(PoseStack pose, MultiBufferSource buffers, int luz, int overlay, float abierto) {
      this.cargar();
      VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(BLANCO));
      float suave = 1.0F - (1.0F - abierto) * (1.0F - abierto) * (1.0F - abierto);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(180.0F));

      for (MallaCofre.Cara cara : this.base) {
         this.poner(vc, pose.last(), cara, luz, overlay, abierto);
      }

      pose.pushPose();
      pose.translate(0.0F, 0.6875F, 0.5F);
      pose.mulPose(Axis.XP.rotationDegrees(100.0F * suave));
      pose.translate(0.0F, -0.6875F, -0.5F);

      for (MallaCofre.Cara cara : this.tapa) {
         this.poner(vc, pose.last(), cara, luz, overlay, abierto);
      }

      pose.popPose();
      pose.popPose();
   }

   private void poner(VertexConsumer vc, Pose p, MallaCofre.Cara cara, int luz, int overlay, float abierto) {
      int luzCara = luz;
      if (this.brillan.contains(cara.tipo())) {
         luzCara = 15728880;
      } else if (cara.tipo() == this.interior && abierto > 0.3F) {
         luzCara = 15728880;
      }

      float[] v = cara.v();

      for (int i = 0; i < 4; i++) {
         vc.addVertex(p, v[i * 3], v[i * 3 + 1], v[i * 3 + 2])
            .setColor(cara.color())
            .setUv(0.5F, 0.5F)
            .setOverlay(overlay)
            .setLight(luzCara)
            .setNormal(p, cara.nx(), cara.ny(), cara.nz());
      }
   }

   private record Cara(float[] v, float nx, float ny, float nz, int color, int tipo) {
   }
}
