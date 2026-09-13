package com.aionmc.mod.client;

import com.aionmc.mod.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Builds the in-game settings screen using Cloth Config.
 *
 * The idea for the provider field: pick "Free (built-in)" and everything
 * else on this screen is irrelevant — the mod just talks to our hosted
 * proxy, no key needed from the player at all. Only switching to one of
 * the "custom" options makes the endpoint/model/key fields matter, and
 * even then, an empty key simply means those requests will fail with a
 * clear in-chat error rather than silently falling back, so the player
 * always knows which mode they're actually in.
 *
 * Reachable two ways: the "/aion config" chat command, and the in-game
 * keybinding (see ConfigKeyBinding). Not currently wired into Mod Menu —
 * that integration was pulled pending a verified Mod Menu build for
 * Minecraft 26.2 (see project notes/history for why).
 */
public final class ConfigScreenFactory {

    private ConfigScreenFactory() {
    }

    /**
     * Opens the screen directly (used by the "/aion config" command and the
     * in-game keybinding). There's no sensible parent screen in either case
     * — both are triggered from gameplay, not from within another screen —
     * so "Done" closes back to the game.
     */
    public static void open(ModConfig config) {
        Minecraft.getInstance().gui.setScreen(build(config, null));
    }

    /**
     * Builds (but does not display) the settings screen with the given
     * parent, so a caller like Mod Menu can display it and handle "Done"
     * navigating back to its own mods list instead of to gameplay.
     */
    public static Screen build(ModConfig config, Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Chat With Chatgpt"))
                .setSavingRunnable(() -> config.save(FabricLoader.getInstance().getConfigDir()));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(
                entryBuilder.startBooleanToggle(Component.literal("Enable AI"), config.aiEnabled)
                        .setDefaultValue(true)
                        .setTooltip(Component.literal(
                                "Master switch. Turn off to stop the mod from contacting the AI\n" +
                                        "entirely -- no chat replies, no ambient event reactions."))
                        .setSaveConsumer(value -> config.aiEnabled = value)
                        .build());

        general.addEntry(
                entryBuilder.startBooleanToggle(Component.literal("Use my own API key"), config.useOwnApiKey)
                        .setDefaultValue(false)
                        .setTooltip(Component.literal(
                                "Off: use the free built-in proxy, no key needed.\n" +
                                        "On: switch the provider below to a Custom option and enter\n" +
                                        "your own endpoint, model, and API key."))
                        .setSaveConsumer(value -> config.useOwnApiKey = value)
                        .build());

        general.addEntry(
                entryBuilder.startEnumSelector(
                                Component.literal("AI provider"),
                                ModConfig.Provider.class,
                                config.provider)
                        .setDefaultValue(ModConfig.Provider.FREE_PROXY)
                        .setTooltip(Component.literal(
                                "Free (built-in): no key needed, uses the mod's hosted proxy.\n" +
                                        "Custom OpenAI-compatible: your own OpenAI, self-hosted, or compatible endpoint.\n" +
                                        "Custom NVIDIA: an NVIDIA build.nvidia.com NIM endpoint."))
                        .setEnumNameProvider(value -> Component.literal(providerDisplayName((ModConfig.Provider) value)))
                        .setSaveConsumer(value -> config.provider = value)
                        .build());

        general.addEntry(
                entryBuilder.startStrField(Component.literal("Custom endpoint URL"), config.customEndpoint)
                        .setDefaultValue("https://api.openai.com/v1/chat/completions")
                        .setTooltip(Component.literal(
                                "Only used when the provider above is a Custom option.\n" +
                                        "Ignored entirely on the Free provider."))
                        .setSaveConsumer(value -> config.customEndpoint = value)
                        .build());

        general.addEntry(
                entryBuilder.startStrField(Component.literal("Custom model name"), config.customModel)
                        .setDefaultValue("gpt-4o-mini")
                        .setTooltip(Component.literal("e.g. gpt-4o-mini, meta/llama3-70b-instruct."))
                        .setSaveConsumer(value -> config.customModel = value)
                        .build());

        general.addEntry(
                entryBuilder.startStrField(Component.literal("Custom API key"), config.customApiKey)
                        .setDefaultValue("")
                        .setTooltip(Component.literal(
                                "Only used when the provider above is a Custom option.\n" +
                                        "Leave the provider on Free if you don't have a key — " +
                                        "the mod will use the built-in free proxy instead."))
                        .setSaveConsumer(value -> config.customApiKey = value)
                        .build());

        ConfigCategory events = builder.getOrCreateCategory(Component.literal("Ambient events"));

        events.addEntry(
                entryBuilder.startBooleanToggle(Component.literal("React to advancements"), config.reactToAdvancements)
                        .setDefaultValue(true)
                        .setSaveConsumer(value -> config.reactToAdvancements = value)
                        .build());

        events.addEntry(
                entryBuilder.startBooleanToggle(Component.literal("React to low health"), config.reactToLowHealth)
                        .setDefaultValue(true)
                        .setSaveConsumer(value -> config.reactToLowHealth = value)
                        .build());

        events.addEntry(
                entryBuilder.startDoubleField(Component.literal("Low health threshold"), config.lowHealthThreshold)
                        .setDefaultValue(0.3)
                        .setMin(0.05)
                        .setMax(0.95)
                        .setTooltip(Component.literal("Fraction of max health (0.05-0.95) considered \"low\"."))
                        .setSaveConsumer(value -> config.lowHealthThreshold = value)
                        .build());

        events.addEntry(
                entryBuilder.startBooleanToggle(Component.literal("React to notable mob kills"), config.reactToNotableKills)
                        .setDefaultValue(true)
                        .setSaveConsumer(value -> config.reactToNotableKills = value)
                        .build());

        ConfigCategory display = builder.getOrCreateCategory(Component.literal("Display"));

        display.addEntry(
                entryBuilder.startEnumSelector(
                                Component.literal("Show AI replies"),
                                ModConfig.DisplayMode.class,
                                config.displayMode)
                        .setDefaultValue(ModConfig.DisplayMode.CHAT)
                        .setTooltip(Component.literal(
                                "Chat: replies appear as normal chat messages.\n" +
                                        "Overlay: replies appear as on-screen text in the chosen corner\n" +
                                        "instead of the chat log."))
                        .setEnumNameProvider(value -> Component.literal(displayModeName((ModConfig.DisplayMode) value)))
                        .setSaveConsumer(value -> config.displayMode = value)
                        .build());

        return builder.build();
    }

    private static String providerDisplayName(ModConfig.Provider provider) {
        return switch (provider) {
            case FREE_PROXY -> "Free (built-in)";
            case CUSTOM_OPENAI_COMPATIBLE -> "Custom: OpenAI-compatible";
            case CUSTOM_NVIDIA -> "Custom: NVIDIA NIM";
        };
    }

    private static String displayModeName(ModConfig.DisplayMode mode) {
        return switch (mode) {
            case CHAT -> "Chat";
            case OVERLAY_TOP_LEFT -> "Overlay: Top Left";
            case OVERLAY_TOP_RIGHT -> "Overlay: Top Right";
            case OVERLAY_BOTTOM_RIGHT -> "Overlay: Bottom Right";
        };
    }
}
