package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
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
                        .add(Attributes.ATTACK_DAMAGE, 3)
                        .add(Attributes.ATTACK_KNOCKBACK)
                        .add(Attributes.ATTACK_SPEED)
                        .build());

        event.put(Otherverse.TYPHLOTIC_JELLYFISH.get(),
                LivingEntity.createLivingAttributes()
                        .add(Attributes.FOLLOW_RANGE)
                        .add(Attributes.FLYING_SPEED, 0.5f)
                        .add(Attributes.ATTACK_DAMAGE, 3)
                        .add(Attributes.ATTACK_KNOCKBACK, -1)
                        .add(Attributes.ATTACK_SPEED)
                        .build());

        event.put(Otherverse.TYPHLOTIC_ZOMBIE.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.FOLLOW_RANGE, 35.0D)
                        .add(Attributes.MOVEMENT_SPEED, (double) 0.23F)
                        .add(Attributes.ATTACK_DAMAGE, 6)
                        .add(Attributes.ARMOR, 2.0D)
                        .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0)
                        .build());

        event.put(Otherverse.SNUFFER.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.FOLLOW_RANGE)
                        .add(Attributes.MOVEMENT_SPEED, (double) 0.2F)
                        .add(Attributes.MAX_HEALTH, 14.0D)
                        .add(Attributes.ATTACK_DAMAGE, 6)
                        .add(Attributes.ATTACK_KNOCKBACK)
                        .add(Attributes.ATTACK_SPEED)
                        .build());

        event.put(Otherverse.GUEST.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 40.0D)
                        .add(Attributes.MOVEMENT_SPEED, (double) 0.3F)
                        .add(Attributes.ATTACK_DAMAGE, 4)
                        .add(Attributes.FOLLOW_RANGE, 64.0D)
                        .build());

        event.put(Otherverse.BUZZED.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.FOLLOW_RANGE)
                        .add(Attributes.FLYING_SPEED, 3f)
                        .add(Attributes.MAX_HEALTH, 14.0D)
                        .add(Attributes.ATTACK_DAMAGE, 2)
                        .add(Attributes.ATTACK_KNOCKBACK)
                        .add(Attributes.ATTACK_SPEED)
                        .build());

        event.put(Otherverse.FURY.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.ATTACK_DAMAGE, 3)
                        .add(Attributes.MOVEMENT_SPEED, (double)0.23F)
                        .add(Attributes.FOLLOW_RANGE, 48.0D)
                        .build());

        event.put(Otherverse.BANSHEE.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.ATTACK_DAMAGE, 2)
                        .build());

        FamiliarManager.loadCustomMobs();
    }

    @SubscribeEvent
    public static void spawnPlacement(SpawnPlacementRegisterEvent event) {
        event.register(Otherverse.TYPHLOTIC_SHARK.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, TyphloticShark::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);

        event.register(Otherverse.TYPHLOTIC_JELLYFISH.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, TyphloticJellyfish::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);

        event.register(Otherverse.TYPHLOTIC_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.WORLD_SURFACE, TyphloticZombie::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(Otherverse.SNUFFER.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, Snuffer::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);

        event.register(Otherverse.GUEST.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, Guest::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(Otherverse.BUZZED.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, Buzzed::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);

        event.register(Otherverse.FURY.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.WORLD_SURFACE, Fury::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);

        event.register(Otherverse.BANSHEE.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.WORLD_SURFACE, Banshee::checkMobSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
