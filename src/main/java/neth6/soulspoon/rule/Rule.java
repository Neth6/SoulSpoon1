package neth6.soulspoon.rule;

public enum Rule {
    BROTES("brotes", "Los items de brote de arbol desaparecen"),
    LECHE("leche", "Los baldes de leche desaparecen"),
    PUERTAS("puertas", "Puertas/trampillas en el agua se vuelven agua"),
    PEARL("pearl", "Ender pearl hace 10 de dano"),
    FUEGO_X3("fuego3", "Fuego y lava hacen x3"),
    FUEGO_X10("fuego10", "Fuego y lava hacen x10"),
    CAIDA_X2("caida2", "Dano por caida x2"),
    AHOGO("ahogo", "Ahogamiento x10 y aire se gasta x5 mas rapido"),
    COMIDA("comida", "Carne, pollo y pescado se pudren"),
    ABSORCION("absorcion", "Las manzanas doradas no dan absorcion"),
    MECANISMOS("mecanismos", "Botones, puertas y placas de presion matan");

    private final String id;
    private final String description;

    Rule(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public static Rule byId(String id) {
        for (Rule rule : values()) {
            if (rule.id.equalsIgnoreCase(id)) {
                return rule;
            }
        }
        return null;
    }
}