package neth6.soulspoon.hermandad;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record HermandadEstandartesPayload(List<ItemStack> coleccion, int activo) implements CustomPacketPayload {
   public static final Type<HermandadEstandartesPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("soulspoon", "hermandad_estandartes"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HermandadEstandartesPayload> CODEC = StreamCodec.composite(
      ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
      HermandadEstandartesPayload::coleccion,
      ByteBufCodecs.VAR_INT,
      HermandadEstandartesPayload::activo,
      HermandadEstandartesPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
