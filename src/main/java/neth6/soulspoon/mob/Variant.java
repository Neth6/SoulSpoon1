package neth6.soulspoon.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class Variant {

    public enum Place {
        GROUND, AIR, WATER
    }

    private final String id;
    private final String name;
    private final EntityType<?> type;

    private Consumer<Mob> setupConsumer;
    private BiConsumer<Mob, ServerLevel> tickConsumer;
    private BiConsumer<Mob, ServerLevel> endConsumer;
    private BiConsumer<Mob, LivingEntity> hurtConsumer;
    private BiConsumer<Mob, ServerPlayer> hitConsumer;
    private Consumer<Mob> deathConsumer;

    private Place place = Place.GROUND;
    private BiPredicate<ServerPlayer, ServerLevel> spawnPredicate;

    // Probabilidad de aparición: 1 entre chance.
    // Por defecto, 1 entre 20.
    private int chance = 20;

    public Variant(String id, String name, EntityType<?> type) {
        this.id = id;
        this.name = name;
        this.type = type;
        // La etiqueta permite distinguir variantes que usan la misma entidad vanilla.
        this.setupConsumer = mob -> mob.addTag(id);
    }

    public String id() { return id; }
    public String tag() { return id; }
    public String getId() { return id; }
    public String getName() { return name; }
    public EntityType<?> getType() { return type; }

    public BiConsumer<Mob, ServerPlayer> getHit() { return hitConsumer; }
    public BiConsumer<Mob, LivingEntity> getHurt() { return hurtConsumer; }
    public BiConsumer<Mob, ServerLevel> getTick() { return tickConsumer; }
    public BiConsumer<Mob, ServerLevel> getEnd() { return endConsumer; }
    public Consumer<Mob> getDeath() { return deathConsumer; }
    public Consumer<Mob> getSetup() { return setupConsumer; }
    public Place getPlace() { return place; }
    public BiPredicate<ServerPlayer, ServerLevel> getSpawnPredicate() { return spawnPredicate; }
    public int getChance() { return chance; }

    public Variant chance(int chance) {
        if (chance < 1) {
            throw new IllegalArgumentException("La probabilidad debe ser mayor que 0");
        }
        this.chance = chance;
        return this;
    }

    public Variant setup(Consumer<Mob> setupConsumer) {
        Consumer<Mob> previous = this.setupConsumer;
        this.setupConsumer = mob -> {
            if (previous != null) {
                previous.accept(mob);
            }
            if (setupConsumer != null) {
                setupConsumer.accept(mob);
            }
            mob.addTag(id);
        };
        return this;
    }

    public Variant tick(BiConsumer<Mob, ServerLevel> tickConsumer) {
        this.tickConsumer = tickConsumer;
        return this;
    }

    public Variant end(BiConsumer<Mob, ServerLevel> endConsumer) {
        this.endConsumer = endConsumer;
        return this;
    }

    public Variant hurt(BiConsumer<Mob, LivingEntity> hurtConsumer) {
        this.hurtConsumer = hurtConsumer;
        return this;
    }

    public Variant hit(BiConsumer<Mob, ServerPlayer> hitConsumer) {
        this.hitConsumer = hitConsumer;
        return this;
    }

    public Variant death(Consumer<Mob> deathConsumer) {
        this.deathConsumer = deathConsumer;
        return this;
    }

    public Variant spawn(Place place, BiPredicate<ServerPlayer, ServerLevel> spawnPredicate) {
        this.place = place;
        this.spawnPredicate = spawnPredicate;
        return this;
    }

    public Variant spawn(Place place) {
        this.place = place;
        return this;
    }
}
