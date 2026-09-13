package com.aionmc.mod.client;

import com.aionmc.mod.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the most recent AI reply as plain on-screen text in a corner (or,
 * for OVERLAY_MIDDLE, centered just above the hotbar) of the HUD, as an
 * alternative to posting it in chat. Only active while
 * {@link ModConfig#displayMode} is set to one of the OVERLAY_* options; the
 * text clears itself after a display window scaled to the reply's length
 * (see displayTicksFor) so it doesn't linger forever over gameplay, but
 * also doesn't vanish before a long reply can be read.
 *
 * Uses HudElementRegistry rather than the older HudRenderCallback -- the
 * latter was removed as of Minecraft 26.1 in favor of this registry-based
 * API. The render callback here takes the plain rendering type used by
 * this build's GUI drawing API (see ConfigScreenFactory/Cloth Config for
 * the settings-screen side of things; this class only concerns the HUD
 * overlay).
 */
public final class AiOverlayHud {

    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath("aionminecraft", "ai_reply_overlay");

    private static final int MARGIN = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_WIDTH = 220;
    /** Height of the hotbar + its margin from the bottom of the screen, in GUI-scaled pixels. */
    private static final int HOTBAR_CLEARANCE = 60;

    /**
     * Display duration scales with message length so a long reply doesn't
     * vanish before it can be read: a flat base time, plus a small amount
     * per character, clamped to a sane range. All in client ticks (20 ticks
     * = 1 real second).
     */
    private static final int BASE_DISPLAY_TICKS = 20 * 6;
    private static final double TICKS_PER_CHARACTER = 0.75;
    private static final int MAX_DISPLAY_TICKS = 20 * 45;
    private static final int TEXT_COLOR = 0xFFFFFFFF; // opaque white (ARGB)

    private final ModConfig config;
    private String currentText = null;
    private int ticksRemaining = 0;

    public AiOverlayHud(ModConfig config) {
        this.config = config;
    }

    public void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                ELEMENT_ID,
                this::render);
    }

    /** Called whenever a new AI reply arrives, regardless of the current display mode. */
    public void show(String text) {
        this.ticksRemaining = displayTicksFor(text);
        this.ticksRemaining = DISPLAY_TICKS;
    }

    private void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (config.displayMode == ModConfig.DisplayMode.CHAT) {
            return;
        }
        if (currentText == null || ticksRemaining <= 0) {
            return;
        }
        ticksRemaining--;

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        List<String> lines = wrap(font, currentText, MAX_WIDTH);

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int blockHeight = lines.size() * LINE_HEIGHT;

        int yStart = switch (config.displayMode) {
            case OVERLAY_TOP_LEFT, OVERLAY_TOP_RIGHT -> MARGIN;
            case OVERLAY_MIDDLE -> screenHeight - HOTBAR_CLEARANCE - blockHeight;
            case OVERLAY_BOTTOM_RIGHT -> screenHeight - blockHeight - MARGIN;
            default -> MARGIN;
        };

        int y = yStart;
        for (String line : lines) {
            int lineWidth = font.width(line);
            int x = switch (config.displayMode) {
                case OVERLAY_TOP_LEFT -> MARGIN;
                case OVERLAY_MIDDLE -> (screenWidth - lineWidth) / 2;
                case OVERLAY_TOP_RIGHT, OVERLAY_BOTTOM_RIGHT -> screenWidth - lineWidth - MARGIN;
                default -> MARGIN;
            };
            graphics.text(font, line, x, y, TEXT_COLOR, true);
            y += LINE_HEIGHT;
        }
    }
    /**
     * Base time plus a small amount per character, so a one-line reply and
     * a multi-paragraph one don't get the same fixed window -- capped so an
     * extremely long reply doesn't sit on screen indefinitely.
     */
    private static int displayTicksFor(String text) {
        int scaled = BASE_DISPLAY_TICKS + (int) Math.round(text.length() * TICKS_PER_CHARACTER);
        return Math.min(scaled, MAX_DISPLAY_TICKS);
    }


    /**
     * Minimal word-wrapping using Font#width, since the exact
     * text-splitting helper name/shape has moved between Minecraft
     * versions in the past and this only needs to handle plain strings.
     */
    private static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}
