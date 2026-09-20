package com.shermansplanet.otherverse;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.binding.BindingRenderer;
import com.shermansplanet.otherverse.binding.GiveItemMessage;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.others.Buzzed;
import com.shermansplanet.otherverse.others.BuzzedSoundInstance;
import com.shermansplanet.otherverse.spirits.HallowTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.ICuriosScreen;

import java.awt.*;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEvents {

    private static final HashMap<UUID, Optional<LivingEntity>> bossCache = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        onMouseEvent(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPress(ScreenEvent.MouseButtonPressed.Pre event) {
        onMouseEvent(event);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        DiagramManager.clearClientData();
        PracticeWorldManager.noJeiPending = false;
        PracticeWorldManager.worldSetUp = false;
        BindingRenderer.resetData();
        ReskinManager.resetData();
        HallowTextureManager.resetData();
    }

    private static void onMouseEvent(ScreenEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (containerScreen instanceof ICuriosScreen) return;
        var menu = mc.player.containerMenu;
        if (menu.containerId == mc.player.inventoryMenu.containerId) return;
        var stack = menu.getCarried();
        if (ImplementManager.isImplement(stack)) event.setCanceled(true);
        var slot = containerScreen.getSlotUnderMouse();
        if (slot == null) return;
        if (ImplementManager.isImplement(slot.getItem())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderFog(ViewportEvent.RenderFog event) {
        var player = Minecraft.getInstance().player;
        if (!player.level().dimension().location().getPath().equals("ruins")) return;
        if (player.getEyeInFluidType() != net.minecraftforge.common.ForgeMod.EMPTY_TYPE.get()) return;
        if (player.isInPowderSnow) return;
        event.setNearPlaneDistance(-32);
        event.setFarPlaneDistance(200);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractionKeyTriggered(InputEvent.Key event) {
        if (event.getKey() != Keybindings.KEY_GIVE.getKey().getValue()) return;
        if (event.getAction() != InputConstants.PRESS) return;
        var hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;
        var e = ((EntityHitResult) hit).getEntity();
        if (!(e instanceof LivingEntity le) || !BindingRenderer.isBound(le)) return;
        Keybindings.KEY_GIVE.consumeClick();
        if (Keybindings.KEY_GIVE.same(Minecraft.getInstance().options.keyDrop)) {
            Minecraft.getInstance().options.keyDrop.consumeClick();
        }
        OtherversePacketHandler.INSTANCE.sendToServer(new GiveItemMessage(le.getId()));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (Keybindings.KEY_SIGHT.consumeClick()) {
            if (!canToggleSight()) return;
            SightManager.toggleSight();
        }
    }

    private static boolean canToggleSight() {
        var sightItems = OtherverseConfig.getSightItems();
        if (sightItems.isEmpty()) return true;
        var player = Minecraft.getInstance().player;
        for (var item : player.getInventory().items) {
            if (sightItems.contains(item.getItem())) return true;
        }
        var inv = CuriosApi.getCuriosInventory(player);
        if (!inv.isPresent() || inv.resolve().isEmpty()) return false;
        for (var curioInventory : inv.resolve().get().getCurios().values()) {
            var stacks = curioInventory.getStacks();
            for (var i = 0; i < stacks.getSlots(); i++) {
                if (sightItems.contains(stacks.getStackInSlot(i).getItem())) return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderBoss(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var bossUUID = event.getBossEvent().getId();
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        Optional<LivingEntity> entityHolder = Optional.empty();
        if (bossCache.containsKey(bossUUID)) {
            entityHolder = bossCache.get(bossUUID);
        } else {
            var toMatch = event.getBossEvent().getName().getString();
            for (var e : level.entitiesForRendering()) {
                if (!e.getDisplayName().getString().equals(toMatch)) continue;
                if (e instanceof LivingEntity le) {
                    entityHolder = Optional.of(le);
                }
                break;
            }
            bossCache.put(bossUUID, entityHolder);
        }
        if (entityHolder.isEmpty()) return;
        var entity = entityHolder.get();
        if (BindingRenderer.isBound(entity)) event.setCanceled(true);
    }

    public static void addBuzzed(Buzzed buzzed) {
        boolean flag = buzzed.getTarget() != null;
        BuzzedSoundInstance beesoundinstance;
        if (flag) {
            beesoundinstance = new BuzzedSoundInstance.BuzzedAggressiveSoundInstance(buzzed);
        } else {
            beesoundinstance = new BuzzedSoundInstance.BuzzedFlyingSoundInstance(buzzed);
        }

        Minecraft.getInstance().getSoundManager().queueTickingSound(beesoundinstance);
    }
}
