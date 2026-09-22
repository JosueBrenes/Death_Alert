package com.josuebrenes.toquedeathalert.status;

import com.josuebrenes.toquedeathalert.ToqueDeathAlert;
import com.josuebrenes.toquedeathalert.core.FontWidth;
import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.Nullable;

/**
 * The two MOTD lines shown under the server name in the multiplayer list.
 *
 * <p>This is the whole of the design surface a server has on that screen. The
 * list is drawn by the client before it ever connects and the status reply
 * carries only the MOTD, the player counts, the version and the 64x64 favicon:
 * no frame, no background, no layout. Everything here works on a plain vanilla
 * client with nothing installed.
 *
 * <p>The client draws the MOTD left aligned and wraps it at {@code rowWidth - 34},
 * which is 305 - 34 pixels, so centring has to be done here by padding. Both
 * lines are measured in pixels rather than characters, because the font is
 * proportional and counting characters would leave them visibly off centre.
 *
 * <p>Rebuilt on every ping, so the Try, the day and the death count are current
 * each time a player refreshes their list.
 */
public final class StatusMotd {
    /** Width the client wraps the MOTD at: the row is 305 wide, less icon and gap. */
    private static final int MOTD_WIDTH_PX = 271;

    private static final String SKULL = "☠";
    private static final String ARROW = "»";

    private static final int TITLE_FROM = 0xFF8A7A;
    private static final int TITLE_TO = 0xA30D0D;
    private static final int STATUS_FROM = 0xFFD257;
    private static final int STATUS_TO = 0xD1761B;

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

        String title = SKULL + " T O Q U E   H A R D C O R E " + SKULL;
        String status = statusLine(server, stats);

        return centred(title, TITLE_FROM, TITLE_TO)
                .append(Text.literal("\n"))
                .append(centred(status, STATUS_FROM, STATUS_TO));
    }

    /**
     * The second line, with the tagline dropped rather than clipped when the
     * numbers have grown long enough to fill the row on their own.
     */
    private static String statusLine(MinecraftServer server, SeriesStatsRepository stats) {
        StringBuilder line = new StringBuilder("[TRY #").append(stats.tryNumber());

        long day = currentDay(server);
        if (day > 0L) {
            line.append(' ').append(ARROW).append(" DÍA ").append(day);
        }
        line.append("]  ·  ").append(SKULL).append(' ').append(stats.totalDeaths());

        String withTagline = line + "  ·  " + tagline();
        return FontWidth.of(withTagline, true) <= MOTD_WIDTH_PX ? withTagline : line.toString();
    }

    /** Centres the line in the MOTD area and fades it from one colour to the other. */
    private static MutableText centred(String text, int from, int to) {
        MutableText result = Text.literal(FontWidth.centre(text, MOTD_WIDTH_PX, true));
        int last = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {
            int colour = blend(from, to, (float) i / last);
            result.append(Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)).withBold(true)));
        }
        return result;
    }

    private static int blend(int from, int to, float ratio) {
        return (channel(from, to, 16, ratio) << 16)
                | (channel(from, to, 8, ratio) << 8)
                | channel(from, to, 0, ratio);
    }

    private static int channel(int from, int to, int shift, float ratio) {
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
