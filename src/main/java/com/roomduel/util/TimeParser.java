package com.roomduel.util;

import java.util.Locale;

public final class TimeParser {

    private TimeParser() {}

    /** Parses "5", "5s", "0.5s" ... into seconds. Returns null if invalid. */
    public static Double parseSeconds(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith("s")) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            double v = Double.parseDouble(s);
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0 || v > 600) return null;
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String format(double seconds) {
        if (seconds % 1 == 0) {
            return String.valueOf((long) seconds);
        }
        return String.valueOf(seconds);
    }
}
