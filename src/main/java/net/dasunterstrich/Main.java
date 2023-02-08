package net.dasunterstrich;

import net.dasunterstrich.commands.BanCommand;
import net.dasunterstrich.commands.MuteCommand;
import net.dasunterstrich.commands.WarnCommand;
import net.dasunterstrich.commands.internal.CommandManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        var commandManager = new CommandManager();

        JDA jda = JDABuilder.createDefault(readToken())
                .setActivity(Activity.playing("with Bocchicord"))
                .addEventListeners(commandManager)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .build();

        // TODO: Permissions
        // TODO: Commands
        // TODO: Change messages

        commandManager.addCommand(new BanCommand());
        commandManager.addCommand(new MuteCommand());
        commandManager.addCommand(new WarnCommand());

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