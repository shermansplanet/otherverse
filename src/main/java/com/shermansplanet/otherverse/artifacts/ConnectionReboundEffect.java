package com.shermansplanet.otherverse.artifacts;

import com.shermansplanet.otherverse.binding.BindingManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class ConnectionReboundEffect extends MobEffect {
    public ConnectionReboundEffect(MobEffectCategory p_19451_, int p_19452_) {
        super(p_19451_, p_19452_);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int p_19468_) {
        if(entity instanceof Player p && p.isCreative()) return;
        var level = entity.level();
        if(level.isClientSide() || level.getGameTime() % 20 != 10) return;
        for(var other : level.getEntities(entity, entity.getBoundingBox().inflate(32))){
            if(!(other instanceof Mob mob)) return;
            BindingManager.startAttacking(mob, entity);
        }
    }

    @Override
    public boolean isDurationEffectTick(int p_19455_, int p_19456_) {
        return true;
    }
}
