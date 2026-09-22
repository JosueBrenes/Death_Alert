package com.josuebrenes.toquedeathalert.series;

import com.josuebrenes.toquedeathalert.core.ToqueLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for the series stats.
 *
 * <p>The document lives in {@code <server root>/config/toque-death-alert/deaths.json},
 * deliberately outside the world directory: Hardcore World Reset deletes and rebuilds
 * the world on every Try, and these counters must survive that untouched.
 */
public final class SeriesStatsRepository {
    private static final String DIRECTORY = "config/toque-death-alert";
    private static final String FILE_NAME = "deaths.json";

    private final Path file;
    private final SeriesStats stats = new SeriesStats();

    public SeriesStatsRepository(Path serverRoot) {
        this.file = serverRoot.resolve(DIRECTORY).resolve(FILE_NAME);
    }

    public Path file() {
        return file;
    }

    public int seriesNumber() {
        return stats.seriesNumber();
    }

    public boolean isVanillaImportOpen() {
        return stats.isVanillaImportOpen();
    }

    public int tryNumber() {
        return stats.tryNumber();
    }

    public String objective() {
        return stats.objective();
    }

    public void setObjective(String objective) {
        stats.setObjective(objective);
        save();
    }

    public void setTryNumber(int tryNumber) {
        stats.setTryNumber(tryNumber);
        save();
    }

    /** Bumps the Try counter when the world has been rebuilt with a new seed. */
    public boolean advanceTryIfWorldChanged(long seed) {
        boolean advanced = stats.advanceTryIfWorldChanged(seed);
        save();
        return advanced;
    }

    public int deathsOf(UUID uuid) {
        return stats.deathsOf(uuid);
    }

    /** Every death by every player so far in this series. */
    public int totalDeaths() {
        return stats.all().stream().mapToInt(PlayerDeathRecord::deaths).sum();
    }

    public Optional<PlayerDeathRecord> find(UUID uuid) {
        return stats.find(uuid);
    }

    public Collection<PlayerDeathRecord> all() {
        return stats.all();
    }

    /** Registers a player without deciding anything about their starting value. */
    public PlayerDeathRecord touch(UUID uuid, String name) {
        PlayerDeathRecord record = stats.getOrCreate(uuid, name);
        save();
        return record;
    }

    /** Closes the migration for a player, fixing their starting value. */
    public PlayerDeathRecord markMigrated(UUID uuid, String name, int startingDeaths) {
        PlayerDeathRecord record = stats.getOrCreate(uuid, name).migrated(startingDeaths);
        stats.put(record);
        save();
        return record;
    }

    /** Adds exactly one death to one player. Nobody else is affected. */
    public PlayerDeathRecord addDeath(UUID uuid, String name) {
        PlayerDeathRecord record = stats.addDeath(uuid, name);
        save();
        return record;
    }

    /** Admin override for a single player; also closes their migration. */
    public PlayerDeathRecord setDeaths(UUID uuid, String name, int deaths) {
        return markMigrated(uuid, name, deaths);
    }

    /** Starts a new series: all counters to 0, vanilla import closed permanently. */
    public void startNewSeries() {
        stats.startNewSeries();
        save();
    }

    /** Reads the document, tolerating a missing, empty or corrupted file. */
    public void load() {
        stats.clear();
        if (!Files.isRegularFile(file)) {
            ToqueLog.info("No stats file yet at {}; starting a fresh series.", file);
            return;
        }
        try {
            SeriesStatsCodec.decodeInto(Files.readString(file, StandardCharsets.UTF_8), stats);
        } catch (IOException | RuntimeException e) {
            ToqueLog.warn("Could not read {} ({}). Starting from empty stats.", file, e.toString());
            stats.clear();
        }
    }

    /** Writes through a temporary file so a crash mid-write cannot corrupt the data. */
    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, SeriesStatsCodec.encode(stats), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ToqueLog.warn("Could not write {} ({}).", file, e.toString());
        }
    }
}
