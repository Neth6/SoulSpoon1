package neth6.soulspoon.mob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import neth6.soulspoon.rule.Rule;
import neth6.soulspoon.rule.RuleData;
import neth6.soulspoon.util.Cmd;

// Reglas de mobs que afectan a todos los mobs vanilla de un tipo (activables con /dedsafio regla).
public class MobRules {
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(MobRules::onTick);

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            RuleData rules = RuleData.get(level.getServer());
            // [10] Creepers electricos.
            if (entity instanceof Creeper creeper && rules.isOn(Rule.CREEPERS_ELECTRICOS)) {
                level.getServer().execute(() -> {
                    if (!creeper.isPowered() && creeper.isAlive()) {
                        Cmd.merge(creeper, "{powered:1b}");
                    }
                });
            }
            // [33] Zombies supremos: espada de hierro con Filo V.
            if (entity instanceof Zombie zombie && rules.isOn(Rule.ZOMBIES_SUPREMOS)) {
                level.getServer().execute(() -> {
                    if (zombie.isAlive() && zombie.getMainHandItem().isEmpty()) {
                        Cmd.run(level, zombie.position(), "item replace entity " + zombie.getUUID()
                                + " weapon.mainhand with minecraft:iron_sword"
                                + "[enchantments={levels:{\"minecraft:sharpness\":5}}]");
                    }
                });
            }
        });

        // [77] Las aranas ponen telarana al golpear...
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && source.getEntity() instanceof Spider
                    && RuleData.get(player.server).isOn(Rule.ARANAS_TELA)) {
                placeWeb(player.serverLevel(), player.blockPosition());
            }
            return true;
        });
        // ...y al morir.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof Spider && entity.level() instanceof ServerLevel level
                    && RuleData.get(level.getServer()).isOn(Rule.ARANAS_TELA)) {
                placeWeb(level, entity.blockPosition());
            }
        });
    }

    private static void placeWeb(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
        }
    }

    private static void onTick(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        RuleData rules = RuleData.get(server);
        int tick = server.getTickCount();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();

            // [23] Zombies trepadores: si chocan contra una pared mientras persiguen, suben.
            if (tick % 2 == 0 && rules.isOn(Rule.TREPADORES)) {
                for (Zombie zombie : level.getEntitiesOfClass(Zombie.class,
                        player.getBoundingBox().inflate(32))) {
                    if (zombie.horizontalCollision && zombie.getTarget() != null) {
                        Vec3 motion = zombie.getDeltaMovement();
                        zombie.setDeltaMovement(motion.x, 0.3, motion.z);
                    }
                }
            }
            if (tick % 20 == 0) {
                // [72] Olfato: los monstruos huelen a los jugadores invisibles.
                if (rules.isOn(Rule.OLFATO) && player.hasEffect(MobEffects.INVISIBILITY)
                        && !player.isCreative() && !player.isSpectator()) {
                    for (Monster monster : level.getEntitiesOfClass(Monster.class,
                            player.getBoundingBox().inflate(20), m -> m.getTarget() == null)) {
                        monster.setTarget(player);
                    }
                }
                // [58][65] Todas las aranas: velocidad infinita y Fuerza III.
                boolean fast = rules.isOn(Rule.ARANAS_VELOCES);
                boolean strong = rules.isOn(Rule.ARANAS_FUERZA);
                if (fast || strong) {
                    for (Spider spider : level.getEntitiesOfClass(Spider.class,
                            player.getBoundingBox().inflate(48))) {
                        if (fast && !spider.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                            spider.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                                    MobEffectInstance.INFINITE_DURATION, 4, false, false));
                        }
                        if (strong && !spider.hasEffect(MobEffects.DAMAGE_BOOST)) {
                            spider.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                                    MobEffectInstance.INFINITE_DURATION, 2, false, false));
                        }
                    }
                }
            }
        }
    }
}
