package ocdiary.twitchy.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import ocdiary.twitchy.Twitchy;
import ocdiary.twitchy.TwitchyConfig;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author UpcraftLP
 */
public class StreamerUtil {

    public static String getPlayerStreamerName()
    {
        String username = TwitchyConfig.streamerModeNameOverride;
        if(StringUtils.isNullOrEmpty(username)) username = Minecraft.getMinecraft().getSession().getUsername();
        return username;
    }

    public static boolean streamerFilter(StreamInfo streamer)
    {
        //Do not show channel if showOfflineChannels == false and streamer is offline
        if(!TwitchyConfig.CHANNELS.showOfflineChannels && !streamer.streaming)
            return false;
        //If streamerMode == OFF, then show streamer
        if(TwitchyConfig.streamerMode == EnumStreamerMode.OFF)
            return true;
        //If username is equal to the streamer name, then don't show
        return !streamer.broadcaster.equalsIgnoreCase(getPlayerStreamerName());
    }

    public static void openTwitchStream(String channel) {
        String url = "https://twitch.tv/" + channel;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
            else {
                Twitchy.LOGGER.error("Can't open browser - Desktop not supported!");
            }
        } catch (URISyntaxException e) {
            Twitchy.LOGGER.error("URL \"" + url + "\" is invalid. Report this to the mod author!");
        } catch (Exception e) {
            Twitchy.LOGGER.error("Can't open browser", e);
        }
    }
}
