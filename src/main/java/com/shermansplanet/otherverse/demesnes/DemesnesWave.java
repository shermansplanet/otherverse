package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DemesnesWave {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum EntityTag {
        OVERWORLD, NETHER, END, FLYING, NIGHT, UNDERWATER, RIDEABLE, RIDER, NOVERWORLD
    }

    public static HashMap<EntityType<? extends Mob>, Set<EntityTag>> entityTagsByType = new HashMap<>();
    public static List<EntityType<? extends Mob>> allTypes = new ArrayList<>();

    public static HashMap<EntityType<? extends LivingEntity>, Float> effectiveHpOverrides = new HashMap<>();

    static {
        register(EntityType.ZOMBIE, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});
        register(EntityType.SKELETON, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT, EntityTag.RIDER});
        register(EntityType.HUSK, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});
        register(EntityType.STRAY, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT, EntityTag.RIDER});
        register(EntityType.SPIDER, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDEABLE});
        register(EntityType.CAVE_SPIDER, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDEABLE});
        register(EntityType.CREEPER, new EntityTag[]{EntityTag.OVERWORLD});
        register(EntityType.SLIME, new EntityTag[]{EntityTag.OVERWORLD});
        register(EntityType.WITCH, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDER});
        register(EntityType.WARDEN, new EntityTag[]{EntityTag.OVERWORLD});
        register(EntityType.SILVERFISH, new EntityTag[]{EntityTag.OVERWORLD});
        register(EntityType.PHANTOM, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT, EntityTag.FLYING, EntityTag.RIDEABLE});
        register(EntityType.WOLF, new EntityTag[]{EntityTag.OVERWORLD});

        register(EntityType.EVOKER, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDER});
        register(EntityType.PILLAGER, new EntityTag[]{EntityTag.OVERWORLD});
        register(EntityType.RAVAGER, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDEABLE});
        register(EntityType.VINDICATOR, new EntityTag[]{EntityTag.OVERWORLD});

        register(EntityType.GUARDIAN, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.UNDERWATER});
        register(EntityType.ELDER_GUARDIAN, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.UNDERWATER});
        register(EntityType.DROWNED, new EntityTag[]{EntityTag.OVERWORLD, EntityTag.UNDERWATER, EntityTag.NIGHT});

        register(EntityType.GHAST, new EntityTag[]{EntityTag.NETHER, EntityTag.FLYING});
        register(EntityType.PIGLIN_BRUTE, new EntityTag[]{EntityTag.NETHER, EntityTag.NOVERWORLD});
        register(EntityType.HOGLIN, new EntityTag[]{EntityTag.NETHER, EntityTag.RIDEABLE, EntityTag.NOVERWORLD});
        register(EntityType.BLAZE, new EntityTag[]{EntityTag.NETHER, EntityTag.FLYING});
        register(EntityType.WITHER_SKELETON, new EntityTag[]{EntityTag.NETHER});
        register(EntityType.MAGMA_CUBE, new EntityTag[]{EntityTag.NETHER});

        register(EntityType.ENDERMAN, new EntityTag[]{EntityTag.END});
        register(EntityType.SHULKER, new EntityTag[]{EntityTag.END});
        register(EntityType.ENDERMITE, new EntityTag[]{EntityTag.END});

        effectiveHpOverrides.put(EntityType.EVOKER, 100f);
    }

    private static void register(EntityType<? extends Mob> type, EntityTag[] entityTags) {
        entityTagsByType.put(type, Set.of(entityTags));
        allTypes.add(type);
    }

    public List<Mob> entities = new ArrayList<>();
    public float combinedEntityHp;
    private int waveIndex;

    public float getRemainingHp() {
        var totalHp = 0f;
        for (var entity : entities) {
            if (entity.isAlive()) totalHp += entity.getHealth();
        }
        return totalHp / combinedEntityHp;
    }

    private Mob trySpawn(DemesnesClaimRitual ritual, int hpRemaining) {
        var r = ritual.level.getRandom();
        var side = r.nextInt(4);
        var sideLength = ritual.maxBlock.getX() - ritual.minBlock.getX() - 2;
        var level = ritual.level;
        var claimantY = ritual.claimant.blockPosition().getY();
        var spawnPosXZ = switch (side) {
            case 0 -> new BlockPos(ritual.minBlock.getX() + 1, 0, ritual.minBlock.getZ() + r.nextInt(sideLength) + 1);
            case 1 -> new BlockPos(ritual.maxBlock.getX() - 1, 0, ritual.minBlock.getZ() + r.nextInt(sideLength) + 1);
            case 2 -> new BlockPos(ritual.minBlock.getX() + 1 + r.nextInt(sideLength), 0, ritual.minBlock.getZ() + 1);
            case 3 -> new BlockPos(ritual.minBlock.getX() + 1 + r.nextInt(sideLength), 0, ritual.maxBlock.getZ() - 1);
            default -> throw new IllegalStateException("Unexpected value: " + side);
        };
        BlockPos bestPos = null;
        int amountOfSpace = 0;
        var isMidair = true;
        for (var y = claimantY + 16; y >= claimantY - 16; y--) {
            var posToCheck = new BlockPos(spawnPosXZ.getX(), y, spawnPosXZ.getZ());
            if (level.getBlockState(posToCheck).getCollisionShape(level, posToCheck).isEmpty()) {
                bestPos = posToCheck;
                amountOfSpace++;
            } else if (bestPos != null) {
                if (amountOfSpace == 1) {
                    bestPos = null;
                } else {
                    isMidair = false;
                    break;
                }
            }
        }
        if (bestPos == null) {
            return null;
        }
        var isWater = level.isWaterAt(bestPos);
        var dimensionTag = getDimensionTag(level);
        boolean finalIsMidair = isMidair;
        int finalAmountOfSpace = amountOfSpace;
        var canSeeSky = level.canSeeSky(bestPos);
        var spawnPool = allTypes.stream().filter(et -> {
            var tags = entityTagsByType.get(et);
            if (isWater != tags.contains(EntityTag.UNDERWATER)) return false;
            if (dimensionTag == EntityTag.OVERWORLD && tags.contains(EntityTag.NOVERWORLD)) return false;
            if (finalIsMidair && !tags.contains(EntityTag.FLYING)) return false;
            if (finalAmountOfSpace < et.getHeight()) return false;
            if (level.isDay() && canSeeSky && tags.contains(EntityTag.NIGHT)) return false;
            var effectiveHp = getEffectiveHp(et);
            if (waveIndex == 1 && effectiveHp > 50) return false;
            if (hpRemaining < effectiveHp) return false;
            return true;
        }).toList();
        if (spawnPool.isEmpty()) return null;
        var newSpawnPool = spawnPool.stream().filter(et -> entityTagsByType.get(et).contains(dimensionTag)).toList();
        if (!newSpawnPool.isEmpty() && r.nextInt(waveIndex * 4) < newSpawnPool.size()) spawnPool = newSpawnPool;
        var typeToSpawn = spawnPool.get(r.nextInt(spawnPool.size()));
        var mob = typeToSpawn.create(level);
        if (mob == null) return null;
        var hp = getEffectiveHp(mob);
        var coeff = hpRemaining / hp;
        if (coeff <= 2 && mob.getBbHeight() * coeff <= finalAmountOfSpace) {
            ScaleTypes.BASE.getScaleData(mob).setScale(coeff);
            ScaleTypes.ATTACK.getScaleData(mob).setScale(coeff);
            ScaleTypes.DEFENSE.getScaleData(mob).setScale(coeff);
            ScaleTypes.KNOCKBACK.getScaleData(mob).setScale(coeff);
            ScaleTypes.HEALTH.getScaleData(mob).setScale(coeff);
        }
        mob.setPos(new Vec3(bestPos.getX() + 0.5f, bestPos.getY(), bestPos.getZ() + 0.5f));
        if (entityTagsByType.get(typeToSpawn).contains(EntityTag.NIGHT)) {
            if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                mob.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
            }
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(bestPos), MobSpawnType.EVENT, null, null);
        level.addFreshEntityWithPassengers(mob);
        mob.getPersistentData().putBoolean("demesnes_challenger", true);
        mob.getPersistentData().putString("demesnes_claimant", ritual.claimant.getGameProfile().getName());
        return mob;
    }

    private EntityTag getDimensionTag(ServerLevel level) {
        var name = level.dimensionTypeId().location().getPath();
        return switch (name) {
            case "overworld" -> EntityTag.OVERWORLD;
            case "the_nether" -> EntityTag.NETHER;
            case "the_end" -> EntityTag.END;
            default -> null;
        };
    }

    public DemesnesWave(DemesnesClaimRitual ritual, int waveIndex) {
        this.waveIndex = waveIndex;
        var hpRemaining = Mth.square(ritual.range * 2 + 1) * 60;
        if(ritual.claimant.getAbilities().instabuild) hpRemaining = 20;
        LOGGER.debug("Spawning mobs with combined HP of " + hpRemaining);
        var timeout = hpRemaining;
        combinedEntityHp = 0;
        while (hpRemaining > 1) {
            var entity = trySpawn(ritual, hpRemaining);
            if (entity == null) {
                timeout--;
                if (timeout == 0) {
                    LOGGER.error("Demesnes wave generation timed out!");
                    return;
                }
                continue;
            }
            entities.add(entity);
            combinedEntityHp += entity.getHealth();
            hpRemaining -= (int) getEffectiveHp(entity);
        }
        for (var entity : entities) {
            BindingManager.startAttacking(entity, ritual.claimant);
        }
    }

    float getEffectiveHp(EntityType<? extends LivingEntity> et) {
        if (effectiveHpOverrides.containsKey(et)) return effectiveHpOverrides.get(et);
        return (float) DefaultAttributes.getSupplier(et).getValue(Attributes.MAX_HEALTH);
    }

    float getEffectiveHp(LivingEntity le) {
        if (effectiveHpOverrides.containsKey(le.getType())) {
            return effectiveHpOverrides.get(le.getType()) * ScaleTypes.BASE.getScaleData(le).getScale();
        }
        return le.getMaxHealth();
    }
}
