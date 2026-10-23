package neth6.soulspoon.client.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import neth6.soulspoon.entity.GeoMobs;
import neth6.soulspoon.entity.ModEntities;

// Renderers GeckoLib de los mobs con modelo de Blockbench. Se llama desde ModEntityRenderers.register().
// Parametros: id (nombre de los archivos), radio de sombra, escala.
public final class GeoMobsClient {
    private GeoMobsClient() {}

    public static void register() {
        EntityRendererRegistry.register(GeoMobs.CREEPER_PASTO,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "creeper_pasto", 0.4f, 1.0f));
        EntityRendererRegistry.register(GeoMobs.CREEPER_PASTEL,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "creeper_pastel", 0.6f, 1.0f));
        EntityRendererRegistry.register(GeoMobs.HEROBRINE,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "herobrine", 0.5f, 1.0f));
        EntityRendererRegistry.register(GeoMobs.FANTASMA_NEGRO,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "fantasma_negro", 0.3f, 1.0f));
        EntityRendererRegistry.register(GeoMobs.FANTASMA_BLANCO,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "fantasma_blanco", 0.3f, 1.0f));
        EntityRendererRegistry.register(GeoMobs.REPTISAURIO_SALVAJE,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "reptisaurio_salvaje", 0.9f, 1.0f));
        EntityRendererRegistry.register(GeoMobs.NARVAL,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "narval", 0.6f, 1.0f));
        EntityRendererRegistry.register(GeoMobs.ZARINOSA_ESPIGUEYA,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "zarinosa_espigueya", 0.6f, 1.0f));
        // El Fantasma Amarillo ya existia como SoulFlyer: ahora con el modelo nuevo.
        EntityRendererRegistry.register(ModEntities.FANTASMA_AMARILLO,
                ctx -> new ModEntityRenderers.ModGeoRenderer<>(ctx, "fantasma_amarillo", 0.3f, 1.0f));
    }
}
