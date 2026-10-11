package neth6.soulspoon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import neth6.soulspoon.entity.SoulFlyer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

// Modelo generico: anima las partes segun su nombre
// (head, leg_l/leg_r, leg_fl/fr/bl/br, arm_l/arm_r, wing_l/wing_r).
public class BlockyModel<T extends Mob> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart head, legL, legR, legFl, legFr, legBl, legBr, armL, armR, wingL, wingR;

    public BlockyModel(ModelPart root) {
        this.root = root;
        this.head = part(root, "head");
        this.legL = part(root, "leg_l");
        this.legR = part(root, "leg_r");
        this.legFl = part(root, "leg_fl");
        this.legFr = part(root, "leg_fr");
        this.legBl = part(root, "leg_bl");
        this.legBr = part(root, "leg_br");
        this.armL = part(root, "arm_l");
        this.armR = part(root, "arm_r");
        this.wingL = part(root, "wing_l");
        this.wingR = part(root, "wing_r");
    }

    private static ModelPart part(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        if (head != null) {
            head.yRot = netHeadYaw * ((float) Math.PI / 180f);
            head.xRot = headPitch * ((float) Math.PI / 180f);
        }
        float swing = Mth.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount;
        float counter = Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.2f * limbSwingAmount;
        boolean flying = entity instanceof SoulFlyer;

        if (legL != null) legL.xRot = swing;
        if (legR != null) legR.xRot = counter;
        if (legFl != null) legFl.xRot = swing;
        if (legBr != null) legBr.xRot = swing;
        if (legFr != null) legFr.xRot = counter;
        if (legBl != null) legBl.xRot = counter;

        if (flying) {
            float flap = Mth.sin(ageInTicks * 0.7f) * 0.7f;
            if (wingL != null) wingL.zRot = flap;
            if (wingR != null) wingR.zRot = -flap;
            float sway = 0.25f + Mth.sin(ageInTicks * 0.12f) * 0.12f;
            if (armL != null) armL.zRot = -sway;
            if (armR != null) armR.zRot = sway;
        } else {
            if (armL != null) armL.xRot = counter;
            if (armR != null) armR.xRot = swing;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
