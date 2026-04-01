package com.froxot.bedasdaynightswitch;

import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSleep;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSleep;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSlumber;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSomnolence;
import com.hypixel.hytale.builtin.beds.sleep.systems.world.CanSleepInWorld;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class CustomStartSlumberSystem extends DelayedSystem<EntityStore> {
    @Nonnull
    private static final Duration NODDING_OFF_DURATION = Duration.ofMillis(3200L);

    private final Config<SleepTilNightConfig> config;

    public CustomStartSlumberSystem(Config<SleepTilNightConfig> config) {
        super(0.3F);
        this.config = config;
    }

    public void delayedTick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        this.checkIfEveryoneIsReadyToSleep(store);
    }

    private void checkIfEveryoneIsReadyToSleep(@Nonnull Store<EntityStore> store) {
        World world = (store.getExternalData()).getWorld();
        Collection<PlayerRef> playerRefs = world.getPlayerRefs();
        if (!playerRefs.isEmpty()) {
            float wakeUpHour;
            if (!CanSleepInWorld.check(world).isNegative()) {
                wakeUpHour = world.getGameplayConfig().getWorldConfig().getSleepConfig().getWakeUpHour();
            } else {
                wakeUpHour = this.config.get().getAfternoonWakeUpHour();
            }
            WorldSomnolence worldSomnolenceResource = store.getResource(WorldSomnolence.getResourceType());
            WorldSleep worldState = worldSomnolenceResource.getState();
            if (worldState == WorldSleep.Awake.INSTANCE) {
                if (this.isEveryoneReadyToSleep(store)) {
                    WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());
                    Instant now = timeResource.getGameTime();
                    Instant target = this.computeWakeupInstant(now, wakeUpHour);
                    float irlSeconds = computeIrlSeconds(now, target);
                    worldSomnolenceResource.setState(new WorldSlumber(now, target, irlSeconds));
                    store.forEachEntityParallel(PlayerSomnolence.getComponentType(), (index, archetypeChunk, commandBuffer) -> {
                        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        commandBuffer.putComponent(ref, PlayerSomnolence.getComponentType(), PlayerSleep.Slumber.createComponent(timeResource));
                    });
                }
            }
        }
    }

    private Instant computeWakeupInstant(@Nonnull Instant now, float wakeUpHour) {
        LocalDateTime ldt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        int hours = (int) wakeUpHour;
        float fractionalHour = wakeUpHour - (float) hours;
        LocalDateTime wakeUpTime = ldt.toLocalDate().atTime(hours, (int) (fractionalHour * 60.0F));
        if (!ldt.isBefore(wakeUpTime)) wakeUpTime = wakeUpTime.plusDays(1L);
        return wakeUpTime.toInstant(ZoneOffset.UTC);
    }

    private static float computeIrlSeconds(@Nonnull Instant startInstant, @Nonnull Instant targetInstant) {
        long ms = Duration.between(startInstant, targetInstant).toMillis();
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        double seconds = Math.max(3.0, (double) hours / 6.0);
        return (float) Math.ceil(seconds);
    }

    private boolean isEveryoneReadyToSleep(ComponentAccessor<EntityStore> store) {
        World world = (store.getExternalData()).getWorld();
        Collection<PlayerRef> playerRefs = world.getPlayerRefs();
        if (playerRefs.isEmpty()) return false;
        for(PlayerRef playerRef : playerRefs) {
            if (!isReadyToSleep(store, playerRef.getReference())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isReadyToSleep(ComponentAccessor<EntityStore> store, Ref<EntityStore> ref) {
        PlayerSomnolence somnolence = store.getComponent(ref, PlayerSomnolence.getComponentType());
        if (somnolence == null) {
            return false;
        } else {
            boolean var10000;
            switch (somnolence.getSleepState()) {
                case PlayerSleep.FullyAwake ignored:
                    var10000 = false;
                    break;
                case PlayerSleep.MorningWakeUp morningWakeUp:
                    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
                    var10000 = morningWakeUp.isReadyToSleepAgain(worldTimeResource.getGameTime());
                    break;
                case PlayerSleep.NoddingOff noddingOff:
                    Instant sleepStart = noddingOff.realTimeStart().plus(NODDING_OFF_DURATION);
                    var10000 = Instant.now().isAfter(sleepStart);
                    break;
                case PlayerSleep.Slumber ignored:
                    var10000 = true;
                    break;
            }

            return var10000;
        }
    }

    public static boolean canNotifyOthersAboutTryingToSleep(@Nonnull ComponentAccessor<EntityStore> store, @Nullable Ref<EntityStore> ref) {
        if (ref != null && ref.isValid()) {
            PlayerSomnolence somnolenceComponent = (PlayerSomnolence)store.getComponent(ref, PlayerSomnolence.getComponentType());
            if (somnolenceComponent == null) {
                return false;
            } else {
                boolean var10000;
                switch (somnolenceComponent.getSleepState()) {
                    case PlayerSleep.FullyAwake fullAwake:
                        var10000 = false;
                        break;
                    case PlayerSleep.MorningWakeUp morningWakeUp:
                        WorldTimeResource worldTimeResource = (WorldTimeResource)store.getResource(WorldTimeResource.getResourceType());
                        var10000 = morningWakeUp.isReadyToSleepAgain(worldTimeResource.getGameTime());
                        break;
                    case PlayerSleep.NoddingOff noddingOff:
                        var10000 = true;
                        break;
                    case PlayerSleep.Slumber ignored:
                        var10000 = true;
                        break;
                }

                return var10000;
            }
        } else {
            return true;
        }
    }
}
