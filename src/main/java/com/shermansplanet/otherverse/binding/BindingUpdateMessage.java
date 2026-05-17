package com.shermansplanet.otherverse.binding;

import com.shermansplanet.otherverse.diagrams.DiagramManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Mob;

public class BindingUpdateMessage {

    public enum BindingUpdateType {
        BIND, CONTRACT, FAMILIAR, BREAK
    }

    public enum BindingType {
        POSITIVE, NEGATIVE, FLESH, TECH, FAMILIAR, DEMESNE, UNBOUND
    }

    public int mobId;
    public BindingUpdateType updateType;
    public int levelValue;
    public CompoundTag data;
    public boolean silent;
    public BindingType type;

    public BindingUpdateMessage(Mob mob, BindingUpdateType updateType, BindingType bindingType, boolean silent) {
        this.mobId = mob.getId();
        this.updateType = updateType;
        this.levelValue = DiagramManager.getDimensionHash(mob.level());
        this.data = new CompoundTag();
        this.silent = silent;
        this.type = bindingType;
    }

    public BindingUpdateMessage(Mob mob, BindingUpdateType updateType, CompoundTag data, BindingType bindingType, boolean silent) {
        this.mobId = mob.getId();
        this.updateType = updateType;
        this.levelValue = DiagramManager.getDimensionHash(mob.level());
        this.data = data;
        this.silent = silent;
        this.type = bindingType;
    }

    private BindingUpdateMessage(int mobId, BindingUpdateType updateType, int levelValue, CompoundTag data, BindingType bindingType, boolean silent) {
        this.mobId = mobId;
        this.updateType = updateType;
        this.levelValue = levelValue;
        this.data = data;
        this.silent = silent;
        this.type = bindingType;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(mobId);
        buffer.writeByte(updateType.ordinal());
        buffer.writeInt(levelValue);
        buffer.writeNbt(data);
        buffer.writeBoolean(silent);
        buffer.writeByte(type.ordinal());
    }

    public static BindingUpdateMessage decode(FriendlyByteBuf buffer) {
        int id = buffer.readInt();
        BindingUpdateType b = BindingUpdateType.values()[buffer.readByte()];
        int lvl = buffer.readInt();
        CompoundTag dta = buffer.readNbt();
        boolean slnt = buffer.readBoolean();
        BindingType bt = BindingType.values()[buffer.readByte()];
        return new BindingUpdateMessage(id, b, lvl, dta, bt, slnt);
    }
}
