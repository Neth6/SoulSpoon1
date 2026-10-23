package neth6.soulspoon.candados;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record CandadoPantallaPayload(BlockPos pos, boolean cerrar, String aviso) implements CustomPacketPayload {
   public static final Type<CandadoPantallaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "candado_pantalla"));
   public static final StreamCodec<ByteBuf, CandadoPantallaPayload> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      CandadoPantallaPayload::pos,
      ByteBufCodecs.BOOL,
      CandadoPantallaPayload::cerrar,
      ByteBufCodecs.stringUtf8(64),
      CandadoPantallaPayload::aviso,
      CandadoPantallaPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
