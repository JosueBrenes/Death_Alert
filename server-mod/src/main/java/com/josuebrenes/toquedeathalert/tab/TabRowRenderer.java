package com.josuebrenes.toquedeathalert.tab;

import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Draws the TOQUE player list: a framed banner on top, the player rows, and a
 * framed objective at the bottom.
 *
 * <p>The frame closes only on the lines this class writes end to end. Player rows
 * carry no right-hand border, because the default font is proportional and the
 * only padding a vanilla client understands is a 4 pixel space, so a border after
 * a name could never line up. Everything inside the frame is centred instead,
 * where being off by a pixel or two cannot be seen.
 */
public final class TabRowRenderer {
    /** Horizontal bars in the frame, and the full width it spans including corners. */
    private static final int FRAME_BARS = 32;
    private static final int FRAME_PX = (FRAME_BARS + 2) * FontWidth.of('═');

    private static final String SKULL = "☠";
    private static final String TOP = "╔" + "═".repeat(FRAME_BARS) + "╗";
    private static final String BOTTOM = "╚" + "═".repeat(FRAME_BARS) + "╝";

    /** Names are padded to this many characters so the rows read as columns. */
    private static final int NAME_WIDTH = 16;
    private static final long TICKS_PER_DAY = 24000L;

    private final SeriesStatsRepository stats;

    public TabRowRenderer(SeriesStatsRepository stats) {
        this.stats = stats;
    }

    public Text row(ServerPlayerEntity player) {
        return Text.literal("  " + pad(player.getGameProfile().getName())).formatted(Formatting.WHITE)
                .append(Text.literal(" ").formatted(Formatting.DARK_GRAY))
                .append(HeartBar.render(player.getHealth(), player.getMaxHealth()))
                .append(Text.literal("  ").formatted(Formatting.DARK_GRAY))
                .append(deaths(stats.deathsOf(player.getUuid())));
    }

    public Text header(MinecraftServer server) {
        String title = SKULL + "  T O Q U E  " + SKULL;
        String subtitle = "HARDCORE SERIES";
        String progress = "TRY #" + stats.tryNumber() + "   ·   DAY " + currentDay(server);

        return Text.empty()
                .append(frameLine(TOP))
                .append(centred(title, Formatting.RED, Formatting.BOLD))
                .append(centred(subtitle, Formatting.GRAY))
                .append(Text.literal("\n"))
                .append(centred(progress, Formatting.GOLD))
                .append(frameLine(BOTTOM))
                .append(Text.literal("\n"));
    }

    public Text footer(MinecraftServer server) {
        String objective = "OBJECTIVE: " + stats.objective().toUpperCase(java.util.Locale.ROOT);
        String online = server.getPlayerManager().getCurrentPlayerCount() + " ONLINE"
                + "   ·   SERIE #" + stats.seriesNumber();

        return Text.literal("\n")
                .append(frameLine(TOP))
                .append(centred(objective, Formatting.GOLD, Formatting.BOLD))
                .append(centred(online, Formatting.DARK_GRAY))
                .append(frameLine(BOTTOM));
    }

    /** Current day of the world this Try is running in. */
    private static long currentDay(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld == null ? 1L : (overworld.getTimeOfDay() / TICKS_PER_DAY) + 1L;
    }

    private static Text frameLine(String bar) {
        return Text.literal(bar + "\n").formatted(Formatting.DARK_RED);
    }

    private static Text centred(String text, Formatting... styles) {
        return Text.literal(FontWidth.centre(text, FRAME_PX) + text + "\n").formatted(styles);
    }

    private static Text deaths(int count) {
        MutableText text = Text.literal(SKULL + " ").formatted(Formatting.DARK_RED);
        return text.append(Text.literal(Integer.toString(count))
                .formatted(count == 0 ? Formatting.GREEN : Formatting.RED));
    }

    private static String pad(String name) {
        return name.length() >= NAME_WIDTH ? name : name + " ".repeat(NAME_WIDTH - name.length());
    }
}
