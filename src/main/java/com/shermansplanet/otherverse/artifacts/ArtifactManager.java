package com.shermansplanet.otherverse.artifacts;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import com.shermansplanet.otherverse.diagrams.*;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import com.shermansplanet.otherverse.spirits.SpiritType;
import com.shermansplanet.otherverse.spirits.Spirits;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArtifactManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void coalTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(OtherverseItems.REALM_WRACKED_COAL.get())) return;
        if (!event.getItemStack().hasTag() || !event.getItemStack().getTag().contains("wracked_biome")) return;
        var biomeTag = event.getItemStack().getTag().getCompound("wracked_biome");
        var location = biomeTag.getString("location");
        var col = 0xed0d47;
        event.getToolTip().add(
                Component.translatable("biome." + location.replace(":", "."))
                        .withStyle(Style.EMPTY.withBold(true).withColor(col)));
        var spiritTag = biomeTag.getCompound("spirits");
        for (var spirit : spiritTag.getAllKeys()) {
            event.getToolTip().add(
                    Component.literal(spiritTag.getInt(spirit) + " " + spirit)
                            .withStyle(Style.EMPTY.withColor(col)));
        }
    }

    @SubscribeEvent
    public static void setBiome(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !event.getItemStack().is(OtherverseItems.REALM_WRACKED_COAL.get()))
            return;
        var biome = sl.getBiome(event.getEntity().blockPosition());
        var spiritTypes = MobBindingInfluenceUtils.getSpiritTypes(biome, sl);
        if (spiritTypes.isEmpty()) return;
        spiritTypes.addAll(new ArrayList<>(spiritTypes));
        spiritTypes.addAll(getColorsFor(biome));
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

    private static Collection<SpiritType> getColorsFor(Holder<Biome> biome) {
        if(biome.is(Biomes.WARPED_FOREST)) return List.of(Spirits.COLOR_CYAN, Spirits.COLOR_CYAN, Spirits.COLOR_RED);
        if(biome.is(Biomes.CRIMSON_FOREST)) return List.of(Spirits.COLOR_RED, Spirits.COLOR_RED, Spirits.COLOR_ORANGE);
        if(biome.is(Biomes.SOUL_SAND_VALLEY)) return List.of(Spirits.COLOR_BROWN, Spirits.COLOR_CYAN, Spirits.COLOR_WHITE);
        if(biome.is(Biomes.DEEP_DARK)) return List.of(Spirits.COLOR_BLACK, Spirits.COLOR_BLACK, Spirits.COLOR_CYAN);
        if(biome.is(Biomes.LUSH_CAVES)) return List.of(Spirits.COLOR_LIME, Spirits.COLOR_PINK, Spirits.COLOR_GRAY);
        if(biome.is(Biomes.DRIPSTONE_CAVES)) return List.of(Spirits.COLOR_BROWN, Spirits.COLOR_GRAY, Spirits.COLOR_LIGHT_GRAY);

        if(biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return List.of(Spirits.COLOR_BLUE, Spirits.COLOR_LIGHT_BLUE, Spirits.COLOR_CYAN);
        if (biome.is(Tags.Biomes.IS_SNOWY)) return List.of(Spirits.COLOR_WHITE, Spirits.COLOR_BROWN, Spirits.COLOR_GRAY);
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH)) return List.of(Spirits.COLOR_BROWN, Spirits.COLOR_YELLOW, Spirits.COLOR_GRAY);
        if (biome.is(Tags.Biomes.IS_CAVE)) return List.of(Spirits.COLOR_BLACK, Spirits.COLOR_LIGHT_GRAY, Spirits.COLOR_GRAY);

        if (biome.is(BiomeTags.IS_OVERWORLD)) return List.of(Spirits.COLOR_GREEN, Spirits.COLOR_BROWN, Spirits.COLOR_GRAY);
        if (biome.is(BiomeTags.IS_NETHER)) return List.of(Spirits.COLOR_RED, Spirits.COLOR_ORANGE, Spirits.COLOR_BLACK);
        if (biome.is(BiomeTags.IS_END)) return List.of(Spirits.COLOR_BLACK, Spirits.COLOR_BLACK, Spirits.COLOR_PURPLE);
        return new ArrayList<>();
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
            if (!event.getItemStack().is(OtherverseItems.REALM_WRACKED_COAL.get())) return;
            if (!event.getItemStack().hasTag() || !event.getItemStack().getTag().contains("wracked_biome")) return;
            if (!event.getEntity().isCreative()) {
                event.getItemStack().shrink(1);
                if (event.getItemStack().isEmpty()) event.getEntity().getInventory().removeItem(event.getItemStack());
            }
            event.setUseItem(Event.Result.DENY);
            event.setUseBlock(Event.Result.DENY);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            brazier.fuel(event.getItemStack().getTag().getCompound("wracked_biome"));
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

    public static boolean tryFillBrazier(ServerLevel level, BlockFocus focus, Diagram diagram) {
        var targetPos = focus.getPos();
        if (!(level.getBlockEntity(targetPos) instanceof BiomeBrazierBlockEntity brazier)) return false;
        if (brazier.spiritCounts == null) return false;
        var foci = new ArrayList<IFocus>();
        for (var sourcePos : diagram.allFocusPositions) {
            if (!targetPos.equals(diagram.influences.get(sourcePos))) continue;
            if (level.getBlockEntity(sourcePos) instanceof ChalkCircle cc) {
                foci.add(cc);
                continue;
            }
            var bf = DiagramManager.getOrCreateLevelData(level).allBlockFoci.get(sourcePos);
            if (bf != null) foci.add(bf);
        }
        var anyTransfer = false;
        for (var sourceFocus : foci) {
            for (var spiritType : brazier.spiritCounts.keySet()) {
                var count = brazier.spiritCounts.get(spiritType);
                var remaining = count.getSecond() - count.getFirst();
                if (remaining == 0) continue;
                var transferred = sourceFocus.drainHallow(spiritType, remaining, false, false);
                if (transferred == 0) continue;
                brazier.spiritCounts.put(spiritType, new Pair<>(count.getFirst() + transferred, count.getSecond()));
                anyTransfer = true;
            }
        }
        if (anyTransfer) {
            LOGGER.debug("ANY TRANSFER");
            var canActivate = true;
            for (var spiritType : brazier.spiritCounts.values()) {
                if (spiritType.getFirst() >= spiritType.getSecond()) continue;
                canActivate = false;
                break;
            }
            var pos = brazier.getBlockPos();
            if (canActivate) {
                brazier.activate(level);
            } else {
                level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1, 1);
            }
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1, 1);
            var r = level.getRandom();
            for (var i = 0; i < 8; i++) {
                level.sendParticles(canActivate ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.LARGE_SMOKE,
                        pos.getX() + r.nextFloat() * 3f - 1f,
                        pos.getY() + r.nextFloat() * 3f - 1f,
                        pos.getZ() + r.nextFloat() * 3f - 1f,
                        0, 0, 0.2f, 0, 0.15f);
            }
            brazier.setLabels();
            brazier.setChanged();
        }
        return anyTransfer;
    }
}
