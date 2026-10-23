package neth6.soulspoon.correo;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.mision.EntregaMisionBlock;
import neth6.soulspoon.mision.MisionPrincipal;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Buzon (mensajero), bloque de entrega de mision y el sistema de correo. Inicializar ANTES de ImportedContent. */
public final class ModCorreo {
    private ModCorreo() {}

    public static final Block MENSAJERO = Registry.register(BuiltInRegistries.BLOCK, SoulSpoon.id("mensajero"),
        new MensajeroBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL)
            .noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Item MENSAJERO_ITEM = Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id("mensajero"),
        new BlockItem(MENSAJERO, new Item.Properties()));

    public static final Block ENTREGA_MISION = Registry.register(BuiltInRegistries.BLOCK, SoulSpoon.id("entrega_mision"),
        new EntregaMisionBlock(Properties.of().strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()
            .isSuffocating((e, l, p) -> false).isViewBlocking((e, l, p) -> false).pushReaction(PushReaction.BLOCK)));
    public static final Item ENTREGA_MISION_ITEM = Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id("entrega_mision"),
        new BlockItem(ENTREGA_MISION, new Item.Properties()));

    public static void initialize() {
        Correo.registrar();
        EntregaMisionBlock.registrar();
        MisionPrincipal.initialize();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> {
            e.accept(MENSAJERO_ITEM);
            e.accept(ENTREGA_MISION_ITEM);
        });
    }
}
