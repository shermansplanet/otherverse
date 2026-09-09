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

import java.util.HashMap;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MacabreCompat {
    public enum QuestStage {UNAWAKENED, AWAKENED, ENLIGHTENED, SAVIOR}

    private static final Logger LOGGER = LogUtils.getLogger();

    //The hemoslime of the rotting field, the abhorrent fly of the decaying meadow, the fatty ogre of the mountain maw, the monolith of the mortem swamp, the veintree of the valley of eyes...

    private static CompoundTag getBookTag() {
        try {
            return TagParser.parseTag("{pages:['{\"text\":\"It hurts. To give your life to a great work - sweat, blood, tears - then realize you\\'ve doomed yourself to an eternity in a twisted perversion of your magnum opus... I would not wish it on anyone. I feel my mind stretch and tear, synapses splayed bare in eternal contemplation of my failure.\",\"color\":\"dark_red\"}'," +
                    "'[\"\",{\"text\":\"I\\'m becoming \",\"color\":\"dark_red\"},{\"text\":\"hollow.\",\"color\":\"red\"},{\"text\":\" Multifid. More than human yet so much less. All I can hope is that my final cogent notes can guide one like you, fellow Practitioner, who has fallen into this realm of horrors by accident. If you came here on purpose... I would rather not speculate on why.\",\"color\":\"dark_red\"}]'," +
                    "'{\"text\":\"What you see around you was once a beautiful pocket Realm where I carried out alchemical experiments in my Demesne. I had made a breakthrough, you see - I had figured out how to alchemically recreate the five pillars. Time, Nature, Fate, War, Death.\",\"color\":\"dark_red\"}'," +
                    "'{\"text\":\"With five incarnate homunculi supporting my Realm, I could fine-tune laws of reality like organs of a great beast. In my hubris and excitement, I grew careless. I took shortcuts. And I soon learned the price, as my homunculi were twisted into abyssal False Prophets.\",\"color\":\"dark_red\"}'," +
                    "'[\"\",{\"text\":\"And this is what happens when such monstrosities are the fundamental building blocks of a Realm. Gore and suffering, repeated into infinity. I can feel myself - my Self - repeating as well. I am divided, divided, divided until I am a cancerous nothing. \",\"color\":\"dark_red\"},{\"text\":\"Hollowness\",\"color\":\"red\"},{\"text\":\", metastasized.\",\"color\":\"dark_red\"}]'," +
                    "'{\"text\":\"You may even be able to find me - one cell of the infinite Horror I am becoming - deep in my Demesne. Look for a pillar of toothy meat, pink and raw, jutting up from this hellish landscape. And when you find me, kill me. Reduce my torment. Or, better yet, bring me with you. Show me grass and sky.\",\"color\":\"dark_red\"}'," +
                    "'[\"\",{\"text\":\"If you do kill one of me, you will find in my \",\"color\":\"dark_red\"},{\"text\":\"hollow\",\"color\":\"red\"},{\"text\":\" chest a root of infestation: a parasitic extension of my Realm. Destroy it. Please. I know you may be tempted to use it as I did, to summon the false prophets at their altars and try to bind them to your will. Such is the nature of Practitioners.\",\"color\":\"dark_red\"}]'," +
                    "'{\"text\":\"I cannot stop you from doing so. But I can warn you: they will never be entirely controlled. Even if you somehow bind them, their destructive Abyssal power will always bleed through. Everything around them will suffer, even you.\\\\n\\\\nAnd you\\'ll deserve it.\",\"color\":\"dark_red\"}'," +
                    "'[\"\",{\"text\":\"God\",\"color\":\"dark_red\"},{\"text\":\"d\",\"color\":\"red\"},{\"text\":\"s\",\"color\":\"dark_red\"},{\"text\":\"s\",\"color\":\"red\"},{\"text\":\", m\",\"color\":\"dark_red\"},{\"text\":\"m\",\"color\":\"red\"},{\"text\":\"y\",\"color\":\"dark_red\"},{\"text\":\"y\",\"color\":\"red\"},{\"text\":\" h\",\"color\":\"dark_red\"},{\"text\":\"h\",\"color\":\"red\"},{\"text\":\"a\",\"color\":\"dark_red\"},{\"text\":\"a\",\"color\":\"red\"},{\"text\":\"n\",\"color\":\"dark_red\"},{\"text\":\"n\",\"color\":\"red\"},{\"text\":\"d\",\"color\":\"dark_red\"},{\"text\":\"d\",\"color\":\"red\"},{\"text\":\"s\",\"color\":\"dark_red\"},{\"text\":\"s\",\"color\":\"red\"},{\"text\":\"... how ma\",\"color\":\"dark_red\"},{\"text\":\"m\",\"color\":\"red\"},{\"text\":\"n\",\"color\":\"dark_red\"},{\"text\":\"a\",\"color\":\"red\"},{\"text\":\"y\",\"color\":\"dark_red\"},{\"text\":\"n\",\"color\":\"red\"},{\"text\":\" y\",\"color\":\"red\"},{\"text\":\"t i\",\"color\":\"dark_red\"},{\"text\":\"t\",\"color\":\"red\"},{\"text\":\"m\",\"color\":\"dark_red\"},{\"text\":\"i\",\"color\":\"red\"},{\"text\":\"e\",\"color\":\"dark_red\"},{\"text\":\"m\",\"color\":\"red\"},{\"text\":\"s\",\"color\":\"dark_red\"},{\"text\":\"e\",\"color\":\"red\"},{\"text\":\" s\",\"color\":\"red\"},{\"text\":\"am I writing this?\\\\n\\\\nThere\\'s less time than I thought.\\\\n\\\\nI don\\'t have much help to offer, but I can give what little knowledge may still remain useful.\",\"color\":\"dark_red\"}]'," +
                    "'{\"text\":\"Some of my twisted works retain their Practical use, but can be difficult to track down or figure out. Now that you recognize my handwriting, you should start to see my notes in tooltips.\",\"color\":\"dark_red\"}'," +
                    "'{\"text\":\"They may help you survive long enough to escape. Dare I hope you bring me with you? Or should I fear what I might inflict on the world outside this hell? I don\\'t have the heart to decide. I don\\'t have a heart at all. Where has it gone?\",\"color\":\"dark_red\"}'," +
                    "'{\"text\":\"A heart beats, somewhere, everywhere, but it is not mine.\\\\n\\\\nNot any more.\",\"color\":\"dark_red\"}'],title:\"final notes\",author:\"???\"}");
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static final HashMap<String, String> tooltipHints = new HashMap<>();

    static {
        tooltipHints.put("entity.hemoslime", "A denizen of the rotting fields. Its blood is my blood, a mocking coagulation devoid of Self.");
        tooltipHints.put("entity.cave_dweller", "What remains of my assistants, hiding underground. They are skittish things, quick to attack but also quick to flee.");
        tooltipHints.put("entity.cave_maggot", "An experiment in ore processing, abandoned underground. Thank the spirits I never finished its life cycle; I would hate to see what sort of fly this twisted form hatches into.");
        tooltipHints.put("entity.eyetree", "Once a source of vitae for my homunculi, now it can only produce endless swarms of those eerie hovering eyeballs.");
        tooltipHints.put("entity.fatty", "I figured out how to store Self in fat, just as it is conducted by blood. But the only Self remaining in these beasts of the Mountain Maw has denatured into something putrid and bilious.");
        tooltipHints.put("entity.fly", "These weren't even my creation. Just normal flies in my Demesne, now abhorrent things buzzing around the decaying meadows. But if you have the patience to catch enough of them...");
        tooltipHints.put("entity.infested", "I had several apprentices. One of them didn't make it out in time, and suffered my fate alongside me. She was my most promising pupil; now all that fills her head are maggots.");
        tooltipHints.put("entity.monolith", "If you see one of these in a mortem swamp, best not to look too closely at its faces. You may recognize your own.");
        tooltipHints.put("entity.veintree_mid", "In the valley of eyes, these trees weep blood. They can be coerced into producing useful blood plasma, if fed correctly.");
        tooltipHints.put("entity.gorebat", "These venomous flying monstrosities can be found in the Mountain maw. Certain towers also have spawners for them at the top.");
        tooltipHints.put("entity.spitter", "A denizen of the Mountain Maw.");
        tooltipHints.put("entity.gorehound", "Found in rotting fields and gloom forests.");
        tooltipHints.put("entity.gargantuan_molar", "Found in teething forests.");
        tooltipHints.put("entity.molar", "Found in teething forests.");
        tooltipHints.put("entity.worm", "Found in decaying meadows.");
        tooltipHints.put("entity.desert_worm", "Found in the lifeless pits and deathless valleys.");
        tooltipHints.put("entity.fernrot", "Found in decaying meadows.");
        tooltipHints.put("bloodfungus", "One of the few things that grow here that still responds to bone meal... perhaps because it has barely been warped from its original form. It can be found in rotting fields and gloom forests.");
        tooltipHints.put("mortis_essence", "My innovation on the ender pearl, flying much further and with less harm to the user. If only my other projects had seen such success...");
        tooltipHints.put("sigil_of_disgust", "Allows you to spread harmful choking spores around you, dealing great damage.");
        tooltipHints.put("bag_of_gore", "Allows you to temporarily summon a few goreslimes to aid you in combat.");
        tooltipHints.put("cubeomeat", "Transforms blocks into toughened meat that is very difficult to break and rich in Protection spirits.");
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
            var hint = tooltipHints.get(prefix + key.getPath().replace("_night", ""));
            if (hint != null)
                event.getToolTip().add(Component.literal(hint).withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
        });
    }
}
