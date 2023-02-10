package net.dasunterstrich.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DiscordUtils {
    private static final Pattern pattern = Pattern.compile("<?@?(\\d+)>?");

    public static Optional<Member> parseStringAsMember(Guild guild, String userString) {
        try {
            var matcher = pattern.matcher(userString);
            matcher.find();

            return Optional.of(guild.retrieveMemberById(matcher.group(1)).timeout(2, TimeUnit.SECONDS).complete());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
