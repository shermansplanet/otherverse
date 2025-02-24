package com.shermansplanet.otherverse.others;// Made with Blockbench 4.10.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.definitions.WardenAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class TyphloticSharkModel<T extends TyphloticShark> extends HierarchicalModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Otherverse.MODID, "typhloticsharkmodel"), "main");
    private final ModelPart body;
    private final ModelPart fin_r;
    private final ModelPart midsection;
    private final ModelPart tail;
    private final ModelPart tip;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart fin_l;

    private final ModelPart root;

    public TyphloticSharkModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.body = root.getChild("body");
        this.fin_r = this.body.getChild("fin_r");
        this.midsection = this.body.getChild("midsection");
        this.tail = this.midsection.getChild("tail");
        this.tip = this.tail.getChild("tip");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.fin_l = this.body.getChild("fin_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 8.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 20.0F, 0.0F));

        PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 36).addBox(-0.5F, -2.545F, -8.2844F, 1.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition fin_r = body.addOrReplaceChild("fin_r", CubeListBuilder.create(), PartPose.offset(4.0F, 2.5F, -1.0F));

        PartDefinition fin_r1 = fin_r.addOrReplaceChild("fin_r1", CubeListBuilder.create().texOffs(29, 40).addBox(0.0F, -0.5F, -2.0F, 7.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.48F, 0.7418F));

        PartDefinition midsection = body.addOrReplaceChild("midsection", CubeListBuilder.create().texOffs(0, 21).addBox(-2.0F, -3.0F, -9.0F, 4.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -7.0F));

        PartDefinition cube_r2 = midsection.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(30, 45).addBox(-0.5F, -0.545F, -6.2844F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -4.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition tail = midsection.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(18, 39).addBox(-1.0F, -2.0F, -7.0F, 2.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -9.0F));

        PartDefinition tip = tail.addOrReplaceChild("tip", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -6.0F));

        PartDefinition cube_r3 = tip.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(45, 40).addBox(-0.5F, -1.545F, -6.2844F, 1.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r4 = tip.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(44, 8).addBox(-0.5F, -2.545F, -7.2844F, 1.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 6.0F));

        PartDefinition teeth_top_r1 = head.addOrReplaceChild("teeth_top_r1", CubeListBuilder.create().texOffs(29, 0).addBox(-3.0F, 0.0F, -1.0F, 6.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(26, 21).addBox(-3.0F, -3.0F, -1.0F, 6.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0873F, 0.0F, 0.0F));

        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0.0F, 1.25F, 0.0F));

        PartDefinition teeth_bottom_r1 = jaw.addOrReplaceChild("teeth_bottom_r1", CubeListBuilder.create().texOffs(20, 32).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(38, 33).addBox(-3.0F, 1.0F, 0.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.25F, 0.0F, -0.3054F, 0.0F, 0.0F));

        PartDefinition fin_l = body.addOrReplaceChild("fin_l", CubeListBuilder.create(), PartPose.offset(-4.0F, 2.5F, -1.0F));

        PartDefinition fin_r2 = fin_l.addOrReplaceChild("fin_r2", CubeListBuilder.create().texOffs(29, 8).addBox(-7.0F, -0.5F, -2.0F, 7.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.48F, -0.7418F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(TyphloticShark entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        var anim_time = ageInTicks / 20f * Math.PI;
        var deg2rad = (float) Math.PI / 180;
        this.head.yRot = (float) Math.sin(anim_time + 60 * deg2rad) * 6f * deg2rad;
        this.jaw.xRot = (float) Math.sin(anim_time * 2) * -10f * deg2rad;
        this.midsection.yRot = (float) Math.sin(anim_time - 60 * deg2rad) * 12f * deg2rad;
        this.tail.yRot = (float) Math.sin(anim_time - 120 * deg2rad) * 24f * deg2rad;
        this.tip.yRot = (float) Math.sin(anim_time - Math.PI) * 20f * deg2rad;
        this.fin_l.yRot = (float) Math.sin(anim_time * 2) * -10f * deg2rad;
        this.fin_l.zRot = (float) Math.sin(anim_time * 2) * -10f * deg2rad;
        this.fin_r.yRot = (float) Math.sin(anim_time * 2) * 10f * deg2rad;
        this.fin_r.zRot = (float) Math.sin(anim_time * 2) * 10f * deg2rad;
        this.animate(entity.attackAnimationState, TyphloticSharkModelAnimation.attack, ageInTicks);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}