package net.dasunterstrich;

import net.dasunterstrich.commands.BanCommand;
import net.dasunterstrich.commands.MuteCommand;
import net.dasunterstrich.commands.WarnCommand;
import net.dasunterstrich.commands.internal.CommandManager;
import net.dasunterstrich.listener.ChannelCreateListener;
import net.dasunterstrich.moderation.Punisher;
import net.dasunterstrich.moderation.ReportManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
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
        // TODO: DM users
        // TODO: Better error handling in commands
        // TODO: Update old mutes/bans

        commandManager.addCommand(new BanCommand(punisher));
        commandManager.addCommand(new MuteCommand(punisher));
        commandManager.addCommand(new WarnCommand(punisher));

        jda.updateCommands().addCommands(commandManager.registeredCommands()).queue();
    }

    private static String readToken() {
        try {
            return Files.readAllLines(Path.of("token.txt")).get(0);
        } catch (IOException e) {
            System.out.println("Token not found, please create a token.txt");
            throw new RuntimeException(e);
        }
    }

}