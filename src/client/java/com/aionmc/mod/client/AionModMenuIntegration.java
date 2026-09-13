package com.aionmc.mod.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers this mod's settings screen with Mod Menu, so a config button
 * shows up next to "Chat With Chatgpt" in the mods list. The actual screen
 * contents live in ConfigScreenFactory (this class only wires that into
 * Mod Menu's API).
 *
 * This class implements com.terraformersmc.modmenu.api.ModMenuApi and is
 * only loaded by Fabric Loader when referenced from a "modmenu" entrypoint
 * in fabric.mod.json -- which Mod Menu itself only reads when installed.
 * Mod Menu is a modCompileOnly + modLocalRuntime dependency (see
 * build.gradle), so nothing outside this "modmenu" entrypoint may ever
 * reference this class: doing so would crash the game on startup for
 * anyone without Mod Menu installed, since the com.terraformersmc.modmenu
 * classes wouldn't exist on their classpath at all.
 *
 * Uses AIOnMinecraftClient.getInstance() to reach the already-running
 * config rather than loading it a second time.
 */
public final class AionModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> com.aionmc.mod.client.ConfigScreenFactory.build(
                AIOnMinecraftClient.getInstance().getConfig(), parent);
    }
}
