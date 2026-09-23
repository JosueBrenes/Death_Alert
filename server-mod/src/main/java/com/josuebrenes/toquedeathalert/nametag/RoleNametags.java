package com.josuebrenes.toquedeathalert.nametag;

import com.josuebrenes.toquedeathalert.role.PlayerRole;
import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts the rank in front of the name floating over a player's head.
 *
 * <p>The floating name is drawn by the client from the player's scoreboard team
 * prefix, so one team per rank is enough: the teams are sent straight down the
 * connection and never written to the world scoreboard, which keeps the world
 * data and any datapack teams untouched and means Hardcore World Reset has
 * nothing of ours to delete.
 *
 * <p>The player list is unaffected. It prefers the display name the tab renderer
 * supplies, and only falls back to team decoration when there is none.
 */
public final class RoleNametags {
    /** Prefixed and numbered so they cannot collide with a datapack's own teams. */
    private static final String TEAM_PREFIX = "toque_rank_";

    private final SeriesStatsRepository stats;
    private final Map<PlayerRole, Team> teams = new EnumMap<>(PlayerRole.class);
    private final Map<UUID, PlayerRole> assigned = new ConcurrentHashMap<>();

    public RoleNametags(SeriesStatsRepository stats) {
        this.stats = stats;
    }

    /**
     * Defines every rank team on this player's client, then places everyone.
     *
     * <p>Also needed after a world change: the client throws its whole scoreboard
     * away, teams included, so the names above heads would lose their rank until
     * the teams are declared again.
     */
    public void install(MinecraftServer server, ServerPlayerEntity player) {
        for (Team team : teamsFor(server).values()) {
            player.networkHandler.sendPacket(TeamS2CPacket.updateRemovedTeam(team));
            player.networkHandler.sendPacket(TeamS2CPacket.updateTeam(team, true));
        }
        assigned.clear();
        refresh(server);
    }

    public void forget(UUID uuid) {
        assigned.remove(uuid);
    }

    /** Moves anyone whose rank has changed into the matching team. */
    public void refresh(MinecraftServer server) {
        Map<PlayerRole, Team> byRole = teamsFor(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerRole role = PlayerRole.fromDeaths(stats.deathsOf(player.getUuid()));
            if (role == assigned.get(player.getUuid())) {
                continue;
            }
            assigned.put(player.getUuid(), role);

            TeamS2CPacket packet = TeamS2CPacket.changePlayerTeam(byRole.get(role),
                    player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD);
            for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
                viewer.networkHandler.sendPacket(packet);
            }
        }
    }

    /**
     * The rank teams, built once against the server scoreboard but never added to
     * it: a Team needs a scoreboard to exist, not to be registered in one.
     */
    private Map<PlayerRole, Team> teamsFor(MinecraftServer server) {
        if (!teams.isEmpty()) {
            return teams;
        }
        for (PlayerRole role : PlayerRole.values()) {
            Team team = new Team(server.getScoreboard(), TEAM_PREFIX + role.ordinal());
            team.setDisplayName(Text.literal(role.label()));
            team.setPrefix(role.tag().append(Text.literal(" ")));
            team.setColor(Formatting.WHITE);
            teams.put(role, team);
        }
        return teams;
    }
}
