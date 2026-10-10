package neth6.soulspoon.wheel;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum WheelColor {
    ROJO("rojo", "Rojo", "Eventos de Reviil",
            "Desastres y castigos ambientales mortales",
            Items.RED_STAINED_GLASS_PANE, ChatFormatting.RED),
    MORADO("morado", "Morado", "Eventos de Nutria",
            "Desafios e intervenciones especiales de Nutria",
            Items.PURPLE_STAINED_GLASS_PANE, ChatFormatting.DARK_PURPLE),
    VERDE("verde", "Verde", "Eventos y ayudas de Eon",
            "Proteccion: bunkeres, zonas seguras y accesos a dimensiones",
            Items.GREEN_STAINED_GLASS_PANE, ChatFormatting.GREEN),
    AMARILLO("amarillo", "Amarillo", "Nuevo mob",
            "Llega una nueva criatura hostil al servidor",
            Items.YELLOW_STAINED_GLASS_PANE, ChatFormatting.YELLOW),
    CIAN("cian", "Cian", "Mision diaria",
            "Tarea secundaria del dia",
            Items.CYAN_STAINED_GLASS_PANE, ChatFormatting.AQUA),
    AZUL("azul", "Azul", "Mision principal del dia",
            "El hito obligatorio del dia",
            Items.BLUE_STAINED_GLASS_PANE, ChatFormatting.BLUE),
    NARANJA("naranja", "Naranja", "Cambio de balance o restriccion",
            "Alteraciones de las mecanicas vanilla",
            Items.ORANGE_STAINED_GLASS_PANE, ChatFormatting.GOLD);

    private final String id;
    private final String colorName;
    private final String title;
    private final String description;
    private final Item icon;
    private final ChatFormatting format;

    WheelColor(String id, String colorName, String title, String description,
               Item icon, ChatFormatting format) {
        this.id = id;
        this.colorName = colorName;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.format = format;
    }

    public String id() {
        return id;
    }

    public String colorName() {
        return colorName;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Item icon() {
        return icon;
    }

    public ChatFormatting format() {
        return format;
    }

    public static WheelColor byId(String id) {
        for (WheelColor color : values()) {
            if (color.id.equals(id)) {
                return color;
            }
        }
        return null;
    }
}