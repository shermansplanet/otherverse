package com.shermansplanet.otherverse.echoes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public class EchoData {
  private static final String NBTKEY_INNER_ENTITY = "innerEntity";
  private static String NBTKEY_PLAYER_ID = "playerId";
  private static String NBTKEY_PLAYER_NAME = "playerName";

  private EntityType<?> innerEntityType;
  private String playerId = "";
  private String playerName = "";

  protected EntityType<?> getInnerEntityType() {
    return innerEntityType;
  }

  protected void setInnerEntityType(EntityType<?> ie) {
    innerEntityType = ie;
  }

  public EchoData() {
    this(null);
  }

  public EchoData(EntityType<?> innerEntityType) {
    this.innerEntityType = innerEntityType;
  }

  public boolean containsPlayer() {
    return getPlayerId() != null && !getPlayerId().isEmpty();
  }

  public boolean hasInnerEntity() {
    return getInnerEntityType() != null;
  }

  public void readNBT(CompoundTag tag) {
    setInnerEntityType(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(tag.getString(NBTKEY_INNER_ENTITY))));
    setPlayerId(tag.getString(NBTKEY_PLAYER_ID));
    setPlayerName(tag.getString(NBTKEY_PLAYER_NAME));
  }

  public void setPlayerName(String s) {
    playerName = s;
  }

  public void setPlayerId(String s) {
    playerId = s;
  }

  public CompoundTag writeNBT(CompoundTag tag) {
    if (getInnerEntityType() != null)
      tag.putString(NBTKEY_INNER_ENTITY, ForgeRegistries.ENTITY_TYPES.getKey(getInnerEntityType()).toString());
    tag.putString(NBTKEY_PLAYER_ID, getPlayerId());
    tag.putString(NBTKEY_PLAYER_NAME, getPlayerName());
    return tag;
  }

  public static EchoData fromNBT(CompoundTag tag) {
    EchoData data = new EchoData();
    data.readNBT(tag);
    return data;
  }

  protected String getPlayerId() {
    return playerId;
  }

  protected String getPlayerName() {
    return playerName;
  }
}
