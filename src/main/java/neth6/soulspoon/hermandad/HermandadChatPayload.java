package neth6.soulspoon.hermandad;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record HermandadChatPayload(List<HermandadChatPayload.Linea> lineas) implements CustomPacketPayload {
   public static final Type<HermandadChatPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandad_chat"));
   public static final StreamCodec<ByteBuf, HermandadChatPayload> CODEC = StreamCodec.composite(
      HermandadChatPayload.Linea.CODEC.apply(ByteBufCodecs.list()), HermandadChatPayload::lineas, HermandadChatPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Linea(String hora, String autor, String texto) {
      public static final StreamCodec<ByteBuf, HermandadChatPayload.Linea> CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         HermandadChatPayload.Linea::hora,
         ByteBufCodecs.STRING_UTF8,
         HermandadChatPayload.Linea::autor,
         ByteBufCodecs.STRING_UTF8,
         HermandadChatPayload.Linea::texto,
         HermandadChatPayload.Linea::new
      );
   }
}
