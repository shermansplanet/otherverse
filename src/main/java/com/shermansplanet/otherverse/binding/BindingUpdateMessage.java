package com.shermansplanet.otherverse.binding;

import com.shermansplanet.otherverse.diagrams.DiagramManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Mob;

public class BindingUpdateMessage {

    public enum BindingUpdateType {
        BIND, CONTRACT, FAMILIAR, BREAK
    }

    public int mobId;
    public BindingUpdateType updateType;
    public int levelValue;
    public CompoundTag data;
    public boolean silent;

    public BindingUpdateMessage(Mob mob, BindingUpdateType updateType, boolean silent) {
        this.mobId = mob.getId();
        this.updateType = updateType;
        this.levelValue = DiagramManager.getDimensionHash(mob.level());
        this.data = new CompoundTag();
        this.silent = silent;
    }

    public BindingUpdateMessage(Mob mob, BindingUpdateType updateType, CompoundTag data, boolean silent) {
        this.mobId = mob.getId();
        this.updateType = updateType;
        this.levelValue = DiagramManager.getDimensionHash(mob.level());
        this.data = data;
        this.silent = silent;
    }

    private BindingUpdateMessage(int mobId, BindingUpdateType updateType, int levelValue, CompoundTag data, boolean silent) {
        this.mobId = mobId;
        this.updateType = updateType;
        this.levelValue = levelValue;
        this.data = data;
        this.silent = silent;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(mobId);
        buffer.writeByte(updateType.ordinal());
        buffer.writeInt(levelValue);
        buffer.writeNbt(data);
        buffer.writeBoolean(silent);
    }

    public static BindingUpdateMessage decode(FriendlyByteBuf buffer) {
        int id = buffer.readInt();
        BindingUpdateType b = BindingUpdateType.values()[buffer.readByte()];
        int lvl = buffer.readInt();
        CompoundTag dta = buffer.readNbt();
        boolean slnt = buffer.readBoolean();
        return new BindingUpdateMessage(id, b, lvl, dta, slnt);
    }
}
