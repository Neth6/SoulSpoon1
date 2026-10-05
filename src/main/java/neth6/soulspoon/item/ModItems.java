package neth6.soulspoon.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import neth6.soulspoon.SoulSpoon;

public class ModItems {
    public static final Item SOUL_SPOON = register(
            "soul_spoon",
            new Item(new Item.Properties().stacksTo(1))
    );

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id(name), item);
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(SOUL_SPOON));
    }
}