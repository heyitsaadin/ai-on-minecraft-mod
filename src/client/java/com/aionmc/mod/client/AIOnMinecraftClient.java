package com.aionmc.mod.client;

import com.aionmc.mod.ai.AiClient;
import com.aionmc.mod.config.ModConfig;
import com.aionmc.mod.event.GameEventWatcher;
import com.aionmc.mod.memory.ChatMemory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AIOnMinecraftClient implements ClientModInitializer {

    private ChatMemory memory;
    private ModConfig config;
    private AiClient aiClient;
    private GameEventWatcher eventWatcher;

    @Override
    public void onInitializeClient() {
        this.config = ModConfig.loadOrCreate(FabricLoader.getInstance().getConfigDir());
        this.memory = new ChatMemory();
        this.aiClient = new AiClient(config);
        this.eventWatcher = new GameEventWatcher(memory, config, this::handleAmbientEvent);

        eventWatcher.register();
        registerChatHook();
    }

    private void registerChatHook() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            memory.addPlayerMessage(message);
            requestAndDisplayReply();
            return true;
        });
    }

    private void handleAmbientEvent(String description) {
        memory.addEvent(description);
        requestAndDisplayReply();
    }

    private void requestAndDisplayReply() {
        List<ChatMemory.Turn> snapshot = memory.snapshot();

        aiClient.requestReply(snapshot).thenAccept(reply -> {
            memory.addAssistantMessage(reply);

            Minecraft.getInstance().execute(() -> {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    client.player.sendSystemMessage(
                            Component.literal("<AI> " + reply)
                    );
                }
            });
        });
    }

    public ModConfig getConfig() {
        return config;
    }
}
