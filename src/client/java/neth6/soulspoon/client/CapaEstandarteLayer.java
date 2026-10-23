package neth6.soulspoon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class CapaEstandarteLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private final ModelPart bandera;

   public CapaEstandarteLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
      super(padre);
      this.bandera = modelos.bakeLayer(ModelLayers.BANNER).getChild("flag");
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int luz,
      AbstractClientPlayer jugador,
      float limbSwing,
      float limbSwingAmount,
      float parcial,
      float edad,
      float cabezaGiro,
      float cabezaArriba
   ) {
      if (!jugador.isInvisible() && jugador.isModelPartShown(PlayerModelPart.CAPE)) {
         if (!jugador.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            ItemStack estandarte = HermandadesCliente.estandarteDe(jugador.getUUID());
            if (estandarte.getItem() instanceof BannerItem item) {
               pose.pushPose();
               pose.translate(0.0F, 0.0F, 0.125F);
               double var29 = Mth.lerp(parcial, jugador.xCloakO, jugador.xCloak) - Mth.lerp(parcial, jugador.xo, jugador.getX());
               double dy = Mth.lerp(parcial, jugador.yCloakO, jugador.yCloak) - Mth.lerp(parcial, jugador.yo, jugador.getY());
               double dz = Mth.lerp(parcial, jugador.zCloakO, jugador.zCloak) - Mth.lerp(parcial, jugador.zo, jugador.getZ());
               float cuerpo = Mth.rotLerp(parcial, jugador.yBodyRotO, jugador.yBodyRot);
               double seno = Mth.sin(cuerpo * (float) (Math.PI / 180.0));
               double coseno = -Mth.cos(cuerpo * (float) (Math.PI / 180.0));
               float sube = Mth.clamp((float)dy * 10.0F, -6.0F, 32.0F);
               float atras = Mth.clamp((float)(var29 * seno + dz * coseno) * 100.0F, 0.0F, 150.0F);
               float costado = Mth.clamp((float)(var29 * coseno - dz * seno) * 100.0F, -20.0F, 20.0F);
               float balanceo = Mth.lerp(parcial, jugador.oBob, jugador.bob);
               sube += Mth.sin(Mth.lerp(parcial, jugador.walkDistO, jugador.walkDist) * 6.0F) * 32.0F * balanceo;
               if (jugador.isCrouching()) {
                  sube += 25.0F;
               }

               pose.mulPose(Axis.XP.rotationDegrees(6.0F + atras / 2.0F + sube));
               pose.mulPose(Axis.ZP.rotationDegrees(costado / 2.0F));
               pose.mulPose(Axis.YP.rotationDegrees(180.0F - costado / 2.0F));
               pose.scale(0.5F, 0.4F, 1.0F);
               this.bandera.x = 0.0F;
               this.bandera.y = 0.0F;
               this.bandera.z = 1.0F;
               this.bandera.xRot = 0.0F;
               BannerPatternLayers dibujos = (BannerPatternLayers)estandarte.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
               BannerRenderer.renderPatterns(
                  pose, buffers, luz, OverlayTexture.NO_OVERLAY, this.bandera, ModelBakery.BANNER_BASE, true, item.getColor(), dibujos
               );
               pose.popPose();
            }
         }
      }
   }
}
