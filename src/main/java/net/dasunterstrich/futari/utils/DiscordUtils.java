package net.dasunterstrich.futari.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.AttachedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DiscordUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordUtils.class);
    private static final Pattern USER_PATTERN = Pattern.compile("<?@?(\\d+)>?");
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "tiff", "svg", "apng"));

    public static Optional<Member> parseStringAsMember(Guild guild, String userString) {
        try {
            var matcher = USER_PATTERN.matcher(userString);
            matcher.find();

            return Optional.of(guild.retrieveMemberById(matcher.group(1)).timeout(2, TimeUnit.SECONDS).complete());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public static Optional<User> parseStringAsUser(JDA jda, String userString) {
        try {
            var matcher = USER_PATTERN.matcher(userString);
            matcher.find();

            return Optional.of(jda.retrieveUserById(matcher.group(1)).timeout(2, TimeUnit.SECONDS).complete());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public static boolean isImageFileURL(String url) {
        var parts = url.toLowerCase().split("\\.");
        return IMAGE_EXTENSIONS.contains(parts[parts.length - 1]);
    }

    public static List<AttachedFile> getAttachedFilesFromUrls(List<String> attachmentUrls) {
        var attachedFiles = new ArrayList<AttachedFile>();

        for (String url : attachmentUrls) {
            try {
                attachedFiles.add(AttachedFile.fromData(new URL(url).openStream(), DiscordUtils.getFileName(url)));
            } catch (IOException exception) {
                LOGGER.warn("Could not convert attachment " + url);
            }
        }

        return attachedFiles;
    }

    public static String getFileName(String url) {
        var parts = url.toLowerCase().split("/");
        return parts[parts.length - 1];
    }
}
