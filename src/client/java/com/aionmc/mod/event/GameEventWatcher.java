package com.aionmc.mod.event;

import com.aionmc.mod.config.ModConfig;
import com.aionmc.mod.memory.ChatMemory;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GameEventWatcher {

    private static final Set<EntityType<?>> NOTABLE_MOBS = new HashSet<>(Set.of(
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.WARDEN,
            EntityType.ELDER_GUARDIAN
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

    private void checkLowHealth(ClientPlayerEntity player) {
        float fraction = player.getHealth() / player.getMaxHealth();
        boolean isLowNow = fraction <= config.lowHealthThreshold;

        if (isLowNow && !wasLowHealth) {
            onAmbientEvent.accept(
                    "The player's health just dropped low (" + Math.round(fraction * 100) + "%)."
            );
        }
        wasLowHealth = isLowNow;
    }

    public void onEntityKilled(LivingEntity killed, Entity killer, MinecraftClient client) {
        if (!config.reactToNotableKills) return;
        if (client.player == null || killer != client.player) return;

        EntityType<?> type = killed.getType();
        if (NOTABLE_MOBS.contains(type)) {
            String name = describeNotableKill(type);
            onAmbientEvent.accept("The player just defeated " + name + "!");
        }
    }

    private String describeNotableKill(EntityType<?> type) {
        if (type == EntityType.ENDER_DRAGON) return "the Ender Dragon";
        if (type == EntityType.WITHER) return "the Wither";
        if (type == EntityType.WARDEN) return "a Warden";
        if (type == EntityType.ELDER_GUARDIAN) return "an Elder Guardian";
        return type.toString();
    }

    public void onAdvancementEarned(String advancementTitle) {
        if (!config.reactToAdvancements) return;
        onAmbientEvent.accept("The player just earned the advancement \"" + advancementTitle + "\".");
    }
}
