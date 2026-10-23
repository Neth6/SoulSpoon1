package neth6.soulspoon.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

// Controlador de animacion generico: "animation.<id>.idle" quieto y "animation.<id>.walk" en movimiento.
// El <id> es el id registrado de la entidad (por ejemplo "narval"), asi que cada mob nuevo solo
// necesita su archivo animations/entity/<id>.animation.json con esas dos animaciones.
public final class GeoAnim {
    private GeoAnim() {}

    public static String idOf(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }

    public static <T extends Entity & GeoAnimatable> void move(T self,
                                                               AnimatableManager.ControllerRegistrar controllers) {
        String id = idOf(self);
        RawAnimation idle = RawAnimation.begin().thenLoop("animation." + id + ".idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation." + id + ".walk");
        controllers.add(new AnimationController<T>(self, "move", 3,
                state -> state.setAndContinue(state.isMoving() ? walk : idle)));
    }
}
