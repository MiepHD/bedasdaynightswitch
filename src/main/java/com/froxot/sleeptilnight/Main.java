package com.froxot.sleeptilnight;

import com.hypixel.hytale.builtin.beds.sleep.systems.player.EnterBedSystem;
import com.hypixel.hytale.builtin.beds.sleep.systems.player.UpdateSleepPacketSystem;
import com.hypixel.hytale.builtin.beds.sleep.systems.world.StartSlumberSystem;
import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

public class Main extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Config<SleepTilNightConfig> config = this.withConfig("SleepTilNightConfig", SleepTilNightConfig.CODEC);

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        this.config.save();
        LOGGER.atInfo().log("Plugin has been enabled!");
        try {
            ComponentRegistryProxy<EntityStore> proxy = this.getEntityStoreRegistry();
            Field registryField = ComponentRegistryProxy.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ComponentRegistry<EntityStore> registry = (ComponentRegistry<EntityStore>) registryField.get(proxy);

            registry.unregisterSystem(StartSlumberSystem.class);
            registry.unregisterSystem(EnterBedSystem.class);
            registry.unregisterSystem(UpdateSleepPacketSystem.class);

            this.getEntityStoreRegistry().registerSystem(new CustomUpdateSleepPacketSystem());
            this.getEntityStoreRegistry().registerSystem(new CustomEnterBedSystem());
            this.getEntityStoreRegistry().registerSystem(new CustomStartSlumberSystem(this.config));
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace StartSlumberSystem", e);
        }
    }
}
