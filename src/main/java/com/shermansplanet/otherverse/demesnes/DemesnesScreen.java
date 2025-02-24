package com.shermansplanet.otherverse.demesnes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DemesnesScreen extends AbstractContainerScreen<DemesnesMenu> {

    private final DemesnesMenu menu;
    static final ResourceLocation SCREEN_LOCATION = new ResourceLocation(Otherverse.MODID, "textures/gui/demesnes.png");
    private final Inventory inventory;

    private List<PerkButton> children = new ArrayList<>();
    private HashSet<DemesnesManager.DemesnePerk> enabledPerks;
    private final DemesnesManager.DemesnePerk[] perkValues = DemesnesManager.DemesnePerk.values();
    private int claimedPerkCount;

    private class PerkButton extends ImageButton {
        public final DemesnesManager.DemesnePerk perk;

        public PerkButton(DemesnesManager.DemesnePerk perk, boolean isActive, boolean sanction, boolean isClickable, int perkVal) {
            super(0, 0, sanction ? 17 : 22, sanction ? 18 : 23, sanction ? 239 : 234,
                    sanction ? (isActive ? 158 : 120) : (isActive ? 72 : 0), sanction ? 19 : 24,
                    SCREEN_LOCATION, 256, 256, (p_170074_) -> {
                        activatePerk(perk);
                    }, new Button.OnTooltip() {
                        public void onTooltip(Button button1, PoseStack poseStack, int x, int y) {
                            var tooltip = new ArrayList<>(perk.tooltip);
                            if (perk.isSanction) {
                                tooltip.add(Component.literal("Currently granted to: ").append(
                                        Component.literal(isActive ? "only those with writs and their bound mobs" : "anyone")
                                                .withStyle(Style.EMPTY.withColor(0x90c833))).append(Component.literal(".")));
                                tooltip.add(Component.literal("Click to toggle.").withStyle(Style.EMPTY.withColor(0x888888)));
                                if (isActive) {
                                    tooltip.add(Component.literal("Shift-click to produce a writ.").withStyle(Style.EMPTY.withColor(0x888888)));
                                }
                            } else if (isClickable) {
                                var cost = DemesnesManager.getLevelCost(claimedPerkCount);
                                if (cost > Minecraft.getInstance().player.experienceLevel) {
                                    tooltip.add(Component.literal("You need " + cost + " levels to claim this perk!").withStyle(Style.EMPTY.withColor(0x888888)));
                                } else {
                                    tooltip.add(Component.literal("Click to claim. This will cost " + cost + " levels.").withStyle(Style.EMPTY.withColor(0x888888)));
                                }
                                tooltip.add(Component.literal("You have claimed " + claimedPerkCount + " out of " + DemesnesManager.MAX_CHOICES + " perks.").withStyle(Style.EMPTY.withColor(0x888888)));
                            }
                            renderTooltip(poseStack, tooltip, Optional.empty(), x, y);
                        }

                        public void narrateTooltip(Consumer<Component> p_239523_) {
                        }
                    }, Component.literal(perk.name));
            this.perk = perk;
            this.active = isClickable || isActive;
        }
    }

    public DemesnesScreen(DemesnesMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.menu = menu;
        this.inventory = inventory;
        refreshButtons();
    }

    private void refreshButtons() {
        enabledPerks = new HashSet<>();
        claimedPerkCount = 0;
        for (var perk : perkValues) {
            var perkVal = menu.perks.get(perk).get();
            if (perkVal > 0) {
                enabledPerks.add(perk);
            }
            if (!perk.isSanction) {
                claimedPerkCount += perkVal;
            }
        }
        children.clear();
        for (var perk : perkValues) {
            var isUnlocked = enabledPerks.contains(perk);
            var isClickable = perk.isSanction || isUnlocked
                    || perk == DemesnesManager.DemesnePerk.SPAWN_SET
                    || perk == DemesnesManager.DemesnePerk.PROTECTION
                    || perk == DemesnesManager.DemesnePerk.BEACON_HIDE
                    || perk == DemesnesManager.DemesnePerk.RECOVERY;
            if (!isClickable) {
                for (var connected : DemesnesManager.connections.get(perk)) {
                    if (!enabledPerks.contains(connected)) continue;
                    isClickable = true;
                    break;
                }
            }
            var perkVal = menu.perks.get(perk).get();
            if (!perk.isSanction && perkVal == perk.maxValue) {
                isClickable = false;
            }
            isClickable = isClickable && (claimedPerkCount < DemesnesManager.MAX_CHOICES || perk.isSanction);
            var button = new PerkButton(perk, isUnlocked, perk.isSanction, isClickable, perkVal);
            children.add(button);
        }
    }

    private void activatePerk(DemesnesManager.DemesnePerk perk) {
        var player = Minecraft.getInstance().player;
        var perkSlot = menu.perks.get(perk);
        var oldVal = perkSlot.get();
        var delta = 1;
        if (player.getAbilities().instabuild) {
            if (hasShiftDown() && oldVal > 0) {
                delta = -1;
            }
        } else if (!perk.isSanction) {
            if (claimedPerkCount >= DemesnesManager.MAX_CHOICES) return;
            if (DemesnesManager.getLevelCost(claimedPerkCount) > player.experienceLevel) return;
        }
        var newVal = 0;
        if (perk.isSanction && hasShiftDown()) {
            var msg = new DemesnesPerkMessage(perk.ordinal(), -1, menu.getBeaconPosition());
            OtherversePacketHandler.INSTANCE.sendToServer(msg);
            return;
        }
        if (!perk.isSanction || oldVal != 1) {
            if (oldVal + delta > perk.maxValue) return;
            newVal = oldVal + delta;
        }
        perkSlot.set(newVal);
        refreshButtons();
        OtherversePacketHandler.INSTANCE.sendToServer(new DemesnesPerkMessage(perk.ordinal(), newVal, menu.getBeaconPosition()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    public void containerTick() {
        for (var perk : menu.perks.values()) {
            if (perk.checkAndClearUpdateFlag()) {
                refreshButtons();
                break;
            }
        }
    }

    public void render(PoseStack pose, int p_95529_, int p_95530_, float p_95531_) {
        this.renderBackground(pose);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SCREEN_LOCATION);
        int i = (this.width - 232) / 2;
        int j = (this.height - 232) / 2;
        this.blit(pose, i, j, 0, 0, 232, 232);
        for (var child : children) {
            child.x = i + child.perk.x - 1;
            child.y = j + child.perk.y - 1;
            child.render(pose, p_95529_, p_95530_, p_95531_);
            var perkVal = menu.perks.get(child.perk).get();
            if (perkVal > 0 && child.perk.maxValue > 1) {
                this.font.draw(pose, String.valueOf(perkVal), child.x + 14, child.y + 17, 0);
                this.font.draw(pose, String.valueOf(perkVal), child.x + 12, child.y + 17, 0);
                this.font.draw(pose, String.valueOf(perkVal), child.x + 13, child.y + 16, 0xbcff00);
            }
        }

        int midx = this.width / 2 - 1;
        int midy = this.height / 2 - 14;

        drawSpiritType(this, menu.spiritType.get(), midx, midy);
    }

    public static void drawSpiritType(GuiComponent screen, int spiritTypeId, int midx, int midy){
        if (spiritTypeId > -1) {
            var spiritType = Spirits.spiritsById.get(spiritTypeId);
            var item = Spirits.spiritItems.get(spiritType).get().getDefaultInstance();
            Minecraft.getInstance().getItemRenderer().renderGuiItem(item, midx - 8, midy - 94);
            Minecraft.getInstance().getItemRenderer().renderGuiItem(item, midx - 7, midy - 94);
            Minecraft.getInstance().getItemRenderer().renderGuiItem(item, midx - 8, midy - 93);
            Minecraft.getInstance().getItemRenderer().renderGuiItem(item, midx - 7, midy - 93);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, SCREEN_LOCATION);
        }
    }

    @Override
    protected void renderBg(PoseStack p_97787_, float p_97788_, int p_97789_, int p_97790_) {

    }

    @Override
    public DemesnesMenu getMenu() {
        return menu;
    }
}
