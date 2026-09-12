package com.aionmc.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    public enum Provider {
        FREE_PROXY,
        CUSTOM_OPENAI_COMPATIBLE,
        CUSTOM_NVIDIA
    }

    public Provider provider = Provider.FREE_PROXY;

    public String customApiKey = "";

    public String customEndpoint = "https://api.openai.com/v1/chat/completions";

    public String customModel = "gpt-4o-mini";

    public String freeProxyUrl = "https://ai-on-minecraft-proxy.onrender.com/chat";

    public boolean reactToAdvancements = true;

    public boolean reactToLowHealth = true;

    public double lowHealthThreshold = 0.3;

    public boolean reactToNotableKills = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig loadOrCreate(Path configDir) {
        Path file = configDir.resolve("aionminecraft.json");
        try {
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    if (loaded != null) {
                        return loaded;
                    }
                }
            }
        } catch (IOException e) {
            // Fall through and create a fresh default config below.
        }

        ModConfig fresh = new ModConfig();
        fresh.save(configDir);
        return fresh;
    }

    public void save(Path configDir) {
        Path file = configDir.resolve("aionminecraft.json");
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[AI on Minecraft] Failed to save config: " + e.getMessage());
        }
    }
}
