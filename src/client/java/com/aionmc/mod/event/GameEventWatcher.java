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

    public void onEntityKilled(LivingEntity killed, Entity killer, Minecraft client) {
        if (!config.reactToNotableKills) return;
        if (client.player == null || killer != client.player) return;

        EntityType<?> type = killed.getType();
        String id = idOf(type);
        if (NOTABLE_MOB_IDS.contains(id)) {
            String name = describeNotableKill(id);
            onAmbientEvent.accept("The player just defeated " + name + "!");
        }
    }

    private String idOf(EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key == null ? "" : key.toString();
    }

    private String describeNotableKill(String id) {
        return switch (id) {
            case "minecraft:ender_dragon" -> "the Ender Dragon";
            case "minecraft:wither" -> "the Wither";
            case "minecraft:warden" -> "a Warden";
            case "minecraft:elder_guardian" -> "an Elder Guardian";
            default -> id;
        };
    }

    public void onAdvancementEarned(String advancementTitle) {
        if (!config.reactToAdvancements) return;
        onAmbientEvent.accept("The player just earned the advancement \"" + advancementTitle + "\".");
    }
}
