package com.example.hotbarcycler;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotbarCyclerMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcycler");

    @Override
    public void onInitialize() {
        // All inventory manipulation is now done client-side via
        // interactionManager.clickSlot() — no custom networking required.
        LOGGER.info("Hotbar Cycler initialised.");
    }
}