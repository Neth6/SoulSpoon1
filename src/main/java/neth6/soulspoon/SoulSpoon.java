package neth6.soulspoon;

import neth6.soulspoon.soul.DeathSequence;

import neth6.soulspoon.item.ImportedContent;

import neth6.soulspoon.sound.ModSounds;



import neth6.soulspoon.item.ModArmor;
import neth6.soulspoon.entity.GeoMobs;
import net.fabricmc.api.ModInitializer;
import neth6.soulspoon.boveda.ModBoveda;
import neth6.soulspoon.hermandad.ModHermandad;
import neth6.soulspoon.cofres.ModCofres;
import neth6.soulspoon.banco.ModCajero;
import neth6.soulspoon.correo.ModCorreo;
import neth6.soulspoon.casino.ModCasino;

import net.minecraft.resources.ResourceLocation;

import neth6.soulspoon.block.ModBlocks;
import neth6.soulspoon.wheel.ModWheel;
import neth6.soulspoon.sound.ModSounds;
import neth6.soulspoon.command.AnnounceCommand;
import neth6.soulspoon.wheel.WheelAnim;
import neth6.soulspoon.command.ModCommands;
import neth6.soulspoon.command.RuletaCommands;
import neth6.soulspoon.codex.CodexCommands;
import neth6.soulspoon.codex.ContentManager;
import neth6.soulspoon.mob.VariantItems;
import neth6.soulspoon.mob.VariantManager;
import neth6.soulspoon.command.DimensionCommands;
import neth6.soulspoon.mob.PrimitivePoison;
import neth6.soulspoon.world.Dimensions;
import neth6.soulspoon.world.EventManager;
import neth6.soulspoon.world.QuiuSpawns;
import neth6.soulspoon.world.WorldRules;
import neth6.soulspoon.codex.QuestService;
import neth6.soulspoon.codex.RecipeFilter;
import neth6.soulspoon.network.CodexPackets;
import neth6.soulspoon.event.ModEvents;
import neth6.soulspoon.entity.ModEntities;
import neth6.soulspoon.item.ModItems;
import neth6.soulspoon.item.ModGear;
import neth6.soulspoon.rule.ModRules;
import neth6.soulspoon.soul.SoulManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoulSpoon implements ModInitializer {
    public static final String MOD_ID = "soulspoon";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModSounds.initialize();
        ModWheel.initialize();
        WheelAnim.initialize();
        DeathSequence.initialize();
        ModItems.initialize();
        ModGear.initialize();
        ModEntities.initialize();
        GeoMobs.initialize();
        ModArmor.initialize();
        ModBlocks.initialize();
        SoulManager.initialize();
        ModCommands.initialize();
        CodexCommands.initialize();
        WorldRules.initialize();
        EventManager.initialize();
        Dimensions.initialize();
        neth6.soulspoon.world.limbo.Limbo.initialize();
        QuiuSpawns.initialize();
        PrimitivePoison.initialize();
        DimensionCommands.initialize();
        VariantManager.initialize();
        VariantItems.initialize();
        // Codex: contenido (config/soulspoon/codex.json) -> paquetes -> eventos de misiones.
        ContentManager.load();
        CodexPackets.initialize();
        QuestService.initialize();
        neth6.soulspoon.economy.Economy.initialize();
        RecipeFilter.initialize();
        ModEvents.initialize();
        ModRules.initialize();
        RuletaCommands.initialize();
        AnnounceCommand.initialize();

        ModCofres.initialize();
        ModCajero.initialize();
        ModCorreo.initialize();
        ModCasino.initialize();
        ModHermandad.initialize();
        ModBoveda.initialize();
        ModSocial.initialize();
        ImportedContent.initialize();

        LOGGER.info("SoulSpoon cargado: la Cuchara de Almas esta registrada");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}