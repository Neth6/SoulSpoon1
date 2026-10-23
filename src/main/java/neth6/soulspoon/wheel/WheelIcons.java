package neth6.soulspoon.wheel;

import neth6.soulspoon.SoulSpoon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Iconos de la ruleta dentro del texto (chat, titulos). Son letras de la fuente
 * assets/soulspoon/font/ruleta_icons.json, asi que no hace falta codigo en el cliente.
 */
public final class WheelIcons {
    private WheelIcons() {}

    private static final ResourceLocation FONT = SoulSpoon.id("ruleta_icons");

    /** Icono del color de la ruleta (sin color de texto para no teñir el PNG). */
    public static MutableComponent of(WheelColor color) {
        return glyph(switch (color) {
            case NARANJA -> 0xE000;
            case AMARILLO -> 0xE002;
            case ROJO -> 0xE003;
            case AZUL -> 0xE004;
            case CIAN -> 0xE005;
            case VERDE -> 0xE006;
            case MORADO -> 0xE007;
        });
    }

    /** Extras: rosada (E001), blanca (E008), apagada (E009). */
    public static MutableComponent rosada() { return glyph(0xE001); }
    public static MutableComponent blanca() { return glyph(0xE008); }
    public static MutableComponent apagada() { return glyph(0xE009); }

    /**
     * El icono va dentro de un componente vacio: asi lo que se anexe despues (el texto del anuncio)
     * NO hereda la fuente de iconos (si la heredara, las letras saldrian como cuadros vacios).
     */
    private static MutableComponent glyph(int code) {
        MutableComponent icon = Component.literal(String.valueOf((char) code)).setStyle(Style.EMPTY.withFont(FONT));
        return Component.empty().append(icon);
    }
}
