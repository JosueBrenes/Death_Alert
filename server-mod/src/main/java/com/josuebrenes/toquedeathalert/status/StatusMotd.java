package com.josuebrenes.toquedeathalert.status;

import com.josuebrenes.toquedeathalert.ToqueDeathAlert;
import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * The two MOTD lines shown under the server name in the multiplayer list.
 *
 * <p>This is the whole of the design surface a server has on that screen. The
 * list is drawn by the client before it ever connects, and the status reply
 * carries nothing but the MOTD, the player counts, the version and the 64x64
 * favicon: no frame, no background, no layout. So the two lines have to carry
 * the identity on their own, and they are built to work on a plain vanilla
 * client with nothing installed.
 *
 * <p>Rebuilt on every ping, so the Try, the day and the death count are current
 * each time a player refreshes their list.
 */
public final class StatusMotd {
    private static final String SKULL = "☠";

    /** The list clips the MOTD at about 45 characters a line, so both lines stay short. */
    private static final String TITLE = SKULL + " T O Q U E   H A R D C O R E " + SKULL;

    private static final int GRADIENT_FROM = 0xFF7B6B;
    private static final int GRADIENT_TO = 0xB01111;

    private static final long TICKS_PER_DAY = 24000L;
    private static final long TAGLINE_MILLIS = 4000L;

    private static final String[] TAGLINES = {
            "UNA VIDA, UNA RUN",
            "SOBREVIVE O REINICIA",
            "¿CUÁNTO DURARÁS?",
            "TODOS O NINGUNO"
    };

    private StatusMotd() {
    }

    /** The replacement description, or null to leave the vanilla MOTD alone. */
    @Nullable
    public static Text build(MinecraftServer server) {
        SeriesStatsRepository stats = ToqueDeathAlert.runtime().stats();
        if (stats == null) {
            return null;
        }
        return gradient(TITLE, GRADIENT_FROM, GRADIENT_TO)
                .append(Text.literal("\n"))
                .append(statusLine(server, stats));
    }

    private static Text statusLine(MinecraftServer server, SeriesStatsRepository stats) {
        MutableText line = Text.literal("TRY #" + stats.tryNumber()).formatted(Formatting.GOLD);

        long day = currentDay(server);
        if (day > 0L) {
            line.append(separator()).append(Text.literal("DÍA " + day).formatted(Formatting.YELLOW));
        }

        line.append(separator())
                .append(Text.literal(SKULL + " " + stats.totalDeaths()).formatted(Formatting.RED));

        return line.append(separator())
                .append(Text.literal(tagline()).formatted(Formatting.GRAY, Formatting.ITALIC));
    }

    private static Text separator() {
        return Text.literal("  ·  ").formatted(Formatting.DARK_GRAY);
    }

    /**
     * Fades the text from one colour to the other, a character at a time.
     *
     * <p>Plain named colours look flat next to the icon, and every client since
     * 1.16 understands the full RGB range in a chat component, so this needs
     * nothing installed either.
     */
    private static MutableText gradient(String text, int from, int to) {
        MutableText result = Text.empty();
        int last = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / last;
            int colour = blend(from, to, ratio);
            result.append(Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)).withBold(true)));
        }
        return result;
    }

    private static int blend(int from, int to, float ratio) {
        int red = channel(from, 16, to, ratio);
        int green = channel(from, 8, to, ratio);
        int blue = channel(from, 0, to, ratio);
        return (red << 16) | (green << 8) | blue;
    }

    private static int channel(int from, int shift, int to, float ratio) {
        int start = (from >> shift) & 0xFF;
        int end = (to >> shift) & 0xFF;
        return Math.round(start + (end - start) * ratio);
    }

    private static long currentDay(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld == null ? -1L : (overworld.getTimeOfDay() / TICKS_PER_DAY) + 1L;
    }

    private static String tagline() {
        int index = (int) ((System.currentTimeMillis() / TAGLINE_MILLIS) % TAGLINES.length);
        return TAGLINES[index];
    }
}
