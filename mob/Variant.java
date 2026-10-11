package neth6.soulspoon.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

// Un mob "variante": un mob vanilla con nombre, atributos y comportamiento propios.
public final class Variant {
    public enum Place { GROUND, AIR, WATER }

    public interface Hit {
        void apply(Mob mob, ServerPlayer player);
    }

    private final String id;
    private final String name;
    private final EntityType<? extends Mob> type;

    Consumer<Mob> setup = mob -> { };
    BiConsumer<Mob, ServerLevel> tick;          // cada 5 ticks
    Hit hit;                                    // cuando golpea a un jugador
    Consumer<Mob> death;                        // al morir
    BiConsumer<Mob, ServerLevel> end;           // al desaparecer (p. ej. creeper que explota)
    BiConsumer<Mob, LivingEntity> hurt;         // cuando le hacen dano
    BiPredicate<ServerPlayer, ServerLevel> spawn; // aparicion natural
    Place place = Place.GROUND;
    int chance = 4;                             // 1 de cada N comprobaciones (cada 10 s)

    Variant(String id, String name, EntityType<? extends Mob> type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public EntityType<? extends Mob> type() {
        return type;
    }

    public String tag() {
        return "ded_" + id;
    }

    Variant setup(Consumer<Mob> value) {
        this.setup = value;
        return this;
    }

    Variant tick(BiConsumer<Mob, ServerLevel> value) {
        this.tick = value;
        return this;
    }

    Variant hit(Hit value) {
        this.hit = value;
        return this;
    }

    Variant death(Consumer<Mob> value) {
        this.death = value;
        return this;
    }

    Variant end(BiConsumer<Mob, ServerLevel> value) {
        this.end = value;
        return this;
    }

    Variant hurt(BiConsumer<Mob, LivingEntity> value) {
        this.hurt = value;
        return this;
    }

    Variant spawn(Place where, BiPredicate<ServerPlayer, ServerLevel> value) {
        this.place = where;
        this.spawn = value;
        return this;
    }
}
