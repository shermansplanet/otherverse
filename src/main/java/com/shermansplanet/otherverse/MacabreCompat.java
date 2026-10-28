package com.shermansplanet.otherverse;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.BindingManager;
import com.shermansplanet.otherverse.binding.IdolItem;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.PickedUpItemTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MacabreCompat {
    public enum QuestStage {UNAWAKENED, AWAKENED, ENLIGHTENED, SAVIOR}

    private static final Logger LOGGER = LogUtils.getLogger();

    //The hemoslime of the rotting field, the abhorrent fly of the decaying meadow, the fatty ogre of the mountain maw, the monolith of the mortem swamp, the veintree of the valley of eyes...

    private static CompoundTag getBookTag() {
        try {
            return TagParser.parseTag("{pages:['{\"translate\":\"otherverse.macabre.final_notes.page_1\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_2\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_3\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_4\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_5\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_6\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_7\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_8\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_9\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_10\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_11\",\"color\":\"dark_red\"}','{\"translate\":\"otherverse.macabre.final_notes.page_12\",\"color\":\"dark_red\"}'],title:\"final notes\",author:\"???\"}");
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static final HashSet<String> tooltipHints = new HashSet<>();

    static {
        tooltipHints.add("entity.hemoslime");
        tooltipHints.add("entity.cave_dweller");
        tooltipHints.add("entity.cave_maggot");
        tooltipHints.add("entity.eyetree");
        tooltipHints.add("entity.fatty");
        tooltipHints.add("entity.fly");
        tooltipHints.add("entity.infested");
        tooltipHints.add("entity.monolith");
        tooltipHints.add("entity.veintree_mid");
        tooltipHints.add("entity.gorebat");
        tooltipHints.add("entity.spitter");
        tooltipHints.add("entity.gorehound");
        tooltipHints.add("entity.gargantuan_molar");
        tooltipHints.add("entity.molar");
        tooltipHints.add("entity.worm");
        tooltipHints.add("entity.desert_worm");
        tooltipHints.add("entity.fernrot");
        tooltipHints.add("bloodfungus");
        tooltipHints.add("mortis_essence");
        tooltipHints.add("sigil_of_disgust");
        tooltipHints.add("bag_of_gore");
        tooltipHints.add("cubeomeat");
    }

    @SubscribeEvent
    public static void onLife(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl != sl.getServer().overworld() || !(event.getEntity() instanceof Mob mob)) return;
        var key = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (key == null || !key.getNamespace().equals("macabre") || !key.getPath().equals("the_hollow_man")) return;
        var player = (ServerPlayer) sl.getNearestPlayer(event.getEntity(), 16);
        if (player == null) return;
        var practiceHolder = player.getCapability(ImplementManager.PRACTICE_HANDLER);
        if (!practiceHolder.isPresent() || practiceHolder.resolve().isEmpty()) return;
        var practice = practiceHolder.resolve().get();
        if (practice.getQuestStage() != QuestStage.ENLIGHTENED) return;
        practice.setQuestStage(QuestStage.SAVIOR, player);
        BindingManager.enforceLoyalty(player, mob, false);
        Otherverse.ADVANCEMENTS.trigger(player, "fresh_air");
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        var entity = event.getSource().getEntity();
        if (entity == null) {
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        var key = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (!key.getNamespace().equals("macabre")) {
            return;
        }
        var practiceHolder = player.getCapability(ImplementManager.PRACTICE_HANDLER);
        if (!practiceHolder.isPresent() || practiceHolder.resolve().isEmpty()) {
            return;
        }
        var practice = practiceHolder.resolve().get();
        if (practice.getQuestStage() != QuestStage.AWAKENED) {
            return;
        }
        var bookTag = getBookTag();
        bookTag.putBoolean("is_hollow_man_book", true);
        var stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.setTag(bookTag);
        var mob = event.getEntity();
        ItemEntity itementity = new ItemEntity(player.serverLevel(),
                mob.getX(0.5f), mob.getY(0.5f), mob.getZ(0.5f), stack);
        itementity.setDefaultPickUpDelay();
        player.serverLevel().addFreshEntity(itementity);
    }

    @SubscribeEvent
    public static void onUseBook(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItemStack().hasTag() || !event.getItemStack().getTag().getBoolean("is_hollow_man_book")) return;
        var practiceHolder = player.getCapability(ImplementManager.PRACTICE_HANDLER);
        if (!practiceHolder.isPresent() || practiceHolder.resolve().isEmpty()) {
            return;
        }
        var practice = practiceHolder.resolve().get();
        if (practice.getQuestStage() != QuestStage.AWAKENED) {
            return;
        }
        practice.setQuestStage(QuestStage.ENLIGHTENED, player);
    }

    @SubscribeEvent
    public static void onUseBook(ItemTooltipEvent event) {
        if (event.getEntity() == null) return;
        event.getEntity().getCapability(ImplementManager.PRACTICE_HANDLER).ifPresent(practice -> {
            if (practice.getQuestStage() == QuestStage.UNAWAKENED || practice.getQuestStage() == QuestStage.AWAKENED)
                return;
            var stack = event.getItemStack();
            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            var prefix = "";
            if (stack.getItem() == OtherverseItems.IDOL.get()) {
                var et = IdolItem.getType(stack);
                key = ForgeRegistries.ENTITY_TYPES.getKey(et);
                prefix = "entity.";
            }
            if (key == null || !key.getNamespace().equals("macabre")) return;
            var hint = prefix + key.getPath().replace("_night", "");
            if (tooltipHints.contains(hint))
                event.getToolTip().add(Component.translatable("otherverse.macabre.hint." + hint).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
        });
    }
}
