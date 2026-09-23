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
    public static final String DEFAULT_OBJECTIVE = "KILL THE DRAGON";

    private final Map<UUID, PlayerDeathRecord> records = new ConcurrentHashMap<>();
    private volatile int seriesNumber = 1;
    private volatile boolean vanillaImportOpen = true;
    private volatile int tryNumber = 1;
    private volatile long lastSeed;
    private volatile boolean seedKnown;
    private volatile String objective = DEFAULT_OBJECTIVE;

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

    public int tryNumber() {
        return tryNumber;
    }

    public void setTryNumber(int tryNumber) {
        this.tryNumber = Math.max(1, tryNumber);
    }

    public String objective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective == null || objective.isBlank() ? DEFAULT_OBJECTIVE : objective;
    }

    public long lastSeed() {
        return lastSeed;
    }

    public boolean isSeedKnown() {
        return seedKnown;
    }

    public void rememberSeed(long seed) {
        this.lastSeed = seed;
        this.seedKnown = true;
    }

    /**
     * A Try is one world. Hardcore World Reset rebuilds the world with a fresh
     * seed, so a seed we have not seen before means the next Try has started.
     * The first seed ever seen only establishes the baseline.
     *
     * @return true when the Try counter moved
     */
    public boolean advanceTryIfWorldChanged(long seed) {
        if (!seedKnown) {
            rememberSeed(seed);
            return false;
        }
        if (seed == lastSeed) {
            return false;
        }
        rememberSeed(seed);
        tryNumber++;
        return true;
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
        tryNumber = 1;
    }

    public void clear() {
        records.clear();
        seriesNumber = 1;
        vanillaImportOpen = true;
        tryNumber = 1;
        lastSeed = 0L;
        seedKnown = false;
        objective = DEFAULT_OBJECTIVE;
    }
}
