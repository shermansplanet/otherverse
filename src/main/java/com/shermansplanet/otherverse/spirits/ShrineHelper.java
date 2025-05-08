package com.shermansplanet.otherverse.spirits;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Vector3f;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

public class ShrineHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum ShrineShape {BELOW, ABOVE, CENTERED}

    public static abstract class OverflowBehavior {
        public boolean affectsIndividualBlocks() {
            return true;
        }

        public ShrineShape getShape() {
            return ShrineShape.BELOW;
        }

        public boolean canRun(int amount, boolean isOverdraw) {
            return amount > 0;
        }

        public int affectShrine(Shrine shrine, Level level, int amount, boolean isOverdraw, boolean simulate) {
            if (isOverdraw) {
                return overdrawShrine(shrine, level, amount, simulate);
            }
            return overflowShrine(shrine, level, amount, simulate);
        }

        public int overflowShrine(Shrine shrine, Level level, int amount, boolean simulate) {
            return 0;
        }

        public int overdrawShrine(Shrine shrine, Level level, int amount, boolean simulate) {
            return 0;
        }

        public int affectBlock(BlockPos pos, Level level, int amount, boolean isOverdraw, boolean simulate) {
            if (isOverdraw) {
                return overdrawBlock(pos, level, amount, simulate);
            }
            return overflowBlock(pos, level, amount, simulate);
        }

        public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
            return 0;
        }

        public int overdrawBlock(BlockPos pos, Level level, int amount, boolean simulate) {
            return 0;
        }

        public int affectEntity(Entity e, int amount, boolean overdraw, Shrine shrine, boolean simulate) {
            if (overdraw) {
                return overdrawEntity(e, amount, shrine, simulate);
            }
            return overflowEntity(e, amount, shrine, simulate);
        }

        public int overflowEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
            return 0;
        }

        public int overdrawEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
            return 0;
        }

        public boolean multipleTargets() {
            return false;
        }
    }

    public static HashMap<SpiritType, OverflowBehavior> overflowBehaviors = new HashMap<>();

    static {
        for (var colorSpirit : Spirits.colorsByDye.entrySet()) {
            var diffuseColors = colorSpirit.getKey().getTextureDiffuseColors();
            var particleOptions = switch (colorSpirit.getValue().label()) {
                case "blue" -> ParticleTypes.UNDERWATER;
                case "brown" -> ParticleTypes.SOUL;
                case "cyan" -> ParticleTypes.SOUL_FIRE_FLAME;
                case "light_gray" -> ParticleTypes.ASH;
                case "gray" -> ParticleTypes.SMOKE;
                case "green" -> ParticleTypes.SPORE_BLOSSOM_AIR;
                case "light_blue" -> ParticleTypes.BUBBLE_POP;
                case "red" -> ParticleTypes.CRIMSON_SPORE;
                case "orange" -> ParticleTypes.SMALL_FLAME;
                case "magenta" -> ParticleTypes.DRAGON_BREATH;
                case "white" -> ParticleTypes.WHITE_ASH;
                default ->
                        new DustParticleOptions(new Vector3f(diffuseColors[0], diffuseColors[1], diffuseColors[2]), 1.0F);
            };
            overflowBehaviors.put(colorSpirit.getValue(), new OverflowBehavior() {
                @Override
                public ShrineShape getShape() {
                    return ShrineShape.ABOVE;
                }

                @Override
                public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                    if (!(level instanceof ServerLevel sl)) return 0;
                    if (!level.isEmptyBlock(pos)) return 0;
                    if (!simulate) {
                        var r = level.random;
                        sl.sendParticles(particleOptions,
                                pos.getX() + r.nextFloat(),
                                pos.getY() + r.nextFloat(),
                                pos.getZ() + r.nextFloat(),
                                0, 0, 0, 0, 0.1
                        );
                    }
                    return 1;
                }

                @Override
                public boolean multipleTargets() {
                    return true;
                }
            });
        }
        overflowBehaviors.put(Spirits.EARTH, new OverflowBehavior() {

            @Override
            public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                if (!level.isEmptyBlock(pos)) return 0;
                //if (amount < 3) {
                if (!simulate) {
                    level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                    level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1, 1);
                }
                return 1;
                /*}
                if (amount < 7) {
                    if(!simulate) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                    return 3;
                }
                if(!simulate) level.setBlockAndUpdate(pos, Blocks.DEEPSLATE.defaultBlockState());
                return 7;*/
            }

            @Override
            public int overdrawBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                var bs = level.getBlockState(pos);
                if (bs.isAir()) return 0;
                try {
                    var item = bs.getBlock().asItem();
                    var spiritCount = SpiritLabeler.getSpiritsFor(item).get(Spirits.EARTH);
                    if (spiritCount == null) return 0;
                    if (!simulate) {
                        level.destroyBlock(pos, false);
                    }
                    return spiritCount;
                } catch (Exception e) {
                    LOGGER.error(e.toString());
                    return 0;
                }
            }
        });
        overflowBehaviors.put(Spirits.AIR, new OverflowBehavior() {
            @Override
            public boolean affectsIndividualBlocks() {
                return false;
            }

            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }

            @Override
            public int overflowEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!simulate) doKnockback(e, amount, shrine);
                return amount;
            }

            @Override
            public int overdrawEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!simulate) doKnockback(e, -amount, shrine);
                return amount;
            }

            private void doKnockback(Entity e, int amount, Shrine shrine) {
                var center = e.getPosition(1.0F).add(0, e.getBbHeight() / 2, 0);
                var vec = center.subtract(shrine.range.center).normalize();
                var knockback = amount * 0.05f;
                if (e instanceof LivingEntity le) {
                    knockback *= (float) (1.0D - le.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                }
                vec = vec.scale(knockback);
                e.push(vec.x, vec.y, vec.z);
            }
        });
        overflowBehaviors.put(Spirits.FIRE, new OverflowBehavior() {

            public boolean canRun(int amount, boolean isOverdraw) {
                return isOverdraw ? amount >= SpiritLabeler.getSpiritsFor(Items.LAVA_BUCKET).get(Spirits.FIRE) : amount >= 7;
            }

            @Override
            public int overflowShrine(Shrine shrine, Level level, int amount, boolean simulate) {
                if (amount < 7) return 0;
                var possibleBlocks = new ArrayList<Pair<BlockPos, Direction>>();
                for (var shrinePos : shrine.hallowPositions) {
                    for (var dir : Direction.values()) {
                        var newpos = shrinePos.relative(dir);
                        if (level.isEmptyBlock(newpos)) possibleBlocks.add(new Pair<>(newpos, dir));
                    }
                }
                if (possibleBlocks.isEmpty()) return 0;
                if (simulate) return 7;
                if (level instanceof ServerLevel sl && !simulate) {
                    var loc = possibleBlocks.get(level.getRandom().nextInt(possibleBlocks.size()));
                    SmallFireball smallfireball = new SmallFireball(sl,
                            loc.getFirst().getX() + 0.5f, loc.getFirst().getY() + 0.5f, loc.getFirst().getZ() + 0.5f,
                            loc.getSecond().getStepX(), loc.getSecond().getStepY(), loc.getSecond().getStepZ());
                    level.playSound(null, loc.getFirst(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1, 1);
                    sl.addFreshEntity(smallfireball);
                }
                return 7;
            }

            @Override
            public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                if (!level.isEmptyBlock(pos)) {
                    var fluidState = level.getFluidState(pos);
                    if (!fluidState.is(Fluids.FLOWING_LAVA)) return 0;
                }
                if (!simulate) level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                return SpiritLabeler.getSpiritsFor(Items.LAVA_BUCKET).get(Spirits.FIRE);
            }

            @Override
            public int overdrawBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                var fluidState = level.getFluidState(pos);
                if (!fluidState.is(Fluids.LAVA)) return 0;
                if (!simulate) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1, 1);
                return SpiritLabeler.getSpiritsFor(Items.LAVA_BUCKET).get(Spirits.FIRE);
            }
        });
        overflowBehaviors.put(Spirits.COLD, new OverflowBehavior() {
            public boolean canRun(int amount, boolean isOverdraw) {
                return amount >= 3;
            }

            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }

            @Override
            public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                var bs = level.getBlockState(pos);
                if (bs.is(Blocks.WATER)) {
                    if (!simulate) level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                    return 3;
                }
                if (bs.is(Blocks.LAVA)) {
                    if (!simulate) level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                    return 3;
                }
                if (bs.is(Blocks.AIR)) {
                    var below = level.getBlockState(pos.below());
                    if (!below.isCollisionShapeFullBlock(level, pos.below())) return 0;
                    var surroundingBlocks = 0;
                    if (!simulate) {
                        for (var dir : Direction.values()) {
                            if (dir.getAxis() == Direction.Axis.Y) continue;
                            if (level.getBlockState(pos.relative(dir)).isCollisionShapeFullBlock(level, pos.relative(dir))) {
                                surroundingBlocks++;
                            }
                        }
                        level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Math.min(7, surroundingBlocks * 2 + 1)));
                    }
                    return 3;
                }
                if (bs.is(Blocks.SNOW_BLOCK)) {
                    if (!simulate) level.setBlockAndUpdate(pos, Blocks.POWDER_SNOW.defaultBlockState());
                    return 24;
                }
                return 0;
            }

            @Override
            public int overdrawBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                var bs = level.getBlockState(pos);
                if (bs.is(Blocks.POWDER_SNOW)) {
                    level.setBlockAndUpdate(pos, Blocks.SNOW_BLOCK.defaultBlockState());
                    return 18;
                }
                if (bs.is(Blocks.SNOW)) {
                    level.destroyBlock(pos, false);
                    return 3;
                }
                return 0;
            }

            @Override
            public boolean multipleTargets() {
                return true;
            }
        });
        overflowBehaviors.put(Spirits.WATER, new OverflowBehavior() {
            @Override
            public boolean canRun(int amount, boolean isOverdraw) {
                return amount >= waterSpiritAmount();
            }

            @Override
            public boolean multipleTargets() {
                return true;
            }

            private int waterSpiritAmount() {
                return SpiritLabeler.getSpiritsFor(Items.WATER_BUCKET).get(Spirits.WATER);
            }

            @Override
            public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                var bs = level.getBlockState(pos);
                if (bs.getBlock() instanceof SimpleWaterloggedBlock wb) {
                    if (!wb.canPlaceLiquid(level, pos, bs, Fluids.WATER)) return 0;
                    if (!simulate) wb.placeLiquid(level, pos, bs, Fluids.WATER.getSource(false));
                } else {
                    if (!bs.isAir() && !level.getFluidState(pos).is(Fluids.FLOWING_WATER)) return 0;
                    if (!simulate) level.setBlock(pos, Fluids.WATER.defaultFluidState().createLegacyBlock(), 11);
                }
                return waterSpiritAmount();
            }

            @Override
            public int overdrawBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                var bs = level.getFluidState(pos);
                if(bs.is(Fluids.FLOWING_WATER)){
                    if (!simulate) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                    return 1;
                }
                if (bs.is(Fluids.WATER)) {
                    if (!simulate) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                    return waterSpiritAmount();
                }
                return 0;
            }
        });
        overflowBehaviors.put(Spirits.PHLOGISTON, new OverflowBehavior() {

            private final ExplosionDamageCalculator damageCalc = new ExplosionDamageCalculator() {
                @Override
                public boolean shouldBlockExplode(@NotNull Explosion p_46094_, @NotNull BlockGetter getter,
                                                  @NotNull BlockPos pos, BlockState bs, float p_46098_) {
                    return isBlockExplodable(getter, pos, bs);
                }
            };

            private boolean isBlockExplodable(BlockGetter getter, BlockPos pos, BlockState bs) {
                if (bs.is(Blocks.AIR)) return false;
                if (bs.is(OtherverseBlocks.CHALK_LINE.get())) return false;
                if (getter.getBlockState(pos.above()).is(OtherverseBlocks.CHALK_LINE.get())) return false;
                if (!(getter instanceof Level level)) return true;
                var data = DiagramManager.getOrCreateLevelData(level);
                var tag = data.getPlacedItemTag(pos);
                return tag == null;
            }

            @Override
            public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                if (amount < 33) return 0;
                if (!isBlockExplodable(level, pos, level.getBlockState(pos))) return 0;
                var extra = (amount / 33);
                if (!simulate && level instanceof ServerLevel sl) {
                    var player = sl.getRandomPlayer();
                    sl.explode(player, null, damageCalc,
                            pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f,
                            4 * extra, false, Explosion.BlockInteraction.BREAK);
                }
                return (extra + 1) * 33;
            }
        });
        overflowBehaviors.put(Spirits.LIGHT, new OverflowBehavior() {
            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }

            @Override
            public boolean affectsIndividualBlocks() {
                return false;
            }

            @Override
            public int overflowEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!(e instanceof Player player)) return 0;
                if (!simulate) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, amount * 20));
                    player.addEffect(new MobEffectInstance(MobEffects.GLOWING, amount * 20));
                }
                return amount;
            }
        });
        overflowBehaviors.put(Spirits.DARK, new OverflowBehavior() {
            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }

            @Override
            public boolean affectsIndividualBlocks() {
                return false;
            }

            @Override
            public int overflowEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!(e instanceof Player player)) return 0;
                if (!simulate) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, amount * 20));
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, amount * 20));
                }
                return amount;
            }
        });
        overflowBehaviors.put(Spirits.PROTECTION, new OverflowBehavior() {
            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }
        });
        overflowBehaviors.put(Spirits.FOOD, new OverflowBehavior() {
            @Override
            public boolean affectsIndividualBlocks() {
                return false;
            }

            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }

            @Override
            public boolean multipleTargets() {
                return true;
            }

            @Override
            public int overflowEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!(e instanceof Player p)) return 0;
                amount = Math.min(20 - p.getFoodData().getFoodLevel(), amount);
                if (!simulate) p.getFoodData().eat(amount, 0);
                return amount;
            }

            @Override
            public int overdrawEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!(e instanceof Player p)) return 0;
                amount = Math.min(p.getFoodData().getFoodLevel(), amount);
                if (!simulate) p.getFoodData().eat(-amount, 1);
                return amount;
            }
        });
        overflowBehaviors.put(Spirits.DEATH, new OverflowBehavior() {
            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }
        });
        overflowBehaviors.put(Spirits.WAR, new OverflowBehavior() {
            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }
        });
        overflowBehaviors.put(Spirits.TECH, new OverflowBehavior() {
            @Override
            public boolean canRun(int amount, boolean isOverdraw) {
                return !isOverdraw && amount >= 3;
            }

            @Override
            public boolean affectsIndividualBlocks() {
                return false;
            }

            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }

            @Override
            public boolean multipleTargets() {
                return true;
            }

            @Override
            public int overflowEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!(e instanceof Mob mob)) return 0;
                if (!Objects.equals(mob.getPersistentData().getString("construct_type"), Spirits.TECH.label()))
                    return 0;
                var effect = mob.getEffect(MobEffects.MOVEMENT_SPEED);
                var amplifier = effect == null ? 0 : effect.getAmplifier() + 1;
                var cost = (int) Math.pow(3, amplifier + 1);
                if (amount < cost) return 0;
                if (!simulate) mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, amplifier));
                return cost;
            }
        });
        overflowBehaviors.put(Spirits.FLESH, new OverflowBehavior() {
            @Override
            public boolean canRun(int amount, boolean isOverdraw) {
                return !isOverdraw && amount >= 3;
            }

            @Override
            public boolean affectsIndividualBlocks() {
                return false;
            }

            @Override
            public ShrineShape getShape() {
                return ShrineShape.CENTERED;
            }

            @Override
            public boolean multipleTargets() {
                return true;
            }

            @Override
            public int overflowEntity(Entity e, int amount, Shrine shrine, boolean simulate) {
                if (!(e instanceof Mob mob)) return 0;
                if (!Objects.equals(mob.getPersistentData().getString("construct_type"), Spirits.FLESH.label()))
                    return 0;
                var effect = mob.getEffect(MobEffects.DAMAGE_BOOST);
                var amplifier = effect == null ? 0 : effect.getAmplifier() + 1;
                var cost = (int) Math.pow(3, amplifier + 1);
                if (amount < cost) return 0;
                if (!simulate) mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, amplifier));
                return cost;
            }
        });
        overflowBehaviors.put(Spirits.NATURE, new OverflowBehavior() {
            public boolean canRun(int amount, boolean isOverdraw) {
                return !isOverdraw && amount >= 33;
            }

            public ShrineShape getShape() {
                return ShrineShape.ABOVE;
            }

            public boolean multipleTargets() {
                return true;
            }

            @Override
            public int overflowBlock(BlockPos pos, Level level, int amount, boolean simulate) {
                if (!(level.getBlockState(pos).getBlock() instanceof BonemealableBlock)) return 0;
                if (!simulate) {
                    if (!BoneMealItem.applyBonemeal(ItemStack.EMPTY, level, pos, null)) return 0;
                    if (!level.isClientSide) level.levelEvent(1505, pos, 0);
                }
                return 33;
            }
        });
    }

    public static class Shrine {
        public final HashSet<BlockPos> hallowPositions;
        public ArrayList<BlockPos> targetPositions;
        public int checkIndex = 0;
        public final int totalCapacity;
        public final SpiritType st;
        public final Range range;
        public final Level level;
        public final boolean isCombined;
        public final HashSet<ChunkPos> chunkPositions = new HashSet<>();
        private int maxY, minY;
        private final HashSet<Shrine> directNeighbors;
        private HashSet<Shrine> network;
        public Shrine parentShrine;

        public Shrine(HashSet<BlockPos> hallowPositions, int totalCapacity, SpiritType st, Level level, boolean isCombined, HashSet<Shrine> initialNetwork) {
            network = initialNetwork;
            directNeighbors = new HashSet<>();
            this.isCombined = isCombined;
            this.level = level;
            this.hallowPositions = hallowPositions;
            this.totalCapacity = totalCapacity;
            this.st = st;
            range = isCombined ? getCombinedRange(network, totalCapacity) : getShrineRange(hallowPositions, totalCapacity, false);
            var data = DiagramManager.getOrCreateLevelData(level);
            for (var pos : hallowPositions) {
                var tag = data.getPlacedItemTag(pos);
                if (tag.contains("shrine")) continue;
                tag.putBoolean("shrine", true);
                data.putPlacedItemTag(pos, tag);
            }

            var shrinesByLevel = shrinesByPosition.computeIfAbsent(level, x -> new HashMap<>());
            for (var p : hallowPositions) {
                var prev = shrinesByLevel.get(p);
                if (prev != null) prev.cleanup();
                shrinesByLevel.put(p, this);
            }

            if (!overflowBehaviors.containsKey(st)) return;

            targetPositions = new ArrayList<>();
            var r = range.radius + 0.01f;
            var r2 = r * r;
            var shape = overflowBehaviors.get(st).getShape();
            var baseY = switch (shape) {
                case BELOW -> range.bottom();
                case ABOVE -> range.bottom() + range.height();
                case CENTERED -> (int) Math.ceil(range.center().y + range.height() / 2f);
            } - 1;
            var minx = Mth.floor(range.center.x - range.radius - 0.5f);
            var maxx = Mth.ceil(range.center.x + range.radius + 0.5f);
            var minz = Mth.floor(range.center.z - range.radius - 0.5f);
            var maxz = Mth.ceil(range.center.z + range.radius + 0.5f);
            for (var x = minx; x <= maxx; x++) {
                var dx = x + 0.5f - range.center.x;
                for (var z = minz; z <= maxz; z++) {
                    var dz = z + 0.5f - range.center.z;
                    if (dx * dx + dz * dz > r2) continue;
                    for (var dy = 0; dy < range.height; dy++) {
                        var bp = new BlockPos(x, baseY - dy, z);
                        targetPositions.add(bp);
                        chunkPositions.add(level.getChunkAt(bp).getPos());
                    }
                }
            }
            maxY = baseY;
            minY = baseY - range.height - 1;

            var possibleNeighbors = new HashSet<Shrine>();

            for (var chunkPos : chunkPositions) {
                var shrines = shrinesByChunk.computeIfAbsent(level, x -> new HashMap<>())
                        .computeIfAbsent(chunkPos, x -> new HashSet<>());
                possibleNeighbors.addAll(shrines);
                shrines.add(this);
            }

            LOGGER.debug("new shrine (" + (isCombined ? "combined" : "uncombined") + ") in " + chunkPositions.size() + " chunks");
            LOGGER.debug("possible neighbors: " + possibleNeighbors.size());

            if (!isCombined) {
                for (var otherShrine : possibleNeighbors) {
                    if (otherShrine.isCombined || otherShrine.st != st || !overlaps(otherShrine)) continue;
                    directNeighbors.add(otherShrine);
                    otherShrine.directNeighbors.add(this);
                }
                LOGGER.debug("direct neighbors: " + directNeighbors.size());
                recalculateNetwork(true);
            }

            if (Spirits.colorsByDye.containsValue(st)) {
                Collections.shuffle(targetPositions);
            } else {
                targetPositions.sort(Comparator.comparingDouble(
                        p -> range.center.distanceToSqr(new Vec3(p.getX() + 0.5f, p.getY() + 0.5f, p.getZ() + 0.5f))));
            }
        }

        private void recalculateNetwork(boolean thisIsPresent) {
            if (thisIsPresent) {
                setNetworkRecursive(new HashSet<>());
                LOGGER.debug("network size: " + network.size());
                refreshNetwork(network);
            } else {
                for (var direct : directNeighbors) {
                    direct.directNeighbors.remove(this);
                }
                var newNetworks = new HashSet<HashSet<Shrine>>();
                for (var direct : directNeighbors) {
                    if (newNetworks.contains(direct.network)) continue;
                    direct.setNetworkRecursive(new HashSet<>());
                    newNetworks.add(direct.network);
                }
                for (var newNetwork : newNetworks) {
                    refreshNetwork(newNetwork);
                }
            }
        }

        private static void refreshNetwork(HashSet<Shrine> network) {
            var avg = new Vec3(0, 0, 0);
            Shrine key = null;
            var totalCapacity = 0;
            for (var shrine : network) {
                if (shrine.parentShrine != null) {
                    shrine.parentShrine.cleanup();
                    shrine.parentShrine = null;
                }
                avg = avg.add(shrine.range.center.scale(1f / network.size()));
                key = shrine;
                totalCapacity += shrine.totalCapacity;
            }
            if (network.size() == 1) return;
            var x = avg.x;
            var z = avg.z;
            var keyX = key.range.center.x;
            var keyZ = key.range.center.z;
            var r = Math.sqrt(Math.pow(x - keyX, 2) + Math.pow(z - keyZ, 2));
            var startAngle = Math.atan2(keyZ - z, keyX - x);
            LOGGER.debug(x + ", " + z + ", " + keyX + ", " + keyZ + ", " + r);
            for (var i = 1; i < network.size(); i++) {
                var angle = startAngle + Math.PI * 2 / network.size();
                var targetX = x + Math.cos(angle) * r;
                var targetZ = z + Math.sin(angle) * r;
                LOGGER.debug("target: " + targetX + ", " + targetZ);
                var shrineFound = false;
                for (var shrine : network) {
                    var shrineX = shrine.range.center.x;
                    var shrineZ = shrine.range.center.z;
                    var dist = Math.sqrt(Math.pow(targetX - shrineX, 2) + Math.pow(targetZ - shrineZ, 2));
                    if (dist < r * 0.1f) {
                        shrineFound = true;
                        LOGGER.debug("shrine found :D");
                        break;
                    }
                }
                if (!shrineFound) {
                    LOGGER.debug("no shrine found D:");
                    return;
                }
            }
            var parentShrine = new Shrine(new HashSet<>(), totalCapacity, key.st, key.level, true, network);
            for (var shrine : network) {
                shrine.parentShrine = parentShrine;
            }
        }

        private void setNetworkRecursive(HashSet<Shrine> network) {
            this.network = network;
            network.add(this);
            for (var direct : directNeighbors) {
                if (!network.contains(direct)) direct.setNetworkRecursive(network);
            }
        }

        private double getSqrDistTo(Shrine otherShrine) {
            var dx = otherShrine.range.center.x - range.center.x;
            var dz = otherShrine.range.center.z - range.center.z;
            return dx * dx + dz * dz;
        }

        private boolean overlaps(Shrine otherShrine) {
            if (otherShrine.maxY < minY || otherShrine.minY > maxY) return false;
            var r = otherShrine.range.radius + range.radius;
            return getSqrDistTo(otherShrine) < r * r;
        }

        public boolean matches(Collection<BlockPos> blockPositions, SpiritType type, int totalCapacity) {
            if (type != st) return false;
            if (totalCapacity != this.totalCapacity) return false;
            if (blockPositions.size() != this.hallowPositions.size()) return false;
            for (var bp : blockPositions) {
                if (!hallowPositions.contains(bp)) return false;
            }
            return true;
        }

        public void markActive(Level level) {
            if(isCombined){
                for(var shrine : network){
                    if(shrine.isCombined) continue;
                    shrine.markActive(level);
                }
                return;
            }
            var data = DiagramManager.getOrCreateLevelData(level);
            for (var pos : hallowPositions) {
                var otherFocus = data.allBlockFoci.get(pos);
                if (otherFocus != null && level instanceof ServerLevel sl)
                    DiagramManager.markDiagramActive(sl, otherFocus.getDiagram());
            }
        }

        public boolean isInRange(Entity entity) {
            var center = range.center;
            var r = range.radius;
            var overflowBehavior = overflowBehaviors.get(st);
            if (overflowBehavior == null) return false;
            var topY = switch (overflowBehavior.getShape()) {
                case BELOW -> range.bottom();
                case ABOVE -> range.bottom() + range.height();
                case CENTERED -> (int) Math.ceil(range.center().y + range.height() / 2f);
            };
            var bottomY = topY - range.height;
            if (entity.getBoundingBox().minY > topY || entity.getBoundingBox().maxY < bottomY) return false;
            var relative = entity.position().subtract(center);
            return (relative.x * relative.x + relative.z * relative.z <= r * r);
        }

        public void cleanup() {
            for (var chunkPos : chunkPositions) {
                var set = shrinesByChunk.get(level).get(chunkPos);
                if (set == null) continue;
                set.remove(this);
            }
            var levelShrines = shrinesByPosition.get(level);
            for (var hallowPos : hallowPositions) {
                if (levelShrines.get(hallowPos) == this) levelShrines.remove(hallowPos);
            }
            if (!isCombined) {
                recalculateNetwork(false);
            }
        }

        public boolean tryDrain(int price, TransientDiagramData data) {

            if(isCombined){
                for(var shrine : network){
                    if(shrine.isCombined) continue;
                    if(shrine.tryDrain(price, data)) return true;
                }
                return false;
            }

            if (ShrineHelper.recalculateShrine(this)) return false;

            var drainPositions = new HashMap<BlockPos, Integer>();

            var remainingPrice = price;
            for (BlockPos sourcePos : hallowPositions) {
                if (remainingPrice > 0) {
                    var ht = data.getPlacedItemTag(sourcePos);
                    var count = ht.getInt("spirit_count");
                    count = Math.min(count, remainingPrice);
                    drainPositions.put(sourcePos, count);
                    remainingPrice -= count;
                }
            }

            if (remainingPrice > 0) {
                return false;
            }

            for (var drainPosition : drainPositions.entrySet()) {
                var shrineTag = data.getPlacedItemTag(drainPosition.getKey());
                shrineTag.putInt("spirit_count", shrineTag.getInt("spirit_count") - drainPosition.getValue());
                data.putPlacedItemTag(drainPosition.getKey(), shrineTag);
                var otherFocus = data.allBlockFoci.get(drainPosition.getKey());
                if (otherFocus == null || !(level instanceof ServerLevel sl)) continue;
                DiagramManager.markDiagramActive(sl, otherFocus.getDiagram());
            }

            return true;
        }
    }

    public static final HashMap<Level, HashMap<BlockPos, Shrine>> shrinesByPosition = new HashMap<>();
    public static final HashMap<Level, HashMap<ChunkPos, Set<Shrine>>> shrinesByChunk = new HashMap<>();

    public static void onStartup() {
        shrinesByPosition.clear();
        shrinesByChunk.clear();
    }

    public static boolean recalculateShrine(Shrine shrine) {
        if (shrine.isCombined) {
            for (var child : shrine.network) {
                if (child.isCombined) {
                    LOGGER.debug("COMBINED SHRINE IN NETWORK");
                    continue;
                }
                if (recalculateShrine(child)) return true;
            }
            return false;
        }
        LOGGER.debug("RECALCULATING");
        for (var pos : shrine.hallowPositions) {
            var newshrine = getShrine(shrine.level, pos, shrine.st);
            if (newshrine == shrine) {
                LOGGER.debug("SHRINE OK");
                return false;
            }
        }
        LOGGER.debug("CLEANING UP");
        shrine.cleanup();
        return true;
    }

    public static Shrine getShrine(Level level, BlockPos pos) {
        var shrine = shrinesByPosition.computeIfAbsent(level, x -> new HashMap<>()).get(pos);
        if (shrine != null) return shrine;
        var data = DiagramManager.getOrCreateLevelData(level);
        var tag = data.getPlacedItemTag(pos);
        if (tag == null || !tag.contains("shrine")) return null;
        return getShrineInternal(level, pos, Spirits.spiritsByLabel.get(tag.getString("spirit_type")), data);
    }

    public static Shrine getShrine(Level level, BlockPos pos, SpiritType st) {
        var data = DiagramManager.getOrCreateLevelData(level);
        var tag = data.getPlacedItemTag(pos);
        if (tag == null || !tag.contains("shrine")) return null;
        return getShrineInternal(level, pos, st, data);
    }

    private static Shrine getShrineInternal(Level level, BlockPos pos, SpiritType st, TransientDiagramData data) {
        var blockPositions = getAllHallows(pos, st, data);
        var shrinesByLevel = shrinesByPosition.computeIfAbsent(level, x -> new HashMap<>());
        var existingShrine = shrinesByLevel.get(pos);
        if (blockPositions.isEmpty()) {
            if (existingShrine != null) existingShrine.cleanup();
            return null;
        }
        var totalCapacity = 0;
        for (var h : blockPositions) {
            totalCapacity += data.getPlacedItemTag(h).getInt("capacity");
        }
        if (existingShrine != null && existingShrine.matches(blockPositions, st, totalCapacity)) {
            return existingShrine;
        }
        if (existingShrine != null) existingShrine.cleanup();
        return new Shrine(new HashSet<>(blockPositions), totalCapacity, st, level, false, null);
    }

    public static int onOverdrawOrOverflow(Level level, BlockPos focusPos, SpiritType spiritType, int amount, boolean overdraw, boolean simulate) {
        var overflowBehavior = overflowBehaviors.get(spiritType);
        if (overflowBehavior == null) {
            LOGGER.error("TRYING TO OVERDRAW ILLEGAL SPIRIT");
            return 0;
        }
        var shrine = getShrine(level, focusPos, spiritType);
        if (shrine == null) {
            return 0;
        }

        if (!overflowBehavior.canRun(amount, overdraw)) return 0;

        if (level instanceof ServerLevel && !simulate) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new OverflowMessage(DiagramManager.getDimensionHash(level), focusPos, spiritType, amount, overdraw));
        }

        var remainingAmount = amount;

        if(shrine.parentShrine != null) shrine = shrine.parentShrine;

        if (overflowBehavior.affectsIndividualBlocks()) {
            for (var i = 0; i < shrine.targetPositions.size(); i++) {
                var newIndex = (i + shrine.checkIndex) % shrine.targetPositions.size();
                if (overflowBehavior.multipleTargets() && !overflowBehavior.canRun(remainingAmount, overdraw)) {
                    if (remainingAmount < amount && !simulate) {
                        if (Set.of(Spirits.colorSpiritTypes).contains(spiritType)) {
                            shrine.checkIndex = (newIndex + 1 % shrine.targetPositions.size());
                        } else {
                            shrine.checkIndex = newIndex;
                            shrine.markActive(level);
                        }
                        if (spiritType == Spirits.WATER) {
                            level.playSound(null, focusPos, overdraw ? SoundEvents.BUCKET_FILL : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
                        }
                    }
                    break;
                }
                var pos = shrine.targetPositions.get(newIndex);
                if(level instanceof ServerLevel sl) {
                    var demesne = DemesnesManager.getData(sl, pos);
                    if(demesne != null && !demesne.hasSanction(DemesnesManager.DemesnePerk.SANCTION_BUILD, null)){
                        continue;
                    }
                }
                if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) continue;
                var transferred = overflowBehavior.affectBlock(pos, level, remainingAmount, overdraw, simulate);
                remainingAmount -= transferred;
                if (!overflowBehavior.multipleTargets() && transferred > 0) {
                    if (!simulate) {
                        shrine.checkIndex = newIndex;
                        shrine.markActive(level);
                    }
                    return transferred;
                }
            }
        } else {
            if (overdraw && (spiritType == Spirits.LIGHT || spiritType == Spirits.DARK)) return 0;
            if (simulate && !overflowBehavior.multipleTargets()) return amount;
            var center = shrine.range.center;
            var r = shrine.range.radius;
            var h = shrine.range.height / 2f;
            var bounds = new AABB(center.x - r, center.y - h, center.z - r,
                    center.x + r, center.y + h, center.z + r);
            var all = level.getEntities(null, bounds);
            for (var e : all) {
                var relative = e.position().subtract(center);
                if (relative.x * relative.x + relative.z * relative.z > r * r) continue;
                var spent = overflowBehavior.affectEntity(e, remainingAmount, overdraw, shrine, simulate);
                if (overflowBehavior.multipleTargets()) {
                    remainingAmount -= spent;
                    if (!overflowBehavior.canRun(remainingAmount, overdraw)) break;
                }
            }
            if (!simulate) shrine.markActive(level);
            return remainingAmount;
        }

        remainingAmount -= overflowBehavior.affectShrine(shrine, level, remainingAmount, overdraw, simulate);

        return amount - remainingAmount;
    }

    public static boolean isOverflowable(SpiritType spiritType) {
        return overflowBehaviors.containsKey(spiritType);
    }

    public record Range(Vec3 center, float radius, int height, int bottom) {
    }

    public static Collection<BlockPos> getAllHallows(BlockPos pos, String spiritType, TransientDiagramData data) {
        var set = new HashSet<BlockPos>();
        getHallowsRecursive(pos, set, data, spiritType);
        return set;
    }

    public static Collection<BlockPos> getAllHallows(BlockPos pos, SpiritType spiritType, TransientDiagramData data) {
        return getAllHallows(pos, spiritType.label(), data);
    }

    private static void getHallowsRecursive(BlockPos pos, HashSet<BlockPos> set, TransientDiagramData data, String label) {
        if (set.contains(pos)) return;
        var tag = data.getPlacedItemTag(pos);
        if (tag == null || !tag.getString("spirit_type").equals(label)) return;

        set.add(pos);
        for (var dir : Direction.values()) getHallowsRecursive(pos.relative(dir), set, data, label);
    }

    public static List<Shrine> getShrinesFor(Entity entity, SpiritType spiritType) {
        var level = entity.level;
        var mobPos = entity.blockPosition();
        var levelShrines = ShrineHelper.shrinesByChunk.get(level);
        if (levelShrines == null || levelShrines.isEmpty()) return List.of();

        var shrines = levelShrines.get(level.getChunkAt(mobPos).getPos());
        if (shrines == null || shrines.isEmpty()) return List.of();
        var list = new ArrayList<Shrine>();
        for (var shrine : shrines) {
            if (shrine.st != spiritType) {
                continue;
            }
            if (!shrine.isInRange(entity)) {
                continue;
            }
            list.add(shrine);
        }
        return list;
    }

    private static Range getCombinedRange(HashSet<Shrine> network, float capacity) {
        var positions = new ArrayList<BlockPos>();
        for (var shrine : network) {
            positions.addAll(shrine.hallowPositions);
        }
        return getShrineRange(positions, capacity, true);
    }

    public static Range getShrineRange(Collection<BlockPos> positions, float capacity, boolean isCombined) {
        var minX = Integer.MAX_VALUE;
        var minY = Integer.MAX_VALUE;
        var minZ = Integer.MAX_VALUE;

        var maxX = Integer.MIN_VALUE;
        var maxY = Integer.MIN_VALUE;
        var maxZ = Integer.MIN_VALUE;

        for (var pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());

            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        var center = new Vec3((minX + maxX + 1) / 2f, (minY + maxY + 1) / 2f, (minZ + maxZ + 1) / 2f);
        capacity = (float) Math.pow(capacity, 1.3f) * 0.25f;
        var shrineWidth = Math.max(maxX - minX, maxZ - minZ) + 1f;
        var shrineHeight = (maxY - minY) + 1f;
        if (maxY == minY && maxZ == minZ) shrineHeight = 0.1f;
        var aspectRatio = shrineWidth / shrineHeight;
        var pow = 3f;
        float r = (float) Math.pow(capacity * aspectRatio / Mth.PI, 1f / pow);
        if (isCombined) {
            var furthestDist = 0d;
            for (var pos : positions) {
                furthestDist = Math.max(furthestDist, (pos.distToCenterSqr(center.x, center.y, center.z)));
            }
            r = Math.min(r, (float) Math.sqrt(furthestDist)) + 0.5f;
            aspectRatio = (float) Math.pow(r, pow) * Mth.PI / capacity;
        }
        if (r < Mth.sqrt(2) / 2) {
            r = Mth.sqrt(2) / 2;
            aspectRatio = (float) Math.pow(r, pow) * Mth.PI / capacity;
        }
        var h = Math.max(1, Math.round(r / aspectRatio));
        r = Mth.sqrt(capacity / Mth.PI / h);
        return new Range(center, r, h, minY);
    }
}
