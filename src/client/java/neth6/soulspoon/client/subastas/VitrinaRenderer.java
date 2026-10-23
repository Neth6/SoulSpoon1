package neth6.soulspoon.client.subastas;

import neth6.soulspoon.subastas.VitrinaBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class VitrinaRenderer implements BlockEntityRenderer<VitrinaBlockEntity> {
   public VitrinaRenderer(Context contexto) {
   }

   public void render(VitrinaBlockEntity vitrina, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int overlay) {
      ItemStack item = vitrina.item();
      if (!item.isEmpty() && vitrina.getLevel() != null) {
         Minecraft mc = Minecraft.getInstance();
         float tiempo = (float)(vitrina.getLevel().getGameTime() % 7200L) + parcial;
         int brillo = 15728880;
         pose.pushPose();
         pose.translate(0.5, 0.45 + Math.sin(tiempo * 0.05) * 0.06, 0.5);
         pose.mulPose(Axis.YP.rotationDegrees(tiempo * 4.5F % 360.0F));
         pose.scale(0.75F, 0.75F, 0.75F);
         mc.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, brillo, overlay, pose, buffers, vitrina.getLevel(), 0);
         pose.popPose();
         pose.pushPose();
         pose.translate(0.5, 1.15, 0.5);
         pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
         pose.scale(0.02F, -0.02F, 0.02F);
         Font letra = mc.font;
         Matrix4f matriz = pose.last().pose();
         Component nombre = (Component)(item.getCount() > 1 ? item.getHoverName().copy().append(" x" + item.getCount()) : item.getHoverName());
         Component precio = Component.literal(vitrina.precio() + " deditas").withStyle(ChatFormatting.GOLD);
         int fondo = (int)(mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
         letra.drawInBatch(nombre, -letra.width(nombre) / 2.0F, 0.0F, -1, false, matriz, buffers, DisplayMode.NORMAL, fondo, brillo);
         letra.drawInBatch(precio, -letra.width(precio) / 2.0F, 11.0F, -1, false, matriz, buffers, DisplayMode.NORMAL, fondo, brillo);
         pose.popPose();
      }
   }

   public boolean shouldRenderOffScreen(VitrinaBlockEntity vitrina) {
      return true;
   }
}
