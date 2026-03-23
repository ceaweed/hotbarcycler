package com.example.hotbarcycler;

import com.example.hotbarcycler.screen.HotbarCyclerConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers the Hotbar Cycler settings screen with ModMenu.
 *
 * This class is only loaded when ModMenu is present. The entrypoint is declared
 * in fabric.mod.json under "modmenu" and ModMenu is listed as an optional
 * dependency — so the mod works fine without ModMenu installed.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HotbarCyclerConfigScreen::new;
    }
}
