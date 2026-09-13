package com.aionmc.mod.client;

import com.aionmc.mod.ai.AiClient;
import com.aionmc.mod.config.ModConfig;
import com.aionmc.mod.event.GameEventWatcher;
import com.aionmc.mod.memory.ChatMemory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        registerGameMessageHook();
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

    /**
     * Matches the vanilla "has made the advancement [Title]" / "has completed
     * the challenge [Title]" / "has reached the goal [Title]" announcement
     * text, capturing just the bracketed title. These are the three
     * announcement shapes vanilla uses for the three advancement frame
     * types (task/challenge/goal).
     */
    private static final Pattern ADVANCEMENT_ANNOUNCEMENT = Pattern.compile(
            "has (?:made the advancement|completed the challenge|reached the goal) \\[(.+)]");

    /**
     * There is no server-side kill event a client-only mod can use (see
     * GameEventWatcher for the full explanation), so advancement/goal/
     * challenge announcements and death messages are both picked up here,
     * from the same client-visible signal: any "game message" the server
     * broadcasts (death messages, advancement announcements, join/leave,
     * etc. -- see ClientReceiveMessageEvents.GAME's Javadoc). This is the
     * same text shown in the chat/system-message log in vanilla.
     */
    private void registerGameMessageHook() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!config.aiEnabled) return;

            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            String text = message.getString();
            String playerName = client.player.getName().getString();

            // Advancement/goal/challenge announcements are always phrased
            // "<name> has made the advancement [...]" for this player's own
            // announcements -- only react to those, not other players'.
            if (text.startsWith(playerName + " ")) {
                Matcher advancementMatch = ADVANCEMENT_ANNOUNCEMENT.matcher(text);
                if (advancementMatch.find()) {
                    eventWatcher.onAdvancementEarned(advancementMatch.group(1));
                }
            }

            // Death messages come in either order ("Foo was slain by the
            // Wither" or "The Warden was slain by Foo") -- onGameMessageReceived
            // handles telling those apart and only reacts to the player's own
            // kills, so every game message is forwarded here unconditionally.
            eventWatcher.onGameMessageReceived(text, playerName);
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
