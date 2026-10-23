package neth6.soulspoon.candados;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record CandadosListaPayload(List<BlockPos> posiciones) implements CustomPacketPayload {
   public static final Type<CandadosListaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "candados_lista"));
   public static final StreamCodec<ByteBuf, CandadosListaPayload> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), CandadosListaPayload::posiciones, CandadosListaPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
