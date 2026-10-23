package neth6.soulspoon.hermandad;

import neth6.soulspoon.marcas.MarcasPayload;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record HermandadUbicacionesPayload(List<MarcasPayload.Marca> ubicaciones) implements CustomPacketPayload {
   public static final Type<HermandadUbicacionesPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandad_ubicaciones"));
   public static final StreamCodec<ByteBuf, HermandadUbicacionesPayload> CODEC = MarcasPayload.Marca.CODEC
      .apply(ByteBufCodecs.list())
      .map(HermandadUbicacionesPayload::new, HermandadUbicacionesPayload::ubicaciones);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
