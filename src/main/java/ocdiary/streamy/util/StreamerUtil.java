package ocdiary.streamy.util;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import ocdiary.streamy.Streamy;
import ocdiary.streamy.StreamyConfig;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author UpcraftLP
 */
public class StreamerUtil {

    public static String getPlayerStreamerName() {
        String username = StreamyConfig.GENERAL.streamerModeNameOverride;
        if (StringUtils.isNullOrEmpty(username)) username = Minecraft.getMinecraft().getSession().getUsername();
        return username;
    }

    private static boolean streamerFilter(StreamInfo streamer) {
        //Do not show channel if showOfflineChannels == false and streamer is offline
        if (!StreamyConfig.CHANNELS.showOfflineChannels && !streamer.streaming)
            return false;
        switch (StreamyConfig.GENERAL.streamerMode) {
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

    public static String[] splitChannelConfig(String channel) {
        int index = channel.indexOf(':');
        if (index < 0) return null;
        String[] split = new String[2];
        split[0] = channel.substring(0, index);
        split[1] = channel.substring(index + 1);
        return split;
    }

    public static List<StreamInfo> getStreamers() {
        Stream<StreamInfo> streamers = Streamy.LIVE_STREAMERS.values().stream()
                .filter(StreamerUtil::streamerFilter);
        if (StreamyConfig.CHANNELS.sortChannels) {
            //Sort by channel name
            return streamers.sorted((o1, o2) -> o1.broadcaster.compareToIgnoreCase(o2.broadcaster)).collect(Collectors.toList());
        } else {
            //Sort by the order in the configs
            Map<String, StreamInfo> streamInfos = streamers.collect(Collectors.toMap(stream -> stream.broadcaster.toLowerCase(Locale.ROOT), stream -> stream));
            List<StreamInfo> sortedList = Lists.newArrayList();
            for (String configChannel : StreamyConfig.CHANNELS.channels) {
                String[] split = splitChannelConfig(configChannel);
                if (split == null) continue;
                String name = split[1].toLowerCase(Locale.ROOT);
                StreamInfo streamInfo = streamInfos.get(name);
                if (streamInfo != null)
                    sortedList.add(streamInfo);
            }
            return sortedList;
        }
    }
}
