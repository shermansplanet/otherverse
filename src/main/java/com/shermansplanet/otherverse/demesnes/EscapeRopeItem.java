package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class EscapeRopeItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int[] cooldowns = new int[]{20, 5, 1};
    private static final String cooldownKey = "escape_rope_cooldown";

    public EscapeRopeItem(Properties p_41383_) {
        super(p_41383_);
    }

    public static void removeCooldown(ServerPlayer player) {
        player.getPersistentData().remove(cooldownKey);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return onUse(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), ctx.getClickLocation(), true).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return onUse(level, player, hand, player.position(), false);
    }

    private InteractionResultHolder<ItemStack> onUse(Level level, Player player, InteractionHand hand, Vec3 pos, boolean fromBlock) {
        var demesne = DemesnesManager.getData(player);
        var itemstack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)
                || demesne == null) {
            return InteractionResultHolder.success(itemstack);
        }

        var playerLevelId = DiagramManager.getDimensionHash(level);
        if (DemesnesManager.getData(sl, player.blockPosition()) == demesne) {
            var tag = itemstack.getOrCreateTag();
            tag.putFloat("escape_rope_x", (float) pos.x);
            tag.putFloat("escape_rope_y", (float) pos.y);
            tag.putFloat("escape_rope_z", (float) pos.z);
            tag.putInt("level_id", playerLevelId);
            for (var i = 0; i < 8; i++) {
                sl.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 1, 0, 0.1f, 0, 0.02D);
            }
            player.displayClientMessage((Component.literal(fromBlock
                    ? "Destination is set to clicked position."
                    : "Destination is set to your current position.")), true);
            return InteractionResultHolder.consume(itemstack);
        }

        if (demesne.getPerkLevel(DemesnesManager.DemesnePerk.HOME) == 0)
            return InteractionResultHolder.success(itemstack);

        long currentTime = System.currentTimeMillis();
        if (!player.getAbilities().instabuild && player.getPersistentData().contains(cooldownKey)) {
            var t = player.getPersistentData().getLong(cooldownKey);
            if (currentTime < t) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(t - currentTime);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(t - currentTime - TimeUnit.MINUTES.toMillis(minutes));
                player.displayClientMessage((Component.literal(
                        "You will be able to use your escape rope in " + minutes + ":" + seconds)), false);
                return InteractionResultHolder.pass(itemstack);
            }
        }

        var x = (demesne.minPos.getX() + demesne.maxPos.getX()) / 2f;
        var z = (demesne.minPos.getZ() + demesne.maxPos.getZ()) / 2f;
        var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z) + 1f;
        var fromSetPosition = false;

        var destLevel = DiagramManager.levelFromHash(sl, demesne.levelId);

        if (itemstack.hasTag() && itemstack.getTag().contains("escape_rope_x")) {
            var newx = itemstack.getTag().getFloat("escape_rope_x");
            var newy = itemstack.getTag().getFloat("escape_rope_y");
            var newz = itemstack.getTag().getFloat("escape_rope_z");
            var levelId = itemstack.getTag().getInt("level_id");
            if (levelId == demesne.levelId && DemesnesManager.getData(destLevel, new BlockPos(newx, newy, newz)) == demesne) {
                x = newx;
                y = newy;
                z = newz;
                fromSetPosition = true;
            }
        }

        if (playerLevelId != demesne.levelId) {
            var playerPos = new Vec3(x, y, z);
            boolean finalFromSetPosition = fromSetPosition;
            player = (Player) player.changeDimension(destLevel, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                    return repositionEntity.apply(false);
                }

                @Override
                public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                    var y = finalFromSetPosition ? playerPos.y
                            : destWorld.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) (playerPos.x), (int) (playerPos.y)) + 1f;
                    var destPos = new Vec3(playerPos.x, y, playerPos.z);
                    return new PortalInfo(destPos, Vec3.ZERO, entity.getYRot(), entity.getXRot());
                }
            });
        } else {
            player.teleportTo(x, y, z);
            sl.playSound(null, x, y, z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1, 1);
        }

        player.getPersistentData().putLong(cooldownKey, currentTime +
                cooldowns[demesne.getPerkLevel(DemesnesManager.DemesnePerk.HOME) - 1] * 1000 * 60);

        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.consume(itemstack);
    }

}
