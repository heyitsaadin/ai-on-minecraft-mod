package com.aionmc.mod.client;

import com.aionmc.mod.ai.AiClient;
import com.aionmc.mod.config.ModConfig;
import com.aionmc.mod.event.GameEventWatcher;
import com.aionmc.mod.memory.ChatMemory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;

public class AIOnMinecraftClient implements ClientModInitializer {

    private static AIOnMinecraftClient instance;

    private ChatMemory memory;
    private ModConfig config;
    private AiClient aiClient;
    private GameEventWatcher eventWatcher;
    private AiOverlayHud overlayHud;

    @Override
    public void onInitializeClient() {
        instance = this;
        this.config = ModConfig.loadOrCreate(FabricLoader.getInstance().getConfigDir());
        this.memory = new ChatMemory();
        this.aiClient = new AiClient(config);
        this.eventWatcher = new GameEventWatcher(memory, config, this::handleAmbientEvent);
        this.overlayHud = new AiOverlayHud(config);

        eventWatcher.register();
        overlayHud.register();
        registerChatHook();
        registerConfigCommand();
    }

    /**
     * Fabric Loader only ever constructs one instance of a client mod
     * initializer, so this is safe as a simple singleton accessor -- used by
     * AionModMenuIntegration (a separate class, only loaded when Mod Menu is
     * present) to reach the running config without a second load path.
     */
    public static AIOnMinecraftClient getInstance() {
        return instance;
    }

    /**
     * "/aion config" opens the settings screen directly. This stays even now
     * that Mod Menu is wired up (see AionModMenuIntegration) as a
     * dependency-free way to reach settings for anyone without Mod Menu
     * installed.
     */
    private void registerConfigCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("aion")
                        .then(ClientCommands.literal("config")
                                .executes(context -> {
                                    ConfigScreenFactory.open(config);
                                    return 1;
                                }))));
    }

    private void registerChatHook() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!config.aiEnabled) {
                return true;
            }
            memory.addPlayerMessage(message);
            requestAndDisplayReply();
            return true;
        });
    }

    private void handleAmbientEvent(String description) {
        if (!config.aiEnabled) {
            return;
        }
        memory.addEvent(description);
        requestAndDisplayReply();
    }

    private void requestAndDisplayReply() {
        List<ChatMemory.Turn> snapshot = memory.snapshot();

        aiClient.requestReply(snapshot).thenAccept(reply -> {
            memory.addAssistantMessage(reply);

            Minecraft.getInstance().execute(() -> {
                if (config.displayMode != ModConfig.DisplayMode.CHAT) {
                    overlayHud.show(reply);
                    return;
                }

                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    // A lighter, brighter blue than the built-in ChatFormatting.BLUE
                    // (which renders quite dark/navy). Reply body stays plain white.
                    Style lightBlue = Style.EMPTY.withColor(TextColor.fromRgb(0x55AAFF));
                    Component tag = Component.literal("<ChatGpt> ").withStyle(lightBlue);
                    Component body = Component.literal(reply).withStyle(ChatFormatting.WHITE);
                    Component message = tag.copy().append(body);
                    client.player.sendSystemMessage(message);
                }
            });
        });
    }

    public ModConfig getConfig() {
        return config;
    }
}
