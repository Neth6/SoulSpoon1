package neth6.soulspoon.economy;

import java.util.ArrayList;
import java.util.List;
import neth6.soulspoon.SoulSpoon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Las monedas como ITEMS (se buscan por id al usarse, asi no importa el orden de registro). */
public final class Coins {
    private Coins() {}

    private static Item get(String id) {
        return BuiltInRegistries.ITEM.getOptional(SoulSpoon.id(id)).orElse(Items.AIR);
    }

    public static Item azul()   { return get("dedita"); }          // 1
    public static Item verde()  { return get("dedita_verde"); }    // 100
    public static Item roja()   { return get("dedita_roja"); }     // 1.000
    public static Item mision() { return get("dedita_mision"); }

    /** Dedita morada (10.000): puede no existir como item todavia. */
    public static Item morada() {
        return BuiltInRegistries.ITEM.getOptional(SoulSpoon.id("dedita_morada")).orElse(null);
    }

    /** Convierte un total (en azules) en pilas de monedas, de la mas grande a la mas chica. */
    public static List<ItemStack> itemsPara(long total) {
        List<ItemStack> out = new ArrayList<>();
        long left = Math.max(0, total);
        Item[] items = {morada(), roja(), verde(), azul()};
        long[] valores = {10_000L, 1_000L, 100L, 1L};
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null || items[i] == Items.AIR) continue;
            long n = left / valores[i];
            left %= valores[i];
            while (n > 0) {
                int tanda = (int) Math.min(items[i].getDefaultMaxStackSize(), n);
                out.add(new ItemStack(items[i], tanda));
                n -= tanda;
            }
        }
        return out;
    }
}
