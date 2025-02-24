package com.shermansplanet.otherverse.spawning;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.Diagram;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import com.shermansplanet.otherverse.spirits.HallowHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpawnAltarManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(OtherverseItems.SPAWN_ALTAR.get())) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof SpawnerBlockEntity spawner)) return;

        var etstring = spawner.saveWithoutMetadata().getCompound("SpawnData").getString("id");
        var et = (EntityType<? extends LivingEntity>) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(etstring));

        event.getLevel().destroyBlock(event.getPos(), false);

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setResult(Event.Result.ALLOW);

        setEntity(event.getItemStack(), et);
    }

    public static void setEntity(ItemStack altar, EntityType et){
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
