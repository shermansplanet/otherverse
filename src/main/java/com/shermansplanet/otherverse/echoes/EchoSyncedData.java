package com.shermansplanet.otherverse.echoes;

import javax.annotation.Nullable;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public class EchoSyncedData extends EchoData {

  private SynchedEntityData dataManager;

  public EchoSyncedData(SynchedEntityData dataManager) {
    super();
    register(dataManager);
  }

  public EchoSyncedData(SynchedEntityData dataManager, EntityType<?> innerEntity) {
    super(innerEntity);
    register(dataManager);
  }

  public void register(SynchedEntityData dataManager) {
    this.dataManager = dataManager;
    EntityType<?> innerEntity = super.getInnerEntityType();
    dataManager.define(EchoEntity.WATCHERID_INNER,
        innerEntity == null ? "" : ForgeRegistries.ENTITY_TYPES.getKey(innerEntity).toString());
    dataManager.define(EchoEntity.WATCHERID_PLAYERID, super.getPlayerId());
    dataManager.define(EchoEntity.WATCHERID_PLAYERNAME, super.getPlayerName());
  }

  @Nullable
  @Override
  public EntityType<?> getInnerEntityType() {
    String entityName = dataManager.get(EchoEntity.WATCHERID_INNER);
    return entityName.isEmpty() ? null
        : ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityName));
  }

  @Override
  public void setInnerEntityType(EntityType<?> innerEntityType) {
    dataManager.set(EchoEntity.WATCHERID_INNER, innerEntityType == null ? "" : ForgeRegistries.ENTITY_TYPES.getKey(innerEntityType).toString());
  }

  @Override
  public String getPlayerId() {
    return dataManager.get(EchoEntity.WATCHERID_PLAYERID);
  }

  @Override
  public void setPlayerId(String playerId) {
    this.dataManager.set(EchoEntity.WATCHERID_PLAYERID, playerId);
  }

  @Override
  public String getPlayerName() {
    return dataManager.get(EchoEntity.WATCHERID_PLAYERNAME);
  }

  @Override
  public void setPlayerName(String playerName) {
    this.dataManager.set(EchoEntity.WATCHERID_PLAYERNAME, playerName);
  }

}
