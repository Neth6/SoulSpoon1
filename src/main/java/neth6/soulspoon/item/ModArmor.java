package neth6.soulspoon.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.entity.ModEntities;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Dos tiers de armadura por encima de la netherita (mesa de herreria: plantilla + material).
 *
 * Tier 1 "Primitiva":  netherita -> casco 4, peto 9, pantalones 7, botas 4 | dureza 4 | retroceso 0.15 | x45
 * Tier 2 "Glebanoide": primitiva -> casco 5, peto 10, pantalones 8, botas 5 | dureza 5 | retroceso 0.20 | x55
 * (netherita: 3/8/6/3, dureza 3, retroceso 0.1, x37). Todo provisional: cambia los numeros aqui.
 *
 * Ademas: "Cedronica" (netherita + plantilla cedronica + cristal cedronico) y
 *         "Limbo" (netherita + plantilla del limbo + pluma del limbo).
 */
public final class ModArmor {
    private ModArmor() {}

    // ---- materiales (id = nombre de las texturas textures/models/armor/<id>_layer_1/2.png)
    public static final Holder<ArmorMaterial> PRIMITIVA = material("primitiva", 4, 9, 7, 4, 4.0f, 0.15f,
            () -> Ingredient.of(ModEntities.ESENCIA_ROJA));
    public static final Holder<ArmorMaterial> GLEBANOIDE = material("glebanoide", 5, 10, 8, 5, 5.0f, 0.20f,
            () -> Ingredient.of(Items.NETHER_STAR));

    public static final Holder<ArmorMaterial> CEDRONICA = material("cedronica", 4, 8, 6, 4, 3.5f, 0.10f,
            () -> Ingredient.of(ModArmor.CRISTAL_CEDRONICO));
    public static final Holder<ArmorMaterial> LIMBO = material("limbo", 4, 9, 7, 4, 4.0f, 0.10f,
            () -> Ingredient.of(ModArmor.PLUMA_LIMBO));

    // ---- materiales de mejora de los tiers nuevos
    public static final Item CRISTAL_CEDRONICO = simple("cristal_cedronico", "Cristal Cedronico");
    public static final Item PLUMA_LIMBO = simple("pluma_limbo", "Pluma del Limbo");

    // ---- plantillas de mejora
    public static final Item PLANTILLA_PRIMITIVA = simple("plantilla_primitiva",
            "Plantilla de mejora: Primitiva");
    public static final Item PLANTILLA_GLEBANOIDE = simple("plantilla_glebanoide",
            "Plantilla de mejora: Glebanoide");
    public static final Item PLANTILLA_CEDRONICA = simple("plantilla_cedronica",
            "Plantilla de mejora: Cedronica");
    public static final Item PLANTILLA_LIMBO = simple("plantilla_limbo",
            "Plantilla de mejora: Limbo");

    // ---- piezas tier 1
    public static final Item CASCO_PRIMITIVA = piece("casco_primitiva", "Casco Primitivo", PRIMITIVA, ArmorItem.Type.HELMET, 45);
    public static final Item PETO_PRIMITIVA = piece("peto_primitiva", "Peto Primitivo", PRIMITIVA, ArmorItem.Type.CHESTPLATE, 45);
    public static final Item PANTALONES_PRIMITIVA = piece("pantalones_primitiva", "Pantalones Primitivos", PRIMITIVA, ArmorItem.Type.LEGGINGS, 45);
    public static final Item BOTAS_PRIMITIVA = piece("botas_primitiva", "Botas Primitivas", PRIMITIVA, ArmorItem.Type.BOOTS, 45);

    // ---- piezas tier 2
    public static final Item CASCO_GLEBANOIDE = piece("casco_glebanoide", "Casco Glebanoide", GLEBANOIDE, ArmorItem.Type.HELMET, 55);
    public static final Item PETO_GLEBANOIDE = piece("peto_glebanoide", "Peto Glebanoide", GLEBANOIDE, ArmorItem.Type.CHESTPLATE, 55);
    public static final Item PANTALONES_GLEBANOIDE = piece("pantalones_glebanoide", "Pantalones Glebanoides", GLEBANOIDE, ArmorItem.Type.LEGGINGS, 55);
    public static final Item BOTAS_GLEBANOIDE = piece("botas_glebanoide", "Botas Glebanoides", GLEBANOIDE, ArmorItem.Type.BOOTS, 55);

    // ---- piezas Cedronica
    public static final Item CASCO_CEDRONICA = piece("casco_cedronica", "Casco Cedronico", CEDRONICA, ArmorItem.Type.HELMET, 40);
    public static final Item PETO_CEDRONICA = piece("peto_cedronica", "Peto Cedronico", CEDRONICA, ArmorItem.Type.CHESTPLATE, 40);
    public static final Item PANTALONES_CEDRONICA = piece("pantalones_cedronica", "Pantalones Cedronicos", CEDRONICA, ArmorItem.Type.LEGGINGS, 40);
    public static final Item BOTAS_CEDRONICA = piece("botas_cedronica", "Botas Cedronicas", CEDRONICA, ArmorItem.Type.BOOTS, 40);

    // ---- piezas Limbo
    public static final Item CASCO_LIMBO = piece("casco_limbo", "Casco del Limbo", LIMBO, ArmorItem.Type.HELMET, 42);
    public static final Item PETO_LIMBO = piece("peto_limbo", "Peto del Limbo", LIMBO, ArmorItem.Type.CHESTPLATE, 42);
    public static final Item PANTALONES_LIMBO = piece("pantalones_limbo", "Pantalones del Limbo", LIMBO, ArmorItem.Type.LEGGINGS, 42);
    public static final Item BOTAS_LIMBO = piece("botas_limbo", "Botas del Limbo", LIMBO, ArmorItem.Type.BOOTS, 42);

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(CASCO_PRIMITIVA);
            entries.accept(PETO_PRIMITIVA);
            entries.accept(PANTALONES_PRIMITIVA);
            entries.accept(BOTAS_PRIMITIVA);
            entries.accept(CASCO_GLEBANOIDE);
            entries.accept(PETO_GLEBANOIDE);
            entries.accept(PANTALONES_GLEBANOIDE);
            entries.accept(BOTAS_GLEBANOIDE);
            entries.accept(CASCO_CEDRONICA);
            entries.accept(PETO_CEDRONICA);
            entries.accept(PANTALONES_CEDRONICA);
            entries.accept(BOTAS_CEDRONICA);
            entries.accept(CASCO_LIMBO);
            entries.accept(PETO_LIMBO);
            entries.accept(PANTALONES_LIMBO);
            entries.accept(BOTAS_LIMBO);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(PLANTILLA_PRIMITIVA);
            entries.accept(PLANTILLA_GLEBANOIDE);
            entries.accept(PLANTILLA_CEDRONICA);
            entries.accept(PLANTILLA_LIMBO);
            entries.accept(CRISTAL_CEDRONICO);
            entries.accept(PLUMA_LIMBO);
        });
    }

    private static Holder<ArmorMaterial> material(String id, int helmet, int chest, int legs, int boots,
                                                  float toughness, float knockback, Supplier<Ingredient> repair) {
        Map<ArmorItem.Type, Integer> defense = Map.of(
                ArmorItem.Type.BOOTS, boots,
                ArmorItem.Type.LEGGINGS, legs,
                ArmorItem.Type.CHESTPLATE, chest,
                ArmorItem.Type.HELMET, helmet);
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(SoulSpoon.id(id)));
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, SoulSpoon.id(id),
                new ArmorMaterial(defense, 20, SoundEvents.ARMOR_EQUIP_NETHERITE, repair, layers,
                        toughness, knockback));
    }

    private static Item piece(String id, String name, Holder<ArmorMaterial> material,
                              ArmorItem.Type type, int durabilityFactor) {
        Item.Properties properties = new Item.Properties()
                .durability(type.getDurability(durabilityFactor))
                .fireResistant()
                .component(DataComponents.ITEM_NAME, Component.literal(name));
        return Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id(id), new ArmorItem(material, type, properties));
    }

    private static Item simple(String id, String name) {
        return Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id(id),
                new Item(new Item.Properties().component(DataComponents.ITEM_NAME, Component.literal(name))));
    }
}
