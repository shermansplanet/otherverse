package com.shermansplanet.otherverse.implement;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Keybindings;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.OtherversePacketHandler;
import com.shermansplanet.otherverse.capabilities.IPracticeCapability;
import com.shermansplanet.otherverse.capabilities.PracticeCapabilityAttacher;
import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.IFocus;
import com.shermansplanet.otherverse.diagrams.SelfManager;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.spirits.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ImplementManager {
    public static final Capability<IPracticeCapability> PRACTICE_HANDLER = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final int BUCKET_CAPACITY = 333;
    public static final float BUCKET_BONUS = 4f / 3f;

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final HashMap<Item, Integer> durabilities = new HashMap<>();
    public static final String IMPLEMENT_MARKER = "otherverseImplement";

    public static final int IMPLEMENT_UI_COLOR = 0xaa2200;

    private static AttributeModifier implementModifier = new AttributeModifier("implement_bonus", 1f / 3f, AttributeModifier.Operation.MULTIPLY_TOTAL);

    static {
        durabilities.put(Items.COBBLESTONE, 64);
        durabilities.put(Items.COBBLED_DEEPSLATE, 64);
        durabilities.put(Items.STONE, 64);
        durabilities.put(Items.DEEPSLATE, 64);
        durabilities.put(Items.SAND, 64);
        durabilities.put(Items.DIRT, 64);
        durabilities.put(Items.RED_SAND, 64);
        durabilities.put(Items.ANDESITE, 64);
        durabilities.put(Items.GRANITE, 64);
        durabilities.put(Items.DIORITE, 64);
        durabilities.put(Items.TUFF, 64);
        durabilities.put(Items.GRAVEL, 64);
        durabilities.put(Items.NETHERRACK, 64);
        durabilities.put(Items.SCAFFOLDING, 64);
        durabilities.put(Items.ICE, 64);
        durabilities.put(Items.REDSTONE, 64);
        durabilities.put(Items.NETHER_BRICKS, 64);
        durabilities.put(Items.BLACKSTONE, 64);
        durabilities.put(Items.BASALT, 64);
        durabilities.put(Items.TORCH, 64);
        durabilities.put(Items.LADDER, 64);
        durabilities.put(Items.VINE, 64);
        durabilities.put(Items.RAIL, 64);
        durabilities.put(Items.BIG_DRIPLEAF, 64);
        durabilities.put(Items.LILY_PAD, 64);

        durabilities.put(Items.SOUL_SAND, 48);
        durabilities.put(Items.LANTERN, 48);
        durabilities.put(Items.INFESTED_STONE, 48);

        durabilities.put(Items.LAVA_BUCKET, 32);
        durabilities.put(Items.AXOLOTL_BUCKET, 32);
        durabilities.put(Items.COD_BUCKET, 32);
        durabilities.put(Items.POWDER_SNOW_BUCKET, 32);
        durabilities.put(Items.PUFFERFISH_BUCKET, 32);
        durabilities.put(Items.TADPOLE_BUCKET, 32);
        durabilities.put(Items.TROPICAL_FISH_BUCKET, 32);

        durabilities.put(Items.OBSIDIAN, 32);
        durabilities.put(Items.COBWEB, 32);
        durabilities.put(Items.GLOWSTONE, 32);

        durabilities.put(Items.TNT, 16);

        durabilities.put(Items.WITHER_SKELETON_SKULL, 3);
    }

    public static final AttributeModifier singleRangeAttributeModifier =
            new AttributeModifier(UUID.fromString("2c10e499-6f4d-4ac0-a594-c0845f4f864c"), "Range modifier", 3,
                    AttributeModifier.Operation.ADDITION);

    private static final Supplier<Multimap<Attribute, AttributeModifier>> rangeModifier = Suppliers.memoize(() ->
            ImmutableMultimap.of(ForgeMod.REACH_DISTANCE.get(), singleRangeAttributeModifier));

    @SubscribeEvent
    public static void fillBucket(FillBucketEvent event) {
        var stack = event.getEmptyBucket().copy();
        if (!isImplement(stack)) return;
        SpiritType targetSpiritType = null;
        var newSpirits = 0;

        var blockhitresult = (BlockHitResult) event.getTarget();
        var p_40703_ = event.getLevel();
        var p_40704_ = event.getEntity();
        if (blockhitresult.getType() == HitResult.Type.MISS) {
            return;
        } else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return;
        } else {
            BlockPos blockpos = blockhitresult.getBlockPos();
            Direction direction = blockhitresult.getDirection();
            BlockPos blockpos1 = blockpos.relative(direction);
            if (p_40703_.mayInteract(p_40704_, blockpos) && p_40704_.mayUseItemAt(blockpos1, direction, stack)) {
                BlockState blockstate1 = p_40703_.getBlockState(blockpos);
                if (blockstate1.getBlock() instanceof BucketPickup) {
                    BucketPickup bucketpickup = (BucketPickup) blockstate1.getBlock();
                    ItemStack itemstack1 = bucketpickup.pickupBlock(p_40703_, blockpos, blockstate1);
                    bucketpickup.getPickupSound(blockstate1).ifPresent((p_150709_) -> {
                        p_40704_.playSound(p_150709_, 1.0F, 1.0F);
                    });
                    if (itemstack1.is(Items.WATER_BUCKET)) {
                        targetSpiritType = Spirits.WATER;
                        newSpirits = 27;
                    } else if (itemstack1.is(Items.LAVA_BUCKET)) {
                        targetSpiritType = Spirits.FIRE;
                        newSpirits = 200;
                    } else if (itemstack1.is(Items.POWDER_SNOW_BUCKET)) {
                        targetSpiritType = Spirits.COLD;
                        newSpirits = 27;
                    }
                }
            }
        }

        var hallowSpiritType = HallowHelper.getSpiritType(stack);
        var itemTag = stack.getTag();
        if (hallowSpiritType == null && targetSpiritType != null) {
            hallowSpiritType = targetSpiritType;
            HallowHelper.addFakeEnchantment(itemTag);
            var hallowTag = new CompoundTag();
            hallowTag.putInt("capacity", 999);
            hallowTag.putInt("spirit_count", 0);
            hallowTag.putString("spirit_type", hallowSpiritType.label());
            itemTag.put("hallow", hallowTag);
        }
        if (hallowSpiritType == targetSpiritType) {
            var hallowTag = itemTag.getCompound("hallow");
            var spiritCount = hallowTag.getInt("spirit_count");
            hallowTag.putInt("spirit_count", Math.min(spiritCount + newSpirits, 999));
        }
        event.setFilledBucket(stack);
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void bonemealableImplement(PlayerInteractEvent.RightClickBlock event) {
        var item = event.getEntity().getItemInHand(event.getHand());
        if (!(item.getItem() instanceof BlockItem bi) || !isImplement(item) || !item.getTag().contains("implement_max_uses"))
            return;
        var blockstate = event.getLevel().getBlockState(event.getPos());
        if (blockstate.getBlock() != bi.getBlock()) return;
        if (!(blockstate.getBlock() instanceof BonemealableBlock bb)) return;
        event.setCanceled(true);
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        bb.performBonemeal(sl, sl.random, event.getPos(), blockstate);
        sl.levelEvent(1505, event.getPos(), 0);
        var tag = item.getTag();
        var remainingUses = tag.getInt("implement_remaining_uses") - 1;
        if (remainingUses > 0) {
            tag.putInt("implement_remaining_uses", remainingUses);
        } else {
            item.shrink(1);
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        var implementData = getImplementData(event.getEntity());
        if (implementData.isEmpty()) return;
        var itemName = implementData.getString("item");
        if (event.getState().getBlock().asItem() == ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName))) {
            event.setNewSpeed(event.getNewSpeed() * 3);
        }
    }

    @SubscribeEvent
    public static void implementRangeIncrease(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        boolean holding = isImplement(player.getMainHandItem());
        CompoundTag persistentData = player.getPersistentData();
        boolean wasHolding = persistentData.contains(IMPLEMENT_MARKER);

        if (holding != wasHolding) {
            if (holding) {
                player.getAttributes().addTransientAttributeModifiers(rangeModifier.get());
                persistentData.putBoolean(IMPLEMENT_MARKER, true);
            } else {
                player.getAttributes().removeAttributeModifiers(rangeModifier.get());
                persistentData.remove(IMPLEMENT_MARKER);
            }
        }
    }

    @SubscribeEvent
    public static void attributeChange(ItemAttributeModifierEvent event) {
        if (!isImplement(event.getItemStack())) return;
        if (event.getItemStack().getItem() instanceof ArmorItem armor) {
            if (event.getSlotType() != armor.getSlot()) return;
            event.addModifier(event.getItemStack().is(Tags.Items.ARMORS_BOOTS)
                    ? Attributes.MOVEMENT_SPEED : Attributes.ARMOR, implementModifier);
        } else if (event.getItemStack().is(Tags.Items.TOOLS)) {
            if (event.getSlotType() != EquipmentSlot.MAINHAND) return;
            event.addModifier(Attributes.ATTACK_SPEED, implementModifier);
        }
    }

    public static CompoundTag getImplementData(IFocus focus) {
        var owner = (focus.getFocusLevel() instanceof ServerLevel sl) ? focus.getDiagram().getOwner(sl) : null;
        return owner == null ? new CompoundTag() : ImplementManager.getImplementData(owner);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (!Keybindings.KEY_IMPLEMENT.consumeClick()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        OtherversePacketHandler.INSTANCE.sendToServer(new FetchImplementMessage(true));
    }

    @SubscribeEvent
    public static void playerDeath(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        e.getEntity().getCapability(PRACTICE_HANDLER).ifPresent(practice ->
        {
            practice.setImplement(getImplementData(e.getOriginal()), null);
            practice.setFamiliar(FamiliarManager.getFamiliarData(e.getOriginal()), null);

            e.getOriginal().invalidateCaps();
            var implement = getImplementInstance(e.getEntity());
            if (implement.isEmpty()) return;
            e.getEntity().getInventory().add(implement);
        });
    }

    @SubscribeEvent
    public static void playerSpawn(EntityJoinLevelEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> practice.sync(player));
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> e) {
        PracticeCapabilityAttacher.attach(e);
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent e) {
        if (!isImplement(e.getItemStack())) return;
        var tag = e.getItemStack().getTag();
        if (tag == null) return;
        var spiritType = tag.getString("implement_spirit");
        e.getToolTip().add(Component.literal("Implement (" + spiritType + ")").withStyle(Style.EMPTY.withColor(IMPLEMENT_UI_COLOR)));
        if (tag.contains("implement_max_uses")) {
            e.getToolTip().add(Component.literal(
                    tag.getInt("implement_remaining_uses") + "/" + tag.getInt("implement_max_uses") + " uses left"
            ).withStyle(Style.EMPTY.withColor(IMPLEMENT_UI_COLOR)));
        }
    }

    @SubscribeEvent
    public static void wakeUp(PlayerWakeUpEvent e) {
        var player = e.getEntity();
        if (player.level.isClientSide) return;
        if (!getImplementData(player).isEmpty() && !player.isCreative()) return;
        var positionsToCheck = new ArrayList<BlockPos>();
        positionsToCheck.add(player.blockPosition());
        positionsToCheck.add(player.blockPosition().north());
        positionsToCheck.add(player.blockPosition().south());
        positionsToCheck.add(player.blockPosition().east());
        positionsToCheck.add(player.blockPosition().west());

        for (var pos : positionsToCheck) {
            var focus = DiagramManager.getOrCreateLevelData(player.getLevel()).allBlockFoci.get(pos);
            if (focus == null) continue;
            for (var influence : focus.getDiagram().influences.entrySet()) {
                if (!influence.getValue().equals(focus.getPos())) continue;
                if (!(player.level.getBlockEntity(influence.getKey()) instanceof ChalkCircle cc)) continue;
                if (!canBeImplement(cc.getItem())) continue;
                new ImplementumProcess(cc, 200, (ServerPlayer) player);
                return;
            }
        }
    }

    private static boolean canBeImplement(ItemStack item) {
        return item.is(Tags.Items.TOOLS) || item.is(Tags.Items.ARMORS) || item.is(Tags.Items.DYES)
                || durabilities.containsKey(item.getItem()) || item.isEdible() || item.is(Items.SCULK_SHRIEKER)
                || item.is(Items.CHAIN) || item.is(Items.BUCKET) || item.is(Items.FLINT_AND_STEEL);
    }

    @SubscribeEvent
    public static void eatItem(LivingEntityUseItemEvent.Finish e) {
        var itemStack = e.getItem();
        if (!isImplement(itemStack) || !itemStack.isEdible()) return;
        if (itemStack.hasTag() && itemStack.getTag().contains("implement_max_uses")) {
            var tag = itemStack.getTag();
            var newAmount = tag.getInt("implement_remaining_uses") - 1;
            if (newAmount <= 0) {
                return;
            }
            tag.putInt("implement_remaining_uses", newAmount);
            e.setResultStack(itemStack);
        }
    }

    @SubscribeEvent
    public static void onPlaceShrieker(PlayerInteractEvent.RightClickBlock e) {
        shriek(e);
    }

    @SubscribeEvent
    public static void onClickShrieker(PlayerInteractEvent.RightClickItem e) {
        shriek(e);
    }

    public static void shriek(PlayerInteractEvent e) {
        var stack = e.getItemStack();
        if (!isImplement(stack) || !stack.is(Items.SCULK_SHRIEKER)) return;
        e.setCancellationResult(InteractionResult.CONSUME);
        e.setCanceled(true);
        if (!(e.getLevel() instanceof ServerLevel sl)) return;
        var player = e.getEntity();
        if(!player.isCreative()) {
            if (player.experienceLevel < 1) return;
            player.giveExperienceLevels(-1);
        }
        var pos = player.blockPosition();
        var didSpawn = SpawnUtil.trySpawnMob(EntityType.WARDEN, MobSpawnType.TRIGGERED, sl, pos, 20, 5, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER).isPresent();
        if (!didSpawn) return;
        player.getInventory().removeItem(stack);
        Warden.applyDarknessAround(sl, Vec3.atCenterOf(pos), null, 40);
        sl.levelEvent(3007, pos, 0);
        sl.gameEvent(GameEvent.SHRIEK, pos, GameEvent.Context.of(player));
    }

    public static CompoundTag getImplementData(Player player) {
        AtomicReference<CompoundTag> tag = new AtomicReference<>(new CompoundTag());
        player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> {
            tag.set(practice.getImplement());
        });
        return tag.get();
    }

    public static ItemStack getImplementInstance(Player player) {
        var tag = getImplementData(player);
        if (!tag.contains("item")) return ItemStack.EMPTY;
        var itemName = tag.getString("item");
        var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)));
        addImplementTag(stack, tag);
        return stack;
    }

    public static boolean isImplement(ItemStack itemStack) {
        return itemStack.hasTag() && itemStack.getTag().contains("implement");
    }

    private static void addImplementTag(ItemStack item, CompoundTag playerImplementTag) {
        var itemTag = item.getOrCreateTag();
        itemTag.putBoolean("implement", true);
        itemTag.putInt("implement_mode", 0);
        if (item.getItem() instanceof DyeItem dye) {
            var spiritType = Spirits.colorsByDye.get(dye.getDyeColor());
            HallowHelper.addFakeEnchantment(itemTag);
            var hallowTag = new CompoundTag();
            hallowTag.putInt("capacity", 999);
            hallowTag.putInt("spirit_count", 999);
            hallowTag.putString("spirit_type", spiritType.label());
            itemTag.put("hallow", hallowTag);
        }
        itemTag.putString("implement_spirit", playerImplementTag.getString("spirit"));
        if (playerImplementTag.contains("Enchantments")) {
            itemTag.put("Enchantments", playerImplementTag.get("Enchantments"));
        }
        var durability = durabilities.get(item.getItem());
        if (item.isEdible()) {
            durability = 9;
        }
        if (durability != null) {
            itemTag.putInt("implement_max_uses", durability);
            itemTag.putInt("implement_remaining_uses", durability);
        }
    }

    public static void makeImplement(ServerPlayer player, ItemStack item) {
        var playerImplementTag = new CompoundTag();
        playerImplementTag.putString("item", ForgeRegistries.ITEMS.getKey(item.getItem()).toString());
        if (item.hasTag() && item.getTag().contains("Enchantments")) {
            playerImplementTag.put("Enchantments", item.getTag().get("Enchantments"));
        }
        var mostSpirit = Spirits.OVERWORLD;
        if (item.hasTag() && item.getTag().contains("hallow")) {
            mostSpirit = Spirits.spiritsByLabel.get(item.getTag().getCompound("hallow").getString("spirit_type"));
        } else {
            var mostAmount = 0;
            for (var spiritType : SpiritLabeler.getSpiritsFor(item.getItem()).entrySet()) {
                var amount = spiritType.getValue();
                if (Arrays.stream(Spirits.colorSpiritTypes).noneMatch((s -> s == spiritType.getKey()))) {
                    amount *= 32;
                }
                if (amount > mostAmount) {
                    mostAmount = amount;
                    mostSpirit = spiritType.getKey();
                }
            }
        }
        SpiritAffinityTracker.setImplementAffinity(mostSpirit, player);
        playerImplementTag.putString("spirit", mostSpirit.label());
        addImplementTag(item, playerImplementTag);
        player.getCapability(PRACTICE_HANDLER).ifPresent(practice -> practice.setImplement(playerImplementTag, player));
        Otherverse.ADVANCEMENTS.trigger(player, "implementum");
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!isImplement(event.getEntity().getItem())) return;
        event.setCanceled(true);
        event.getPlayer().addItem(event.getEntity().getItem());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var inventory = p.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var item = inventory.getItem(i);
            if (isImplement(item)) {
                inventory.removeItem(i, item.getCount());
            }
        }
    }

    public static void fetchImplement(ServerPlayer player) {
        var demesne = DemesnesManager.getData(player);
        if (demesne != null && demesne == DemesnesManager.getData(player.getLevel(), player.blockPosition())
                && demesne.getPerkLevel(DemesnesManager.DemesnePerk.VEIN_MINE) > 0
                && player.getMainHandItem().getItem() instanceof DiggerItem) {
            changeImplementMode(player.getMainHandItem(), player);
        }
        var implement = getImplementInstance(player);
        if (implement.isEmpty()) return;
        for (var item : player.getInventory().items) {
            if (isImplement(item)) {
                LOGGER.debug("already has implement: " + item);
                if (item.getTag().contains("hallow")) {
                    var hallow = item.getTag().getCompound("hallow");
                    if (!player.isCreative() && !SelfManager.ChangeSelf(player, -1)) return;
                    hallow.putInt("spirit_count", hallow.getInt("capacity"));
                }
                if (item.getItem() instanceof DiggerItem) {
                    changeImplementMode(item, player);
                }
                return;
            }
        }
        if (player.getInventory().getFreeSlot() == -1) {
            player.displayClientMessage(Component.literal("No free inventory slot in which to summon your Implement!"), true);
            return;
        }
        if (!player.isCreative() && !SelfManager.ChangeSelf(player, -1)) return;
        LOGGER.debug("adding implement: " + implement + " in " + player.getInventory().getFreeSlot());
        player.getInventory().add(implement);
    }

    private static void changeImplementMode(ItemStack item, ServerPlayer player) {
        var oldMode = item.getTag().getInt("implement_mode");
        var vals = ImplementVeinMining.MiningMode.values();
        var newModeIndex = (oldMode + 1) % vals.length;
        var newMode = ImplementVeinMining.MiningMode.values()[newModeIndex];
        player.displayClientMessage(Component.literal(switch (newMode) {
            case NONE -> "Breaking blocks normally";
            case SQUARE -> "Breaking blocks in 3x3 square";
            case TUNNEL -> "Breaking blocks in a tunnel";
            case ANY -> "Breaking any connected blocks";
        }), true);
        item.getTag().putInt("implement_mode", newModeIndex);
    }

    public static void preloadImplements() {
        for (var item : ForgeRegistries.ITEMS) {
            if (item instanceof BlockItem bi && bi.getBlock() instanceof BonemealableBlock) {
                durabilities.put(item, 32);
            }
        }
    }
}
