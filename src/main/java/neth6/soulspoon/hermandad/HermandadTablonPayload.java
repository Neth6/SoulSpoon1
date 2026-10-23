package neth6.soulspoon.hermandad;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record HermandadTablonPayload(List<HermandadTablonPayload.Anuncio> anuncios) implements CustomPacketPayload {
   public static final Type<HermandadTablonPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandad_tablon"));
   public static final StreamCodec<ByteBuf, HermandadTablonPayload> CODEC = StreamCodec.composite(
      HermandadTablonPayload.Anuncio.CODEC.apply(ByteBufCodecs.list()), HermandadTablonPayload::anuncios, HermandadTablonPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Anuncio(String fecha, String autor, String texto) {
      public static final StreamCodec<ByteBuf, HermandadTablonPayload.Anuncio> CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         HermandadTablonPayload.Anuncio::fecha,
         ByteBufCodecs.STRING_UTF8,
         HermandadTablonPayload.Anuncio::autor,
         ByteBufCodecs.STRING_UTF8,
         HermandadTablonPayload.Anuncio::texto,
         HermandadTablonPayload.Anuncio::new
      );
   }
}
