package com.aionmc.mod.client;

import com.aionmc.mod.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Draws the most recent AI reply as plain on-screen text in a corner of the
 * HUD, as an alternative to posting it in chat. Only active while
 * {@link ModConfig#displayMode} is set to one of the OVERLAY_* options; the
 * text clears itself after a short display window so it doesn't linger
 * forever over gameplay.
 */
public final class AiOverlayHud {

    private static final int MARGIN = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_WIDTH = 220;
    /** How long a reply stays on screen before fading out, in client ticks (20 ticks = 1s). */
    private static final int DISPLAY_TICKS = 20 * 12;

    private final ModConfig config;
    private String currentText = null;
    private int ticksRemaining = 0;

    public AiOverlayHud(ModConfig config) {
        this.config = config;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    /** Called whenever a new AI reply arrives, regardless of the current display mode. */
    public void show(String text) {
        this.currentText = text;
        this.ticksRemaining = DISPLAY_TICKS;
    }

    private void onHudRender(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        if (config.displayMode == ModConfig.DisplayMode.CHAT) {
            return;
        }
        if (currentText == null || ticksRemaining <= 0) {
            return;
        }
        ticksRemaining--;

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(Component.literal(currentText), MAX_WIDTH);

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int blockHeight = lines.size() * LINE_HEIGHT;

        int x = switch (config.displayMode) {
            case OVERLAY_TOP_LEFT, OVERLAY_BOTTOM_RIGHT -> MARGIN;
            case OVERLAY_TOP_RIGHT -> screenWidth - MAX_WIDTH - MARGIN;
            default -> MARGIN;
        };
        // Overlay_top_right and overlay_bottom_right both anchor from the right edge;
        // recompute x precisely per-line below since line widths vary.

        int yStart = switch (config.displayMode) {
            case OVERLAY_TOP_LEFT, OVERLAY_TOP_RIGHT -> MARGIN;
            case OVERLAY_BOTTOM_RIGHT -> screenHeight - blockHeight - MARGIN;
            default -> MARGIN;
        };

        int y = yStart;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            int lineWidth = font.width(line);
            int lineX = switch (config.displayMode) {
                case OVERLAY_TOP_LEFT -> MARGIN;
                case OVERLAY_TOP_RIGHT, OVERLAY_BOTTOM_RIGHT -> screenWidth - lineWidth - MARGIN;
                default -> MARGIN;
            };
            graphics.drawString(font, line, lineX, y, 0xFFFFFF, true);
            y += LINE_HEIGHT;
        }
    }
}
