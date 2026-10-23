package neth6.soulspoon.marcas;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record MarcasPayload(List<MarcasPayload.Marca> marcas) implements CustomPacketPayload {
   public static final Type<MarcasPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "marcas"));
   public static final StreamCodec<ByteBuf, MarcasPayload> CODEC = MarcasPayload.Marca.CODEC
      .apply(ByteBufCodecs.list())
      .map(MarcasPayload::new, MarcasPayload::marcas);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Marca(String nombre, String dimension, int x, int y, int z, int color) {
      public static final StreamCodec<ByteBuf, MarcasPayload.Marca> CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         MarcasPayload.Marca::nombre,
         ByteBufCodecs.STRING_UTF8,
         MarcasPayload.Marca::dimension,
         ByteBufCodecs.VAR_INT,
         MarcasPayload.Marca::x,
         ByteBufCodecs.VAR_INT,
         MarcasPayload.Marca::y,
         ByteBufCodecs.VAR_INT,
         MarcasPayload.Marca::z,
         ByteBufCodecs.INT,
         MarcasPayload.Marca::color,
         MarcasPayload.Marca::new
      );
   }
}
