package com.shermansplanet.otherverse;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingUpdateMessage;
import com.shermansplanet.otherverse.demesnes.*;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.ChalkCircleScreen;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.familiar.SummonFamiliarMessage;
import com.shermansplanet.otherverse.implement.FetchImplementMessage;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.implement.SyncPracticeDataMessage;
import com.shermansplanet.otherverse.integrations.jei.OtherverseJeiPlugin;
import com.shermansplanet.otherverse.ruins.RuinsManager;
import com.shermansplanet.otherverse.ruins.ShockLightningMessage;
import com.shermansplanet.otherverse.spirits.Chronomancy;
import com.shermansplanet.otherverse.spirits.ColorSpiritMessage;
import com.shermansplanet.otherverse.spirits.HallowUpdateMessage;
import com.shermansplanet.otherverse.spirits.OverflowMessage;
import com.shermansplanet.otherverse.sympathy.SympathyRangeUpdateMessage;
import com.shermansplanet.otherverse.sympathy.SympathySelectRenderer;
import com.shermansplanet.otherverse.sympathy.SympathyUpdateMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class OtherversePacketHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Otherverse.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                HallowUpdateMessage.class,
                HallowUpdateMessage::encode,
                HallowUpdateMessage::decode,
                OtherversePacketHandler::handleHallowUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                BindingUpdateMessage.class,
                BindingUpdateMessage::encode,
                BindingUpdateMessage::decode,
                OtherversePacketHandler::handleBindingUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                ColorSpiritMessage.class,
                ColorSpiritMessage::encode,
                ColorSpiritMessage::decode,
                OtherversePacketHandler::handleColorUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                FetchImplementMessage.class,
                FetchImplementMessage::encode,
                FetchImplementMessage::decode,
                OtherversePacketHandler::handleImplementRequest
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                SummonFamiliarMessage.class,
                SummonFamiliarMessage::encode,
                SummonFamiliarMessage::decode,
                OtherversePacketHandler::handleFamiliarSummon
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                ShockLightningMessage.class,
                ShockLightningMessage::encode,
                ShockLightningMessage::decode,
                OtherversePacketHandler::handleShockUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                SyncPracticeDataMessage.class,
                SyncPracticeDataMessage::encode,
                SyncPracticeDataMessage::decode,
                OtherversePacketHandler::syncPracticeData
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                SympathyUpdateMessage.class,
                SympathyUpdateMessage::encode,
                SympathyUpdateMessage::decode,
                OtherversePacketHandler::handleSympathyUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                DemesnesClaimStartMessage.class,
                DemesnesClaimStartMessage::encode,
                DemesnesClaimStartMessage::decode,
                OtherversePacketHandler::handleDemesnesStart
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                DemesnesClientboundMessage.class,
                DemesnesClientboundMessage::encode,
                DemesnesClientboundMessage::decode,
                OtherversePacketHandler::handleDemesnesUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                DemesnesPerkMessage.class,
                DemesnesPerkMessage::encode,
                DemesnesPerkMessage::decode,
                OtherversePacketHandler::handleDemesnesPerk
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                DemesnesWarpMessage.class,
                DemesnesWarpMessage::encode,
                DemesnesWarpMessage::decode,
                OtherversePacketHandler::tryWarp
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                SympathyRangeUpdateMessage.class,
                SympathyRangeUpdateMessage::encode,
                SympathyRangeUpdateMessage::decode,
                OtherversePacketHandler::handleSympathyRangeUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                PracticeWorldUpdateMessage.class,
                PracticeWorldUpdateMessage::encode,
                PracticeWorldUpdateMessage::decode,
                OtherversePacketHandler::handlePracticeWorldUpdate
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                OverflowMessage.class,
                OverflowMessage::encode,
                OverflowMessage::decode,
                OtherversePacketHandler::handleOverflow
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                Chronomancy.ChronomancyMessage.class,
                Chronomancy.ChronomancyMessage::encode,
                Chronomancy.ChronomancyMessage::decode,
                OtherversePacketHandler::handleChronomancyMessage
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                SightToggleMessage.class,
                SightToggleMessage::encode,
                SightToggleMessage::decode,
                SightManager::onToggleServer
        );
        OtherversePacketHandler.INSTANCE.registerMessage(
                id++,
                ChalkCircleScreen.SetInscriptionMessage.class,
                ChalkCircleScreen.SetInscriptionMessage::encode,
                ChalkCircleScreen.SetInscriptionMessage::decode,
                OtherversePacketHandler::handleInscriptionUpdate
        );
    }

    private static void handleInscriptionUpdate(ChalkCircleScreen.SetInscriptionMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var sender = ctx.get().getSender();
            if (sender == null || !(sender.serverLevel().getBlockEntity(msg.pos()) instanceof ChalkCircle cc)) return;
            cc.inscription = msg.inscription();
            cc.markUpdated();
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleChronomancyMessage(Chronomancy.ChronomancyMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OtherverseClientPacketHandler.chronomancy(msg, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleOverflow(OverflowMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OtherverseClientPacketHandler.handleOverflow(msg, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handlePracticeWorldUpdate(PracticeWorldUpdateMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OtherverseJeiPlugin.handleWorldUpdate(msg, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    /*private static  <MSG> BiConsumer<MSG, Supplier<Context>> getClientHandler(BiConsumer<MSG, Supplier<Context>> toRun){
        return (MSG msg, Supplier<Context> ctx) -> {
            ctx.get().enqueueWork(() ->
                    // Make sure it's only executed on the physical client
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,() -> () -> toRun.accept(msg, ctx))
            );
            ctx.get().setPacketHandled(true);
        };
    }*/

    private static void handleSympathyRangeUpdate(SympathyRangeUpdateMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> SympathySelectRenderer.updateRange(msg, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void tryWarp(DemesnesWarpMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DemesnesManager.tryWarpServer(msg, ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleDemesnesPerk(DemesnesPerkMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DemesnesManager.setPerkValue(msg, ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleDemesnesUpdate(DemesnesClientboundMessage message, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> DemesnesRenderer.handleEvent(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleDemesnesStart(DemesnesClaimStartMessage msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DemesnesManager.startClaim(msg, ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleSympathyUpdate(SympathyUpdateMessage message, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OtherverseClientPacketHandler.handleSympathyPacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void syncPracticeData(SyncPracticeDataMessage message, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OtherverseClientPacketHandler.syncPracticeData(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleShockUpdate(ShockLightningMessage message, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> RuinsManager.handleShockUpdate(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    private static void handleImplementRequest(FetchImplementMessage fetchImplementMessage, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ImplementManager.fetchImplement(ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleFamiliarSummon(SummonFamiliarMessage summonFamiliarMessage, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            FamiliarManager.summonFamiliar(ctx.get().getSender(), summonFamiliarMessage.hitResult);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleColorUpdate(ColorSpiritMessage colorSpiritMessage, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PracticeWorldManager.onColorsReceived(colorSpiritMessage.colorSpiritMappings, ctx.get());
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleBindingUpdate(BindingUpdateMessage message, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OtherverseClientPacketHandler.handleBindingPacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    public static void handleHallowUpdate(HallowUpdateMessage message, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> OtherverseClientPacketHandler.handleHallowPacket(message, ctx))
        );
        ctx.get().setPacketHandled(true);
    }
}
