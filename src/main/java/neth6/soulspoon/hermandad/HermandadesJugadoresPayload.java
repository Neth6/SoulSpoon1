package neth6.soulspoon.hermandad;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record HermandadesJugadoresPayload(List<HermandadesJugadoresPayload.Entrada> entradas, List<HermandadesJugadoresPayload.Activo> activos)
   implements CustomPacketPayload {
   public static final Type<HermandadesJugadoresPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandades_jugadores"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HermandadesJugadoresPayload> CODEC = StreamCodec.composite(
      HermandadesJugadoresPayload.Entrada.CODEC.apply(ByteBufCodecs.list()),
      HermandadesJugadoresPayload::entradas,
      HermandadesJugadoresPayload.Activo.CODEC.apply(ByteBufCodecs.list()),
      HermandadesJugadoresPayload::activos,
      HermandadesJugadoresPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Activo(String hermandad, ItemStack estandarte) {
      public static final StreamCodec<RegistryFriendlyByteBuf, HermandadesJugadoresPayload.Activo> CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         HermandadesJugadoresPayload.Activo::hermandad,
         ItemStack.STREAM_CODEC,
         HermandadesJugadoresPayload.Activo::estandarte,
         HermandadesJugadoresPayload.Activo::new
      );
   }

   public record Entrada(UUID jugador, String hermandad, int color, String rango) {
      public static final StreamCodec<ByteBuf, HermandadesJugadoresPayload.Entrada> CODEC = StreamCodec.composite(
         UUIDUtil.STREAM_CODEC,
         HermandadesJugadoresPayload.Entrada::jugador,
         ByteBufCodecs.STRING_UTF8,
         HermandadesJugadoresPayload.Entrada::hermandad,
         ByteBufCodecs.INT,
         HermandadesJugadoresPayload.Entrada::color,
         ByteBufCodecs.STRING_UTF8,
         HermandadesJugadoresPayload.Entrada::rango,
         HermandadesJugadoresPayload.Entrada::new
      );
   }
}
