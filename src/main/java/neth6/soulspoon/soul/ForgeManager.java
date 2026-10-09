package neth6.soulspoon.soul;

import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import neth6.soulspoon.item.ModItems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ForgeManager {
    // Muestra la receta actual en el chat.
    public static void showRecipe(ServerPlayer player) {
        SpoonRecipe recipe = SpoonRecipe.get(player.server);
        player.displayClientMessage(Component.literal(
                "Receta de la Cuchara de Almas: " + recipe.describe()), false);
        player.displayClientMessage(Component.literal(
                "Shift + clic derecho con las manos vacias para forjarla"), false);
    }

    // Intenta forjar una cuchara cobrando los ingredientes del inventario.
    public static void tryForge(ServerPlayer player) {
        SpoonRecipe recipe = SpoonRecipe.get(player.server);

        if (recipe.getAll().isEmpty()) {
            player.displayClientMessage(Component.literal(
                    "La receta esta vacia: pide a un administrador que la configure"), false);
            return;
        }

        // Convertimos los IDs de la receta en objetos reales.
        Map<Item, Integer> needs = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : recipe.getAll().entrySet()) {
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(entry.getKey());
            if (item.isEmpty()) {
                player.displayClientMessage(Component.literal(
                        "La receta usa un objeto que no existe: " + entry.getKey()), false);
                return;
            }
            needs.put(item.get(), entry.getValue());
        }

        // En creativo no se piden ni se gastan ingredientes.
        if (!player.isCreative()) {
            StringBuilder missing = new StringBuilder();
            for (Map.Entry<Item, Integer> entry : needs.entrySet()) {
                int have = count(player, entry.getKey());
                if (have < entry.getValue()) {
                    if (missing.length() > 0) {
                        missing.append(", ");
                    }
                    missing.append(entry.getValue() - have).append("x ")
                            .append(BuiltInRegistries.ITEM.getKey(entry.getKey()));
                }
            }
            if (missing.length() > 0) {
                player.displayClientMessage(Component.literal(
                        "Te faltan: " + missing), false);
                return;
            }

            for (Map.Entry<Item, Integer> entry : needs.entrySet()) {
                remove(player, entry.getKey(), entry.getValue());
            }
        }

        // Entregamos la cuchara (si no cabe en el inventario, cae al suelo).
        ItemStack spoon = new ItemStack(ModItems.SOUL_SPOON);
        if (!player.getInventory().add(spoon)) {
            player.drop(spoon, false);
        }

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 0.5, 0.5, 0.5);
        player.displayClientMessage(Component.literal(
                "Has forjado una Cuchara de Almas"), true);
    }

    // Cuantas unidades de un objeto lleva el jugador.
    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // Quita "amount" unidades del objeto, recorriendo el inventario.
    private static void remove(ServerPlayer player, Item item, int amount) {
        int left = takeFrom(player.getInventory().items, item, amount);
        takeFrom(player.getInventory().offhand, item, left);
    }

    // Devuelve cuantas unidades faltan por quitar.
    private static int takeFrom(NonNullList<ItemStack> stacks, Item item, int amount) {
        int left = amount;
        for (ItemStack stack : stacks) {
            if (left <= 0) {
                break;
            }
            if (stack.is(item)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
        return left;
    }
}