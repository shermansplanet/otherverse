package com.shermansplanet.otherverse.mixin;

import com.mojang.authlib.GameProfile;
import com.shermansplanet.otherverse.familiar.ITextureSetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerInjector extends Player implements ITextureSetter {
    private ResourceLocation overrideTexture;

    public AbstractClientPlayerInjector(Level p_219727_, BlockPos p_219728_, float p_219729_, GameProfile p_219730_, @Nullable ProfilePublicKey p_219731_) {
        super(p_219727_, p_219728_, p_219729_, p_219730_, p_219731_);
    }

    @Override
    public boolean isSpectator() {
        PlayerInfo playerinfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getGameProfile().getId());
        return playerinfo != null && playerinfo.getGameMode() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        PlayerInfo playerinfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getGameProfile().getId());
        return playerinfo != null && playerinfo.getGameMode() == GameType.CREATIVE;
    }

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    public void onGetTextureLocation(CallbackInfoReturnable<ResourceLocation> ci) {
        if (overrideTexture != null) {
            ci.setReturnValue(overrideTexture);
            ci.cancel();
        }
    }

    @Override
    public void setTexture(ResourceLocation rl) {
        if (overrideTexture != null && rl == null) {
            Minecraft.getInstance().getTextureManager().release(overrideTexture);
        }
        overrideTexture = rl;
    }

    @Shadow
    protected PlayerInfo getPlayerInfo() {
        return null;
    }

    public boolean isSkinLoadedForReskin() {
        var playerInfo = getPlayerInfo();
        if (playerInfo == null || !playerInfo.isSkinLoaded()) return false;
        return playerInfo.getSkinLocation() != DefaultPlayerSkin.getDefaultSkin(playerInfo.getProfile().getId());
    }
}
