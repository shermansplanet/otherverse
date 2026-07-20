package com.shermansplanet.otherverse;

import com.shermansplanet.otherverse.binding.MobBindingInfluenceUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemOrEntityType {
    public final Item item;
    public final EntityType<?> entityType;
    public final boolean isItem;

    public ItemOrEntityType(Item item) {
        this.item = item;
        this.entityType = null;
        isItem = true;
    }

    public ItemOrEntityType(EntityType<?> entityType) {
        this.entityType = entityType;
        this.item = null;
        isItem = false;
    }

    @Override
    public int hashCode() {
        return isItem ? this.item.hashCode() : this.entityType.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ItemOrEntityType other = (ItemOrEntityType) obj;
        if (other.isItem != isItem) {
            return false;
        }
        return other.isItem ? other.item.equals(item) : other.entityType.equals(entityType);
    }

    public ItemStack getItemStack() {
        return isItem ? item.getDefaultInstance() : MobBindingInfluenceUtils.getIdol(entityType);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isItem);
        if (isItem) {
            buffer.writeInt(Item.getId(item));
        } else {
            buffer.writeInt(BuiltInRegistries.ENTITY_TYPE.getId(entityType));
        }
    }

    public static ItemOrEntityType decode(FriendlyByteBuf buffer) {
        var isItem = buffer.readBoolean();
        if (isItem) {
            return new ItemOrEntityType(Item.byId(buffer.readInt()));
        } else {
            return new ItemOrEntityType(BuiltInRegistries.ENTITY_TYPE.byId(buffer.readInt()));
        }
    }
}
