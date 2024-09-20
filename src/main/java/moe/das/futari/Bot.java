package moe.das.futari;

import moe.das.futari.commands.*;
import moe.das.futari.listener.*;
import net.dasunterstrich.futari.commands.*;
import moe.das.futari.commands.internal.CommandManager;
import moe.das.futari.database.DatabaseHandler;
import net.dasunterstrich.futari.listener.*;
import moe.das.futari.moderation.Punisher;
import moe.das.futari.moderation.modlog.ModlogManager;
import moe.das.futari.moderation.reports.ReportManager;
import moe.das.futari.scheduler.MessageLogHandler;
import moe.das.futari.scheduler.TimedPunishmentHandler;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void start() {
        var databaseHandler = initializeDatabase();
        var commandManager = new CommandManager();
        var reportManager = new ReportManager(databaseHandler);
        var modlogManager = new ModlogManager();
        var punisher = new Punisher(databaseHandler, reportManager, modlogManager);
        var messageLogHandler = new MessageLogHandler(databaseHandler);

        JDA jda = JDABuilder.createDefault(readToken())
                .setActivity(Activity.playing("with Bocchicord"))
                .addEventListeners(
                        commandManager,
                        new ChannelCreationListener(),
                        new GuildMemberJoinListener(databaseHandler),
                        new UsernameUpdateListener(reportManager),
                        new CommandAutoCompleteListener(),
                        new MessageDeletionListener(messageLogHandler),
                        new MessageCreationListener(messageLogHandler))
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

        // TODO: Migrate to Postgres
        // TODO: Add DelWarn, Duration, Softban commands
        // TODO: Update old mutes/bans
        // TODO: Alt linking
        // TODO: Right-click user interactions
        // TODO: Message attachments in report threads
        // TODO: Not being able to punish equally ranked users
        // TODO: Config
        // TODO: Handle deleted messages
        // TODO: DM user at the end of the punishment process
        // TODO: Handle NONE messages (user interactions)
        // TODO: Investigate if queue Consumer is async

        commandManager.addCommand(new HelpCommand(commandManager));
        commandManager.addCommand(new BanCommand(punisher));
        commandManager.addCommand(new MuteCommand(punisher));
        commandManager.addCommand(new WarnCommand(punisher));
        commandManager.addCommand(new KickCommand(punisher));
        commandManager.addCommand(new UnbanCommand(punisher));
        commandManager.addCommand(new UnmuteCommand(punisher));
        commandManager.addCommand(new ModlogCommand(databaseHandler));
        commandManager.addCommand(new ReasonCommand(databaseHandler, reportManager, modlogManager));

        jda.updateCommands().addCommands(commandManager.registeredCommandData()).queue();
        logger.info("Commands initialized");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                messageLogHandler.addMessagesToDatabase();
                logger.info("Saved cached messages in database");
                databaseHandler.closeDataSource();
                logger.info("Database connection shutdown!");
            }
        });
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

        logger.info("Database connection established!");
        return databaseHandler;
    }
}
