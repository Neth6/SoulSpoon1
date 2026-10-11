package neth6.soulspoon.client;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.entity.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

// Llamar a ModEntitiesClient.initialize() desde SoulSpoonClient.onInitializeClient().
public final class ModEntitiesClient {
    private ModEntitiesClient() {}

    public static void initialize() {
        layer("reaper", ModelDefs::reaper);
        layer("medula", ModelDefs::medula);
        layer("cerebro", ModelDefs::cerebro);
        layer("ghost", ModelDefs::ghost);
        layer("dactylo", ModelDefs::dactylo);
        layer("kragtops", ModelDefs::kragtops);
        layer("visceral", ModelDefs::visceral);
        layer("ojo", ModelDefs::ojo);

        //                    entidad                       id/textura          forma       escala sombra
        renderer(ModEntities.REAPER_PEQUENO,    "reaper_pequeno",    "reaper",   0.85f, 0.4f);
        renderer(ModEntities.MEDULA_OSEA,       "medula_osea",       "medula",   1.0f, 0.5f);
        renderer(ModEntities.CEREBRO_ROJO,      "cerebro_rojo",      "cerebro",  1.0f, 0.4f);
        renderer(ModEntities.CEREBRO_AMARILLO,  "cerebro_amarillo",  "cerebro",  1.0f, 0.4f);
        renderer(ModEntities.FANTASMA_ROJO,     "fantasma_rojo",     "ghost",    1.0f, 0.3f);
        renderer(ModEntities.FANTASMA_AMARILLO, "fantasma_amarillo", "ghost",    1.0f, 0.3f);
        renderer(ModEntities.DACTYLO_ADULTO,    "dactylo_adulto",    "dactylo",  2.0f, 0.8f);
        renderer(ModEntities.KRAGTOPS,          "kragtops",          "kragtops", 1.6f, 1.2f);
        renderer(ModEntities.CREEPER_VISCERAL,  "creeper_visceral",  "visceral", 1.0f, 0.5f);
        renderer(ModEntities.OJO_VOLADOR,       "ojo_volador",       "ojo",      1.5f, 0.4f);
    }

    private static ModelLayerLocation location(String shape) {
        return new ModelLayerLocation(SoulSpoon.id(shape), "main");
    }

    private static void layer(String shape, EntityModelLayerRegistry.TexturedModelDataProvider provider) {
        EntityModelLayerRegistry.registerModelLayer(location(shape), provider);
    }

    private static void renderer(EntityType<? extends Mob> type, String mobId, String shape,
                                 float scale, float shadow) {
        EntityRendererRegistry.register(type,
                context -> new BlockyRenderer(context, location(shape), mobId, scale, shadow));
    }
}
