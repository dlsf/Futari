package net.dasunterstrich.futari;

import net.dasunterstrich.futari.commands.BanCommand;
import net.dasunterstrich.futari.commands.MuteCommand;
import net.dasunterstrich.futari.commands.WarnCommand;
import net.dasunterstrich.futari.commands.internal.CommandManager;
import net.dasunterstrich.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.listener.ChannelCreateListener;
import net.dasunterstrich.futari.moderation.Punisher;
import net.dasunterstrich.futari.moderation.ReportManager;
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

        JDA jda = JDABuilder.createDefault(readToken())
                .setActivity(Activity.playing("with Bocchicord"))
                .addEventListeners(commandManager, new ChannelCreateListener())
                .setMemberCachePolicy(MemberCachePolicy.ONLINE)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .build();

        var reportManager = new ReportManager();
        var punisher = new Punisher(reportManager);

        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(@NotNull ReadyEvent event) {
                reportManager.setGuild(jda.getGuildById(497092213034188806L));
            }
        });

        // TODO: Consistent parameters (nullability)
        // TODO: Add all commands
        // TODO: Automatically remove punishments
        // TODO: Better error handling in commands
        // TODO: Update old mutes/bans

        commandManager.addCommand(new BanCommand(punisher));
        commandManager.addCommand(new MuteCommand(punisher));
        commandManager.addCommand(new WarnCommand(punisher));

        jda.updateCommands().addCommands(commandManager.registeredCommands()).queue();
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
        databaseHandler.initializeConnectionPool();

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
