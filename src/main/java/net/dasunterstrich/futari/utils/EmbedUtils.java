package net.dasunterstrich.futari.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;

public class EmbedUtils {
    public static MessageEmbed custom(String title, String text, Color color) {
        return new EmbedBuilder().setColor(color).setTitle(title).setDescription(text).build();
    }

    public static MessageEmbed customWithTimestamp(String title, String text, Color color) {
        return new EmbedBuilder().setColor(color).setTitle(title).setDescription(text).setTimestamp(Instant.now()).build();
    }

    public static MessageEmbed success(String title, String text) {
        return new EmbedBuilder().setColor(Color.GREEN).setTitle(title).setDescription(text).build();
    }

    public static MessageEmbed error(String text) {
        return new EmbedBuilder().setColor(Color.RED).setDescription(text).build();
    }
}
