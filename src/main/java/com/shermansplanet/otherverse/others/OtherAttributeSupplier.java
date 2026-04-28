package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
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
                        .add(Attributes.ATTACK_DAMAGE, 2)
                        .add(Attributes.ATTACK_KNOCKBACK)
                        .add(Attributes.ATTACK_SPEED)
                        .build());

        event.put(Otherverse.TYPHLOTIC_JELLYFISH.get(),
                LivingEntity.createLivingAttributes()
                        .add(Attributes.FOLLOW_RANGE)
                        .add(Attributes.FLYING_SPEED, 0.5f)
                        .add(Attributes.ATTACK_DAMAGE, 2)
                        .add(Attributes.ATTACK_KNOCKBACK, -1)
                        .add(Attributes.ATTACK_SPEED)
                        .build());

        event.put(Otherverse.TYPHLOTIC_ZOMBIE.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.FOLLOW_RANGE, 35.0D)
                        .add(Attributes.MOVEMENT_SPEED, (double)0.23F)
                        .add(Attributes.ATTACK_DAMAGE, 3.0D)
                        .add(Attributes.ARMOR, 2.0D)
                        .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0)
                        .build());
    }

    @SubscribeEvent
    public static void spawnPlacement(SpawnPlacementRegisterEvent event) {
        event.register(Otherverse.TYPHLOTIC_SHARK.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, TyphloticShark::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);

        event.register(Otherverse.TYPHLOTIC_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.WORLD_SURFACE, Zombie::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
