package com.josuebrenes.toqueserverbanner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.ServerMetadata;

public final class ToqueServerBannerRenderer {
    private static final int BANNER_TEXTURE_WIDTH = 128;
    private static final int BANNER_TEXTURE_HEIGHT = 16;
    private static final int BANNER_HEIGHT = 72;

    private static final String[] MESSAGES = {
            "☠ UNA VIDA. UNA RUN. UN DESTINO.",
            "⚔ SOBREVIVE • EXPLORA • CONSTRUYE",
            "🔥 ¿CUÁNTO DURARÁ ESTA RUN?",
            "☠ SI UNO MUERE, TODOS REINICIAMOS."
    };

    private ToqueServerBannerRenderer() {
    }

    public static void render(
            DrawContext context,
            ServerInfo server,
            int x,
            int y,
            int entryWidth,
            int mouseX,
            int mouseY,
            boolean hovered
    ) {
        int height = BANNER_HEIGHT;
        int right = x + entryWidth;

        context.drawTexture(
                ToqueServerBannerClientAssets.BANNER,
                x,
                y,
                0,
                0,
                entryWidth,
                height,
                BANNER_TEXTURE_WIDTH,
                BANNER_TEXTURE_HEIGHT
        );

        long time = System.currentTimeMillis();
        int messageIndex = (int) ((time / 1800L) % MESSAGES.length);
        float pulse = 0.5F + 0.5F * (float) Math.sin(time / 260.0D);
        int glow = 150 + (int) (90.0F * pulse);

        if (hovered) {
            context.fill(x, y, right, y + height, (glow << 24) | 0x330000);
            context.drawBorder(x, y, entryWidth, height, 0xFFFF3333);
        } else {
            context.drawBorder(x, y, entryWidth, height, (glow << 24) | 0x660000);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        String playerCount = getPlayerCount(server);
        int countWidth = textRenderer.getWidth(playerCount);
        context.drawTextWithShadow(
                textRenderer,
                playerCount,
                right - countWidth - 42,
                y + 10,
                0xFFF2F2F2
        );

        drawPing(context, x, y, entryWidth, server.ping);

        String tryText = extractTag(server.label.getString(), "TRY #");
        String dayText = extractTag(server.label.getString(), "DÍA");
        if (tryText == null) {
            tryText = "TRY --";
        }
        if (dayText == null) {
            dayText = "DÍA --";
        }

        context.drawTextWithShadow(textRenderer, tryText, right - 150, y + 10, 0xFFFF4A4A);
        context.drawTextWithShadow(textRenderer, dayText, right - 88, y + 10, 0xFFE0E0E0);

        String message = MESSAGES[messageIndex];
        int maxMessageWidth = Math.max(120, entryWidth - 230);
        if (textRenderer.getWidth(message) > maxMessageWidth) {
            message = "☠ TOQUE HARDCORE ☠";
        }

        int messageColor = messageIndex == 3 ? 0xFFFF5555 : 0xFFF4F4F4;
        context.drawTextWithShadow(
                textRenderer,
                message,
                x + 108,
                y + 52,
                messageColor
        );

        String subtitle = "TOQUE HARDCORE";
        context.drawTextWithShadow(
                textRenderer,
                subtitle,
                x + 108,
                y + 30,
                0xFFFF3333
        );
    }

    private static String getPlayerCount(ServerInfo server) {
        if (server.players != null) {
            return server.players.online() + "/" + server.players.max();
        }

        String fallback = server.playerCountLabel == null ? "" : server.playerCountLabel.getString();
        return fallback.isBlank() ? "—/—" : fallback;
    }

    private static void drawPing(DrawContext context, int x, int y, int entryWidth, long ping) {
        int bars = ping < 0 ? 0 : ping < 80 ? 5 : ping < 150 ? 4 : ping < 250 ? 3 : ping < 400 ? 2 : 1;
        int startX = x + entryWidth - 25;
        int baseY = y + 55;

        for (int i = 0; i < 5; i++) {
            int barHeight = 4 + i * 3;
            int left = startX + i * 4;
            int top = baseY - barHeight;
            int color = i < bars ? 0xFF35E35A : 0xFF5A5A5A;
            context.fill(left, top, left + 3, baseY, color);
        }
    }

    private static String extractTag(String text, String tag) {
        int start = text.indexOf(tag);
        if (start < 0) {
            return null;
        }

        int end = text.indexOf('|', start);
        if (end < 0) {
            end = text.length();
        }

        String value = text.substring(start, end).trim();
        return value.isEmpty() ? null : value;
    }
}
