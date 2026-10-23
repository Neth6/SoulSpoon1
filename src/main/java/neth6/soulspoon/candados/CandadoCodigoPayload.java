package neth6.soulspoon.candados;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record CandadoCodigoPayload(BlockPos pos, boolean cerrar, String codigo) implements CustomPacketPayload {
   public static final Type<CandadoCodigoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "candado_codigo"));
   public static final StreamCodec<ByteBuf, CandadoCodigoPayload> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      CandadoCodigoPayload::pos,
      ByteBufCodecs.BOOL,
      CandadoCodigoPayload::cerrar,
      ByteBufCodecs.stringUtf8(16),
      CandadoCodigoPayload::codigo,
      CandadoCodigoPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
