package com.froxot.bedasdaynightswitch;


import com.hypixel.hytale.builtin.beds.sleep.systems.player.EnterBedSystem;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class CustomEnterBedSystem extends EnterBedSystem {

    public CustomEnterBedSystem(@NonNullDecl ComponentType<EntityStore, MountedComponent> mountedComponentType, @NonNullDecl ComponentType<EntityStore, PlayerRef> playerRefComponentType) {
        super(mountedComponentType, playerRefComponentType);
    }

    private static void onEnterBed(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ComponentType<EntityStore, PlayerRef> playerRefComponentType) {}
}
