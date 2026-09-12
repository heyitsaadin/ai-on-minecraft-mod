package com.aionmc.mod.client;

import com.aionmc.mod.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Registers a keybinding (default: '.') that opens the settings screen from
 * anywhere in-game, no chat command needed. Shows up under
 * Options -> Controls -> Key Binds -> "Chat With Chatgpt" so the player can
 * freely rebind it if '.' collides with something else.
 *
 * Only fires when no other screen is currently open (client.screen == null),
 * so it won't try to pop the config screen over gameplay HUD elements or
 * steal the keypress while, say, a chat box or inventory is already open.
 */
public final class ConfigKeyBinding {

    private static KeyMapping openConfigKey;

    private ConfigKeyBinding() {
    }

    public static void register(ModConfig config) {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.aionminecraft.open_config",
                GLFW.GLFW_KEY_PERIOD,
                "key.categories.aionminecraft"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    ConfigScreenFactory.open(config);
                }
            }
        });
    }
}
