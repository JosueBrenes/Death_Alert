package com.josuebrenes.toqueserverbanner;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Paints the TOQUE entry over the vanilla one.
 *
 * <p>Drawing happens after vanilla has rendered, covering the whole row with an
 * opaque panel. Vanilla still runs: the entry is what kicks off the status ping
 * and loads the favicon, so it must not be cancelled or the row would never learn
 * its player count, its ping or its MOTD.
 */
public final class ToqueServerBannerRenderer {
    /** Every character here must exist in Minecraft's font: no emoji above the BMP. */
    private static final String[] MESSAGES = {
            "☠ UNA VIDA. UNA RUN. UN DESTINO.",
            "⚔ SOBREVIVE • EXPLORA • CONSTRUYE",
            "⚡ ¿CUÁNTO DURARÁ ESTA RUN?",
            "☠ SI UNO MUERE, TODOS REINICIAMOS."
    };
    private static final long MESSAGE_MILLIS = 1800L;
    private static final long PULSE_MILLIS = 2200L;

    private static final int LOGO_TEXTURE_WIDTH = 128;
    private static final int LOGO_TEXTURE_HEIGHT = 16;
    private static final int LOGO_WIDTH = 80;
    private static final int LOGO_HEIGHT = 10;

    private static final int PADDING = 5;

    private static final int BACKGROUND_TOP = 0xF01A0406;
    private static final int BACKGROUND_BOTTOM = 0xF00D0203;
    private static final int BACKGROUND_TOP_HOVER = 0xF043090D;
    private static final int BACKGROUND_BOTTOM_HOVER = 0xF01E0407;

    private ToqueServerBannerRenderer() {
    }

    public static void render(DrawContext context, ServerInfo server,
                              int x, int y, int entryWidth, int entryHeight, boolean hovered) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        int right = x + entryWidth;
        int bottom = y + entryHeight;

        RenderSystem.enableBlend();
        drawPanel(context, x, y, right, bottom, hovered);

        context.drawTexture(ToqueServerBannerClientAssets.BANNER,
                x + PADDING, y + PADDING, LOGO_WIDTH, LOGO_HEIGHT,
                0.0F, 0.0F, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT,
                LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);

        drawSeries(context, font, server, right, y + PADDING);
        context.drawTextWithShadow(font,
                Text.literal("☠ TOQUE HARDCORE ☠").formatted(Formatting.RED, Formatting.BOLD),
                x + PADDING, y + PADDING + LOGO_HEIGHT + 3, 0xFFFF5555);
        drawStatus(context, font, server, right, y + PADDING + LOGO_HEIGHT + 3);
        context.drawTextWithShadow(font,
                Text.literal(currentMessage()).formatted(Formatting.GRAY),
                x + PADDING, bottom - PADDING - font.fontHeight + 1, 0xFFB0A0A0);

        RenderSystem.disableBlend();
    }

    /** Dark panel with a border that breathes, and brightens under the cursor. */
    private static void drawPanel(DrawContext context, int x, int y, int right, int bottom,
                                  boolean hovered) {
        context.fillGradient(x, y, right, bottom,
                hovered ? BACKGROUND_TOP_HOVER : BACKGROUND_TOP,
                hovered ? BACKGROUND_BOTTOM_HOVER : BACKGROUND_BOTTOM);

        float pulse = (float) ((Math.sin(System.currentTimeMillis() % PULSE_MILLIS
                / (double) PULSE_MILLIS * 2.0 * Math.PI) + 1.0) / 2.0);
        int red = hovered ? 200 + (int) (55 * pulse) : 110 + (int) (60 * pulse);
        int border = 0xFF000000 | (red << 16) | (red / 6 << 8) | (red / 6);

        context.drawBorder(x, y, right - x, bottom - y, border);
        context.fill(x, y, x + 2, bottom, border);
    }

    /** TRY and DAY, right aligned on the first line, when the MOTD carries them. */
    private static void drawSeries(DrawContext context, TextRenderer font, ServerInfo server,
                                   int right, int y) {
        SeriesInfo series = SeriesInfo.from(server);
        if (!series.hasTry() && !series.hasDay()) {
            return;
        }
        StringBuilder text = new StringBuilder();
        if (series.hasTry()) {
            text.append("TRY #").append(series.tryNumber());
        }
        if (series.hasTry() && series.hasDay()) {
            text.append("   ");
        }
        if (series.hasDay()) {
            text.append("DÍA ").append(series.day());
        }

        String line = text.toString();
        context.drawTextWithShadow(font, Text.literal(line).formatted(Formatting.GOLD),
                right - PADDING - font.getWidth(line), y, 0xFFFFAA00);
    }

    /** Player count and ping, right aligned on the second line. */
    private static void drawStatus(DrawContext context, TextRenderer font, ServerInfo server,
                                   int right, int y) {
        String players = playerCount(server);
        String ping = server.ping > 0L ? server.ping + "ms" : "--";
        String line = players + "   " + ping;

        context.drawTextWithShadow(font, Text.literal(line).formatted(Formatting.GRAY),
                right - PADDING - font.getWidth(line), y, pingColour(server.ping));
    }

    private static String playerCount(ServerInfo server) {
        if (server.players == null) {
            return "-/-";
        }
        return server.players.online() + "/" + server.players.max();
    }

    private static int pingColour(long ping) {
        if (ping <= 0L) {
            return 0xFF808080;
        }
        if (ping < 150L) {
            return 0xFF55FF55;
        }
        return ping < 300L ? 0xFFFFFF55 : 0xFFFF5555;
    }

    private static String currentMessage() {
        int index = (int) ((System.currentTimeMillis() / MESSAGE_MILLIS) % MESSAGES.length);
        return MESSAGES[index];
    }
}
