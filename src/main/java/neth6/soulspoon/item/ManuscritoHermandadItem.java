package neth6.soulspoon.item;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

public class ManuscritoHermandadItem extends Item {
   private static final Style ICONOS = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("soulspoon", "iconos"));
   private static final String ICONO_DEDITA = String.valueOf('\ue002');
   private static final Style GRIS = Style.EMPTY.withColor(ChatFormatting.GRAY);
   private static final Style VERDE = Style.EMPTY.withColor(ChatFormatting.GREEN);
   private static final Style NARANJA = Style.EMPTY.withColor(ChatFormatting.GOLD);
   private static final Style CELESTE = Style.EMPTY.withColor(ChatFormatting.AQUA);
   private static final Style AMARILLO = Style.EMPTY.withColor(ChatFormatting.YELLOW);
   private static final Style VERDE_OLIVA = Style.EMPTY.withColor(TextColor.fromRgb(11060844));
   public static Consumer<InteractionHand> abrirPantalla = mano -> {};
   private final int minimoInscritos;
   private final boolean admin;

   public ManuscritoHermandadItem(Properties propiedades, int minimoInscritos, boolean admin) {
      super(propiedades);
      this.minimoInscritos = minimoInscritos;
      this.admin = admin;
   }

   public static boolean es(ItemStack stack) {
      return stack.getItem() instanceof ManuscritoHermandadItem;
   }

   public static int minimoInscritos(ItemStack stack) {
      return stack.getItem() instanceof ManuscritoHermandadItem m ? m.minimoInscritos : 3;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player jugador, InteractionHand mano) {
      if (level.isClientSide()) {
         abrirPantalla.accept(mano);
      }

      return InteractionResultHolder.sidedSuccess(jugador.getItemInHand(mano), level.isClientSide());
   }

   public Component getName(ItemStack stack) {
      return super.getName(stack).copy().withStyle(ChatFormatting.GREEN);
   }

   public void appendHoverText(ItemStack stack, TooltipContext contexto, List<Component> lineas, TooltipFlag flag) {
      if (this.admin) {
         lineas.add(texto("Versión de admin: solo necesita " + this.minimoInscritos + " inscrito.", Style.EMPTY.withColor(ChatFormatting.RED)));
      }

      lineas.add(
         texto("Valor de compra: ", NARANJA)
            .append(texto("1 ", Style.EMPTY.withColor(ChatFormatting.WHITE)))
            .append(texto(ICONO_DEDITA, ICONOS))
            .append(texto(" por 1", NARANJA))
      );
      lineas.add(Component.empty());
      lineas.add(texto("Utilízalo para crear tu propia", GRIS));
      lineas.add(texto("Hermandad. Las Hermandades", GRIS));
      lineas.add(texto("cuentan con:", GRIS));
      lineas.add(Component.empty());

      for (String ventaja : new String[]{
         "Banco Compartido", "Capas Personalizables", "Waypoints Propios", "Tablón de Anuncios", "Entre otras características."
      }) {
         lineas.add(texto("◆ " + ventaja, VERDE));
      }

      lineas.add(Component.empty());
      String miembros = this.minimoInscritos + (this.minimoInscritos == 1 ? " Miembro" : " Miembros");
      lineas.add(texto("Requiere que al menos ", GRIS).append(texto(miembros, NARANJA)));
      lineas.add(texto("firmen el Manuscrito para fundar", GRIS));
      lineas.add(texto("una Hermandad. Pulsa ", GRIS).append(texto("H", NARANJA)).append(texto(" para acceder", GRIS)));
      lineas.add(texto("a la Interfaz de Hermandades.", GRIS));
      lineas.add(Component.empty());
      lineas.add(texto("Una vez hayan firmado, el ", GRIS).append(texto("Maestro", CELESTE)));
      lineas.add(texto("de la Hermandad", CELESTE).append(texto(" debe utilizar", GRIS)));
      lineas.add(texto("el comando ", GRIS).append(texto("/guild found", NARANJA)).append(texto(" para", GRIS)));
      lineas.add(texto("crearla.", GRIS));
      lineas.add(Component.empty());
      lineas.add(texto("⚠ Atención: Solamente es necesario UN", AMARILLO));
      lineas.add(texto("Manuscrito para crear una Hermandad.", AMARILLO));
      lineas.add(Component.empty());
      lineas.add(texto("Puedes adquirirlo en las ", GRIS).append(texto("Nutrias", VERDE_OLIVA)));
      lineas.add(texto("de Piedra de la Montaña.", VERDE_OLIVA));
   }

   private static MutableComponent texto(String texto, Style estilo) {
      return Component.literal(texto).withStyle(estilo);
   }
}
