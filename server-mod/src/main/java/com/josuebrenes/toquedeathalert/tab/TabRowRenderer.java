package com.josuebrenes.toquedeathalert.tab;

import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Turns a player into the line shown in the TOQUE player list, plus header and footer. */
public final class TabRowRenderer {
    /** Names are padded to this many characters so the columns roughly line up. */
    private static final int NAME_WIDTH = 16;
    private static final String SKULL = "☠";

    private final SeriesStatsRepository stats;

    public TabRowRenderer(SeriesStatsRepository stats) {
        this.stats = stats;
    }

    public Text row(ServerPlayerEntity player) {
        return Text.literal(pad(player.getGameProfile().getName())).formatted(Formatting.WHITE)
                .append(Text.literal(" ").formatted(Formatting.DARK_GRAY))
                .append(HeartBar.render(player.getHealth(), player.getMaxHealth()))
                .append(Text.literal("  ").formatted(Formatting.DARK_GRAY))
                .append(deaths(stats.deathsOf(player.getUuid())));
    }

    public Text header() {
        return Text.empty()
                .append(Text.literal("\n" + SKULL + " ").formatted(Formatting.DARK_RED, Formatting.BOLD))
                .append(Text.literal("TOQUE HARDCORE").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(" " + SKULL + "\n").formatted(Formatting.DARK_RED, Formatting.BOLD));
    }

    public Text footer(MinecraftServer server) {
        return Text.empty()
                .append(Text.literal("\nSerie ").formatted(Formatting.GRAY))
                .append(Text.literal("#" + stats.seriesNumber()).formatted(Formatting.RED))
                .append(Text.literal("  ·  ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(server.getPlayerManager().getCurrentPlayerCount() + " en linea")
                        .formatted(Formatting.GRAY))
                .append(Text.literal("\n"));
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
