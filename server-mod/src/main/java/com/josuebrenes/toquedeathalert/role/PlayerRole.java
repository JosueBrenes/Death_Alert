package com.josuebrenes.toquedeathalert.role;

import net.minecraft.util.Formatting;

/**
 * Automatic TOQUE status derived only from the player's current-series death count.
 * No role is stored separately, so it can never become out of sync with the counter.
 */
public enum PlayerRole {
    INTACTO(0, "INTACTO", Formatting.AQUA),
    SOBREVIVIENTE(1, "SOBREVIVIENTE", Formatting.GREEN),
    MARCADO(2, "MARCADO", Formatting.YELLOW),
    CONDENADO(3, "CONDENADO", Formatting.GOLD),
    DELINCUENTE(4, "DELINCUENTE", Formatting.RED),
    ESPECTRO(5, "ESPECTRO", Formatting.LIGHT_PURPLE),
    HIJO_DE_LA_MUERTE(6, "HIJO DE LA MUERTE", Formatting.DARK_PURPLE);

    private final int minimumDeaths;
    private final String label;
    private final Formatting formatting;

    PlayerRole(int minimumDeaths, String label, Formatting formatting) {
        this.minimumDeaths = minimumDeaths;
        this.label = label;
        this.formatting = formatting;
    }

    public String label() {
        return label;
    }

    public Formatting formatting() {
        return formatting;
    }

    public static PlayerRole fromDeaths(int deaths) {
        int safeDeaths = Math.max(0, deaths);
        PlayerRole selected = INTACTO;
        for (PlayerRole role : values()) {
            if (safeDeaths >= role.minimumDeaths) {
                selected = role;
            } else {
                break;
            }
        }
        return selected;
    }
}
