package neth6.soulspoon.casino;

import java.util.ArrayList;
import java.util.List;
import neth6.soulspoon.SoulSpoon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Tabla de premios del casino: cada entrada es un item con un "peso".
 * Probabilidad = peso / suma de todos los pesos. La entrada con item vacio es "perder" (no da nada).
 */
public final class CasinoPool {
    private CasinoPool() {}

    public static final int MAX_ENTRADAS = 40;
    public static final int MAX_PESO = 100_000;

    public record Entry(ItemStack stack, int weight) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::stack,
                ByteBufCodecs.VAR_INT, Entry::weight,
                Entry::new);

        public boolean nada() { return stack.isEmpty(); }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, List<Entry>> LIST_CODEC =
            Entry.CODEC.apply(ByteBufCodecs.list(MAX_ENTRADAS));

    public static Item corazon() {
        return BuiltInRegistries.ITEM.getOptional(SoulSpoon.id("corazon")).orElse(Items.GOLDEN_APPLE);
    }

    /** Tabla por defecto (pesos sobre 1000). */
    public static List<Entry> porDefecto() {
        List<Entry> l = new ArrayList<>();
        l.add(new Entry(ItemStack.EMPTY, 400));                                   // perder
        l.add(new Entry(new ItemStack(Items.COOKED_BEEF, 16), 100));
        l.add(new Entry(new ItemStack(Items.IRON_INGOT, 5), 90));
        l.add(new Entry(new ItemStack(Items.EXPERIENCE_BOTTLE, 16), 80));
        l.add(new Entry(PotionContents.createItemStack(Items.POTION, Potions.HEALING), 60));
        l.add(new Entry(new ItemStack(Items.DIAMOND), 40));
        l.add(new Entry(new ItemStack(Items.GOLDEN_APPLE), 30));
        l.add(new Entry(new ItemStack(Items.EXPERIENCE_BOTTLE, 64), 20));
        l.add(new Entry(new ItemStack(Items.DIAMOND, 5), 10));
        l.add(new Entry(new ItemStack(Items.NETHERITE_SCRAP), 8));
        l.add(new Entry(new ItemStack(corazon()), 5));                            // 0,5 %: muy raro
        return l;
    }

    public static int total(List<Entry> pool) {
        int t = 0;
        for (Entry e : pool) t += Math.max(0, e.weight());
        return t;
    }

    public static Entry elegir(List<Entry> pool, RandomSource azar) {
        int t = total(pool);
        if (t <= 0) return new Entry(ItemStack.EMPTY, 1);
        int r = azar.nextInt(t);
        for (Entry e : pool) {
            r -= Math.max(0, e.weight());
            if (r < 0) return e;
        }
        return pool.get(pool.size() - 1);
    }

    /** Valida lo que manda un cliente: tamano, pesos y que no haya items vacios fuera de "perder". */
    public static List<Entry> limpiar(List<Entry> in) {
        List<Entry> out = new ArrayList<>();
        boolean tienePerder = false;
        for (Entry e : in) {
            if (out.size() >= MAX_ENTRADAS) break;
            int w = Math.max(0, Math.min(MAX_PESO, e.weight()));
            if (e.stack().isEmpty()) {
                if (tienePerder) continue;
                tienePerder = true;
                out.add(new Entry(ItemStack.EMPTY, w));
            } else {
                out.add(new Entry(e.stack().copy(), w));
            }
        }
        if (!tienePerder) out.add(0, new Entry(ItemStack.EMPTY, 0));
        return out;
    }
}
