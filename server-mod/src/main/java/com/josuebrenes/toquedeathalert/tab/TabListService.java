package com.josuebrenes.toquedeathalert.tab;

import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the player list in sync over plain vanilla protocol.
 *
 * <p>Only two packets are involved, both of which every vanilla client already
 * understands, so nobody has to install anything: the player-list header/footer,
 * and a display-name update for the rows that actually changed.
 */
public final class TabListService {
    /** Refresh interval in server ticks; 4 ticks is five updates per second. */
    private static final int REFRESH_TICKS = 4;

    private final TabRowRenderer renderer;
    private final Map<UUID, String> lastSent = new ConcurrentHashMap<>();
    private int tickCounter;

    public TabListService(TabRowRenderer renderer) {
        this.renderer = renderer;
    }

    /** The text the mixin hands to the client for this player's row. */
    public Text rowFor(ServerPlayerEntity player) {
        return renderer.row(player);
    }

    public void onServerTick(MinecraftServer server) {
        if (++tickCounter < REFRESH_TICKS) {
            return;
        }
        tickCounter = 0;
        broadcastChangedRows(server);
    }

    private void broadcastChangedRows(MinecraftServer server) {
        List<ServerPlayerEntity> online = server.getPlayerManager().getPlayerList();
        if (online.isEmpty()) {
            return;
        }

        List<ServerPlayerEntity> changed = new ArrayList<>();
        for (ServerPlayerEntity player : online) {
            String rendered = renderer.row(player).getString();
            if (!rendered.equals(lastSent.get(player.getUuid()))) {
                lastSent.put(player.getUuid(), rendered);
                changed.add(player);
            }
        }
        if (changed.isEmpty()) {
            return;
        }

        PlayerListS2CPacket packet = new PlayerListS2CPacket(
                EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), changed);
        for (ServerPlayerEntity viewer : online) {
            viewer.networkHandler.sendPacket(packet);
        }
    }

    /** Forces every row to be resent on the next tick. */
    public void invalidateAll() {
        lastSent.clear();
    }

    public void invalidate(UUID uuid) {
        lastSent.remove(uuid);
    }

    public void sendHeaderAndFooter(MinecraftServer server) {
        PlayerListHeaderS2CPacket packet =
                new PlayerListHeaderS2CPacket(renderer.header(), renderer.footer(server));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(packet);
        }
    }
}
