package neth6.soulspoon.rule;

public enum Rule {
    // ---- Parte 1: balance ----
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
    MECANISMOS("mecanismos", "Botones, puertas y placas de presion matan"),
    // ---- Parte 2: entorno y mundo ----
    WARDEN("warden", "[2] Wardens aparecen en el Deep Dark"),
    SHRIEKER("shrieker", "[8] Al morir un Warden queda un shrieker"),
    GAS("gas", "[85] Gas toxico permanente sobre la capa 150"),
    PANALES("panales", "[21] Romper un panal/colmena invoca TNT"),
    MAGMA("magma", "[22] Pisar bloques de magma mata"),
    ARANAS("aranas", "[36] Romper telaranas invoca aranas de cueva"),
    EXTINCION("extincion", "[45] Los animales pasivos desaparecen"),
    INSOMNIA("insomnia", "[49] Indicador de insomnia (se resetea durmiendo)"),
    NETHER_LETAL("netherletal", "[78] Sin armadura completa en el Nether: fuego infinito"),
    NETHER_CERRADO("nethercerrado", "[39] El Nether esta cerrado (mata)"),
    ZOMBIFICACION("zombificacion", "[90] Contador de infeccion: a 0 pierdes un totem o mueres"),
    // ---- Parte 3: mobs y mecanicas ----
    CREEPERS_ELECTRICOS("creeperselectricos", "[10] Todos los creepers son electricos"),
    TREPADORES("trepadores", "[23] Los zombies trepan paredes"),
    ZOMBIES_SUPREMOS("zombiessupremos", "[33] Zombies con espada de hierro Filo V"),
    POD_SKIFF_X4("podskiffx4", "[43] Pod y Skiff Phora hacen x4 de dano"),
    OLFATO("olfato", "[72] Los monstruos detectan jugadores invisibles"),
    ARANAS_VELOCES("aranasveloces", "[58] Todas las aranas con velocidad infinita"),
    ARANAS_FUERZA("aranasfuerza", "[65] Todas las aranas con Fuerza III"),
    ARANAS_TELA("aranastela", "[77] Las aranas ponen telaranas al golpear y al morir");

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
