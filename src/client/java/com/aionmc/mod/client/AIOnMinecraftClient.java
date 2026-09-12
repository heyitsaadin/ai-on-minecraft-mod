package com.aionmc.mod.client;

import com.aionmc.mod.ai.AiClient;
import com.aionmc.mod.config.ModConfig;
import com.aionmc.mod.event.GameEventWatcher;
import com.aionmc.mod.memory.ChatMemory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
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
                    Component tag = Component.literal("<ChatGpt> ").withStyle(ChatFormatting.BLUE);
                    Component message = tag.copy().append(Component.literal(reply));
                    client.player.sendSystemMessage(message);
                }
            });
        });
    }

    public ModConfig getConfig() {
        return config;
    }
}
