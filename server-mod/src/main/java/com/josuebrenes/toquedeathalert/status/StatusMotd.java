package com.josuebrenes.toquedeathalert.status;

import com.josuebrenes.toquedeathalert.ToqueDeathAlert;
import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * The two MOTD lines shown under the server name in the multiplayer list.
 *
 * <p>This is the only part of that screen a server can change. The list is drawn
 * by the client before it ever connects, and the status reply carries nothing but
 * the MOTD, the player counts, the version and the 64x64 favicon. There is no
 * frame, no background and no layout to send, so the design lives entirely in
 * these two lines of coloured text.
 *
 * <p>Rebuilt on every ping, so the Try and the day are current and the tagline
 * changes each time the player refreshes the list.
 */
public final class StatusMotd {
    private static final String SKULL = "☠";
    private static final long TICKS_PER_DAY = 24000L;
    private static final long TAGLINE_MILLIS = 4000L;

    private static final String[] TAGLINES = {
            SKULL + " UNA VIDA. UNA RUN. UN DESTINO.",
            "⚔ SOBREVIVE · EXPLORA · CONSTRUYE",
            "⚡ ¿CUÁNTO DURARÁ ESTA RUN?",
            SKULL + " SI UNO MUERE, TODOS REINICIAMOS."
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

        MutableText line = Text.literal(SKULL + " TOQUE HARDCORE " + SKULL)
                .formatted(Formatting.DARK_RED, Formatting.BOLD);
        line.append(Text.literal("   ·   ").formatted(Formatting.DARK_GRAY));
        line.append(Text.literal("TRY #" + stats.tryNumber()).formatted(Formatting.GOLD));

        long day = currentDay(server);
        if (day > 0L) {
            line.append(Text.literal("   ·   ").formatted(Formatting.DARK_GRAY));
            line.append(Text.literal("DÍA " + day).formatted(Formatting.YELLOW));
        }

        return line.append(Text.literal("\n"))
                .append(Text.literal(tagline()).formatted(Formatting.GRAY));
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
