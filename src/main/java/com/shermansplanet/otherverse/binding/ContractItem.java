package com.shermansplanet.otherverse.binding;

import com.shermansplanet.otherverse.demesnes.DemesnesManager;
import com.shermansplanet.otherverse.diagrams.DiagramManager;
import com.shermansplanet.otherverse.diagrams.TransientDiagramData;
import com.shermansplanet.otherverse.registries.OtherverseItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ContractItem extends Item {

    public ContractItem(Properties p_41383_) {
        super(p_41383_);
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!stack.is(OtherverseItems.CONTRACT.get())) return InteractionResult.PASS;
        if (!(target instanceof Mob mob)) return InteractionResult.PASS;
        CompoundTag mobData = mob.getPersistentData();
        if (!mobData.contains("bindingId")) {
            if (tryTameContract(mob, stack, player)) return InteractionResult.SUCCESS;
            return InteractionResult.PASS;
        }
        TransientDiagramData data = DiagramManager.getOrCreateLevelData(player.getLevel().getServer().overworld());
        BindingInfo binding = data.bindingsById.get(mobData.getUUID("bindingId"));
        if (binding == null) {
            return InteractionResult.PASS;
        }
        ContractManager.applyContract(stack.getTag().getCompound("contract"), mob, true);
        return InteractionResult.SUCCESS;
    }

    private boolean tryTameContract(Mob mob, ItemStack stack, Player player) {
        if (!(mob.getLevel() instanceof ServerLevel sl)) return false;
        if ((!(mob instanceof TamableAnimal ta) || !ta.isOwnedBy(player))
                && (!(mob instanceof AbstractHorse horse) || horse.getOwnerUUID() != player.getUUID())) return false;
        var demesne = DemesnesManager.getData(sl, mob.blockPosition());
        if (demesne == null) return false;
        if (demesne.getPerkLevel(DemesnesManager.DemesnePerk.COLLAR) == 0) return false;
        mob.getPersistentData().put("unbound_contract", stack.getTag().getCompound("contract"));
        BindingManager.applyUnboundContract(mob, true);
        return true;
    }
}
