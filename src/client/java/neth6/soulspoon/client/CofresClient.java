package neth6.soulspoon.client;

import neth6.soulspoon.client.cofres.CofreHuesosRenderer;
import neth6.soulspoon.client.cofres.CofreRenderer;
import neth6.soulspoon.cofres.ModCofres;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.Direction;

/** Llamar a CofresClient.initialize() desde SoulSpoonClient.onInitializeClient(). */
public final class CofresClient {
    private CofresClient() {}

    public static void initialize() {
        BlockEntityRenderers.register(ModCofres.COFRE_ENTIDAD, CofreRenderer::new);
        BuiltinItemRendererRegistry.INSTANCE.register(ModCofres.COFRE_ITEM, (pila, modo, pose, buffers, luz, overlay) -> {
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0.0, 0.5, 0.0);
            CofreRenderer.dibujar(pose, buffers, luz, overlay, Direction.SOUTH, 0.0F, false);
            pose.popPose();
        });
        BlockEntityRenderers.register(ModCofres.COFRE_HUESOS_ENTIDAD, CofreHuesosRenderer::new);
        BuiltinItemRendererRegistry.INSTANCE.register(ModCofres.COFRE_HUESOS_ITEM, (pila, modo, pose, buffers, luz, overlay) -> {
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0.0, 0.5, 0.0);
            CofreHuesosRenderer.dibujar(pose, buffers, luz, overlay, Direction.SOUTH, 0.0F, false);
            pose.popPose();
        });
    }
}
