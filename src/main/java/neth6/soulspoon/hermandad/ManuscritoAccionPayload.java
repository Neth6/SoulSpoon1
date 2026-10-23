package neth6.soulspoon.hermandad;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record ManuscritoAccionPayload(int accion, String nombre, boolean manoPrincipal) implements CustomPacketPayload {
   public static final int GUARDAR_NOMBRE = 0;
   public static final int INSCRIBIRSE = 1;
   public static final int CREAR = 2;
   public static final Type<ManuscritoAccionPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "manuscrito_accion"));
   public static final StreamCodec<ByteBuf, ManuscritoAccionPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ManuscritoAccionPayload::accion,
      ByteBufCodecs.stringUtf8(64),
      ManuscritoAccionPayload::nombre,
      ByteBufCodecs.BOOL,
      ManuscritoAccionPayload::manoPrincipal,
      ManuscritoAccionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
