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
 * Opened via the "/aion config" command (see AIOnMinecraftClient) rather
 * than through Mod Menu, since Mod Menu's Minecraft 26.2 build wasn't
 * something this could pin a verified version for.
 */
public final class ConfigScreenFactory {

    private ConfigScreenFactory() {
    }

    public static void open(ModConfig config) {
        Minecraft client = Minecraft.getInstance();
        // We don't have a confirmed way to read the currently-open screen
        // back from Gui in 26.2 (getScreen() isn't the right name here),
        // and we don't strictly need it: this is always opened fresh via
        // the /aion config command, not from within another screen, so a
        // null parent is fine — "Done" will just close back to the game.
        Screen parent = null;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("AI on Minecraft"))
                .setSavingRunnable(() -> config.save(FabricLoader.getInstance().getConfigDir()));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

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

        client.gui.setScreen(builder.build());
    }

    private static String providerDisplayName(ModConfig.Provider provider) {
        return switch (provider) {
            case FREE_PROXY -> "Free (built-in)";
            case CUSTOM_OPENAI_COMPATIBLE -> "Custom: OpenAI-compatible";
            case CUSTOM_NVIDIA -> "Custom: NVIDIA NIM";
        };
    }
}
