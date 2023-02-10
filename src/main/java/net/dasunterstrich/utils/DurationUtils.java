package net.dasunterstrich.utils;

import java.util.regex.Pattern;

public class DurationUtils {

    private static final Pattern durationPattern = Pattern.compile("(\\d+[mhd])");
    private static final int HOURS = 24;
    private static final int MINUTES = 60;
    private static final int SECONDS = 60;
    private static final int MILLISECONDS = 1000;

    public static long durationStringToMillis(String durationString) throws IllegalArgumentException {
        durationString = durationString.toLowerCase();

        var matcher = durationPattern.matcher(durationString);
        if (!matcher.find()) throw new IllegalArgumentException();

        var duration = 0L;
        do {
            var group = matcher.group(1);
            var amount = Integer.parseInt(group.substring(0, group.length() - 1));

            duration += switch (group.toCharArray()[group.length() - 1]) {
                case 'm' -> amount * SECONDS * MILLISECONDS;
                case 'h' -> amount * MINUTES * SECONDS * MILLISECONDS;
                case 'd' -> amount * HOURS * MINUTES * SECONDS * MILLISECONDS;
                default -> throw new IllegalArgumentException();
            };
        } while (matcher.find());

        return duration;
    }

    public static boolean isValidDurationString(String input) {
        var matcher = durationPattern.matcher(input);
        return matcher.find();
    }

    public static String toReadableDuration(String durationString) {
        if (durationString.isEmpty()) {
            return "Permanent";
        } else {
            return durationString
                    .replace(" ", ", ")
                    .replace("h", " hours")
                    .replace("d", " days")
                    .replace("m", " minutes");
        }
    }

}
