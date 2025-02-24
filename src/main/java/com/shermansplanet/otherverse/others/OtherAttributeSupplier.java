package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OtherAttributeSupplier {
    @SubscribeEvent
    public static void newEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Otherverse.TYPHLOTIC_SHARK.get(),
                LivingEntity.createLivingAttributes()
                        .add(Attributes.FOLLOW_RANGE)
                        .add(Attributes.FLYING_SPEED, 1f)
                        .add(Attributes.ATTACK_DAMAGE, 16)
                        .add(Attributes.ATTACK_KNOCKBACK)
                        .add(Attributes.ATTACK_SPEED)
                        .build());
    }

    @SubscribeEvent
    public static void spawnPlacement(SpawnPlacementRegisterEvent event) {
        event.register(Otherverse.TYPHLOTIC_SHARK.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, TyphloticShark::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
    }
}
