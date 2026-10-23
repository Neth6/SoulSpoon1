package neth6.soulspoon.item;

import neth6.soulspoon.SoulSpoon;
import neth6.soulspoon.world.Dimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;

/**
 * Objetos sueltos: Tridente del Limbo, Totem de corazones, Totem dorado, Arco de hueso y Espada violeta.
 * Texturas: assets/soulspoon/textures/item/<id>.png
 */
public final class ModGear {
    private ModGear() {}

    /** Tridente: funciona como un tridente pero SOLO en el Limbo. */
    public static class LimboTrident extends TridentItem {
        public LimboTrident(Properties properties) {
            super(properties);
        }

        private static boolean inLimbo(Level level) {
            return level.dimension() == Dimensions.LIMBO;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!inLimbo(level)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("Este tridente solo funciona en el Limbo"), true);
                }
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            return super.use(level, player, hand);
        }

        @Override
        public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
            if (!inLimbo(level)) {
                return;
            }
            super.releaseUsing(stack, level, entity, timeLeft);
        }
    }

    public static final Item TRIDENTE_LIMBO = register("tridente_limbo",
            new LimboTrident(new Item.Properties()
                    .rarity(Rarity.EPIC)
                    .durability(250)
                    .attributes(TridentItem.createAttributes())
                    .component(DataComponents.TOOL, TridentItem.createToolProperties())));

    /** Con este totem en la mano izquierda: +2 corazones. */
    public static final Item TOTEM_CORAZONES = register("totem_corazones",
            new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final Item TOTEM_DORADO = register("totem_dorado",
            new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final Item ARCO_HUESO = register("arco_hueso",
            new BowItem(new Item.Properties().durability(384)));

    public static final Item ESPADA_VIOLETA = register("espada_violeta",
            new SwordItem(Tiers.NETHERITE, new Item.Properties()
                    .rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.4f))));

    private static final ResourceLocation HEARTS_ID = SoulSpoon.id("totem_corazones");
    private static final double HEARTS_BONUS = 4.0; // 2 corazones

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, SoulSpoon.id(name), item);
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(TRIDENTE_LIMBO);
            entries.accept(TOTEM_CORAZONES);
            entries.accept(TOTEM_DORADO);
            entries.accept(ARCO_HUESO);
            entries.accept(ESPADA_VIOLETA);
        });

        // Totem de corazones: +2 corazones mientras este en la mano izquierda (offhand).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 5 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
                if (max == null) {
                    continue;
                }
                boolean holding = player.getOffhandItem().is(TOTEM_CORAZONES);
                boolean active = max.hasModifier(HEARTS_ID);
                if (holding && !active) {
                    max.addTransientModifier(new AttributeModifier(
                            HEARTS_ID, HEARTS_BONUS, AttributeModifier.Operation.ADD_VALUE));
                } else if (!holding && active) {
                    max.removeModifier(HEARTS_ID);
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
            }
        });
    }
}
