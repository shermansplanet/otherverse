package com.shermansplanet.otherverse.ruins;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.*;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.others.Banshee;
import com.shermansplanet.otherverse.others.Buzzed;
import com.shermansplanet.otherverse.others.Fury;
import com.shermansplanet.otherverse.others.Guest;
import com.shermansplanet.otherverse.potions.OtherversePotions;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.Spirits;
import com.shermansplanet.otherverse.sympathy.SpindleItem;
import com.shermansplanet.otherverse.sympathy.SympathyManager;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RuinsManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float LIGHTNING_MAX_DIST = 20;
    public static final float LIGHTNING_SPEED = 0.15f;
    public static HashMap<Player, Vec3> lightningPositions = new HashMap<>();
    public static HashMap<Player, Vec3> priorPositions = new HashMap<>();
    public static HashMap<Player, Integer> ticksOnSand = new HashMap<>();
    public static HashMap<Player, HashMap<Mob, Integer>> claimAmounts = new HashMap<>();
    public static HashSet<Player> hasRuinsEffect = new HashSet<>();

    public static float skyMultiplier = 1f;
    private static float skyMultiplierRaw = 1f;
    private static int soundTicks = 100;
    private static long lastGuestFed = 0;

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
    public static void guestAnger(EnderManAngerEvent event) {
        if (event.getEntity() instanceof Guest g) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void guestPickup(EntityMobGriefingEvent event) {
        if (event.getEntity() instanceof Guest g) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static final SoundEvent[] fearSounds = new SoundEvent[]{
            SoundEvents.POLAR_BEAR_AMBIENT,
            SoundEvents.POLAR_BEAR_WARNING,
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.GHAST_WARN,
            SoundEvents.WARDEN_NEARBY_CLOSEST,
            SoundEvents.WARDEN_NEARBY_CLOSER,
            SoundEvents.WARDEN_HEARTBEAT,
            SoundEvents.WARDEN_STEP,
    };

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
        var hadEffect = hasRuinsEffect.contains(event.player);
        var hasEffect = event.player.hasEffect(OtherversePotions.RUINS_BOUND.get());
        if (hadEffect && !hasEffect) {
            hasRuinsEffect.remove(player);
            returnFromRuins(player);
            return;
        } else if (!hadEffect && hasEffect) {
            hasRuinsEffect.add(player);
        }
        var level = player.serverLevel();
        level.getBiome(player.blockPosition()).unwrapKey().ifPresent(biome -> {
            var path = biome.location().getPath();
            if (!path.equals("ruins_shock")) {
                if (level.getGameTime() % 10 == 0) {
                    OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                            new ShockLightningMessage(LIGHTNING_MAX_DIST / 2));
                    lightningPositions.remove(player);
                }
            }

            switch (path) {
                case "ruins_shock" -> {
                    var isImmune = isImmune(player, "otherverse:ruins_shock");
                    float lightningDistance = LIGHTNING_MAX_DIST / 2;
                    if (!isImmune) {
                        if (!lightningPositions.containsKey(player)) {
                            lightningPositions.put(player, player.position().add(0, LIGHTNING_MAX_DIST, 0));
                        }
                        var pos = lightningPositions.get(player);
                        var dir = player.position().subtract(pos);
                        lightningDistance = (float) dir.length();
                        if (lightningDistance > LIGHTNING_MAX_DIST) {
                            pos = player.position().subtract(dir.normalize().scale(LIGHTNING_MAX_DIST));
                        } else {
                            pos = pos.add(dir.normalize().scale(LIGHTNING_SPEED));
                        }
                        lightningPositions.put(player, pos);
                    }

                    var r = level.random;
                    if (r.nextFloat() > 0.99f) {
                        var shouldStrikeAnyway = player.isHolding(Items.LIGHTNING_ROD) && FamiliarManager.hasFamiliarType(player, Otherverse.SNUFFER.get());
                        if (isImmune && !shouldStrikeAnyway) return;
                        LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
                        if (lightningbolt != null) {
                            var target = player.position().add(player.getLookAngle().scale(lightningDistance * 2));
                            target = target.add(new Vec3(r.nextFloat() - 0.5f, 0, r.nextFloat() - 0.5f)
                                    .normalize().scale(r.nextFloat() * lightningDistance * 0.4f));
                            if (shouldStrikeAnyway) {
                                target = player.position();
                            }
                            var blockTarget = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(target));
                            lightningbolt.moveTo(Vec3.atBottomCenterOf(blockTarget));
                            lightningbolt.setVisualOnly(false);
                            if (!isImmune) {
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
                    if (isImmune(player, "otherverse:ruins_misery")) return;
                    player.addEffect(new MobEffectInstance(OtherversePotions.HEAVINESS_EFFECT.get(), 100));
                }
                case "ruins_disgust" -> {
                    if (player.hasEffect(MobEffects.CONFUSION)) return;
                    if (!level.isWaterAt(player.blockPosition())) return;
                    if (isImmune(player, "otherverse:ruins_disgust")) return;
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 500));
                }
                case "ruins_anger" -> {
                    if (!player.onGround() || EnchantmentHelper.hasFrostWalker(player)) return;
                    if (isImmune(player, "otherverse:ruins_anger")) return;
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
                        soundTicks = r.nextInt(80, 1200);
                        if (isImmune(player, "otherverse:ruins_fear")) return;
                        var dir = new Vec3(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
                        var pos = player.position().add(dir.normalize().scale(r.nextInt(2, 16)));
                        level.playSound(null, pos.x, pos.y, pos.z,
                                fearSounds[r.nextInt(fearSounds.length)],
                                SoundSource.HOSTILE, 1, 1);
                    }
                }
                case "ruins_vigil" -> {
                    var playerPos = player.position();
                    var priorPos = priorPositions.getOrDefault(player, playerPos);
                    priorPositions.put(player, playerPos);
                    var playerVel = playerPos.subtract(priorPos);
                    if (playerVel.lengthSqr() > 10) return;
                    if (isImmune(player, "otherverse:ruins_vigil")) return;
                    for (var h = 0; h < 64; h++) {
                        var ticksToFall = Mth.sqrt(2 * h / 0.04f);
                        var projectedPlayerPos = playerPos.add(playerVel.scale(ticksToFall));
                        var pos = new BlockPos(Mth.floor(projectedPlayerPos.x), Mth.floor(playerPos.y) + h + 1, Mth.floor(projectedPlayerPos.z));
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
                case "ruins_ecstasy" -> {
                    if (isImmune(player, "otherverse:ruins_ecstasy")) {
                        if (player.isCrouching()) {
                            player.removeEffect(MobEffects.SLOW_FALLING);
                        } else {
                            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40));
                        }
                    }
                    if (level.getGameTime() % 20 != 0) return;
                    var range = Math.min(player.experienceLevel, 33);
                    for (var e : level.getEntitiesOfClass(Buzzed.class, player.getBoundingBox().inflate(range))) {
                        e.setAggressive(true);
                        e.setTarget(player);
                    }
                }
            }
        });
    }

    private static boolean isImmune(ServerPlayer player, String biomeName) {
        switch (biomeName) {
            case "otherverse:ruins_misery" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.TYPHLOTIC_JELLYFISH.get())) return true;
            }
            case "otherverse:ruins_shock" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.SNUFFER.get())) return true;
            }
            case "otherverse:ruins_fear" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.TYPHLOTIC_SHARK.get())) return true;
            }
            case "otherverse:ruins_trust" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.GUEST.get())) return true;
            }
            case "otherverse:ruins_ecstasy" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.BUZZED.get())) return true;
            }
            case "otherverse:ruins_anger" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.FURY.get())) return true;
            }
            case "otherverse:ruins_vigil" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.BANSHEE.get())) return true;
            }
            case "otherverse:ruins_disgust" -> {
                if (FamiliarManager.hasFamiliarType(player, Otherverse.TYPHLOTIC_ZOMBIE.get())) return true;
            }
        }
        var data = DemesnesManager.getData(player);
        if (data == null) return false;
        return data.biome.equals(biomeName);
    }

    @SubscribeEvent
    public static void setClaim(LivingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof ClaimArrow)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!(event.getEntity() instanceof Mob target)) return;
        if (!claimAmounts.containsKey(sp)) claimAmounts.put(sp, new HashMap<>());
        var claim = claimAmounts.get(sp);
        var claimAmount = claim.getOrDefault(target, 0) + 1;
        claim.put(target, claimAmount);
        var offhandItem = sp.getOffhandItem();
        if (offhandItem.getItem() instanceof SpindleItem || offhandItem.getItem() == OtherverseItems.SPINDLE_BLOODY.get()) {
            var newstack = new ItemStack(OtherverseItems.SPINDLE_BLOODY.get(), offhandItem.getCount());
            newstack.getOrCreateTag().putString("sympathy_target", SympathyManager.getKeyFor(target));
            newstack.getOrCreateTag().putString("sympathy_label", target.getType().getDescriptionId());
            sp.setItemSlot(EquipmentSlot.OFFHAND, newstack);
            offhandItem = newstack;
        }
        if (offhandItem.getItem() != OtherverseItems.SPINDLE_BLOODY.get()) return;
        var tag = offhandItem.getOrCreateTag();
        if (!tag.getString("sympathy_target").equals(SympathyManager.getKeyFor(target)))
            return;
        if (claimAmount < 3) return;
        if (!tag.contains("Enchantments", 9)) {
            tag.put("Enchantments", new ListTag());
            ListTag listtag = tag.getList("Enchantments", 10);
            CompoundTag fakeEnchantment = new CompoundTag();
            fakeEnchantment.putString("id", "Claim");
            fakeEnchantment.putInt("lvl", 1);
            listtag.add(fakeEnchantment);
        }
        tag.putBoolean("can_summon", true);
        if (claimAmount < 9) return;
        tag.putBoolean("can_bind_remotely", true);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var e = event.getSource().getEntity();
        var claim = claimAmounts.get(sp);
        if (claim != null && event.getSource().getEntity() instanceof Mob mob) {
            claim.remove(mob);
        }
        if (e instanceof Guest || e instanceof Fury || e instanceof Banshee) {
            SelfManager.changeSelf(sp, -(int) (event.getAmount() * SelfManager.getSelfCoeff(sp)));
        }
        if (!event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || sp.level().dimension() != ModDimensions.RUINS_KEY) {
            return;
        }
        returnFromRuins(sp);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ArmorItem armor) || armor.getMaterial() != OtherverseItems.OtherverseArmorMaterials.RUINS)
            return;
        event.getToolTip().add(1, Component.literal("See the Ruins book for more info on this armor's special abilities.").withStyle(Style.EMPTY.withItalic(true).withColor(0x707070)));
    }

    @SubscribeEvent
    public static void getArrow(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        for (var item : player.getInventory().items) {
            if (item.getItem() == OtherverseItems.CLAIM_ARROW.get()) {
                event.setProjectileItemStack(item);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onExplode(ExplosionEvent event) {
        var center = event.getExplosion().getPosition();
        var extent = new Vec3(64, 64, 64);
        var bounds = new AABB(center.subtract(extent), center.add(extent));
        for (var banshee : event.getLevel().getEntitiesOfClass(Banshee.class, bounds)) {
            ((Banshee) banshee).alert();
        }
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().isEdible()) {
            if (!player.level().getBiome(player.blockPosition()).unwrapKey().get().location().getPath().equals("ruins_trust"))
                return;
            if (player.level().getGameTime() - lastGuestFed > 20 * 60) {
                player.displayClientMessage(Component.literal("By not sharing food, you have violated Hospitality."), true);
                breakHospitality(player);
            }
        }
    }

    @SubscribeEvent
    public static void onEat(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (!player.level().getBiome(player.blockPosition()).unwrapKey().get().location().getPath().equals("ruins_trust"))
                return;
            var block = event.getState().getBlock();
            if (block == Blocks.STRIPPED_CHERRY_WOOD || block == Blocks.CHERRY_LEAVES) {
                player.displayClientMessage(Component.literal("By felling a tree, you have violated Hospitality."), true);
                breakHospitality(player);
            } else if (Arrays.stream(TrustTreeFeature.flowers).anyMatch(b -> b.is(block))) {
                player.displayClientMessage(Component.literal("By picking a flower, you have violated Hospitality."), true);
                breakHospitality(player);
            }
        }
    }

    public static void breakHospitality(ServerPlayer player) {
        for (var e : player.level().getEntities(player, player.getBoundingBox().inflate(64))) {
            if (e instanceof Guest guest) {
                guest.setAggressive(true);
                guest.setTarget(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var coeff = 1f - SelfManager.getSelfCoeff(player);
        var xp = (int) (player.totalExperience * coeff);
        if (xp <= 0) return;
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        var spreader = SculkSpreader.createLevelSpreader();
        var pos = player.blockPosition();
        spreader.addCursors(pos, xp);
        while (!spreader.getCursors().isEmpty()) {
            spreader.updateCursors(player.level(), pos, player.getRandom(), true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl) || !(event.getEntity() instanceof ServerPlayer player))
            return;
        var data = DiagramManager.getOrCreateLevelData(sl);
        var focus = DiagramManager.getFocusInBoundingBox(data, player.getBoundingBox());
        if (focus == null) return;
        var blockBelow = sl.getBlockState(focus.getPos().below());
        if (!blockBelow.is(Blocks.SCULK) && !blockBelow.is(Blocks.REINFORCED_DEEPSLATE)) return;
        var xpCost = 0;
        for (var influence : focus.getDiagram().influences.entrySet()) {
            if (!influence.getValue().equals(focus.getPos())) continue;
            var influenceState = sl.getBlockState(influence.getKey());
            if (influenceState.is(Blocks.SOUL_FIRE)) xpCost += 10; // 1 minute
            if (influenceState.is(Blocks.SOUL_TORCH)) xpCost += 100; // 10 minutes
            if (influenceState.is(Blocks.SOUL_LANTERN)) xpCost += 600; // 1 hour
        }
        if (blockBelow.is(Blocks.REINFORCED_DEEPSLATE)) {
            if (xpCost <= 0) return;
        } else {
            xpCost = Math.min(xpCost, player.totalExperience);
            if (xpCost <= 0) return;
            player.giveExperiencePoints(-xpCost);
            var seconds = xpCost * 6;
            var effect = new MobEffectInstance(OtherversePotions.RUINS_BOUND.get(), seconds * 20, 0, false, false, true);
            effect.setCurativeItems(List.of(OtherverseItems.ESCAPE_ROPE.get().getDefaultInstance()));
            player.addEffect(effect);
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        sendToRuins(player);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        tryUseEye(event.getEntity(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        tryUseEye(event.getEntity(), event.getItemStack());
    }

    private static void tryUseEye(Player player, ItemStack itemStack) {
        var eyeItem = OtherverseItems.EYE_OF_THE_EYELESS.get();
        if (!itemStack.is(eyeItem) || !(player.level() instanceof ServerLevel sl) || player.getCooldowns().isOnCooldown(eyeItem))
            return;
        new Thread(() -> {
            Registry<Structure> registry = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
            HolderSet<Structure> holderset = HolderSet.direct(registry.getHolderOrThrow(BuiltinStructures.ANCIENT_CITY));
            Pair<BlockPos, Holder<Structure>> result = sl.getChunkSource().getGenerator().findNearestMapStructure(sl, holderset, player.blockPosition(), 100, false);
            if (result == null || result.getFirst() == null) {
                player.displayClientMessage(Component.literal("Could not find ancient city."), false);
            } else {
                var r = RandomSource.create(sl.getGameTime());
                var pos = player.position().add(0, 1, 0);
                var dir = result.getFirst().getCenter().subtract(pos).with(Direction.Axis.Y, 0);
                var isClose = dir.lengthSqr() < 32 * 32;
                dir = dir.normalize().scale(3);
                for (var i = 0; i < 12; i++) {
                    if (isClose) {
                        dir = new Vec3(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f).scale(3);
                    }
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.x + r.nextFloat() * 0.8f - 0.4f,
                            pos.y + r.nextFloat() * 0.5f - 0.25f,
                            pos.z + r.nextFloat() * 0.8f - 0.4f,
                            0, dir.x, dir.y, dir.z, 0.15f);
                }
            }
            player.getCooldowns().addCooldown(eyeItem, 200);
        }).start();
    }

    private static void sendToRuins(ServerPlayer player) {
        if (isInRuins(player)) return;
        var key = DiagramManager.getDimensionHash(player.serverLevel());
        player.getPersistentData().putInt("dimension_before_ruins", key);
        ServerLevel level = player.getServer().getLevel(ModDimensions.RUINS_KEY);
        player.changeDimension(level, new NoReturnTeleporter());
        Otherverse.ADVANCEMENTS.trigger(player, "ruins");
    }

    public static void returnFromRuins(ServerPlayer player) {
        player.removeEffect(OtherversePotions.RUINS_BOUND.get());
        var dest = DiagramManager.levelFromHash(player.serverLevel(), player.getPersistentData().getInt("dimension_before_ruins"));
        if (dest == null) dest = player.server.overworld();
        player.changeDimension(dest, new NoReturnTeleporter());
    }

    public static void handleShockUpdate(ShockLightningMessage message, Supplier<NetworkEvent.Context> ctx) {
        skyMultiplierRaw = message.distance / LIGHTNING_MAX_DIST;
    }

    public static Vec3 getClosestScent(Level level, Vec3 position) {
        if (!level.dimension().location().getPath().equals("ruins")) return null;
        Vec3 closestPos = null;
        var closestDist = Double.MAX_VALUE;
        for (var pos : lightningPositions.values()) {
            var dist = position.distanceToSqr(pos);
            if (dist < closestDist) {
                closestPos = pos;
                closestDist = dist;
            }
        }
        return closestPos;
    }

    public static void onFedGuest(long time) {
        lastGuestFed = time;
    }

    public static boolean isInRuins(ServerPlayer player) {
        return player.level().dimension().location().getPath().equals("ruins");
    }

    public static boolean tryEnchantArmor(ServerLevel level, ChalkCircle circle, Diagram diagram) {
        if (circle.isEmpty() || circle.getProcess() != null) return false;
        var stack = circle.getItem();
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getMaterial() != OtherverseItems.OtherverseArmorMaterials.RUINS)
            return false;
        if (!level.getBiome(circle.getPos()).unwrapKey().get().location().getPath().equals("ruins_chaos")) return false;
        var spiritsToDrain = 99;
        var procAmounts = new HashMap<IFocus, Integer>();
        var selfAmounts = new HashMap<IFocus, Integer>();
        var selfNeeded = 21;
        for (var influence : diagram.influences.entrySet()) {
            if (!circle.getPos().equals(influence.getValue())) continue;
            var focus = DiagramManager.getFocus(influence.getKey(), level);
            if (focus == null) continue;
            if (spiritsToDrain > 0) {
                var drained = focus.drainHallow(Spirits.PROTECTION, spiritsToDrain, false, true);
                if (drained > 0) {
                    spiritsToDrain -= drained;
                    procAmounts.put(focus, drained);
                    continue;
                }
            }
            if (selfNeeded > 0) {
                var item = focus.getItem();
                if (item.is(OtherverseItems.SELF.get())) {
                    var self = Math.min(selfNeeded, item.getCount());
                    if (self > 0) {
                        selfNeeded -= self;
                        selfAmounts.put(focus, self);
                    }
                }
            }
        }
        if (spiritsToDrain > 0 || selfNeeded > 0) return false;
        new ArmorEnchantProcess(circle, 60, procAmounts, selfAmounts);
        return true;
    }

    public static boolean tryTeleportToRuins(ServerLevel level, BlockFocus focus, Diagram diagram) {
        if (level.dimension().location().getPath().equals("ruins")) return false;
        var pos = focus.getPos();
        if (!level.getBlockState(pos.below()).is(Blocks.SCULK)) return false;
        var eyeCount = 0;
        for (var influence : diagram.influences.entrySet()) {
            if (!pos.equals(influence.getValue())) continue;
            if (!(level.getBlockEntity(influence.getKey()) instanceof ChalkCircle cc)) continue;
            if (!cc.getItem().is(OtherverseItems.EYE_OF_THE_EYELESS.get())) continue;
            if (!level.getBlockState(influence.getKey().below()).is(Blocks.SCULK)) return false;
            eyeCount++;
            if (eyeCount == 12) {
                for (var e : level.getEntities(null, new AABB(pos))) {
                    if (e instanceof ServerPlayer player) {
                        sendToRuins(player);
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    private static class ArmorEnchantProcess extends DiagramProcess {
        private final HashMap<IFocus, Integer> procAmounts;
        private final HashMap<IFocus, Integer> selfAmounts;

        public ArmorEnchantProcess(ChalkCircle sink, int duration, HashMap<IFocus, Integer> procAmounts, HashMap<IFocus, Integer> selfAmounts) {
            super(sink, sink, duration);
            this.procAmounts = procAmounts;
            this.selfAmounts = selfAmounts;
        }

        @Override
        public void tick() {
            super.tick();
            var level = (ServerLevel) sink.getFocusLevel();
            var r = level.random;
            var pos = sink.getPos();
            if (remainingDuration % 8 == 0) {
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                        pos.getX() + r.nextFloat() * 3f - 1f,
                        pos.getY(),
                        pos.getZ() + r.nextFloat() * 3f - 1f,
                        0, 0, 0.2f, 0, 0.15f);
            }
            if (remainingDuration > 0) {
                return;
            }
            abandon();
            var stack = sink.getItem();
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getMaterial() != OtherverseItems.OtherverseArmorMaterials.RUINS) {
                abandon();
                return;
            }
            for (var proc : procAmounts.entrySet()) {
                var drained = proc.getKey().drainHallow(Spirits.PROTECTION, proc.getValue(), true, false);
                if (drained != proc.getValue()) {
                    abandon();
                    return;
                }
            }
            for (var self : selfAmounts.entrySet()) {
                var item = self.getKey().getItem();
                if (!item.is(OtherverseItems.SELF.get())) {
                    abandon();
                    return;
                }
                item.shrink(self.getValue());
                if (item.isEmpty()) self.getKey().drainItem();
            }
            stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 5);
            stack.enchant(Enchantments.MENDING, 1);
            ((ChalkCircle) sink).markUpdated();
            for (var i = 0; i < 8; i++) {
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                        pos.getX() + r.nextFloat() * 3f - 1f,
                        pos.getY(),
                        pos.getZ() + r.nextFloat() * 3f - 1f,
                        0, 0, 0.2f, 0, 0.15f);
            }
        }
    }

    public static class NoReturnTeleporter implements ITeleporter {
        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            var pos = entity.position();
            var coeff = entity.level().dimensionType().coordinateScale() / destWorld.dimensionType().coordinateScale();
            var x = (int) (pos.x * coeff);
            var y = 100;
            var z = (int) (pos.z * coeff);
            var r = destWorld.getRandom();
            var diff = new Vec3(r.nextFloat() - 0.5f, 0, r.nextFloat() - 0.5f).normalize();
            var dx = (int) Math.round(diff.x * 10);
            var dz = (int) Math.round(diff.z * 10);
            for (var i = 0; i < 10; i++) {
                destWorld.getPoiManager().ensureLoadedAndValid(destWorld, new BlockPos(x, y, z), 16);
                var airBlocks = 0;
                for (y = destWorld.getMaxBuildHeight(); y >= destWorld.getMinBuildHeight(); y--) {
                    var blockState = destWorld.getBlockState(new BlockPos(x, y, z));
                    if (blockState.is(Blocks.BEDROCK) || blockState.is(Blocks.LAVA)) {
                        airBlocks = 0;
                        continue;
                    }
                    if (airBlocks >= 2 && !blockState.isAir()) {
                        return new PortalInfo(new Vec3(x + 0.5f, y + 1, z + 0.5f), Vec3.ZERO, entity.getYRot(), entity.getXRot());
                    }
                    if (blockState.isAir()) airBlocks++;
                }
                x += dx;
                z += dz;
            }
            return new PortalInfo(new Vec3(x + 0.5f, destWorld.getMaxBuildHeight() + 1, z + 0.5f), Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }

        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
            return repositionEntity.apply(false);
        }
    }
}
