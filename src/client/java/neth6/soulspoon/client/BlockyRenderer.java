package neth6.soulspoon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import neth6.soulspoon.SoulSpoon;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class BlockyRenderer extends MobRenderer<Mob, BlockyModel<Mob>> {
    private final ResourceLocation texture;
    private final float scale;

    public BlockyRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                          String mobId, float scale, float shadow) {
        super(context, new BlockyModel<>(context.bakeLayer(layer)), shadow);
        this.texture = SoulSpoon.id("textures/entity/" + mobId + ".png");
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        return texture;
    }

    @Override
    protected void scale(Mob entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(scale, scale, scale);
    }
}
