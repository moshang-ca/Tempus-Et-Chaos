package org.moshang.tempusetchaos.util;

import org.moshang.tempusetchaos.annotation.Localization;
import org.moshang.tempusetchaos.annotation.Translation;

public class TECLocalizations {
    private static final String[] SUFFIXES = {"", "k", "M", "G", "T", "P", "E", "Z"};

    @Localization(translations = {@Translation(text = "%s/T")})
    public static final String CHRONON_RATE = "info.tempusetchaos.chronon_rate";
    @Localization(translations = {@Translation(text = "%s / %s")})
    public static final String CHRONON  = "info.tempusetchaos.chronon";

    @Localization(translations = {@Translation(text = "Time Extracting")})
    public static final String JEI_TIME_EXTRACTING = "jei.tempusetchaos.time_extracting";
    @Localization(translations = {@Translation(text = "Entropy Forging")})
    public static final String JEI_ENTROPY_FORGING = "jei.tempusetchaos.entropy_forging";
    @Localization(translations = {@Translation(text = "%s t")})
    public static final String JEI_TIME = "jei.tempusetchaos.time";
    @Localization(translations = {@Translation(text = "%s CH")})
    public static final String JEI_CHRONON = "jei.tempusetchaos.chronon";
    @Localization(translations = {@Translation(text = "%s FE")})
    public static final String JEI_ENERGY = "jei.tempusetchaos.energy";
    @Localization(translations = {@Translation(text = "%s%%")})
    public static final String JEI_CHANCE = "jei.tempusetchaos.chance";
    @Localization(translations = {@Translation(text = "Per Tick: %s")})
    public static final String JEI_TICK = "jei.tempusetchaos.per_tick";

    public static String formatCompact(double number, boolean milli, String unit, boolean keepPlus) {
        if (Double.isNaN(number)) return "NaN" + unit;
        if (Double.isInfinite(number)) return (number > 0 ? "∞" : "-∞") + unit;

        StringBuilder sb = new StringBuilder();

        if (number < 0) {
            sb.append('-');
            number = -number;
        } else if (number > 0 && keepPlus) {
            sb.append('+');
        }

        if (milli && number >= 1000) {
            number /= 1000;
            milli = false;
        }

        int exp = 0;
        if (number >= 1000) {
            while (number > 1000) {
                number /= 1000;
                exp++;
            }
        }

        if (number == (long) number) {
            sb.append((long) number);
        } else {
            sb.append(String.format("%.1f", number));
        }

        if (exp > 0) {
            sb.append(SUFFIXES[exp]);
        } else if (milli) {
            sb.append('m');
        }

        sb.append(unit);
        return sb.toString();
    }

    public static String formatCompact(double number, boolean milli, String unit) {
        return formatCompact(number, milli, unit, false);
    }
}
