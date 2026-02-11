package com.froxot.sleeptilnight;

import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSleep;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.builtin.beds.sleep.components.SleepTracker;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSleep;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSlumber;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSomnolence;
import com.hypixel.hytale.builtin.beds.sleep.systems.player.UpdateSleepPacketSystem;
import com.hypixel.hytale.builtin.beds.sleep.systems.world.CanSleepInWorld;
import com.hypixel.hytale.builtin.beds.sleep.systems.world.StartSlumberSystem;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.protocol.packets.world.SleepClock;
import com.hypixel.hytale.protocol.packets.world.SleepMultiplayer;
import com.hypixel.hytale.protocol.packets.world.UpdateSleepState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomUpdateSleepPacketSystem extends UpdateSleepPacketSystem {
    private static final UUID[] EMPTY_UUIDS = new UUID[0];
    private static final UpdateSleepState PACKET_NO_SLEEP_UI = new UpdateSleepState(false, false, (SleepClock)null, (SleepMultiplayer)null);

    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        UpdateSleepState packet = this.createSleepPacket(store, index, archetypeChunk);
        SleepTracker sleepTrackerComponent = (SleepTracker)archetypeChunk.getComponent(index, SleepTracker.getComponentType());

        assert sleepTrackerComponent != null;

        packet = sleepTrackerComponent.generatePacketToSend(packet);
        if (packet != null) {
            PlayerRef playerRefComponent = (PlayerRef)archetypeChunk.getComponent(index, PlayerRef.getComponentType());

            assert playerRefComponent != null;

            playerRefComponent.getPacketHandler().write(packet);
        }

    }

    private UpdateSleepState createSleepPacket(@Nonnull Store<EntityStore> store, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk) {
        World world = ((EntityStore)store.getExternalData()).getWorld();
        WorldSomnolence worldSomnolence = (WorldSomnolence)store.getResource(WorldSomnolence.getResourceType());
        WorldSleep worldSleepState = worldSomnolence.getState();
        PlayerSomnolence playerSomnolenceComponent = (PlayerSomnolence)archetypeChunk.getComponent(index, PlayerSomnolence.getComponentType());

        assert playerSomnolenceComponent != null;

        PlayerSleep playerSleepState = playerSomnolenceComponent.getSleepState();
        SleepClock var10000;
        if (worldSleepState instanceof WorldSlumber) {
            WorldSlumber slumber = (WorldSlumber)worldSleepState;
            var10000 = slumber.createSleepClock();
        } else {
            var10000 = null;
        }

        SleepClock clock = var10000;
        UpdateSleepState var21;
        switch (playerSleepState) {
            case PlayerSleep.FullyAwake ignored:
                var21 = PACKET_NO_SLEEP_UI;
                break;
            case PlayerSleep.MorningWakeUp ignored:
                var21 = PACKET_NO_SLEEP_UI;
                break;
            case PlayerSleep.NoddingOff noddingOff:
                    long elapsedMs = Duration.between(noddingOff.realTimeStart(), Instant.now()).toMillis();
                    boolean grayFade = elapsedMs > SPAN_BEFORE_BLACK_SCREEN.toMillis();
                    Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                    boolean readyToSleep = CustomStartSlumberSystem.isReadyToSleep(store, ref);
                    var21 = new UpdateSleepState(grayFade, false, clock, readyToSleep ? this.createSleepMultiplayer(store) : null);
                break;
            case PlayerSleep.Slumber ignored:
                var21 = new UpdateSleepState(true, true, clock, (SleepMultiplayer)null);
                break;
        }

        return var21;
    }

    @Nullable
    private SleepMultiplayer createSleepMultiplayer(@Nonnull Store<EntityStore> store) {
        World world = ((EntityStore)store.getExternalData()).getWorld();
        List<PlayerRef> playerRefs = new ArrayList(world.getPlayerRefs());
        playerRefs.removeIf((playerRefx) -> playerRefx.getReference() == null);
        if (playerRefs.size() <= 1) {
            return null;
        } else {
            playerRefs.sort(Comparator.comparingLong((refx) -> (long)(refx.getUuid().hashCode() + world.hashCode())));
            int sleepersCount = 0;
            int awakeCount = 0;
            List<UUID> awakeSampleList = new ArrayList(playerRefs.size());

            for(PlayerRef playerRef : playerRefs) {
                Ref<EntityStore> ref = playerRef.getReference();
                boolean readyToSleep = CustomStartSlumberSystem.isReadyToSleep(store, ref);
                if (readyToSleep) {
                    ++sleepersCount;
                } else {
                    ++awakeCount;
                    awakeSampleList.add(playerRef.getUuid());
                }
            }

            UUID[] awakeSample = awakeSampleList.size() > 5 ? EMPTY_UUIDS : (UUID[])awakeSampleList.toArray((x$0) -> new UUID[x$0]);
            return new SleepMultiplayer(sleepersCount, awakeCount, awakeSample);
        }
    }
}
