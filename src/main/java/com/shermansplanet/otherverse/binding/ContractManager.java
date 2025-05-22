package com.shermansplanet.otherverse.binding;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.diagrams.*;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.registries.OtherverseBlocks;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.sympathy.FateWebBlock;
import com.shermansplanet.otherverse.sympathy.SympathyManager;
import com.shermansplanet.otherverse.sympathy.SympathyRangeUpdateMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE)
public class ContractManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class PositionOrSpindle {
        boolean isPosition;
        int x, y, z;
        String playerName;
        DyeColor color;
        final String key;

        public PositionOrSpindle(BlockPos pos) {
            isPosition = true;
            x = pos.getX();
            y = pos.getY();
            z = pos.getZ();
            key = "";
        }

        public PositionOrSpindle(BlockFocus focus) {
            var state = focus.getFocusLevel().getBlockState(focus.getPos());
            color = state.getValue(FateWebBlock.color);
            playerName = focus.getDiagram().getOwner((ServerLevel) focus.getFocusLevel()).getGameProfile().getName();
            key = SympathyManager.getKey(playerName, color);
        }

        public PositionOrSpindle(CompoundTag tag) {
            isPosition = tag.getBoolean("isPosition");
            if (isPosition) {
                var coords = tag.getIntArray("coords");
                x = coords[0];
                y = coords[1];
                z = coords[2];
                key = "";
            } else {
                playerName = tag.getString("playerName");
                color = DyeColor.byId(tag.getInt("color"));
                key = SympathyManager.getKey(playerName, color);
            }
        }

        public CompoundTag toTag() {
            var tag = new CompoundTag();
            tag.putBoolean("isPosition", isPosition);
            if (isPosition) {
                tag.putIntArray("coords", new int[]{x, y, z});
            } else {
                tag.putString("playerName", playerName);
                tag.putInt("color", color.getId());
            }
            return tag;
        }

        public BlockPos getPos(Level level) {
            if (isPosition) return new BlockPos(x, y, z);
            var data = DiagramManager.getOrCreateLevelData(level);
            return data.getSympathyPosition(key);
        }

        public boolean trySendUpdate(ServerLevel level, int dx, int dy, int dz, AABB bounds, boolean visible) {
            var player = FamiliarManager.getPlayerFromName(level, playerName);
            if (player == null) return false;
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new SympathyRangeUpdateMessage(dx, dy, dz, bounds, color, visible));
            return true;
        }
    }

    public static boolean tryMakeContract(ServerLevel level, ChalkCircle circle, Diagram diagram) {
        if (!circle.getItem().is(Items.PAPER)) {
            return false;
        }
        /*var player = new HashSet<EntityType<?>>();
        player.add(EntityType.PLAYER);
        if (!diagram.trySpendPower(level, circle.getBlockPos(), 3, player)) {
            return false;
        }*/
        CompoundTag contractTag = makeDeciderTag(level, circle, diagram, circle.getBlockPos());
        if (contractTag.isEmpty()) return false;
        ItemStack contract = OtherverseItems.CONTRACT.get().getDefaultInstance();
        contract.getOrCreateTag().put("contract", contractTag);
        circle.item = contract;
        circle.markUpdated();
        level.playSound(null, circle.getBlockPos(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1f, 1f);
        BlockPos bp = circle.getPos();
        for (int n = 0; n < 10; ++n) {
            level.sendParticles(ParticleTypes.POOF, bp.getX() + 0.5, bp.getY() + 0.25, bp.getZ() + 0.5,
                    1, 0, 0, 0, 0.15);
        }
        Otherverse.ADVANCEMENTS.trigger(diagram.getOwner(level), "contract");
        LOGGER.debug("DIAGRAM SUCCESS: CONTRACT");
        return true;
    }

    public static CompoundTag makeDeciderTag(ServerLevel level, ChalkCircle circle, Diagram diagram, BlockPos originalPos) {
        CompoundTag tag = new CompoundTag();
        int i = 0;
        int range = 8;
        for (var influencePos : diagram.influences.entrySet()) {
            if (!influencePos.getValue().equals(circle.getPos()) || influencePos.getKey().equals(originalPos)) {
                continue;
            }
            if (level.getBlockEntity(influencePos.getKey()) instanceof ChalkCircle sourceCircle) {
                if (sourceCircle.item.is(Items.ORANGE_DYE)) {
                    CompoundTag deciderTag = makeDeciderTag(level, sourceCircle, diagram, originalPos);
                    if (!deciderTag.isEmpty()) {
                        tag.put("fallback", deciderTag);
                    }
                    continue;
                }
                if (sourceCircle.item.is(Items.SPYGLASS)) {
                    range += 8;
                    continue;
                }
                CompoundTag taskTag = makeTaskTag(level, sourceCircle, diagram, originalPos);
                if (taskTag.isEmpty()) {
                    continue;
                }
                tag.put("task_" + i++, taskTag);
                i++;
            }
        }
        tag.putInt("range", range);
        return tag;
    }

    public static CompoundTag makeTaskTag(ServerLevel level, ChalkCircle circle, Diagram diagram, BlockPos originalPos) {
        CompoundTag tag = new CompoundTag();
        ItemStack taskItem = circle.getItem();
        boolean isCrafting = false;
        ArrayList<Pair<BlockPos, ItemStack>> craftingItemPositions = null;

        if (taskItem.getItem() instanceof PickaxeItem) {
            tag.putString("type", "break");
        } else if (taskItem.getItem() instanceof SwordItem) {
            tag.putString("type", "attack");
        } else if (taskItem.is(Items.HOPPER)) {
            tag.putString("type", "take");
        } else if (taskItem.is(Items.DISPENSER)) {
            tag.putString("type", "put");
        } else if (taskItem.is(Items.OBSERVER)) {
            tag.putString("type", "observe");
        } else if (taskItem.is(Items.RAIL)) {
            tag.putString("type", "go");
        } else if (taskItem.getItem() instanceof HoeItem) {
            tag.putString("type", "harvest");
        } else if (taskItem.is(Items.CRAFTING_TABLE)) {
            tag.putString("type", "craft");
            isCrafting = true;
            craftingItemPositions = new ArrayList<>();
        }

        if (tag.isEmpty()) {
            return tag;
        }

        int filterIndex = 0;
        int positionIndex = 0;
        int offsetIndex = 0;
        int blockIndex = 0;
        TransientDiagramData diagramData = DiagramManager.getOrCreateLevelData(level);
        for (var influencePos : diagram.influences.entrySet()) {
            if (!influencePos.getValue().equals(circle.getPos()) || influencePos.getKey().equals(originalPos)) {
                continue;
            }
            if (level.getBlockEntity(influencePos.getKey()) instanceof ChalkCircle sourceCircle) {
                ItemStack stack = sourceCircle.getItem();
                if (stack.isEmpty() || stack.is(Items.AIR) || stack.getItem() instanceof ChalkItem) {
                    continue;
                }
                if (stack.is(Items.GREEN_DYE) || stack.is(Items.YELLOW_DYE) || stack.is(Items.RED_DYE)) {
                    CompoundTag deciderTag = makeDeciderTag(level, sourceCircle, diagram, originalPos);
                    if (!deciderTag.isEmpty()) {
                        String label = stack.is(Items.GREEN_DYE) ? "success" : stack.is(Items.RED_DYE) ? "failure" : "either";
                        tag.put(label, deciderTag);
                        continue;
                    }
                } else if (stack.is(Items.GLASS) || stack.is(Items.COMPASS)) {
                    var positions = new ArrayList<PositionOrSpindle>();
                    for (var blockFocusPosition : diagram.blockFocusPositions) {
                        var influence = diagram.influences.get(blockFocusPosition);
                        if (influence == null || !influence.equals(influencePos.getKey())) {
                            continue;
                        }
                        var focus = diagramData.allBlockFoci.get(blockFocusPosition);
                        if (focus == null) continue;
                        var blockItem = focus.getItem();
                        if (blockItem.isEmpty()) {
                            positions.add(new PositionOrSpindle(blockFocusPosition));
                        } else if (blockItem.is(OtherverseItems.WEB_OF_FATE.get())) {
                            positions.add(new PositionOrSpindle(focus));
                        }
                    }
                    if (stack.is(Items.GLASS)) {
                        if (positions.size() == 2) {
                            for (int i = 0; i < positions.size(); i++) {
                                tag.put("corner_" + i, positions.get(i).toTag());
                            }
                            continue;
                        }
                    } else {
                        for (PositionOrSpindle pos : positions) {
                            var compassPos = influencePos.getKey();
                            tag.put("offset_" + offsetIndex, pos.toTag());
                            tag.putIntArray("offset_" + offsetIndex + "_basis",
                                    new int[]{compassPos.getX(), compassPos.getY(), compassPos.getZ()});
                            offsetIndex++;
                        }
                        continue;
                    }
                } else if (stack.is(Items.COMPARATOR)) {
                    var hasSubFilter = false;
                    for (var entry : diagram.influences.entrySet()) {
                        if (!entry.getValue().equals(influencePos.getKey())) continue;
                        if (level.getBlockEntity(entry.getKey()) instanceof ChalkCircle cc) {
                            var item = cc.getItem();
                            if (item.isEmpty() || item.getItem() instanceof ChalkItem) continue;
                            hasSubFilter = true;
                            tag.putInt("filter_" + filterIndex, Item.getId(item.getItem()));
                            tag.put("tag_item_" + filterIndex, item.getOrCreateTag());
                            filterIndex++;
                            continue;
                        }
                        BlockFocus bf = diagramData.allBlockFoci.get(entry.getKey());
                        if (bf == null) continue;
                        var item = bf.getItem();
                        if (item.isEmpty() || item.is(Items.AIR)) continue;
                        tag.putInt("filter_" + filterIndex, Item.getId(item.getItem()));
                        var blocktags = new CompoundTag();
                        for (var blocktag : level.getBlockState(entry.getKey()).getValues().entrySet()) {
                            blocktags.putString(blocktag.getKey().getName(), blocktag.getValue().toString());
                        }
                        tag.put("tag_block_" + filterIndex, blocktags);
                        filterIndex++;
                        hasSubFilter = true;
                    }
                    if (hasSubFilter) {
                        continue;
                    }
                } else if (sourceCircle.isNumber) {
                    if (tag.contains("min")) continue;
                    if (tag.contains("count")) {
                        var count1 = tag.getInt("count");
                        var count2 = stack.getCount();
                        tag.putInt("min", Math.min(count1, count2));
                        tag.putInt("max", Math.max(count1, count2));
                    } else {
                        tag.putInt("count", stack.getCount());
                    }
                    continue;
                }
                if (isCrafting) {
                    craftingItemPositions.add(new Pair<>(influencePos.getKey().subtract(influencePos.getValue()), stack));
                }
                tag.putInt("filter_" + filterIndex, Item.getId(stack.getItem()));
                filterIndex++;
            } else {
                BlockPos pos = influencePos.getKey();
                BlockFocus blockFocus = diagramData.allBlockFoci.get(pos);
                if (blockFocus == null) {
                    continue;
                }
                var blockItem = blockFocus.getItem();
                if (blockItem.isEmpty()) {
                    tag.put("position_" + positionIndex, new PositionOrSpindle(pos).toTag());
                } else if (blockItem.is(OtherverseItems.WEB_OF_FATE.get())) {
                    tag.put("position_" + positionIndex, new PositionOrSpindle(blockFocus).toTag());
                } else {
                    var blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null && blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                        tag.putInt("inventory_" + blockIndex, Item.getId(blockItem.getItem()));
                    } else {
                        tag.putInt("block_" + blockIndex, Item.getId(blockItem.getItem()));
                    }
                    blockIndex++;
                }
            }
        }
        if (isCrafting) {
            var sum = new BlockPos(0, 0, 0);
            for (var pos : craftingItemPositions) {
                sum = sum.offset(pos.getA());
            }
            while (sum.getZ() < Math.abs(sum.getX())) {
                sum = new BlockPos(sum.getZ(), 0, -sum.getX());
                for (int i = 0; i < craftingItemPositions.size(); i++) {
                    var pos = craftingItemPositions.get(i).getA();
                    craftingItemPositions.set(i, new Pair<>(
                            new BlockPos(pos.getZ(), 0, -pos.getX()), craftingItemPositions.get(i).getB()));
                }
                var minX = Integer.MAX_VALUE;
                var maxX = Integer.MIN_VALUE;
                var minZ = Integer.MAX_VALUE;
                var maxZ = Integer.MIN_VALUE;
                for (var pair : craftingItemPositions) {
                    minX = Math.min(minX, pair.getA().getX());
                    maxX = Math.max(maxX, pair.getA().getX());
                    minZ = Math.min(minZ, pair.getA().getZ());
                    maxZ = Math.max(maxZ, pair.getA().getZ());
                }
                var width = maxX - minX + 1;
                var height = maxZ - minZ + 1;
                CraftingContainer craftingContainer = new CraftingContainer(new DiagramCraftingMenu(), width, height);
                for (var pair : craftingItemPositions) {
                    craftingContainer.setItem((pair.getA().getX() - minX) + (maxZ - pair.getA().getZ()) * width, pair.getB());
                }
                Optional<CraftingRecipe> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
                if (recipe.isPresent()) {
                    tag.putString("recipe_id", recipe.get().getId().toString());
                    tag.putInt("recipe_result", Item.getId(recipe.get().getResultItem().getItem()));
                }
            }
        }
        return tag;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().hasTag()) {
            return;
        }
        CompoundTag tag = event.getItemStack().getTag();
        if (tag == null || !tag.contains("contract")) {
            return;
        }
        CompoundTag contractTag = tag.getCompound("contract");
        var range = contractTag.getInt("range");
        if(range > 8) event.getToolTip().add(Component.literal("Range: " + range + " blocks"));
        makeTooltipRecursive(event.getToolTip(), contractTag, "");
    }

    @SubscribeEvent
    public static void onItem(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown()) return;
        if (!event.getItemStack().is(Tags.Items.SEEDS)) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(OtherverseBlocks.CHALK_LINE.get())) return;
        event.setUseBlock(Event.Result.ALLOW);
        event.setUseItem(Event.Result.DENY);
    }

    private static void makeTooltipRecursive(List<Component> toolTip, CompoundTag contractTag, String prefix) {
        String taskType = contractTag.getString("type");
        if (contractTag.contains("corner_0")) {
            toolTip.add(Component.literal(prefix + "from ")
                    .append(getPositionComponent(contractTag.getCompound("corner_0")))
                    .append(" to ")
                    .append(getPositionComponent(contractTag.getCompound("corner_1"))));
        }
        if (contractTag.contains("recipe_result")) {
            var item = Item.byId(contractTag.getInt("recipe_result"));
            toolTip.add(Component.literal(prefix).append(item.getDescription()));
        }
        for (String key : contractTag.getAllKeys()) {
            if (key.startsWith("corner") || key.startsWith("tag") || key.startsWith("recipe") || key.equals("min") || key.equals("max") || key.equals("range")) {
                continue;
            }
            if (key.endsWith("_basis")) {
                continue;
            }
            if (key.equals("count")) {
                if (contractTag.contains("min")) {
                    toolTip.add(Component.literal(prefix + "between "
                            + contractTag.getInt("min") + " and " + contractTag.getInt("max")));
                } else {
                    toolTip.add(Component.literal(prefix + "exactly " + contractTag.getInt(key)));
                }
                continue;
            }
            String prep = (taskType.equals("take") || taskType.equals("craft")) ? "from " : taskType.equals("go") ? "to " : "in/on ";
            if (key.startsWith("filter") || key.startsWith("block") || key.startsWith("inventory")) {
                var item = Item.byId(contractTag.getInt(key));
                if (taskType.equals("attack")) {
                    if (key.startsWith("block") || key.startsWith("inventory")) {
                        var s = item == Items.HAY_BLOCK ? "passive mobs"
                                : item == Items.TARGET ? "hostile mobs"
                                : item == Items.CRAFTING_TABLE ? "players"
                                : item == Items.CHAIN ? "bound mobs"
                                : "";
                        if (!s.isEmpty()) {
                            toolTip.add(Component.literal(prefix + s));
                            continue;
                        }
                    }
                    var entities = LootHelper.entitiesThatDropItem.get(item);
                    if (entities == null) {
                        continue;
                    }
                    for (var entity : entities) {
                        toolTip.add(Component.literal(prefix).append(entity.getDescription()));
                    }
                    continue;
                }
                if (key.startsWith("filter") && !taskType.equals("go") && !taskType.equals("craft")) {
                    prep = "";
                }
                toolTip.add(Component.literal(prefix + prep).append(item.getDescription()));
                var index = key.substring(7);
                var tagitemkey = "tag_item_" + index;
                if (contractTag.contains(tagitemkey)) {
                    toolTip.add(Component.literal("-" + prefix + "tag = " + contractTag.getCompound(tagitemkey)));
                }
                var tagblockkey = "tag_block_" + index;
                if (contractTag.contains(tagblockkey)) {
                    var compound = contractTag.getCompound(tagblockkey);
                    for (var k : compound.getAllKeys()) {
                        toolTip.add(Component.literal("-" + prefix + k + " = " + compound.getString(k)));
                    }
                }
                continue;
            }
            if (key.startsWith("offset")) {
                toolTip.add(Component.literal(prefix + "offset ")
                        .append(getPositionComponent(contractTag.getCompound(key), contractTag.getIntArray(key + "_basis"))));
                continue;
            }
            if (key.startsWith("position")) {
                CompoundTag pos = contractTag.getCompound(key);
                toolTip.add(Component.literal(prefix + prep).append(getPositionComponent(pos)));
                continue;
            }
            if (key.startsWith("task")) {
                CompoundTag subTag = contractTag.getCompound(key);
                if (subTag.contains("type")) {
                    String typestring = subTag.getString("type");
                    typestring = typestring.substring(0, 1).toUpperCase() + typestring.substring(1);
                    toolTip.add(Component.literal(prefix + typestring));
                }
            } else if (!key.equals("type")) {
                toolTip.add(Component.literal(prefix + "On " + key + ":"));
            }
            makeTooltipRecursive(toolTip, contractTag.getCompound(key), "-" + prefix);
        }
    }

    private static Component getPositionComponent(CompoundTag tag) {
        var positionOrSpindle = new PositionOrSpindle(tag);
        if (positionOrSpindle.isPosition) {
            return getPositionComponent(new int[]{
                    positionOrSpindle.x, positionOrSpindle.y, positionOrSpindle.z
            });
        }
        var component = Component.literal(positionOrSpindle.playerName + " "
                + positionOrSpindle.color.getName());
        return component.withStyle(Style.EMPTY.withColor(positionOrSpindle.color.getTextColor()));
    }

    private static Component getPositionComponent(CompoundTag tag, int[] basis) {
        var positionOrSpindle = new PositionOrSpindle(tag);
        if (positionOrSpindle.isPosition) {
            return getPositionComponent(new int[]{
                    positionOrSpindle.x - basis[0],
                    positionOrSpindle.y - basis[1],
                    positionOrSpindle.z - basis[2]
            });
        }
        var component = Component.literal(positionOrSpindle.playerName + "'s "
                + positionOrSpindle.color.getName() + " position");
        return component.withStyle(Style.EMPTY.withColor(positionOrSpindle.color.getTextColor()));
    }


    private static Component getPositionComponent(int[] pos) {
        var blockpos = new BlockPos(pos[0], pos[1], pos[2]);
        var positions = new HashSet<BlockPos>();
        var positionComponent = Component.literal(String.format("[%d,%d,%d]", blockpos.getX(), blockpos.getY(), blockpos.getZ()));
        if (!positions.isEmpty()) {
            positionComponent = positionComponent.withStyle(Style.EMPTY.withColor(TextColor.fromRgb((20 << 16) | (128 << 8) | 255)));
        }
        return positionComponent;
    }

    public static void applyContract(CompoundTag contractTag, Mob mob, boolean fromPlayerClick) {
        for (var g : mob.goalSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof BoundGoal bg) {
                bg.setContract(contractTag, fromPlayerClick);
                OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new BindingUpdateMessage(mob, BindingUpdateMessage.BindingUpdateType.CONTRACT, false));
                return;
            }
        }
    }
}
