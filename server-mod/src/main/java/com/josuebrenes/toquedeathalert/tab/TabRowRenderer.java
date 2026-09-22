package com.josuebrenes.toquedeathalert.tab;

import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

/**
 * Draws the TOQUE player list: a framed banner on top, the player rows, and a
 * framed objective at the bottom.
 *
 * <p>Nothing here is centred by hand. The client already centres every line of
 * the header and the footer on its own, so adding leading spaces would centre
 * the text twice and push it off to the right.
 *
 * <p>The frame closes only on the lines written end to end here. Player rows carry
 * no right-hand border: the font is proportional and the only padding a vanilla
 * client understands is a 4 pixel space, so a border after a name could never be
 * made to line up.
 */
public final class TabRowRenderer {
    private static final int FRAME_BARS = 26;
    private static final String SKULL = "☠";
    private static final String TOP = "╔" + "═".repeat(FRAME_BARS) + "╗";
    private static final String BOTTOM = "╚" + "═".repeat(FRAME_BARS) + "╝";

    /** Column positions inside a player row, in pixels. */
    private static final int NAME_COLUMN_PX = 84;
    private static final int HEALTH_COLUMN_PX = 128;

    private static final long TICKS_PER_DAY = 24000L;

    private final SeriesStatsRepository stats;

    public TabRowRenderer(SeriesStatsRepository stats) {
        this.stats = stats;
    }

    public Text row(ServerPlayerEntity player) {
        String name = FontWidth.padTo(player.getGameProfile().getName(), NAME_COLUMN_PX);
        Text health = HeartBar.render(player.getHealth(), player.getMaxHealth());
        String gap = FontWidth.spaces(HEALTH_COLUMN_PX - FontWidth.of(health.getString()));

        return Text.literal(name).formatted(Formatting.WHITE)
                .append(health)
                .append(Text.literal(gap))
                .append(deaths(stats.deathsOf(player.getUuid())));
    }

    public Text header(MinecraftServer server) {
        return Text.empty()
                .append(line(TOP, Formatting.DARK_RED))
                .append(line(SKULL + "  T O Q U E  " + SKULL, Formatting.RED, Formatting.BOLD))
                .append(line("HARDCORE SERIES", Formatting.GRAY))
                .append(line("TRY #" + stats.tryNumber() + "   ·   DAY " + currentDay(server),
                        Formatting.GOLD))
                .append(Text.literal(BOTTOM).formatted(Formatting.DARK_RED));
    }

    public Text footer(MinecraftServer server) {
        String objective = "OBJECTIVE: " + stats.objective().toUpperCase(Locale.ROOT);
        String status = server.getPlayerManager().getCurrentPlayerCount() + " ONLINE"
                + "   ·   SERIE #" + stats.seriesNumber();

        return Text.empty()
                .append(line(TOP, Formatting.DARK_RED))
                .append(line(objective, Formatting.GOLD, Formatting.BOLD))
                .append(line(status, Formatting.DARK_GRAY))
                .append(Text.literal(BOTTOM).formatted(Formatting.DARK_RED));
    }

    /** Current day of the world this Try is running in. */
    private static long currentDay(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld == null ? 1L : (overworld.getTimeOfDay() / TICKS_PER_DAY) + 1L;
    }

    private static Text line(String text, Formatting... styles) {
        return Text.literal(text + "\n").formatted(styles);
    }

    private static Text deaths(int count) {
        MutableText text = Text.literal(SKULL + " ").formatted(Formatting.DARK_RED);
        return text.append(Text.literal(Integer.toString(count))
                .formatted(count == 0 ? Formatting.GREEN : Formatting.RED));
    }
}
