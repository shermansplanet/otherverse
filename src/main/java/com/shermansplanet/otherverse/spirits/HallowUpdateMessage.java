package com.shermansplanet.otherverse.spirits;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class HallowUpdateMessage {

  public BlockPos position;
  public CompoundTag tag;
  public int levelValue;

  public HallowUpdateMessage(BlockPos position, CompoundTag tag, int levelValue) {
    this.position = position;
    this.tag = tag;
    this.levelValue = levelValue;
  }

  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(position);
    buffer.writeBoolean(tag == null);
    buffer.writeInt(levelValue);
    if (tag != null) {
      SpiritType spiritType = Spirits.spiritsByLabel.get(tag.getString("spirit_type"));
      buffer.writeInt(spiritType.id());
      buffer.writeInt(tag.getInt("spirit_count"));
      buffer.writeInt(tag.getInt("capacity"));
      buffer.writeBoolean(tag.contains("shrine"));
    }
  }

  public static HallowUpdateMessage decode(FriendlyByteBuf buffer) {
    BlockPos pos = buffer.readBlockPos();
    boolean remove = buffer.readBoolean();
    int lvl = buffer.readInt();
    if (remove) {
      return new HallowUpdateMessage(pos, null, lvl);
    }
    int id = buffer.readInt();
    CompoundTag tag = new CompoundTag();
    tag.putString("spirit_type", Spirits.spiritsById.get(id).label());
    tag.putInt("spirit_count", buffer.readInt());
    tag.putInt("capacity", buffer.readInt());
    if(buffer.readBoolean()) tag.putBoolean("shrine", true);
    return new HallowUpdateMessage(pos, tag, lvl);
  }
}
