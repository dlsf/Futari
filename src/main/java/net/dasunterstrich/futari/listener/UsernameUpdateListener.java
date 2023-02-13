package net.dasunterstrich.futari.listener;

import net.dasunterstrich.futari.reports.ReportManager;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class UsernameUpdateListener extends ListenerAdapter {
    private final ReportManager reportManager;

    public UsernameUpdateListener(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        var user = event.getUser();

        System.out.println(reportManager.hasReportThread(user));
        if (!reportManager.hasReportThread(user)) {
            return;
        }

        var reportThreadID = reportManager.getReportThreadID(user);
        event.getJDA().getGuildById(497092213034188806L)
                .getThreadChannelById(reportThreadID)
                .getManager()
                .setName(user.getAsTag() + " (" + user.getIdLong() + ")")
                .reason("User updated name")
                .queue();
    }
}
