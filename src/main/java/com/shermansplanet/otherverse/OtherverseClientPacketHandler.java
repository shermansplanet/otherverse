package com.shermansplanet.otherverse;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingRenderer;
import com.shermansplanet.otherverse.binding.BindingUpdateMessage;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.implement.SyncPracticeDataMessage;
import com.shermansplanet.otherverse.spirits.HallowUpdateMessage;
import com.shermansplanet.otherverse.spirits.OverflowMessage;
import com.shermansplanet.otherverse.spirits.ShrineHelper;
import com.shermansplanet.otherverse.sympathy.SympathyUpdateMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent.Context;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class OtherverseClientPacketHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static HashMap<Integer, ArrayList<BindingUpdateMessage>> waitingMessages = new HashMap<>();

    public static void handleBindingPacket(BindingUpdateMessage bindingUpdateMessage, Supplier<Context> ctx) {
        TransientDiagramData data = DiagramManager.getOrCreateLevelData(bindingUpdateMessage.levelValue, true);
        if (data.level != null && data.level.getEntity(bindingUpdateMessage.mobId) instanceof LivingEntity le) {
            BindingRenderer.updateBinding(le, bindingUpdateMessage);
        } else {
            if (!waitingMessages.containsKey(bindingUpdateMessage.mobId))
                waitingMessages.put(bindingUpdateMessage.mobId, new ArrayList<>());
            waitingMessages.get(bindingUpdateMessage.mobId).add(bindingUpdateMessage);
        }
    }

    public static void tryDisplayBindings(Level level) {
        for (var id : new ArrayList<>(waitingMessages.keySet())) {
            var entity = level.getEntity(id);
            if (!(entity instanceof LivingEntity le)) continue;
            LOGGER.debug("RENDERING BINDING ON REFRESH");
            var msgs = waitingMessages.get(id);
            if (msgs != null) {
                for (var msg : msgs) {
                    BindingRenderer.updateBinding(le, msg);
                }
                waitingMessages.remove(id);
            }
        }
    }

    public static void handleHallowPacket(HallowUpdateMessage hallowUpdateMessage, Supplier<Context> ctx) {
        TransientDiagramData data = DiagramManager.getOrCreateLevelData(hallowUpdateMessage.levelValue, true);
        if (hallowUpdateMessage.tag == null) {
            data.removePlacedItemTag(hallowUpdateMessage.position);
        } else {
            data.putPlacedItemTag(hallowUpdateMessage.position, hallowUpdateMessage.tag);
        }
    }

    public static void syncPracticeData(SyncPracticeDataMessage msg, Supplier<Context> ctx) {
        Minecraft.getInstance().player.getCapability(ImplementManager.PRACTICE_HANDLER).ifPresent(practice -> {
            practice.deserializeNBT(msg.nbt);
        });
    }

    public static void handleSympathyPacket(SympathyUpdateMessage message, Supplier<Context> ctx) {
        TransientDiagramData data = DiagramManager.getOrCreateLevelData(message.levelValue, true);
        data.putSympathyPosition(message.key, message.position);
    }

    public static void handleOverflow(OverflowMessage msg, Supplier<Context> ctx) {
        var level = Minecraft.getInstance().level;
        if (level == null || DiagramManager.getDimensionHash(level) != msg.dimension()) return;
        ShrineHelper.onOverdrawOrOverflow(level, msg.focusPos(), msg.spiritType(), msg.amount(), msg.overdraw(), false);
    }
}
