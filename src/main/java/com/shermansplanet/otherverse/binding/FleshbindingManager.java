package com.shermansplanet.otherverse.binding;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.SightManager;
import com.shermansplanet.otherverse.diagrams.BlockFocus;
import com.shermansplanet.otherverse.diagrams.ChalkCircle;
import com.shermansplanet.otherverse.diagrams.Diagram;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FleshbindingManager {

    public static final int FLESHBINDING_HP = 10;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final HashMap<String, ResourceLocation> texturesByLabel = new HashMap<>();

    static {
        texturesByLabel.put("minecraft:lapis_block", ResourceLocation.parse("textures/block/lapis_block.png"));
        texturesByLabel.put("minecraft:diamond", ResourceLocation.parse("textures/block/diamond_block.png"));
        texturesByLabel.put("otherverse:cinnabar_block", ResourceLocation.fromNamespaceAndPath("otherverse", "textures/block/cinnabar_block.png"));
    }

//    @SubscribeEvent
//    public static void onEntityHurt(LivingDamageEvent event) {
//        if (!SightManager.shouldRenderSight()) return;
//        var entity = event.getEntity();
//        if (entity.getType() == EntityType.PLAYER) return;
//        var level = entity.level();
//        if (!(level instanceof ServerLevel sl)) return;
//        var hp = entity.getHealth();
//        if (hp > FLESHBINDING_HP && hp - event.getAmount() <= FLESHBINDING_HP) {
//            for (var i = 0; i < 6; i++) {
//                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, entity.getRandomX(0.5D), entity.getRandomY(), entity.getRandomZ(0.5D), 1, 0, 0, 0, 0.5D);
//            }
//            sl.playSound(null, entity.getX(), entity.getY(0.5D), entity.getZ(), SoundEvents.GLOW_SQUID_SQUIRT, SoundSource.HOSTILE, 0.7f, 0.8f);
//        }
//    }

    private static boolean canItemFleshbindEntity(Item item, EntityType<?> entityType) {
        var entities = LootHelper.entitiesThatDropItem.get(item);
        if (entities != null && entities.contains(entityType)) {
            return true;
        }
        var entitiesThatTransfuseItem = MobTransfusions.transfusionsByResult.get(item);
        return entitiesThatTransfuseItem != null && entitiesThatTransfuseItem.contains(entityType);
    }

    public static boolean tryFleshbindMob(Mob mob, BlockFocus focus, ServerLevel level) {
        if (mob.getHealth() > 10) {
            return false;
        }
        ChalkCircle circleWithDrop = null;
        ChalkCircle circleWithPrime = null;
        ChalkCircle circleWithMaterial = null;
        String material = "";
        Diagram diagram = focus.getDiagram();
        Item prime = Otherverse.primeForDimension(level);
        for (var influence : diagram.influences.entrySet()) {
            if (!influence.getValue().equals(focus.getPos())
                    || !(level.getBlockEntity(influence.getKey()) instanceof ChalkCircle cc)
                    || cc.getItem().isEmpty()) continue;
            if (canItemFleshbindEntity(cc.item.getItem(), mob.getType())) {
                circleWithDrop = cc;
            }
            if (cc.item.is(prime)) {
                circleWithPrime = cc;
            }
            var path = ForgeRegistries.ITEMS.getKey(cc.item.getItem()).toString();
            if (texturesByLabel.containsKey(path) || cc.item.is(ItemTags.PLANKS) || cc.item.is(ItemTags.LOGS)) {
                material = path;
                circleWithMaterial = cc;
            }
        }
        if (circleWithPrime == null || circleWithDrop == null || circleWithMaterial == null ||
                !diagram.trySpendPower(level, focus.getPos(), 1, new HashSet<>())) return false;

        circleWithPrime.drainItem();
        circleWithDrop.drainItem();
        circleWithMaterial.drainItem();

        fleshbindMob(mob, material, level);
        Otherverse.ADVANCEMENTS.trigger(diagram.getOwner(level), "fleshbind");
        return true;
    }

    public static void fleshbindMob(Mob mob, String material, ServerLevel sl) {
        RandomSource r = sl.random;
        for (var i = 0; i < 16; i++) {
            sl.sendParticles(ParticleTypes.POOF,
                    mob.getX(r.nextFloat() - 0.5f),
                    mob.getY(r.nextFloat()),
                    mob.getZ(r.nextFloat() - 0.5f),
                    1, 0, 0, 0, 0.15);
        }
        sl.playSound(null, mob.blockPosition(), SoundEvents.PUFFER_FISH_DEATH, SoundSource.AMBIENT, 1, 1);
        mob.remove(Entity.RemovalReason.KILLED);
        mob.discard();
        var item = IdolItem.makeFrom(mob, material);
        ItemEntity itementity = new ItemEntity(mob.level(), mob.getX(), mob.getY(0.5f), mob.getZ(), item);
        itementity.setDefaultPickUpDelay();
        mob.level().addFreshEntity(itementity);
    }

    public static boolean tryCinnabind(ServerLevel level, ChalkCircle circle, Diagram diagram) {
        if (!circle.item.is(OtherverseItems.CINNABAR_BLOCK.get())) return false;
        var targetPos = diagram.influences.get(circle.getPos());
        if (targetPos == null) return false;
        var targetBinding = DiagramManager.getOrCreateLevelData(level).bindingsByPosition.get(targetPos);
        if (targetBinding == null) return false;
        if (targetBinding.mob == null || targetBinding.mob.isRemoved()) return false;
        fleshbindMob(targetBinding.mob, "otherverse:cinnabar_block", level);
        circle.drainItem();
        return true;
    }

    public static void addWoodTextures() {
        for (var item : ForgeRegistries.ITEMS) {
            var instance = item.getDefaultInstance();
            if (!instance.is(ItemTags.PLANKS) && !instance.is(ItemTags.LOGS)) continue;
            var key = ForgeRegistries.ITEMS.getKey(item);
            var val = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/block/" + key.getPath() + ".png");
            texturesByLabel.put(key.toString(), val);
        }
    }
}
