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
}
