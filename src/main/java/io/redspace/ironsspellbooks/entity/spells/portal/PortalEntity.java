package io.redspace.ironsspellbooks.entity.spells.portal;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.PortalManager;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

/**
 * /kill @e[type=irons_spellbooks:portal]
 */

public class PortalEntity extends Entity implements AntiMagicSusceptible {
    static {
        DATA_ID_OWNER_UUID = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
        DATA_PORTAL_CONNECTED = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.BOOLEAN);
    }

    private static final EntityDataAccessor<Optional<UUID>> DATA_ID_OWNER_UUID;
    private static final EntityDataAccessor<Boolean> DATA_PORTAL_CONNECTED;

    private long ticksToLive = 0;
    private boolean isPortalConnected = false;

    public PortalEntity(Level level, PortalData portalData) {
        this(EntityRegistry.PORTAL.get(), level);
        PortalManager.INSTANCE.addPortalData(uuid, portalData);
        this.ticksToLive = portalData.ticksToLive;
    }

    public PortalEntity(EntityType<PortalEntity> portalEntityEntityType, Level level) {
        super(portalEntityEntityType, level);
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        if (!level.isClientSide) {
            discard();
        }
    }

    @Override
    public void onRemovedFromWorld() {
        if (level.isClientSide) return;

        var removalReason = getRemovalReason();
        if (removalReason != null && removalReason.shouldDestroy()) {
            PortalManager.INSTANCE.killPortal(uuid, getOwnerUUID());
        }

        MagicManager.spawnParticles(level, new DustParticleOptions(new Vector3f(.5f, .05f, .6f), 1.5f), getX(), getY(), getZ(), 25, .4, .8, .4, .03, false);

        super.onRemovedFromWorld();
    }

    public void checkForEntitiesToTeleport() {
        if (this.level.isClientSide) return;
        level.getEntities((Entity) null, this.getBoundingBox(), (entity -> !entity.getType().is(ModTags.CANT_USE_PORTAL) && (entity.isPickable() || entity instanceof Projectile) && !entity.isVehicle() && !entity.isSpectator())).forEach(entity -> {
            //IronsSpellbooks.LOGGER.debug("PortalEntity: entity near portal:{}, portal:{}", entity, uuid);

            PortalManager.INSTANCE.processDelayCooldown(uuid, entity.getUUID(), 1);

            if (PortalManager.INSTANCE.canUsePortal(this, entity)) {
                //IronsSpellbooks.LOGGER.debug("PortalEntity: teleport entity:{} portal:{}", entity, uuid);

                PortalManager.INSTANCE.addPortalCooldown(entity, uuid);

                var portalData = PortalManager.INSTANCE.getPortalData(this);
                portalData.getConnectedPortalPos(uuid).ifPresent(portalPos -> {
                    Vec3 destination = portalPos.pos().add(0, entity.getY() - this.getY(), 0);
                    entity.setYRot(portalPos.rotation());
                    this.level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1f, 1f);
                    if (level.dimension().equals(portalPos.dimension())) {
                        entity.teleportTo(destination.x, destination.y + .1, destination.z);
                        var delta = entity.getDeltaMovement();
                        float hspeed = (float) Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                        float f = portalPos.rotation() * Mth.DEG_TO_RAD;
                        entity.setDeltaMovement(-Mth.sin(f) * hspeed, delta.y, Mth.cos(f) * hspeed);
                    } else {
                        //IronsSpellbooks.LOGGER.debug("PortalEntity: teleport entity:{} to dimension: {}", entity, portalPos.dimension());
                        var server = level.getServer();
                        if (server != null) {
                            var dim = server.getLevel(portalPos.dimension());
                            if (dim != null) {
                                entity.changeDimension(dim, new PortalTeleporter(destination));
                            }
                        }
                    }
                    this.level.playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1f, 1f);
                });
            }
        });
    }

    private Vec3 getDestinationPosition(PortalPos globalPos, Entity entity) {
        Vec3 offset = new Vec3(this.getX() - entity.getX(), this.getY() - entity.getY(), this.getZ() - entity.getZ());
        return new Vec3(globalPos.pos().x - offset.x, globalPos.pos().y - offset.y, globalPos.pos().z - offset.z);
    }

    public void setTicksToLive(int ticksToLive) {
        this.ticksToLive = ticksToLive;
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
//            spawnParticles(.5f, new Vector3f(.8f, .15f, .65f));
//            if (isPortalConnected) {
//                spawnParticles(.36f, new Vector3f(.5f, .05f, .6f));
//                spawnParticles(.2f, new Vector3f(1f, .2f, .7f));
//            }
            Vec3 center = this.getBoundingBox().getCenter();
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleHelper.PORTAL_FRAME, center.x, center.y, center.z, 1f, 2.1f, this.getYRot());
            }
            return;
        }

        PortalManager.INSTANCE.processCooldownTick(uuid, -1);
        checkForEntitiesToTeleport();

        if (--ticksToLive <= 0) {
            discard();
        }
    }

    public void spawnParticles(float radius, Vector3f color) {
        int particles = 40;
        float step = 6.28f / particles;
        Vec3 center = this.getBoundingBox().getCenter();
        for (int i = 0; i < particles; i++) {
            float x = Mth.cos(i * step) * radius;
            float y = Mth.sin(i * step) * radius * 2;
            Vec3 offset = new Vec3(x, y, 0).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
            level.addParticle(ParticleHelper.UNSTABLE_ENDER, true, center.x + offset.x, center.y + offset.y, center.z + offset.z, 0d, 0d, 0);
        }
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_ID_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerUUID() {
        return this.entityData
                .get(DATA_ID_OWNER_UUID)
                .orElseGet(() -> this.entityData.get(DATA_ID_OWNER_UUID).orElse(null));
    }

    public void setPortalConnected() {
        this.entityData.set(DATA_PORTAL_CONNECTED, true);
    }

    public boolean getPortalConnected() {
        return this.entityData.get(DATA_PORTAL_CONNECTED);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ID_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_PORTAL_CONNECTED, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);

        if (!level.isClientSide) {
            return;
        }

        if (pKey.getId() == DATA_PORTAL_CONNECTED.getId()) {
            isPortalConnected = getPortalConnected();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains("ownerUUID")) {
            setOwnerUUID(compoundTag.getUUID("ownerUUID"));
        }

        if (compoundTag.contains("ticksToLive")) {
            ticksToLive = compoundTag.getLong("ticksToLive");
        }

        var portalData = PortalManager.INSTANCE.getPortalData(this);

        if (portalData == null) {
            ticksToLive = 0;
        } else {
            if (portalData.portalEntityId1 != null && portalData.portalEntityId2 != null) {
                setPortalConnected();
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putLong("ticksToLive", ticksToLive);
        compoundTag.putUUID("ownerUUID", getOwnerUUID());
    }
}


