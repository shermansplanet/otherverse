package com.shermansplanet.otherverse;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.binding.FleshbindingManager;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.binding.MobTransfusions;
import com.shermansplanet.otherverse.implement.ImplementManager;
import com.shermansplanet.otherverse.integrations.jei.OtherverseJeiPlugin;
import com.shermansplanet.otherverse.spirits.SpiritLabeler;
import com.shermansplanet.otherverse.spirits.SpiritTransfusions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashMap;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PracticeWorldManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean noJeiPending = false;
    public static boolean jeiInitialized = false;
    public static boolean worldSetUp = false;

    public static abstract class WorldTraitComponent<T> {
        public WorldTrait<?> parent;
        public T data = null;

        public void setData(T data) {
            this.data = data;
            this.parent.trySynthesize();
        }
    }

    public static abstract class WorldTrait<T> {
        public final WorldTraitComponent<?>[] components;

        public T data = null;

        public WorldTrait(WorldTraitComponent<?>[] components) {
            this.components = components;
            for (var component : components) {
                component.parent = this;
            }
        }

        public void trySynthesize() {
            if (!synthesize()) return;
            PracticeWorldManager.tryUpdatePlayers();
        }

        public abstract boolean synthesize();

        public void clear() {
            data = null;
            for (var component : components) {
                component.data = null;
            }
        }
    }

    @SubscribeEvent
    public static void levelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.getServer().overworld() != sl) return;
        var smeltingRecipes = sl.getServer().getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        SpiritLabeler.analyzeSmeltingRecipes(smeltingRecipes);
        MobBindingInfluenceUtils.analyzeMobInstances(sl.getServer().overworld());
    }

    @SubscribeEvent
    public static void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (worldSetUp && event.getEntity() instanceof ServerPlayer sp) {
            OtherversePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), getUpdateMessage());
        }
    }

    private static PracticeWorldUpdateMessage getUpdateMessage() {
        LOGGER.debug("GETTING UPDATE MESSAGE");
        var message = new PracticeWorldUpdateMessage(SpiritLabeler.SPIRIT_TYPE_OF.data,
                SpiritTransfusions.ALL_SPIRIT_TRANSFUSIONS.data,
                MobTransfusions.ALL_MOB_TRANSFUSIONS.data,
                MobBindingInfluenceUtils.ALL_BINDING_INFLUENCES.data,
                MobBindingInfluenceUtils.mobSpirits);
        LOGGER.debug("GOT UPDATE MESSAGE");
        return message;
    }

    private static void tryUpdatePlayers() {
        if (SpiritLabeler.SPIRIT_TYPE_OF.data == null) {
            LOGGER.debug("WORLD MANAGER: NO SPIRIT TYPES");
        }
        if (SpiritTransfusions.ALL_SPIRIT_TRANSFUSIONS.data == null) {
            LOGGER.debug("WORLD MANAGER: NO SPIRIT TRANSFUSIONS");
        }
        if (MobTransfusions.ALL_MOB_TRANSFUSIONS.data == null) {
            LOGGER.debug("WORLD MANAGER: NO MOB TRANSFUSIONS");
        }
        if(SpiritLabeler.SPIRIT_TYPE_OF.data == null || SpiritTransfusions.ALL_SPIRIT_TRANSFUSIONS.data == null || MobTransfusions.ALL_MOB_TRANSFUSIONS.data == null) return;
        worldSetUp = true;
        LOGGER.debug("WORLD MANAGER: SENDING MESSAGE");
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () ->
                {
                    LOGGER.debug("SERVER SENDING...");
                    OtherversePacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), getUpdateMessage());
                    LOGGER.debug("SERVER SENT");
                }
        );
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                {
                    LOGGER.debug("CLIENT UPDATING...");
                    OtherverseJeiPlugin.addPracticeRecipes();
                    LOGGER.debug("UPDATED");
                }
        );
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            SpiritLabeler.AnalyzeTags();
        }
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        SpiritLabeler.SPIRIT_TYPE_OF.clear();
        SpiritTransfusions.ALL_SPIRIT_TRANSFUSIONS.clear();
        MobTransfusions.ALL_MOB_TRANSFUSIONS.clear();
        MobBindingInfluenceUtils.ALL_BINDING_INFLUENCES.clear();
        MobBindingInfluenceUtils.mobSpirits.clear();
        worldSetUp = false;
        noJeiPending = false;
        jeiInitialized = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerStart(ServerStartedEvent event) {
        ImplementManager.preloadImplements();
    }

    public static void onColorsReceived(HashMap<Item, SpiritLabeler.SpiritAmount[]> mappings, NetworkEvent.Context context) {
        SpiritLabeler.onColorsUpdated(mappings);
        FleshbindingManager.addWoodTextures();
    }
}
