package com.josuebrenes.toquedeathalert.tab;

import com.josuebrenes.toquedeathalert.core.FontWidth;
import com.josuebrenes.toquedeathalert.core.Gradient;
import com.josuebrenes.toquedeathalert.role.PlayerRole;
import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

/**
 * Draws the TOQUE player list: a two line banner, the player rows, and the
 * objective underneath.
 *
 * <p>The role is derived automatically from the current TOQUE death counter.
 * Nothing is stored separately, so the role always follows the death count.
 */
public final class TabRowRenderer {
    private static final String SKULL = "☠";
    private static final String SEPARATOR = "   ·   ";

    /** Column positions inside a player row, in pixels. */
    private static final int NAME_COLUMN_PX = 84;
    private static final int ROLE_COLUMN_PX = 176;
    private static final int HEALTH_COLUMN_PX = 108;

    private static final long TICKS_PER_DAY = 24000L;

    private final SeriesStatsRepository stats;

    public TabRowRenderer(SeriesStatsRepository stats) {
        this.stats = stats;
    }

    public Text row(ServerPlayerEntity player) {
        int deaths = stats.deathsOf(player.getUuid());

        String name = FontWidth.padTo(player.getGameProfile().getName(), NAME_COLUMN_PX);

        PlayerRole role = PlayerRole.fromDeaths(deaths);
        Text roleText = Text.literal(role.label()).formatted(role.formatting());

        String roleGap = FontWidth.spaces(
                Math.max(4, ROLE_COLUMN_PX - FontWidth.of(roleText.getString()))
        );

        Text health = HeartBar.render(player.getHealth(), player.getMaxHealth());
        String healthGap = FontWidth.spaces(
                Math.max(4, HEALTH_COLUMN_PX - FontWidth.of(health.getString()))
        );

        return Text.literal(name).formatted(Formatting.WHITE)
                .append(roleText)
                .append(Text.literal(roleGap))
                .append(health)
                .append(Text.literal(healthGap))
                .append(deaths(stats.deathsOf(player.getUuid())));
    }

    /** The name, then where the series stands, in the same shape as the server list. */
    public Text header(MinecraftServer server) {
        MutableText progress = Text.literal("TRY #" + stats.tryNumber()).formatted(Formatting.GOLD);

        long day = currentDay(server);
        if (day > 0L) {
            progress.append(separator())
                    .append(Text.literal("DÍA " + day).formatted(Formatting.YELLOW));
        }
        progress.append(separator()).append(deaths(stats.totalDeaths()));

        return Text.literal("\n")
                .append(Gradient.apply(SKULL + " TOQUE HARDCORE " + SKULL,
                        Gradient.RED_FROM, Gradient.RED_TO, true))
                .append(Text.literal("\n"))
                .append(progress)
                .append(Text.literal("\n"));
    }

    public Text footer(MinecraftServer server) {
        return Text.literal("\n")
                .append(Gradient.apply(stats.objective().toUpperCase(Locale.ROOT),
                        Gradient.GOLD_FROM, Gradient.GOLD_TO, true))
                .append(Text.literal("\n"));
    }

    /** Current day of the world this Try is running in. */
    private static long currentDay(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld == null ? -1L : (overworld.getTimeOfDay() / TICKS_PER_DAY) + 1L;
    }

    private static Text separator() {
        return Text.literal(SEPARATOR).formatted(Formatting.DARK_GRAY);
    }

    private static Text deaths(int count) {
        MutableText text = Text.literal(SKULL + " ").formatted(Formatting.DARK_RED);
        return text.append(Text.literal(Integer.toString(count))
                .formatted(count == 0 ? Formatting.GREEN : Formatting.RED));
    }
}
