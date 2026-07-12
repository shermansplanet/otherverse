package com.shermansplanet.otherverse.demesnes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.binding.ContractManager;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.Diagram;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.SpiritAffinityTracker;
import com.shermansplanet.otherverse.sympathy.FateWebBlock;
import com.shermansplanet.otherverse.sympathy.SympathyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DemesnesManager {
    public static final int MAX_CHOICES = 10;
    public static final int SPIRITS_FROM_DEMESNE = 3;

    public static int getLevelCost(int claimedPerkCount) {
        return (int) Math.pow(1.6f, claimedPerkCount) + 1;
    }

    public enum DemesnePerk {
        BASE_TELEPORT(172, 85, "Omnipresence", "When using your Sight, warp between any Demesne beacons you have named in an anvil by looking towards them and right-clicking. This works even through walls."),
        BEACON_HIDE(89, 89, "Beam Hiding", "Ordinary beacons will still function even if their beams are blocked."),
        BINDING(39, 172, "Mandatory Hospitality", "Bindings within the Demesne will only require 2/3 the influence. Choosing this perk again continues to reduce it by 2/3.", MAX_CHOICES),
        BLOCK_GEN(39, 39, "Material Ad Infinitum", "Each time you choose this perk, you declare the stone or log block above this beacon as a favored material. An unlimited supply of all these materials can then be extracted from any Demesne beacon in your Demesne via hopper, bound mob, or other means.", MAX_CHOICES),
        CAGE(68, 143, "Reinforced Bindings", "Powerful hostile mobs no longer drain their bindings in your Demesne."),
        COLLAR(85, 172, "Positive Bindings", "You can give contracts to mobs you've tamed (such as wolves or horses) in your Demesne even if they're not bound."),
        FLIGHT(126, 39, "Flight", "You gain creative flight in your Demesne."),
        HOME(143, 68, "Escape Rope", "You can now use escape ropes to instantly teleport yourself back to your Demesne from anywhere, with a cooldown of 20 minutes. Choosing this perk again reduces the cooldown to 5 minutes, then 1 minute. You can read more about escape ropes in your Demesnes book.", 3),
        IDOLS(39, 126, "Idol Hands", "All wooden or lapis idols in this Demesne now act as if they were diamond."),
        MOB_REPOSITION(172, 126, "Spawn Consolidation", "If you put a spawn altar on top of a Demesne beacon, it will redirect all natural mob spawns in the bounds of your Demesne to spawn there instead. If there are multiple altars, a random one will be chosen."),
        PORTAL(172, 39, "Portals", "You can create permanent portals that anyone can use to travel to and from your Demesne, even across dimensions. Read more about how to create these portals in your Demesnes book."),
        PROTECTION(89, 122, "Protection", "You are immune to damage from falling, heat, cold, and drowning while within your Demesne."),
        RECOVERY(122, 122, "Recovery", "Your health and hunger replenish within your Demesne."),
        SCHEMATIC(39, 85, "Living Architecture", "You can create diagrams that magically place blocks they are given, quickly copying and pasting large areas. Read more about these diagrams in your Demesnes book."),
        SPAWN_SET(122, 89, "Sanctuary", "Spawns can be set in your Demesne, even if a bed would normally explode in this dimension."),
        SPIRITS(126, 172, "Ultimate Shrine", "Each time you choose this perk, you declare the spirits in the hallow above this beacon as a favored type. Demesne beacons in your Demesne can now be repeatedly drained by hallows for a renewable supply of this spirit type.", MAX_CHOICES),
        TIME(172, 172, "Chronomancy", "If two Demesne beacons are aligned vertically with nothing between them, all random ticks in your Demesne will be concentrated into the range of heights between the beacons."),
        TOOL_REPAIR(85, 39, "Hearth and Hone", "Tools in your inventory will now regain durability in your Demesne."),
        VEIN_MINE(68, 68, "Mine What's Yours", "Tools can now break multiple blocks at once in your Demesne. Shift-right-click with a tool to toggle modes. Choosing this again will increase the number of blocks broken at once from 9 to 64.", 2),
        WEATHER(143, 143, "Sky Painter", "Using dye on a Demesne beacon will shift the color of the sky in your Demesne (use dye repeatedly to shift more). Using a clock will shift the apparent time of day (shift-clicking will remove this effect)."),

        SANCTION_FIGHT(108, 66, "Sanction: Violence", "Deal damage of any sort.", true),
        SANCTION_BUILD(66, 108, "Sanction: Reshaping", "Place or destroy blocks", true),
        SANCTION_INTERACT(108, 150, "Sanction: Utility", "Interact with blocks (chests, diagrams, doors, etc.)", true),
        SANCTION_TAKE(150, 108, "Sanction: Avarice", "Pick up loose items.", true);

        public final int x, y, maxValue;
        public final String name, description;
        public final List<Component> tooltip = new ArrayList<>();
        public final boolean isSanction;

        private void constructTooltip() {
            tooltip.add(Component.literal(name).withStyle(Style.EMPTY.withColor(0x90c833).withUnderlined(true)));
            tooltip.add(Component.literal(description));
        }

        DemesnePerk(int x, int y, String name, String description) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.description = description;
            this.maxValue = 1;
            isSanction = false;
            constructTooltip();
        }

        DemesnePerk(int x, int y, String name, String description, boolean isSanction) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.description = description;
            this.maxValue = 1;
            this.isSanction = isSanction;
            constructTooltip();
        }

        DemesnePerk(int x, int y, String name, String description, int maxChoices) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.description = description;
            this.maxValue = maxChoices;
            isSanction = false;
            constructTooltip();
        }
    }

    public static HashMap<DemesnePerk, Set<DemesnePerk>> connections = new HashMap<>();

    static {
        connect(DemesnePerk.BLOCK_GEN, DemesnePerk.TOOL_REPAIR);
        connect(DemesnePerk.TOOL_REPAIR, DemesnePerk.FLIGHT);
        connect(DemesnePerk.FLIGHT, DemesnePerk.PORTAL);
        connect(DemesnePerk.BLOCK_GEN, DemesnePerk.SCHEMATIC);
        connect(DemesnePerk.SCHEMATIC, DemesnePerk.VEIN_MINE);
        connect(DemesnePerk.VEIN_MINE, DemesnePerk.TOOL_REPAIR);
        connect(DemesnePerk.BEACON_HIDE, DemesnePerk.VEIN_MINE);
        connect(DemesnePerk.SPAWN_SET, DemesnePerk.HOME);
        connect(DemesnePerk.FLIGHT, DemesnePerk.HOME);
        connect(DemesnePerk.BASE_TELEPORT, DemesnePerk.HOME);
        connect(DemesnePerk.BASE_TELEPORT, DemesnePerk.PORTAL);
        connect(DemesnePerk.SCHEMATIC, DemesnePerk.IDOLS);
        connect(DemesnePerk.BASE_TELEPORT, DemesnePerk.MOB_REPOSITION);
        connect(DemesnePerk.IDOLS, DemesnePerk.CAGE);
        connect(DemesnePerk.PROTECTION, DemesnePerk.CAGE);
        connect(DemesnePerk.RECOVERY, DemesnePerk.WEATHER);
        connect(DemesnePerk.WEATHER, DemesnePerk.MOB_REPOSITION);
        connect(DemesnePerk.IDOLS, DemesnePerk.BINDING);
        connect(DemesnePerk.CAGE, DemesnePerk.COLLAR);
        connect(DemesnePerk.WEATHER, DemesnePerk.SPIRITS);
        connect(DemesnePerk.MOB_REPOSITION, DemesnePerk.TIME);
        connect(DemesnePerk.BINDING, DemesnePerk.COLLAR);
        connect(DemesnePerk.COLLAR, DemesnePerk.SPIRITS);
        connect(DemesnePerk.SPIRITS, DemesnePerk.TIME);
    }

    private static void connect(DemesnePerk perk1, DemesnePerk perk2) {
        if (!connections.containsKey(perk1)) connections.put(perk1, new HashSet<>());
        connections.get(perk1).add(perk2);
        if (!connections.containsKey(perk2)) connections.put(perk2, new HashSet<>());
        connections.get(perk2).add(perk1);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private static HashMap<String, DemesnesClaimRitual> activeRituals = new HashMap<>();
    private static List<DemesnesClaimRitual> ritualsToRemove = new ArrayList<>();
    private static HashMap<ServerLevel, HashMap<ChunkPos, ClaimedDemesneData>> claimedDemesnes = new HashMap<>();
    private static HashMap<Integer, List<ClaimedDemesneData>> demesnesPendingLoad = new HashMap<>();
    private static HashMap<String, ClaimedDemesneData> demesnesByPlayer = new HashMap<>();

    @SubscribeEvent
    public static void startup(ServerAboutToStartEvent event) {
        activeRituals = new HashMap<>();
        ritualsToRemove = new ArrayList<>();
        claimedDemesnes = new HashMap<>();
        demesnesPendingLoad = new HashMap<>();
        demesnesByPlayer = new HashMap<>();
    }

    public static void startClaim(DemesnesClaimStartMessage msg, ServerPlayer player) {
        var ritual = new DemesnesClaimRitual(msg, player);
        if (ritual.range == -1) return;
        player.giveExperienceLevels(-Mth.square(ritual.range * 2 + 1));
        activeRituals.put(player.getGameProfile().getName(), ritual);
        player.level().playSound(null, msg.centerPos(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1, 0.5f);
        player.level().playSound(null, msg.centerPos(), SoundEvents.TRIDENT_RETURN, SoundSource.BLOCKS, 1, 0.5f);
        player.level().playSound(null, msg.centerPos(), SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1, 0.5f);

        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new DemesnesClientboundMessage(DemesnesClientboundMessage.EventType.START, ritual.minBlock, ritual.maxBlock, ritual.levelId, ritual.claimant.getGameProfile().getName()));
    }

    public static void setPerkValue(DemesnesPerkMessage msg, ServerPlayer player) {
        var perk = DemesnePerk.values()[msg.perkIndex()];
        if (perk.isSanction && msg.newValue() < 0) {
            var stack = new ItemStack(OtherverseItems.WRIT.get(), 1);
            stack.getOrCreateTag().putInt("sanction", msg.perkIndex());
            player.drop(stack, false);
            player.level().playSound(null, player, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1f, 1f);
            return;
        }
        var demesne = demesnesByPlayer.get(player.getGameProfile().getName());
        if (demesne == null) return;
        if (perk == DemesnePerk.BLOCK_GEN) {
            if (!trySetBlock(demesne, player.serverLevel(), msg.beaconPos())) {
                return;
            }
        } else if (perk == DemesnePerk.SPIRITS) {
            if (!trySetSpirit(demesne, player.serverLevel(), msg.beaconPos())) {
                return;
            }
        } else if (perk == DemesnePerk.HOME) {
            EscapeRopeItem.removeCooldown(player);
        } else if (perk == DemesnePerk.VEIN_MINE) {
            updateMiningLevel(player, msg.newValue());
        } else if (perk == DemesnePerk.WEATHER && demesne.getPerkLevel(DemesnePerk.WEATHER) > msg.newValue()) {
            demesne.adjustSkyColor(null);
        }
        var perksClaimed = 0;
        for (var perkVal : DemesnePerk.values()) {
            perksClaimed += demesne.getPerkLevel(perkVal);
        }
        player.giveExperienceLevels(-getLevelCost(perksClaimed));
        demesne.setPerkValue(perk, msg.newValue());
        DiagramManager.getOrCreateLevelData(player.level().getServer().overworld()).savedData.setDirty();
    }

    private static void updateMiningLevel(ServerPlayer player, int perkValue) {
        var amount = getMiningLevel(perkValue);
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new DemesnesClientboundMessage(DemesnesClientboundMessage.EventType.MINING_SET,
                        BlockPos.ZERO, BlockPos.ZERO, amount, ""));
    }

    public static int getMiningLevel(int perkValue) {
        return perkValue == 0 ? 1 : perkValue == 1 ? 9 : 64;
    }

    private static boolean trySetSpirit(ClaimedDemesneData demesne, ServerLevel level, BlockPos blockPos) {
        var tag = DiagramManager.getOrCreateLevelData(level).getPlacedItemTag(blockPos.above());
        if (tag == null && level.getBlockEntity(blockPos.above()) instanceof ChalkCircle cc) {
            var item = cc.getItem();
            if (item != null && item.hasTag() && item.getTag().contains("hallow")) {
                tag = item.getTag().getCompound("hallow");
            }
        }
        if (tag == null) return false;
        demesne.addFavoredSpirit(tag.getString("spirit_type"));
        return true;
    }

    private static boolean trySetBlock(ClaimedDemesneData demesne, ServerLevel level, BlockPos pos) {
        var blockstate = level.getBlockState(pos.above());
        if (!blockstate.is(Tags.Blocks.STONE) && !blockstate.is(BlockTags.LOGS)) return false;
        demesne.addFavoredMaterial(blockstate);
        return true;
    }

    public static boolean tryMakePortal(ServerLevel level, ChalkCircle circle, Diagram diagram) {
        var item = circle.item;
        if (!item.hasTag() || !item.getTag().contains("escape_rope_x")) return false;
        var target = diagram.influences.get(circle.getPos());
        if (target == null || !level.isEmptyBlock(target)) return false;
        var x = item.getTag().getFloat("escape_rope_x");
        var y = item.getTag().getFloat("escape_rope_y");
        var z = item.getTag().getFloat("escape_rope_z");
        var destination = new BlockPos(Mth.floor(x), Math.round(y), Mth.floor(z));
        var destLevelId = item.getTag().getInt("level_id");
//        LOGGER.info("LEVEL ID: {}", destLevelId);
        var owner = diagram.getOwner(level);
        if (owner == null) return false;
        ClaimedDemesneData demesne = null;
        for (var demesneLevel : level.getServer().getAllLevels()) {
//            LOGGER.info("LEVEL {}: {}", DiagramManager.getDimensionHash(demesneLevel), demesneLevel.dimension());
            if (destLevelId == DiagramManager.getDimensionHash(demesneLevel)) {
//                LOGGER.info("LEVEL MATCH");
                demesne = getData(demesneLevel, destination);
                break;
            }
        }
//        LOGGER.info("TRYING TO OPEN DEMESNE PORTAL...");
        if (demesne == null) {
//            LOGGER.info("NULL DEMESNE");
            return false;
        }
        if (demesne.getPerkLevel(DemesnePerk.PORTAL) == 0) {
//            LOGGER.info("NO PORTAL PERK");
            return false;
        }
        if (demesne.levelId != destLevelId) {
//            LOGGER.info("{} DOES NOT MATCH {}", demesne.levelId, destLevelId);
            return false;
        }
        if (!(new AABB(demesne.minPos, demesne.maxPos).contains(x, y, z))) {
//            LOGGER.info("DESTINATION NOT IN DEMESNE");
            return false;
        }
        if (!demesne.hasSanction(DemesnePerk.SANCTION_INTERACT, owner)) {
//            LOGGER.info("UNSANCTIONED");
            owner.displayClientMessage(Component.literal("You don't have permission to open a portal to this Demesne."), true);
            return false;
        }
        var cost = destLevelId == DiagramManager.getDimensionHash(level) ? 6 : 18;
        if (!diagram.trySpendPower(level, target, cost, new HashSet<>())) {
//            LOGGER.info("NOT ENOUGH POWER");
            return false;
        }
        circle.drainItem();
        level.setBlockAndUpdate(target, OtherverseBlocks.DEMESNE_PORTAL.get().defaultBlockState());
        if (level.getBlockEntity(target) instanceof DemesnesPortal portal) {
            portal.destinationPosition = destination;
            portal.destinationLevel = destLevelId;
            portal.markUpdated();
        }
        return true;
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        var gameTime = event.player.level().getGameTime();
        if (gameTime % 20 != 0) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        var abilities = sp.getAbilities();
        if (abilities.instabuild) return;
        var demesnes = getData(sp);
        if (demesnes == null) return;
        var inDemesnes = demesnes == getData(sp.serverLevel(), sp.blockPosition());
        if (inDemesnes && gameTime % 60 == 0 && demesnes.getPerkLevel(DemesnePerk.RECOVERY) > 0) {
            sp.heal(1);
            sp.getFoodData().eat(1, 1);
        }
        if (demesnes.getPerkLevel(DemesnePerk.FLIGHT) == 0) return;
        var fastFlying = abilities.mayfly && abilities.getFlyingSpeed() == 0.05f;
        if (inDemesnes && !fastFlying) {
            abilities.mayfly = true;
            abilities.setFlyingSpeed(0.05f);
            sp.onUpdateAbilities();
        }
        if (fastFlying && !inDemesnes) {
            abilities.mayfly = false;
            FamiliarManager.updateAbilities(sp, !inDemesnes);
            if (!abilities.mayfly) abilities.flying = false;
            sp.onUpdateAbilities();
        }
    }

    public record ArchitectureInventory(IItemHandler inventory, BlockFocus focus,
                                        ContractManager.PositionOrSpindle pos) {
    }

    public static void tryGrowArchitecture(ServerLevel level, BlockFocus focus, Diagram diagram) {
        if (!focus.getBlock().is(OtherverseBlocks.DEMESNE_BEACON.get())) return;
        var beaconPos = focus.getPos();
        var demesne = getData(level, beaconPos);
        if (demesne == null || demesne.getPerkLevel(DemesnePerk.SCHEMATIC) == 0) return;
        var webPositions = new ArrayList<BlockPos>();
        var inventories = new ArrayList<ArchitectureInventory>();
        var data = DiagramManager.getOrCreateLevelData(level);

        for (var sourcePos : diagram.blockFocusPositions) {
            if (!beaconPos.equals(diagram.influences.get(sourcePos))) continue;
            var state = level.getBlockState(sourcePos);
            if (state.is(OtherverseBlocks.WEB_OF_FATE.get())) {
                var color = state.getValue(FateWebBlock.color);
                var playerName = focus.getDiagram().getOwner((ServerLevel) focus.getFocusLevel()).getGameProfile().getName();
                var key = SympathyManager.getKey(playerName, color);
                var pos = data.getSympathyPosition(key);
                if (pos != null) {
                    webPositions.add(pos);
                }
                continue;
            }
            var sourceState = level.getBlockState(sourcePos);
            var be = level.getBlockEntity(sourcePos);
            if (be == null) {
                continue;
            }
            var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
            if (cap.isEmpty()) {
                continue;
            }
            var inventory = cap.get();
            ArchitectureInventory architectureInventory = null;
            for (var webPos : diagram.blockFocusPositions) {
                if (!sourcePos.equals(diagram.influences.get(webPos))) continue;
                state = level.getBlockState(webPos);
                if (!state.is(OtherverseBlocks.WEB_OF_FATE.get())) {
                    continue;
                }
                architectureInventory = new ArchitectureInventory(inventory, data.allBlockFoci.get(sourcePos),
                        new ContractManager.PositionOrSpindle(data.allBlockFoci.get(webPos)));
                break;
            }
            if (architectureInventory != null) inventories.add(architectureInventory);
        }
        if (webPositions.size() == 2) {
            for (var inventory : inventories) {
                new GrowArchitectureProcess(focus, inventory, webPositions, demesne);
            }
        }
    }

    public static void onBeaconBroken(ServerLevel sl, BlockPos pos) {
        LOGGER.debug("GETTING DATA FROM DEMESNE BEACON BROKEN");
        var demesne = getData(sl, pos);
        if (demesne == null || demesne.minChronoPos == null) return;
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new DemesnesClientboundMessage(
                DemesnesClientboundMessage.EventType.CHRONO_UNSET, demesne.minChronoPos, demesne.maxChronoPos, demesne.levelId, demesne.practitioner));
        demesne.minChronoPos = null;
        demesne.maxChronoPos = null;
        demesne.refreshChrono(sl);
    }

    @SubscribeEvent
    public static void onPlayerTickInDemesne(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        var demesne = getData(sp);
        if (demesne == null || demesne != getData(sp.serverLevel(), sp.blockPosition())) return;
        if (sp.serverLevel().getGameTime() % 20 == 0 && demesne.getPerkLevel(DemesnePerk.TOOL_REPAIR) > 0) {
            for (var stack : sp.getInventory().items) {
                if (!stack.isDamageableItem()) continue;
                var damage = stack.getDamageValue();
                if (damage > 0) stack.setDamageValue(damage - 1);
            }
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var item = event.getItemStack();
        if (!item.is(OtherverseItems.WRIT.get())) return;
        event.getToolTip().add(Component.literal("Use on another player to grant or revoke.")
                .withStyle(Style.EMPTY.withColor(0x888888)));
    }


    @SubscribeEvent
    public static void onSleep(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var sl = sp.serverLevel();
        if (sl.dimensionType().bedWorks()) return;
        if (!(sl.getBlockState(event.getPos()).getBlock() instanceof BedBlock)) return;
        var demesne = getData(sl, event.getPos());
        if (demesne == null || demesne.getPerkLevel(DemesnePerk.SPAWN_SET) == 0) return;
        sp.setRespawnPosition(sl.dimension(), event.getPos(), sp.getYRot(), false, true);
        event.setUseBlock(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        var item = event.getItemStack();
        if (!item.is(OtherverseItems.WRIT.get())) return;
        if (!(event.getEntity() instanceof ServerPlayer owner)) return;
        if (!(event.getTarget() instanceof ServerPlayer sp)) return;
        grantWrit(owner, sp, DemesnePerk.values()[item.getOrCreateTag().getInt("sanction")], item);
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        var item = event.getItemStack();
        if (!event.getEntity().getAbilities().instabuild || !item.is(OtherverseItems.WRIT.get())) return;
        event.setCancellationResult(InteractionResult.CONSUME);
        if (!(event.getEntity() instanceof ServerPlayer owner)) return;
        grantWrit(owner, owner, DemesnePerk.values()[item.getOrCreateTag().getInt("sanction")], item);
    }


    private static void grantWrit(ServerPlayer demesneOwner, ServerPlayer recipient, DemesnePerk perk, ItemStack item) {
        var demesneData = getData(demesneOwner);
        if (demesneData == null) return;
        var hasPermission = demesneData.toggleSanction(perk, recipient);
        var sl = recipient.serverLevel();
        sl.playSound(null, recipient, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        var r = sl.random;
        for (var i = 0; i < (hasPermission ? 10 : 5); i++) {
            sl.sendParticles(hasPermission ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER,
                    recipient.getX(r.nextFloat()),
                    recipient.getY(r.nextFloat()),
                    recipient.getZ(r.nextFloat()),
                    1, 0, 0, 0, 0.1
            );
        }
        item.shrink(1);
        if (item.isEmpty()) demesneOwner.getInventory().removeItem(item);
        demesneOwner.getInventory().setChanged();
        var message = recipient.getGameProfile().getName() + (hasPermission ? " has been granted a " : " has lost the ")
                + perk.name.replace("Sanction:", "Writ of")
                + " for " + demesneOwner.getGameProfile().getName() + "'s Demesne";
        recipient.displayClientMessage(Component.literal(message), false);
        if (recipient != demesneOwner) demesneOwner.displayClientMessage(Component.literal(message), false);
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().getPersistentData().getBoolean("demesnes_challenger")) {
            event.getEntity().setGlowingTag(SightManager.shouldRenderSight());
        }
    }

    @SubscribeEvent
    public static void livingTick(LivingDeathEvent event) {
        var data = event.getEntity().getPersistentData();
        if (!data.getBoolean("demesnes_challenger")) return;
        var ritual = activeRituals.get(data.getString("demesnes_claimant"));
        if (ritual == null) return;
        ritual.onChallengerDeath(event);
    }

    @SubscribeEvent
    public static void onChangeDimensions(PlayerEvent.PlayerChangedDimensionEvent event) {
        tryAbandon(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        tryAbandon(event.getEntity());
    }

    private static void tryAbandon(Player player) {
        var ritual = activeRituals.get(player.getGameProfile().getName());
        if (ritual == null) return;
        ritual.abandon();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        for (var ritual : activeRituals.values()) {
            ritual.tick();
        }
        if (!ritualsToRemove.isEmpty()) {
            for (var ritual : ritualsToRemove) {
                activeRituals.remove(ritual.claimant.getGameProfile().getName());
            }
            ritualsToRemove.clear();
        }
    }

    @SubscribeEvent
    public static void onSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            for (var ritual : activeRituals.values()) {
                OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DemesnesClientboundMessage(DemesnesClientboundMessage.EventType.LOAD_RITUAL,
                                ritual.minBlock, ritual.maxBlock, ritual.levelId, ritual.claimant.getGameProfile().getName()));
            }
            for (var demesne : DiagramManager.getOrCreateLevelData(sp.serverLevel()).claimedDemesnes.values()) {
                LOGGER.debug("SENDING DEMESNE DATA TO CLIENT");
                sendDemesneDataToClient(demesne, sp);
            }
        }
    }

    private static void sendDemesneDataToClient(ClaimedDemesneData demesne, ServerPlayer player) {
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new DemesnesClientboundMessage(DemesnesClientboundMessage.EventType.LOAD_CLAIMED,
                        demesne.minPos, demesne.maxPos, demesne.levelId, demesne.practitioner));
        if (demesne.maxChronoPos != null) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new DemesnesClientboundMessage(
                    DemesnesClientboundMessage.EventType.CHRONO_SET, demesne.minChronoPos, demesne.maxChronoPos, demesne.levelId, demesne.practitioner));
        }
        if (player != null) {
            updateMiningLevel(player, demesne.getPerkLevel(DemesnePerk.VEIN_MINE));
        }
        sendWeatherUpdate(demesne);
    }

    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.RightClickEmpty event) {
        tryWarpClient();
    }

    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().isEmpty()) return;
        if (!event.getLevel().isClientSide()) return;
        tryWarpClient();
    }

    @SubscribeEvent
    public static void useItemOn(PlayerInteractEvent.RightClickBlock event) {
        var bs = event.getLevel().getBlockState(event.getPos());
        if (event.getEntity() instanceof ServerPlayer sp && event.getEntity().getAbilities().instabuild
                && event.getItemStack().is(Items.STICK) && bs.is(OtherverseBlocks.DEMESNE_BEACON.get())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setUseBlock(Event.Result.DENY);
            event.setCanceled(true);
            var data = DiagramManager.getOrCreateLevelData(sp.serverLevel().getServer().overworld());
            LOGGER.debug("ACCESSING DEMESNE DATA FROM USING ITEM");
            data.claimedDemesnes.remove(sp.getGameProfile().getName());
            data.savedData.setDirty();
        }
        if (event.getItemStack().is(Items.CLOCK) && bs.is(OtherverseBlocks.DEMESNE_BEACON.get())) {
            if (event.getEntity() instanceof ServerPlayer sp) {
                var demesne = getData(sp);
                if (demesne == null || demesne != getData(sp.serverLevel(), event.getPos())
                        || demesne.getPerkLevel(DemesnePerk.WEATHER) == 0) return;
                if (sp.isShiftKeyDown()) {
                    demesne.fixedTime = -1;
                } else if (demesne.fixedTime == -1) {
                    demesne.fixedTime = sp.serverLevel().getDayTime();
                } else {
                    demesne.fixedTime += 1000;
                }
                sendWeatherUpdate(demesne);
                event.setCancellationResult(InteractionResult.CONSUME);
                event.setUseBlock(Event.Result.DENY);
                event.setCanceled(true);
                DiagramManager.getOrCreateLevelData(sp.serverLevel().getServer().overworld()).savedData.setDirty();
                return;
            } else {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
        }
        if (!(event.getItemStack().getItem() instanceof DyeItem dye)) return;
        if (bs.is(OtherverseBlocks.DEMESNE_PORTAL.get())) {
            event.getLevel().setBlockAndUpdate(event.getPos(),
                    OtherverseBlocks.DEMESNE_PORTAL.get().defaultBlockState().setValue(DemesnesPortalBlock.color, dye.getDyeColor()));
            return;
        }
        if (bs.is(OtherverseBlocks.DEMESNE_BEACON.get()) && event.getEntity() instanceof ServerPlayer sp) {
            var demesne = getData(sp);
            if (demesne != null && demesne == getData(sp.serverLevel(), event.getPos())
                    && demesne.getPerkLevel(DemesnePerk.WEATHER) > 0) {
                demesne.adjustSkyColor(dye.getDyeColor());
                sendWeatherUpdate(demesne);
                event.setCancellationResult(InteractionResult.CONSUME);
                event.setUseBlock(Event.Result.DENY);
                DiagramManager.getOrCreateLevelData(sp.serverLevel().getServer().overworld()).savedData.setDirty();
            }
        }
    }

    private static void sendWeatherUpdate(ClaimedDemesneData demesne) {
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new DemesnesClientboundMessage(DemesnesClientboundMessage.EventType.COLOR_SET,
                        demesne.minPos, demesne.getWeather(), demesne.levelId, demesne.practitioner));
    }

    private static void tryWarpClient() {
        if (!SightManager.shouldRenderSight()) return;
        var beacon = DemesnesBeaconRenderer.clientLookingAt;
        if (beacon == null) return;
        var player = Minecraft.getInstance().player;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var playerPos = camera.getPosition();
        var beaconPos = new Vec3(
                beacon.getBlockPos().getX() + 0.5f,
                beacon.getBlockPos().getY() + 0.5f,
                beacon.getBlockPos().getZ() + 0.5f
        );
        var dot = player.getForward().dot(beaconPos.subtract(playerPos).normalize());
        var isLookingAt = dot > DemesnesBeaconRenderer.LOOK_RADIUS;
        if (!isLookingAt) return;
        OtherversePacketHandler.INSTANCE.sendToServer(new DemesnesWarpMessage(beacon.getBlockPos()));
    }

    public static void tryWarpServer(DemesnesWarpMessage msg, ServerPlayer sender) {
        var demesne = getData(sender);
        if (demesne == null) return;
        if (demesne.getPerkLevel(DemesnePerk.BASE_TELEPORT) == 0) return;
        if (getData(sender.serverLevel(), sender.blockPosition()) != demesne) return;
        if (!(sender.level().getBlockEntity(msg.beaconPosition()) instanceof DemesnesBeacon beacon)) return;
        if (!beacon.inDemesneOf.equals(sender.getGameProfile().getName())) return;
        sender.level().playSound(sender, sender.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
        sender.teleportTo(beacon.getBlockPos().getX() + 0.5f,
                beacon.getBlockPos().getY() + 1,
                beacon.getBlockPos().getZ() + 0.5f);
        sender.level().playSound(null, beacon.getBlockPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
    }

    @SubscribeEvent
    public static void onGetHurt(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var demesne = getData(sp.serverLevel(), sp.blockPosition());
        if (demesne == null
                || !Objects.equals(demesne.practitioner, sp.getGameProfile().getName())
                || demesne.getPerkLevel(DemesnePerk.PROTECTION) == 0) return;
        if (event.getSource().is(DamageTypes.FALL)
                || event.getSource().is(DamageTypes.ON_FIRE)
                || event.getSource().is(DamageTypes.IN_FIRE)
                || event.getSource().is(DamageTypes.LAVA)
                || event.getSource().is(DamageTypes.FREEZE)
                || event.getSource().is(DamageTypes.DROWN)
                || event.getSource().is(DamageTypes.HOT_FLOOR)) {
            event.setCanceled(true);
        }
    }

    public static void abandon(DemesnesClaimRitual ritual) {
        ritualsToRemove.add(ritual);
        ritual.cleanup();
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new DemesnesClientboundMessage(DemesnesClientboundMessage.EventType.ABANDON, ritual.minBlock, ritual.maxBlock, ritual.levelId, ritual.claimant.getGameProfile().getName()));
    }

    public static int getDemesnesRange(LevelAccessor lvl, BlockPos pos) {
        var i = 1;
        for (; i <= 4; i++) {
            if (!isValidSquare(pos, i, lvl)) break;
        }
        return i - 2;
    }

    private static boolean isValidSquare(BlockPos basePos, int i, LevelAccessor worldLevel) {
        for (var x = -i; x <= i; x++) {
            for (var z = -i; z <= i; z++) {
                if (!worldLevel.getBlockState(basePos.offset(x, -i, z)).is(BlockTags.BEACON_BASE_BLOCKS)) return false;
            }
        }
        return true;
    }

    public static void complete(DemesnesClaimRitual ritual) {
        ritualsToRemove.add(ritual);
        LOGGER.debug("CREATING DEMESNE DATA WITH " + ritual.spiritType.label());
        var biome = ritual.level.getBiome(ritual.claimPos).unwrapKey().get().location().toString();
        OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new DemesnesClientboundMessage(DemesnesClientboundMessage.EventType.SUCCEED, ritual.minBlock, ritual.maxBlock, ritual.levelId, ritual.claimant.getGameProfile().getName()));
        var demesne = new ClaimedDemesneData(ritual.minBlock, ritual.maxBlock, biome, ritual.claimant.getGameProfile().getName(), ritual.range, ritual.levelId, ritual.spiritType);
        demesne.addToChunks(claimedDemesnes, ritual.level);
        if (ritual.spiritType != null) SpiritAffinityTracker.setDemesneAffinity(ritual.spiritType, ritual.claimant);
        demesnesByPlayer.put(ritual.claimant.getGameProfile().getName(), demesne);
        DiagramManager.getOrCreateLevelData(ritual.claimant.level().getServer().overworld()).registerClaimedDemesne(demesne);
        Otherverse.ADVANCEMENTS.trigger(ritual.claimant, "demesnes");
    }

    public static void tryLoadDemesne(ClaimedDemesneData demesne, ServerLevel overworld) {
        var sl = DiagramManager.levelFromHash(overworld, demesne.levelId);
        demesnesByPlayer.put(demesne.practitioner, demesne);
        if (sl == null) {
            if (!demesnesPendingLoad.containsKey(demesne.levelId))
                demesnesPendingLoad.put(demesne.levelId, new ArrayList<>());
            demesnesPendingLoad.get(demesne.levelId).add(demesne);
            return;
        }
        loadDemesne(demesne, sl);
    }

    private static void loadDemesne(ClaimedDemesneData demesne, ServerLevel sl) {
        LOGGER.debug("LOADING DEMESNE");
        demesne.addToChunks(claimedDemesnes, sl);
        sendDemesneDataToClient(demesne, FamiliarManager.getPlayerFromName(sl, demesne.practitioner));
    }

    private static void loadDemesnesFor(ServerLevel sl) {
        var pending = demesnesPendingLoad.get(DiagramManager.getDimensionHash(sl));
        if (pending == null) return;
        for (var demesnes : pending) {
            loadDemesne(demesnes, sl);
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel sl) loadDemesnesFor(sl);
    }

    @SubscribeEvent
    public static void onLevelLoad(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof ServerLevel sl) loadDemesnesFor(sl);
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level() instanceof ServerLevel sl) loadDemesnesFor(sl);
    }

    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        var mob = event.getEntity();
        if (event.getSpawnType() != MobSpawnType.NATURAL
                && event.getSpawnType() != MobSpawnType.REINFORCEMENT
                && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION
                && event.getSpawnType() != MobSpawnType.PATROL
                && event.getSpawnType() != MobSpawnType.EVENT
                && event.getSpawnType() != MobSpawnType.JOCKEY) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;
        var demesne = getData(sl, BlockPos.containing(event.getX(), event.getY(), event.getZ()));
        if (demesne == null || demesne.getPerkLevel(DemesnePerk.MOB_REPOSITION) == 0 || demesne.spawnDestinations.isEmpty())
            return;
        var arr = demesne.spawnDestinations.toArray(new BlockPos[0]);
        var pos = arr[sl.getRandom().nextInt(arr.length)];
        mob.setPos(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
    }

    public static ClaimedDemesneData getData(Player player) {
        return demesnesByPlayer.get(player.getGameProfile().getName());
    }

    public static boolean doesIntersectExistingClaim(DemesnesBeacon beacon) {
        var sl = (ServerLevel) beacon.getLevel();
        LOGGER.debug("CHECKING INTERSECTION");
        if (!claimedDemesnes.containsKey(sl) || beacon.range < 0) return false;
        var chunksForLevel = claimedDemesnes.get(sl);
        var minChunk = Otherverse.chunkAt(beacon.minBlock);
        var maxChunk = Otherverse.chunkAt(beacon.maxBlock);
        for (var x = minChunk.x; x <= maxChunk.x; x++) {
            for (var z = minChunk.z; z <= maxChunk.z; z++) {
                if (chunksForLevel.containsKey(new ChunkPos(x, z))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ClaimedDemesneData getData(ServerLevel sl, BlockPos pos) {
        var chunkClaims = claimedDemesnes.get(sl);
        if (chunkClaims == null) return null;
//        LOGGER.info("GETTING DATA FOR {} AT {}", sl.dimension(), Otherverse.chunkAt(pos));
        var ret = chunkClaims.getOrDefault(Otherverse.chunkAt(pos), null);
//        if(ret == null){
//            for(var cc : chunkClaims.keySet()){
//                LOGGER.info("{}: {}", cc.toString(), chunkClaims.get(cc) == null ? "NULL" : chunkClaims.get(cc));
//            }
//        }
        return ret;
    }

    @SubscribeEvent
    public static void pickupItem(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var demesne = getData(sp.serverLevel(), event.getItem().blockPosition());
        if (demesne == null) return;
        if (!demesne.hasSanction(DemesnePerk.SANCTION_TAKE, sp)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void breakBlock(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getPosition().isEmpty()) return;
        var demesne = getData(sp.serverLevel(), event.getPosition().get());
        if (demesne == null) return;
        if (!demesne.hasSanction(DemesnePerk.SANCTION_BUILD, sp)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var demesne = getData(sp.serverLevel(), event.getPos());
        if (demesne == null) return;
        if (!demesne.hasSanction(DemesnePerk.SANCTION_BUILD, sp)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onHurt(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        var demesne = getData(sp.serverLevel(), event.getEntity().blockPosition());
        if (demesne == null) return;
        if (!demesne.hasSanction(DemesnePerk.SANCTION_FIGHT, sp)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var demesne = getData(sp.serverLevel(), event.getPos());
        if (demesne == null) return;
        if (!demesne.hasSanction(DemesnePerk.SANCTION_INTERACT, sp)) event.setCanceled(true);
    }
}
