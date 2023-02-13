package net.dasunterstrich.futari;

import net.dasunterstrich.futari.commands.*;
import net.dasunterstrich.futari.commands.internal.CommandManager;
import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.listener.ChannelCreateListener;
import net.dasunterstrich.futari.listener.GuildMemberJoinListener;
import net.dasunterstrich.futari.listener.UsernameUpdateListener;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.TimedPunishmentHandler;
import net.dasunterstrich.futari.reports.ReportManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Bot {
    Logger logger = LoggerFactory.getLogger(getClass());

    public void start() {
        var databaseHandler = initializeDatabase();
        var commandManager = new CommandManager();
        var reportManager = new ReportManager(databaseHandler);
        var punisher = new Punisher(databaseHandler, reportManager);

        JDA jda = JDABuilder.createDefault(readToken())
                .setActivity(Activity.playing("with Bocchicord"))
                .addEventListeners(commandManager, new ChannelCreateListener(), new GuildMemberJoinListener(databaseHandler), new UsernameUpdateListener(reportManager))
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(@NotNull ReadyEvent event) {
                var guild = jda.getGuildById(497092213034188806L);
                reportManager.setGuild(guild);
                new TimedPunishmentHandler(databaseHandler, punisher, jda, guild);
            }
        });

        // TODO: Add all commands (+ help, reason, duration)
        // TODO: Better error handling in commands
        // TODO: Update old mutes/bans
        // TODO: Ban message deletion argument
        // TODO: Split Punisher
        // TODO: Warn if can't punish user
        // TODO: Alt linking
        // TODO: Right-click user interactions
        // TODO: Message attachments
        // TODO: Update thread names

        commandManager.addCommand(new HelpCommand(commandManager));
        commandManager.addCommand(new BanCommand(punisher));
        commandManager.addCommand(new MuteCommand(punisher));
        commandManager.addCommand(new WarnCommand(punisher));
        commandManager.addCommand(new KickCommand(punisher));
        commandManager.addCommand(new UnbanCommand(punisher));
        commandManager.addCommand(new UnmuteCommand(punisher));

        jda.updateCommands().addCommands(commandManager.registeredCommandData()).queue();
        logger.info("Commands initialized");
    }

    private String readToken() {
        try {
            return Files.readAllLines(Path.of("token.txt")).get(0);
        } catch (IOException e) {
            logger.error("Token not found, please create a token.txt");
            throw new RuntimeException(e);
        }
    }

    private DatabaseHandler initializeDatabase() {
        var databaseHandler = new DatabaseHandler();
        databaseHandler.initializeDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                databaseHandler.closeDataSource();
                logger.info("Database connection shutdown!");
            }
        });

        logger.info("Database connection established!");
        return databaseHandler;
    }
}
