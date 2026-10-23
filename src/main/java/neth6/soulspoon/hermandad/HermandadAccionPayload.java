package neth6.soulspoon.hermandad;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record HermandadAccionPayload(int accion, String texto) implements CustomPacketPayload {
   public static final int PEDIR_INFO = 0;
   public static final int INVITAR = 1;
   public static final int EXPULSAR = 2;
   public static final int COLOR = 3;
   public static final int CHAT = 4;
   public static final int ESTANDARTE_GUARDAR = 6;
   public static final int ESTANDARTE_ACTIVAR = 7;
   public static final int ESTANDARTE_RECUPERAR = 8;
   public static final int ESTANDARTE_BORRAR = 9;
   public static final int ESTANDARTE_QUITAR = 10;
   public static final int ANUNCIO_PUBLICAR = 11;
   public static final int ANUNCIO_BORRAR = 12;
   public static final int SUBIR_RANGO = 13;
   public static final int BAJAR_RANGO = 14;
   public static final int DISOLVER = 15;
   public static final int SALIR = 16;
   public static final int UBICACION_AGREGAR = 17;
   public static final int UBICACION_BORRAR = 18;
   public static final int LARGO_CHAT = 256;
   public static final Type<HermandadAccionPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandad_accion"));
   public static final StreamCodec<ByteBuf, HermandadAccionPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, HermandadAccionPayload::accion, ByteBufCodecs.stringUtf8(256), HermandadAccionPayload::texto, HermandadAccionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
