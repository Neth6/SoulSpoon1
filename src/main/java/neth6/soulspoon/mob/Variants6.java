package neth6.soulspoon.mob;

import neth6.soulspoon.entity.GeoMobs;

// Mobs extra con modelo de Blockbench que no estaban en las otras partes.
public final class Variants6 {
    private Variants6() {}

    private static boolean done = false;

    public static void defineAll() {
        if (done) {
            return;
        }
        done = true;

        // Zarinosa Espigueya (estadisticas provisionales en GeoMobs; sin aparicion natural todavia).
        Variants.register(new Variant("zarinosa_espigueya", "Zarinosa Espigüeya", GeoMobs.ZARINOSA_ESPIGUEYA));
    }
}
