package com.shermansplanet.otherverse.artifacts;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.Diagram;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.SpiritLabeler;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArtifactManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void coalTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(OtherverseItems.REALM_WRACKED_COAL.get())) return;
        if (!event.getItemStack().hasTag()) return;
        var biomeTag = event.getItemStack().getTag().getCompound("wracked_biome");
        var location = biomeTag.getString("location");
        event.getToolTip().add(Component.translatable("biome." + location.replace(":", ".")));
        var spiritTag = biomeTag.getCompound("spirits");
        for (var spirit : spiritTag.getAllKeys()) {
            event.getToolTip().add(Component.literal(spiritTag.getInt(spirit) + " " + spirit));
        }
    }

    @SubscribeEvent
    public static void setBiome(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !event.getItemStack().is(OtherverseItems.REALM_WRACKED_COAL.get()))
            return;
        var biome = sl.getBiome(event.getEntity().blockPosition());
        var spiritTypes = MobBindingInfluenceUtils.getSpiritTypes(biome, sl);
        if (spiritTypes.isEmpty()) return;
        var total = 120;
        var spiritsPerType = (total / spiritTypes.size());
        var spiritAmountTag = new CompoundTag();
        for (var i = 0; i < spiritTypes.size(); i++) {
            var st = spiritTypes.get(i);
            var count = i == 0 ? (total - spiritsPerType * (spiritTypes.size() - 1)) : spiritsPerType;
            count += spiritAmountTag.getInt(st.label());
            spiritAmountTag.putInt(st.label(), count);
        }
        var tag = new CompoundTag();
        tag.putString("registry", biome.unwrapKey().get().registry().toString());
        tag.putString("location", biome.unwrapKey().get().location().toString());
        tag.put("spirits", spiritAmountTag);
        event.getItemStack().getOrCreateTag().put("wracked_biome", tag);
    }

    @SubscribeEvent
    public static void makeCoal(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ItemEntity ie) || !ie.getItem().is(ItemTags.COALS))
            return;
        ie.setItem(new ItemStack(OtherverseItems.REALM_WRACKED_COAL.get(), ie.getItem().getCount()));
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        var be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof BiomeBrazierBlockEntity brazier) {
            var spiritsFor = SpiritLabeler.getSpiritsFor(event.getItemStack().getItem());
            if (event.getEntity().isShiftKeyDown() || spiritsFor == null || !spiritsFor.containsKey(Spirits.PHLOGISTON))
                return;
            brazier.fuel(spiritsFor.get(Spirits.PHLOGISTON));
            event.getItemStack().shrink(1);
            if (event.getItemStack().isEmpty()) event.getEntity().getInventory().removeItem(event.getItemStack());
            event.setUseItem(Event.Result.DENY);
            event.setUseBlock(Event.Result.DENY);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (!event.getItemStack().is(OtherverseItems.SPAWN_ALTAR.get())) return;
        if (!(be instanceof SpawnerBlockEntity spawner)) return;

        var etstring = spawner.saveWithoutMetadata().getCompound("SpawnData").getString("id");
        var et = (EntityType<? extends LivingEntity>) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(etstring));

        event.getLevel().destroyBlock(event.getPos(), false);

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setResult(Event.Result.ALLOW);

        setEntity(event.getItemStack(), et);
    }

    public static void setEntity(ItemStack altar, EntityType et) {
        int hp = (int) DefaultAttributes.getSupplier(et).getValue(Attributes.MAX_HEALTH);

        var hallowTag = new CompoundTag();
        hallowTag.putString("spirit_type", "war");
        hallowTag.putInt("spirit_amount", 0);
        hallowTag.putInt("capacity", hp * 3);
        altar.getOrCreateTag().put("hallow", hallowTag);

        HallowHelper.addFakeEnchantment(altar.getOrCreateTag());

        altar.getOrCreateTagElement("BlockEntityTag")
                .putString("spawn_altar_type", ForgeRegistries.ENTITY_TYPES.getKey(et).toString());
    }

    public static boolean trySpawn(ServerLevel level, BlockFocus focus, Diagram diagram) {
        var item = focus.getItem();
        if (!(level.getBlockEntity(focus.getPos()) instanceof SpawnAltarBlockEntity altar)) {
            return false;
        }
        if (!item.hasTag() || !item.getTag().contains("hallow")) {
            return false;
        }
        var hallowTag = item.getTag().getCompound("hallow");
        int hp = (int) DefaultAttributes.getSupplier(altar.spawnType).getValue(Attributes.MAX_HEALTH);
        var count = hallowTag.getInt("spirit_count");
        var mobCount = count / hp;
        if (mobCount == 0) {
            return false;
        }
        hallowTag.putInt("spirit_count", count - hp * mobCount);

        var spawnPos = focus.getPos();

        for (var i = 0; i < mobCount; i++) {
            var e = altar.spawnType.create(level, null, null, null,
                    spawnPos, MobSpawnType.SPAWN_EGG, false, false);
            e.setPos(new Vec3(
                    spawnPos.getX() + 0.4f + level.random.nextFloat() * 0.2f,
                    spawnPos.getY() + 0.25f,
                    spawnPos.getZ() + 0.4f + level.random.nextFloat() * 0.2f));
            level.addFreshEntityWithPassengers(e);
        }

        return true;
    }
}
