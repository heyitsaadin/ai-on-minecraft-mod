package com.aionmc.mod.event;

import com.aionmc.mod.config.ModConfig;
import com.aionmc.mod.memory.ChatMemory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GameEventWatcher {

    /**
     * Notable-mob ids as strings rather than EntityType.XYZ static constants.
     * The static constants on EntityType weren't resolving against this
     * build's Minecraft 26.2 setup (Fabric Loom's non-remapping mode for
     * 26.x doesn't expose them the way older, remapped versions did), so we
     * look the type up by its stable resource-location id instead, which
     * works the same way across mapping/remapping configurations.
     */
    private static final Set<String> NOTABLE_MOB_IDS = new HashSet<>(Set.of(
            "minecraft:ender_dragon",
            "minecraft:wither",
            "minecraft:warden",
            "minecraft:elder_guardian"
    ));

    private final ChatMemory memory;
    private final ModConfig config;
    private final Consumer<String> onAmbientEvent;

    private boolean wasLowHealth = false;
    private int tickCounter = 0;

    public GameEventWatcher(ChatMemory memory, ModConfig config, Consumer<String> onAmbientEvent) {
        this.memory = memory;
        this.config = config;
        this.onAmbientEvent = onAmbientEvent;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        tickCounter++;
        if (tickCounter % 10 != 0) return;

        if (config.reactToLowHealth) {
            checkLowHealth(player);
        }
    }

    private void checkLowHealth(LocalPlayer player) {
        float fraction = player.getHealth() / player.getMaxHealth();
        boolean isLowNow = fraction <= config.lowHealthThreshold;

        if (isLowNow && !wasLowHealth) {
            onAmbientEvent.accept(
                    "The player's health just dropped low (" + Math.round(fraction * 100) + "%)."
            );
        }
        wasLowHealth = isLowNow;
    }

    /**
     * Notable-mob display names, matched against the plain text of a death
     * message rather than any entity object. There is no server-side kill
     * event available to a client-only mod (ServerLivingEntityEvents.AFTER_DEATH
     * and friends only fire on the logical server, which this mod has no
     * access to on someone else's server); death messages are the standard
     * client-visible signal instead, delivered the same way in singleplayer
     * and multiplayer via ClientReceiveMessageEvents.GAME (see
     * AIOnMinecraftClient.registerGameMessageHook). Vanilla's death-message
     * text always includes the mob's display name, so a simple substring
     * match is reliable without needing an entity reference at all.
     */
    private static final Set<String> NOTABLE_MOB_NAMES = new HashSet<>(Set.of(
            "Ender Dragon",
            "Wither",
            "Warden",
            "Elder Guardian"
    ));

    /**
     * Checks a death-message string (as broadcast by the server and received
     * verbatim by this client) for a notable-mob name, alongside the
     * player's own name so we only react to the player's own kills rather
     * than other players' or mobs' deaths. Matching by containing the
     * player's name AND a notable mob's name catches both message orders
     * Minecraft uses (e.g. "Foo was slain by the Wither" for the player
     * dying, vs. "The Warden was slain by Foo" for the player's kill) --
     * we only want the latter shape, so we also require the message not
     * start with the player's own name (which would mean the player died).
     */
    public void onGameMessageReceived(String messageText, String playerName) {
        if (!config.reactToNotableKills) return;
        if (messageText.startsWith(playerName)) return;
        if (!messageText.contains(playerName)) return;

        for (String mobName : NOTABLE_MOB_NAMES) {
            if (messageText.contains(mobName)) {
                onAmbientEvent.accept("The player just defeated " + describeNotableKill(mobName) + "!");
                return;
            }
        }
    }

    private String describeNotableKill(String mobName) {
        return switch (mobName) {
            case "Ender Dragon" -> "the Ender Dragon";
            case "Wither" -> "the Wither";
            case "Warden" -> "a Warden";
            case "Elder Guardian" -> "an Elder Guardian";
            default -> mobName;
        };
    }

    public void onAdvancementEarned(String advancementTitle) {
        if (!config.reactToAdvancements) return;
        onAmbientEvent.accept("The player just earned the advancement \"" + advancementTitle + "\".");
    }
}
