package neth6.soulspoon.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

// GENERADO por tools/make_mobs.py: no editar a mano.
public final class ModelDefs {
    private ModelDefs() {}

    public static LayerDefinition reaper() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 28).addBox(-5.0f, -10.0f, -5.0f, 10.0f, 4.0f, 10.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(72, 0).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(8, 0).addBox(-5.0f, 12.0f, -3.0f, 10.0f, 12.0f, 6.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(96, 0).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offset(5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(112, 0).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f), PartPose.offsetAndRotation(-5.0f, 2.0f, 0.0f, -1.1f, 0.0f, 0.0f));
        root.addOrReplaceChild("scythe_handle", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -14.0f, -1.0f, 2.0f, 26.0f, 2.0f), PartPose.offset(-6.0f, 10.0f, -6.0f));
        root.addOrReplaceChild("scythe_blade", CubeListBuilder.create().texOffs(40, 28).addBox(-1.0f, -14.0f, -10.0f, 2.0f, 3.0f, 10.0f), PartPose.offset(-6.0f, 10.0f, -6.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition medula() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("rib1", CubeListBuilder.create().texOffs(0, 16).addBox(-5.0f, 1.0f, -3.0f, 10.0f, 2.0f, 6.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("rib2", CubeListBuilder.create().texOffs(32, 16).addBox(-5.0f, 5.0f, -3.0f, 10.0f, 2.0f, 6.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("rib3", CubeListBuilder.create().texOffs(64, 16).addBox(-5.0f, 9.0f, -3.0f, 10.0f, 2.0f, 6.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("spine1", CubeListBuilder.create().texOffs(96, 16).addBox(-1.0f, 0.0f, 2.0f, 2.0f, 3.0f, 3.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("spine2", CubeListBuilder.create().texOffs(106, 16).addBox(-1.0f, 4.0f, 2.0f, 2.0f, 3.0f, 3.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("spine3", CubeListBuilder.create().texOffs(116, 16).addBox(-1.0f, 8.0f, 2.0f, 2.0f, 3.0f, 3.0f), PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(56, 0).addBox(-1.0f, -2.0f, -1.0f, 3.0f, 12.0f, 3.0f), PartPose.offset(5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(68, 0).addBox(-2.0f, -2.0f, -1.0f, 3.0f, 12.0f, 3.0f), PartPose.offset(-5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(80, 0).addBox(-1.0f, 0.0f, -1.0f, 3.0f, 12.0f, 3.0f), PartPose.offset(2.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(92, 0).addBox(-2.0f, 0.0f, -1.0f, 3.0f, 12.0f, 3.0f), PartPose.offset(-2.0f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition cerebro() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -6.0f, -4.0f, 8.0f, 6.0f, 8.0f), PartPose.offset(0.0f, 21.0f, 0.0f));
        root.addOrReplaceChild("lobe", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0f, -9.0f, -3.0f, 6.0f, 3.0f, 6.0f), PartPose.offset(0.0f, 21.0f, 0.0f));
        root.addOrReplaceChild("lobe2", CubeListBuilder.create().texOffs(56, 0).addBox(-1.0f, -11.0f, -2.0f, 2.0f, 2.0f, 4.0f), PartPose.offset(0.0f, 21.0f, 0.0f));
        root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(68, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(3.0f, 21.0f, -2.0f));
        root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(76, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(-3.0f, 21.0f, -2.0f));
        root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(84, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(3.0f, 21.0f, 2.0f));
        root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(92, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f), PartPose.offset(-3.0f, 21.0f, 2.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition ghost() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.offset(0.0f, 8.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0f, 0.0f, -2.0f, 6.0f, 10.0f, 4.0f), PartPose.offset(0.0f, 8.0f, 0.0f));
        root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(68, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(0.0f, 18.0f, 0.0f));
        root.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(84, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f), PartPose.offset(0.0f, 24.0f, 0.0f));
        root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(52, 0).addBox(0.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(3.0f, 9.0f, 0.0f));
        root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(60, 0).addBox(-2.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f), PartPose.offset(-3.0f, 9.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition dactylo() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.0f, -8.0f, 6.0f, 6.0f, 16.0f), PartPose.offset(0.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(44, 0).addBox(-2.0f, 3.0f, -7.0f, 4.0f, 1.0f, 14.0f), PartPose.offset(0.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(86, 35).addBox(-2.0f, -2.0f, -5.0f, 4.0f, 4.0f, 5.0f), PartPose.offset(0.0f, 12.0f, -8.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 35).addBox(-2.0f, -3.0f, -7.0f, 4.0f, 4.0f, 6.0f), PartPose.offset(0.0f, 10.0f, -12.0f));
        root.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(0, 35).addBox(-1.0f, -1.0f, -17.0f, 2.0f, 2.0f, 10.0f), PartPose.offset(0.0f, 10.0f, -12.0f));
        root.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(68, 35).addBox(-1.0f, -5.0f, -2.0f, 2.0f, 3.0f, 7.0f), PartPose.offset(0.0f, 10.0f, -12.0f));
        root.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 22).addBox(0.0f, 0.0f, -5.0f, 20.0f, 1.0f, 12.0f), PartPose.offset(3.0f, 10.0f, 0.0f));
        root.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(64, 22).addBox(-20.0f, 0.0f, -5.0f, 20.0f, 1.0f, 12.0f), PartPose.offset(-3.0f, 10.0f, 0.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 35).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 10.0f), PartPose.offset(0.0f, 12.0f, 8.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition kragtops() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0f, 0.0f, -9.0f, 12.0f, 10.0f, 18.0f), PartPose.offset(0.0f, 10.0f, 0.0f));
        root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(36, 28).addBox(-4.0f, -2.0f, -6.0f, 8.0f, 2.0f, 12.0f), PartPose.offset(0.0f, 10.0f, 0.0f));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(60, 0).addBox(-5.0f, -5.0f, -8.0f, 10.0f, 10.0f, 8.0f), PartPose.offset(0.0f, 14.0f, -9.0f));
        root.addOrReplaceChild("frill", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0f, -9.0f, 0.0f, 16.0f, 14.0f, 2.0f), PartPose.offset(0.0f, 14.0f, -9.0f));
        root.addOrReplaceChild("horn_c", CubeListBuilder.create().texOffs(36, 44).addBox(-1.0f, 0.0f, -13.0f, 2.0f, 2.0f, 5.0f), PartPose.offset(0.0f, 14.0f, -9.0f));
        root.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(50, 44).addBox(3.0f, -6.0f, -12.0f, 2.0f, 2.0f, 5.0f), PartPose.offset(0.0f, 14.0f, -9.0f));
        root.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(64, 44).addBox(-5.0f, -6.0f, -12.0f, 2.0f, 2.0f, 5.0f), PartPose.offset(0.0f, 14.0f, -9.0f));
        root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(76, 28).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(4.0f, 18.0f, -6.0f));
        root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(92, 28).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(-4.0f, 18.0f, -6.0f));
        root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(108, 28).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(4.0f, 18.0f, 6.0f));
        root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(0, 44).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(-4.0f, 18.0f, 6.0f));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 44).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 2.0f, 8.0f), PartPose.offset(0.0f, 12.0f, 9.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition visceral() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f), PartPose.offset(0.0f, 6.0f, 0.0f));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f), PartPose.offset(0.0f, 6.0f, 0.0f));
        root.addOrReplaceChild("organ1", CubeListBuilder.create().texOffs(10, 16).addBox(-3.0f, 2.0f, -3.0f, 6.0f, 3.0f, 2.0f), PartPose.offset(0.0f, 6.0f, 0.0f));
        root.addOrReplaceChild("organ2", CubeListBuilder.create().texOffs(26, 16).addBox(-2.0f, 6.0f, -3.0f, 4.0f, 3.0f, 2.0f), PartPose.offset(0.0f, 6.0f, 0.0f));
        root.addOrReplaceChild("organ3", CubeListBuilder.create().texOffs(0, 16).addBox(-5.0f, 3.0f, -1.0f, 2.0f, 5.0f, 3.0f), PartPose.offset(0.0f, 6.0f, 0.0f));
        root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(56, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(2.0f, 18.0f, -4.0f));
        root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(72, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(-2.0f, 18.0f, -4.0f));
        root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(88, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(2.0f, 18.0f, 4.0f));
        root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(104, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f), PartPose.offset(-2.0f, 18.0f, 4.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition ojo() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -5.0f, -5.0f, 10.0f, 10.0f, 10.0f), PartPose.offset(0.0f, 14.0f, 0.0f));
        root.addOrReplaceChild("veins", CubeListBuilder.create().texOffs(40, 0).addBox(-5.0f, -2.0f, -5.0f, 10.0f, 1.0f, 10.0f), PartPose.offset(0.0f, 14.0f, 0.0f));
        root.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 20).addBox(0.0f, 0.0f, -3.0f, 8.0f, 1.0f, 6.0f), PartPose.offset(5.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(28, 20).addBox(-8.0f, 0.0f, -3.0f, 8.0f, 1.0f, 6.0f), PartPose.offset(-5.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("tendril1", CubeListBuilder.create().texOffs(88, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f), PartPose.offset(-2.0f, 19.0f, 1.0f));
        root.addOrReplaceChild("tendril2", CubeListBuilder.create().texOffs(96, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f), PartPose.offset(2.0f, 19.0f, -1.0f));
        root.addOrReplaceChild("tendril3", CubeListBuilder.create().texOffs(80, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f), PartPose.offset(0.0f, 19.0f, 2.0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

}
