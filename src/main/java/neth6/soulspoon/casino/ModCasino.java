package neth6.soulspoon.casino;

import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Casino: una maquina (mob GeckoLib) que se gira con deditas.
 * Cada giro cuesta PRECIO_INICIAL azules (100 = 1 verde) salvo que el admin lo cambie con /casino precio.
 * Tambien acepta la ficha "dedita_casino" del original. Inicializar ANTES de ImportedContent.
 */
public final class ModCasino {
    private ModCasino() {}

    /** 100 azules = 1 dedita verde. */
    public static final long PRECIO_INICIAL = 100L;

    public static final EntityType<CasinoEntity> CASINO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, SoulSpoon.id("casino"),
            EntityType.Builder.<CasinoEntity>of(CasinoEntity::new, MobCategory.MISC)
                    .sized(1.4f, 2.6f).clientTrackingRange(10).build("casino"));

    public static final Item CASINO_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM, SoulSpoon.id("casino_spawn_egg"),
            new SpawnEggItem(CASINO, 0x7A0020, 0xF2B130, new Item.Properties()));

    // Sonidos: se arman al vuelo con el id (ya existen en sounds.json); no se registran de nuevo.
    private static final SoundEvent GIRA = SoundEvent.createVariableRangeEvent(SoulSpoon.id("casino"));
    private static final SoundEvent GANA = SoundEvent.createVariableRangeEvent(SoulSpoon.id("casino_win"));

    public static SoundEvent sonidoGira() { return GIRA; }
    public static SoundEvent sonidoGana() { return GANA; }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(CASINO, CasinoEntity.crearAtributos());
        CasinoNet.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> CasinoPremios.registrarComandos(dispatcher));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(e -> e.accept(CASINO_SPAWN_EGG));
    }
}
