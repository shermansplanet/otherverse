package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritAffinityTracker;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityInjector extends Entity {
    public ItemEntityInjector(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Shadow
    public Entity getOwner() {
        return null;
    }

    @Shadow
    public ItemStack getItem() {
        return null;
    }

    @Shadow
    public void setItem(ItemStack p_32046_) {
    }

    @Inject(method = "setUnderwaterMovement", at = @At("RETURN"))
    private void onSetUnderwaterMovement(CallbackInfo ci) {
        //if (level.getGameTime() % 4 != 0) return;
        var item = getItem();
        if (item.is(OtherverseItems.SPINDLE_BLOODY.get())) {
            setItem(new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "spindle_white")), item.getCount()));
            return;
        }

        var throwingEntity = getOwner();
        if (!(throwingEntity instanceof ServerPlayer sp)) return;

        if (item.isEmpty() || !item.hasTag() || !item.getTag().contains("hallow")) return;

        var state = level().getBlockState(blockPosition());
        if (!state.getFluidState().is(Fluids.WATER)) return;
        var maxy = seek(1) + 1;
        var miny = seek(-1);

        var spiritLabel = item.getTag().getCompound("hallow").getString("spirit_type");
        var affinity = SpiritAffinityTracker.getAffinity(sp.getGameProfile().getName(), Spirits.spiritsByLabel.get(spiritLabel));
        var lerp = (affinity + 2.5f) / 5f;
        var targetY = miny + lerp * (maxy - miny);
        var dy = (targetY - position().y) * 0.01f;

        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(movement.x, movement.y + dy, movement.z));
    }

    private int seek(int dy) {
        var pos = blockPosition().mutable();
        int y = pos.getY();
        for (var i = 1; i < 64; i++) {
            pos.setY(y + i * dy);
            var state = level().getBlockState(pos);
            if (!state.getFluidState().is(Fluids.WATER)) return y + (i - 1) * dy;
        }
        return y + 64 * dy;
    }
}
