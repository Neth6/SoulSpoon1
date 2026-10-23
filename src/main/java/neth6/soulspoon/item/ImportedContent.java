package neth6.soulspoon.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contenido importado de DEDSAFIO 4 (assets.zip): bloques simples, items sueltos y eventos de sonido.
 * Se inicializa AL FINAL de onInitialize y salta todo id que ya exista, asi no choca con el resto del mod.
 * Las texturas llegan aparte (sin ellas se ven como cuadros morado/negro).
 * Los items son "solo el objeto": la funcion de cada uno se agrega despues.
 */
public final class ImportedContent {
    private ImportedContent() {}

    /** Bloques cuadrados sin propiedades (los que tienen edad/orientacion quedan para despues). */
    private static final String[] BLOCKS = {
            "agua_portal",
            "bloque_amarillo",
            "bloque_baneados",
            "bloque_carne",
            "bloque_carne_rosa",
            "bloque_celeste",
            "bloque_dilitio",
            "bloque_luces",
            "bloque_naranja",
            "bloque_pixeles",
            "bloque_rojo",
            "bloque_verde",
            "boveda",
            "caca",
            "carne_con_venas",
            "carne_roja",
            "carne_rosa",
            "cofre_huesos",
            "cofre_protegido",
            "cuerda",
            "entrega_mision",
            "gelatina_roja",
            "gelatina_rosa",
            "grasa_coagulada",
            "grasa_liquida",
            "mineral_de_ambar",
            "mineral_de_grasa",
            "mineral_verde",
            "musculo",
            "pasto_rosa",
            "puerta_rosa",
            "puerta_verde",
            "puesto_intercambio",
            "pulmones",
            "roca_techo",
            "soporte_nave",
            "vitrina"
    };

    private static final String[] ITEMS = {
            "alma",
            "ambar",
            "ambar_con_insecto",
            "ambar_en_bruto",
            "amuleto_verde",
            "antidoto_primitivo",
            "arco_glebanoide",
            "barra_cristal_verde",
            "bateria_dilitio",
            "bolsa_de_tela",
            "bolsa_ender",
            "bolsa_primitiva_black",
            "bolsa_primitiva_blue",
            "bolsa_primitiva_brown",
            "bolsa_primitiva_cyan",
            "bolsa_primitiva_gray",
            "bolsa_primitiva_green",
            "bolsa_primitiva_light_blue",
            "bolsa_primitiva_light_gray",
            "bolsa_primitiva_lime",
            "bolsa_primitiva_magenta",
            "bolsa_primitiva_orange",
            "bolsa_primitiva_pink",
            "bolsa_primitiva_purple",
            "bolsa_primitiva_red",
            "bolsa_primitiva_white",
            "bolsa_primitiva_yellow",
            "candado",
            "caparazon",
            "carne_glebanoide",
            "cerebro_glebanoide",
            "chip_phora",
            "combustible",
            "corazon",
            "corazon_dorado",
            "corazon_glebanoide",
            "corazon_violeta",
            "cristal_verde",
            "cuchara_dorada_hojas",
            "cuchara_hojas",
            "cuchara_madera",
            "cuerda_resistente",
            "cuero_glebanoide",
            "dedita",
            "dedita_casino",
            "dedita_mercado_negro",
            "dedita_mision",
            "dedita_roja",
            "dedita_verde",
            "diente_glebanoide",
            "dilitio",
            "escupitajo_dactylo",
            "espada_glebanoide",
            "espada_hoja_qumara",
            "espada_limbo",
            "excremento",
            "fibra_glebanoide",
            "fruta_espacio",
            "fruto_quiu",
            "gema_blanca",
            "gema_dorada",
            "gema_gris",
            "gema_roja",
            "insecto",
            "jeringa",
            "linterna",
            "llave",
            "llave_candado",
            "manuscrito_hermandad",
            "manuscrito_hermandad_admin",
            "manuscrito_hermandad_sellado",
            "manzana_de_ambar",
            "marco_grande",
            "martillo_netherite",
            "mascara_anti_esporas",
            "nave_aleron_derecho",
            "nave_aleron_izquierdo",
            "nave_biplaza",
            "nave_cabina",
            "nave_motor",
            "pegamento_primitivo",
            "pelo_eburia",
            "pelo_glebanoide",
            "pildora",
            "pildora_vacia",
            "pimpollo_qumara",
            "racimo_dilitio",
            "saco",
            "sangre_reptisaurio",
            "semilla_arandano",
            "semilla_eburia",
            "semilla_solaria",
            "semilla_uvina",
            "signo_interrogacion",
            "sin_alma",
            "tanque_dilitio",
            "tarjeta_puerta_rosa",
            "tarjeta_puerta_verde",
            "tarjeta_rosa",
            "tela_primitiva",
            "tenedor_hojas",
            "tenedor_simple",
            "totem_espacio",
            "totem_frerico",
            "totem_glebanoide",
            "totem_idolo",
            "totem_limbo",
            "totem_yunque",
            "trozo_amarillo"
    };

    /** Items que no se apilan (armas, totems, bolsas...). */
    private static final Set<String> SINGLE = Set.of(
            "arco_glebanoide",
            "bolsa_de_tela",
            "bolsa_ender",
            "bolsa_primitiva_black",
            "bolsa_primitiva_blue",
            "bolsa_primitiva_brown",
            "bolsa_primitiva_cyan",
            "bolsa_primitiva_gray",
            "bolsa_primitiva_green",
            "bolsa_primitiva_light_blue",
            "bolsa_primitiva_light_gray",
            "bolsa_primitiva_lime",
            "bolsa_primitiva_magenta",
            "bolsa_primitiva_orange",
            "bolsa_primitiva_pink",
            "bolsa_primitiva_purple",
            "bolsa_primitiva_red",
            "bolsa_primitiva_white",
            "bolsa_primitiva_yellow",
            "cuchara_dorada_hojas",
            "cuchara_hojas",
            "cuchara_madera",
            "espada_glebanoide",
            "espada_hoja_qumara",
            "espada_limbo",
            "jeringa",
            "linterna",
            "llave",
            "llave_candado",
            "manuscrito_hermandad",
            "manuscrito_hermandad_admin",
            "manuscrito_hermandad_sellado",
            "martillo_netherite",
            "mascara_anti_esporas",
            "nave_aleron_derecho",
            "nave_aleron_izquierdo",
            "nave_biplaza",
            "nave_cabina",
            "nave_motor",
            "tenedor_hojas",
            "tenedor_simple",
            "totem_espacio",
            "totem_frerico",
            "totem_glebanoide",
            "totem_idolo",
            "totem_limbo",
            "totem_yunque");

    private static final Set<String> TRANSLUCENT = Set.of("vitrina", "cuerda", "agua_portal", "gelatina_roja", "gelatina_rosa", "pulmones");
    private static final Set<String> LUMINOUS = Set.of("bloque_luces", "bloque_pixeles");

    public static void initialize() {
        List<Item> blockItems = new ArrayList<>();
        List<Item> items = new ArrayList<>();

        for (String name : BLOCKS) {
            ResourceLocation id = SoulSpoon.id(name);
            if (BuiltInRegistries.BLOCK.containsKey(id)) continue;
            BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(2.0f);
            if (TRANSLUCENT.contains(name)) props = props.noOcclusion();
            if (LUMINOUS.contains(name)) props = props.lightLevel(state -> 15);
            Block block = Registry.register(BuiltInRegistries.BLOCK, id, new Block(props));
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                blockItems.add(Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties())));
            }
        }

        for (String name : ITEMS) {
            ResourceLocation id = SoulSpoon.id(name);
            if (BuiltInRegistries.ITEM.containsKey(id)) continue;
            Item.Properties props = new Item.Properties();
            if (SINGLE.contains(name)) props = props.stacksTo(1);
            items.add(Registry.register(BuiltInRegistries.ITEM, id, new Item(props)));
        }

        registerSounds();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS)
                .register(entries -> blockItems.forEach(entries::accept));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> items.forEach(entries::accept));
        SoulSpoon.LOGGER.info("Contenido importado: {} bloques, {} items", blockItems.size(), items.size());
    }

    /** Registra un evento de sonido por cada entrada de assets/soulspoon/sounds.json que aun no exista. */
    private static void registerSounds() {
        try (InputStream in = ImportedContent.class.getResourceAsStream("/assets/soulspoon/sounds.json")) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : root.keySet()) {
                ResourceLocation id = SoulSpoon.id(key);
                if (!BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                    Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
                }
            }
        } catch (Exception e) {
            SoulSpoon.LOGGER.warn("No se pudieron registrar los sonidos importados: {}", e.toString());
        }
    }
}
