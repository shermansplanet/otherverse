package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.diagrams.SelfManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class SelfDrainAttackGoal extends MeleeAttackGoal {
    public SelfDrainAttackGoal(PathfinderMob p_25552_, double p_25553_, boolean p_25554_) {
        super(p_25552_, p_25553_, p_25554_);
    }

    protected void checkAndPerformAttack(LivingEntity le, double p_25558_) {
        double d0 = this.getAttackReachSqr(le);
        if (p_25558_ <= d0 && this.getTicksUntilNextAttack() <= 0) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            SelfManager.SelfDrainAttack(this.mob, le);
        }
    }
}
