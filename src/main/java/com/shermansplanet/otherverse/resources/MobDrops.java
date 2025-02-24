package com.shermansplanet.otherverse.resources;

import com.shermansplanet.otherverse.Otherverse;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobDrops {

    public static HashSet<EntityType<?>> ungulates = new HashSet<>();

    static {
        ungulates.add(EntityType.COW);
        ungulates.add(EntityType.SHEEP);
        ungulates.add(EntityType.GOAT);
        ungulates.add(EntityType.HORSE);
        ungulates.add(EntityType.PIG);
        ungulates.add(EntityType.HOGLIN);
    }

    /*@SubscribeEvent
    public static void onKill(LivingDropsEvent e) {
        var entity = e.getEntity();
        if (entity.level.isClientSide) return;
        if (!ungulates.contains(entity.getType())) return;
        if (entity.level.getRandom().nextFloat() > 0.2f) return;
        var pos = entity.position();
        e.getDrops().add(new ItemEntity(entity.level, pos.x, pos.y, pos.z, new ItemStack(OtherverseItems.BEAST_SKULL.get())));
    }*/
}
