package neth6.soulspoon.client.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.entity.ModEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Un solo renderer genérico para todos los mobs GeckoLib.
 * DefaultedEntityGeoModel busca, por nombre:
 *   assets/soulspoon/geo/entity/<name>.geo.json
 *   assets/soulspoon/textures/entity/<name>.png
 *   assets/soulspoon/animations/entity/<name>.animation.json
 * Para cambiar el modelo, basta reemplazar esos 3 archivos con los de Blockbench.
 */
public final class ModEntityRenderers {
    private ModEntityRenderers() {}

    public static class ModGeoRenderer<T extends Entity & GeoEntity> extends GeoEntityRenderer<T> {
        public ModGeoRenderer(EntityRendererProvider.Context ctx, String name, float shadow, float scale) {
            super(ctx, new DefaultedEntityGeoModel<>(SoulSpoon.id(name)));
            this.shadowRadius = shadow;
            withScale(scale);
        }
    }

    public static void register() {
        EntityRendererRegistry.register(ModEntities.NUCLEAR_CREEPER,
                ctx -> new ModGeoRenderer<>(ctx, "creeper_nuclear", 0.6f, 1.0f));
        EntityRendererRegistry.register(ModEntities.PLUMOSAURIO,
                ctx -> new ModGeoRenderer<>(ctx, "plumosaurio", 1.4f, 1.0f));
        EntityRendererRegistry.register(ModEntities.BOMBA_WARDEN,
                ctx -> new ModGeoRenderer<>(ctx, "bomba_warden", 0.3f, 1.0f));
        EntityRendererRegistry.register(ModEntities.DROMORAPTOR_ROJO,
                ctx -> new ModGeoRenderer<>(ctx, "dromoraptor_rojo", 0.6f, 1.0f));
    }
}
