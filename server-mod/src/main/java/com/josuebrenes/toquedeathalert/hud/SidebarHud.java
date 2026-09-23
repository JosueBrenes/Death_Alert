package com.josuebrenes.toquedeathalert.hud;

import com.josuebrenes.toquedeathalert.core.Gradient;
import com.josuebrenes.toquedeathalert.role.PlayerRole;
import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The panel down the right hand side of the screen.
 *
 * <p>Sent per player rather than through the world scoreboard. The world
 * scoreboard has one sidebar shared by everybody, and half of what belongs on
 * this panel is personal: coordinates, rank and death count differ for each
 * player. So the objective, the display slot and every line are written straight
 * to one connection, and the world scoreboard is never touched. That also keeps
 * it clear of the datapack objectives already on this server.
 *
 * <p>Scores carry their own display text, so the lines are free text rather than
 * the fake player names this used to need, and the objective asks for a blank
 * number format so the red score column on the right is not drawn at all.
 */
public final class SidebarHud {
    private static final String OBJECTIVE_NAME = "toque_hud";

    /** Lines refresh five times a second; coordinates change as the player walks. */
    private static final int REFRESH_TICKS = 4;

    /**
     * How often the objective itself is re-declared. A client drops its whole
     * scoreboard when it changes dimension, and Hardcore World Reset moves
     * everybody to a freshly generated world, so re-sending it heals the panel
     * without needing to catch every way a world can go away.
     */
    private static final int REINSTALL_TICKS = 100;

    private final SeriesStatsRepository stats;
    private final Map<UUID, List<String>> lastLines = new ConcurrentHashMap<>();
    private ScoreboardObjective objective;
    private int refreshCounter;
    private int reinstallCounter;

    public SidebarHud(SeriesStatsRepository stats) {
        this.stats = stats;
    }

    public void onServerTick(MinecraftServer server) {
        if (++reinstallCounter >= REINSTALL_TICKS) {
            reinstallCounter = 0;
            server.getPlayerManager().getPlayerList().forEach(player -> install(server, player));
        }
        if (++refreshCounter < REFRESH_TICKS) {
            return;
        }
        refreshCounter = 0;
        server.getPlayerManager().getPlayerList().forEach(player -> refresh(server, player));
    }

    /** Declares the objective on this client and puts it in the sidebar slot. */
    public void install(MinecraftServer server, ServerPlayerEntity player) {
        ScoreboardObjective target = objective(server);
        player.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(target, 0));
        player.networkHandler.sendPacket(
                new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, target));
        lastLines.remove(player.getUuid());
    }

    public void forget(UUID uuid) {
        lastLines.remove(uuid);
    }

    /** Re-sends only the lines that changed since the last refresh. */
    private void refresh(MinecraftServer server, ServerPlayerEntity player) {
        List<Text> lines = lines(server, player);
        List<String> rendered = lines.stream().map(Text::getString).toList();
        List<String> previous = lastLines.get(player.getUuid());

        for (int i = 0; i < lines.size(); i++) {
            if (previous != null && i < previous.size() && previous.get(i).equals(rendered.get(i))) {
                continue;
            }
            // The sidebar sorts by score descending, so the first line needs the
            // highest one for the panel to read top to bottom.
            player.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket(
                    holder(i), OBJECTIVE_NAME, lines.size() - i,
                    Optional.of(lines.get(i)), Optional.of(BlankNumberFormat.INSTANCE)));
        }
        lastLines.put(player.getUuid(), rendered);
    }

    private List<Text> lines(MinecraftServer server, ServerPlayerEntity player) {
        PlayerRole role = PlayerRole.fromDeaths(stats.deathsOf(player.getUuid()));
        BlockPos pos = player.getBlockPos();

        List<Text> lines = new ArrayList<>();
        lines.add(entry("Jugador", Text.literal(player.getGameProfile().getName())
                .formatted(Formatting.WHITE)));
        lines.add(entry("Rango", role.badge()));
        lines.add(entry("Muertes", Text.literal("☠ " + stats.deathsOf(player.getUuid()))
                .formatted(Formatting.RED)));
        lines.add(blank());
        lines.add(entry("XYZ", Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ())
                .formatted(Formatting.GRAY)));
        lines.add(entry("Mundo", Text.literal(dimension(player)).formatted(Formatting.GRAY)));
        lines.add(blank());
        lines.add(entry("Try", Text.literal("#" + stats.tryNumber()).formatted(Formatting.GOLD)));
        lines.add(entry("Día", Text.literal(Long.toString(currentDay(server)))
                .formatted(Formatting.YELLOW)));
        lines.add(entry("Objetivo", Text.literal(stats.objective()).formatted(Formatting.GOLD)));
        return lines;
    }

    private static Text entry(String label, Text value) {
        return Text.literal("▪ ").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(label + ": ").formatted(Formatting.GRAY))
                .append(value);
    }

    private static Text blank() {
        return Text.literal("");
    }

    private static String dimension(ServerPlayerEntity player) {
        String path = player.getWorld().getRegistryKey().getValue().getPath();
        return switch (path) {
            case "overworld" -> "Superficie";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path;
        };
    }

    private static long currentDay(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld == null ? 1L : (overworld.getTimeOfDay() / 24000L) + 1L;
    }

    /** One stable holder per row, so a row is updated rather than duplicated. */
    private static String holder(int index) {
        return "toque_line_" + index;
    }

    private ScoreboardObjective objective(MinecraftServer server) {
        if (objective == null) {
            objective = new ScoreboardObjective(server.getScoreboard(), OBJECTIVE_NAME,
                    ScoreboardCriterion.DUMMY,
                    Gradient.apply("☠ TOQUE HARDCORE ☠", Gradient.RED_FROM, Gradient.RED_TO, true),
                    ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE);
        }
        return objective;
    }
}
