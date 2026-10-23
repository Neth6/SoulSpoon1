package neth6.soulspoon.economy;

/**
 * Las 4 monedas del mundo. Todo se guarda como un solo numero (valor en "azules")
 * y se muestra repartido: 10.000 = 1 morada, 1.000 = 1 roja, 100 = 1 verde, 1 = 1 azul.
 */
public enum Dedita {
    AZUL("azul", "azul", "azules", 1L, 0xFF3A7BFF),
    VERDE("verde", "verde", "verdes", 100L, 0xFF3FBF6A),
    ROJA("roja", "roja", "rojas", 1_000L, 0xFFE5484D),
    MORADA("morada", "morada", "moradas", 10_000L, 0xFF9B5DE5);

    public final String id;
    public final String singular;
    public final String plural;
    public final long value;
    public final int color;

    Dedita(String id, String singular, String plural, long value, int color) {
        this.id = id;
        this.singular = singular;
        this.plural = plural;
        this.value = value;
        this.color = color;
    }

    public static Dedita byId(String id) {
        for (Dedita d : values()) {
            if (d.id.equalsIgnoreCase(id)) return d;
        }
        return null;
    }

    /** Reparte un total en monedas; el indice es el ordinal (0 azul ... 3 morada). */
    public static long[] split(long total) {
        long[] out = new long[values().length];
        long left = Math.max(0, total);
        for (int i = values().length - 1; i >= 0; i--) {
            out[i] = left / values()[i].value;
            left %= values()[i].value;
        }
        return out;
    }

    /** "1 morada, 3 verdes, 5 azules" */
    public static String describe(long total) {
        long[] parts = split(total);
        StringBuilder sb = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i] == 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(parts[i]).append(' ').append(parts[i] == 1 ? values()[i].singular : values()[i].plural);
        }
        return sb.length() == 0 ? "0 azules" : sb.toString();
    }
}
