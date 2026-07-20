package com.shermansplanet.otherverse.binding;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.function.Consumer;

public class IdolItem extends Item {

    public IdolItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext useCtx) {
        if (!(useCtx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        ItemStack stack = useCtx.getItemInHand();
        var type = getType(stack);
        var location = useCtx.getClickLocation();
        if (!stack.hasTag() || !stack.getTag().contains("material")) {
            var entity = type.create(sp.level());
            entity.setPos(location);
            sp.level().addFreshEntity(entity);
        } else {
            FamiliarManager.makeMobFromTag(type, stack.getTag(), location, sp.serverLevel());
            stack.shrink(1);
            if (stack.getTag().getString("material").equals("otherverse:cinnabar_block")) {
                var player = useCtx.getPlayer();
                var item = new ItemStack(OtherverseItems.CINNABAR_BLOCK.get(), 1);
                if (!player.addItem(item)) {
                    player.drop(item, false);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    public static EntityType<?> getType(ItemStack stack) {
        var type = MobBindingInfluenceUtils.typesByIdol.get(stack);
        if (type == null) {
            var tag = stack.getTag();
            if (tag == null) {
                return null;
            }
            return FamiliarManager.getEntityTypeFromTag(tag);
        } else {
            return type;
        }
    }

    public static CompoundTag mobToTag(Mob mob) {
        var data = new CompoundTag();
        data.put("EntityTag", mob.saveWithoutId(new CompoundTag()));
        var tag = new CompoundTag();
        tag.put("mob_data", data);
        var loc = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        tag.putString("entity_type_namespace", loc.getNamespace());
        tag.putString("entity_type_path", loc.getPath());
        var heldItem = BindingManager.getHeldItem(mob);
        if (!heldItem.isEmpty()) {
            var nbt = new CompoundTag();
            heldItem.save(nbt);
            tag.put("held_item", nbt);
        }
        return tag;
    }

    public static ItemStack makeFrom(Mob mob, String material) {
        var tag = mobToTag(mob);
        tag.putString("material", material);
        if(material.equals("otherverse:cinnabar_block")){
            LogUtils.getLogger().debug("MAKING MOB FROM CINNABAR");
            CompoundTag persistentData = mob.getPersistentData();
            if (persistentData.contains("bindingId")) {
                LogUtils.getLogger().debug("HAS BINDING");
                TransientDiagramData data = DiagramManager.getOrCreateLevelData(mob.level().getServer().overworld());
                BindingInfo binding = data.bindingsById.get(persistentData.getUUID("bindingId"));
                if (binding != null) {
                    LogUtils.getLogger().debug("MARKING BINDING AS CINNABAR");
                    binding.isCinnabar = true;
                    binding.mob = null;
                }
            }
        }
        var stack = new ItemStack(OtherverseItems.IDOL.get(), 1);
        stack.setTag(tag);
        return stack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return IdolRenderer.instance;
            }
        });
    }
}
