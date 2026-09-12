package com.aionmc.mod.client;

import com.aionmc.mod.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

// Note: com.aionmc.mod.client.ConfigScreenFactory (our own screen builder,
// used below with its full qualified name) is a DIFFERENT class from Mod
// Menu's com.terraformersmc.modmenu.api.ConfigScreenFactory imported above
// — same simple name, different package. Both are used deliberately.

/**
 * Lets Mod Menu show a settings gear icon for this mod in the mods list,
 * opening the same Cloth Config screen as "/aion config" and the in-game
 * keybinding.
 *
 * This class is ONLY loaded by Fabric Loader if Mod Menu is installed: it's
 * registered under the "modmenu" custom value in fabric.mod.json rather than
 * the normal "client" entrypoint, which is how Mod Menu's own soft-dependency
 * mechanism works. Players without Mod Menu installed never load this class,
 * so there's no hard dependency and no crash risk if it's absent — the
 * keybinding and /aion config command keep working either way.
 */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ModConfig.loadOrCreate(FabricLoader.getInstance().getConfigDir());
            return com.aionmc.mod.client.ConfigScreenFactory.build(config, parent);
        };
    }
}
