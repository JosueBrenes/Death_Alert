package com.josuebrenes.toquedeathalert.tab;

/**
 * Approximate width, in pixels, of a string in Minecraft's default font.
 *
 * <p>The font is proportional, so laying anything out by counting characters is
 * wrong: {@code i} is 2 pixels wide and {@code m} is 6. The server has no access
 * to the client's text renderer, so the ASCII advances are tabulated here.
 *
 * <p>Padding can only be done in whole spaces, which are 4 pixels, so centring is
 * accurate to within 3 pixels. That is invisible for centred text, which is why
 * the player list frame centres its lines instead of trying to close a right-hand
 * border it could not align.
 */
public final class FontWidth {
    /** Advance of every printable ASCII character, including the 1px gap after it. */
    private static final int[] ASCII_ADVANCE = new int[128];

    /** Box drawing, hearts and the skull all come from the uniform font page. */
    private static final int SYMBOL_ADVANCE = 8;

    public static final int SPACE = 4;

    static {
        String widths =
                //  !"#$%&'()*+,-./
                "4256666356556262"
                // 0123456789:;<=>?
                + "6666666666225656"
                // @ABCDEFGHIJKLMNO
                + "7666666666666666"
                // PQRSTUVWXYZ[\]^_
                + "6666666666646466"
                // `abcdefghijklmno
                + "3666665662653666"
                // pqrstuvwxyz{|}~
                + "6666646666665256";
        for (int i = 0; i < widths.length(); i++) {
            ASCII_ADVANCE[32 + i] = widths.charAt(i) - '0';
        }
    }

    private FontWidth() {
    }

    public static int of(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += of(text.charAt(i));
        }
        return width;
    }

    public static int of(char character) {
        if (character < ASCII_ADVANCE.length) {
            return ASCII_ADVANCE[character];
        }
        return SYMBOL_ADVANCE;
    }

    /** Leading spaces that place {@code text} in the middle of a {@code targetPx} line. */
    public static String centre(String text, int targetPx) {
        int slack = targetPx - of(text);
        return slack <= 0 ? "" : " ".repeat(Math.round(slack / 2.0F / SPACE));
    }
}
