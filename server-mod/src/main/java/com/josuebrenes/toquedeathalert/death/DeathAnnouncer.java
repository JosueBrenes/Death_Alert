package com.josuebrenes.toquedeathalert.death;

import com.josuebrenes.toquedeathalert.ToqueDeathAlert;
import com.josuebrenes.toquedeathalert.role.PlayerRole;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/** Shows the TOQUE death title to everyone and plays the series sound. */
public final class DeathAnnouncer {
    private static final SoundEvent DEATH_SOUND = SoundEvent.of(Identifier.of("toque", "death"));
    private static final String SKULL = "☠";

    public void announce(MinecraftServer server, ServerPlayerEntity deadPlayer) {
        int deaths = ToqueDeathAlert.runtime().stats() == null
                ? 0
                : ToqueDeathAlert.runtime().stats().deathsOf(deadPlayer.getUuid());
        PlayerRole role = PlayerRole.fromDeaths(deaths);

        Text title = Text.literal(SKULL + " ")
                .formatted(Formatting.DARK_RED, Formatting.BOLD)
                .append(deadPlayer.getName().copy().formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(" HA MUERTO " + SKULL).formatted(Formatting.DARK_RED, Formatting.BOLD));

        Text subtitle = Text.literal(role.label() + "  •  LA RUN HA TERMINADO")
                .formatted(role.formatting(), Formatting.BOLD);

        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            viewer.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 80, 10));
            viewer.networkHandler.sendPacket(new TitleS2CPacket(title));
            viewer.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            viewer.playSoundToPlayer(DEATH_SOUND, SoundCategory.MASTER, 10.0F, 1.0F);
        }
    }
}
