package neth6.soulspoon.hermandad;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record HermandadInfoPayload(
   String nombre, int color, UUID maestro, List<HermandadInfoPayload.Miembro> miembros, long balance, int nivelBanco, String motd
) implements CustomPacketPayload {
   private static final StreamCodec<ByteBuf, List<HermandadInfoPayload.Miembro>> MIEMBROS = HermandadInfoPayload.Miembro.CODEC.apply(ByteBufCodecs.list());
   public static final Type<HermandadInfoPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandad_info"));
   public static final StreamCodec<ByteBuf, HermandadInfoPayload> CODEC = StreamCodec.of(
      (buf, p) -> {
         ByteBufCodecs.STRING_UTF8.encode(buf, p.nombre());
         ByteBufCodecs.INT.encode(buf, p.color());
         UUIDUtil.STREAM_CODEC.encode(buf, p.maestro());
         MIEMBROS.encode(buf, p.miembros());
         ByteBufCodecs.VAR_LONG.encode(buf, p.balance());
         ByteBufCodecs.VAR_INT.encode(buf, p.nivelBanco());
         ByteBufCodecs.STRING_UTF8.encode(buf, p.motd());
      },
      buf -> new HermandadInfoPayload(
         (String)ByteBufCodecs.STRING_UTF8.decode(buf),
         (Integer)ByteBufCodecs.INT.decode(buf),
         (UUID)UUIDUtil.STREAM_CODEC.decode(buf),
         (List<HermandadInfoPayload.Miembro>)MIEMBROS.decode(buf),
         (Long)ByteBufCodecs.VAR_LONG.decode(buf),
         (Integer)ByteBufCodecs.VAR_INT.decode(buf),
         (String)ByteBufCodecs.STRING_UTF8.decode(buf)
      )
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Miembro(UUID uuid, String nombre, boolean lider) {
      public static final StreamCodec<ByteBuf, HermandadInfoPayload.Miembro> CODEC = StreamCodec.composite(
         UUIDUtil.STREAM_CODEC,
         HermandadInfoPayload.Miembro::uuid,
         ByteBufCodecs.STRING_UTF8,
         HermandadInfoPayload.Miembro::nombre,
         ByteBufCodecs.BOOL,
         HermandadInfoPayload.Miembro::lider,
         HermandadInfoPayload.Miembro::new
      );
   }
}
