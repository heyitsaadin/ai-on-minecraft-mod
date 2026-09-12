package com.aionmc.mod.ai;

import com.aionmc.mod.config.ModConfig;
import com.aionmc.mod.memory.ChatMemory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AiClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String SYSTEM_PROMPT = """
            You're the player's homie, chatting with them right in their Minecraft \
            chat. Talk casual \u2014 like a close friend gaming with them, not a formal \
            assistant. Use everyday slang naturally (e.g. "yo", "nah", "lowkey", \
            "bro", "fr", "ngl") where it fits, contractions, and a relaxed, \
            easygoing tone. Don't force slang into every sentence \u2014 just talk the \
            way a friend actually would.

            You can see things that happen in the game as short "event" notes \
            (advancements, low health, notable mob kills) even when the player \
            hasn't typed anything \u2014 react to those naturally, like a friend \
            watching over their shoulder, without ever saying you "received an \
            event."

            Match your reply length to the question. A quick check-in or simple \
            question gets a quick, short reply (1-2 sentences). But if the player \
            asks for something that actually needs detail \u2014 crafting steps, \
            enchant recommendations, a recipe, directions, explaining game \
            mechanics \u2014 give the full, complete answer with all the steps, even \
            if that runs long. Never cut a multi-step explanation short to keep \
            things brief; a half-finished recipe is useless. Keep the casual, \
            friend-to-friend voice even in longer replies \u2014 detailed doesn't mean \
            formal.

            Never mention being an AI model, an API, or that you have a memory \
            limit.
            """;

    private final ModConfig config;

    public AiClient(ModConfig config) {
        this.config = config;
    }

    public CompletableFuture<String> requestReply(List<ChatMemory.Turn> history) {
        return switch (config.provider) {
            case FREE_PROXY -> requestViaProxy(history);
            case CUSTOM_OPENAI_COMPATIBLE, CUSTOM_NVIDIA -> requestViaCustomEndpoint(history);
        };
    }

    private CompletableFuture<String> requestViaProxy(List<ChatMemory.Turn> history) {
        JsonObject body = new JsonObject();
        body.add("messages", buildMessagesArray(history));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.freeProxyUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::extractProxyReply)
                .exceptionally(this::describeError);
    }

    private String extractProxyReply(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return "(the AI proxy returned an error: " + response.statusCode() + ")";
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (json.has("reply")) {
            return json.get("reply").getAsString();
        }
        return "(unexpected proxy response)";
    }

    private CompletableFuture<String> requestViaCustomEndpoint(List<ChatMemory.Turn> history) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.customModel);
        body.add("messages", buildMessagesArray(history));
        body.addProperty("max_tokens", 1000);
        body.addProperty("temperature", 0.9);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.customEndpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.customApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::extractOpenAiCompatibleReply)
                .exceptionally(this::describeError);
    }

    private String extractOpenAiCompatibleReply(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return "(the AI endpoint returned an error: " + response.statusCode() + ")";
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "(no reply from the model)";
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        return message.get("content").getAsString().trim();
    }

    private JsonArray buildMessagesArray(List<ChatMemory.Turn> history) {
        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", SYSTEM_PROMPT);
        messages.add(system);

        for (ChatMemory.Turn turn : history) {
            JsonObject entry = new JsonObject();
            switch (turn.role()) {
                case PLAYER -> {
                    entry.addProperty("role", "user");
                    entry.addProperty("content", turn.content());
                }
                case ASSISTANT -> {
                    entry.addProperty("role", "assistant");
                    entry.addProperty("content", turn.content());
                }
                case EVENT -> {
                    entry.addProperty("role", "system");
                    entry.addProperty("content", "[game event] " + turn.content());
                }
            }
            messages.add(entry);
        }

        return messages;
    }

    private String describeError(Throwable t) {
        return "(couldn't reach the AI right now: " + t.getMessage() + ")";
    }
}
