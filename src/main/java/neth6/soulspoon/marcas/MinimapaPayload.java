package neth6.soulspoon.marcas;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record MinimapaPayload(boolean adminsLoUsan) implements CustomPacketPayload {
   public static final Type<MinimapaPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("soulspoon", "minimapa"));
   public static final StreamCodec<ByteBuf, MinimapaPayload> CODEC = ByteBufCodecs.BOOL.map(MinimapaPayload::new, MinimapaPayload::adminsLoUsan);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
