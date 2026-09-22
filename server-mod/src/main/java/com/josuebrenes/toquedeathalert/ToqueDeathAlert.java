package com.josuebrenes.toquedeathalert;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ToqueDeathAlert implements ModInitializer {
    private static final SoundEvent DEATH_SOUND =
            SoundEvent.of(Identifier.of("toque", "death"));

    @Override
    public void onInitialize() {
        System.out.println("[TOQUE] Death Alert loaded.");

        // Hardcore World Reset intercepts/cancels the normal death flow,
        // so AFTER_DEATH may never fire. ALLOW_DEATH runs when fatal damage
        // is detected, before HWR's death interception.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }

            MinecraftServer server = player.getServer();
            if (server == null) {
                return true;
            }

            // ALLOW_DEATH fires before totems are checked. Mirror vanilla's
            // tryUseTotem: a held totem saves the player unless the damage
            // bypasses invulnerability (e.g. /kill, the void).
            if (!damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                    || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))) {
                return true;
            }

            System.out.println("[TOQUE] Death detected: " + player.getName().getString());
            announceDeath(server, player);

            // Do not cancel the death. Hardcore World Reset still handles it.
            return true;
        });
    }

    private static void announceDeath(MinecraftServer server, ServerPlayerEntity deadPlayer) {
        Text title = Text.literal("☠ ")
                .formatted(Formatting.DARK_RED, Formatting.BOLD)
                .append(deadPlayer.getName().copy().formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(" HA MUERTO ☠").formatted(Formatting.DARK_RED, Formatting.BOLD));

        Text subtitle = Text.literal("LA RUN HA TERMINADO")
                .formatted(Formatting.RED, Formatting.BOLD);

        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            viewer.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 80, 10));
            viewer.networkHandler.sendPacket(new TitleS2CPacket(title));
            viewer.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            viewer.playSoundToPlayer(DEATH_SOUND, SoundCategory.MASTER, 10.0F, 1.0F);
        }
    }
}
