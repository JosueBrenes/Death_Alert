package com.josuebrenes.toqueserverbanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Decides which entry in the multiplayer list is the TOQUE server.
 *
 * <p>Matching is done on the address, never on the display name: the name is
 * whatever the player typed when they added the server and they can rename it at
 * any time, while the address is what actually identifies the server.
 *
 * <p>The addresses live in {@code config/toque-server-banner.json} so they can be
 * changed without rebuilding the mod.
 */
public final class ToqueServerBannerConfig {
    private static final String FILE_NAME = "toque-server-banner.json";
    private static final String DEFAULT_ADDRESS = "ivan-fda.tun.ply.gg";

    private static volatile Set<String> addresses = Set.of(DEFAULT_ADDRESS);

    private ToqueServerBannerConfig() {
    }

    public static Set<String> addresses() {
        return addresses;
    }

    /** Reads the config, writing a default one the first time the mod runs. */
    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            writeDefault(file);
            return;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            JsonArray array = parsed.getAsJsonObject().getAsJsonArray("addresses");

            Set<String> loaded = new LinkedHashSet<>();
            for (JsonElement element : array) {
                String address = normalise(element.getAsString());
                if (!address.isEmpty()) {
                    loaded.add(address);
                }
            }
            if (!loaded.isEmpty()) {
                addresses = Set.copyOf(loaded);
            }
            log("Watching " + addresses);
        } catch (IOException | RuntimeException e) {
            log("Could not read " + file + " (" + e + "). Using the default address.");
        }
    }

    /** True when this list entry points at a TOQUE server. */
    public static boolean matches(ServerInfo server) {
        if (server == null) {
            return false;
        }
        String address = normalise(server.address);
        if (address.isEmpty()) {
            return false;
        }
        for (String known : addresses) {
            if (address.equals(known) || address.endsWith("." + known)) {
                return true;
            }
        }
        return false;
    }

    /** Lower cases, trims, and drops the port so "HOST:25565" matches "host". */
    private static String normalise(String address) {
        if (address == null) {
            return "";
        }
        String trimmed = address.trim().toLowerCase(Locale.ROOT);
        int port = trimmed.lastIndexOf(':');
        return port > 0 ? trimmed.substring(0, port) : trimmed;
    }

    private static void writeDefault(Path file) {
        JsonArray array = new JsonArray();
        array.add(DEFAULT_ADDRESS);
        JsonObject root = new JsonObject();
        root.add("addresses", array);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
            log("Wrote a default config to " + file);
        } catch (IOException e) {
            log("Could not write " + file + " (" + e + ").");
        }
    }

    private static void log(String message) {
        System.out.println("[TOQUE] " + message);
    }
}
