package net.dasunterstrich;

import net.dasunterstrich.commands.ModalBanCommand;
import net.dasunterstrich.listener.ComponentListener;
import net.dasunterstrich.listener.MessageListener;
import net.dasunterstrich.listener.SlashCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        JDA jda = JDABuilder.createDefault(readToken())
                .setActivity(Activity.playing("with Bocchicord"))
                .addEventListeners(new ComponentListener(), new SlashCommandListener(), new MessageListener(), new ModalBanCommand())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .build();

        // TODO: Permissions

        jda.updateCommands()
                .addCommands(RegisterableCommand.BAN.getCommandData())
                .addCommands(RegisterableCommand.BAN_MODAL.getCommandData())
                .queue();
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