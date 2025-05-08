package com.shermansplanet.otherverse.mixin;

import com.shermansplanet.otherverse.implement.ImplementManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackInjector extends net.minecraftforge.common.capabilities.CapabilityProvider<ItemStack> implements net.minecraftforge.common.extensions.IForgeItemStack {

    @Shadow
    private CompoundTag tag;

    protected ItemStackInjector(Class<ItemStack> baseClass) {
        super(baseClass);
    }

    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    public void onBarVisible(CallbackInfoReturnable<Boolean> ci) {
        if (tag == null) return;
        if (tag.contains("implement_max_uses") || tag.contains("connection_blocker_total")) {
            ci.setReturnValue(true);
            ci.cancel();
        }
    }

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    public void onBarWidth(CallbackInfoReturnable<Integer> ci) {
        if (tag == null) return;
        if (tag.contains("implement_max_uses")) {
            ci.setReturnValue(Math.round(tag.getInt("implement_remaining_uses") * 13.0F
                    / tag.getInt("implement_max_uses")));
            ci.cancel();
        } else if (tag.contains("connection_blocker_total")) {
            ci.setReturnValue(Math.round(tag.getInt("connection_blocker_remaining") * 13.0F
                    / tag.getInt("connection_blocker_total")));
            ci.cancel();
        }
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    public void onBarColor(CallbackInfoReturnable<Integer> ci) {
        if (tag == null) return;
        if (tag.contains("implement_max_uses")) {
            ci.setReturnValue(ImplementManager.IMPLEMENT_UI_COLOR);
            ci.cancel();
        } else if (tag.contains("connection_blocker_total")) {
            var remainingAmount = tag.getInt("connection_blocker_remaining") * 1f / tag.getInt("connection_blocker_total");
            ci.setReturnValue(Mth.hsvToRgb(remainingAmount / 3.0F, 1.0F, 1.0F));
            ci.cancel();
        }
    }
}
