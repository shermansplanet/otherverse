package com.shermansplanet.otherverse.echoes;

import com.mojang.logging.LogUtils;
import com.shermansplanet.otherverse.Otherverse;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Otherverse.MODID, bus = Bus.FORGE)
public class EchoEntity extends Entity {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final EntityDataAccessor<String> WATCHERID_INNER = SynchedEntityData
      .defineId(EchoEntity.class, EntityDataSerializers.STRING);
  public static final EntityDataAccessor<String> WATCHERID_PLAYERID = SynchedEntityData
      .defineId(EchoEntity.class, EntityDataSerializers.STRING);
  public static final EntityDataAccessor<String> WATCHERID_PLAYERNAME = SynchedEntityData
      .defineId(EchoEntity.class, EntityDataSerializers.STRING);


  private EchoSyncedData data;
  private Mob innerEntity = null;
  private EntityType<?> preferredInnerEntity;
  private String playerId;
  private String playerName;

  public Vec3 glitchOffset = Vec3.ZERO;
  private int glitchCountdown;

  public EchoEntity(EntityType<? extends EchoEntity> type, Level level) {
    this(type, level, null);
  }

  public EchoEntity(Level level) {
    this(Otherverse.ENTITY_ECHO.get(), level, null);
  }

  public EchoEntity(EntityType<? extends EchoEntity> type, Level level,
      @Nullable EntityType<?> preferredInnerEntity) {
    super(type, level);
    this.preferredInnerEntity = preferredInnerEntity;
  }

  /*@SubscribeEvent(priority = EventPriority.NORMAL)
  public static void onDeath(LivingDeathEvent event) {
    if (event.getEntity() == null || event.getEntity().getType() == Otherverse.ENTITY_ECHO.get()) {
      return;
    }
    Level level = event.getEntity().level;
    if (level.isClientSide()) {
      return;
    }
    LOGGER.debug("ECHO CREATED");
    EchoEntity echo = new EchoEntity(level);
    echo.setInnerEntity(event.getEntity());
    echo.copyPosition(event.getEntity());
    level.addFreshEntity(echo);
  }*/

  public void setInnerEntity(LivingEntity innerEntity) {
    if (innerEntity instanceof Player) {
      setPlayerId(((Player) innerEntity).getGameProfile().getId().toString());
      setPlayerName(((Player) innerEntity).getGameProfile().getName());
      this.data.setInnerEntityType(EntityType.ZOMBIE);
    } else {
      this.data.setInnerEntityType(innerEntity.getType());
    }
  }

  public Mob getInnerEntity() {
    EntityType<?> entityType = data.getInnerEntityType();
    if (innerEntity != null) {
      if (entityType != null && entityType == innerEntity.getType()) {
        return innerEntity;
      }
    }
    try {
      innerEntity = (Mob) (entityType.create(level()));
      innerEntity.setPose(Pose.DYING);
      innerEntity.copyPosition(this);
      return innerEntity;
    } catch (NullPointerException | ClassCastException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public EntityDimensions getDimensions(Pose poseIn) {
    LivingEntity innerEntity = getInnerEntity();
    if (innerEntity != null) {
      return innerEntity.getDimensions(poseIn);
    }
    return super.getDimensions(poseIn);
  }

  @Override
  public void defineSynchedData() {
    data = new EchoSyncedData(this.entityData, preferredInnerEntity);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    data.writeNBT(tag);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    data.readNBT(tag);
  }

  @Override
  public void tick() {
    super.tick();

    getInnerEntity().setYRot(random.nextInt(360));

    glitchCountdown--;
    if (glitchCountdown == 0) {
      glitchOffset = new Vec3(random.nextFloat() - 0.5f, 0,
          random.nextFloat() - 0.5f);
    } else if (glitchCountdown == -1) {
      glitchOffset = Vec3.ZERO;
      glitchCountdown = random.nextInt(60);

      Player p = level().getNearestPlayer(this, 10f);
      if (p != null) {
        setYRot(
            (float) (Math.atan2(p.position().x - position().x, p.position().z - position().z) * 180f
                / Math.PI));
      }
    }
  }

  public boolean isPlayer() {
    return containsPlayer();
  }

  private boolean containsPlayer() {
    return getPlayerId() != null && !getPlayerId().isEmpty();
  }

  public String getPlayerId() {
    return playerId;
  }

  public void setPlayerId(String s) {
    playerId = s;
  }

  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String s) {
    playerName = s;
  }

  public UUID getPlayerUUID() {
    try {
      return UUID.fromString(getPlayerId());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  public boolean canBeCollidedWith() {
    return false;
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    return null;
  }

  @Override
  public boolean isPickable() { // Drop through this entity when checking for attack targets.
    return false;
  }
}
