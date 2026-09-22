package com.josuebrenes.toquedeathalert.series;

import java.util.UUID;

/**
 * One player's standing in the current TOQUE series.
 *
 * @param uuid     stable identity; a player may rename, this never changes
 * @param name     last known name, kept only so the JSON file stays readable
 * @param deaths   deaths accumulated across every Try of the current series
 * @param migrated whether the starting value has already been decided, either by
 *                 importing the vanilla statistic or by a series reset. Once true,
 *                 the vanilla statistic is never read for this player again.
 */
public record PlayerDeathRecord(UUID uuid, String name, int deaths, boolean migrated) {

    public static PlayerDeathRecord fresh(UUID uuid, String name) {
        return new PlayerDeathRecord(uuid, name, 0, false);
    }

    public PlayerDeathRecord withName(String newName) {
        return newName.equals(name) ? this : new PlayerDeathRecord(uuid, newName, deaths, migrated);
    }

    public PlayerDeathRecord withDeaths(int newDeaths) {
        return new PlayerDeathRecord(uuid, name, Math.max(0, newDeaths), migrated);
    }

    public PlayerDeathRecord migrated(int startingDeaths) {
        return new PlayerDeathRecord(uuid, name, Math.max(0, startingDeaths), true);
    }

    public PlayerDeathRecord plusOneDeath() {
        return new PlayerDeathRecord(uuid, name, deaths + 1, migrated);
    }

    public String displayName() {
        return name == null || name.isBlank() ? uuid.toString() : name;
    }
}
