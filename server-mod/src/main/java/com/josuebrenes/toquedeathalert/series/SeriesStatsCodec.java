package com.josuebrenes.toquedeathalert.series;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.UUID;

/**
 * Reads and writes the {@code deaths.json} document.
 *
 * <p>Format 2 adds {@code vanillaImport} and the per-player {@code migrated} flag.
 * A format 1 document still loads: no flags means nobody has been migrated yet and
 * the import is open, which is exactly what an upgrade from the older mod needs.
 */
final class SeriesStatsCodec {
    static final int FORMAT_VERSION = 2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SeriesStatsCodec() {
    }

    static String encode(SeriesStats stats) {
        JsonObject players = new JsonObject();
        for (PlayerDeathRecord record : stats.all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", record.name() == null ? "" : record.name());
            entry.addProperty("deaths", record.deaths());
            entry.addProperty("migrated", record.migrated());
            players.add(record.uuid().toString(), entry);
        }

        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", FORMAT_VERSION);
        root.addProperty("series", stats.seriesNumber());
        root.addProperty("vanillaImport", stats.isVanillaImportOpen());
        root.add("players", players);
        return GSON.toJson(root);
    }

    /** Fills {@code target} from {@code raw}, skipping only the entries that are broken. */
    static void decodeInto(String raw, SeriesStats target) {
        target.clear();

        JsonElement parsed = JsonParser.parseString(raw);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("deaths.json root is not an object");
        }
        JsonObject root = parsed.getAsJsonObject();

        if (root.has("series") && root.get("series").isJsonPrimitive()) {
            target.setSeriesNumber(root.get("series").getAsInt());
        }
        if (root.has("vanillaImport") && root.get("vanillaImport").isJsonPrimitive()) {
            target.setVanillaImportOpen(root.get("vanillaImport").getAsBoolean());
        }
        if (!root.has("players") || !root.get("players").isJsonObject()) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("players").entrySet()) {
            try {
                UUID uuid = UUID.fromString(entry.getKey());
                JsonObject value = entry.getValue().getAsJsonObject();
                String name = value.has("name") ? value.get("name").getAsString() : "";
                int deaths = Math.max(0, value.get("deaths").getAsInt());
                boolean migrated = value.has("migrated") && value.get("migrated").getAsBoolean();
                target.put(new PlayerDeathRecord(uuid, name, deaths, migrated));
            } catch (RuntimeException malformedEntry) {
                // One bad row must not cost us the whole series.
            }
        }
    }
}
