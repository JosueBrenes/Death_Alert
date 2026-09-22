package com.josuebrenes.toquedeathalert.series;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state of a TOQUE series: who has died how often, and whether the
 * one-off import from the vanilla statistics is still open.
 *
 * <p>Pure state, no I/O. {@link SeriesStatsRepository} owns persistence.
 */
public final class SeriesStats {
    private final Map<UUID, PlayerDeathRecord> records = new ConcurrentHashMap<>();
    private volatile int seriesNumber = 1;
    private volatile boolean vanillaImportOpen = true;

    public int seriesNumber() {
        return seriesNumber;
    }

    public void setSeriesNumber(int seriesNumber) {
        this.seriesNumber = Math.max(1, seriesNumber);
    }

    /** False once a series has been reset: TOQUE is then the only source of truth. */
    public boolean isVanillaImportOpen() {
        return vanillaImportOpen;
    }

    public void setVanillaImportOpen(boolean open) {
        this.vanillaImportOpen = open;
    }

    public int deathsOf(UUID uuid) {
        PlayerDeathRecord record = records.get(uuid);
        return record == null ? 0 : record.deaths();
    }

    public Optional<PlayerDeathRecord> find(UUID uuid) {
        return Optional.ofNullable(records.get(uuid));
    }

    public Collection<PlayerDeathRecord> all() {
        return List.copyOf(records.values());
    }

    public void put(PlayerDeathRecord record) {
        records.put(record.uuid(), record);
    }

    public PlayerDeathRecord getOrCreate(UUID uuid, String name) {
        return records.compute(uuid, (key, existing) ->
                existing == null ? PlayerDeathRecord.fresh(key, name) : existing.withName(name));
    }

    public PlayerDeathRecord addDeath(UUID uuid, String name) {
        return records.compute(uuid, (key, existing) -> existing == null
                ? new PlayerDeathRecord(key, name, 1, true)
                : existing.withName(name).plusOneDeath());
    }

    /** Everyone back to zero, and the vanilla import closed for good. */
    public void startNewSeries() {
        records.replaceAll((uuid, record) -> record.migrated(0));
        vanillaImportOpen = false;
        seriesNumber++;
    }

    public void clear() {
        records.clear();
        seriesNumber = 1;
        vanillaImportOpen = true;
    }
}
