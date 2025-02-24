package com.shermansplanet.otherverse.spirits;

import com.shermansplanet.otherverse.Otherverse;
import com.shermansplanet.otherverse.familiar.FamiliarManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SpiritItem extends Item {
    public final SpiritType spiritType;
    public SpiritItem(SpiritType st, Properties p_41383_) {
        super(p_41383_);
        spiritType = st;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var item = player.getItemInHand(hand);
        if(FamiliarManager.trySonicBoom(player, item)){
            return InteractionResultHolder.pass(item);
        }
        if(!player.isCreative()){
            return InteractionResultHolder.fail(item);
        }
        var tag = item.getOrCreateTag();
        HallowHelper.addFakeEnchantment(tag);
        CompoundTag hallowTag = new CompoundTag();
        hallowTag.putInt("capacity", 999);
        hallowTag.putInt("spirit_count", 999);
        hallowTag.putString("spirit_type", spiritType.label());
        tag.put("hallow", hallowTag);
        Otherverse.primeForDimension(level);
        return InteractionResultHolder.pass(item);
    }
}
