package neth6.soulspoon;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import neth6.soulspoon.block.ModBlocks;
import neth6.soulspoon.command.ModCommands;
import neth6.soulspoon.event.ModEvents;
import neth6.soulspoon.item.ModItems;
import neth6.soulspoon.soul.SoulManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoulSpoon implements ModInitializer {
    public static final String MOD_ID = "soulspoon";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
        SoulManager.initialize();
        ModCommands.initialize();
        ModEvents.initialize();

        LOGGER.info("SoulSpoon cargado: la Cuchara de Almas esta registrada");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}