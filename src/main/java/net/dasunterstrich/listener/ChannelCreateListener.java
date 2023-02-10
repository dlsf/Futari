package net.dasunterstrich.listener;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class ChannelCreateListener extends ListenerAdapter {
    private final List<Permission> muteRolePermissions = List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION);

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!(event.getChannel() instanceof StandardGuildChannel)) return;

        var channelManager = event.getChannel().asStandardGuildChannel().getManager();
        channelManager.putRolePermissionOverride(1073208950280953906L, null, muteRolePermissions).queue();
    }
}
