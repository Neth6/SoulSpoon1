package neth6.soulspoon.candados;

import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Items Candado y Llave de Candado. Llamar a initialize() antes de ImportedContent.initialize(). */
public final class ModCandados {
    public static final Item CANDADO = Registry.register(BuiltInRegistries.ITEM, id("candado"),
        new Descrito(new Item.Properties().stacksTo(16), new String[]{"Haz |Click Derecho| sobre un", "Cofre e introduce un código", "para protegerlo."}));
    public static final Item LLAVE_CANDADO = Registry.register(BuiltInRegistries.ITEM, id("llave_candado"),
        new Descrito(new Item.Properties().stacksTo(1), new String[]{"Haz |Click Derecho| sobre un", "Cofre Protegido de tu propiedad", "para desprotegerlo."}));

    private ModCandados() {}

    private static ResourceLocation id(String n) { return ResourceLocation.fromNamespaceAndPath("soulspoon", n); }

    public static void initialize() {
        Candados.registrar();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> {
            e.accept(CANDADO);
            e.accept(LLAVE_CANDADO);
        });
    }

    static final class Descrito extends Item {
        private static final int CELESTE = 5621480, TEXTO = 13027014, NARANJA = 15905626;
        private final String[] lineas;

        Descrito(Properties p, String[] lineas) { super(p); this.lineas = lineas; }

        @Override
        public Component getName(ItemStack pila) {
            return Component.translatable(this.getDescriptionId()).withStyle(s -> s.withColor(TextColor.fromRgb(CELESTE)));
        }

        @Override
        public void appendHoverText(ItemStack pila, TooltipContext ctx, List<Component> out, TooltipFlag flag) {
            out.add(Component.empty());
            for (String l : lineas) {
                MutableComponent linea = Component.empty();
                String[] partes = l.split("\\|", -1);
                for (int i = 0; i < partes.length; i++) {
                    linea.append(Component.literal(partes[i]).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(i % 2 == 1 ? NARANJA : TEXTO))));
                }
                out.add(linea);
            }
        }
    }
}
