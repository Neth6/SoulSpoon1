package neth6.soulspoon.entity;

import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

// Parte 4: entidades con modelo propio y los items que sueltan o necesitan.
public final class ModEntities {
    private ModEntities() {}

    // Items (los de Parte 6 -altavoz y linterna- quedan como items simples por ahora).
    public static final Item ESENCIA_ROJA = item("esencia_roja", "Esencia Roja", new Item.Properties());
    public static final Item MANZANA_AMBAR = item("manzana_ambar", "Manzana de Ámbar",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build()));
    public static final Item ALTAVOZ = item("altavoz", "Altavoz", new Item.Properties().stacksTo(1));
    public static final Item LINTERNA = item("linterna", "Linterna", new Item.Properties().stacksTo(1));

    // Terrestres: vida, daño, velocidad.
    public static final EntityType<SoulMob> CEREBRO_ROJO = ground("cerebro_rojo", 0.7f, 0.8f, 14, 2, 0.30);
    public static final EntityType<SoulMob> CEREBRO_AMARILLO = ground("cerebro_amarillo", 0.7f, 0.8f, 14, 2, 0.30);
    public static final EntityType<SoulMob> MEDULA_OSEA = ground("medula_osea", 0.6f, 1.9f, 40, 7, 0.32);
    public static final EntityType<SoulMob> REAPER_PEQUENO = ground("reaper_pequeno", 0.5f, 1.5f, 30, 6, 0.30);
    public static final EntityType<SoulMob> KRAGTOPS = ground("kragtops", 1.6f, 1.9f, 80, 15, 0.22);
    public static final EntityType<SoulMob> CREEPER_VISCERAL = ground("creeper_visceral", 0.7f, 1.7f, 30, 0, 0.26);

    // Voladores: vida, daño, velocidad de vuelo.
    public static final EntityType<SoulFlyer> DACTYLO_ADULTO = flyer("dactylo_adulto", 2.2f, 1.2f, 60, 8, 0.9);
    public static final EntityType<SoulFlyer> FANTASMA_ROJO = flyer("fantasma_rojo", 0.7f, 1.6f, 20, 5, 1.1);
    public static final EntityType<SoulFlyer> FANTASMA_AMARILLO = flyer("fantasma_amarillo", 0.7f, 1.6f, 24, 4, 0.8);
    public static final EntityType<SoulFlyer> OJO_VOLADOR = flyer("ojo_volador", 1.0f, 1.0f, 40, 4, 0.7);

    // Entidades GeckoLib (Parte 3, con modelo de Blockbench). Las registra aqui ModEntityRenderers.
    public static final EntityType<NuclearCreeperEntity> NUCLEAR_CREEPER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id("creeper_nuclear"),
            EntityType.Builder.<NuclearCreeperEntity>of(NuclearCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.7f).clientTrackingRange(8).build("creeper_nuclear"));

    public static final EntityType<PlumosaurioEntity> PLUMOSAURIO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id("plumosaurio"),
            EntityType.Builder.<PlumosaurioEntity>of(PlumosaurioEntity::new, MobCategory.CREATURE)
                    .sized(1.6f, 2.4f).clientTrackingRange(10).build("plumosaurio"));

    public static final EntityType<BombaWardenEntity> BOMBA_WARDEN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id("bomba_warden"),
            EntityType.Builder.<BombaWardenEntity>of(BombaWardenEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 0.7f).clientTrackingRange(10).build("bomba_warden"));

    public static final EntityType<DromoraptorRojoEntity> DROMORAPTOR_ROJO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id("dromoraptor_rojo"),
            EntityType.Builder.<DromoraptorRojoEntity>of(DromoraptorRojoEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 1.4f).clientTrackingRange(10).build("dromoraptor_rojo"));

    public static void initialize() {
        // Los campos estaticos ya registraron todo al cargar la clase.
        // Atributos de las entidades GeckoLib (vida, dano y velocidad segun el PDF).
        FabricDefaultAttributeRegistry.register(NUCLEAR_CREEPER, Creeper.createAttributes());
        FabricDefaultAttributeRegistry.register(PLUMOSAURIO, Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 24.0));
        FabricDefaultAttributeRegistry.register(BOMBA_WARDEN, Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0));
        FabricDefaultAttributeRegistry.register(DROMORAPTOR_ROJO, Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.FOLLOW_RANGE, 32.0));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(ESENCIA_ROJA);
            entries.accept(MANZANA_AMBAR);
            entries.accept(ALTAVOZ);
            entries.accept(LINTERNA);
        });
    }

    private static Item item(String id, String name, Item.Properties properties) {
        return Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id(id),
                new Item(properties.component(DataComponents.ITEM_NAME, Component.literal(name))));
    }

    private static EntityType<SoulMob> ground(String id, float width, float height,
                                              double health, double damage, double speed) {
        EntityType<SoulMob> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id(id),
                EntityType.Builder.<SoulMob>of(SoulMob::new, MobCategory.MONSTER)
                        .sized(width, height).clientTrackingRange(10).build(id));
        FabricDefaultAttributeRegistry.register(type, SoulMob.attributes(health, damage, speed));
        return type;
    }

    private static EntityType<SoulFlyer> flyer(String id, float width, float height,
                                               double health, double damage, double speed) {
        EntityType<SoulFlyer> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id(id),
                EntityType.Builder.<SoulFlyer>of(SoulFlyer::new, MobCategory.MONSTER)
                        .sized(width, height).clientTrackingRange(10).build(id));
        FabricDefaultAttributeRegistry.register(type, SoulFlyer.attributes(health, damage, speed));
        return type;
    }
}