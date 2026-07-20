package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.spirits.SpiritLabeler;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.EnumSet;

public class TyphloticZombie extends Zombie {
    public TyphloticZombie(EntityType<? extends Zombie> p_34271_, Level p_34272_) {
        super(p_34271_, p_34272_);
    }

    private ItemEntity itemTarget;
    private int pathfindCooldown;
    private int healCooldown;

    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new TyphloticZombieAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, le -> {
            if (!(le instanceof Player p)) return false;
            for (var item : p.getInventory().items) {
                if (isFreshFood(item)) return true;
            }
            return false;
        }));
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> et, LevelAccessor level, MobSpawnType spawnType, BlockPos blockPos, RandomSource r) {
        return true;
    }

    public static boolean isFreshFood(ItemStack item) {
        if (SpiritLabeler.SPIRIT_TYPE_OF.data == null) return false;
        var spirits = SpiritLabeler.getSpiritsFor(item.getItem());
        if (spirits == null) return false;
        if (spirits.containsKey(Spirits.FLESH)) return false;
        return spirits.containsKey(Spirits.FOOD);
    }

    protected boolean convertsInWater() {
        return false;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.WITHER)) {
            if (healCooldown > 0) return false;
            this.heal(amount);
            healCooldown = 20;
            if (this.level() instanceof ServerLevel sl) {
                var pos = position();
                var r = sl.random;
                for (var i = 0; i < 6; i++) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            pos.x + r.nextDouble() - 0.5f,
                            pos.y + r.nextDouble() * 2,
                            pos.z + r.nextDouble() - 0.5f,
                            1, 0, 0, 0, 0.1
                    );
                }
            }
            return false;
        }
        if (source.getEntity() instanceof LivingEntity le) {
            setTarget(le);
            setAggressive(true);
        }
        return super.hurt(source, amount);
    }

    public void aiStep() {
        super.aiStep();
        if (healCooldown > 0) healCooldown--;
    }

    public void customServerAiStep(){
        if (!(level() instanceof ServerLevel sl)) return;
        if (itemTarget != null && !itemTarget.isRemoved() && !itemTarget.getItem().is(Items.ROTTEN_FLESH)) {
            navigation.moveTo(itemTarget, 1);
            return;
        }
        pathfindCooldown--;
        if (pathfindCooldown <= 0) {
            pathfindCooldown = random.nextInt(10, 20);
            for (var e : sl.getEntities(EntityTypeTest.forClass(ItemEntity.class), getBoundingBox().inflate(16), t -> isFreshFood(t.getItem()))) {
                itemTarget = e;
                return;
            }
        }
    }

    private class TyphloticZombieAttackGoal extends SelfDrainAttackGoal {
        private final Zombie zombie;
        private int raiseArmTicks;

        public TyphloticZombieAttackGoal(Zombie p_26019_, double p_26020_, boolean p_26021_) {
            super(p_26019_, p_26020_, p_26021_);
            this.zombie = p_26019_;
        }

        public void start() {
            super.start();
            this.raiseArmTicks = 0;
        }

        public void stop() {
            super.stop();
            this.zombie.setAggressive(false);
        }

        public void tick() {
            super.tick();
            ++this.raiseArmTicks;
            if (this.raiseArmTicks >= 5 && this.getTicksUntilNextAttack() < this.getAttackInterval() / 2) {
                this.zombie.setAggressive(true);
            } else {
                this.zombie.setAggressive(false);
            }

        }
    }
}
