package com.shermansplanet.otherverse.ruins;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.potions.OtherversePotions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RuinsManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float LIGHTNING_MAX_DIST = 20;
    public static final float LIGHTNING_SPEED = 0.15f;
    public static HashMap<Player, Vec3> lightningPositions = new HashMap<>();
    public static HashMap<Player, Integer> ticksOnSand = new HashMap<>();

    public static float skyMultiplier = 1f;
    private static float skyMultiplierRaw = 1f;
    private static int soundTicks = 100;

    private static final HashSet<Block> burnBlocks = new HashSet<>(
            Arrays.stream(new Block[]{Blocks.RED_SAND, Blocks.ORANGE_TERRACOTTA, Blocks.MAGMA_BLOCK}).toList());

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(biome -> {
            var path = biome.location().getPath();
            if (path.equals("ruins_ecstasy")) {
                var vel = player.getDeltaMovement().scale(10);
                player.push(vel.x, 1.5f, vel.z);
            }
        });
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!event.player.level().dimension().location().getPath().equals("ruins")) {
            skyMultiplier = 1;
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            event.player.level().getBiome(event.player.blockPosition()).unwrapKey().ifPresent(biome -> {
                var path = biome.location().getPath();
                if (path.equals("ruins_ecstasy")) {
                    skyMultiplierRaw = 1.25f;
                } else if (!path.equals("ruins_shock")) {
                    skyMultiplierRaw = 1f;
                }
            });
            skyMultiplier = skyMultiplierRaw * 0.2f + skyMultiplier * 0.8f;
            event.player.level().setRainLevel(1);
            return;
        }
        var level = player.serverLevel();
        level.getBiome(player.blockPosition()).unwrapKey().ifPresent(biome -> {
            var path = biome.location().getPath();
            if (!path.equals("ruins_shock") || !lightningPositions.containsKey(player)) {
                lightningPositions.put(player, player.position().add(0, LIGHTNING_MAX_DIST, 0));
                if (level.getGameTime() % 10 == 0) {
                    OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                            new ShockLightningMessage(LIGHTNING_MAX_DIST / 2));
                }
            }

            switch (path) {
                case "ruins_shock" -> {
                    var pos = lightningPositions.get(player);
                    var dir = player.position().subtract(pos);
                    float lightningDistance = (float) dir.length();
                    if (lightningDistance > LIGHTNING_MAX_DIST) {
                        pos = player.position().subtract(dir.normalize().scale(LIGHTNING_MAX_DIST));
                    } else {
                        pos = pos.add(dir.normalize().scale(LIGHTNING_SPEED));
                    }
                    lightningPositions.put(player, pos);

                    var r = level.random;
                    if (r.nextFloat() > 0.98f) {
                        LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
                        if (lightningbolt != null) {
                            var target = player.position().add(player.getLookAngle().scale(lightningDistance));
                            target = target.add(new Vec3(r.nextFloat() - 0.5f, 0, r.nextFloat() - 0.5f)
                                    .normalize().scale(r.nextFloat() * lightningDistance * 0.2f));
                            var blockTarget = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(target));
                            lightningbolt.moveTo(Vec3.atBottomCenterOf(blockTarget));
                            lightningbolt.setVisualOnly(false);
                            var special = r.nextFloat();
                            if (special < 0.25f || level.isEmptyBlock(blockTarget.below(3))) {
                                level.explode(lightningbolt, blockTarget.getX(), blockTarget.getY(), blockTarget.getZ(),
                                        2, Level.ExplosionInteraction.BLOCK);
                                for (var dx = -1; dx <= 1; dx++) {
                                    for (var dy = -2; dy <= 0; dy++) {
                                        for (var dz = -1; dz <= 1; dz++) {
                                            var offsetPos = blockTarget.offset(dx, dy, dz);
                                            if (level.getBlockState(offsetPos).is(Blocks.CYAN_CONCRETE_POWDER)) {
                                                level.scheduleTick(offsetPos, Blocks.CYAN_CONCRETE_POWDER, 0);
                                            }
                                        }
                                    }
                                }
                            } else if (special > 0.75f) {
                                var raise = r.nextInt(1, 6);
                                lightningbolt.moveTo(Vec3.atBottomCenterOf(blockTarget.above(raise)));
                                ShockSpireFeature.spawnSpire(level, raise, blockTarget, r, 1);
                                for (int i = 0; i < 20; i++) {
                                    level.sendParticles(ParticleTypes.POOF,
                                            blockTarget.getX() - 2 + r.nextFloat() * 5,
                                            blockTarget.getY() + r.nextFloat() * (raise + 1),
                                            blockTarget.getZ() - 2 + r.nextFloat() * 5,
                                            1, 0, 0, 0, 0.15);
                                }
                            }
                            level.addFreshEntity(lightningbolt);
                        }
                    }

                    if (level.getGameTime() % 10 == 0) {
                        OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                                new ShockLightningMessage(lightningDistance));
                    }
                }
                case "ruins_misery" -> {
                    if (player.hasEffect(OtherversePotions.HEAVINESS_EFFECT.get())) return;
                    if (!level.isWaterAt(player.blockPosition())) return;
                    player.addEffect(new MobEffectInstance(OtherversePotions.HEAVINESS_EFFECT.get(), 100));
                }
                case "ruins_disgust" -> {
                    if (player.hasEffect(MobEffects.CONFUSION)) return;
                    if (!level.isWaterAt(player.blockPosition())) return;
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 500));
                }
                case "ruins_anger" -> {
                    if (!player.onGround() || EnchantmentHelper.hasFrostWalker(player)) return;
                    var sandTicks = ticksOnSand.getOrDefault(player, 0);
                    if (!burnBlocks.contains(level.getBlockState(player.blockPosition().below()).getBlock())) {
                        if (sandTicks > 0) sandTicks--;
                    } else if (sandTicks < 40) {
                        sandTicks++;
                    } else {
                        player.hurt(level.damageSources().hotFloor(), 1.0F);
                    }
                    ticksOnSand.put(player, sandTicks);
                }
                case "ruins_fear" -> {
                    soundTicks--;
                    if (soundTicks <= 0) {
                        var r = level.random;
                        soundTicks = r.nextInt(80, 400);
                        var dir = new Vec3(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
                        var pos = player.position().add(dir.normalize().scale(r.nextInt(8, 24)));
                        level.playSound(null, pos.x, pos.y, pos.z,
                                level.random.nextBoolean() ? SoundEvents.POLAR_BEAR_AMBIENT : SoundEvents.POLAR_BEAR_WARNING,
                                SoundSource.HOSTILE, 1, 1);
                    }
                }
                case "ruins_vigil" -> {
                    var playerPos = player.position();
                    var playerVel = player.getDeltaMovement().scale(20);
                    var gravity = 9.8f;
                    for (var h = 0; h < 64; h++) {
                        var secondsToFall = Mth.sqrt(2 * h / gravity);
                        var projectedPlayerPos = playerPos.add(playerVel.scale(secondsToFall));
                        var pos = new BlockPos(Mth.floor(projectedPlayerPos.x), Mth.floor(projectedPlayerPos.y) + h + 1, Mth.floor(projectedPlayerPos.z));
                        var bs = level.getBlockState(pos);
                        if (bs.is(Blocks.ANVIL) && level.getBlockState(pos.above()).is(Blocks.CHAIN)) {
                            level.destroyBlock(pos.above(), true);
                        } else if (bs.is(Blocks.POINTED_DRIPSTONE)) {
                            for (var i = 0; i < 8; i++) {
                                pos = pos.above();
                                if (!level.getBlockState(pos).is(Blocks.POINTED_DRIPSTONE)) break;
                            }
                            if (level.getBlockState(pos).is(Blocks.DRIPSTONE_BLOCK)) {
                                level.destroyBlock(pos, true);
                            }
                        }
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (!event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || event.getEntity().level().dimension() != ModDimensions.RUINS_KEY) {
            return;
        }
        var pos = event.getEntity().position();
        event.getEntity().setPos(pos.x, 100, pos.y);
        event.getEntity().changeDimension(sl.getServer().overworld(), new NoReturnTeleporter());
    }

    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                || event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.REINFORCED_DEEPSLATE
                || !event.getItemStack().is(Items.FLINT_AND_STEEL))
            return;

        ServerLevel level = ((ServerLevel) event.getLevel()).getServer().getLevel(
                event.getEntity().level().dimension() == Level.OVERWORLD ? ModDimensions.RUINS_KEY : Level.OVERWORLD);
        if (level == null) {
            System.out.println("Ruins not found!");
            return;
        }
        event.setCanceled(true);
        event.getEntity().changeDimension(level, new NoReturnTeleporter());
    }

    public static void handleShockUpdate(ShockLightningMessage message, Supplier<NetworkEvent.Context> ctx) {
        skyMultiplierRaw = message.distance / LIGHTNING_MAX_DIST;
    }

    public static class NoReturnTeleporter implements ITeleporter {
    }
}
