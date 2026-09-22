package com.josuebrenes.toquedeathalert.tab;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

/**
 * Renders a health bar for the player list.
 *
 * <p>The ten slots are scaled to the player's actual maximum health, so attribute
 * or effect changes still read correctly instead of pretending everyone has 20 HP.
 * The default font has no half-heart glyph, so a half is shown as a gold heart and
 * the exact numbers are appended, which removes any ambiguity.
 */
public final class HeartBar {
    private static final int SLOTS = 10;
    private static final String FULL = "♥";
    private static final String EMPTY = "♡";

    private HeartBar() {
    }

    public static Text render(float health, float maxHealth) {
        float safeMax = maxHealth <= 0.0F ? 20.0F : maxHealth;
        float current = Math.max(0.0F, Math.min(health, safeMax));

        int halves = Math.round((current / safeMax) * (SLOTS * 2));
        if (halves == 0 && current > 0.0F) {
            halves = 1; // never show a living player as completely empty
        }
        int full = halves / 2;
        boolean half = (halves % 2) == 1;
        int empty = SLOTS - full - (half ? 1 : 0);

        MutableText bar = Text.empty();
        if (full > 0) {
            bar.append(Text.literal(FULL.repeat(full)).formatted(Formatting.RED));
        }
        if (half) {
            bar.append(Text.literal(FULL).formatted(Formatting.GOLD));
        }
        if (empty > 0) {
            bar.append(Text.literal(EMPTY.repeat(empty)).formatted(Formatting.DARK_GRAY));
        }
        bar.append(Text.literal(" " + number(current) + "/" + number(safeMax))
                .formatted(Formatting.GRAY));
        return bar;
    }

    private static String number(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        return rounded == Math.floor(rounded)
                ? Integer.toString((int) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
    }
}
