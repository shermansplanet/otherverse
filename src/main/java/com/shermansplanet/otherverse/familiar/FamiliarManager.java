package com.shermansplanet.otherverse.familiar;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.ItemOrEntityType;
import com.shermansplanet.otherverse.Keybindings;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.binding.*;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.SelfManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.potions.OtherversePotions;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.SpiritAffinityTracker;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import top.theillusivec4.caelus.api.CaelusApi;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.shermansplanet.otherverse.implement.ImplementManager.PRACTICE_HANDLER;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FamiliarManager {
    private static final UUID FAMILIAR_MODIFIER = UUID.fromString("99183fd5-143f-4404-a5a9-e2154a4a2914");

    private static final String HARBINGER_KEY = "harbinger_timer";
    public static float TETHER_DISTANCE = 16;

    public static boolean isChestedHorseFamiliar(LivingEntity entity) {
        if (!isFamiliar(entity)) return false;
        var t = entity.getType();
        return t.equals(EntityType.DONKEY) || t.equals(EntityType.LLAMA) || t.equals(EntityType.MULE) || t.equals(EntityType.TRADER_LLAMA);
    }

    private enum MobBenefitCondition {ANYTIME, UNDERWATER, IN_WATER, DARK, COLD, IN_LAVA}

    private record MobBenefit(MobBenefitCondition condition, MobEffect effect, int amplifier) {
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static ServerPlayer justJoinedPlayer;
    private static HashSet<EntityType<?>> flyingMobs = new HashSet<>();
    private static HashMap<EntityType<?>, Item> swarmMobs = new HashMap<>();
    private static HashMap<EntityType<?>, EntityType<? extends LivingEntity>> enemyMobs = new HashMap<>();
    private static HashMap<EntityType<?>, EntityType<? extends Mob>> zombification = new HashMap<>();
    public static HashSet<EntityType<?>> fishingMobs = new HashSet<>();
    private static HashMap<EntityType<?>, List<MobBenefit>> familiarEffects = new HashMap<>();
    private static List<ServerPlayer> playersToUpdate = new ArrayList<>();
    private static HashMap<EntityType<?>, Mob> mobInstances = new HashMap<>();

    static {
        familiarEffects.put(EntityType.AXOLOTL, new ArrayList<>(Collections.singleton(
                new MobBenefit(MobBenefitCondition.IN_WATER, MobEffects.REGENERATION, 1))));
        familiarEffects.put(EntityType.BAT, new ArrayList<>(Collections.singleton(
                new MobBenefit(MobBenefitCondition.DARK, MobEffects.NIGHT_VISION, 0))));
        familiarEffects.put(EntityType.DOLPHIN, new ArrayList<>(List.of(
                new MobBenefit(MobBenefitCondition.UNDERWATER, MobEffects.DOLPHINS_GRACE, 0),
                new MobBenefit(MobBenefitCondition.UNDERWATER, MobEffects.MOVEMENT_SPEED, 1))));
        familiarEffects.put(EntityType.GUARDIAN, new ArrayList<>(Collections.singleton(
                new MobBenefit(MobBenefitCondition.IN_WATER, MobEffects.CONDUIT_POWER, 1))));
        familiarEffects.put(EntityType.SNOW_GOLEM, new ArrayList<>(Collections.singleton(
                new MobBenefit(MobBenefitCondition.COLD, MobEffects.REGENERATION, 1))));
        familiarEffects.put(EntityType.POLAR_BEAR, new ArrayList<>(List.of(
                new MobBenefit(MobBenefitCondition.COLD, MobEffects.MOVEMENT_SPEED, 0),
                new MobBenefit(MobBenefitCondition.COLD, MobEffects.DAMAGE_BOOST, 2))));
        familiarEffects.put(EntityType.RABBIT, new ArrayList<>(List.of(
                new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.MOVEMENT_SPEED, 1),
                new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.JUMP, 1))));
        familiarEffects.put(EntityType.RAVAGER, new ArrayList<>(List.of(
                new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.DAMAGE_BOOST, 2),
                new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.DAMAGE_RESISTANCE, 1),
                new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.BAD_OMEN, 0))));
        familiarEffects.put(EntityType.STRIDER, new ArrayList<>(List.of(
                new MobBenefit(MobBenefitCondition.COLD, MobEffects.MOVEMENT_SLOWDOWN, 0))));
        familiarEffects.put(EntityType.WARDEN, new ArrayList<>(List.of(
                new MobBenefit(MobBenefitCondition.DARK, MobEffects.DAMAGE_BOOST, 1),
                new MobBenefit(MobBenefitCondition.DARK, MobEffects.ABSORPTION, 2))));
        familiarEffects.put(EntityType.WITHER, new ArrayList<>(List.of(
                new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.WITHER, 2),
                new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.REGENERATION, 0))));

        flyingMobs.add(EntityType.ALLAY);
        flyingMobs.add(EntityType.BEE);
        flyingMobs.add(EntityType.BLAZE);
        flyingMobs.add(EntityType.PARROT);
        flyingMobs.add(EntityType.PHANTOM);
        flyingMobs.add(EntityType.VEX);
        flyingMobs.add(EntityType.WITHER);
        flyingMobs.add(EntityType.ENDER_DRAGON);
        flyingMobs.add(EntityType.GHAST);

        swarmMobs.put(EntityType.BEE, Items.HONEY_BOTTLE);
        swarmMobs.put(EntityType.ENDERMITE, Items.ENDER_PEARL);
        swarmMobs.put(EntityType.SILVERFISH, Items.INFESTED_STONE);
        swarmMobs.put(EntityType.WOLF, Items.BONE);

        enemyMobs.put(EntityType.CAT, EntityType.WOLF);
        enemyMobs.put(EntityType.OCELOT, EntityType.WOLF);

        zombification.put(EntityType.VILLAGER, EntityType.ZOMBIE_VILLAGER);
        zombification.put(EntityType.HORSE, EntityType.ZOMBIE_HORSE);
        zombification.put(EntityType.HOGLIN, EntityType.ZOGLIN);
        zombification.put(EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN);
        zombification.put(EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN);

        fishingMobs.addAll(List.of(EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PIGLIN, EntityType.SQUID));
    }

    private static Mob getMobInstance(ServerLevel sl, EntityType<?> et) {
        var mob = mobInstances.get(et);
        if (mob != null) return mob;
        var mobInstance = et.create(sl.getServer().overworld());
        if (!(mobInstance instanceof Mob m)) return null;
        mobInstances.put(et, m);
        return m;
    }

    public static boolean hasFamiliarType(Player player, EntityType<? extends LivingEntity> et) {
        return Objects.equals(getFamiliarType(player), et);
    }

    public static boolean hasCatlikeBlessing(ServerPlayer player) {
        if (!player.hasEffect(OtherversePotions.FAMILIAR_BLESSING_EFFECT.get())) return false;
        var t = getFamiliarType(player);
        return Objects.equals(t, EntityType.CAT)
                || Objects.equals(t, EntityType.OCELOT)
                || Objects.equals(t, EntityType.FOX);
    }

    public static boolean isEligibleFamiliar(LivingEntity entity) {
        return getPractitionerForFamiliar(entity).isEmpty() && entity.getPersistentData().hasUUID("bindingId");
    }

    public static String getPractitionerForFamiliar(Entity entity) {
        return entity.getPersistentData().getString("practitioner");
    }

    public static ServerPlayer getPlayerFromName(ServerLevel level, String name) {
        for (var player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getGameProfile().getName().equals(name)) {
                return player;
            }
        }
        if (justJoinedPlayer != null && justJoinedPlayer.getGameProfile().getName().equals(name))
            return justJoinedPlayer;
        return null;
    }

    @SubscribeEvent()
    public static void witherWake(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !hasFamiliarType(sp, EntityType.WITHER)) return;
        for (Entity e : sp.getLevel().getEntities(sp, sp.getBoundingBox().inflate(32))) {
            if (!(e instanceof LivingEntity le)) continue;
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * (8 + sp.getRandom().nextInt(24)), 1));
        }
    }

    @SubscribeEvent()
    public static void skeletonFire(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof Player p) || !hasFamiliarType(p, EntityType.SKELETON)) return;
        event.setProjectileItemStack(event.getProjectileItemStack().isEmpty() ? Items.ARROW.getDefaultInstance() : event.getProjectileItemStack().copy());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!trySonicBoom(event.getEntity(), event.getItemStack())) return;
        event.setResult(Event.Result.ALLOW);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hirePiglin(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)
                || !hasFamiliarType(sp, EntityType.PIGLIN_BRUTE)
                || !(event.getTarget() instanceof Mob mob)) return;

        var mobtype = event.getTarget().getType();
        var isPiglin = event.getItemStack().is(Items.GOLD_INGOT) && mobtype == EntityType.PIGLIN;
        var isBrute = event.getItemStack().is(Items.GOLD_BLOCK) && mobtype == EntityType.PIGLIN_BRUTE;
        if (!isPiglin && !isBrute) return;

        event.getItemStack().shrink(1);
        if (event.getItemStack().isEmpty()) {
            sp.getInventory().removeItem(event.getItemStack());
        }
        event.setResult(Event.Result.ALLOW);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);

        BindingManager.enforceLoyalty(sp, mob, false);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFeedCarrot(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)
                || !(event.getTarget() instanceof Pig pig)
                || !event.getItemStack().is(Items.GOLDEN_CARROT)
                || !getPractitionerForFamiliar(pig).equals(sp.getGameProfile().getName())) return;
        var duration = 20 * 60;
        pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 3));

        event.getItemStack().shrink(1);
        if (event.getItemStack().isEmpty()) {
            sp.getInventory().removeItem(event.getItemStack());
        }
        event.setResult(Event.Result.ALLOW);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFeed(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)
                || !(event.getTarget() instanceof Cat cat)
                || !cat.isFood(event.getItemStack())
                || !getPractitionerForFamiliar(cat).equals(event.getEntity().getGameProfile().getName())) return;

        var duration = 20 * 30;
        sp.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 1, false, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false));
        sp.addEffect(new MobEffectInstance(OtherversePotions.FAMILIAR_BLESSING_EFFECT.get(), duration, 0, false, true, true));
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !hasFamiliarType(player, EntityType.FROG)) return;
        var bs = player.getLevel().getBlockState(player.blockPosition());
        if (!bs.is(Blocks.LILY_PAD) && !bs.is(Blocks.BIG_DRIPLEAF)) return;
        player.push(0, 1, 0);
        if (!(player instanceof ServerPlayer sp)) return;
        sp.getPersistentData().putBoolean("frogfall", true);
        sp.getLevel().playSound(null, sp, SoundEvents.FROG_LONG_JUMP, SoundSource.PLAYERS, 1f, 1f);
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        var effect = event.getEffectInstance();
        if (effect.getEffect() != MobEffects.HUNGER) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!hasFamiliarType(sp, EntityType.HUSK) && !hasFamiliarType(sp, EntityType.ZOMBIE)) return;
        sp.addEffect(new MobEffectInstance(MobEffects.SATURATION, effect.getDuration(), effect.getAmplifier()));
        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onDie(LivingDeathEvent event) {
        if (!(event.getEntity().getLevel() instanceof ServerLevel sl)) return;
        var cause = event.getSource().getEntity();
        if (!(cause instanceof Fox fox)) return;
        var sp = getPlayerFromName(sl, getPractitionerForFamiliar(fox));
        if (sp == null) return;

        var duration = (int) Math.min(10 * 60 * 20, Math.pow(event.getEntity().getMaxHealth(), 2) + 200);
        sp.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 1, false, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false));
        sp.addEffect(new MobEffectInstance(OtherversePotions.FAMILIAR_BLESSING_EFFECT.get(), duration, 0, false, true, true));
    }

    public static void makeFamiliar(LivingEntity entity, Player player) {
        if (!(entity instanceof Mob mob) || !(player instanceof ServerPlayer sp)) return;
        var practitioner = player.getGameProfile().getName();
        entity.getPersistentData().putString("practitioner", practitioner);

        if (entity instanceof TamableAnimal tamable) {
            tamable.tame(player);
        }
        if (entity instanceof AbstractHorse ah) {
            ah.setTamed(true);
        }
        if (entity instanceof AbstractChestedHorse ach) {
            ach.setChest(true);
        }
        if (entity.getType() == EntityType.HORSE) {
            for (var attr : new Attribute[]{Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH}) {
                entity.getAttribute(attr).setBaseValue(entity.getAttribute(attr).getBaseValue() * 2);
            }
        }

        CompoundTag tag = new CompoundTag();
        tag.putString("practitioner", practitioner);
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new BindingUpdateMessage(mob, BindingUpdateMessage.BindingUpdateType.FAMILIAR, tag, false));

        var familiarTag = IdolItem.mobToTag(mob);
        player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> practice.setFamiliar(familiarTag, sp));
        mob.playAmbientSound();

        var spiritType = MobBindingInfluenceUtils.mobSpirits.get(mob.getType());
        if(spiritType != null) SpiritAffinityTracker.setFamiliarAffinity(spiritType, sp);

        Otherverse.ADVANCEMENTS.trigger(sp, "famulus");

        updateAbilities(sp);
    }

    @SubscribeEvent
    public static void changeMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        playersToUpdate.add(sp);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (playersToUpdate.isEmpty()) return;
        for (var player : playersToUpdate) {
            updateAbilities(player);
        }
        playersToUpdate.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var abilities = event.player.getAbilities();
        if (!abilities.instabuild && abilities.flying && abilities.getFlyingSpeed() < 0.04f) {
            event.player.push(0, -0.01f, 0);
        }
        var type = getFamiliarType(event.player);
        if (type == null) return;
        if (type.equals(EntityType.CHICKEN) && !event.player.isCrouching()) {
            var dm = event.player.getDeltaMovement();
            event.player.setDeltaMovement(dm.x, Math.max(dm.y, -0.1f), dm.z);
            event.player.hasImpulse = true;
        } else if (type.equals(EntityType.PARROT) && !event.player.getAbilities().flying) {
            var inv = event.player.getInventory();
            var heaviness = inv.items.size() / (float) inv.getContainerSize();
            event.player.push(0, -0.01f * heaviness, 0);
        }

        if (!(event.player instanceof ServerPlayer sp)) return;

        if (type.equals(EntityType.DROWNED)) {
            if (sp.getAirSupply() < 0) {
                if (sp.getLevel().getGameTime() % 10 != 0) {
                    sp.setAirSupply(0);
                    sp.heal(1);
                    var duration = 60;
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * duration, 2, false, true, true));
                    sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * duration, 2, false, true, true));
                    sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * duration, 2, false, true, true));
                }
            } else if (!sp.isUnderWater() && sp.getAirSupply() < sp.getMaxAirSupply()) {
                var x = sp.getLevel().getGameTime() % 4 == 0 ? 3 : 4;
                sp.setAirSupply(sp.getAirSupply() - x);
            }
        }

        if (type.equals(EntityType.SNOW_GOLEM)) {
            freezeUnderfoot(sp);
        } else if (type.equals(EntityType.TURTLE)) {
            var attr = sp.getAttribute(Attributes.ARMOR);
            var hasModifiers = attr.getModifier(FAMILIAR_MODIFIER) != null;
            var shouldHaveModifiers = sp.isCrouching();
            if (hasModifiers && !shouldHaveModifiers) {
                attr.removePermanentModifier(FAMILIAR_MODIFIER);
            } else if (!hasModifiers && shouldHaveModifiers) {
                attr.addPermanentModifier(new AttributeModifier(FAMILIAR_MODIFIER, "Familiar", 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        int checkInterval = 20;

        if (sp.getLevel().getGameTime() % checkInterval != 0) return;

        var swarmItem = swarmMobs.get(type);
        if (swarmItem != null && sp.getInventory().contains(swarmItem.getDefaultInstance())) {
            maintainSwarm(sp, type, 8);
        }
        if (type.equals(EntityType.SLIME) || type.equals(EntityType.MAGMA_CUBE)) {
            var hp = sp.getHealth() / sp.getMaxHealth();
            var targetCount = hp > 0.5f ? 0 : hp > 0.25f ? 2 : 8;
            if (targetCount > 0) maintainSwarm(sp, type, targetCount);
        } else if (type.equals(EntityType.PIG)) {
            if (sp.getVehicle() instanceof Pig && sp.getLevel().dimensionTypeId().location().getPath().equals("the_nether")) {
                maintainSwarm(sp, EntityType.PIGLIN_BRUTE, 1);
            }
        }

        if (NeutralMob.class.isAssignableFrom(type.getBaseClass())) {
            for (Entity e : sp.getLevel().getEntities(sp, AABB.ofSize(sp.position(),
                    6, 6, 6))) {
                if (e.getType() != type || !(e instanceof NeutralMob nm) || !nm.isAngryAt(sp)) continue;
                nm.stopBeingAngry();
            }
        }

        if (type.equals(EntityType.FROG)) {
            if (sp.isOnGround()) sp.getPersistentData().putBoolean("frogfall", false);
        } else if (type.equals(EntityType.ENDER_DRAGON)) {
            List<EndCrystal> list = sp.level.getEntitiesOfClass(EndCrystal.class, sp.getBoundingBox().inflate(32.0D));
            if (!list.isEmpty()) {
                sp.heal(list.size());
            }
            var data = sp.getPersistentData();
            if (!data.contains(HARBINGER_KEY)) {
                data.putInt(HARBINGER_KEY, 24000 + sp.getRandom().nextInt(12000));
            } else {
                var timer = data.getInt(HARBINGER_KEY) - checkInterval;
                if (timer > 0) {
                    data.putInt(HARBINGER_KEY, timer);
                } else {
                    var dir = sp.getLookAngle().scale(2.5f);
                    data.putInt(HARBINGER_KEY, sp.getRandom().nextInt(12000));
                    DragonFireball dragonFireball = new DragonFireball(sp.level, sp, dir.x, dir.y, dir.z);
                    dragonFireball.setPos(sp.getEyePosition());
                    sp.level.addFreshEntity(dragonFireball);
                }
            }
        } else if (type.equals(EntityType.ELDER_GUARDIAN)) {
            if (sp.isUnderWater()) {
                for (Entity e : sp.getLevel().getEntities(sp, AABB.ofSize(sp.position(),
                        32, 32, 32))) {
                    if ((e instanceof Monster m) && m.isAggressive() && m.getTarget() == sp) {
                        m.setTarget(null);
                        m.setAggressive(false);
                    }
                }
                if (sp.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                    sp.removeEffect(MobEffects.DIG_SLOWDOWN);
                }
            }
        } else if (type.equals(EntityType.GLOW_SQUID)) {
            if (sp.getInventory().contains(Items.GLOW_INK_SAC.getDefaultInstance())) {
                for (Entity e : sp.getLevel().getEntities(sp, AABB.ofSize(sp.position(),
                        32, 32, 32))) {
                    if ((e instanceof Mob m) && !m.hasEffect(MobEffects.GLOWING)) {
                        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 5, 0));
                    }
                }
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 21 * 13, 0, false, false, false));
            }
        } else if (type.equals(EntityType.PHANTOM)) {
            if (!sp.getLevel().isDay()) {
                var attr = sp.getAttribute(CaelusApi.getInstance().getFlightAttribute());
                attr.setBaseValue(1);
            }
        } else if (type.equals(EntityType.SNOW_GOLEM)) {
            if (sp.getRandom().nextInt(20) == 0
                    && (sp.getLevel().getBiome(sp.blockPosition()).value().shouldSnowGolemBurn(sp.blockPosition())
                    || sp.getLevel().dimensionType().ultraWarm())) {
                var slotIndex = sp.getInventory().findSlotMatchingItem(Items.SNOW_BLOCK.getDefaultInstance());
                if (slotIndex < 0) {
                    sp.hurt(DamageSource.HOT_FLOOR, 2);
                } else {
                    var stack = sp.getInventory().getItem(slotIndex);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        sp.getInventory().removeItem(stack);
                    }
                }
            }
        } else if (type.equals(EntityType.SHULKER)) {
            if (sp.hasEffect(MobEffects.LEVITATION) && sp.isCrouching()) {
                sp.removeEffect(MobEffects.LEVITATION);
                sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200));
            }
        } else if (type.equals(EntityType.WOLF)) {
            var isFullMoon = sp.getLevel().isNight() && sp.getLevel().getMoonPhase() == 0;
            var hasAttr = sp.getAttribute(Attributes.MAX_HEALTH).getModifier(FAMILIAR_MODIFIER) != null;
            if (isFullMoon != hasAttr) {
                for (var attrName : new Attribute[]{Attributes.ATTACK_KNOCKBACK, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.MAX_HEALTH, Attributes.KNOCKBACK_RESISTANCE}) {
                    var attr = sp.getAttribute(attrName);
                    attr.removePermanentModifier(FAMILIAR_MODIFIER);
                    if (isFullMoon) {
                        attr.addPermanentModifier(new AttributeModifier(FAMILIAR_MODIFIER, "Familiar", 0.5F, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                }
            }
        }else if(type.equals(EntityType.STRIDER)){
            var inLava = sp.getLevel().getBlockState(sp.blockPosition()).is(Blocks.LAVA);
            var attr = sp.getAttribute(Attributes.MOVEMENT_SPEED);
            attr.removePermanentModifier(FAMILIAR_MODIFIER);
            if (inLava) {
                attr.addPermanentModifier(new AttributeModifier(FAMILIAR_MODIFIER, "Familiar", 0.8F, AttributeModifier.Operation.MULTIPLY_TOTAL));
                if (sp.getLevel().getGameTime() % checkInterval * 3 == 0) {
                    sp.heal(1);
                }
            }
        }

        if (sp.isInWaterOrRain() && (type.fireImmune() || type.equals(EntityType.ENDERMAN))) {
            if (sp.isInWater() || !sp.hasItemInSlot(EquipmentSlot.HEAD) || sp.getRandom().nextInt(20) == 0) {
                sp.hurt(DamageSource.MAGIC, 1);
            }
        }

        var enemy = enemyMobs.get(type);
        if (enemy != null) {
            for (Entity e : sp.getLevel().getEntities(sp, AABB.ofSize(sp.position(),
                    24, 8, 24))) {
                if (e.getType() != enemy || !(e instanceof NeutralMob nm)) continue;
                nm.setTarget(sp);
            }
        }

        var benefits = new ArrayList<>(familiarEffects.getOrDefault(type, new ArrayList<>()));
        if (type.fireImmune()) benefits.add(new MobBenefit(MobBenefitCondition.ANYTIME, MobEffects.FIRE_RESISTANCE, 0));
        var mobInstance = getMobInstance(sp.getLevel(), type);
        if (mobInstance != null) {
            if (mobInstance.getMobType() == MobType.WATER && !mobInstance.canDrownInFluidType(ForgeMod.WATER_TYPE.get())) {
                benefits.add(new MobBenefit(MobBenefitCondition.UNDERWATER, MobEffects.WATER_BREATHING, 0));
            }
        }
        for (var benefit : benefits) {
            var lightLevel = 0;
            if (benefit.condition == MobBenefitCondition.DARK) {
                lightLevel = sp.getLevel().getLightEngine().getRawBrightness(sp.blockPosition(), sp.getLevel().getSkyDarken());
                if (type.equals(EntityType.BAT)) {
                    abilities.mayfly = lightLevel <= 10;
                    abilities.setFlyingSpeed(0.05f * (1f - lightLevel / 11f));
                    if (!sp.isOnGround()) abilities.flying = abilities.mayfly;
                    sp.onUpdateAbilities();
                }
                if (lightLevel > 10) continue;
            } else if (benefit.condition == MobBenefitCondition.IN_WATER) {
                if (!sp.isInWaterOrRain()) continue;
            } else if (benefit.condition == MobBenefitCondition.UNDERWATER) {
                if (!sp.isUnderWater()) continue;
            } else if (benefit.condition == MobBenefitCondition.COLD) {
                if (!sp.getLevel().getBiome(sp.blockPosition()).value().coldEnoughToSnow(sp.blockPosition())) continue;
            } else if (benefit.condition == MobBenefitCondition.IN_LAVA) {
                if (!sp.getLevel().getBlockState(sp.blockPosition()).is(Blocks.LAVA)) return;
            }
            var effect = sp.getEffect(benefit.effect);
            if (effect != null && effect.getDuration() > 21 * 12) continue;
            sp.addEffect(new MobEffectInstance(benefit.effect, 21 * 13, benefit.amplifier, false, false, false));
        }
    }

    private static void freezeUnderfoot(ServerPlayer sp) {
        if (!sp.getInventory().contains(Items.BLUE_ICE.getDefaultInstance())) return;
        var replacePos = sp.blockPosition().below();
        var blockState = sp.getLevel().getBlockState(replacePos);
        if (blockState.is(Blocks.WATER)) {
            if (blockState.getFluidState().isSource()) {
                sp.getLevel().setBlockAndUpdate(replacePos, Blocks.FROSTED_ICE.defaultBlockState());
                sp.level.playSound(null, sp, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1f, 1f);
            }
        } else if (blockState.is(Blocks.LAVA)) {
            sp.getLevel().setBlockAndUpdate(replacePos,
                    blockState.getFluidState().isSource()
                            ? Blocks.OBSIDIAN.defaultBlockState()
                            : Blocks.STONE.defaultBlockState());
            sp.level.playSound(null, sp, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
        }
    }

    private static void maintainSwarm(ServerPlayer sp, EntityType<? extends LivingEntity> type, int targetCount) {
        if (type == EntityType.WOLF) {
            if (sp.getLevel().dimensionTypeId().location().getPath().equals("overworld")) {
                var phase = sp.getLevel().getMoonPhase();
                targetCount = (new int[]{9, 7, 5, 3, 1, 3, 5, 7})[phase];
            } else {
                targetCount = 3;
            }
        }
        var swarmCount = 0;
        var name = sp.getGameProfile().getName();
        for (Entity e : sp.getLevel().getEntities(sp, AABB.ofSize(sp.position(),
                TETHER_DISTANCE * 2, TETHER_DISTANCE * 2, TETHER_DISTANCE * 2))) {
            if (e.getType() != type) continue;
            if (!e.getPersistentData().getString("practitioner_loyalty").equals(name)) continue;
            swarmCount++;
        }

        if (swarmCount >= targetCount) return;

        var e = (Mob) type.create(sp.getLevel());

        var spawnDistance = TETHER_DISTANCE;

        if (e instanceof Slime slime) {
            slime.setSize(targetCount < 4 ? 2 : 1, true);
            spawnDistance = 4;
        }
        var pos = getSpaceAroundPlayer(sp, spawnDistance);
        if (pos == null) return;
        e.setPos(pos);

        sp.getLevel().addFreshEntity(e);
        BindingManager.enforceLoyalty(sp, e, true);
    }

    public static Vec3 getSpaceAroundPlayer(Player sp, float spawnDistance) {
        var r = sp.getLevel().getRandom();
        var pos = sp.position().add(
                (r.nextFloat() - 0.5f) * spawnDistance,
                (r.nextFloat() * 0.5f) * spawnDistance,
                (r.nextFloat() - 0.5f) * spawnDistance);

        var blockPos = new BlockPos(pos);
        var blockState = sp.getLevel().getBlockState(blockPos);
        if (!blockState.is(Blocks.AIR) && !blockState.getCollisionShape(sp.getLevel(), blockPos).isEmpty()) return null;

        while (blockPos.getY() > sp.blockPosition().getY() - TETHER_DISTANCE / 3) {
            var newblockpos = blockPos.below();
            var bs = sp.getLevel().getBlockState(newblockpos);
            if (!bs.is(Blocks.AIR) && !bs.getCollisionShape(sp.getLevel(), newblockpos).isEmpty()) break;
            blockPos = newblockpos;
            pos = pos.add(0, -1, 0);
        }
        return pos;
    }

    @SubscribeEvent
    public static void playHorn(LivingEntityUseItemEvent event) {
        if (!event.getItem().is(Items.GOAT_HORN)) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!hasFamiliarType(sp, EntityType.GOAT)) return;
        for (Entity e : sp.getLevel().getEntities(sp, sp.getBoundingBox().inflate(32))) {
            if (!(e instanceof LivingEntity le) || !le.getType().equals(EntityType.PLAYER) && !isFamiliar(le)) return;
            le.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 1));
        }
    }

    @SubscribeEvent
    public static void mobGrief(LivingDestroyBlockEvent event) {
        if (!event.getState().is(OtherverseBlocks.CHALK_LINE.get())) return;
        var lvl = event.getEntity().getLevel();
        var below = event.getPos().below();
        var stateBelow = lvl.getBlockState(below);
        if (stateBelow.canEntityDestroy(lvl, below, event.getEntity())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onConvert(LivingConversionEvent.Pre event) {
        if (!isFamiliar(event.getEntity())) return;
        event.setConversionTimer(0);
        event.setCanceled(true);
    }

    public static void updateAbilities(ServerPlayer player) {
        updateAbilities(player, true);
    }

    public static void updateAbilities(ServerPlayer player, boolean setSpeed) {
        var type = getFamiliarType(player);
        if (type == null) return;

        player.refreshDimensions();
        tryChangeScale(player);

        if (type.equals(EntityType.HOGLIN) || type.equals(EntityType.RAVAGER)) {
            var attr = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
            attr.removePermanentModifier(FAMILIAR_MODIFIER);
            attr.addPermanentModifier(new AttributeModifier(FAMILIAR_MODIFIER, "Familiar", 1.0D, AttributeModifier.Operation.ADDITION));
            attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            attr.removePermanentModifier(FAMILIAR_MODIFIER);
            attr.addPermanentModifier(new AttributeModifier(FAMILIAR_MODIFIER, "Familiar", 0.6D, AttributeModifier.Operation.ADDITION));
        } else if (type.equals(EntityType.IRON_GOLEM)) {
            var attr = player.getAttribute(Attributes.ARMOR);
            attr.removePermanentModifier(FAMILIAR_MODIFIER);
            attr.addPermanentModifier(new AttributeModifier(FAMILIAR_MODIFIER, "Familiar", 15.0D, AttributeModifier.Operation.ADDITION));
            attr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            attr.removePermanentModifier(FAMILIAR_MODIFIER);
            attr.addPermanentModifier(new AttributeModifier(FAMILIAR_MODIFIER, "Familiar", 1.0D, AttributeModifier.Operation.ADDITION));
        }

        var abilities = player.getAbilities();
        if (player.isCreative()) {
            abilities.setFlyingSpeed(0.05f);
            return;
        }
        if (flyingMobs.contains(type)) {
            if (type.equals(EntityType.ENDER_DRAGON) || type.equals(EntityType.PHANTOM) || type.equals(EntityType.PARROT)) {
                var attr = player.getAttribute(CaelusApi.getInstance().getFlightAttribute());
                if (type.equals(EntityType.PHANTOM)) {
                    ServerStatsCounter serverstatscounter = player.getStats();
                    int j = Mth.clamp(serverstatscounter.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                    attr.setBaseValue(j > 20 * 60 * 10 ? 1 : 0);
                } else {
                    attr.setBaseValue(1);
                }
            } else {
                abilities.mayfly = true;
                if(setSpeed) {
                    abilities.setFlyingSpeed(type.equals(EntityType.WITHER) ? 0.024f : 0.012f);
                }
                if (!player.isOnGround()) abilities.flying = true;
                player.onUpdateAbilities();
            }
        }
    }

    public static CompoundTag getFamiliarData(Player player) {
        AtomicReference<CompoundTag> tag = new AtomicReference<>(new CompoundTag());
        player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> {
            tag.set(practice.getFamiliarData());
        });
        return tag.get();
    }

    public static EntityType<LivingEntity> getFamiliarType(Player player) {
        if (player.getLevel().isClientSide()) {
            return BindingRenderer.getFamiliarType(player);
        }
        var data = getFamiliarData(player);
        if (data.isEmpty()) return null;
        return (EntityType<LivingEntity>) getEntityTypeFromTag(data);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (!Keybindings.KEY_FAMILIAR.consumeClick()) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        OtherversePacketHandler.INSTANCE.sendToServer(new SummonFamiliarMessage(true, mc.hitResult));
    }

    public static Entity makeMobFromTag(EntityType<?> type, CompoundTag tag, Vec3 spawnPos, ServerLevel level) {
        var mobData = tag.getCompound("mob_data");
        var id = mobData.getCompound("EntityTag").getUUID("UUID");

        var entity = level.getEntity(id);
        if (entity != null) {
            entity.discard();
        }

        var e = type.create(level, mobData, null, null,
                new BlockPos(spawnPos), MobSpawnType.MOB_SUMMONED, false, false);

        e.setPos(spawnPos);

        addMobToLevel(e, level);
        return e;
    }

    private static void addMobToLevel(Entity e, ServerLevel level) {
        CompoundTag persistentData = e.getPersistentData();
        if (persistentData.contains("bindingId") && e instanceof Mob mob) {
            TransientDiagramData data = DiagramManager.getOrCreateLevelData(level.getServer().overworld());
            BindingInfo binding = data.bindingsById.get(persistentData.getUUID("bindingId"));
            if(binding == null){
                persistentData.remove("bindingId");
            }else {
                binding.mob = mob;
                if (isFamiliar(mob)) {
                    binding.dimensionHash = DiagramManager.getDimensionHash(level);
                    if (data.savedData != null) {
                        data.savedData.setDirty();
                    }
                }
                data.retryUpdateClient();
            }
        }
        level.addFreshEntityWithPassengers(e);
    }

    @SubscribeEvent
    public static void onEntityLoad(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            var familiarData = getFamiliarData(player);
            if (familiarData.isEmpty()) return;
            LOGGER.debug("PLAYER JOINING");
            justJoinedPlayer = player;
            var mobData = familiarData.getCompound("mob_data");
            var entityTag = mobData.getCompound("EntityTag");
            var data = DiagramManager.getOrCreateLevelData(player.getServer().overworld());
            var binding = data.bindingsById.get(entityTag.getCompound("ForgeData").getUUID("bindingId"));
            if (binding != null) {
                for (var level : sl.getServer().getAllLevels()) {
                    if (DiagramManager.getDimensionHash(level) != binding.dimensionHash) continue;
                    EntityType<?> type = getEntityTypeFromTag(familiarData);
                    var hp = entityTag.getFloat("Health");
                    if (hp <= 0f) break;
                    var mob = (Mob) type.create(level);
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, entityTag);
                    var listTag = entityTag.getList("Pos", 6);
                    LOGGER.debug("FAMILIAR POSITION: " + listTag.getDouble(0) + ", " + listTag.getDouble(2));
                    EntityType.updateCustomEntityTag(level, player, mob, mobData);
                    addMobToLevel(mob, level);
                    LOGGER.debug("MOB ADDED");
                    break;
                }
            }
            LOGGER.debug("DONE SEARCHING LEVELS");
            updateAbilities(player);
            justJoinedPlayer = null;
            return;
        }
        var practitioner = getPractitionerForFamiliar(event.getEntity());
        if (practitioner.isEmpty()) return;
        if (getPlayerFromName(sl, practitioner) == null) {
            LOGGER.debug("NULL PLAYER FOR FAMILIAR");
            event.setCanceled(true);
            event.getEntity().discard();
            return;
        }

        LOGGER.debug("FAMILIAR JOINING LEVEL: " + sl.dimension() + " " + DiagramManager.getDimensionHash(sl));
        var data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
        if(!event.getEntity().getPersistentData().contains("bindingId")) return;
        var binding = data.bindingsById.get(event.getEntity().getPersistentData().getUUID("bindingId"));
        if (binding == null) {
            LOGGER.debug("NULL BINDING FOR FAMILIAR");
            return;
        }
        var localLevel = binding.getLocalLevel();

        LOGGER.debug("BINDING LEVEL: " + (localLevel == null ? "null" : localLevel.dimension()) + " " + binding.dimensionHash);
        if (binding.dimensionHash != DiagramManager.getDimensionHash(sl)) {
            event.setCanceled(true);
            event.getEntity().discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LOGGER.debug("PLAYER LEAVING");
        unloadFamiliar(player);
    }

    private static void unloadFamiliar(ServerPlayer player) {
        var familiarData = getFamiliarData(player);
        if (familiarData.isEmpty()) return;
        var id = familiarData.getCompound("mob_data").getCompound("EntityTag").getUUID("UUID");
        for (var level : player.getLevel().getServer().getAllLevels()) {
            var entity = level.getEntity(id);
            if (!(entity instanceof Mob mob)) continue;
            var familiarTag = IdolItem.mobToTag(mob);
            player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> practice.setFamiliar(familiarTag, player));
            entity.discard();
            LOGGER.debug("UNLOADING FAMILIAR");
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        var practitioner = getPractitionerForFamiliar(mob);
        if (practitioner.isEmpty()) return;
        var player = getPlayerFromName(sl, practitioner);
        if (player == null) {
            LOGGER.debug("NULL PLAYER");
            return;
        }
        var familiarTag = IdolItem.mobToTag(mob);
        player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> {
            var familiarData = practice.getFamiliarData();
            if (familiarData.isEmpty()) return;
            EntityType<LivingEntity> type = (EntityType<LivingEntity>) getEntityTypeFromTag(familiarData);
            if (mob.getType() != type) {
                LOGGER.debug("THIS IS NOT MY BEAUTIFUL FAMILIAR");
                return;
            }
            practice.setFamiliar(familiarTag, player);
        });
    }

    @SubscribeEvent
    public static void onDimensionTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity().getLevel() instanceof ServerLevel sl)) return;
        var destination = sl.getServer().getLevel(event.getDimension());
        var id = event.getEntity().getUUID();
        var obsoleteClone = destination.getEntity(id);
        if (obsoleteClone != null) {
            obsoleteClone.discard();
        }
        if (!isFamiliar(event.getEntity())) return;
        var data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
        var binding = data.bindingsById.get(event.getEntity().getPersistentData().getUUID("bindingId"));
        binding.dimensionHash = DiagramManager.getDimensionHash(event.getDimension());
        if (data.savedData != null) {
            data.savedData.setDirty();
        }
    }

    public static void summonFamiliar(ServerPlayer player, BlockHitResult hitResult) {
        var familiarData = getFamiliarData(player);
        if (familiarData.isEmpty()) {
            LOGGER.debug("NULL FAMILIAR DATA");
            return;
        }

        for (var hand : new InteractionHand[]{InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND}) {
            if (tryFamiliarTransfuse(player, hand, familiarData, hitResult)) return;
        }

        var entityTag = familiarData.getCompound("mob_data").getCompound("EntityTag");
        var id = entityTag.getUUID("UUID");
        var entity = player.getLevel().getEntity(id);
        var spawnPos = player.getEyePosition().add(player.getLookAngle());
        if (hitResult != null) {
            var nrm = hitResult.getDirection().getNormal();
            spawnPos = hitResult.getLocation().add(nrm.getX() / 2f, 0, nrm.getZ() / 2f);
        }

        var hp = entityTag.getFloat("Health");
        var deathTime = entityTag.getShort("DeathTime");
        EntityType<LivingEntity> type = (EntityType<LivingEntity>) getEntityTypeFromTag(familiarData);
        if (deathTime > 0 || hp <= 0) {
            if (entity != null) {
                entity.discard();
                entity = null;
            }
            var maxHp = (float) DefaultAttributes.getSupplier(type).getValue(Attributes.MAX_HEALTH);
            var cost = (int) Math.ceil(Mth.log2((int) maxHp) * 0.6f);
            if (!player.isCreative() && !SelfManager.ChangeSelf(player, -cost)) {
                player.displayClientMessage(Component.literal("You need " + cost + " Self to revive your Familiar!"), true);
            } else {
                entityTag.putFloat("Health", maxHp);
                entityTag.putShort("DeathTime", (short) 0);
                player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> practice.setFamiliar(familiarData, player));
                if (type.equals(EntityType.VEX)) {
                    entityTag.putInt("LifeTicks", 60 * (30 + player.getRandom().nextInt(90)));
                    var spawnBlock = new BlockPos(spawnPos);
                    for (var i = 0; i < 6; i++) {
                        Vex vex = EntityType.VEX.create(player.level);
                        vex.moveTo(spawnBlock, 0.0F, 0.0F);
                        vex.finalizeSpawn(player.getLevel(), player.level.getCurrentDifficultyAt(spawnBlock), MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null, (CompoundTag) null);
                        vex.setBoundOrigin(spawnBlock);
                        vex.setLimitedLife(20 * (30 + player.getRandom().nextInt(90)));
                        player.getLevel().addFreshEntityWithPassengers(vex);
                        BindingManager.enforceLoyalty(player, vex, false);
                    }
                }
            }
        }

        var data = DiagramManager.getOrCreateLevelData(player.getServer().overworld());
        var binding = data.bindingsById.get(entityTag.getCompound("ForgeData").getUUID("bindingId"));
        var oldDimensionHash = binding.dimensionHash;
        var playerDimensionHash = DiagramManager.getDimensionHash(player.getLevel());
        if (oldDimensionHash != playerDimensionHash) {
            var oldDimension = binding.getLocalLevel();
            if (oldDimension != null) {
                var oldEntity = oldDimension.getEntity(id);
                if (oldEntity != null) {
                    oldEntity.discard();
                }
            }
            binding.dimensionHash = DiagramManager.getDimensionHash(player.getLevel());
            if (data.savedData != null) {
                data.savedData.setDirty();
            }
        }

        if (entity == null) {
            LOGGER.debug("CREATING NEW FAMILIAR");
            unloadFamiliar(player);
            entity = makeMobFromTag(type, familiarData, spawnPos, player.getLevel());
        } else {
            entity.setPos(spawnPos);
        }
    }

    @SubscribeEvent
    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        if (!hasFamiliarType(event.getPlayer(), EntityType.ENDERMAN)) return;
        event.getPlayer().getInventory().add(new ItemStack(Items.ENDER_PEARL, 1));
        event.setAttackDamage(0);
    }

    @SubscribeEvent
    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!hasFamiliarType(sp, EntityType.ENDERMAN)) return;
        sp.getInventory().add(new ItemStack(Items.CHORUS_FRUIT, 1));
    }

    public static boolean trySonicBoom(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!hasFamiliarType(player, EntityType.WARDEN)
                || player.getCooldowns().isOnCooldown(stack.getItem())
                || !HallowHelper.tryDrainHallow(stack, Spirits.DARK, 13)) {
            return false;
        }
        var start = player.getEyePosition();
        var trajectory = player.getLookAngle().scale(16);
        Vec3 trajectoryNrm = trajectory.normalize();
        for (Entity e : player.getLevel().getEntities(player, player.getBoundingBox().inflate(16))) {
            if (!(e instanceof LivingEntity target)) continue;
            if (target.getBoundingBox().clip(start, start.add(trajectory)).isEmpty()) continue;
            trajectory = target.getEyePosition().subtract(start);
            trajectoryNrm = trajectory.normalize();
            target.hurt(DamageSource.sonicBoom(player), 10.0F);
            var knockback = (1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            target.push(trajectoryNrm.x() * 2.5D * knockback,
                    trajectoryNrm.y() * 0.5D * knockback,
                    trajectoryNrm.z() * 2.5D * knockback);
            break;
        }

        for (int i = 1; i < Mth.floor(trajectory.length()) + 7; ++i) {
            Vec3 vec33 = start.add(trajectoryNrm.scale(i));
            sp.getLevel().sendParticles(ParticleTypes.SONIC_BOOM, vec33.x, vec33.y, vec33.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        player.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 10, 1));
        player.getCooldowns().addCooldown(stack.getItem(), 20 * 10);
        return true;
    }

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (event.getItemStack().is(Items.TNT)) {
            tryThrowTnt(event);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getItemStack().is(Items.SHEARS)) {
            event.setResult(Event.Result.ALLOW);
            player.hurt(DamageSource.MAGIC, 1);
            var dropped = player.getRandom().nextInt(4) + 1;
            var mushroomList = player.getLevel().dimensionType().bedWorks()
                    ? new Item[]{Items.BROWN_MUSHROOM, Items.RED_MUSHROOM}
                    : new Item[]{Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS};
            for (var i = 0; i < dropped; i++) {
                var mushroom = mushroomList[player.getRandom().nextInt(2)];
                player.drop(new ItemStack(mushroom, 1), false);
                player.level.playSound(null, player, SoundEvents.MOOSHROOM_SHEAR, SoundSource.PLAYERS, 1f, 1f);
            }
            return;
        }
        if (tryWitherShoot(player, event.getItemStack())) {
            event.setResult(Event.Result.ALLOW);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (!event.getItemStack().is(Items.FLINT_AND_STEEL)) return;
        if (player.getCooldowns().isOnCooldown(Items.FLINT_AND_STEEL)) return;
        var familiarType = getFamiliarType(player);
        var hasBlazeFamiliar = Objects.equals(familiarType, EntityType.BLAZE);
        var hasDragonFamiliar = Objects.equals(familiarType, EntityType.ENDER_DRAGON);
        var hasGhastFamiliar = Objects.equals(familiarType, EntityType.GHAST);
        var isImplement = ImplementManager.isImplement(event.getItemStack());
        if (!hasBlazeFamiliar && !hasDragonFamiliar && !hasGhastFamiliar && !isImplement) return;
        var cooldown = hasDragonFamiliar ? 200 : hasGhastFamiliar ? 100 : hasBlazeFamiliar ? 60 : 300;
        if (isImplement) cooldown = cooldown / 5;
        player.getCooldowns().addCooldown(Items.FLINT_AND_STEEL, cooldown);
        event.setResult(Event.Result.ALLOW);
        var dir = player.getLookAngle().scale(2.5f);
        if (hasDragonFamiliar) {
            player.getPersistentData().putInt(HARBINGER_KEY, 24000 + player.getRandom().nextInt(12000));
            DragonFireball dragonFireball = new DragonFireball(sl, player, dir.x, dir.y, dir.z);
            dragonFireball.setPos(player.getEyePosition());
            sl.addFreshEntity(dragonFireball);
            event.getItemStack().hurtAndBreak(7, player, (p) -> p.broadcastBreakEvent(event.getHand()));
        } else if (hasGhastFamiliar) {
            LargeFireball fireball = new LargeFireball(sl, player, dir.x, dir.y, dir.z, 1);
            fireball.setPos(player.getEyePosition());
            sl.addFreshEntity(fireball);
            event.getItemStack().hurtAndBreak(3, player, (p) -> p.broadcastBreakEvent(event.getHand()));
        } else {
            SmallFireball smallfireball = new SmallFireball(sl, player, dir.x, dir.y, dir.z);
            smallfireball.setPos(player.getEyePosition());
            sl.addFreshEntity(smallfireball);
            event.getItemStack().hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(event.getHand()));
        }
    }

    private static boolean tryWitherShoot(Player player, ItemStack itemStack) {
        if (!(player instanceof ServerPlayer sp)
                || !itemStack.is(Items.WITHER_SKELETON_SKULL)
                || player.getCooldowns().isOnCooldown(Items.WITHER_SKELETON_SKULL)
                || !hasFamiliarType(player, EntityType.WITHER)
                || player.getCooldowns().isOnCooldown(Items.WITHER_SKELETON_SKULL)
        ) return false;

        double d0 = player.getX();
        double d1 = player.getY(0.66f);
        double d2 = player.getZ();
        double d3 = player.getLookAngle().x;
        double d4 = player.getLookAngle().y;
        double d5 = player.getLookAngle().z;
        WitherSkull witherskull = new WitherSkull(player.level, player, d3, d4, d5);
        witherskull.setOwner(player);
        witherskull.setDangerous(true);
        witherskull.setPosRaw(d0, d1, d2);
        sp.getLevel().addFreshEntity(witherskull);

        if(!player.getAbilities().instabuild){
            itemStack.shrink(1);
            if (itemStack.isEmpty()) {
                player.getInventory().removeItem(itemStack);
            }
        }

        player.getCooldowns().addCooldown(Items.WITHER_SKELETON_SKULL, 20);
        return true;
    }

    private static void tryThrowTnt(PlayerInteractEvent.RightClickItem event) {
        var player = event.getEntity();
        if (!hasFamiliarType(player, EntityType.CREEPER)) return;
        var itemstack = event.getItemStack();
        if (itemstack.hasTag() && itemstack.getTag().contains("implement_max_uses")) {
            var tag = itemstack.getTag();
            var newAmount = tag.getInt("implement_remaining_uses") - 1;
            tag.putInt("implement_remaining_uses", newAmount);
            if (newAmount <= 0) {
                itemstack.shrink(1);
                if (itemstack.isEmpty()) {
                    player.getInventory().removeItem(itemstack);
                }
            }
        } else {
            itemstack.shrink(1);
            if (itemstack.isEmpty()) {
                player.getInventory().removeItem(itemstack);
            }
        }
        ThrownTNT thrownTnt = new ThrownTNT(player.level, player);
        var otherstack = event.getItemStack().copy();
        otherstack.setCount(1);
        thrownTnt.setItem(otherstack);
        thrownTnt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        player.getLevel().addFreshEntity(thrownTnt);
    }

    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && hasFamiliarType(player, EntityType.CREEPER)) {
            event.setCanceled(true);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 120, 2, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 120, 2, false, true, true));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player p && event.getSource().isFall()) {
            var scale = ScaleTypes.BASE.getScaleData(p).getScale();
            var newAmount = event.getAmount() * scale + 1 - 1f / scale;
            if (newAmount <= 0) event.setCanceled(true);
            else event.setAmount(newAmount);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Player p) {
            var type = getFamiliarType(p);
            if (type != null) {
                if (type.equals(EntityType.CAVE_SPIDER)) {
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 7 * 20, 0), p);
                } else if (type.equals(EntityType.STRAY)) {
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 7 * 20, 1), p);
                } else if (type.equals(EntityType.WITHER_SKELETON)) {
                    var weapon = p.getMainHandItem();
                    if (weapon.is(Items.STONE_SWORD)) {
                        if (ImplementManager.isImplement(weapon)) {
                            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 60, 2), p);
                        } else {
                            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 10), p);
                        }
                    }
                } else if (type.equals(EntityType.GOAT)) {
                    var diff = event.getEntity().position().subtract(p.position());
                    var len = diff.length();
                    if (len < p.getReachDistance()) {
                        diff = diff.scale(event.getAmount() / len / 2);
                        diff = diff.add(p.getDeltaMovement());
                        p.setDeltaMovement(Vec3.ZERO);
                        event.getEntity().push(diff.x, diff.y, diff.z);
                        event.getEntity().causeFallDamage(p.fallDistance, 1, DamageSource.FALL);
                        p.resetFallDistance();
                    }
                } else if (type.equals(EntityType.HOGLIN)) {
                    var diff = event.getEntity().position().subtract(p.position());
                    var len = diff.length();
                    if (len < p.getReachDistance()) {
                        event.getEntity().push(0, event.getAmount() / 4, 0);
                        event.getEntity().setOnGround(false);
                    }
                } else if (type.equals(EntityType.ZOMBIE) || type.equals(EntityType.HUSK)) {
                    if (event.getEntity().getRandom().nextInt(7) == 0) {
                        var zombieResult = zombification.get(event.getEntity().getType());
                        if (zombieResult != null && event.getEntity() instanceof Mob mob) {
                            mob.convertTo(zombieResult, true);
                        }
                        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 1));
                        event.getEntity().hurt(event.getSource(), event.getAmount());
                    }
                }
            }
        }
        if (isFamiliar(event.getEntity())) {
            if (event.getSource() == DamageSource.DRY_OUT) event.setCanceled(true);
            else if (event.getSource() == DamageSource.DROWN && event.getEntity() instanceof WaterAnimal)
                event.setCanceled(true);
        }
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var familiarType = getFamiliarType(sp);
        if (familiarType == null) return;
        tryChangeScale(sp);
        if (event.getSource().isFall()) {
            var scale = ScaleTypes.BASE.getScaleData(sp).getScale();
            var newAmount = event.getAmount() * scale + 1 - 1f / scale;
            if (newAmount <= 0) event.setCanceled(true);
        }
        if (event.getSource().isFall() && hasCatlikeBlessing(sp)) event.setCanceled(true);
        else if (familiarType.equals(EntityType.CREEPER) && event.getSource().isExplosion()) event.setCanceled(true);
        else if (familiarType.equals(EntityType.ENDER_DRAGON) && event.getSource() == DamageSource.OUT_OF_WORLD) {
            var end = sp.getServer().getLevel(Level.END);
            sp.changeDimension(end, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                    var y = end.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
                    var e = ITeleporter.super.placeEntity(entity, currentWorld, destWorld, yaw, repositionEntity);
                    e.moveTo(0.5f, y, 0.5f);
                    return e;
                }
            });
        } else if (familiarType.equals(EntityType.FROG) && event.getSource().isFall() && sp.getPersistentData().getBoolean("frogfall")) {
            event.setCanceled(true);
            sp.getPersistentData().putBoolean("frogfall", false);
        } else if (event.getSource() == DamageSource.FREEZE && (familiarType.equals(EntityType.SNOW_GOLEM) || familiarType.equals(EntityType.POLAR_BEAR))) {
            event.setCanceled(true);
        } else if (familiarType.equals(EntityType.SLIME) || familiarType.equals(EntityType.MAGMA_CUBE)) {
            if (event.getSource().getEntity() != null && event.getSource().getEntity().getType().equals(familiarType)) {
                event.setCanceled(true);
            }
        } else if (familiarType.equals(EntityType.SQUID)) {
            var cutoff = sp.getMaxHealth() / 3;
            if (sp.getHealth() > cutoff && sp.getHealth() - event.getAmount() < cutoff) {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 2));
            }
        } else if (familiarType.equals(EntityType.WITHER) && event.getSource() == DamageSource.WITHER) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) tryChangeScale(sp);
    }

    public static void tryChangeScale(Player p) {
        var t = getFamiliarType(p);
        if (!Objects.equals(t, EntityType.MAGMA_CUBE) && !Objects.equals(t, EntityType.SLIME)) return;
        var hp = p.getHealth() / p.getMaxHealth();
        var scale = hp > 0.5f ? 1 : hp > 0.25f ? 0.5f : 0.25f;
        for (var st : ScaleRegistries.SCALE_TYPES.values()) {
            st.getScaleData(p).setScale(1);
        }
        ScaleTypes.BASE.getScaleData(p).setScale(scale);
        ScaleTypes.REACH.getScaleData(p).setScale(1f / scale);
        ScaleTypes.MOTION.getScaleData(p).setScale(1f / scale);
        ScaleTypes.JUMP_HEIGHT.getScaleData(p).setScale(1f / (float) Math.pow(scale, 0.5f));
    }

    private static boolean tryFamiliarTransfuse(ServerPlayer player, InteractionHand hand, CompoundTag familiarData, BlockHitResult hitResult) {
        var item = player.getItemInHand(hand);
        if (item.isEmpty()) return false;
        var et = getEntityTypeFromTag(familiarData);

        if (et == EntityType.ALLAY) {
            return tryMakeAllayContract(player, hand, hitResult);
        }

        var ioe = new ItemOrEntityType(item.getItem());
        var possibleTransfusions = MobTransfusions.ALL_MOB_TRANSFUSIONS.data;
        if (!possibleTransfusions.containsKey(ioe)) {
            return false;
        }
        List<MobTransfusions.MobTransfusionData> transfusions = possibleTransfusions.get(ioe);
        for (int i = transfusions.size() - 1; i >= 0; i--) {
            var possibleTransfusion = transfusions.get(i);
            if (possibleTransfusion.entityTypes().size() != 1) continue;
            if (!possibleTransfusion.entityTypes().contains(et)) continue;
            if (!player.isCreative()) {
                var price = possibleTransfusion.price();
                for (var itemstack : player.getInventory().items) {
                    if (itemstack == null) continue;
                    var types = MobBindingInfluenceUtils.allFoods.get(new ItemOrEntityType(itemstack.getItem()));
                    if (types == null) continue;
                    var hp = types.get(et);
                    if (hp == null) continue;
                    var itemsToRemove = Math.min(itemstack.getCount(), (int) Math.ceil(price / (float) hp));
                    itemstack.shrink(itemsToRemove);
                    if (itemstack.isEmpty()) {
                        player.getInventory().removeItem(itemstack);
                    }
                    price -= itemsToRemove * hp;
                    if (price <= 0) {
                        price = 0;
                        break;
                    }
                }

                if (price > 0) {
                    if (player.getHealth() + player.getAbsorptionAmount() <= price) continue;
                    if (!player.hurt(DamageSource.OUT_OF_WORLD, price)) {
                        continue;
                    }
                }
            }
            var stack = possibleTransfusion.destItem().copy();
            stack.setCount(1);
            if (item.getCount() > 1) {
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
            } else {
                player.setItemInHand(hand, stack);
            }

            var instance = IdolRenderer.renderEntities.get(et);
            if (instance instanceof LivingEntity le) {
                player.level.playSound(null, player, ((ISoundGetter) le).publicGetHurtSound(DamageSource.GENERIC), SoundSource.NEUTRAL, 1, 1);
            }

            return true;
        }
        return false;
    }

    private static boolean tryMakeAllayContract(ServerPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return false;

        var pos = player.blockPosition().below();
        if (hitResult != null) {
            pos = hitResult.getBlockPos();
        }

        var contractTag = new CompoundTag();
        var taskTag = new CompoundTag();

        if (player.getMainHandItem().is(Items.PAPER) && player.getMainHandItem().getCount() == 1) {
            taskTag.putString("type", "take");
            if (!player.getOffhandItem().isEmpty()) {
                taskTag.putInt("filter_0", Item.getId(player.getOffhandItem().getItem()));
            }
            taskTag.putIntArray("position_0", new int[]{pos.getX(), pos.getY(), pos.getZ()});

            contractTag.put("task_0", taskTag);
        } else if (player.getMainHandItem().is(OtherverseItems.CONTRACT.get())) {
            contractTag = player.getMainHandItem().getTag().getCompound("contract");
            taskTag.putString("type", "put");
            if (!player.getOffhandItem().isEmpty()) {
                taskTag.putInt("filter_0", Item.getId(player.getOffhandItem().getItem()));
            }
            taskTag.putIntArray("position_0", new int[]{pos.getX(), pos.getY(), pos.getZ()});

            contractTag.put("task_1", taskTag);
        }

        ItemStack contract = OtherverseItems.CONTRACT.get().getDefaultInstance();
        contract.getOrCreateTag().put("contract", contractTag);
        player.setItemInHand(InteractionHand.MAIN_HAND, contract);
        player.level.playSound(null, player, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1f, 1f);
        return true;
    }

    public static EntityType<?> getEntityTypeFromTag(CompoundTag tag) {
        return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(tag.getString("entity_type_namespace"), tag.getString("entity_type_path")));
    }

    public static boolean isFamiliar(Entity entity) {
        if (entity == null) return false;
        return !getPractitionerForFamiliar(entity).isEmpty();
    }

    public static void onInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        var target = event.getTarget();
        if (target instanceof EnderDragonPart ep) target = ep.parentMob;
        if (!getPractitionerForFamiliar(target).equals(event.getEntity().getGameProfile().getName())) return;
        if (!(target instanceof Mob mob)) return;
        for (var g : mob.goalSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof BoundGoal bg) {
                bg.toggleFamiliarBehavior();
                return;
            }
        }
    }
}
