package com.shermansplanet.otherverse.others;

import com.shermansplanet.otherverse.spirits.SpiritLabeler;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

public class Fury extends Blaze {

    public Fury(EntityType<? extends Blaze> p_32219_, Level p_32220_) {
        super(p_32219_, p_32220_);
    }

    public static boolean checkMobSpawnRules(EntityType<? extends Mob> p_217058_, LevelAccessor p_217059_, MobSpawnType p_217060_, BlockPos p_217061_, RandomSource p_217062_) {
        return true;
    }

    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player p && !p.isHolding(s -> {
            var spirits = SpiritLabeler.getSpiritsFor(s.getItem());
            if (spirits == null) return false;
            return spirits.containsKey(Spirits.WAR);
        })) {
            return;
        }
        super.setTarget(target);
    }
}