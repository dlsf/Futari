package moe.das.futari.utils;

import java.util.Arrays;

public class ExceptionUtils {
    public static String stringify(Throwable throwable) {
        var message = throwable.getMessage().split(" ");
        return String.join(" ", Arrays.copyOfRange(message, 1, message.length));
    }
}
