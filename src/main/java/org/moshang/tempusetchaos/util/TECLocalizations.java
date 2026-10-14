package org.moshang.tempusetchaos.util;

import org.moshang.tempusetchaos.annotation.Localization;
import org.moshang.tempusetchaos.annotation.Translation;

public class TECLocalizations {
    private static final String[] SUFFIXES = {"", "k", "M", "G", "T", "P", "E", "Z"};

    @Localization(translations = {@Translation(text = "%s/T")})
    public static final String CHRONON_COST = "info.tempusetchaos.chronon_cost";
    @Localization(translations = {@Translation(text = "%s / %s")})
    public static final String CHRONON  = "info.tempusetchaos.chronon";

    public static String formatCompact(double number, boolean milli, String unit) {
        if (Double.isNaN(number)) return "NaN" + unit;
        if (Double.isInfinite(number)) return (number > 0 ? "∞" : "-∞") + unit;

        StringBuilder sb = new StringBuilder();

        if (number < 0) {
            sb.append('-');
            number = -number;
        }

        if (milli && number >= 1000) {
            number /= 1000;
            milli = false;
        }

        int exp = 0;
        if (number >= 1000) {
            exp = Math.min((int) (Math.log10(number) / 3), 7);
            number /= Math.pow(1000, exp);
        }

        if (number < 10) {
            sb.append(String.format("%.2f", number));
        } else if (number < 100) {
            sb.append(String.format("%.1f", number));
        } else {
            sb.append(String.format("%.0f", number));
        }

        if (exp > 0) {
            sb.append(SUFFIXES[exp]);
        } else if (milli) {
            sb.append('m');
        }

        sb.append(unit);
        return sb.toString();
    }
}
