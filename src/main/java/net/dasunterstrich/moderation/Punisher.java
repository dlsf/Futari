package net.dasunterstrich.moderation;

import net.dasunterstrich.utils.DurationUtils;
import net.dasunterstrich.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Punisher {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ReportManager reportManager;

    public Punisher(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    public boolean ban(Guild guild, Member member, Member moderator, String reason, String duration, String comments, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return false;

        // DM user
        boolean contactSuccess = contactUser(member.getUser(), EmbedUtils.custom(
                "Banned from " + guild.getName(),
                "**Duration**: " + DurationUtils.toReadableDuration(duration) + "\n**Reason**: " + reason,
                Color.RED));

        // Ban user
        try {
            guild.ban(member, 0, TimeUnit.DAYS).reason(reason + "(" + duration + ")").queue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }

        // Database Update

        // Thread update
        var report = new Report(ReportType.BAN, member.getUser(), moderator.getUser(), reason, comments);
        report.setDuration(duration);

        reportManager.createReport(member.getUser(), report);
        return true;
    }

    public boolean mute(Guild guild, Member member, Member moderator, String reason, String duration, String comments, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return false;

        // DM user
        boolean contactSuccess = contactUser(member.getUser(), EmbedUtils.custom(
                "Muted from " + guild.getName(),
                "**Duration**: " + DurationUtils.toReadableDuration(duration) + "\n**Reason**: " + reason,
                Color.GRAY));

        guild.addRoleToMember(member, guild.getRoleById(1073208950280953906L)).reason(reason).queue();

        // Database update

        // Thread update
        var report = new Report(ReportType.MUTE, member.getUser(), moderator.getUser(), reason, comments);
        report.setDuration(duration);

        reportManager.createReport(member.getUser(), report);
        return true;
    }

    public boolean warn(Guild guild, Member member, Member moderator, String reason, String comments, ReportedMessage reportedMessage) {
        if (!moderator.hasPermission(Permission.BAN_MEMBERS)) return false;

        // DM user
        boolean contactSuccess = contactUser(member.getUser(), EmbedUtils.custom(
                "Warned from " + guild.getName(),
                "**Reason**: " + reason,
                Color.YELLOW));

        // Thread update
        var report = new Report(ReportType.WARN, member.getUser(), moderator.getUser(), reason, comments);
        reportManager.createReport(member.getUser(), report);

        return true;
    }

    private boolean contactUser(User user, MessageEmbed embed) {
        try {
            user.openPrivateChannel()
                    .flatMap(privateChannel -> privateChannel.sendMessageEmbeds(embed))
                    .timeout(3, TimeUnit.SECONDS)
                    .complete();
        } catch (Exception exception) {
            // When the bot doesn't share a guild with the user etc just treat it as a failed contact attempt
            return false;
        }

        return true;
    }

}
