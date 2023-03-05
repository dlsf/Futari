package net.dasunterstrich.futari.utils;

public class StringUtils {
    public static boolean isInteger(String string) {
        try {
            Integer.parseInt(string);
        } catch (NumberFormatException exception) {
            return false;
        }

        return true;
    }
}
