package com.shermansplanet.otherverse.familiar;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.OtherverseConfig;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.PowerSource;
import com.shermansplanet.otherverse.spirits.SpiritAffinityTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.slf4j.Logger;

import java.util.HashSet;

public class FamiliarNameTagItem extends NameTagItem {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FamiliarNameTagItem(Properties p_42952_) {
        super(p_42952_);
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!stack.hasCustomHoverName()) {
            LOGGER.debug("Name tag has no name!");
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (!FamiliarManager.isEligibleFamiliar(entity)) {
            LOGGER.debug(entity.getPersistentData().hasUUID("bindingId") ? "has binding" : "no binding");
            LOGGER.debug(entity.getType() + " cannot be a familiar");
            return InteractionResult.PASS;
        }

        if (!FamiliarManager.getFamiliarData(player).isEmpty()) {
            if (!player.isCreative()) {
                if (OtherverseConfig.CAN_REDO_RITUALS.get()) {
                    SpiritAffinityTracker.decreaseAllAffinities(sp);
                } else {
                    return InteractionResult.PASS;
                }
            }
            FamiliarManager.abandonFamiliar(sp);
        }

        var sl = (ServerLevel) player.level();
        var data = DiagramManager.getOrCreateLevelData(sl.getServer().overworld());
        var binding = data.bindingsById.get(entity.getPersistentData().getUUID("bindingId"));
        if (binding == null || binding.mob != entity) return InteractionResult.PASS;
        var diagram = binding.getFocus().getDiagram();
        var blockFocus = data.allBlockFoci.get(entity.blockPosition());
        if (blockFocus != null) diagram = blockFocus.getDiagram();
        var playerSet = new HashSet<EntityType<?>>();
        playerSet.add(EntityType.PLAYER);
        var requiredPower = Math.max(1, (int) (entity.getMaxHealth() / (binding.isPositive ? 4 : 2)));
        BlockFocus otherfocus = DiagramManager.getFocusInBoundingBox(DiagramManager.getOrCreateLevelData(sl), entity.getBoundingBox());
        if (player.isCreative()
                || diagram.trySpendPower(sl, binding.position, requiredPower * PowerSource.POWER_FROM_SELF, playerSet)
                || (otherfocus != null && otherfocus.getDiagram() != null &&
                otherfocus.getDiagram().trySpendPower(sl, otherfocus.getPos(), requiredPower * PowerSource.POWER_FROM_SELF, playerSet))) {
            entity.setCustomName(stack.getHoverName());
            FamiliarManager.makeFamiliar(entity, player);
        } else {
            player.sendSystemMessage(Component.literal("This mob needs to be influenced by " + requiredPower + " Self to complete this ritual."));
        }
        return InteractionResult.CONSUME;
    }
}
