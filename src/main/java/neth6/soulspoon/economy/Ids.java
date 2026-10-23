package neth6.soulspoon.economy;

import neth6.soulspoon.SoulSpoon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Busca items del mod por id en el momento de usarlos (no importa el orden de registro). */
public final class Ids {
    private Ids() {}

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(SoulSpoon.id(id)).orElse(Items.AIR);
    }
}
