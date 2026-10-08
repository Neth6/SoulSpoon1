package neth6.soulspoon.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import neth6.soulspoon.SoulSpoon;

public class ModBlocks {
    public static final Block SOUL_ALTAR = register(
            "soul_altar",
            new SoulAltarBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 7))
    );

    // Registra el bloque y tambien su item (para poder tenerlo en el inventario).
    private static Block register(String name, Block block) {
        Block registered = Registry.register(
                BuiltInRegistries.BLOCK, SoulSpoon.id(name), block);
        Registry.register(
                BuiltInRegistries.ITEM, SoulSpoon.id(name),
                new BlockItem(registered, new Item.Properties()));
        return registered;
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(SOUL_ALTAR));
    }
}