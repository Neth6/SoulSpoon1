package neth6.soulspoon.mob;

import neth6.soulspoon.SoulSpoon;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Un huevo por cada variante, en la pestana creativa "SoulSpoon: Mobs".
public class VariantItems {
    private static final List<Item> EGGS = new ArrayList<>();

    public static void initialize() {
        for (Variant variant : Variants.getAll()) {
            String shown = variant.getName() != null ? variant.getName() : variant.id();
            Item.Properties properties = new Item.Properties()
                    .component(DataComponents.ITEM_NAME, Component.literal("Huevo de " + shown));
            Item egg = Registry.register(BuiltInRegistries.ITEM,
                    SoulSpoon.id("egg_" + variant.id()),
                    new VariantEggItem(variant.id(), properties));
            EGGS.add(egg);
        }

        CreativeModeTab tab = FabricItemGroup.builder()
                .title(Component.literal("SoulSpoon: Mobs"))
                .icon(() -> new ItemStack(EGGS.get(0)))
                .displayItems((parameters, output) -> {
                    for (Item egg : EGGS) {
                        output.accept(egg);
                    }
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SoulSpoon.id("mobs"), tab);
    }
}
