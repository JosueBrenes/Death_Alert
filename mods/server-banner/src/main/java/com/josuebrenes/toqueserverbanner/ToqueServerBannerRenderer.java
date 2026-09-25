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

    private static final int LOGO_TEXTURE_SIZE = 64;
    private static final int LOGO_SIZE = 26;

    private static final int PADDING = 5;
    /** Gap between the logo and the text column beside it. */
    private static final int LOGO_GAP = 6;

    private static final int BACKGROUND_TOP = 0xFF1A0406;
    private static final int BACKGROUND_BOTTOM = 0xFF0D0203;
    private static final int BACKGROUND_TOP_HOVER = 0xFF43090D;
    private static final int BACKGROUND_BOTTOM_HOVER = 0xFF1E0407;

    private ToqueServerBannerRenderer() {
    }

    public static void render(DrawContext context, ServerInfo server,
                              int x, int y, int entryWidth, int entryHeight, boolean hovered) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        int right = x + entryWidth;
        int bottom = y + entryHeight;

        // Vanilla's text is batched and only reaches the screen when the draw
        // context is flushed, which happens after this injection returns. Without
        // this the name and the MOTD are painted on top of the panel meant to hide
        // them, and the row reads as two rows of text on top of each other.
        context.draw();

        RenderSystem.enableBlend();
        drawPanel(context, x, y, right, bottom, hovered);

        context.drawTexture(ToqueServerBannerClientAssets.BANNER,
                x + PADDING, y + (entryHeight - LOGO_SIZE) / 2, LOGO_SIZE, LOGO_SIZE,
                0.0F, 0.0F, LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE,
                LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE);

        int textX = x + PADDING + LOGO_SIZE + LOGO_GAP;
        drawTitle(context, font, textX, y + 4);
        drawSeries(context, font, server, right, y + 4);
        drawStatus(context, font, server, right, y + 15);
        context.drawTextWithShadow(font,
                Text.literal(currentMessage()).formatted(Formatting.GRAY),
                textX, y + 25, 0xFFB0A0A0);

        // Flush again so the panel's own text cannot be reordered behind whatever
        // the screen draws after this entry.
        context.draw();
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

    private static void drawTitle(DrawContext context, TextRenderer font, int x, int y) {
        context.drawTextWithShadow(font,
                Text.literal("☠ TOQUE HARDCORE ☠")
                        .formatted(Formatting.RED, Formatting.BOLD),
                x, y, 0xFFFF5555);
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
