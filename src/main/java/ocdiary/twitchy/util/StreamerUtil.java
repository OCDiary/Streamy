package ocdiary.twitchy.util;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import ocdiary.twitchy.Twitchy;
import ocdiary.twitchy.TwitchyConfig;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author UpcraftLP
 */
public class StreamerUtil {

    public static String getPlayerStreamerName() {
        String username = TwitchyConfig.GENERAL.streamerModeNameOverride;
        if (StringUtils.isNullOrEmpty(username)) username = Minecraft.getMinecraft().getSession().getUsername();
        return username;
    }

    private static boolean streamerFilter(StreamInfo streamer) {
        //Do not show channel if showOfflineChannels == false and streamer is offline
        if (!TwitchyConfig.CHANNELS.showOfflineChannels && !streamer.streaming)
            return false;
        switch (TwitchyConfig.GENERAL.streamerMode) {
            case FULL:
                if (streamer.broadcaster.equalsIgnoreCase(getPlayerStreamerName()))
                    return false;
            case PARTIAL:
                if (streamer.broadcaster.equalsIgnoreCase(getPlayerStreamerName()) && streamer.streaming)
                    return false;
            case OFF:
            default:
                return true;
        }
    }

    public static Map<String, StreamInfo> getStreamers() {
        return Twitchy.LIVE_STREAMERS.entrySet().stream()
                .filter(entry -> StreamerUtil.streamerFilter(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static List<String> sortChannelNames(Set<String> names) {
        if (TwitchyConfig.CHANNELS.sortChannels) {
            List<String> sortedList = Lists.newArrayList(names);
            sortedList.sort(String::compareToIgnoreCase);
            return sortedList;
        } else {
            //Sort by the order in the configs
            List<String> sortedList = Lists.newArrayList();
            List<String> configNames = Lists.newArrayList(TwitchyConfig.CHANNELS.channels);
            configNames.forEach(name -> {
                name = name.toLowerCase();
                if (names.contains(name))
                    sortedList.add(name);
            });
            return sortedList;
        }
    }

    public static void openTwitchStream(String channel) {
        String url = "https://twitch.tv/" + channel;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Twitchy.LOGGER.error("Can't open browser - Desktop not supported!");
            }
        } catch (URISyntaxException e) {
            Twitchy.LOGGER.error("URL \"" + url + "\" is invalid. Report this to the mod author!");
        } catch (Exception e) {
            Twitchy.LOGGER.error("Can't open browser", e);
        }
    }
}
