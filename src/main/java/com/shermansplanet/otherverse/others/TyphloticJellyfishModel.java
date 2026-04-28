package com.shermansplanet.otherverse.others;// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TyphloticJellyfishModel<T extends TyphloticJellyfish> extends HierarchicalModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "typhloticjellyfishmodel"), "main");
    private final ModelPart root;
    private final ModelPart tentacle_1;
    private final ModelPart tentacle_3;
    private final ModelPart tentacle_2;
    private final ModelPart tentacle_4;
    private final ModelPart bell;
    private final ModelPart inner;

    public TyphloticJellyfishModel(ModelPart root) {
        this.root = root.getChild("root");
        this.tentacle_1 = this.root.getChild("tentacle_1");
        this.tentacle_3 = this.root.getChild("tentacle_3");
        this.tentacle_2 = this.root.getChild("tentacle_2");
        this.tentacle_4 = this.root.getChild("tentacle_4");
        this.bell = this.root.getChild("bell");
        this.inner = this.root.getChild("inner");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, -8.0F, 0.0F));

        PartDefinition tentacle_1 = root.addOrReplaceChild("tentacle_1", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, 0.0F, 0.0F, 14.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 0.0F, 1.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition tentacle_3 = root.addOrReplaceChild("tentacle_3", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, 0.0F, 0.0F, 14.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 0.0F, 7.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition tentacle_2 = root.addOrReplaceChild("tentacle_2", CubeListBuilder.create().texOffs(0, 32).addBox(-7.0F, 0.0F, 0.0F, 14.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -7.0F));

        PartDefinition tentacle_4 = root.addOrReplaceChild("tentacle_4", CubeListBuilder.create().texOffs(0, 32).addBox(-7.0F, 0.0F, 0.0F, 14.0F, 32.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition bell = root.addOrReplaceChild("bell", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -6.0F, -8.0F, 16.0F, 6.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition bell_r1 = bell.addOrReplaceChild("bell_r1", CubeListBuilder.create().texOffs(4, 2).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition inner = root.addOrReplaceChild("inner", CubeListBuilder.create().texOffs(32, 40).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(TyphloticJellyfish mob, float a, float b, float animTime, float d, float e) {
        var tentacleAngle = animTime < 15 ? (15 - animTime) / 10f : (animTime - 15f) / 25f;
        tentacleAngle = Mth.clamp(tentacleAngle, 0f, 1f);
        tentacleAngle *= -0.3f;
        tentacle_1.xRot = tentacleAngle;
        tentacle_2.xRot = tentacleAngle;
        tentacle_3.xRot = tentacleAngle;
        tentacle_4.xRot = tentacleAngle;

        var bellSwell = animTime < 10 ? (10 - animTime) / 10f : (animTime - 10f) / 30f;
        bellSwell = Mth.clamp(bellSwell, 0f, 1f);
        bellSwell *= 0.4f;
        inner.xScale = 1 + bellSwell;
        inner.yScale = 1 - bellSwell;
        inner.zScale = 1 + bellSwell;

        bellSwell *= 0.5f;
        bell.xScale = 1 + bellSwell;
        bell.yScale = 1 - bellSwell;
        bell.zScale = 1 + bellSwell;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}