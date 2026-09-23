package com.josuebrenes.toquedeathalert.death;

import com.josuebrenes.toquedeathalert.core.ToqueLog;
import com.josuebrenes.toquedeathalert.core.ToqueRuntime;
import com.josuebrenes.toquedeathalert.series.PlayerDeathRecord;
import com.josuebrenes.toquedeathalert.series.SeriesStatsRepository;
import com.josuebrenes.toquedeathalert.tab.TabListService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The single place a death is detected and counted.
 *
 * <p>Hardcore World Reset intercepts and cancels the normal death flow, so
 * {@code AFTER_DEATH} may never fire. {@code ALLOW_DEATH} runs when fatal damage
 * is detected, before that interception, which is why this mod hooks it.
 */
public final class DeathListener {
    private final ToqueRuntime runtime;
    private final DeathAnnouncer announcer;

    public DeathListener(ToqueRuntime runtime, DeathAnnouncer announcer) {
        this.runtime = runtime;
        this.announcer = announcer;
    }

    public void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                onFatalDamage(player, damageSource);
            }
            // Never cancel the death. Hardcore World Reset still handles it.
            return true;
        });
    }

    private void onFatalDamage(ServerPlayerEntity player, DamageSource damageSource) {
        MinecraftServer server = player.getServer();
        if (server == null || isSavedByTotem(player, damageSource)) {
            return;
        }

        ToqueLog.info("Death detected: {}", player.getName().getString());

        SeriesStatsRepository stats = runtime.stats();
        if (stats != null) {
            PlayerDeathRecord record =
                    stats.addDeath(player.getUuid(), player.getGameProfile().getName());
            ToqueLog.info("Series deaths for {}: {}", record.displayName(), record.deaths());
        }

        TabListService tabList = runtime.tabList();
        if (tabList != null) {
            tabList.invalidate(player.getUuid());
        }

        announcer.announce(server, player);
    }

    /**
     * ALLOW_DEATH fires before totems are checked, so this mirrors vanilla's
     * tryUseTotem: a held totem saves the player unless the damage bypasses
     * invulnerability (for example /kill or the void). A player saved this way
     * is neither announced nor counted.
     */
    private static boolean isSavedByTotem(ServerPlayerEntity player, DamageSource damageSource) {
        if (damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }
}
