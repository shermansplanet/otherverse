package com.shermansplanet.otherverse.binding;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class BindingOrFleshbinding {

    public EntityType entityType;
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean isIdol;
    private ItemStack idolItem;
    public BlockPos position;
    public Level level;
    private CompoundTag idolTag;
    private BindingInfo bindingInfo;
    public final int efficiencyReduction;
    private final boolean isCreative;
    private ChalkCircle circle;

    public BindingOrFleshbinding(ChalkCircle circle) {
        var item = circle.item;
        this.circle = circle;
        level = circle.getLevel();
        position = circle.getPos();
        entityType = IdolItem.getType(item);
        idolItem = item;
        isCreative = !item.hasTag() || !item.getTag().contains("mob_data");
        if (!isCreative) {
            idolTag = item.getTag().getCompound("mob_data").getCompound("EntityTag");
            var hasPerk = false;
            if (level instanceof ServerLevel sl) {
                var demesne = DemesnesManager.getData(sl, position);
                if (demesne != null && demesne.getPerkLevel(DemesnesManager.DemesnePerk.IDOLS) != 0) hasPerk = true;
            }
            efficiencyReduction = (hasPerk || item.getTag().getString("material").equals("minecraft:diamond")) ? 1 : 3;
        } else {
            efficiencyReduction = 1;
        }
        isIdol = true;
    }

    public BindingOrFleshbinding(BindingInfo info) {
        entityType = info.mob.getType();
        bindingInfo = info;
        position = info.mob.blockPosition();
        level = info.mob.level;
        efficiencyReduction = 1;
        isCreative = false;
        isIdol = false;
    }

    public static BindingOrFleshbinding getFromPosition(ServerLevel sl, BlockPos bp) {
        var binding = DiagramManager.getBindingOrBoundMobAt(sl, bp);
        if (binding != null && binding.mob != null) {
            return new BindingOrFleshbinding(binding);
        }
        if (sl.getBlockEntity(bp) instanceof ChalkCircle cc && cc.getItem().is(OtherverseItems.IDOL.get())) {
            return new BindingOrFleshbinding(cc);
        }
        return null;
    }

    public int getHealth() {
        return isCreative ? 10 : (int) (isIdol ? idolTag.getFloat("Health") : bindingInfo.mob.getHealth());
    }

    public void changeHealth(int delta, ServerLevel sl) {
        if (isCreative) return;
        int currentHealth = getHealth();
        int newHealth = currentHealth + delta;
        if (isIdol) {
            idolTag.putFloat("Health", newHealth);
            if (delta < 0) {
                var instance = IdolRenderer.renderEntities.get(entityType);
                if (instance instanceof LivingEntity le) {
                    sl.playSound(null, position, ((ISoundGetter) le).publicGetHurtSound(DamageSource.GENERIC), SoundSource.NEUTRAL, 1, 1);
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                            position.getX() + 0.5,
                            position.getY() + 0.3,
                            position.getZ() + 0.5,
                            1, 0, 0, 0, 0.1
                    );
                }
            }
        } else {
            if (delta < 0) {
                bindingInfo.mob.hurt(DamageSource.OUT_OF_WORLD, -delta);
            } else {
                bindingInfo.mob.heal(delta);
            }
        }
        if (delta > 0) {
            var r = sl.random;
            for (var i = 0; i < 6; i++) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        position.getX() + r.nextDouble(),
                        position.getY() + r.nextDouble(),
                        position.getZ() + r.nextDouble(),
                        1, 0, 0, 0, 0.1
                );
            }
        }
    }

    public int getMaxHealth() {
        return isIdol ? (int) DefaultAttributes.getSupplier(entityType).getValue(Attributes.MAX_HEALTH) : (int) bindingInfo.mob.getMaxHealth();
    }

    public int invulnerableTime() {
        return isIdol ? circle.cooldownTicks : bindingInfo.mob.invulnerableTime;
    }

    public void setInvulnerableTime(int invulnerableTime) {
        if (isIdol) {
            circle.cooldownTicks = invulnerableTime;
        } else {
            bindingInfo.mob.invulnerableTime = invulnerableTime;
        }
    }

    public boolean canBeHealed() {
        if (level instanceof ServerLevel sl) {
            var demesne = DemesnesManager.getData(sl, position);
            if (demesne != null && demesne.getPerkLevel(DemesnesManager.DemesnePerk.IDOLS) != 0) return true;
        }
        if (isIdol && !isCreative) {
            var mat = idolItem.getTag().getString("material");
            return mat.equals("minecraft:diamond") || mat.equals("minecraft:lapis_block");
        }
        return true;
    }
}
