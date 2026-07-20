package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherverseConfig;
import com.shermansplanet.otherverse.binding.BindingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.*;

public class DemesnesWave {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum EntityTag {
        OVERWORLD, NETHER, END, RUINS, FLYING, NIGHT, UNDERWATER, RIDEABLE, RIDER, PIT, NOVERWORLD
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

        register(EntityType.PIGLIN_BRUTE, new EntityTag[]{EntityTag.NETHER, EntityTag.NOVERWORLD});
        register(EntityType.HOGLIN, new EntityTag[]{EntityTag.NETHER, EntityTag.RIDEABLE, EntityTag.NOVERWORLD});
        register(EntityType.BLAZE, new EntityTag[]{EntityTag.NETHER, EntityTag.FLYING});
        register(EntityType.WITHER_SKELETON, new EntityTag[]{EntityTag.NETHER});
        register(EntityType.MAGMA_CUBE, new EntityTag[]{EntityTag.NETHER});

        register(EntityType.ENDERMAN, new EntityTag[]{EntityTag.END});
        register(EntityType.SHULKER, new EntityTag[]{EntityTag.END});
        register(EntityType.ENDERMITE, new EntityTag[]{EntityTag.END});

        register(Otherverse.TYPHLOTIC_JELLYFISH.get(), new EntityTag[]{EntityTag.RUINS, EntityTag.FLYING, EntityTag.UNDERWATER});
        register(Otherverse.TYPHLOTIC_SHARK.get(), new EntityTag[]{EntityTag.RUINS, EntityTag.FLYING, EntityTag.UNDERWATER});
        register(Otherverse.TYPHLOTIC_ZOMBIE.get(), new EntityTag[]{EntityTag.RUINS, EntityTag.NIGHT});
        register(Otherverse.SNUFFER.get(), new EntityTag[]{EntityTag.RUINS});
        register(Otherverse.GUEST.get(), new EntityTag[]{EntityTag.RUINS});
        register(Otherverse.BUZZED.get(), new EntityTag[]{EntityTag.RUINS, EntityTag.FLYING});
        register(Otherverse.BANSHEE.get(), new EntityTag[]{EntityTag.RUINS, EntityTag.FLYING, EntityTag.NIGHT});
        register(Otherverse.FURY.get(), new EntityTag[]{EntityTag.RUINS, EntityTag.FLYING});

        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "boundroid"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "brainiac"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "caniac"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "caramel_cube"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "corrodent"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "ferrouslime"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.FLYING});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "gammaroach"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDEABLE});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "grottoceratops"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDEABLE});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "gum_worm"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "gumbeeper"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "hullbreaker"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.UNDERWATER});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "licowitch"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDER});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "magnetron"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "relicheirus"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "teletor"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "tremorsaurus"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "underzealot"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "vallumraptor"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "vesper"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "deep_one_knight"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.UNDERWATER});
        register(ResourceLocation.fromNamespaceAndPath("alexscaves", "deep_one_mage"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.UNDERWATER});

        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "crawler"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "direwolf"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDEABLE});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "equestrian"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "faller"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "intruder"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "huntsman"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDER, EntityTag.NIGHT});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "meature"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "scorpion"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "silverqueen"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "slugger"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "sprinter"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "starved"), new EntityTag[]{EntityTag.OVERWORLD});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "tarantula"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.RIDEABLE});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "troll"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "vampire"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});
        register(ResourceLocation.fromNamespaceAndPath("enemyexpansion", "wasp"), new EntityTag[]{EntityTag.OVERWORLD, EntityTag.NIGHT});

        register(ResourceLocation.fromNamespaceAndPath("macabre", "skinmaw"), new EntityTag[]{EntityTag.PIT});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "limbsplitter"), new EntityTag[]{EntityTag.PIT});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "marrow"), new EntityTag[]{EntityTag.PIT});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "spitter"), new EntityTag[]{EntityTag.PIT});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "cave_maggot"), new EntityTag[]{EntityTag.PIT});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "gorehound"), new EntityTag[]{EntityTag.PIT});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "crawler"), new EntityTag[]{EntityTag.PIT});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "gorebat"), new EntityTag[]{EntityTag.PIT, EntityTag.FLYING});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "ribserpent"), new EntityTag[]{EntityTag.PIT, EntityTag.UNDERWATER});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "stinger"), new EntityTag[]{EntityTag.PIT, EntityTag.UNDERWATER});
        register(ResourceLocation.fromNamespaceAndPath("macabre", "crack"), new EntityTag[]{EntityTag.PIT, EntityTag.UNDERWATER});

        effectiveHpOverrides.put(EntityType.EVOKER, 200f);

        if(OtherverseConfig.DEMESNES_MOB_GRIEFING.get()) {
            register(EntityType.GHAST, new EntityTag[]{EntityTag.NETHER, EntityTag.FLYING});
            register(EntityType.CREEPER, new EntityTag[]{EntityTag.OVERWORLD});
            effectiveHpOverrides.put(EntityType.GHAST, 50f);
        }
    }

    @SuppressWarnings("unchecked")
    private static void register(ResourceLocation resourceLocation, EntityTag[] entityTags) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(resourceLocation);
        if (type == null) return;
        register((EntityType<? extends Mob>) type, entityTags);
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

    public boolean areAnyEncroaching(AABB bounds) {
        var smallerBounds = bounds.deflate(4);
        for (var entity : entities) {
            if (entityTagsByType.get(entity.getType()).contains(EntityTag.FLYING)) continue;
            if (smallerBounds.contains(entity.position())) return true;
        }
        return false;
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
        if (dimensionTag != null) {
            var newSpawnPool = spawnPool.stream().filter(et -> entityTagsByType.getOrDefault(et, new HashSet<>()).contains(dimensionTag)).toList();
            if (!newSpawnPool.isEmpty() && r.nextInt(waveIndex * 4) < newSpawnPool.size()) spawnPool = newSpawnPool;
        }
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
            case "ruins" -> EntityTag.RUINS;
            case "the_pit" -> EntityTag.PIT;
            default -> null;
        };
    }

    public DemesnesWave(DemesnesClaimRitual ritual, int waveIndex) {
        this.waveIndex = waveIndex;
        var hpRemaining = Mth.square(ritual.range * 2 + 1) * 60;
        if (ritual.claimant.getAbilities().instabuild) hpRemaining = 20;
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
            BindingManager.forceAttack(entity, ritual.claimant);
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
