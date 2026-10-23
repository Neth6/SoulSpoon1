package neth6.soulspoon.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;

public final class MapaHermandad {
   public static final ResourceLocation TEXTURA = ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandad_mapa");
   private static final int SIN_CARGAR = -15462386;
   private static DynamicTexture textura;
   private static int ancho;
   private static int alto;
   private static long armado;
   private static int ultimaEscala;

   private MapaHermandad() {
   }

   public static void actualizar(int w, int h, int cx, int cz, int escala) {
      long ahora = Util.getMillis();
      if (textura == null || w != ancho || h != alto || escala != ultimaEscala || ahora - armado >= 1500L) {
         Minecraft mc = Minecraft.getInstance();
         ClientLevel level = mc.level;
         if (level != null && mc.player != null) {
            armado = ahora;
            ultimaEscala = escala;
            int yJugador = mc.player.getBlockY();
            NativeImage img = new NativeImage(w, h, false);
            int[] alturaArriba = new int[w];

            for (int pz = 0; pz < h; pz++) {
               int z = cz + (pz - h / 2) * escala;

               for (int px = 0; px < w; px++) {
                  int x = cx + (px - w / 2) * escala;
                  if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                     img.setPixelRGBA(px, pz, -15462386);
                     alturaArriba[px] = Integer.MIN_VALUE;
                  } else {
                     int y = altura(level, x, z, yJugador);
                     BlockState estado = level.getBlockState(new BlockPos(x, y, z));
                     MapColor color = estado.getMapColor(level, new BlockPos(x, y, z));
                     Brightness brillo = Brightness.NORMAL;
                     if (alturaArriba[px] != Integer.MIN_VALUE && pz > 0) {
                        if (y > alturaArriba[px]) {
                           brillo = Brightness.HIGH;
                        } else if (y < alturaArriba[px]) {
                           brillo = Brightness.LOW;
                        }
                     }

                     alturaArriba[px] = y;
                     img.setPixelRGBA(px, pz, color == MapColor.NONE ? -15462386 : color.calculateRGBColor(brillo));
                  }
               }
            }

            if (textura != null && w == ancho && h == alto) {
               textura.setPixels(img);
               textura.upload();
            } else {
               if (textura != null) {
                  textura.close();
               }

               textura = new DynamicTexture(img);
               mc.getTextureManager().register(TEXTURA, textura);
               ancho = w;
               alto = h;
            }
         }
      }
   }

   public static void olvidar() {
      armado = 0L;
   }

   public static int altura(ClientLevel level, int x, int z, int yJugador) {
      int arriba = level.getHeight(Types.WORLD_SURFACE, x, z) - 1;
      if (arriba <= yJugador + 30) {
         return Math.max(level.getMinBuildHeight(), arriba);
      }

      MutableBlockPos pos = new MutableBlockPos(x, yJugador + 10, z);
      int minimo = level.getMinBuildHeight();

      for (int i = 0; i < 40 && pos.getY() > minimo && !level.getBlockState(pos).isAir(); i++) {
         pos.move(0, -1, 0);
      }

      for (int i = 0; i < 120 && pos.getY() > minimo && level.getBlockState(pos).isAir(); i++) {
         pos.move(0, -1, 0);
      }

      return pos.getY();
   }
}
